package com.nuclearboy.app.ui.settings.parts

import com.nuclearboy.common.AppError

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
