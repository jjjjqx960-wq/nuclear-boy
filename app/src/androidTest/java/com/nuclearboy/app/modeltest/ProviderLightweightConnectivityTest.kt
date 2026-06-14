package com.nuclearboy.app.modeltest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nuclearboy.api.deepseek.ApiKeyManager
import com.nuclearboy.api.deepseek.DeepSeekApiClient
import com.nuclearboy.api.deepseek.ProviderEndpointMode
import com.nuclearboy.common.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProviderLightweightConnectivityTest {

    @Test
    fun activeCustomProviderRespondsToLightweightPing() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val apiKeyManager = ApiKeyManager(context)
        configureProviderFromInstrumentationArgs(apiKeyManager)
        val modelName = apiKeyManager.getModelOverride()
        assertTrue(
            "轻量连通性测试需要当前选中自定义模型，或通过 nbBaseUrl/nbModel instrumentation 参数注入",
            apiKeyManager.isCustomProviderEnabled(),
        )
        assertFalse("自定义模型名不能为空", modelName.isNullOrBlank())

        val apiClient = DeepSeekApiClient(
            apiKeyProvider = { apiKeyManager.getActiveKey() },
            baseUrlProvider = { apiKeyManager.getActiveBaseUrl() },
            modelOverrideProvider = { apiKeyManager.getModelOverride() },
            providerProtocolProvider = { apiKeyManager.getActiveProviderProtocol() },
            providerEndpointModeProvider = { apiKeyManager.getActiveProviderEndpointMode() },
        )

        try {
            when (val result = apiClient.testCustomProvider(
                baseUrl = apiKeyManager.getActiveBaseUrl(),
                modelName = modelName.orEmpty(),
                apiKey = apiKeyManager.getActiveKey(),
                protocol = apiKeyManager.getActiveProviderProtocol(),
                endpointMode = apiKeyManager.getActiveProviderEndpointMode(),
            )) {
                is AppResult.Success -> {
                    assertTrue("轻量 ping 应返回非空模型名", result.data.modelName.isNotBlank())
                }
                is AppResult.Failure -> fail(
                    "轻量 ping 不应失败：${result.error} ${result.technicalDetail?.take(240).orEmpty()}",
                )
            }
        } finally {
            apiClient.close()
        }
    }

    private fun configureProviderFromInstrumentationArgs(apiKeyManager: ApiKeyManager) {
        val args = InstrumentationRegistry.getArguments()
        val baseUrl = args.getString("nbBaseUrl")?.trim().orEmpty()
        val model = args.getString("nbModel")?.trim().orEmpty()
        if (baseUrl.isBlank() || model.isBlank()) return

        apiKeyManager.setCustomProviderConfig(
            baseUrl = baseUrl,
            modelName = model,
            apiKey = args.getString("nbApiKey")?.trim(),
            endpointMode = parseEndpointMode(args.getString("nbEndpointMode")),
        )
    }

    private fun parseEndpointMode(raw: String?): ProviderEndpointMode =
        when (raw.orEmpty().trim().lowercase()) {
            "exact", "full", "完整地址" -> ProviderEndpointMode.EXACT
            else -> ProviderEndpointMode.AUTO
        }
}
