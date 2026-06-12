package com.nuclearboy.remotepc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 手机端与 nb-pc-bridge 之间的 WebSocket JSON 消息协议。
 *
 * 出站: auth / run / cancel / ping
 * 入站: auth_ok / auth_fail / accepted / output / done / error / pong
 */
object PcBridgeProtocol {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        // type 字段是带默认值的常量，必须编码进消息；null 可选字段不发
        encodeDefaults = true
        explicitNulls = false
    }

    // ── 出站消息 ─────────────────────────────────────

    @Serializable
    data class AuthMessage(val type: String = "auth", val token: String)

    @Serializable
    data class RunMessage(
        val type: String = "run",
        val id: String,
        val cli: String,
        val prompt: String,
        val cwd: String? = null,
        val timeoutSec: Int? = null,
        /** 续传已有 claude 会话（上次 Done.sessionId） */
        val sessionId: String? = null,
    )

    @Serializable
    data class CancelMessage(val type: String = "cancel", val id: String)

    @Serializable
    data class GetResultMessage(val type: String = "get_result", val id: String)

    @Serializable
    data class ListTasksMessage(val type: String = "list_tasks")

    @Serializable
    data class PingMessage(val type: String = "ping")

    fun encodeAuth(token: String): String = json.encodeToString(AuthMessage(token = token))
    fun encodeRun(msg: RunMessage): String = json.encodeToString(msg)
    fun encodeCancel(id: String): String = json.encodeToString(CancelMessage(id = id))
    fun encodeGetResult(id: String): String = json.encodeToString(GetResultMessage(id = id))
    fun encodeListTasks(): String = json.encodeToString(ListTasksMessage())
    fun encodePing(): String = json.encodeToString(PingMessage())

    // ── 入站消息 ─────────────────────────────────────

    sealed interface Inbound {
        data class AuthOk(val host: String, val clis: Map<String, String>) : Inbound
        data class AuthFail(val message: String) : Inbound
        data class Accepted(val id: String) : Inbound
        data class Output(val id: String, val kind: String, val text: String) : Inbound
        data class Done(
            val id: String,
            val exitCode: Int,
            val result: String,
            val durationMs: Long,
            val sessionId: String = "",
        ) : Inbound
        data class Error(val id: String, val message: String) : Inbound
        data object Pong : Inbound
        data class Cancelled(val id: String) : Inbound
        data class Tasks(val tasks: List<RunningTask>) : Inbound
        data class Unknown(val type: String) : Inbound
    }

    data class RunningTask(
        val id: String,
        val cli: String,
        val promptPreview: String,
        val cwd: String,
        val elapsedMs: Long,
    )

    /**
     * 解析服务端消息。非法 JSON 或缺 type 字段返回 null（调用方按协议错误处理）。
     */
    fun parseInbound(raw: String): Inbound? {
        val obj: JsonObject = try {
            json.parseToJsonElement(raw).jsonObject
        } catch (e: Exception) {
            return null
        }
        val type = obj["type"]?.jsonPrimitive?.content ?: return null
        return when (type) {
            "auth_ok" -> Inbound.AuthOk(
                host = obj.stringOrEmpty("host"),
                clis = (obj["clis"] as? JsonObject)?.mapValues { it.value.jsonPrimitive.content }
                    ?: emptyMap(),
            )
            "auth_fail" -> Inbound.AuthFail(obj.stringOrEmpty("message"))
            "accepted" -> Inbound.Accepted(obj.stringOrEmpty("id"))
            "output" -> Inbound.Output(
                id = obj.stringOrEmpty("id"),
                kind = obj.stringOrEmpty("kind"),
                text = obj.stringOrEmpty("text"),
            )
            "done" -> Inbound.Done(
                id = obj.stringOrEmpty("id"),
                exitCode = obj["exitCode"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1,
                result = obj.stringOrEmpty("result"),
                durationMs = obj["durationMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                sessionId = obj.stringOrEmpty("sessionId"),
            )
            "error" -> Inbound.Error(
                id = obj.stringOrEmpty("id"),
                message = obj.stringOrEmpty("message"),
            )
            "pong" -> Inbound.Pong
            "cancelled" -> Inbound.Cancelled(obj.stringOrEmpty("id"))
            "tasks" -> Inbound.Tasks(
                (obj["tasks"] as? kotlinx.serialization.json.JsonArray)
                    ?.mapNotNull { el ->
                        (el as? JsonObject)?.let { t ->
                            RunningTask(
                                id = t.stringOrEmpty("id"),
                                cli = t.stringOrEmpty("cli"),
                                promptPreview = t.stringOrEmpty("promptPreview"),
                                cwd = t.stringOrEmpty("cwd"),
                                elapsedMs = t["elapsedMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                            )
                        }
                    } ?: emptyList()
            )
            else -> Inbound.Unknown(type)
        }
    }

    private fun JsonObject.stringOrEmpty(key: String): String =
        this[key]?.jsonPrimitive?.content ?: ""
}
