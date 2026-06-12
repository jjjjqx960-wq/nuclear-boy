package com.nuclearboy.remotepc

import com.nuclearboy.common.AppError
import com.nuclearboy.common.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
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
        /** 本次任务的会话 ID，下次传入 runCliTask 的 sessionId 可继续对话 */
        val sessionId: String = "",
        /** 隔离执行时改动所在的 worktree 路径和分支 */
        val worktreePath: String = "",
        val worktreeBranch: String = "",
    )

    data class RunningTask(
        val id: String,
        val cli: String,
        val promptPreview: String,
        val cwd: String,
        val elapsedMs: Long,
    )

    /** 当前由本客户端发起、还在电脑上执行的任务 ID */
    private val activeTasks = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        useWorktree: Boolean = false,
        approval: String? = null,
        onOutput: suspend (kind: String, text: String) -> Unit = { _, _ -> },
        onPermissionRequest: (suspend (toolName: String, inputSummary: String) -> Boolean)? = null,
    ): AppResult<CliTaskResult> {
        if (!configStore.isEnabled()) {
            return AppResult.failure(AppError.InvalidRequest, "远程电脑功能未开启，去设置页打开并配置连接")
        }
        if (!configStore.isConfigured()) {
            return AppResult.failure(AppError.InvalidRequest, "远程电脑还没配置好，去设置页填写地址和 token")
        }
        // session="last" → 自动续传该 CLI 最近一次任务的会话
        val resolvedSessionId = when {
            sessionId == LAST_SESSION_ALIAS -> configStore.lastSession(cli).ifBlank {
                return AppResult.failure(AppError.InvalidRequest, "还没有可续传的 $cli 会话，先跑一次任务再用 last")
            }
            else -> sessionId
        }
        val taskId = UUID.randomUUID().toString().replace("-", "")
        // 任务超时之外额外留出连接和收尾的余量
        val deadline = System.currentTimeMillis() + (timeoutSec + SESSION_GRACE_SEC) * 1000L
        val outputLog = mutableListOf<String>()
        // 任务是否已被电脑接受：决定断线重连后是补发 run 还是用 get_result 取回
        var taskAccepted = false
        // 已处理的最大输出 seq：重连时据此请求增量补发，并去重（历史增量同步）
        var maxSeq = -1
        var lastFailure: AppResult<CliTaskResult> =
            AppResult.failure(AppError.NetworkUnavailable, "没能连上电脑")

        activeTasks.add(taskId)
        try {
        repeat(MAX_TASK_ATTEMPTS) { attempt ->
            val remainingMs = deadline - System.currentTimeMillis()
            if (remainingMs <= 0) {
                return AppResult.failure(AppError.NetworkTimeout, "任务等待超时（断线重连后仍未拿到结果）")
            }
            val result = withSession(configStore.currentUrl(), configStore.currentToken(), remainingMs) { session ->
                val request = if (!taskAccepted) {
                    PcBridgeProtocol.encodeRun(
                        PcBridgeProtocol.RunMessage(
                            id = taskId, cli = cli, prompt = prompt,
                            cwd = cwd?.takeIf { it.isNotBlank() }, timeoutSec = timeoutSec,
                            sessionId = resolvedSessionId?.takeIf { it.isNotBlank() },
                            worktree = if (useWorktree) true else null,
                            approval = approval?.takeIf { it.isNotBlank() },
                        )
                    )
                } else {
                    // 断线前任务已开始：取回结果或重挂流，并请求增量补发漏掉的输出
                    PcBridgeProtocol.encodeGetResult(taskId, sinceSeq = maxSeq + 1)
                }
                session.send(request)
                while (true) {
                    when (val msg = session.receive()) {
                        is PcBridgeProtocol.Inbound.Accepted -> if (msg.id == taskId) taskAccepted = true
                        is PcBridgeProtocol.Inbound.Output -> {
                            // seq<=maxSeq 说明是重连补发里已见过的，去重跳过
                            if (msg.id == taskId && msg.text.isNotBlank() &&
                                (msg.seq < 0 || msg.seq > maxSeq)
                            ) {
                                if (msg.seq >= 0) maxSeq = msg.seq
                                taskAccepted = true
                                if (outputLog.size < MAX_OUTPUT_LOG_LINES) {
                                    outputLog.add("[${msg.kind}] ${msg.text}")
                                }
                                onOutput(msg.kind, msg.text)
                            }
                        }
                        is PcBridgeProtocol.Inbound.Done -> if (msg.id == taskId) {
                            configStore.recordLastSession(cli, msg.sessionId)
                            return@withSession AppResult.success(
                                CliTaskResult(
                                    msg.exitCode, msg.result, msg.durationMs, outputLog,
                                    msg.sessionId, msg.worktreePath, msg.worktreeBranch,
                                )
                            )
                        }
                        is PcBridgeProtocol.Inbound.PermissionRequest -> if (msg.id == taskId) {
                            // 电脑端 claude 请求权限：交给界面决定，结果回传 bridge
                            val approved = onPermissionRequest?.invoke(msg.toolName, msg.inputSummary) ?: false
                            session.send(
                                PcBridgeProtocol.encodePermissionResponse(taskId, approved)
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
            when {
                result is AppResult.Success -> return result
                // 网络波动断线：稍候重连取回，不让任务白跑
                result is AppResult.Failure && result.error in RETRYABLE_ERRORS && attempt < MAX_TASK_ATTEMPTS - 1 -> {
                    lastFailure = result
                    kotlinx.coroutines.delay(RECONNECT_DELAY_MS)
                }
                else -> return result
            }
        }
        return lastFailure
        } finally {
            activeTasks.remove(taskId)
        }
    }

    /**
     * 取消所有由本客户端发起、仍在电脑上执行的任务。
     * 用户取消对话时调用（fire-and-forget），避免电脑端白跑。
     */
    fun cancelActiveTasksAsync() {
        val snapshot = activeTasks.toList()
        if (snapshot.isEmpty() || !configStore.isConfigured()) return
        clientScope.launch {
            withSession(configStore.currentUrl(), configStore.currentToken(), CONNECT_TEST_TIMEOUT_MS) { session ->
                snapshot.forEach { id -> session.send(PcBridgeProtocol.encodeCancel(id)) }
                AppResult.success(true)
            }
        }
    }

    /** 查询电脑上正在执行的任务列表。 */
    suspend fun listRunningTasks(): AppResult<List<RunningTask>> {
        if (!configStore.isEnabled()) {
            return AppResult.failure(AppError.InvalidRequest, "远程电脑功能未开启，去设置页打开并配置连接")
        }
        if (!configStore.isConfigured()) {
            return AppResult.failure(AppError.InvalidRequest, "远程电脑还没配置好，去设置页填写地址和 token")
        }
        return withSession(configStore.currentUrl(), configStore.currentToken(), CONNECT_TEST_TIMEOUT_MS) { session ->
            session.send(PcBridgeProtocol.encodeListTasks())
            while (true) {
                when (val msg = session.receive()) {
                    is PcBridgeProtocol.Inbound.Tasks -> return@withSession AppResult.success(
                        msg.tasks.map { RunningTask(it.id, it.cli, it.promptPreview, it.cwd, it.elapsedMs) }
                    )
                    is PcBridgeProtocol.Inbound.Error ->
                        return@withSession AppResult.failure(AppError.ServerError, msg.message)
                    else -> Unit
                }
            }
            @Suppress("UNREACHABLE_CODE")
            AppResult.failure(AppError.Unknown, "查询意外结束")
        }
    }

    data class DirListing(val path: String, val entries: List<PcBridgeProtocol.DirEntry>, val truncated: Boolean)
    data class FileContent(val path: String, val content: String, val size: Long, val truncated: Boolean)

    /** 列出电脑上某目录（只读）。 */
    suspend fun listDir(path: String): AppResult<DirListing> {
        if (!configStore.isConfigured()) {
            return AppResult.failure(AppError.InvalidRequest, "远程电脑还没配置好，去设置页填写地址和 token")
        }
        return withSession(configStore.currentUrl(), configStore.currentToken(), CONNECT_TEST_TIMEOUT_MS) { session ->
            session.send(PcBridgeProtocol.encodeListDir("ld", path))
            while (true) {
                when (val msg = session.receive()) {
                    is PcBridgeProtocol.Inbound.DirListing ->
                        return@withSession AppResult.success(DirListing(msg.path, msg.entries, msg.truncated))
                    is PcBridgeProtocol.Inbound.Error ->
                        return@withSession AppResult.failure(AppError.ServerError, msg.message)
                    else -> Unit
                }
            }
            @Suppress("UNREACHABLE_CODE")
            AppResult.failure(AppError.Unknown, "列目录意外结束")
        }
    }

    /** 读取电脑上某文件文本（只读，默认上限 64KB）。 */
    suspend fun readFile(path: String, maxBytes: Int? = null): AppResult<FileContent> {
        if (!configStore.isConfigured()) {
            return AppResult.failure(AppError.InvalidRequest, "远程电脑还没配置好，去设置页填写地址和 token")
        }
        return withSession(configStore.currentUrl(), configStore.currentToken(), CONNECT_TEST_TIMEOUT_MS) { session ->
            session.send(PcBridgeProtocol.encodeReadFile("rf", path, maxBytes))
            while (true) {
                when (val msg = session.receive()) {
                    is PcBridgeProtocol.Inbound.FileContent ->
                        return@withSession AppResult.success(FileContent(msg.path, msg.content, msg.size, msg.truncated))
                    is PcBridgeProtocol.Inbound.Error ->
                        return@withSession AppResult.failure(AppError.ServerError, msg.message)
                    else -> Unit
                }
            }
            @Suppress("UNREACHABLE_CODE")
            AppResult.failure(AppError.Unknown, "读文件意外结束")
        }
    }

    data class FileWritten(val path: String, val bytes: Long)

    /** 写电脑上某文件（覆盖或追加）。危险操作，调用方应先经用户审批。 */
    suspend fun writeFile(path: String, content: String, append: Boolean = false): AppResult<FileWritten> {
        if (!configStore.isConfigured()) {
            return AppResult.failure(AppError.InvalidRequest, "远程电脑还没配置好，去设置页填写地址和 token")
        }
        return withSession(configStore.currentUrl(), configStore.currentToken(), CONNECT_TEST_TIMEOUT_MS) { session ->
            session.send(PcBridgeProtocol.encodeWriteFile("wf", path, content, if (append) true else null))
            while (true) {
                when (val msg = session.receive()) {
                    is PcBridgeProtocol.Inbound.FileWritten ->
                        return@withSession AppResult.success(FileWritten(msg.path, msg.bytes))
                    is PcBridgeProtocol.Inbound.Error ->
                        return@withSession AppResult.failure(AppError.ServerError, msg.message)
                    else -> Unit
                }
            }
            @Suppress("UNREACHABLE_CODE")
            AppResult.failure(AppError.Unknown, "写文件意外结束")
        }
    }

    // ── 会话管理 ─────────────────────────────────────

    private class Session(
        val ws: WebSocket,
        val info: BridgeInfo,
        private val inbox: Channel<PcBridgeProtocol.Inbound>,
        private val cryptoKey: ByteArray?,
    ) {
        suspend fun receive(): PcBridgeProtocol.Inbound = inbox.receive()
        /** 发送消息：开启加密时包成 AES-GCM 信封，否则明文。 */
        fun send(message: String) {
            ws.send(if (cryptoKey != null) PcCrypto.envelope(cryptoKey, message) else message)
        }
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
        val cryptoKey = if (configStore.isEncryptionEnabled() && token.isNotBlank())
            PcCrypto.deriveKey(token) else null
        var webSocket: WebSocket? = null
        return try {
            withTimeout(timeoutMs) {
                val ws = openWebSocket(url, inbox, cryptoKey)
                webSocket = ws
                // 加密时握手用信封（不含 token，解密成功即证明知道 token）；否则明文带 token
                if (cryptoKey != null) {
                    ws.send(PcCrypto.envelope(cryptoKey, """{"type":"auth"}"""))
                } else {
                    ws.send(PcBridgeProtocol.encodeAuth(token))
                }
                when (val first = inbox.receive()) {
                    is PcBridgeProtocol.Inbound.AuthOk -> {
                        val session = Session(ws, BridgeInfo(first.host, first.clis), inbox, cryptoKey)
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

    /** 从 {"enc":"<base64>"} 信封里取出密文 base64。 */
    private fun extractEnc(raw: String): String =
        raw.substringAfter(""""enc":"""").substringBeforeLast("\"")

    private suspend fun openWebSocket(
        url: String,
        inbox: Channel<PcBridgeProtocol.Inbound>,
        cryptoKey: ByteArray? = null,
    ): WebSocket = suspendCancellableCoroutine { cont ->
        val request = Request.Builder().url(url.toHttpWsUrl()).build()
        val ws = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (cont.isActive) cont.resumeWith(Result.success(webSocket))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // 加密会话：先解出明文再解析；解密失败（密钥不符/篡改）丢弃
                val plain = if (cryptoKey != null) {
                    runCatching { PcCrypto.decrypt(cryptoKey, extractEnc(text)) }.getOrNull() ?: return
                } else text
                PcBridgeProtocol.parseInbound(plain)?.let { inbox.trySend(it) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (cont.isActive) {
                    cont.resumeWith(Result.failure(t))
                } else {
                    inbox.close(BridgeClosedException(t.message ?: "连接异常"))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                inbox.close(BridgeClosedException(friendlyCloseMessage(code, reason)))
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
        const val LAST_SESSION_ALIAS = "last"

        /**
         * 把 WebSocket 关闭码翻译成给用户看的可操作中文提示。
         *
         * 4003/4004/4009/4000 是公网中继（relay_server.py）的应用关闭码，
         * 4029 是电脑端 bridge 鉴权限速封禁；其余回退到原因文本或通用提示。
         */
        fun friendlyCloseMessage(code: Int, reason: String): String = when (code) {
            4003 -> "中继口令不对，检查手机地址里的 key 和电脑端 --relay-key 是否一致"
            4004 -> "电脑还没上线（中继里这个房间没有电脑）。先在电脑端运行 bridge serve --relay 反连中继"
            4009 -> "这个房间已经有另一台电脑了，换个 room 或确认电脑没重复启动"
            4000 -> "中继地址格式不对，应是 ws://中继地址:端口/client/<房间>"
            4029 -> "连接被电脑临时拒绝：短时间内 token 错误次数过多被封禁，等几分钟再试"
            else -> reason.ifBlank { "连接已关闭（$code）" }
        }
        private const val SESSION_GRACE_SEC = 30
        private const val CONNECT_TEST_TIMEOUT_MS = 15_000L
        private const val MAX_OUTPUT_LOG_LINES = 200
        private const val MAX_TASK_ATTEMPTS = 5
        private const val RECONNECT_DELAY_MS = 2_000L
        private val RETRYABLE_ERRORS = setOf(AppError.NetworkUnavailable, AppError.NetworkTimeout)
    }
}
