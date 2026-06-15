package com.nuclearboy.app.diagnostics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import com.nuclearboy.api.deepseek.ApiKeyManager
import com.nuclearboy.api.deepseek.ProviderEndpointMode
import com.nuclearboy.api.deepseek.ProviderProtocol
import com.nuclearboy.app.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DebugModelConfigReceiver : BroadcastReceiver() {

    @Inject lateinit var apiKeyManager: ApiKeyManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SAVE_CUSTOM_MODEL) return
        if (!BuildConfig.DEBUG) {
            Log.e(TAG, "ignored in non-debug build")
            return
        }

        val baseUrl = intent.stringExtra(EXTRA_BASE_URL, EXTRA_BASE_URL_B64).orEmpty().trim()
        val modelName = intent.stringExtra(EXTRA_MODEL_NAME, EXTRA_MODEL_NAME_B64).orEmpty().trim()
        val displayName = intent.stringExtra(EXTRA_DISPLAY_NAME, EXTRA_DISPLAY_NAME_B64)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: modelName.ifBlank { "调试模型" }
        val apiKey = intent.stringExtra(EXTRA_API_KEY, EXTRA_API_KEY_B64)?.trim().orEmpty()
        val protocol = parseProtocol(intent.getStringExtra(EXTRA_PROTOCOL))
        val endpointMode = parseEndpointMode(intent.getStringExtra(EXTRA_ENDPOINT_MODE))
        val selectAfterSave = intent.getBooleanExtra(EXTRA_SELECT_AFTER_SAVE, true)
        val keepOnly = intent.getBooleanExtra(EXTRA_KEEP_ONLY, false)

        if (baseUrl.isBlank() || modelName.isBlank()) {
            Log.e(TAG, "missing required config baseUrlBlank=${baseUrl.isBlank()} modelBlank=${modelName.isBlank()}")
            return
        }

        val existingId = apiKeyManager.state.value.customModels.firstOrNull {
            (it.baseUrl == baseUrl && it.modelName == modelName) || it.modelName == modelName
        }?.id

        val id = apiKeyManager.saveCustomModel(
            existingId = existingId,
            displayName = displayName,
            baseUrl = baseUrl,
            modelName = modelName,
            protocol = protocol,
            endpointMode = endpointMode,
            apiKey = apiKey,
            selectAfterSave = selectAfterSave,
        )
        if (keepOnly) {
            apiKeyManager.state.value.customModels
                .filterNot { it.id == id }
                .forEach { apiKeyManager.deleteCustomModel(it.id) }
            if (selectAfterSave) {
                apiKeyManager.selectModel(id)
            }
        }
        Log.e(
            TAG,
            "saved custom model id=$id replaced=${existingId != null} selected=$selectAfterSave keepOnly=$keepOnly protocol=$protocol endpointMode=$endpointMode hasKey=${apiKey.isNotBlank()} keyLength=${apiKey.length}",
        )
    }

    private fun parseProtocol(raw: String?): ProviderProtocol =
        when (raw.orEmpty().trim().lowercase()) {
            "openai" -> ProviderProtocol.OPENAI
            "anthropic" -> ProviderProtocol.ANTHROPIC
            else -> ProviderProtocol.AUTO
        }

    private fun parseEndpointMode(raw: String?): ProviderEndpointMode =
        when (raw.orEmpty().trim().lowercase()) {
            "exact", "full", "完整地址" -> ProviderEndpointMode.EXACT
            else -> ProviderEndpointMode.AUTO
        }

    private fun Intent.stringExtra(rawKey: String, base64Key: String): String? =
        getStringExtra(base64Key)?.decodeBase64Utf8OrNull() ?: getStringExtra(rawKey)

    private fun String.decodeBase64Utf8OrNull(): String? =
        runCatching { String(Base64.decode(this, Base64.NO_WRAP), Charsets.UTF_8) }.getOrNull()

    companion object {
        private const val TAG = "NuclearBoyDebugModelConfig"
        const val ACTION_SAVE_CUSTOM_MODEL = "com.nuclearboy.app.DEBUG_SAVE_CUSTOM_MODEL"
        const val EXTRA_BASE_URL = "base_url"
        const val EXTRA_BASE_URL_B64 = "base_url_b64"
        const val EXTRA_MODEL_NAME = "model_name"
        const val EXTRA_MODEL_NAME_B64 = "model_name_b64"
        const val EXTRA_DISPLAY_NAME = "display_name"
        const val EXTRA_DISPLAY_NAME_B64 = "display_name_b64"
        const val EXTRA_API_KEY = "api_key"
        const val EXTRA_API_KEY_B64 = "api_key_b64"
        const val EXTRA_PROTOCOL = "protocol"
        const val EXTRA_ENDPOINT_MODE = "endpoint_mode"
        const val EXTRA_SELECT_AFTER_SAVE = "select_after_save"
        const val EXTRA_KEEP_ONLY = "keep_only"
    }
}
