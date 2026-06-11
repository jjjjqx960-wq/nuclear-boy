package com.nuclearboy.app.ui.settings.parts

import com.nuclearboy.common.AppError
import java.net.URI
import java.security.MessageDigest

private val inactiveProviderPattern = Regex(
    pattern = """no active credentials for provider:\s*([A-Za-z0-9_.-]+)""",
    option = RegexOption.IGNORE_CASE,
)
private val providerPrefixPattern = Regex("[A-Za-z0-9_.-]+")

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

internal fun modelTestFailureActionHint(technicalDetail: String?): String {
    val detail = technicalDetail.orEmpty()
    val inactiveProvider = inactiveProviderPattern
        .find(detail)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }

    return when {
        inactiveProvider != null ->
            "操作建议：网关把模型名前缀 $inactiveProvider 当作上游 provider，但当前没有可用凭证。先点“获取模型列表”选择网关实际返回的模型名；若必须使用 $inactiveProvider，请在网关后台补齐对应上游 Key 或额度。"
        detail.contains("model_not_found", ignoreCase = true) ||
            detail.contains("model not found", ignoreCase = true) ||
            detail.contains("unknown model", ignoreCase = true) ||
            detail.contains("模型名不存在") ->
            "操作建议：先点“获取模型列表”确认网关可用模型，并点选列表里的完整模型名；如果列表为空，请检查网关后台的模型映射和上游凭证。"
        detail.contains("HTTP 401", ignoreCase = true) ||
            detail.contains("unauthorized", ignoreCase = true) ||
            detail.contains("invalid api key", ignoreCase = true) ->
            "操作建议：核对这个 Key 是否属于当前服务地址；如果这是免鉴权本地网关，也可以清空 API Key 后重试。"
        detail.contains("HTTP 403", ignoreCase = true) ||
            detail.contains("forbidden", ignoreCase = true) ||
            detail.contains("permission", ignoreCase = true) ->
            "操作建议：当前 Key 或上游账号没有访问该模型的权限，请换有权限的 Key，或改用模型列表中可用的模型名。"
        detail.contains("timeout", ignoreCase = true) ||
            detail.contains("timed out", ignoreCase = true) ||
            detail.contains("failed to connect", ignoreCase = true) ->
            "操作建议：检查手机网络、VPN、服务地址和端口是否可达；确认浏览器或抓包工具能访问后再重试测试。"
        else -> ""
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

internal fun providerDisplayNameSuggestion(
    baseUrl: String,
    modelName: String,
): String {
    val modelLabel = providerModelDisplayLabel(modelName)
    if (modelLabel.isBlank()) return ""
    val hostLabel = providerHostDisplayLabel(baseUrl)
    return listOf(hostLabel, modelLabel)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

internal fun providerModelRouteHint(
    modelName: String,
    modelIds: List<String>,
): String {
    val normalizedModel = modelName.trim()
    if (normalizedModel.isBlank()) return ""
    val slashIndex = normalizedModel.indexOf('/')
    if (slashIndex <= 0 || slashIndex == normalizedModel.lastIndex) return ""
    val providerPrefix = normalizedModel.substring(0, slashIndex).trim()
    if (!providerPrefix.matches(providerPrefixPattern)) return ""

    val normalizedModels = modelIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    val hasExactModel = normalizedModels.any { it == normalizedModel }
    return when {
        hasExactModel ->
            "模型列表已包含此完整模型名；如果仍 404，多半是网关上游 $providerPrefix 凭证或额度问题。"
        normalizedModels.isNotEmpty() ->
            "当前模型列表未包含此完整模型名；建议点选列表返回的模型，避免网关把 $providerPrefix 当作上游 provider 后报凭证缺失。"
        else ->
            "此模型名带 $providerPrefix 前缀；若测试返回 provider 凭证缺失，先获取模型列表并点选实际可用模型名。"
    }
}

internal fun providerModelRouteSuggestedModel(
    modelName: String,
    modelIds: List<String>,
): String {
    val normalizedModel = modelName.trim()
    val slashIndex = normalizedModel.indexOf('/')
    if (slashIndex <= 0 || slashIndex == normalizedModel.lastIndex) return ""
    val providerPrefix = normalizedModel.substring(0, slashIndex).trim()
    if (!providerPrefix.matches(providerPrefixPattern)) return ""

    val normalizedModels = modelIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    if (normalizedModels.any { it == normalizedModel }) return ""

    val suffix = normalizedModel.substring(slashIndex + 1).trim()
    val lastSegment = suffix.substringAfterLast('/').trim()
    return normalizedModels.firstOrNull { it.equals(suffix, ignoreCase = true) }
        ?: normalizedModels.firstOrNull { it.endsWith("/$suffix", ignoreCase = true) }
        ?: normalizedModels.firstOrNull { it.equals(lastSegment, ignoreCase = true) }
        ?: normalizedModels.firstOrNull { it.endsWith("/$lastSegment", ignoreCase = true) }
        ?: ""
}

internal fun providerBaseUrlCleanupSummary(
    rawBaseUrl: String,
    sanitizedBaseUrl: String,
): String {
    val raw = rawBaseUrl.trim()
    val sanitized = sanitizedBaseUrl.trim()
    if (sanitized.isBlank() || raw == sanitized) return ""
    return "已自动清理服务地址中的隐藏字符；实际请求使用：$sanitized"
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

internal fun providerRequestCurlTemplate(
    protocolLabel: String,
    endpoint: String,
    modelName: String,
    hasApiKey: Boolean,
): String {
    val normalizedEndpoint = endpoint.trim()
    val normalizedModel = modelName.trim()
    if (normalizedEndpoint.isBlank() || normalizedModel.isBlank()) return ""

    val isAnthropic = protocolLabel.trim().equals("Anthropic", ignoreCase = true)
    val body = if (isAnthropic) {
        """{"model":"${normalizedModel.jsonEscaped()}","max_tokens":8,"messages":[{"role":"user","content":"ping"}]}"""
    } else {
        """{"model":"${normalizedModel.jsonEscaped()}","messages":[{"role":"user","content":"ping"}],"temperature":0.0,"top_p":1.0,"max_tokens":8,"stream":false}"""
    }
    val headers = buildList {
        add("  -H ${"Accept: application/json".shellSingleQuoted()}")
        add("  -H ${"Content-Type: application/json; charset=utf-8".shellSingleQuoted()}")
        if (hasApiKey) {
            add("  -H ${"Authorization: Bearer <REDACTED_TOKEN>".shellSingleQuoted()}")
        }
    }

    return buildString {
        append("curl -X POST ")
        append(normalizedEndpoint.shellSingleQuoted())
        append(" \\\n")
        append(headers.joinToString(" \\\n"))
        append(" \\\n")
        append("  --data ")
        append(body.shellSingleQuoted())
    }
}

internal fun providerModelListCurlTemplate(
    endpoint: String,
    hasApiKey: Boolean,
): String {
    val normalizedEndpoint = endpoint.trim()
    if (normalizedEndpoint.isBlank()) return ""

    val headers = buildList {
        add("  -H ${"Accept: application/json".shellSingleQuoted()}")
        if (hasApiKey) {
            add("  -H ${"Authorization: Bearer <REDACTED_TOKEN>".shellSingleQuoted()}")
        }
    }

    return buildString {
        append("curl -X GET ")
        append(normalizedEndpoint.shellSingleQuoted())
        append(" \\\n")
        append(headers.joinToString(" \\\n"))
    }
}

internal fun providerModelListSummary(
    endpoint: String,
    modelIds: List<String>,
    latencyMs: Long,
    sampleLimit: Int = 12,
): String {
    val normalizedEndpoint = endpoint.trim()
    val normalizedModels = modelIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    if (normalizedEndpoint.isBlank() || normalizedModels.isEmpty()) return ""
    val limit = sampleLimit.coerceAtLeast(1)
    val sample = normalizedModels.take(limit).joinToString("\n") { "- $it" }
    val tail = if (normalizedModels.size > limit) "\n还有 ${normalizedModels.size - limit} 个未显示" else ""
    return "模型列表：${normalizedModels.size} 个 · ${latencyMs.coerceAtLeast(0)} ms\nGET $normalizedEndpoint\n$sample$tail"
}

internal fun providerModelListVisibleModels(
    modelIds: List<String>,
    query: String,
    limit: Int = 12,
): List<String> {
    val normalizedModels = modelIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    val normalizedQuery = query.trim()
    val filtered = if (normalizedQuery.isBlank()) {
        normalizedModels
    } else {
        normalizedModels.filter { it.contains(normalizedQuery, ignoreCase = true) }
    }
    return filtered.take(limit.coerceAtLeast(1))
}

internal fun providerModelListPickerHint(
    totalCount: Int,
    visibleCount: Int,
    query: String,
): String {
    val safeTotal = totalCount.coerceAtLeast(0)
    val safeVisible = visibleCount.coerceAtLeast(0)
    val normalizedQuery = query.trim()
    return when {
        safeTotal == 0 -> ""
        normalizedQuery.isBlank() -> "点选模型名填入输入框；当前显示 $safeVisible / $safeTotal 个"
        safeVisible == 0 -> "没有匹配“$normalizedQuery”的模型名，可删减输入框关键词再试"
        else -> "已按“$normalizedQuery”过滤，显示 $safeVisible / $safeTotal 个匹配模型"
    }
}

internal fun providerModelListClearFilterActionLabel(
    totalCount: Int,
    visibleCount: Int,
    query: String,
): String {
    val normalizedQuery = query.trim()
    return if (totalCount > 0 && visibleCount == 0 && normalizedQuery.isNotBlank()) {
        "查看全部模型"
    } else {
        ""
    }
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

internal fun providerExactEndpointRecoveryActionLabel(warning: String): String =
    if (warning.trim().isBlank()) "" else "切回智能拼接"

internal fun providerExactEndpointCompletionActionLabel(
    warning: String,
    currentEndpoint: String,
    suggestedEndpoint: String,
): String {
    val normalizedSuggested = suggestedEndpoint.trim()
    if (warning.trim().isBlank() || normalizedSuggested.isBlank()) return ""
    if (currentEndpoint.trim() == normalizedSuggested) return ""
    return "补成完整地址"
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

internal fun modelTestTroubleshootingBundle(
    testSummary: String,
    requestTemplate: String = "",
    modelListSummary: String = "",
    modelListRequestTemplate: String = "",
): String {
    val sections = mutableListOf<Pair<String, String>>()
    val seenBodies = mutableSetOf<String>()

    fun addSection(title: String, rawBody: String) {
        val body = redactSettingsCopySecrets(rawBody).trim()
        if (body.isBlank() || !seenBodies.add(body)) return
        sections += title to body
    }

    val normalizedTestSummary = redactSettingsCopySecrets(testSummary).trim()
    if (normalizedTestSummary.isNotBlank()) {
        seenBodies += normalizedTestSummary
    }
    addSection("POST 请求模板：", requestTemplate)
    addSection("模型列表摘要：", modelListSummary)
    addSection("模型列表请求模板：", modelListRequestTemplate)

    return buildString {
        if (normalizedTestSummary.isNotBlank()) {
            append(normalizedTestSummary)
        }
        sections.forEach { (title, body) ->
            if (isNotEmpty()) append("\n\n")
            append(title)
            append('\n')
            append(body)
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

private fun String.shellSingleQuoted(): String = "'${replace("'", "'\\''")}'"

private fun providerModelDisplayLabel(modelName: String): String =
    modelName.trim().substringAfterLast('/').trim()

private fun providerHostDisplayLabel(baseUrl: String): String {
    val normalized = baseUrl.trim().trimEnd('/')
    if (normalized.isBlank()) return ""
    val uriText = if (normalized.contains("://")) normalized else "https://$normalized"
    val host = runCatching { URI(uriText).host.orEmpty() }.getOrDefault("")
        .removePrefix("www.")
        .trim()
    if (host.isBlank()) return ""
    val isIpv4 = host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))
    return if (isIpv4) host else host.substringBefore('.')
}

private fun String.jsonEscaped(): String = buildString {
    this@jsonEscaped.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> {
                if (char.code < 0x20) {
                    append("\\u")
                    append(char.code.toString(16).padStart(4, '0'))
                } else {
                    append(char)
                }
            }
        }
    }
}
