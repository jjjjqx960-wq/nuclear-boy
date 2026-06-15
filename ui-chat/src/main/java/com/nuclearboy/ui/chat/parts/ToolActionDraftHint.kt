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
    val hasApiObject = apiObjectMarkers.any { compact.contains(it) || lower.contains(it) }
    val hasApiMutation = apiMutationMarkers.any { compact.contains(it) || lower.contains(it) }
    val hasApiAction = apiActionMarkers.any { compact.contains(it) || lower.contains(it) } ||
        (hasApiObject && hasApiMutation)

    if (!hasExplicitFileWrite && !hasExplicitFileRead && !hasCommand && !hasApiAction && !(hasRunAction && hasToolObject)) {
        return null
    }

    val scope = when {
        hasExplicitFileWrite -> "写入/创建文件"
        hasExplicitFileRead -> "读取/查看文件"
        hasCommand -> "运行命令或脚本"
        hasApiAction -> "调用接口/API 或远程配置"
        else -> "执行/验证项目任务"
    }
    return ToolActionDraftHint(
        title = "可能需要工具能力",
        summary = "$scope 需要模型通过工具真实执行；若当前网关只支持聊天，发送后会只能给方案，不能真实落盘或运行。",
        semantics = "工具能力预警：当前草稿可能需要工具真实执行，请确认当前模型网关支持 tools 或 function call。",
    )
}

fun appendToolRealityGuard(text: String): String {
    val trimmed = text.trimEnd()
    if (trimmed.isBlank() || hasRealityGuard(trimmed)) return text
    return "$trimmed\n\n$TOOL_REALITY_GUARD"
}

fun buildToolActionEvidenceMessage(text: String): String? {
    val hint = detectToolActionDraftHint(text) ?: return null
    return "本轮工具能力提示：${hint.summary}\n回看时请以可见工具执行卡、文件变更卡或明确的“工具受限，未真实执行”为准；没有这些证据就不要当作已完成。"
}

fun buildToolActionModelGuard(text: String): String? {
    val hint = detectToolActionDraftHint(text) ?: return null
    return """
        【本轮工具真实性约束】
        用户这轮请求已命中工具型任务风险：${hint.summary}
        如果你没有通过真实工具调用拿到成功结果，不得声称已经读取、写入、运行、安装、测试或验证。
        没有工具结果时必须明确写：工具受限，未真实执行；然后给出下一步需要的真实操作。
        如果工具已成功执行，回复必须引用可见工具结果、文件变更或验证结果。
    """.trimIndent()
}

fun buildToolActionMissingEvidenceReview(
    userText: String,
    assistantText: String,
    hasVisibleToolEvidence: Boolean,
): String? {
    val hint = detectToolActionDraftHint(userText) ?: return null
    if (hasVisibleToolEvidence || assistantDeclaresToolLimitation(assistantText)) return null
    return "本轮结果复核：${hint.summary}\n未看到工具执行卡、文件变更卡，也未看到明确的“工具受限，未真实执行”说明；请先不要把本轮回复当作已完成结果。"
}

private fun hasRealityGuard(text: String): Boolean =
    realityGuardMarkers.any { text.contains(it, ignoreCase = true) }

private fun assistantDeclaresToolLimitation(text: String): Boolean =
    toolLimitationMarkers.any { text.contains(it, ignoreCase = true) }

const val TOOL_REALITY_GUARD: String =
    "如果当前没有真实工具调用能力，请明确回答：工具受限，未真实执行；不要编造已读取、已写入、已运行或已验证的结果。"

private val realityGuardMarkers = listOf(
    "工具受限，未真实执行",
    "不要编造已读取",
    "不要伪造成功",
    "禁止伪造",
)

private val toolLimitationMarkers = listOf(
    "工具受限",
    "未真实执行",
    "尚未执行",
    "没有真实工具",
    "无法调用工具",
    "不能真实",
)

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

private val apiObjectMarkers = listOf(
    "api",
    "接口",
    "endpoint",
    "webhook",
    "http",
    "https",
    "服务端",
    "服务器",
    "后台",
    "网关",
)

private val apiActionMarkers = listOf(
    "走api",
    "走 api",
    "调用api",
    "调用 api",
    "调用接口",
    "请求接口",
    "接口请求",
    "发请求",
    "发起请求",
    "post ",
    "put ",
    "patch ",
    "delete ",
    "curl ",
)

private val apiMutationMarkers = listOf(
    "加进去",
    "添加",
    "新增",
    "创建",
    "更新",
    "配置",
    "导入",
    "同步",
    "提交",
    "上传",
    "注册",
    "绑定",
    "切换",
    "删除",
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
