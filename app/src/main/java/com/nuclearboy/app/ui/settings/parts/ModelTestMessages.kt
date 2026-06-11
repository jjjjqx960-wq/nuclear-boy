package com.nuclearboy.app.ui.settings.parts

import com.nuclearboy.common.AppError
import java.security.MessageDigest

private val inactiveProviderPattern = Regex(
    pattern = """no active credentials for provider:\s*([A-Za-z0-9_.-]+)""",
    option = RegexOption.IGNORE_CASE,
)

internal fun modelTestFailureMessage(
    error: AppError,
    technicalDetail: String?,
): String {
    val detail = technicalDetail.orEmpty()
    val inactiveProvider = inactiveProviderPattern
        .find(detail)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }

    return when {
        inactiveProvider != null -> "网关缺少 $inactiveProvider 上游凭证"
        detail.contains("model_not_found", ignoreCase = true) ||
            detail.contains("model not found", ignoreCase = true) ||
            detail.contains("unknown model", ignoreCase = true) ||
            detail.contains("模型名不存在") -> "模型路由失败，请核对模型名"
        else -> error.humanMessage
    }
}

internal fun apiKeyFingerprintSummary(apiKey: String?): String {
    val normalized = apiKey.orEmpty().trim()
    if (normalized.isBlank()) return ""
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(normalized.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(12)
    return "Key 指纹：sha256 $digest · ${normalized.length} 位"
}

internal fun modelNameCleanupSummary(
    rawModelName: String,
    sanitizedModelName: String,
): String {
    val raw = rawModelName.trim()
    val sanitized = sanitizedModelName.trim()
    if (sanitized.isBlank() || raw == sanitized) return ""
    return "已自动清理模型名中的隐藏字符；实际请求使用：$sanitized"
}

internal fun providerEndpointPreviewSummary(
    protocolLabel: String,
    endpointModeLabel: String,
    endpoint: String,
): String {
    val normalizedEndpoint = endpoint.trim()
    if (normalizedEndpoint.isBlank()) return ""
    return "实际请求：${protocolLabel.trim()} · ${endpointModeLabel.trim()}\nPOST $normalizedEndpoint"
}

internal fun providerExactEndpointWarning(
    protocolLabel: String,
    endpoint: String,
): String {
    val normalizedEndpoint = endpoint.trim()
    if (normalizedEndpoint.isBlank()) return ""
    val normalizedProtocol = protocolLabel.trim().ifBlank { "当前协议" }
    val lowerEndpoint = normalizedEndpoint.lowercase()
    val isAnthropic = normalizedProtocol.equals("Anthropic", ignoreCase = true)
    val looksCompleteEndpoint = if (isAnthropic) {
        lowerEndpoint.endsWith("/messages")
    } else {
        lowerEndpoint.endsWith("/chat/completions")
    }
    if (looksCompleteEndpoint) return ""
    val expectedPath = if (isAnthropic) "/v1/messages" else "/v1/chat/completions"
    return "完整地址模式会直接 POST 到此地址；当前不像完整 $normalizedProtocol 接口，建议填写 $expectedPath 结尾的完整 URL，或切回智能拼接。"
}

internal fun modelTestRequestContextSummary(
    endpoint: String,
    modelName: String,
    protocolLabel: String,
    endpointModeLabel: String,
    keyFingerprintSummary: String,
): String {
    val normalizedEndpoint = endpoint.trim()
    val normalizedModel = modelName.trim()
    val normalizedProtocol = protocolLabel.trim()
    val normalizedEndpointMode = endpointModeLabel.trim()
    val normalizedFingerprint = keyFingerprintSummary.trim()
    if (
        normalizedEndpoint.isBlank() &&
        normalizedModel.isBlank() &&
        normalizedProtocol.isBlank() &&
        normalizedEndpointMode.isBlank() &&
        normalizedFingerprint.isBlank()
    ) {
        return ""
    }
    return buildString {
        append("请求上下文：")
        if (normalizedEndpoint.isNotBlank()) {
            append('\n')
            append("端点：")
            append(normalizedEndpoint)
        }
        if (normalizedModel.isNotBlank()) {
            append('\n')
            append("模型：")
            append(normalizedModel)
        }
        if (normalizedProtocol.isNotBlank()) {
            append('\n')
            append("协议：")
            append(normalizedProtocol)
        }
        if (normalizedEndpointMode.isNotBlank()) {
            append('\n')
            append("地址模式：")
            append(normalizedEndpointMode)
        }
        if (normalizedFingerprint.isNotBlank()) {
            append('\n')
            append(normalizedFingerprint)
        }
    }
}

internal fun modelTestCopySummary(
    inProgress: Boolean,
    success: Boolean?,
    message: String,
    detail: String,
): String {
    val status = when {
        inProgress -> "测试中"
        success == true -> "成功"
        success == false -> "失败"
        else -> "未完成"
    }
    val normalizedMessage = message.trim()
    val normalizedDetail = redactModelTestSecrets(detail).trim()
    return buildString {
        append("第三方模型测试：")
        append(status)
        if (normalizedMessage.isNotBlank()) {
            append('\n')
            append("标题：")
            append(normalizedMessage)
        }
        if (normalizedDetail.isNotBlank()) {
            append('\n')
            append("详情：")
            append(normalizedDetail)
        }
    }
}

internal data class DiagnosticsCopyItem(
    val name: String,
    val status: String,
    val message: String,
    val durationMs: Long,
    val detail: String,
)

internal fun fullDiagnosticsCopySummary(items: List<DiagnosticsCopyItem>): String {
    if (items.isEmpty()) return "全量自检：暂无结果"
    val failed = items.count { it.status.equals("FAIL", ignoreCase = true) }
    val warned = items.count { it.status.equals("WARN", ignoreCase = true) }
    return buildString {
        append("全量自检：")
        append(items.size)
        append(" 项，失败 ")
        append(failed)
        append("，警告 ")
        append(warned)
        items.forEach { item ->
            append('\n')
            append("- ")
            append(item.statusLabel())
            append(' ')
            append(item.name.trim())
            append("：")
            append(item.message.trim())
            append("（")
            append(item.durationMs)
            append(" ms）")
            val detail = redactSettingsCopySecrets(item.detail).trim()
            if (detail.isNotBlank()) {
                append('\n')
                append("  ")
                append(detail.replace("\n", "\n  "))
            }
        }
    }
}

private fun DiagnosticsCopyItem.statusLabel(): String =
    when {
        status.equals("PASS", ignoreCase = true) -> "PASS"
        status.equals("WARN", ignoreCase = true) -> "WARN"
        status.equals("FAIL", ignoreCase = true) -> "FAIL"
        else -> status.trim().ifBlank { "UNKNOWN" }
    }

private fun redactModelTestSecrets(raw: String): String = redactSettingsCopySecrets(raw)

private fun redactSettingsCopySecrets(raw: String): String =
    raw.replace(Regex("Bearer\\s+[A-Za-z0-9._~+/=-]+", RegexOption.IGNORE_CASE), "Bearer <REDACTED_TOKEN>")
        .replace(Regex("sk-[A-Za-z0-9_-]{6,}"), "sk-<REDACTED_TOKEN>")
