package com.nuclearboy.ui.chat.parts

data class ToolLimitNotice(
    val title: String,
    val summary: String,
    val actions: List<String>,
    val diagnosticLabel: String = "tool.protocol",
    val verificationLabel: String = "正式聊天 / stream=true / 工具定义",
    val semantics: String,
)

fun detectToolLimitNotice(content: String): ToolLimitNotice? {
    val text = content.trim()
    if (text.isBlank()) return null

    val hasLimitMarker = TOOL_LIMIT_MARKERS.any { text.contains(it) }
    if (!hasLimitMarker) return null

    val needsExecution = TOOL_ACTION_MARKERS.any { text.contains(it) }
    val summary = if (needsExecution) {
        "模型连接正常，但当前网关没有可用的工具调用协议；本轮读写、运行或测试没有真实发生。"
    } else {
        "模型连接正常，但当前网关没有可用的工具调用协议；只能继续普通问答。"
    }
    val actions = if (needsExecution) {
        listOf(
            "切换到支持 tools/function_call 的网关或官方模型后重试",
            "继续问答可以保留当前模型，但不要把本轮当作已执行",
        )
    } else {
        listOf(
            "需要操作文件或运行测试时先切换工具可用模型",
            "只做说明、分析或改写时可以继续当前模型",
        )
    }

    return ToolLimitNotice(
        title = "工具受限",
        summary = summary,
        actions = actions,
        semantics = "工具受限提示：模型连接正常，但当前网关工具调用协议不可用，本轮未真实执行。",
    )
}

private val TOOL_LIMIT_MARKERS = listOf(
    "工具受限",
    "未真实执行",
    "第三方网关不支持工具调用协议",
    "没有可用工具调用协议",
    "不能调用 read_file",
    "伪造工具",
)

private val TOOL_ACTION_MARKERS = listOf(
    "读取",
    "写入",
    "运行",
    "测试",
    "验证",
    "read_file",
    "write_file",
    "run_python",
    "工具调用",
)
