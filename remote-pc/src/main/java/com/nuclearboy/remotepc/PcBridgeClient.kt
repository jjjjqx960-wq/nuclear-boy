package com.nuclearboy.remotepc

import com.nuclearboy.common.AppError
import com.nuclearboy.common.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * nb-pc-bridge 的 WebSocket 客户端。
 *
 * 每次操作使用独立连接（连接 → 鉴权 → 执行 → 关闭），避免长连接的
 * 重连和多路复用复杂度；任务为低频长耗时操作，连接开销可忽略。
 */
class PcBridgeClient(private val configStore: PcBridgeConfigStore) {

    data class BridgeInfo(val host: String, val clis: Map<String, String>)

    data class CliTaskResult(
        val exitCode: Int,
        val result: String,
        val durationMs: Long,
        val outputLog: List<String>,
        /** 本次任务的会话 ID，下次传入 runCliTask 的 sessionId 可继续对话（仅 claude） */
        val sessionId: String = "",
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // 流式任务无读超时，由任务级超时控制
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    /**
     * 测试连接：鉴权成功后返回主机名和 CLI 版本，并记录到配置。
     * url/token 传 null 时使用已保存配置。
     */
    suspend fun testConnection(url: String? = null, token: String? = null): AppResult<BridgeInfo> {
        val targetUrl = url ?: configStore.currentUrl()
        val targetToken = token ?: configStore.currentToken()
        if (targetUrl.isBlank() || targetToken.isBlank()) {
            return AppResult.failure(AppError.InvalidRequest, "先填写电脑地址和 token 再测试连接")
        }
        return withSession(targetUrl, targetToken, timeoutMs = CONNECT_TEST_TIMEOUT_MS) { session ->
            configStore.recordConnected(session.info.host, session.info.clis)
            AppResult.success(session.info)
        }
    }

    /**
     * 在电脑上执行一条 CLI 任务，流式输出经 [onOutput] 推送，返回最终结果。
     */
    suspend fun runCliTask(
        cli: String,
        prompt: String,
        cwd: String? = null,
        timeoutSec: Int = DEFAULT_TASK_TIMEOUT_SEC,
        sessionId: String? = null,
        onOutput: suspend (kind: String, text: String) -> Unit = { _, _ -> },
    ): AppResult<CliTaskResult> {
        if (!configStore.isEnabled()) {
            return AppResult.failure(AppError.InvalidRequest, "远程电脑功能未开启，去设置页打开并配置连接")
        }
        if (!configStore.isConfigured()) {
            return AppResult.failure(AppError.InvalidRequest, "远程电脑还没配置好，去设置页填写地址和 token")
        }
        val taskId = UUID.randomUUID().toString().replace("-", "")
        // 任务超时之外额外留出连接和收尾的余量
        val sessionTimeoutMs = (timeoutSec + SESSION_GRACE_SEC) * 1000L

        return withSession(configStore.currentUrl(), configStore.currentToken(), sessionTimeoutMs) { session ->
            session.ws.send(
                PcBridgeProtocol.encodeRun(
                    PcBridgeProtocol.RunMessage(
                        id = taskId, cli = cli, prompt = prompt,
                        cwd = cwd?.takeIf { it.isNotBlank() }, timeoutSec = timeoutSec,
                        sessionId = sessionId?.takeIf { it.isNotBlank() },
                    )
                )
            )
            val outputLog = mutableListOf<String>()
            while (true) {
                when (val msg = session.receive()) {
                    is PcBridgeProtocol.Inbound.Accepted -> Unit
                    is PcBridgeProtocol.Inbound.Output -> {
                        if (msg.id == taskId && msg.text.isNotBlank()) {
                            if (outputLog.size < MAX_OUTPUT_LOG_LINES) {
                                outputLog.add("[${msg.kind}] ${msg.text}")
                            }
                            onOutput(msg.kind, msg.text)
                        }
                    }
                    is PcBridgeProtocol.Inbound.Done -> if (msg.id == taskId) {
                        return@withSession AppResult.success(
                            CliTaskResult(msg.exitCode, msg.result, msg.durationMs, outputLog, msg.sessionId)
                        )
                    }
                    is PcBridgeProtocol.Inbound.Error -> if (msg.id == taskId || msg.id.isBlank()) {
                        return@withSession AppResult.failure(AppError.ServerError, msg.message)
                    }
                    else -> Unit // pong / unknown 忽略
                }
            }
            @Suppress("UNREACHABLE_CODE")
            AppResult.failure(AppError.Unknown, "任务意外结束")
        }
    }

    // ── 会话管理 ─────────────────────────────────────

    private class Session(
        val ws: WebSocket,
        val info: BridgeInfo,
        private val inbox: Channel<PcBridgeProtocol.Inbound>,
    ) {
        suspend fun receive(): PcBridgeProtocol.Inbound = inbox.receive()
    }

    /**
     * 建立连接并完成鉴权，把会话交给 [block]，结束后保证连接关闭。
     * 连接失败、鉴权失败、超时统一转成 AppResult.Failure。
     */
    private suspend fun <T> withSession(
        url: String,
        token: String,
        timeoutMs: Long,
        block: suspend (Session) -> AppResult<T>,
    ): AppResult<T> {
        val inbox = Channel<PcBridgeProtocol.Inbound>(Channel.UNLIMITED)
        var webSocket: WebSocket? = null
        return try {
            withTimeout(timeoutMs) {
                val ws = openWebSocket(url, inbox)
                webSocket = ws
                ws.send(PcBridgeProtocol.encodeAuth(token))
                when (val first = inbox.receive()) {
                    is PcBridgeProtocol.Inbound.AuthOk -> {
                        val session = Session(ws, BridgeInfo(first.host, first.clis), inbox)
                        block(session)
                    }
                    is PcBridgeProtocol.Inbound.AuthFail ->
                        AppResult.failure(AppError.ApiKeyInvalid, "电脑拒绝了连接：${first.message}。检查 token 是否和电脑端 config.json 一致")
                    else ->
                        AppResult.failure(AppError.ServerError, "电脑回复了意外消息，确认地址指向 nb-pc-bridge 服务")
                }
            }
        } catch (e: CancellationException) {
            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                AppResult.failure(AppError.NetworkTimeout, "连接电脑超时，确认手机和电脑在同一网络、bridge 已启动")
            } else {
                throw e
            }
        } catch (e: BridgeClosedException) {
            AppResult.failure(AppError.NetworkUnavailable, "和电脑的连接断开了：${e.message}")
        } catch (e: Exception) {
            AppResult.failure(AppError.NetworkUnavailable, "连不上电脑（${e.message ?: e.javaClass.simpleName}），检查地址和网络")
        } finally {
            webSocket?.close(1000, "done")
            inbox.close()
        }
    }

