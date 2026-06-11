package com.nuclearboy.api.deepseek

internal fun buildProviderNotFoundHint(
    endpoint: String,
    body: String,
    modelName: String,
): String {
    val normalizedEndpoint = endpoint.trimEnd('/')
    val lowerEndpoint = normalizedEndpoint.lowercase()
    val lowerBody = body.lowercase()
    val isModelRoutingError = "model_not_found" in lowerBody ||
        "no active credentials" in lowerBody ||
        "model not found" in lowerBody ||
        "unknown model" in lowerBody ||
        "does not exist" in lowerBody
    val isStandardChatEndpoint = lowerEndpoint.endsWith("/v1/chat/completions")
    val modelHint = providerModelNameHint(modelName)

    return buildString {
        if (isModelRoutingError || isStandardChatEndpoint) {
            append(
                "接口路径可能正常，是网关在路由模型时报错：模型名不存在，或网关没有为该模型的上游 provider 配置可用凭证。\n" +
                    "请求 GET <服务地址>/v1/models（带你的 Key）确认可用模型名，并在网关侧检查对应 provider 的 API Key 是否已配置且有额度。"
            )
        } else {
            append("接口路径不存在。当前会请求 $endpoint；若你的网关提供完整接口地址，请在地址模式里选择“完整地址”。")
        }
        if (modelHint.isNotBlank()) {
            append('\n')
            append(modelHint)
        }
    }
}

internal fun isInactiveProviderCredentialError(body: String): Boolean =
    "no active credentials for provider" in body.lowercase()

internal fun providerModelNameHint(modelName: String): String {
    val normalized = modelName.trim()
    val lower = normalized.lowercase()
    return when {
        lower == "nvidia/minimaxai/minimax-m2.7" ->
            "NVIDIA 官方 OpenAI 兼容模型名通常是 minimaxai/minimax-m2.7；如果你的网关模型列表里没有 provider 前缀，请尝试去掉开头的 nvidia/。"
        lower.startsWith("nvidia/") ->
            "如果这个网关不是 provider/model 命名的聚合网关，NVIDIA 模型名可能不需要 nvidia/ 前缀；最终以 GET /v1/models 返回的模型名为准。"
        else -> ""
    }
}
