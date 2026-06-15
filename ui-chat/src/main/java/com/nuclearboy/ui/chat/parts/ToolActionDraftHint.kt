package com.nuclearboy.ui.chat.parts

data class ToolActionDraftHint(
    val title: String,
    val summary: String,
    val semantics: String,
    val evidenceTargets: String = "工具执行卡、文件变更卡",
    val prohibitedActions: String = "读取、写入、运行、安装、测试或验证",
)

fun detectToolActionDraftHint(text: String): ToolActionDraftHint? {
    val compact = text.withoutToolRealityGuard().trim()
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

    if (shouldSkipApiLearningQuestion(compact, lower, hasApiObject, hasApiAction)) {
        return null
    }

    if (!hasExplicitFileWrite && !hasExplicitFileRead && !hasCommand && !hasApiAction && !(hasRunAction && hasToolObject)) {
        return null
    }

    val scope = when {
        hasExplicitFileWrite -> ToolActionScope(
            name = "写入/创建文件",
            unavailableOutcome = "不能真实落盘或写入",
            evidenceTargets = "工具执行卡、文件变更卡",
            prohibitedActions = "读取、写入、运行、安装、测试或验证",
        )
        hasExplicitFileRead -> ToolActionScope(
            name = "读取/查看文件",
            unavailableOutcome = "不能真实读取文件",
            evidenceTargets = "工具执行卡、文件读取记录或文件变更卡",
            prohibitedActions = "读取、写入、运行、安装、测试或验证",
        )
        hasCommand -> ToolActionScope(
            name = "运行命令或脚本",
            unavailableOutcome = "不能真实运行命令、脚本或验证结果",
            evidenceTargets = "工具执行卡、命令输出或文件变更卡",
            prohibitedActions = "读取、写入、运行、安装、测试或验证",
        )
        hasApiAction -> ToolActionScope(
            name = "调用接口/API 或远程配置",
            unavailableOutcome = "不能真实调用接口、提交请求或修改远程配置",
            evidenceTargets = "工具执行卡、接口/API 调用记录、远程配置变更记录或文件变更卡",
            prohibitedActions = "读取、写入、运行、安装、测试、验证、调用接口、提交请求或修改远程配置",
        )
        else -> ToolActionScope(
            name = "执行/验证项目任务",
            unavailableOutcome = "不能真实执行或验证",
            evidenceTargets = "工具执行卡、文件变更卡或验证结果",
            prohibitedActions = "读取、写入、运行、安装、测试或验证",
        )
    }
    return ToolActionDraftHint(
        title = "可能需要工具能力",
        summary = "${scope.name} 需要模型通过工具真实执行；若当前网关只支持聊天，发送后会只能给方案，${scope.unavailableOutcome}。",
        semantics = "工具能力预警：当前草稿可能需要工具真实执行，请确认当前模型网关支持 tools 或 function call。",
        evidenceTargets = scope.evidenceTargets,
        prohibitedActions = scope.prohibitedActions,
    )
}

fun appendToolRealityGuard(text: String): String {
    val trimmed = text.trimEnd()
    if (trimmed.isBlank() || hasRealityGuard(trimmed)) return text
    return "$trimmed\n\n$TOOL_REALITY_GUARD"
}

fun buildToolActionEvidenceMessage(text: String): String? {
    val hint = detectToolActionDraftHint(text) ?: return null
    return "本轮工具能力提示：${hint.summary}\n回看时请以可见${hint.evidenceTargets}或明确的“工具受限，未真实执行”为准；没有这些证据就不要当作已完成。"
}

fun buildToolActionModelGuard(text: String): String? {
    val hint = detectToolActionDraftHint(text) ?: return null
    return """
        【本轮工具真实性约束】
        用户这轮请求已命中工具型任务风险：${hint.summary}
        如果你没有通过真实工具调用拿到成功结果，不得声称已经${hint.prohibitedActions}。
        没有工具结果时必须明确写：工具受限，未真实执行；然后给出下一步需要的真实操作。
        如果工具已成功执行，回复必须引用可见${hint.evidenceTargets}。
    """.trimIndent()
}

fun buildToolActionMissingEvidenceReview(
    userText: String,
    assistantText: String,
    hasVisibleToolEvidence: Boolean,
): String? {
    val hint = detectToolActionDraftHint(userText) ?: return null
    if (hasVisibleToolEvidence || assistantDeclaresToolLimitation(assistantText)) return null
    return "本轮结果复核：${hint.summary}\n未看到${hint.evidenceTargets}，也未看到明确的“工具受限，未真实执行”说明；请先不要把本轮回复当作已完成结果。"
}

private data class ToolActionScope(
    val name: String,
    val unavailableOutcome: String,
    val evidenceTargets: String,
    val prohibitedActions: String,
)

private fun hasRealityGuard(text: String): Boolean =
    realityGuardMarkers.any { text.contains(it, ignoreCase = true) }

private fun String.withoutToolRealityGuard(): String =
    replace(TOOL_REALITY_GUARD, "", ignoreCase = true)

private fun assistantDeclaresToolLimitation(text: String): Boolean =
    toolLimitationMarkers.any { text.contains(it, ignoreCase = true) }

private fun shouldSkipApiLearningQuestion(
    compact: String,
    lower: String,
    hasApiObject: Boolean,
    hasApiAction: Boolean,
): Boolean {
    if (!hasApiObject && !hasApiAction) return false
    val hasLearningQuestion = apiLearningQuestionMarkers.any { compact.contains(it) || lower.contains(it) }
    if (!hasLearningQuestion) return false
    val asksForExplanation = apiExplanationMarkers.any { compact.contains(it) || lower.contains(it) }
    val hasExecutionIntent = apiExecutionIntentMarkers.any { compact.contains(it) || lower.contains(it) }
    return asksForExplanation || !hasExecutionIntent
}

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
    "provider",
    "模型配置",
    "模型路由",
    "默认模型",
    "路由",
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
    "加上",
    "加到",
    "加一下",
    "加入",
    "添加",
    "新增",
    "接入",
    "接上",
    "接到",
    "创建",
    "更新",
    "改成",
    "改为",
    "配置",
    "配上",
    "设为",
    "设成",
    "设置为",
    "设置成",
    "设默认",
    "设成默认",
    "设为默认",
    "填到",
    "填进",
    "填上",
    "放到",
    "放进",
    "部署到",
    "发布到",
    "上线到",
    "挂到",
    "挂上",
    "挂进",
    "换成",
    "换为",
    "替换成",
    "替换为",
    "开通",
    "启用",
    "录入",
    "导入",
    "同步",
    "提交",
    "上传",
    "注册",
    "绑定",
    "切换",
    "删除",
)

private val apiLearningQuestionMarkers = listOf(
    "怎么",
    "如何",
    "怎样",
    "什么是",
    "是什么",
    "用法",
    "教程",
    "示例",
    "文档",
)

private val apiExplanationMarkers = listOf(
    "请问",
    "告诉我",
    "讲讲",
    "解释",
    "说明",
    "介绍",
    "用法",
    "教程",
    "示例",
    "文档",
    "参数",
    "请求体",
    "header",
    "headers",
    "鉴权",
    "字段",
    "返回值",
)

private val apiExecutionIntentMarkers = listOf(
    "帮我",
    "给我",
    "替我",
    "请调用",
    "请把",
    "直接",
    "马上",
    "现在",
    "走api",
    "走 api",
    "加进去",
    "加上",
    "加到",
    "加一下",
    "加入",
    "放到",
    "放进",
    "挂到",
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
