package com.nuclearboy.api.deepseek

import kotlinx.serialization.Serializable

@Serializable
enum class ProviderProtocol(
    val displayName: String,
    val description: String,
) {
    AUTO("自动", "根据地址自动选择 OpenAI 或 Anthropic 协议"),
    OPENAI("OpenAI", "OpenAI Chat Completions 兼容协议"),
    ANTHROPIC("Anthropic", "Anthropic Messages 兼容协议");

    companion object {
        fun fromStored(value: String?): ProviderProtocol =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AUTO

        fun resolve(protocol: ProviderProtocol, baseUrl: String, modelName: String = ""): ProviderProtocol {
            if (protocol != AUTO) return protocol
            val lowerUrl = baseUrl.lowercase()
            return when {
                "api.anthropic.com" in lowerUrl -> ANTHROPIC
                "/anthropic" in lowerUrl -> ANTHROPIC
                lowerUrl.endsWith("/v1/messages") || lowerUrl.endsWith("/messages") -> ANTHROPIC
                else -> OPENAI
            }
        }
    }
}
