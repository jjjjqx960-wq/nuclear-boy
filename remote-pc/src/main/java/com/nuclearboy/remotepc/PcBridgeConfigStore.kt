package com.nuclearboy.remotepc

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 远程电脑桥接配置的加密存储。
 *
 * token 属于敏感凭证，与 API Key 一样走 EncryptedSharedPreferences。
 */
class PcBridgeConfigStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "nuclearboy_remotepc_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    data class PcBridgeConfig(
        val enabled: Boolean = false,
        val url: String = "",
        val hasToken: Boolean = false,
        val tokenMasked: String = "",
        val lastConnectedHost: String = "",
        val lastConnectedClis: String = "",
    )

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<PcBridgeConfig> = _state.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        refresh()
    }

    /** 保存地址和 token。token 传 null 表示保留已存的不变。 */
    fun saveConnection(url: String, token: String?) {
        val editor = prefs.edit().putString(KEY_URL, url.trim())
        if (token != null) {
            editor.putString(KEY_TOKEN, token.trim())
        }
        editor.apply()
        refresh()
    }

    fun recordConnected(host: String, clis: Map<String, String>) {
        prefs.edit()
            .putString(KEY_LAST_HOST, host)
            .putString(KEY_LAST_CLIS, clis.entries.joinToString("、") { "${it.key} ${it.value}" })
            .apply()
        refresh()
    }

    fun clear() {
        prefs.edit().clear().apply()
        refresh()
    }

    fun currentUrl(): String = prefs.getString(KEY_URL, "") ?: ""

    fun currentToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun isConfigured(): Boolean = currentUrl().isNotBlank() && currentToken().isNotBlank()

    private fun refresh() {
        _state.value = readState()
    }

    private fun readState(): PcBridgeConfig {
        val token = prefs.getString(KEY_TOKEN, "") ?: ""
        return PcBridgeConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            url = prefs.getString(KEY_URL, "") ?: "",
            hasToken = token.isNotBlank(),
            tokenMasked = maskToken(token),
            lastConnectedHost = prefs.getString(KEY_LAST_HOST, "") ?: "",
            lastConnectedClis = prefs.getString(KEY_LAST_CLIS, "") ?: "",
        )
    }

    private fun maskToken(token: String): String = when {
        token.isBlank() -> ""
        token.length <= 12 -> "****"
        else -> "${token.take(6)}...${token.takeLast(4)}（共 ${token.length} 位）"
    }

    private companion object {
        const val KEY_ENABLED = "pc_bridge_enabled"
        const val KEY_URL = "pc_bridge_url"
        const val KEY_TOKEN = "pc_bridge_token"
        const val KEY_LAST_HOST = "pc_bridge_last_host"
        const val KEY_LAST_CLIS = "pc_bridge_last_clis"
    }
}