    private class BridgeClosedException(message: String) : Exception(message)

    private suspend fun openWebSocket(
        url: String,
        inbox: Channel<PcBridgeProtocol.Inbound>,
    ): WebSocket = suspendCancellableCoroutine { cont ->
        val request = Request.Builder().url(url.toHttpWsUrl()).build()
        val ws = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (cont.isActive) cont.resumeWith(Result.success(webSocket))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                PcBridgeProtocol.parseInbound(text)?.let { inbox.trySend(it) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (cont.isActive) {
                    cont.resumeWith(Result.failure(t))
                } else {
                    inbox.close(BridgeClosedException(t.message ?: "连接异常"))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                inbox.close(BridgeClosedException(reason.ifBlank { "连接已关闭($code)" }))
            }
        })
        cont.invokeOnCancellation { ws.cancel() }
    }

    /** OkHttp 要求 http/https scheme，把 ws:// 映射成 http://。 */
    private fun String.toHttpWsUrl(): String = when {
        startsWith("ws://") -> "http://" + removePrefix("ws://")
        startsWith("wss://") -> "https://" + removePrefix("wss://")
        startsWith("http://") || startsWith("https://") -> this
        else -> "http://$this"
    }

    companion object {
        const val DEFAULT_TASK_TIMEOUT_SEC = 600
        private const val SESSION_GRACE_SEC = 30
        private const val CONNECT_TEST_TIMEOUT_MS = 15_000L
        private const val MAX_OUTPUT_LOG_LINES = 200
    }
}
