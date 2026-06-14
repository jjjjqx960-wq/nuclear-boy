package com.nuclearboy.ui.chat.parts

data class ToolActionDraftHint(
    val title: String,
    val summary: String,
    val semantics: String,
)

fun detectToolActionDraftHint(text: String): ToolActionDraftHint? {
    val compact = text.trim()
    if (compact.length < 4) return null

    val lower = compact.lowercase()
    val hasExplicitFileWrite = fileWriteMarkers.any { compact.contains(it) || lower.contains(it) }
    val hasExplicitFileRead = fileReadMarkers.any { compact.contains(it) || lower.contains(it) }
    val hasCommand = commandMarkers.any { lower.contains(it) }
    val hasRunAction = runMarkers.any { compact.contains(it) || lower.contains(it) }
    val hasToolObject = toolObjectMarkers.any { compact.contains(it) || lower.contains(it) }

    if (!hasExplicitFileWrite && !hasExplicitFileRead && !hasCommand && !(hasRunAction && hasToolObject)) {
        return null
    }

    val scope = when {
        hasExplicitFileWrite -> "写入/创建文件"
        hasExplicitFileRead -> "读取/查看文件"
        hasCommand -> "运行命令或脚本"
        else -> "执行/验证项目任务"
    }
    return ToolActionDraftHint(
        title = "可能需要工具能力",
        summary = "$scope 需要模型通过工具真实执行；若当前网关只支持聊天，发送后会只能给方案，不能真实落盘或运行。",
        semantics = "工具能力预警：当前草稿可能需要工具真实执行，请确认当前模型网关支持 tools 或 function call。",
    )
}

private val fileWriteMarkers = listOf(
    "写入",
    "写文件",
    "创建文件",
    "保存到",
    "落盘",
    "生成文件",
    "覆盖文件",
    "append",
    "write_file",
)

private val fileReadMarkers = listOf(
    "读取",
    "读文件",
    "查看文件",
    "打开文件",
    "列出目录",
    "list_directory",
    "read_file",
)

private val runMarkers = listOf(
    "运行",
    "执行",
    "编译",
    "安装",
    "测试",
    "验证",
    "抓包",
)

private val commandMarkers = listOf(
    " adb ",
    " adb.exe",
    " gradle",
    " gradlew",
    " python ",
    " powershell",
    " shell",
    " curl ",
    " npm ",
    " yarn ",
    " pnpm ",
    " ./",
    "run_python",
)

private val toolObjectMarkers = listOf(
    "文件",
    "目录",
    "路径",
    "项目",
    "仓库",
    "代码",
    "源码",
    "日志",
    "skill",
    "workspace",
    ".md",
    ".kt",
    ".java",
    ".py",
    ".json",
    ".gradle",
)
