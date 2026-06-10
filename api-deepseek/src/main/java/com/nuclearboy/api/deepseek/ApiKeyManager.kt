package com.nuclearboy.api.deepseek

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nuclearboy.common.AppConstants
import com.nuclearboy.common.AppError
import com.nuclearboy.common.AppResult
import com.nuclearboy.common.maskApiKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Secure API key management using Android Keystore-backed encryption.
 *
 * Features:
 * - Encrypted storage via EncryptedSharedPreferences
 * - Primary + backup key support
 * - Automatic failover when primary key is exhausted
 * - Balance checking and validation
 * - Safe display (masked) in UI
 */
class ApiKeyManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "nuclearboy_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    data class ApiKeyState(
        val hasPrimaryKey: Boolean = false,
        val primaryKeyMasked: String = "",
        val hasBackupKey: Boolean = false,
        val backupKeyMasked: String = "",
        val balanceCny: String? = null,
        val isValidated: Boolean = false,
        val lastValidatedAt: Long = 0,
        val autoSwitchEnabled: Boolean = true,
        val simpleTasksUseFlash: Boolean = true,
        val customProviderEnabled: Boolean = false,
        val activeModelId: String = OFFICIAL_MODEL_ID,
        val activeModelLabel: String = "DeepSeek 官方",
        val customModels: List<CustomModelUiState> = emptyList(),
        val customBaseUrl: String = "",
        val customModelName: String = "",
        val hasCustomKey: Boolean = false,
        val customKeyMasked: String = "",
    )

    data class CustomModelUiState(
        val id: String,
        val displayName: String,
        val baseUrl: String,
        val modelName: String,
        val protocol: ProviderProtocol,
        val hasKey: Boolean,
        val keyMasked: String,
    )

    data class CustomModelConfig(
        val id: String,
        val displayName: String,
        val baseUrl: String,
        val modelName: String,
        val protocol: ProviderProtocol,
        val apiKey: String,
    )

    @Serializable
    private data class CustomModelRecord(
        val id: String,
        val displayName: String,
        val baseUrl: String,
        val modelName: String,
        val protocol: ProviderProtocol = ProviderProtocol.AUTO,
        val apiKey: String = "",
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _state = MutableStateFlow(ApiKeyState())
    val state: StateFlow<ApiKeyState> = _state.asStateFlow()

    private val primaryKey: String?
        get() = prefs.getString(KEY_PRIMARY, null)

    private val backupKey: String?
        get() = prefs.getString(KEY_BACKUP, null)

    init {
        refreshState()
    }

    /**
     * Get the active API key.
     * Custom providers only use their own key.
     * Some self-hosted gateways don't require auth, so an empty string means
     * "send no Authorization header" while still allowing the request to run.
     * Otherwise returns primary, falling back to backup.
     */
    fun getActiveKey(): String? {
        val custom = getActiveCustomModel()
        if (custom != null) {
            if (custom.apiKey.isNotBlank()) return custom.apiKey
            return ""
        }
        return primaryKey ?: backupKey
    }

    // ── Custom Provider (OpenAI-compatible, e.g. 9router) ──

    fun isCustomProviderEnabled(): Boolean =
        getActiveCustomModel() != null

    fun setCustomProviderEnabled(enabled: Boolean) {
        val editor = prefs.edit()
        if (enabled) {
            val firstCustom = loadCustomModels().firstOrNull()
            if (firstCustom != null) {
                editor.putString(KEY_ACTIVE_MODEL_ID, firstCustom.id)
            }
        } else {
            editor.putString(KEY_ACTIVE_MODEL_ID, OFFICIAL_MODEL_ID)
        }
        editor.putBoolean(KEY_CUSTOM_ENABLED, enabled).apply()
        refreshState()
    }

    /**
     * Save the custom OpenAI-compatible provider config.
     * @param baseUrl Service root, e.g. "https://my9router.example.com",
     * ".../v1", or a pasted ".../v1/chat/completions" endpoint.
     * @param modelName Model id passed in the request body, e.g. "gpt-4o" / "deepseek-chat".
     * @param apiKey null = keep the currently stored key; blank = clear it (gateways without auth).
     */
    fun setCustomProviderConfig(baseUrl: String, modelName: String, apiKey: String?) {
        saveCustomModel(
            existingId = getActiveCustomModel()?.id,
            displayName = modelName.trim().ifBlank { "自定义模型" },
            baseUrl = baseUrl,
            modelName = modelName,
            protocol = ProviderProtocol.AUTO,
            apiKey = apiKey,
            selectAfterSave = true,
        )
    }

    fun saveCustomModel(
        existingId: String?,
        displayName: String,
        baseUrl: String,
        modelName: String,
        protocol: ProviderProtocol = ProviderProtocol.AUTO,
        apiKey: String?,
        selectAfterSave: Boolean = true,
    ): String {
        val normalizedModelName = modelName.trim()
        val resolvedProtocol = ProviderProtocol.resolve(protocol, baseUrl, normalizedModelName)
        val normalizedBaseUrl = normalizeCustomBaseUrl(baseUrl, resolvedProtocol)
        val id = existingId?.takeIf { it.isNotBlank() } ?: "custom_${UUID.randomUUID()}"
        val currentModels = loadCustomModels()
        val previous = currentModels.firstOrNull { it.id == id }
        val normalizedKey = when {
            apiKey == null -> previous?.apiKey.orEmpty()
            else -> apiKey.trim()
        }
        val record = CustomModelRecord(
            id = id,
            displayName = displayName.trim().ifBlank { normalizedModelName.ifBlank { "自定义模型" } },
            baseUrl = normalizedBaseUrl,
            modelName = normalizedModelName,
            protocol = protocol,
            apiKey = normalizedKey,
        )
        val models = currentModels
            .filterNot { it.id == id }
            .plus(record)
            .filter { it.baseUrl.isNotBlank() && it.modelName.isNotBlank() }

        val editor = prefs.edit()
            .putString(KEY_CUSTOM_MODELS_JSON, json.encodeToString(models))
            .putString(KEY_CUSTOM_BASE_URL, record.baseUrl)
            .putString(KEY_CUSTOM_MODEL, record.modelName)
            .putString(KEY_CUSTOM_KEY, record.apiKey)
        if (selectAfterSave && models.any { it.id == id }) {
            editor.putString(KEY_ACTIVE_MODEL_ID, id)
                .putBoolean(KEY_CUSTOM_ENABLED, true)
        }
        editor.apply()
        refreshState()
        return id
    }

    fun selectModel(modelId: String) {
        val targetId = modelId.takeIf { it.isNotBlank() } ?: OFFICIAL_MODEL_ID
        val resolvedId = if (targetId == OFFICIAL_MODEL_ID || loadCustomModels().any { it.id == targetId }) {
            targetId
        } else {
            OFFICIAL_MODEL_ID
        }
        prefs.edit()
            .putString(KEY_ACTIVE_MODEL_ID, resolvedId)
            .putBoolean(KEY_CUSTOM_ENABLED, resolvedId != OFFICIAL_MODEL_ID)
            .apply()
        refreshState()
    }

    fun deleteCustomModel(modelId: String) {
        val models = loadCustomModels().filterNot { it.id == modelId }
        val wasActive = getActiveModelId() == modelId
        val nextActive = if (wasActive) OFFICIAL_MODEL_ID else getActiveModelId()
        prefs.edit()
            .putString(KEY_CUSTOM_MODELS_JSON, json.encodeToString(models))
            .putString(KEY_ACTIVE_MODEL_ID, nextActive)
            .putBoolean(KEY_CUSTOM_ENABLED, nextActive != OFFICIAL_MODEL_ID)
            .apply()
        refreshState()
    }

    fun getCustomModelConfig(modelId: String): CustomModelConfig? {
        return loadCustomModels().firstOrNull { it.id == modelId }?.let {
            CustomModelConfig(
                id = it.id,
                displayName = it.displayName,
                baseUrl = it.baseUrl,
                modelName = it.modelName,
                protocol = it.protocol,
                apiKey = it.apiKey,
            )
        }
    }

    /**
     * The base URL requests should target. Normalized: no trailing slash, common
     * chat-completions suffixes stripped, provider-specific version paths retained.
     */
    fun getActiveBaseUrl(): String {
        val custom = getActiveCustomModel()
        if (custom != null) {
            val url = normalizeCustomBaseUrl(custom.baseUrl, getResolvedProtocol(custom))
            if (url.isNotBlank()) return url
        }
        return AppConstants.DEEPSEEK_BASE_URL
    }

    fun getActiveProviderProtocol(): ProviderProtocol =
        getActiveCustomModel()?.let { getResolvedProtocol(it) } ?: ProviderProtocol.OPENAI

    /**
     * Model id override. Non-null only when the custom provider is active and configured —
     * the client then uses it instead of the DeepSeek tier model and omits
     * DeepSeek-specific fields (thinking / reasoning_effort).
     */
    fun getModelOverride(): String? {
        return getActiveCustomModel()?.modelName?.trim()?.takeIf { it.isNotBlank() }
    }

    // ── Legacy Python isolation toggle ─────────────────

    fun isSandboxEnabled(): Boolean = false

    fun setSandboxEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SANDBOX_ENABLED, false).apply()
        refreshState()
    }

    /**
     * Store and validate the primary API key.
     */
    suspend fun setPrimaryKey(apiKey: String): AppResult<Boolean> {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            return AppResult.failure(AppError.ApiKeyInvalid, "API Key 不能为空")
        }
        if (!trimmed.startsWith("sk-")) {
            return AppResult.failure(
                AppError.ApiKeyInvalid,
                "API Key 格式不正确，应该以 sk- 开头"
            )
        }

        return withContext(Dispatchers.IO) {
            prefs.edit().putString(KEY_PRIMARY, trimmed).apply()
            refreshState()
            AppResult.success(true)
        }
    }

    /**
     * Store a backup API key.
     */
    suspend fun setBackupKey(apiKey: String): AppResult<Boolean> {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            return AppResult.failure(AppError.ApiKeyInvalid, "备用 Key 不能为空")
        }

        return withContext(Dispatchers.IO) {
            prefs.edit().putString(KEY_BACKUP, trimmed).apply()
            refreshState()
            AppResult.success(true)
        }
    }

    /**
     * Switch to backup key (called when primary is exhausted).
     */
    fun switchToBackup(): Boolean {
        val backup = backupKey ?: return false
        prefs.edit()
            .putString(KEY_PRIMARY, backup)
            .remove(KEY_BACKUP)
            .apply()
        refreshState()
        return true
    }

    /**
     * Remove a key.
     */
    suspend fun removeKey(isPrimary: Boolean) {
        withContext(Dispatchers.IO) {
            if (isPrimary) {
                prefs.edit().remove(KEY_PRIMARY).apply()
            } else {
                prefs.edit().remove(KEY_BACKUP).apply()
            }
            refreshState()
        }
    }

    /**
     * Update balance display.
     */
    fun updateBalance(cny: String?) {
        _state.value = _state.value.copy(
            balanceCny = cny,
            isValidated = cny != null,
            lastValidatedAt = if (cny != null) System.currentTimeMillis()
                else _state.value.lastValidatedAt,
        )
    }

    /**
     * Toggle auto-switch behavior.
     */
    fun setAutoSwitch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SWITCH, enabled).apply()
        _state.value = _state.value.copy(autoSwitchEnabled = enabled)
    }

    fun setSimpleTasksUseFlash(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMPLE_FLASH, enabled).apply()
        _state.value = _state.value.copy(simpleTasksUseFlash = enabled)
    }

    private val customKey: String?
        get() = getActiveCustomModel()?.apiKey?.takeIf { it.isNotBlank() }

    private fun normalizeCustomBaseUrl(raw: String, protocol: ProviderProtocol): String {
        return when (protocol) {
            ProviderProtocol.ANTHROPIC -> DeepSeekApiClient.normalizeAnthropicBaseUrl(raw)
            ProviderProtocol.OPENAI -> DeepSeekApiClient.normalizeOpenAiBaseUrl(raw)
            ProviderProtocol.AUTO -> {
                when (ProviderProtocol.resolve(ProviderProtocol.AUTO, raw)) {
                    ProviderProtocol.ANTHROPIC -> DeepSeekApiClient.normalizeAnthropicBaseUrl(raw)
                    else -> DeepSeekApiClient.normalizeOpenAiBaseUrl(raw)
                }
            }
        }
    }

    private fun getResolvedProtocol(record: CustomModelRecord): ProviderProtocol {
        return ProviderProtocol.resolve(record.protocol, record.baseUrl, record.modelName)
    }

    fun getActiveModelId(): String =
        prefs.getString(KEY_ACTIVE_MODEL_ID, null)
            ?: if (prefs.getBoolean(KEY_CUSTOM_ENABLED, false) && loadCustomModels().isNotEmpty()) {
                loadCustomModels().first().id
            } else {
                OFFICIAL_MODEL_ID
            }

    private fun getActiveCustomModel(): CustomModelRecord? {
        val activeId = getActiveModelId()
        if (activeId == OFFICIAL_MODEL_ID) return null
        return loadCustomModels().firstOrNull { it.id == activeId }
    }

    private fun loadCustomModels(): List<CustomModelRecord> {
        val stored = prefs.getString(KEY_CUSTOM_MODELS_JSON, null)
        if (stored != null) {
            if (stored.isBlank()) return emptyList()
            return runCatching {
                json.decodeFromString<List<CustomModelRecord>>(stored)
            }.getOrDefault(emptyList())
        }

        val legacyBaseUrlRaw = prefs.getString(KEY_CUSTOM_BASE_URL, "").orEmpty()
        val legacyBaseUrl = normalizeCustomBaseUrl(legacyBaseUrlRaw, ProviderProtocol.AUTO)
        val legacyModel = prefs.getString(KEY_CUSTOM_MODEL, "").orEmpty().trim()
        if (legacyBaseUrl.isBlank() || legacyModel.isBlank()) return emptyList()
        return listOf(
            CustomModelRecord(
                id = LEGACY_CUSTOM_MODEL_ID,
                displayName = legacyModel,
                baseUrl = legacyBaseUrl,
                modelName = legacyModel,
                protocol = ProviderProtocol.AUTO,
                apiKey = prefs.getString(KEY_CUSTOM_KEY, "").orEmpty().trim(),
            )
        )
    }

    private fun refreshState() {
        val primary = primaryKey
        val backup = backupKey
        val customModels = loadCustomModels()
        val activeModelId = getActiveModelId().let { id ->
            if (id == OFFICIAL_MODEL_ID || customModels.any { it.id == id }) id else OFFICIAL_MODEL_ID
        }
        val activeCustom = customModels.firstOrNull { it.id == activeModelId }
        val custom = activeCustom?.apiKey?.takeIf { it.isNotBlank() }
        _state.value = ApiKeyState(
            hasPrimaryKey = primary != null,
            primaryKeyMasked = primary?.maskApiKey() ?: "",
            hasBackupKey = backup != null,
            backupKeyMasked = backup?.maskApiKey() ?: "",
            balanceCny = _state.value.balanceCny,
            isValidated = _state.value.isValidated,
            lastValidatedAt = _state.value.lastValidatedAt,
            autoSwitchEnabled = prefs.getBoolean(KEY_AUTO_SWITCH, true),
            simpleTasksUseFlash = prefs.getBoolean(KEY_SIMPLE_FLASH, true),
            customProviderEnabled = activeCustom != null,
            activeModelId = activeModelId,
            activeModelLabel = activeCustom?.displayName ?: "DeepSeek 官方",
            customModels = customModels.map {
                CustomModelUiState(
                    id = it.id,
                    displayName = it.displayName,
                    baseUrl = it.baseUrl,
                    modelName = it.modelName,
                    protocol = it.protocol,
                    hasKey = it.apiKey.isNotBlank(),
                    keyMasked = it.apiKey.takeIf { key -> key.isNotBlank() }?.maskApiKey() ?: "",
                )
            },
            customBaseUrl = activeCustom?.baseUrl ?: "",
            customModelName = activeCustom?.modelName ?: "",
            hasCustomKey = custom != null,
            customKeyMasked = custom?.maskApiKey() ?: "",
        )
    }

    companion object {
        private const val KEY_PRIMARY = "api_key_primary"
        private const val KEY_BACKUP = "api_key_backup"
        private const val KEY_AUTO_SWITCH = "auto_switch_enabled"
        private const val KEY_SIMPLE_FLASH = "simple_tasks_use_flash"
        private const val KEY_SANDBOX_ENABLED = "sandbox_enabled"
        const val OFFICIAL_MODEL_ID = "deepseek_official"
        private const val LEGACY_CUSTOM_MODEL_ID = "custom_legacy"
        private const val KEY_ACTIVE_MODEL_ID = "active_model_id"
        private const val KEY_CUSTOM_MODELS_JSON = "custom_models_json"
        private const val KEY_CUSTOM_ENABLED = "custom_provider_enabled"
        private const val KEY_CUSTOM_BASE_URL = "custom_provider_base_url"
        private const val KEY_CUSTOM_MODEL = "custom_provider_model"
        private const val KEY_CUSTOM_KEY = "custom_provider_api_key"
    }
}
