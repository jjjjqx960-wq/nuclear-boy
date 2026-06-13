package com.nuclearboy.remotepc

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 远程终端会话：管理一条到 nb-pc-bridge 的长连接，开一个 ConPTY 终端，
 * 把输出以 [TerminalEvent] 流出，接受键入/改大小/关闭。
 *
 * 与 [PcBridgeClient.runCliTask] 的请求-响应不同，终端是交互式长连接：
 * 连接断开即终端结束（电脑侧也会关掉对应 ConPTY）。
 */
class PcTerminalSession(
    private val url: String,
    private val token: String,
    private val cwd: String? = null,
    private val cmd: String? = null,
    private val cols: Int = 80,
    private val rows: Int = 24,
    private val encrypted: Boolean = false,
) {
    private val termId: String = UUID.randomUUID().toString().replace("-", "")
    private val cryptoKey: ByteArray? =
        if (encrypted && token.isNotBlank()) PcCrypto.deriveKey(token) else null

    private fun send(ws: WebSocket, message: String) {
        ws.send(if (cryptoKey != null) PcCrypto.envelope(cryptoKey, message) else message)
    }

    private fun plainOf(text: String): String? {
        val key = cryptoKey ?: return text
        val enc = PcBridgeProtocol.extractEnc(text) ?: return null
        return runCatching { PcCrypto.decrypt(key, enc) }.getOrNull()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var webSocket: WebSocket? = null

    sealed interface TerminalEvent {
        /** 终端已就绪可以接收键入 */
        data object Ready : TerminalEvent
        data class Output(val data: String) : TerminalEvent
        data class Exit(val code: Int) : TerminalEvent
        data class Failed(val message: String) : TerminalEvent
    }

    /**
     * 建立会话并以冷流返回终端事件。collect 开始时连接、取消时关闭。
     * 鉴权成功后自动发送 term_open。
     */
    fun connect(): Flow<TerminalEvent> = callbackFlow {
        val request = Request.Builder().url(url.toHttpWsUrl()).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                if (cryptoKey != null) send(ws, """{"type":"auth"}""")
                else ws.send(PcBridgeProtocol.encodeAuth(token))
            }

            override fun onMessage(ws: WebSocket, text: String) {
                val plain = plainOf(text) ?: return
                when (val msg = PcBridgeProtocol.parseInbound(plain)) {
                    is PcBridgeProtocol.Inbound.AuthOk ->
                        send(ws, PcBridgeProtocol.encodeTermOpen(termId, cols, rows, cwd, cmd))
                    is PcBridgeProtocol.Inbound.AuthFail -> {
                        trySend(TerminalEvent.Failed("电脑拒绝连接：${msg.message}"))
                        close()
                    }
                    is PcBridgeProtocol.Inbound.Accepted ->
                        if (msg.id == termId) trySend(TerminalEvent.Ready)
                    is PcBridgeProtocol.Inbound.TermOutput ->
                        if (msg.id == termId) trySend(TerminalEvent.Output(msg.data))
                    is PcBridgeProtocol.Inbound.TermExit ->
                        if (msg.id == termId) {
                            trySend(TerminalEvent.Exit(msg.code))
                            close()
                        }
                    is PcBridgeProtocol.Inbound.Error ->
                        if (msg.id == termId || msg.id.isEmpty()) {
                            trySend(TerminalEvent.Failed(msg.message))
                            close()
                        }
                    else -> Unit
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                trySend(TerminalEvent.Failed(t.message ?: "连接异常"))
                close()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                trySend(TerminalEvent.Failed(PcBridgeClient.friendlyCloseMessage(code, reason)))
                close()
            }
        }
        webSocket = httpClient.newWebSocket(request, listener)

        awaitClose {
            runCatching { webSocket?.let { send(it, PcBridgeProtocol.encodeTermClose(termId)) } }
            webSocket?.close(1000, "done")
            webSocket = null
        }
    }

    /** 发送键入（含控制字符，如回车 \r、Ctrl-C ）。 */
    fun input(data: String) {
        webSocket?.let { send(it, PcBridgeProtocol.encodeTermInput(termId, data)) }
    }

    /** 终端窗口大小变化时同步给电脑端，保证排版正确。 */
    fun resize(cols: Int, rows: Int) {
        webSocket?.let { send(it, PcBridgeProtocol.encodeTermResize(termId, cols, rows)) }
    }

    fun close() {
        runCatching { webSocket?.let { send(it, PcBridgeProtocol.encodeTermClose(termId)) } }
        webSocket?.close(1000, "done")
        webSocket = null
    }

    /** OkHttp 要求 http/https scheme，把 ws:// 映射成 http://（同 PcBridgeClient）。 */
    private fun String.toHttpWsUrl(): String = when {
        startsWith("ws://") -> "http://" + removePrefix("ws://")
        startsWith("wss://") -> "https://" + removePrefix("wss://")
        startsWith("http://") || startsWith("https://") -> this
        else -> "http://$this"
    }
}
