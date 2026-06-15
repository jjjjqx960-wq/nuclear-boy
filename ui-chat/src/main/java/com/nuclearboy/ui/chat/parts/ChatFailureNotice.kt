package com.nuclearboy.ui.chat.parts

data class ChatFailureNotice(
    val title: String,
    val summary: String,
    val actions: List<String>,
    val semantics: String,
)

fun detectChatFailureNotice(content: String): ChatFailureNotice? {
    val text = content.trim()
    if (text.isBlank()) return null

    inactiveProviderPattern.find(text)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { provider ->
        return ChatFailureNotice(
            title = "模型路由失败",
            summary = "网关把模型前缀 $provider 当作上游 provider，但当前没有可用凭证或额度；测试 ping 可能通过，正式聊天仍会失败。",
            actions = listOf(
                "到设置页获取模型列表，点选网关实际返回的完整模型名",
                "如果必须使用 $provider 前缀，请在网关后台补齐该 provider 的 API Key 或额度",
            ),
            semantics = "模型路由失败提示：网关缺少 $provider 上游凭证，正式聊天未完成。",
        )
    }

    if (modelRoutingMarkers.any { text.contains(it, ignoreCase = true) }) {
        return ChatFailureNotice(
            title = "模型路由失败",
            summary = "当前网关没有找到这个模型，或没有为它配置可用上游路由；本轮正式聊天没有完成。",
            actions = listOf(
                "先获取模型列表并点选列表里的模型名",
                "如果列表为空，检查网关后台模型映射、上游 Key 和额度",
            ),
            semantics = "模型路由失败提示：模型名或上游路由不可用，正式聊天未完成。",
        )
    }

    if (formatFailureMarkers.any { text.contains(it, ignoreCase = true) }) {
        return ChatFailureNotice(
            title = "聊天链路失败",
            summary = "正式聊天没有生成有效回复；请不要把本轮当作完成结果。",
            actions = listOf(
                "先重启对话或新建对话后重试，清掉可能损坏的上下文",
                "若刚切换第三方模型，回设置页运行正式聊天测试并确认支持 stream=true 和工具定义",
            ),
            semantics = "聊天链路失败提示：正式聊天没有生成有效回复，请重启对话或重新测试模型。",
        )
    }

    return null
}

private val inactiveProviderPattern = Regex(
    pattern = """no active credentials for provider:\s*([A-Za-z0-9_.-]+)""",
    option = RegexOption.IGNORE_CASE,
)

private val modelRoutingMarkers = listOf(
    "model_not_found",
    "model not found",
    "unknown model",
    "模型名不存在",
    "模型路由失败",
)

private val formatFailureMarkers = listOf(
    "请求格式有误",
    "内部数据可能不一致",
    "没能生成回复",
    "空回复",
)
