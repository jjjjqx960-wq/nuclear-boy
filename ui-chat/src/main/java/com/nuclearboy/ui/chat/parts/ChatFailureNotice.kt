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

    if (authFailureMarkers.any { text.contains(it, ignoreCase = true) }) {
        return ChatFailureNotice(
            title = "鉴权失败",
            summary = "当前 Key 没有通过服务端验证；本轮正式聊天没有完成。",
            actions = listOf(
                "核对 API Key 是否属于当前服务地址，注意不要把其他网关的 Key 填到这里",
                "如果这是免鉴权本地网关，可以清空 API Key 后重新测试正式聊天",
            ),
            semantics = "鉴权失败提示：API Key 未通过验证，正式聊天未完成。",
        )
    }

    if (permissionFailureMarkers.any { text.contains(it, ignoreCase = true) }) {
        return ChatFailureNotice(
            title = "模型权限不足",
            summary = "当前 Key 或上游账号没有权限访问所选模型；本轮正式聊天没有完成。",
            actions = listOf(
                "换用有该模型权限的 Key，或点选模型列表中当前账号可用的模型",
                "如果网关有上游账号池，检查对应 provider 的权限和额度",
            ),
            semantics = "模型权限不足提示：当前账号无法访问所选模型，正式聊天未完成。",
        )
    }

    if (quotaFailureMarkers.any { text.contains(it, ignoreCase = true) }) {
        return ChatFailureNotice(
            title = "额度或限流不足",
            summary = "当前 Key、上游账号或网关触发额度不足/限流；本轮正式聊天没有完成。",
            actions = listOf(
                "检查网关和上游账号余额、额度、并发数或 RPM/TPM 限制",
                "稍后重试，或切换到有可用额度的模型/Key",
            ),
            semantics = "额度或限流不足提示：请求被额度或限流拦截，正式聊天未完成。",
        )
    }

    if (networkFailureMarkers.any { text.contains(it, ignoreCase = true) }) {
        return ChatFailureNotice(
            title = "网络连接失败",
            summary = "App 没有稳定连到模型服务；本轮正式聊天没有完成。",
            actions = listOf(
                "检查手机网络、VPN、服务地址、端口和网关进程是否可达",
                "在设置页重新运行轻量连通和正式聊天测试，确认 stream=true 能持续返回",
            ),
            semantics = "网络连接失败提示：模型服务不可达或超时，正式聊天未完成。",
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

private val authFailureMarkers = listOf(
    "HTTP 401",
    "unauthorized",
    "invalid api key",
    "invalid_api_key",
    "incorrect api key",
    "authentication",
    "鉴权失败",
    "认证失败",
)

private val permissionFailureMarkers = listOf(
    "HTTP 403",
    "forbidden",
    "permission denied",
    "insufficient permissions",
    "权限不足",
    "没有权限",
)

private val quotaFailureMarkers = listOf(
    "HTTP 429",
    "rate limit",
    "rate_limit",
    "too many requests",
    "quota",
    "insufficient_quota",
    "余额不足",
    "额度不足",
    "限流",
)

private val networkFailureMarkers = listOf(
    "timeout",
    "timed out",
    "failed to connect",
    "connection refused",
    "connection reset",
    "sockettimeout",
    "unknownhost",
    "无法连接",
    "连接超时",
)

private val formatFailureMarkers = listOf(
    "请求格式有误",
    "内部数据可能不一致",
    "没能生成回复",
    "空回复",
)
