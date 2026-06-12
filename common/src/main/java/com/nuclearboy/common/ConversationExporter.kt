package com.nuclearboy.common

/**
 * 把对话导出成 Markdown 文本，便于分享/存档（借鉴 gpt_mobile/claudecodeui 的会话导出）。
 *
 * 纯函数、无 Android 依赖，方便单测。导出用户与助手消息，工具调用压成简短一行，
 * 跳过系统消息与空消息；助手的 reasoning（思考过程）默认不导出。
 */
object ConversationExporter {

    private const val TOOL_OUTPUT_PREVIEW = 200

    /**
     * @param title 文档标题（如项目名或"核弹男孩对话"）
     * @param messages 对话消息
     * @param dateLabel 由调用方传入的日期串（common 层不取系统时间，避免不可测）
     */
    fun toMarkdown(
        title: String,
        messages: List<ChatMessage>,
        dateLabel: String = "",
    ): String {
        val sb = StringBuilder()
        sb.append("# ").append(title.ifBlank { "核弹男孩对话" }).append("\n\n")
        if (dateLabel.isNotBlank()) sb.append("> 导出时间：").append(dateLabel).append("\n\n")

        var written = 0
        for (msg in messages) {
            if (msg.role == MessageRole.SYSTEM) continue
            val body = msg.content.trim()
            val hasTools = msg.toolCalls.isNotEmpty()
            if (body.isEmpty() && !hasTools) continue

            sb.append("## ").append(msg.role.displayName).append("\n\n")
            if (body.isNotEmpty()) sb.append(body).append("\n\n")

            if (hasTools) {
                sb.append("**工具调用：**\n\n")
                for (tc in msg.toolCalls) {
                    val out = tc.output?.trim().orEmpty()
                    val preview = if (out.length > TOOL_OUTPUT_PREVIEW)
                        out.take(TOOL_OUTPUT_PREVIEW) + "…" else out
                    sb.append("- `").append(tc.toolName).append("`")
                    if (preview.isNotEmpty()) {
                        sb.append(" → ").append(preview.replace("\n", " "))
                    }
                    sb.append("\n")
                }
                sb.append("\n")
            }
            written++
        }
        if (written == 0) sb.append("_（没有可导出的对话内容）_\n")
        return sb.toString().trimEnd() + "\n"
    }
}
