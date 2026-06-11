package com.nuclearboy.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.nuclearboy.api.deepseek.ApiKeyManager
import com.nuclearboy.api.deepseek.DeepSeekApiClient
import com.nuclearboy.api.deepseek.ProviderEndpointMode
import com.nuclearboy.api.deepseek.ProviderProtocol
import com.nuclearboy.api.deepseek.sanitizeProviderBaseUrl
import com.nuclearboy.api.deepseek.sanitizeProviderModelName
import com.nuclearboy.app.R
import com.nuclearboy.app.diagnostics.AppDiagnostics
import com.nuclearboy.app.diagnostics.DiagnosticResult
import com.nuclearboy.app.diagnostics.DiagnosticStatus
import com.nuclearboy.app.ui.settings.parts.DiagnosticsCopyItem
import com.nuclearboy.app.ui.settings.parts.apiKeyFingerprintSummary
import com.nuclearboy.app.ui.settings.parts.fullDiagnosticsCopySummary
import com.nuclearboy.app.ui.settings.parts.modelNameCleanupSummary
import com.nuclearboy.app.ui.settings.parts.modelTestCopySummary
import com.nuclearboy.app.ui.settings.parts.modelTestFailureMessage
import com.nuclearboy.app.ui.settings.parts.modelTestRequestContextSummary
import com.nuclearboy.app.ui.settings.parts.providerBaseUrlCleanupSummary
import com.nuclearboy.app.ui.settings.parts.providerExactEndpointCompletionActionLabel
import com.nuclearboy.app.ui.settings.parts.providerExactEndpointRecoveryActionLabel
import com.nuclearboy.app.ui.settings.parts.providerExactEndpointWarning
import com.nuclearboy.app.ui.settings.parts.providerEndpointPreviewSummary
import com.nuclearboy.app.update.UpdateDownloader
import com.nuclearboy.app.update.UpdateManager
import com.nuclearboy.common.AppResult
import com.nuclearboy.common.ModelTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.widget.Toast
import javax.inject.Inject

data class ModelTestUiState(
    val targetId: String? = null,
    val inProgress: Boolean = false,
    val success: Boolean? = null,
    val message: String = "",
    val detail: String = "",
)

