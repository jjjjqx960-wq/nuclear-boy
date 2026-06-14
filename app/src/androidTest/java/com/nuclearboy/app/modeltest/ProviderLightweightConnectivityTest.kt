package com.nuclearboy.app.modeltest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nuclearboy.api.deepseek.ApiKeyManager
import com.nuclearboy.api.deepseek.DeepSeekApiClient
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
        val modelName = apiKeyManager.getModelOverride()
        assertTrue("轻量连通性测试需要当前选中自定义模型", apiKeyManager.isCustomProviderEnabled())
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
}
