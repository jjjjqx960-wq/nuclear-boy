package com.nuclearboy.app.ui.settings.parts

import com.nuclearboy.common.AppError
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelTestMessagesTest {
    @Test
    fun `failure message names inactive upstream provider`() {
        val message = modelTestFailureMessage(
            error = AppError.InvalidRequest,
            technicalDetail = "HTTP 404: No active credentials for provider: nvidia",
        )

        assertEquals("网关缺少 nvidia 上游凭证", message)
    }

    @Test
    fun `failure message identifies model routing errors`() {
        val message = modelTestFailureMessage(
            error = AppError.InvalidRequest,
            technicalDetail = """{"error":{"code":"model_not_found"}}""",
        )

        assertEquals("模型路由失败，请核对模型名", message)
    }

    @Test
    fun `failure message falls back to app error message`() {
        val message = modelTestFailureMessage(
            error = AppError.NetworkTimeout,
            technicalDetail = "timeout",
        )

        assertEquals(AppError.NetworkTimeout.humanMessage, message)
    }

    @Test
    fun `api key fingerprint summary uses sha256 prefix and length`() {
        val summary = apiKeyFingerprintSummary("abc")

        assertEquals("Key 指纹：sha256 ba7816bf8f01 · 3 位", summary)
    }

    @Test
    fun `api key fingerprint summary trims input before hashing`() {
        val summary = apiKeyFingerprintSummary(" abc ")

        assertEquals("Key 指纹：sha256 ba7816bf8f01 · 3 位", summary)
    }

    @Test
    fun `api key fingerprint summary is empty for blank input`() {
        val summary = apiKeyFingerprintSummary(" ")

        assertEquals("", summary)
    }

    @Test
    fun `model name cleanup summary reports hidden character normalization`() {
        val summary = modelNameCleanupSummary(
            rawModelName = "\u200Bnvidia/minimaxai/minimax-m2.7",
            sanitizedModelName = "nvidia/minimaxai/minimax-m2.7",
        )

        assertEquals(
            "已自动清理模型名中的隐藏字符；实际请求使用：nvidia/minimaxai/minimax-m2.7",
            summary,
        )
    }

    @Test
    fun `model name cleanup summary is empty when unchanged`() {
        val summary = modelNameCleanupSummary(
            rawModelName = "nvidia/minimaxai/minimax-m2.7",
            sanitizedModelName = "nvidia/minimaxai/minimax-m2.7",
        )

        assertEquals("", summary)
    }
}
