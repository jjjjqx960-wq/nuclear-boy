package com.nuclearboy.ui.chat

/**
 * 斜杠命令支持（类似 Claude Code / Codex 的 CLI 命令）。
 *
 * 在聊天输入框输入以 "/" 开头的内容即可触发，
 * 解析与分发在 [ChatViewModel.handleSlashCommand]。
 */
internal data class ParsedSlashCommand(
    val name: String,
    val args: String,
)

/**
 * 解析 "/cmd 参数..." 形式的输入。命令名统一转小写。
 */
internal fun parseSlashCommand(input: String): ParsedSlashCommand {
    val parts = input.removePrefix("/").trim().split(Regex("\\s+"), limit = 2)
    return ParsedSlashCommand(
        name = parts.getOrNull(0)?.lowercase() ?: "",
        args = parts.getOrNull(1)?.trim() ?: "",
    )
}

internal const val SLASH_HELP = """📖 可用命令：

/goal <目标>　设定会话目标，之后每轮对话都会朝它推进
/goal　　　　 查看当前目标
/goal clear　 清除目标

/loop [轮数] <任务>　循环模式：AI 自动多轮推进任务直到完成（默认 5 轮，最多 10 轮）

/compact　　 压缩对话历史为摘要，释放上下文空间
/rewind [n]　回退最近 n 轮对话（默认 1 轮）
/clear　　　  清空全部对话
/stop　　　　停止当前任务（含循环）

/model　　　　　 查看当前模型与可切换列表
/model <序号/名称>　切换模型（0 = DeepSeek 官方）
/help　　　　　　显示本帮助"""
