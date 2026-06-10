package com.nuclearboy.api.deepseek

import kotlinx.serialization.Serializable

@Serializable
enum class ProviderEndpointMode(
    val displayName: String,
    val description: String,
) {
    AUTO("智能拼接", "按协议把服务根地址拼成标准接口路径"),
    EXACT("完整地址", "不改写地址，直接请求你填写的完整接口 URL");

    companion object {
        fun fromStored(value: String?): ProviderEndpointMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AUTO
    }
}
