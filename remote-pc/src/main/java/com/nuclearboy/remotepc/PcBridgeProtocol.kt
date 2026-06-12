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
        /** true 时在 cwd 仓库旁创建隔离 git worktree 执行，互不干扰 */
        val worktree: Boolean? = null,
        /** "ask" 时电脑端每个工具操作弹手机审批（仅 claude） */
        val approval: String? = null,
    )

    @Serializable
    data class CancelMessage(val type: String = "cancel", val id: String)

    @Serializable
    data class GetResultMessage(
        val type: String = "get_result",
        val id: String,
        /** 已收到的最大 seq+1，桥接据此只补发漏掉的输出（历史增量同步） */
        val sinceSeq: Int? = null,
    )

    @Serializable
    data class ListTasksMessage(val type: String = "list_tasks")

    @Serializable
    data class ListSessionsMessage(
        val type: String = "list_sessions",
        val id: String = "ls",
        val limit: Int = 20,
        val cwd: String? = null,
    )

    @Serializable
    data class PermissionResponseMessage(
        val type: String = "permission_response",
        val id: String,
        val approved: Boolean,
        val message: String = "",
    )

    @Serializable
    data class PingMessage(val type: String = "ping")

    @Serializable
    data class TermOpenMessage(
        val type: String = "term_open",
        val id: String,
        val cols: Int,
        val rows: Int,
        val cwd: String? = null,
        val cmd: String? = null,
    )

    @Serializable
    data class TermInputMessage(val type: String = "term_input", val id: String, val data: String)

    @Serializable
    data class TermResizeMessage(val type: String = "term_resize", val id: String, val cols: Int, val rows: Int)

    @Serializable
    data class TermCloseMessage(val type: String = "term_close", val id: String)

    @Serializable
    data class ListDirMessage(val type: String = "list_dir", val id: String, val path: String)

    @Serializable
    data class ReadFileMessage(
        val type: String = "read_file",
        val id: String,
        val path: String,
        val maxBytes: Int? = null,
    )

    @Serializable
    data class WriteFileMessage(
        val type: String = "write_file",
        val id: String,
        val path: String,
        val content: String,
        val append: Boolean? = null,
    )

    fun encodeAuth(token: String): String = json.encodeToString(AuthMessage(token = token))
    fun encodeRun(msg: RunMessage): String = json.encodeToString(msg)
    fun encodeCancel(id: String): String = json.encodeToString(CancelMessage(id = id))
    fun encodeGetResult(id: String, sinceSeq: Int? = null): String =
        json.encodeToString(GetResultMessage(id = id, sinceSeq = sinceSeq))
    fun encodeListTasks(): String = json.encodeToString(ListTasksMessage())
    fun encodeListSessions(limit: Int = 20, cwd: String? = null): String =
        json.encodeToString(ListSessionsMessage(limit = limit, cwd = cwd))
    fun encodePermissionResponse(id: String, approved: Boolean, message: String = ""): String =
        json.encodeToString(PermissionResponseMessage(id = id, approved = approved, message = message))
    fun encodePing(): String = json.encodeToString(PingMessage())
    fun encodeTermOpen(id: String, cols: Int, rows: Int, cwd: String? = null, cmd: String? = null): String =
        json.encodeToString(TermOpenMessage(id = id, cols = cols, rows = rows, cwd = cwd, cmd = cmd))
    fun encodeTermInput(id: String, data: String): String =
        json.encodeToString(TermInputMessage(id = id, data = data))
    fun encodeTermResize(id: String, cols: Int, rows: Int): String =
        json.encodeToString(TermResizeMessage(id = id, cols = cols, rows = rows))
    fun encodeTermClose(id: String): String = json.encodeToString(TermCloseMessage(id = id))
    fun encodeListDir(id: String, path: String): String =
        json.encodeToString(ListDirMessage(id = id, path = path))
    fun encodeReadFile(id: String, path: String, maxBytes: Int? = null): String =
        json.encodeToString(ReadFileMessage(id = id, path = path, maxBytes = maxBytes))
    fun encodeWriteFile(id: String, path: String, content: String, append: Boolean? = null): String =
        json.encodeToString(WriteFileMessage(id = id, path = path, content = content, append = append))

    // ── 入站消息 ─────────────────────────────────────

    sealed interface Inbound {
        data class AuthOk(val host: String, val clis: Map<String, String>) : Inbound
        data class AuthFail(val message: String) : Inbound
        data class Accepted(val id: String) : Inbound
        data class Output(val id: String, val kind: String, val text: String, val seq: Int = -1) : Inbound
        data class Done(
            val id: String,
            val exitCode: Int,
            val result: String,
            val durationMs: Long,
            val sessionId: String = "",
            val worktreePath: String = "",
            val worktreeBranch: String = "",
        ) : Inbound
        data class Error(val id: String, val message: String) : Inbound
        data object Pong : Inbound
        data class Cancelled(val id: String) : Inbound
        data class Tasks(val tasks: List<RunningTask>) : Inbound
        data class PermissionRequest(
            val id: String,
            val toolName: String,
            val inputSummary: String,
        ) : Inbound
        data class TermOutput(val id: String, val data: String) : Inbound
        data class TermExit(val id: String, val code: Int) : Inbound
        data class DirListing(
            val id: String,
            val path: String,
            val entries: List<DirEntry>,
            val truncated: Boolean,
        ) : Inbound
        data class FileContent(
            val id: String,
            val path: String,
            val content: String,
            val size: Long,
            val truncated: Boolean,
        ) : Inbound
        data class FileWritten(val id: String, val path: String, val bytes: Long) : Inbound
        data class SessionsList(val id: String, val sessions: List<SessionInfo>) : Inbound
        data class Unknown(val type: String) : Inbound
    }

    data class DirEntry(val name: String, val isDir: Boolean, val size: Long)
    data class SessionInfo(
        val sessionId: String,
        val cli: String,
        val cwd: String,
        val preview: String,
        val mtimeMs: Long,
    )

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
                seq = obj["seq"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1,
            )
            "done" -> Inbound.Done(
                id = obj.stringOrEmpty("id"),
                exitCode = obj["exitCode"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1,
                result = obj.stringOrEmpty("result"),
                durationMs = obj["durationMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                sessionId = obj.stringOrEmpty("sessionId"),
                worktreePath = obj.stringOrEmpty("worktreePath"),
                worktreeBranch = obj.stringOrEmpty("worktreeBranch"),
            )
            "error" -> Inbound.Error(
                id = obj.stringOrEmpty("id"),
                message = obj.stringOrEmpty("message"),
            )
            "pong" -> Inbound.Pong
            "cancelled" -> Inbound.Cancelled(obj.stringOrEmpty("id"))
            "term_output" -> Inbound.TermOutput(
                id = obj.stringOrEmpty("id"),
                data = obj.stringOrEmpty("data"),
            )
            "term_exit" -> Inbound.TermExit(
                id = obj.stringOrEmpty("id"),
                code = obj["code"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            )
            "dir_listing" -> Inbound.DirListing(
                id = obj.stringOrEmpty("id"),
                path = obj.stringOrEmpty("path"),
                entries = (obj["entries"] as? kotlinx.serialization.json.JsonArray)
                    ?.mapNotNull { el ->
                        (el as? JsonObject)?.let { en ->
                            DirEntry(
                                name = en.stringOrEmpty("name"),
                                isDir = en["isDir"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                                size = en["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                            )
                        }
                    } ?: emptyList(),
                truncated = obj["truncated"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
            )
            "file_content" -> Inbound.FileContent(
                id = obj.stringOrEmpty("id"),
                path = obj.stringOrEmpty("path"),
                content = obj.stringOrEmpty("content"),
                size = obj["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                truncated = obj["truncated"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
            )
            "file_written" -> Inbound.FileWritten(
                id = obj.stringOrEmpty("id"),
                path = obj.stringOrEmpty("path"),
                bytes = obj["bytes"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            )
            "sessions_list" -> Inbound.SessionsList(
                id = obj.stringOrEmpty("id"),
                sessions = (obj["sessions"] as? kotlinx.serialization.json.JsonArray)
                    ?.mapNotNull { el ->
                        (el as? JsonObject)?.let { s ->
                            SessionInfo(
                                sessionId = s.stringOrEmpty("sessionId"),
                                cli = s.stringOrEmpty("cli"),
                                cwd = s.stringOrEmpty("cwd"),
                                preview = s.stringOrEmpty("preview"),
                                mtimeMs = s["mtimeMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                            )
                        }
                    } ?: emptyList(),
            )
            "permission_request" -> Inbound.PermissionRequest(
                id = obj.stringOrEmpty("id"),
                toolName = obj.stringOrEmpty("toolName"),
                inputSummary = (obj["input"] as? JsonObject)?.let { inp ->
                    inp.entries.joinToString("，") { (k, v) ->
                        "$k=${v.toString().take(120)}"
                    }
                } ?: "",
            )
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
