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
    fun `failure action hint explains inactive provider recovery`() {
        val hint = modelTestFailureActionHint(
            "HTTP 404: No active credentials for provider: nvidia",
        )

        assertEquals(
            "操作建议：网关把模型名前缀 nvidia 当作上游 provider，但当前没有可用凭证。先点“获取模型列表”选择网关实际返回的模型名；若必须使用 nvidia，请在网关后台补齐对应上游 Key 或额度。",
            hint,
        )
    }

    @Test
    fun `failure action hint suggests model list for routing errors`() {
        val hint = modelTestFailureActionHint(
            """{"error":{"code":"model_not_found"}}""",
        )

        assertEquals(
            "操作建议：先点“获取模型列表”确认网关可用模型，并点选列表里的完整模型名；如果列表为空，请检查网关后台的模型映射和上游凭证。",
            hint,
        )
    }

    @Test
    fun `failure action hint explains auth and permission errors`() {
        assertEquals(
            "操作建议：核对这个 Key 是否属于当前服务地址；如果这是免鉴权本地网关，也可以清空 API Key 后重试。",
            modelTestFailureActionHint("HTTP 401: unauthorized"),
        )
        assertEquals(
            "操作建议：当前 Key 或上游账号没有访问该模型的权限，请换有权限的 Key，或改用模型列表中可用的模型名。",
            modelTestFailureActionHint("HTTP 403: forbidden"),
        )
    }

    @Test
    fun `failure action hint explains connectivity errors`() {
        val hint = modelTestFailureActionHint("failed to connect after timeout")

        assertEquals(
            "操作建议：检查手机网络、VPN、服务地址和端口是否可达；确认浏览器或抓包工具能访问后再重试测试。",
            hint,
        )
    }

    @Test
    fun `failure action hint is empty for generic errors`() {
        val hint = modelTestFailureActionHint("unexpected gateway body")

        assertEquals("", hint)
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

    @Test
    fun `model name cleanup summary ignores normal surrounding whitespace`() {
        val summary = modelNameCleanupSummary(
            rawModelName = " nvidia/minimaxai/minimax-m2.7 ",
            sanitizedModelName = "nvidia/minimaxai/minimax-m2.7",
        )

        assertEquals("", summary)
    }

    @Test
    fun `provider base url cleanup summary reports hidden character normalization`() {
        val summary = providerBaseUrlCleanupSummary(
            rawBaseUrl = "\u200Bhttp://154.12.90.249:20128/v1",
            sanitizedBaseUrl = "http://154.12.90.249:20128/v1",
        )

        assertEquals(
            "已自动清理服务地址中的隐藏字符；实际请求使用：http://154.12.90.249:20128/v1",
            summary,
        )
    }

    @Test
    fun `provider base url cleanup summary ignores normal surrounding whitespace`() {
        val summary = providerBaseUrlCleanupSummary(
            rawBaseUrl = " http://154.12.90.249:20128/v1 ",
            sanitizedBaseUrl = "http://154.12.90.249:20128/v1",
        )

        assertEquals("", summary)
    }

    @Test
    fun `provider endpoint preview summary includes protocol mode and post endpoint`() {
        val summary = providerEndpointPreviewSummary(
            protocolLabel = "OpenAI",
            endpointModeLabel = "智能拼接",
            endpoint = " http://154.12.90.249:20128/v1/chat/completions ",
        )

        assertEquals(
            "实际请求：OpenAI · 智能拼接\nPOST http://154.12.90.249:20128/v1/chat/completions",
            summary,
        )
    }

    @Test
    fun `provider endpoint preview summary is empty for blank endpoint`() {
        val summary = providerEndpointPreviewSummary(
            protocolLabel = "OpenAI",
            endpointModeLabel = "智能拼接",
            endpoint = " ",
        )

        assertEquals("", summary)
    }

    @Test
    fun `provider model list summary includes count endpoint samples and remaining count`() {
        val summary = providerModelListSummary(
            endpoint = " http://154.12.90.249:20128/v1/models ",
            modelIds = listOf("nvidia/minimaxai/minimax-m2.7", "gpt-4o", "claude-3-5-sonnet"),
            latencyMs = 123,
            sampleLimit = 2,
        )

        assertEquals(
            "模型列表：3 个 · 123 ms\n" +
                "GET http://154.12.90.249:20128/v1/models\n" +
                "- nvidia/minimaxai/minimax-m2.7\n" +
                "- gpt-4o\n" +
                "还有 1 个未显示",
            summary,
        )
    }

    @Test
    fun `provider model list summary ignores blank and duplicate models`() {
        val summary = providerModelListSummary(
            endpoint = "http://154.12.90.249:20128/v1/models",
            modelIds = listOf("gpt-4o", " ", "gpt-4o"),
            latencyMs = -1,
        )

        assertEquals(
            "模型列表：1 个 · 0 ms\n" +
                "GET http://154.12.90.249:20128/v1/models\n" +
                "- gpt-4o",
            summary,
        )
    }

    @Test
    fun `provider model list visible models filters by query before limiting`() {
        val visible = providerModelListVisibleModels(
            modelIds = listOf(
                "gpt-4o",
                "nvidia/minimaxai/minimax-m2.7",
                "minimax-chat",
                "claude-3-5-sonnet",
            ),
            query = "mini",
            limit = 1,
        )

        assertEquals(listOf("nvidia/minimaxai/minimax-m2.7"), visible)
    }

    @Test
    fun `provider model list visible models deduplicates and ignores blanks`() {
        val visible = providerModelListVisibleModels(
            modelIds = listOf("gpt-4o", " ", "gpt-4o", "claude-3-5-sonnet"),
            query = "",
            limit = 12,
        )

        assertEquals(listOf("gpt-4o", "claude-3-5-sonnet"), visible)
    }

    @Test
    fun `provider model list picker hint describes filtered state`() {
        assertEquals(
            "点选模型名填入输入框；当前显示 12 / 38 个",
            providerModelListPickerHint(totalCount = 38, visibleCount = 12, query = ""),
        )
        assertEquals(
            "已按“mini”过滤，显示 2 / 38 个匹配模型",
            providerModelListPickerHint(totalCount = 38, visibleCount = 2, query = " mini "),
        )
        assertEquals(
            "没有匹配“qwen”的模型名，可删减输入框关键词再试",
            providerModelListPickerHint(totalCount = 38, visibleCount = 0, query = "qwen"),
        )
    }

    @Test
    fun `provider model list clear filter action appears only when query hides all models`() {
        assertEquals(
            "查看全部模型",
            providerModelListClearFilterActionLabel(totalCount = 38, visibleCount = 0, query = " nvidia "),
        )
        assertEquals(
            "",
            providerModelListClearFilterActionLabel(totalCount = 38, visibleCount = 3, query = "mini"),
        )
        assertEquals(
            "",
            providerModelListClearFilterActionLabel(totalCount = 38, visibleCount = 12, query = ""),
        )
        assertEquals(
            "",
            providerModelListClearFilterActionLabel(totalCount = 0, visibleCount = 0, query = "mini"),
        )
    }

    @Test
    fun `provider exact endpoint warning flags openai root endpoint`() {
        val warning = providerExactEndpointWarning(
            protocolLabel = "OpenAI",
            endpoint = "http://154.12.90.249:20128/v1",
        )

        assertEquals(
            "完整地址模式会直接 POST 到此地址；当前不像完整 OpenAI 接口，建议填写 /v1/chat/completions 结尾的完整 URL，或切回智能拼接。",
            warning,
        )
    }

    @Test
    fun `provider exact endpoint warning is empty for openai chat completions endpoint`() {
        val warning = providerExactEndpointWarning(
            protocolLabel = "OpenAI",
            endpoint = "http://154.12.90.249:20128/v1/chat/completions",
        )

        assertEquals("", warning)
    }

    @Test
    fun `provider exact endpoint warning is empty for anthropic messages endpoint`() {
        val warning = providerExactEndpointWarning(
            protocolLabel = "Anthropic",
            endpoint = "https://api.example.com/v1/messages",
        )

        assertEquals("", warning)
    }

    @Test
    fun `provider exact endpoint recovery action appears only when warning exists`() {
        assertEquals(
            "切回智能拼接",
            providerExactEndpointRecoveryActionLabel("完整地址模式会直接 POST 到此地址"),
        )
        assertEquals("", providerExactEndpointRecoveryActionLabel(" "))
    }

    @Test
    fun `provider exact endpoint completion action appears only for different suggestion`() {
        assertEquals(
            "补成完整地址",
            providerExactEndpointCompletionActionLabel(
                warning = "完整地址模式会直接 POST 到此地址",
                currentEndpoint = "http://154.12.90.249:20128/v1",
                suggestedEndpoint = "http://154.12.90.249:20128/v1/chat/completions",
            ),
        )
        assertEquals(
            "",
            providerExactEndpointCompletionActionLabel(
                warning = "完整地址模式会直接 POST 到此地址",
                currentEndpoint = "http://154.12.90.249:20128/v1/chat/completions",
                suggestedEndpoint = " http://154.12.90.249:20128/v1/chat/completions ",
            ),
        )
        assertEquals(
            "",
            providerExactEndpointCompletionActionLabel(
                warning = " ",
                currentEndpoint = "http://154.12.90.249:20128/v1",
                suggestedEndpoint = "http://154.12.90.249:20128/v1/chat/completions",
            ),
        )
    }

    @Test
    fun `model test request context includes endpoint model protocol mode and fingerprint`() {
        val summary = modelTestRequestContextSummary(
            endpoint = " http://154.12.90.249:20128/v1/chat/completions ",
            modelName = " nvidia/minimaxai/minimax-m2.7 ",
            protocolLabel = " OpenAI ",
            endpointModeLabel = " 智能拼接 ",
            keyFingerprintSummary = " Key 指纹：sha256 ba7816bf8f01 · 3 位 ",
        )

        assertEquals(
            "请求上下文：\n" +
                "端点：http://154.12.90.249:20128/v1/chat/completions\n" +
                "模型：nvidia/minimaxai/minimax-m2.7\n" +
                "协议：OpenAI\n" +
                "地址模式：智能拼接\n" +
                "Key 指纹：sha256 ba7816bf8f01 · 3 位",
            summary,
        )
    }

    @Test
    fun `model test request context is empty for blank values`() {
        val summary = modelTestRequestContextSummary(
            endpoint = " ",
            modelName = "",
            protocolLabel = " ",
            endpointModeLabel = " ",
            keyFingerprintSummary = " ",
        )

        assertEquals("", summary)
    }

    @Test
    fun `model test copy summary includes status title and detail`() {
        val summary = modelTestCopySummary(
            inProgress = false,
            success = true,
            message = "模型连接成功",
            detail = "模型：nvidia/minimaxai/minimax-m2.7",
        )

        assertEquals(
            "第三方模型测试：成功\n标题：模型连接成功\n详情：模型：nvidia/minimaxai/minimax-m2.7",
            summary,
        )
    }

    @Test
    fun `model test copy summary redacts bearer and sk tokens`() {
        val summary = modelTestCopySummary(
            inProgress = false,
            success = false,
            message = "鉴权失败",
            detail = "Authorization: Bearer sk-test123456\napi_key=sk-another_secret_123",
        )

        assertEquals(
            "第三方模型测试：失败\n标题：鉴权失败\n详情：Authorization: Bearer <REDACTED_TOKEN>\napi_key=sk-<REDACTED_TOKEN>",
            summary,
        )
    }

    @Test
    fun `full diagnostics copy summary includes counts rows and details`() {
        val summary = fullDiagnosticsCopySummary(
            listOf(
                DiagnosticsCopyItem(
                    name = "模型配置状态",
                    status = "PASS",
                    message = "当前模型配置可解析",
                    durationMs = 1,
                    detail = "ok",
                ),
                DiagnosticsCopyItem(
                    name = "第三方模型连通性",
                    status = "FAIL",
                    message = "部分第三方模型不可用",
                    durationMs = 20095,
                    detail = "HTTP 401",
                ),
            ),
        )

        assertEquals(
            "全量自检：2 项，失败 1，警告 0\n" +
                "- PASS 模型配置状态：当前模型配置可解析（1 ms）\n" +
                "  ok\n" +
                "- FAIL 第三方模型连通性：部分第三方模型不可用（20095 ms）\n" +
                "  HTTP 401",
            summary,
        )
    }

    @Test
    fun `full diagnostics copy summary redacts secrets in details`() {
        val summary = fullDiagnosticsCopySummary(
            listOf(
                DiagnosticsCopyItem(
                    name = "第三方模型连通性",
                    status = "WARN",
                    message = "上游响应较慢",
                    durationMs = 1010,
                    detail = "Authorization: Bearer sk-test123456\napi_key=sk-another_secret_123",
                ),
            ),
        )

        assertEquals(
            "全量自检：1 项，失败 0，警告 1\n" +
                "- WARN 第三方模型连通性：上游响应较慢（1010 ms）\n" +
                "  Authorization: Bearer <REDACTED_TOKEN>\n" +
                "  api_key=sk-<REDACTED_TOKEN>",
            summary,
        )
    }

    @Test
    fun `full diagnostics copy summary is stable for empty results`() {
        val summary = fullDiagnosticsCopySummary(emptyList())

        assertEquals("全量自检：暂无结果", summary)
    }
}
