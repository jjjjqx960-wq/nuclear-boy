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

    val hasLimitMarker = text.containsAnyToolMarker(TOOL_LIMIT_MARKERS)
    if (!hasLimitMarker) return null

    val needsExecution = text.containsAnyToolMarker(TOOL_ACTION_MARKERS)
    val summary = if (needsExecution) {
        "模型连接正常，但当前网关没有可用的工具调用协议或外部操作能力；本轮访问网站、服务器、ADB/SSH、读写、运行或测试没有真实发生。"
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
        semantics = "工具受限提示：模型连接正常，但当前网关工具调用协议或外部操作能力不可用，本轮未真实执行。",
    )
}

private val TOOL_LIMIT_MARKERS = listOf(
    "工具受限",
    "未真实执行",
    "第三方网关不支持工具调用协议",
    "没有可用工具调用协议",
    "不能调用 read_file",
    "伪造工具",
    "tool calls are not supported",
    "tools are not supported",
    "tool use is disabled",
    "tools disabled",
    "function_call unsupported",
    "function calling is not supported",
    "does not support tools",
    "no available tool",
    "no tool calls",
    "can't access local files",
    "cannot access local files",
    "can't access files",
    "cannot access files",
    "don't have access to files",
    "do not have access to files",
    "no access to the filesystem",
    "no access to file system",
    "can't run commands",
    "cannot run commands",
    "can't execute commands",
    "cannot execute commands",
    "can't execute code",
    "cannot execute code",
    "can't browse the internet",
    "cannot browse the internet",
    "can't browse the web",
    "cannot browse the web",
    "can't access the internet",
    "cannot access the internet",
    "can't access external websites",
    "cannot access external websites",
    "can't access github",
    "cannot access github",
    "no internet access",
    "no browser access",
    "can't connect to your server",
    "cannot connect to your server",
    "can't use ssh",
    "cannot use ssh",
    "can't ssh",
    "cannot ssh",
    "can't use adb",
    "cannot use adb",
    "无法访问文件",
    "不能访问文件",
    "无法运行命令",
    "不能运行命令",
    "无法执行代码",
    "不能执行代码",
    "无法联网",
    "无法访问互联网",
    "不能访问互联网",
    "无法浏览网页",
    "不能浏览网页",
    "无法访问网站",
    "不能访问网站",
    "无法访问外部网站",
    "无法连接服务器",
    "不能连接服务器",
    "无法使用 ssh",
    "不能使用 ssh",
    "无法使用 adb",
    "不能使用 adb",
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
    "read",
    "write",
    "run",
    "execute",
    "test",
    "validate",
    "function_call",
    "access",
    "command",
    "commands",
    "file",
    "files",
    "filesystem",
    "file system",
    "browse",
    "internet",
    "network",
    "web",
    "website",
    "url",
    "github",
    "server",
    "ssh",
    "adb",
    "device",
    "android",
    "联网",
    "互联网",
    "网络",
    "网页",
    "网站",
    "接口",
    "github",
    "服务器",
    "手机",
    "设备",
)

private fun String.containsAnyToolMarker(markers: List<String>): Boolean =
    markers.any { contains(it, ignoreCase = true) }