data class FullDiagnosticsUiState(
    val running: Boolean = false,
    val results: List<DiagnosticResult> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val apiKeyManager: ApiKeyManager,
    private val apiClient: DeepSeekApiClient,
    private val appDiagnostics: AppDiagnostics,
) : androidx.lifecycle.ViewModel() {

    val apiKeyState = apiKeyManager.state
    private val _modelTestState = MutableStateFlow(ModelTestUiState())
    val modelTestState: StateFlow<ModelTestUiState> = _modelTestState.asStateFlow()
    private val _fullDiagnosticsState = MutableStateFlow(FullDiagnosticsUiState())
    val fullDiagnosticsState: StateFlow<FullDiagnosticsUiState> = _fullDiagnosticsState.asStateFlow()

    fun setApiKey(key: String) {
        viewModelScope.launch {
            apiKeyManager.setPrimaryKey(key)
            // Trigger balance check
            // apiClient.checkBalance(key) -- needs a client instance
        }
    }

    fun removeApiKey() {
        viewModelScope.launch { apiKeyManager.removeKey(isPrimary = true) }
    }

    fun setAutoSwitch(enabled: Boolean) { apiKeyManager.setAutoSwitch(enabled) }
    fun setSimpleTasksUseFlash(enabled: Boolean) { apiKeyManager.setSimpleTasksUseFlash(enabled) }

    fun setCustomProviderEnabled(enabled: Boolean) { apiKeyManager.setCustomProviderEnabled(enabled) }

    /** key 传 null 表示保留已存的 Key 不变 */
    fun saveCustomProviderConfig(baseUrl: String, model: String, key: String?) {
        apiKeyManager.setCustomProviderConfig(baseUrl, model, key)
    }

    fun saveCustomModel(
        displayName: String,
        baseUrl: String,
        model: String,
        protocol: ProviderProtocol,
        endpointMode: ProviderEndpointMode,
        key: String?,
    ) {
        apiKeyManager.saveCustomModel(
            existingId = null,
            displayName = displayName,
            baseUrl = baseUrl,
            modelName = model,
            protocol = protocol,
            endpointMode = endpointMode,
            apiKey = key,
            selectAfterSave = true,
        )
    }

    fun selectModel(modelId: String) { apiKeyManager.selectModel(modelId) }

    fun deleteCustomModel(modelId: String) { apiKeyManager.deleteCustomModel(modelId) }

    fun testCustomModelInput(
        baseUrl: String,
        model: String,
        protocol: ProviderProtocol,
        endpointMode: ProviderEndpointMode,
        key: String?,
    ) {
        runCustomModelTest(
            targetId = NEW_MODEL_TEST_ID,
            baseUrl = baseUrl,
            model = model,
            protocol = protocol,
            endpointMode = endpointMode,
            key = key,
            label = model.ifBlank { "未命名模型" },
        )
    }

    fun testSavedCustomModel(modelId: String) {
        val config = apiKeyManager.getCustomModelConfig(modelId)
        if (config == null) {
            _modelTestState.value = ModelTestUiState(
                targetId = modelId,
                success = false,
                message = "找不到这个模型配置",
                detail = "它可能已经被删除，请刷新设置页后重试。",
            )
            return
        }
        runCustomModelTest(
            targetId = modelId,
            baseUrl = config.baseUrl,
            model = config.modelName,
            protocol = config.protocol,
            endpointMode = config.endpointMode,
            key = config.apiKey,
            label = config.displayName,
        )
    }

    private fun runCustomModelTest(
        targetId: String,
        baseUrl: String,
        model: String,
        protocol: ProviderProtocol,
        endpointMode: ProviderEndpointMode,
        key: String?,
        label: String,
    ) {
        val rawBaseUrl = baseUrl.trim()
        val normalizedBaseUrl = sanitizeProviderBaseUrl(baseUrl)
        val trimmedModel = model.trim()
        val normalizedModel = sanitizeProviderModelName(model)
        if (normalizedBaseUrl.isBlank() || normalizedModel.isBlank()) {
            _modelTestState.value = ModelTestUiState(
                targetId = targetId,
                success = false,
                message = "请先填写服务地址和模型名",
                detail = "服务地址示例：http://your-gateway:20128/v1；模型名示例：nvidia/minimaxai/minimax-m2.7",
            )
            return
        }
        val keyFingerprint = apiKeyFingerprintSummary(key)
        val baseUrlCleanupSummary = providerBaseUrlCleanupSummary(rawBaseUrl, normalizedBaseUrl)
        val cleanupSummary = modelNameCleanupSummary(trimmedModel, normalizedModel)
        val resolvedProtocol = ProviderProtocol.resolve(protocol, normalizedBaseUrl, normalizedModel)
        val requestEndpoint = when (resolvedProtocol) {
            ProviderProtocol.ANTHROPIC ->
                DeepSeekApiClient.buildAnthropicMessagesEndpoint(normalizedBaseUrl, endpointMode)
            else ->
                DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint(normalizedBaseUrl, endpointMode)
        }
        val requestContext = modelTestRequestContextSummary(
            endpoint = requestEndpoint,
            modelName = normalizedModel,
            protocolLabel = resolvedProtocol.displayName,
            endpointModeLabel = endpointMode.displayName,
            keyFingerprintSummary = keyFingerprint,
        )
        val testLabel = sanitizeProviderModelName(label).ifBlank { normalizedModel }
        _modelTestState.value = ModelTestUiState(
            targetId = targetId,
            inProgress = true,
            message = "正在测试 $testLabel",
            detail = buildString {
                append("会发送一条 max_tokens=8 的 ping 请求，不会保存或输出明文 Key。")
                if (baseUrlCleanupSummary.isNotBlank()) {
                    append('\n')
                    append(baseUrlCleanupSummary)
                }
                if (cleanupSummary.isNotBlank()) {
                    append('\n')
                    append(cleanupSummary)
                }
                if (requestContext.isNotBlank()) {
                    append('\n')
                    append(requestContext)
                }
            },
        )
        viewModelScope.launch {
            _modelTestState.value = when (val result = apiClient.testCustomProvider(
                baseUrl = normalizedBaseUrl,
                modelName = normalizedModel,
                apiKey = key,
                protocol = protocol,
                endpointMode = endpointMode,
            )) {
                is AppResult.Success -> ModelTestUiState(
                    targetId = targetId,
                    success = true,
                    message = "模型连接成功",
                    detail = buildString {
                        append(
                            modelTestRequestContextSummary(
                                endpoint = result.data.endpoint,
                                modelName = result.data.modelName,
                                protocolLabel = result.data.protocol.displayName,
                                endpointModeLabel = result.data.endpointMode.displayName,
                                keyFingerprintSummary = keyFingerprint,
                            ),
                        )
                        append('\n')
                        if (baseUrlCleanupSummary.isNotBlank()) {
                            append(baseUrlCleanupSummary)
                            append('\n')
                        }
                        if (cleanupSummary.isNotBlank()) {
                            append(cleanupSummary)
                            append('\n')
                        }
                        append("耗时：${result.data.latencyMs} ms")
                        if (result.data.replyPreview.isNotBlank()) {
                            append("\n响应：${result.data.replyPreview}")
                        }
                    },
                )
                is AppResult.Failure -> ModelTestUiState(
                    targetId = targetId,
                    success = false,
                    message = modelTestFailureMessage(result.error, result.technicalDetail),
                    detail = buildString {
                        if (requestContext.isNotBlank()) {
                            append(requestContext)
                            append('\n')
                        }
                        if (baseUrlCleanupSummary.isNotBlank()) {
                            append(baseUrlCleanupSummary)
                            append('\n')
                        }
                        if (cleanupSummary.isNotBlank()) {
                            append(cleanupSummary)
                            append('\n')
                        }
                        append(result.technicalDetail ?: "没有更多错误细节。")
                    },
                )
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val key = apiKeyManager.getActiveKey() ?: return@launch
            // Validate by making a lightweight API call
            apiKeyManager.updateBalance("测试中…")
        }
    }

    fun runFullDiagnostics() {
        if (_fullDiagnosticsState.value.running) return
        _fullDiagnosticsState.value = _fullDiagnosticsState.value.copy(running = true)
        viewModelScope.launch {
            val results = appDiagnostics.runAll()
            _fullDiagnosticsState.value = FullDiagnosticsUiState(
                running = false,
                results = results,
            )
        }
    }

    companion object {
        const val NEW_MODEL_TEST_ID = "__new_custom_model__"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onMenuClick: () -> Unit = {},
    onNavigateToTutorial: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val apiKeyState by viewModel.apiKeyState.collectAsState()
    val modelTestState by viewModel.modelTestState.collectAsState()
    val fullDiagnosticsState by viewModel.fullDiagnosticsState.collectAsState()
    val scrollState = rememberScrollState()
    var showSponsorDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── API Key Section ──────────────────────────
            Text("🔑 API 设置",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // API Key status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (apiKeyState.hasPrimaryKey) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (apiKeyState.hasPrimaryKey) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (apiKeyState.hasPrimaryKey) "已配置: ${apiKeyState.primaryKeyMasked}"
                                   else "未配置 API Key",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    if (apiKeyState.balanceCny != null) {
                        Spacer(Modifier.height(4.dp))
                        Text("💰 余额: ${apiKeyState.balanceCny}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(12.dp))

                    // API Key input
                    var apiKeyInput by remember { mutableStateOf("") }
                    var showKeyInput by remember { mutableStateOf(!apiKeyState.hasPrimaryKey) }

                    if (showKeyInput || !apiKeyState.hasPrimaryKey) {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("DeepSeek API Key (sk-v4-...)") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (apiKeyInput.isNotBlank()) {
                                    viewModel.setApiKey(apiKeyInput.trim())
                                    apiKeyInput = ""
                                    showKeyInput = false
                                }
                            }),
                            singleLine = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    viewModel.setApiKey(apiKeyInput.trim())
                                    apiKeyInput = ""
                                    showKeyInput = false
                                }
                            }, enabled = apiKeyInput.isNotBlank()) {
                                Text("保存")
                            }
                            if (apiKeyState.hasPrimaryKey) {
                                OutlinedButton(onClick = { showKeyInput = false }) {
                                    Text("取消")
                                }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showKeyInput = true }) { Text("更换 Key") }
                            OutlinedButton(onClick = { viewModel.removeApiKey() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text("删除 Key")
                            }
                        }
                    }
                }
            }

            // ── Model Section ────────────────────────────
            Text("🧠 模型偏好",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("默认模型: ${ModelTier.V4_PRO.displayName}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("说明: 简单任务自动用 ${ModelTier.V4_FLASH.displayName} 省钱",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(8.dp))
                    var autoSwitch by remember { mutableStateOf(apiKeyState.autoSwitchEnabled) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = autoSwitch, onCheckedChange = {
                            autoSwitch = it; viewModel.setAutoSwitch(it)
                        })
                        Text("自动选择模型", style = MaterialTheme.typography.bodyMedium)
                    }
                    var simpleFlash by remember { mutableStateOf(apiKeyState.simpleTasksUseFlash) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = simpleFlash, onCheckedChange = {
                            simpleFlash = it; viewModel.setSimpleTasksUseFlash(it)
                        })
                        Text("简单任务用 Flash", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ── Third-Party Provider Section ─────────────
            Text("🌐 第三方模型（OpenAI / Anthropic 兼容）",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("当前模型：${apiKeyState.activeModelLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                    Text("支持保存多个 OpenAI 或 Anthropic 兼容模型，列表中点选即可切换。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("HTTP 私有网关也可测试；若网关暴露在公网，优先建议配置 HTTPS。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(12.dp))
                    ModelOptionRow(
                        title = "DeepSeek 官方",
                        subtitle = "使用官方 API Key，模式仍由聊天/思考/专家控制",
                        selected = apiKeyState.activeModelId == ApiKeyManager.OFFICIAL_MODEL_ID,
                        onSelect = { viewModel.selectModel(ApiKeyManager.OFFICIAL_MODEL_ID) },
                    )

                    apiKeyState.customModels.forEach { model ->
                        Spacer(Modifier.height(6.dp))
                        ModelOptionRow(
                            title = model.displayName,
                            subtitle = "${model.protocol.displayName} · ${model.endpointMode.displayName} · ${model.modelName} · ${model.baseUrl}",
                            selected = apiKeyState.activeModelId == model.id,
                            keyText = if (model.hasKey) model.keyMasked else "无鉴权",
                            testing = modelTestState.inProgress && modelTestState.targetId == model.id,
                            onSelect = { viewModel.selectModel(model.id) },
                            onTest = { viewModel.testSavedCustomModel(model.id) },
                            onDelete = { viewModel.deleteCustomModel(model.id) },
                        )
                    }

                    if (modelTestState.message.isNotBlank() &&
                        modelTestState.targetId != SettingsViewModel.NEW_MODEL_TEST_ID
                    ) {
                        Spacer(Modifier.height(10.dp))
                        ModelTestResultBox(modelTestState)
                    }

                    Spacer(Modifier.height(14.dp))
                    var displayNameInput by remember { mutableStateOf("") }
                    var baseUrlInput by remember { mutableStateOf("") }
                    var modelInput by remember { mutableStateOf("") }
                    var protocolInput by remember { mutableStateOf(ProviderProtocol.AUTO) }
                    var endpointModeInput by remember { mutableStateOf(ProviderEndpointMode.AUTO) }
                    var customKeyInput by remember { mutableStateOf("") }
                    val sanitizedBaseUrlInput = remember(baseUrlInput) {
                        sanitizeProviderBaseUrl(baseUrlInput)
                    }
                    val baseUrlInputCleanupSummary = remember(baseUrlInput, sanitizedBaseUrlInput) {
                        providerBaseUrlCleanupSummary(baseUrlInput, sanitizedBaseUrlInput)
                    }
                    val sanitizedModelInput = remember(modelInput) {
                        sanitizeProviderModelName(modelInput)
                    }
                    val modelInputCleanupSummary = remember(modelInput, sanitizedModelInput) {
                        modelNameCleanupSummary(modelInput, sanitizedModelInput)
                    }
                    val providerEndpointPreview = remember(
                        sanitizedBaseUrlInput,
                        protocolInput,
                        endpointModeInput,
                        sanitizedModelInput,
                    ) {
                        val baseUrl = sanitizedBaseUrlInput
                        if (baseUrl.isBlank()) {
                            ""
                        } else {
                            val resolvedProtocol = ProviderProtocol.resolve(
                                protocolInput,
                                baseUrl,
                                sanitizedModelInput,
                            )
                            val endpoint = when (resolvedProtocol) {
                                ProviderProtocol.ANTHROPIC ->
                                    DeepSeekApiClient.buildAnthropicMessagesEndpoint(baseUrl, endpointModeInput)
                                else ->
                                    DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint(baseUrl, endpointModeInput)
                            }
                            providerEndpointPreviewSummary(
                                protocolLabel = resolvedProtocol.displayName,
                                endpointModeLabel = endpointModeInput.displayName,
                                endpoint = endpoint,
                            )
                        }
                    }
                    val providerEndpointWarning = remember(
                        sanitizedBaseUrlInput,
                        protocolInput,
                        endpointModeInput,
                        sanitizedModelInput,
                    ) {
                        val baseUrl = sanitizedBaseUrlInput
                        if (baseUrl.isBlank() || endpointModeInput != ProviderEndpointMode.EXACT) {
                            ""
                        } else {
                            val resolvedProtocol = ProviderProtocol.resolve(
                                protocolInput,
                                baseUrl,
                                sanitizedModelInput,
                            )
                            val endpoint = when (resolvedProtocol) {
                                ProviderProtocol.ANTHROPIC ->
                                    DeepSeekApiClient.buildAnthropicMessagesEndpoint(baseUrl, endpointModeInput)
                                else ->
                                    DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint(baseUrl, endpointModeInput)
                            }
                            providerExactEndpointWarning(
                                protocolLabel = resolvedProtocol.displayName,
                                endpoint = endpoint,
                            )
                        }
                    }
                    val providerExactCompletionEndpoint = remember(
                        sanitizedBaseUrlInput,
                        protocolInput,
                        endpointModeInput,
                        sanitizedModelInput,
                        providerEndpointWarning,
                    ) {
                        val baseUrl = sanitizedBaseUrlInput
                        if (baseUrl.isBlank() || providerEndpointWarning.isBlank()) {
                            ""
                        } else {
                            val resolvedProtocol = ProviderProtocol.resolve(
                                protocolInput,
                                baseUrl,
                                sanitizedModelInput,
                            )
                            when (resolvedProtocol) {
                                ProviderProtocol.ANTHROPIC ->
                                    DeepSeekApiClient.buildAnthropicMessagesEndpoint(baseUrl, ProviderEndpointMode.AUTO)
                                else ->
                                    DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint(baseUrl, ProviderEndpointMode.AUTO)
                            }
                        }
                    }
                    val providerEndpointCompletionAction = remember(
                        providerEndpointWarning,
                        sanitizedBaseUrlInput,
                        providerExactCompletionEndpoint,
                    ) {
                        providerExactEndpointCompletionActionLabel(
                            warning = providerEndpointWarning,
                            currentEndpoint = sanitizedBaseUrlInput,
                            suggestedEndpoint = providerExactCompletionEndpoint,
                        )
                    }
                    val providerEndpointRecoveryAction = remember(providerEndpointWarning) {
                        providerExactEndpointRecoveryActionLabel(providerEndpointWarning)
                    }

                    Text("添加模型",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        label = { Text("显示名称（如 9router Claude）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("服务地址（如 https://my-9router.com）") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            if (baseUrlInputCleanupSummary.isNotBlank()) {
                                Text(baseUrlInputCleanupSummary)
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("模型名（如 gpt-4o / claude-3-5-sonnet）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            if (modelInputCleanupSummary.isNotBlank()) {
                                Text(modelInputCleanupSummary)
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("协议",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProviderProtocol.entries.forEach { protocol ->
                            FilterChip(
                                selected = protocolInput == protocol,
                                onClick = { protocolInput = protocol },
                                label = { Text(protocol.displayName) },
                            )
                        }
                    }
                    Text(protocolInput.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("地址模式",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProviderEndpointMode.entries.forEach { mode ->
                            FilterChip(
                                selected = endpointModeInput == mode,
                                onClick = { endpointModeInput = mode },
                                label = { Text(mode.displayName) },
                            )
                        }
                    }
                    Text(endpointModeInput.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (providerEndpointPreview.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            providerEndpointPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    if (providerEndpointWarning.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            providerEndpointWarning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (
                        providerEndpointCompletionAction.isNotBlank() ||
                        providerEndpointRecoveryAction.isNotBlank()
                    ) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (providerEndpointCompletionAction.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        baseUrlInput = providerExactCompletionEndpoint
                                        Toast.makeText(context, "已补成完整地址", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        Icons.Filled.Build,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(providerEndpointCompletionAction)
                                }
                            }
                            if (providerEndpointRecoveryAction.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        endpointModeInput = ProviderEndpointMode.AUTO
                                        Toast.makeText(context, "已切回智能拼接", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(providerEndpointRecoveryAction)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customKeyInput,
                        onValueChange = { customKeyInput = it },
                        label = { Text("API Key（可留空，部分网关不需要）") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        supportingText = {
                            val fingerprint = apiKeyFingerprintSummary(customKeyInput)
                            if (fingerprint.isNotBlank()) {
                                Text(fingerprint)
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.testCustomModelInput(
                                    sanitizedBaseUrlInput,
                                    sanitizedModelInput,
                                    protocolInput,
                                    endpointModeInput,
                                    customKeyInput.trim(),
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = sanitizedBaseUrlInput.isNotBlank() &&
                                sanitizedModelInput.isNotBlank() &&
                                !(modelTestState.inProgress &&
                                    modelTestState.targetId == SettingsViewModel.NEW_MODEL_TEST_ID),
                        ) {
                            if (modelTestState.inProgress &&
                                modelTestState.targetId == SettingsViewModel.NEW_MODEL_TEST_ID
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.WifiTethering, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("测试填写")
                        }
                        Button(
                            onClick = {
                                viewModel.saveCustomModel(
                                    displayNameInput.trim().ifBlank { sanitizedModelInput },
                                    sanitizedBaseUrlInput,
                                    sanitizedModelInput,
                                    protocolInput,
                                    endpointModeInput,
                                    customKeyInput.trim(),
                                )
                                displayNameInput = ""
                                baseUrlInput = ""
                                modelInput = ""
                                protocolInput = ProviderProtocol.AUTO
                                endpointModeInput = ProviderEndpointMode.AUTO
                                customKeyInput = ""
                            },
                            modifier = Modifier.weight(1f),
                            enabled = sanitizedBaseUrlInput.isNotBlank() && sanitizedModelInput.isNotBlank(),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("添加并选中")
                        }
                    }
                    if (modelTestState.message.isNotBlank() &&
                        modelTestState.targetId == SettingsViewModel.NEW_MODEL_TEST_ID
                    ) {
                        Spacer(Modifier.height(10.dp))
                        ModelTestResultBox(modelTestState)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("智能拼接会按协议补全标准路径；完整地址会原样请求你填写的 URL，适合非标准第三方网关。第三方模型会自动关闭 DeepSeek 专属思考参数。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Diagnostics Section ──────────────────────
            Text("🧪 功能自检",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("一键检查模型配置、第三方接口、Python 执行器、文件工具和 Agent 工具链。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.runFullDiagnostics() },
                        enabled = !fullDiagnosticsState.running,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (fullDiagnosticsState.running) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(if (fullDiagnosticsState.running) "自检中..." else "运行全量自检")
                    }

                    if (fullDiagnosticsState.results.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        val failed = fullDiagnosticsState.results.count { it.status == DiagnosticStatus.FAIL }
                        val warned = fullDiagnosticsState.results.count { it.status == DiagnosticStatus.WARN }
                        val diagnosticsCopySummary = remember(fullDiagnosticsState.results) {
                            fullDiagnosticsCopySummary(
                                fullDiagnosticsState.results.map { result ->
                                    DiagnosticsCopyItem(
                                        name = result.name,
                                        status = result.status.name,
                                        message = result.message,
                                        durationMs = result.durationMs,
                                        detail = result.detail,
                                    )
                                }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "结果：${fullDiagnosticsState.results.size} 项，失败 $failed，警告 $warned",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(diagnosticsCopySummary))
                                    Toast.makeText(context, "已复制自检摘要", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "复制自检摘要",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            fullDiagnosticsState.results.forEach { result ->
                                DiagnosticResultRow(result)
                            }
                        }
                    }
                }
            }

            // ── API Key Tutorial ─────────────────────────
            Text("📖 API Key 帮助",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("不知道怎么获取 API Key？",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("我们准备了一份详细的图文教程，手把手教你注册和获取 DeepSeek API Key。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateToTutorial,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF08090B)),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("查看图文教程", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Sponsor Section ──────────────────────────
            Text("❤️ 投喂作者",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("☢️ 核弹男孩 · 为爱发电",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "我是 mzpr00，一名普通的开发者。\n这款 App 永久免费开源，为所有同学打造。\n如果它帮到了你，可以请我喝杯奶茶 ☕",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )

                    Spacer(Modifier.height(16.dp))

                    // Payment tier buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SponsorTierButton(
                            emoji = "🍫",
                            label = "一块脆脆鲨",
                            amount = "¥1",
                            modifier = Modifier.weight(1f),
                            onClick = { showSponsorDialog = true },
                        )
                        SponsorTierButton(
                            emoji = "🧋",
                            label = "一杯奶茶",
                            amount = "¥6",
                            modifier = Modifier.weight(1f),
                            onClick = { showSponsorDialog = true },
                        )
                        SponsorTierButton(
                            emoji = "🍜",
                            label = "一顿午饭",
                            amount = "¥15",
                            modifier = Modifier.weight(1f),
                            onClick = { showSponsorDialog = true },
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "每一份支持都是我继续开发的动力 💪",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // ── Sponsor Dialog ────────────────────────────
            if (showSponsorDialog) {
                AlertDialog(
                    onDismissRequest = { showSponsorDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text("📱 扫码投喂",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth())
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            // 收款二维码
                            Image(
                                painter = painterResource(R.drawable.payment_qr),
                                contentDescription = "收款码",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "感谢每一位支持者！你的名字会被记录在 App 的致谢名单中 🌟",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSponsorDialog = false }) {
                            Text("先不了，下次一定", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                )
            }

            // ── About Section ────────────────────────────
            Text("ℹ️ 关于",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("☢️ 核弹男孩 NUCLEAR BOY", style = MaterialTheme.typography.titleMedium)

                    // 动态版本号
                    val currentVersion = try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
                    } catch (e: Exception) { "0.0.0" }
                    Text("v$currentVersion · BUILD ${com.nuclearboy.app.BuildConfig.BUILD_TYPE.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(4.dp))
                    Text("作者: mzpr00 · mapr00@163.com", style = MaterialTheme.typography.bodySmall)
                    Text("免费开源 · 为所有同学打造", style = MaterialTheme.typography.bodySmall)
                    Text("新时代的贾维斯，比你更懂你的手机", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.height(12.dp))

                    // 检查更新按钮
                    var updateChecking by remember { mutableStateOf(false) }
                    var updateResult by remember { mutableStateOf<UpdateManager.UpdateResult?>(null) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                updateChecking = true
                                updateResult = null
                                kotlinx.coroutines.MainScope().launch {
                                    val um = UpdateManager(context)
                                    updateResult = um.checkForUpdate(force = true)
                                    updateChecking = false
                                }
                            },
                            enabled = !updateChecking,
                        ) {
                            if (updateChecking) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("检查中...")
                            } else {
                                Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("检查更新")
                            }
                        }
                    }

                    // 更新结果
                    when (val result = updateResult) {
                        is UpdateManager.UpdateResult.Available -> {
                            Spacer(Modifier.height(8.dp))
                            Text("🆕 新版本 ${result.version} 可用！",
                                color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                            if (result.body.isNotBlank()) {
                                Text(result.body.take(200),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = {
                                UpdateDownloader.download(context, result.url, result.version)
                            }) { Text("下载并安装 →", color = Color(0xFF00E676)) }
                        }
                        is UpdateManager.UpdateResult.UpToDate -> {
                            Spacer(Modifier.height(8.dp))
                            Text("✅ 已是最新版本", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is UpdateManager.UpdateResult.Error -> {
                            Spacer(Modifier.height(8.dp))
                            Text("⚠️ 检查失败: ${result.message}", color = MaterialTheme.colorScheme.error)
                        }
                        null -> {}
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SponsorTierButton(
    emoji: String,
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF00E676).copy(alpha = 0.06f),
            contentColor = Color(0xFF00E676),
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
        }
    }
}

@Composable
private fun ModelTestResultBox(state: ModelTestUiState) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val copySummary = remember(state.inProgress, state.success, state.message, state.detail) {
        modelTestCopySummary(
            inProgress = state.inProgress,
            success = state.success,
            message = state.message,
            detail = state.detail,
        )
    }
    val color = when (state.success) {
        true -> MaterialTheme.colorScheme.primary
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.28f)),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.inProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = color,
                    )
                } else {
                    Icon(
                        imageVector = if (state.success == true) Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    state.message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = color,
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(copySummary))
                        Toast.makeText(context, "已复制测试摘要", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "复制测试摘要",
                        tint = color,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (state.detail.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    state.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticResultRow(result: DiagnosticResult) {
    val color = when (result.status) {
        DiagnosticStatus.PASS -> MaterialTheme.colorScheme.primary
        DiagnosticStatus.WARN -> Color(0xFFFFB020)
        DiagnosticStatus.FAIL -> MaterialTheme.colorScheme.error
    }
    val icon = when (result.status) {
        DiagnosticStatus.PASS -> Icons.Filled.CheckCircle
        DiagnosticStatus.WARN -> Icons.Filled.Warning
        DiagnosticStatus.FAIL -> Icons.Filled.Error
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${result.name} · ${result.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = color,
                )
                Text(
                    "耗时 ${result.durationMs} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (result.detail.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        result.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    keyText: String? = null,
    testing: Boolean = false,
    onSelect: () -> Unit,
    onTest: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (!keyText.isNullOrBlank()) {
                Text(keyText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onTest != null) {
            IconButton(
                onClick = onTest,
                enabled = !testing,
                modifier = Modifier.size(36.dp),
            ) {
                if (testing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.WifiTethering, contentDescription = "测试模型")
                }
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "删除模型", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
