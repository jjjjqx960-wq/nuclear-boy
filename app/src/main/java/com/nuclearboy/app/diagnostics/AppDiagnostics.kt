package com.nuclearboy.app.diagnostics

import android.util.Log
import com.nuclearboy.agent.ToolRegistry
import com.nuclearboy.api.deepseek.ApiKeyManager
import com.nuclearboy.api.deepseek.DeepSeekApiClient
import com.nuclearboy.common.AppResult
import com.nuclearboy.python.PythonSandbox
import com.nuclearboy.tools.docgen.FileOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class DiagnosticStatus {
    PASS,
    WARN,
    FAIL,
}

data class DiagnosticResult(
    val name: String,
    val status: DiagnosticStatus,
    val message: String,
    val detail: String = "",
    val durationMs: Long = 0,
)

@Singleton
class AppDiagnostics @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val apiClient: DeepSeekApiClient,
    private val pythonSandbox: PythonSandbox,
    private val fileOperations: FileOperations,
    private val toolRegistry: ToolRegistry,
) {
    suspend fun runAll(): List<DiagnosticResult> = withContext(Dispatchers.IO) {
        val checks = listOf<suspend () -> DiagnosticResult>(
            { checkModelState() },
            { checkModelCrud() },
            { checkCustomProviders() },
            { checkPythonExecutor() },
            { checkPythonWriteAccess() },
            { checkFileOperations() },
            { checkToolRegistry() },
            { checkToolPythonBridge() },
        )
        checks.map { check ->
            runCatching { check() }.getOrElse { error ->
                DiagnosticResult(
                    name = "诊断流程",
                    status = DiagnosticStatus.FAIL,
                    message = "诊断步骤异常",
                    detail = error.message.orEmpty().take(300),
                )
            }.also { result ->
                Log.e(
                    "NuclearBoy",
                    "[Diag] ${result.name} status=${result.status} durationMs=${result.durationMs} detailLen=${result.detail.length}",
                )
            }
        }
    }

    private fun checkModelState(): DiagnosticResult = timed("模型配置状态") {
        val state = apiKeyManager.state.value
        val customCount = state.customModels.size
        val activeKnown = state.activeModelId == ApiKeyManager.OFFICIAL_MODEL_ID ||
            state.customModels.any { it.id == state.activeModelId }
        when {
            !activeKnown -> DiagnosticResult(
                name = "模型配置状态",
                status = DiagnosticStatus.FAIL,
                message = "当前模型 ID 不在列表中",
                detail = "activeModelId=${state.activeModelId}; customCount=$customCount",
            )
            state.activeModelId == ApiKeyManager.OFFICIAL_MODEL_ID && !state.hasPrimaryKey -> DiagnosticResult(
                name = "模型配置状态",
                status = DiagnosticStatus.WARN,
                message = "当前为官方模型，但 DeepSeek Key 未配置",
                detail = "可切换到已保存第三方模型，或在 API 设置里配置官方 Key。",
            )
            else -> DiagnosticResult(
                name = "模型配置状态",
                status = DiagnosticStatus.PASS,
                message = "当前模型配置可解析",
                detail = "active=${state.activeModelLabel}; customCount=$customCount",
            )
        }
    }

    private fun checkModelCrud(): DiagnosticResult = timed("模型列表增删选") {
        val previousActive = apiKeyManager.getActiveModelId()
        var tempId: String? = null
        try {
            tempId = apiKeyManager.saveCustomModel(
                existingId = null,
                displayName = "诊断临时模型",
                baseUrl = "http://127.0.0.1:9/v1",
                modelName = "diagnostic-temp-model",
                protocol = com.nuclearboy.api.deepseek.ProviderProtocol.OPENAI,
                endpointMode = com.nuclearboy.api.deepseek.ProviderEndpointMode.AUTO,
                apiKey = "",
                selectAfterSave = true,
            )
            val afterAdd = apiKeyManager.state.value
            if (afterAdd.activeModelId != tempId || afterAdd.customModels.none { it.id == tempId }) {
                return@timed DiagnosticResult(
                    name = "模型列表增删选",
                    status = DiagnosticStatus.FAIL,
                    message = "新增或选中临时模型失败",
                    detail = "activeMatched=${afterAdd.activeModelId == tempId}; listContains=${afterAdd.customModels.any { it.id == tempId }}",
                )
            }

            apiKeyManager.selectModel(ApiKeyManager.OFFICIAL_MODEL_ID)
            if (apiKeyManager.getActiveModelId() != ApiKeyManager.OFFICIAL_MODEL_ID) {
                return@timed DiagnosticResult(
                    name = "模型列表增删选",
                    status = DiagnosticStatus.FAIL,
                    message = "切回官方模型失败",
                )
            }

            apiKeyManager.deleteCustomModel(tempId)
            tempId = null
            if (apiKeyManager.state.value.customModels.any { it.modelName == "diagnostic-temp-model" }) {
                return@timed DiagnosticResult(
                    name = "模型列表增删选",
                    status = DiagnosticStatus.FAIL,
                    message = "删除临时模型失败",
                )
            }

            restoreModelSelection(previousActive)
            DiagnosticResult(
                name = "模型列表增删选",
                status = DiagnosticStatus.PASS,
                message = "模型新增、选择、删除正常",
                detail = "已恢复原选中模型。",
            )
        } catch (e: Exception) {
            DiagnosticResult(
                name = "模型列表增删选",
                status = DiagnosticStatus.FAIL,
                message = "模型 CRUD 自检异常",
                detail = e.message.orEmpty().take(300),
            )
        } finally {
            tempId?.let { apiKeyManager.deleteCustomModel(it) }
            restoreModelSelection(previousActive)
        }
    }

    private fun restoreModelSelection(modelId: String) {
        if (modelId == ApiKeyManager.OFFICIAL_MODEL_ID ||
            apiKeyManager.state.value.customModels.any { it.id == modelId }
        ) {
            apiKeyManager.selectModel(modelId)
        } else {
            apiKeyManager.selectModel(ApiKeyManager.OFFICIAL_MODEL_ID)
        }
    }

    private suspend fun checkCustomProviders(): DiagnosticResult = timedSuspend("第三方模型连通性") {
        val state = apiKeyManager.state.value
        if (state.customModels.isEmpty()) {
            return@timedSuspend DiagnosticResult(
                name = "第三方模型连通性",
                status = DiagnosticStatus.WARN,
                message = "没有已保存的第三方模型",
                detail = "在设置页添加模型后可运行连通性测试。",
            )
        }

        val failures = mutableListOf<String>()
        var passCount = 0
        state.customModels.forEach { model ->
            val config = apiKeyManager.getCustomModelConfig(model.id)
            if (config == null) {
                failures.add("${model.displayName}: 配置缺失")
                return@forEach
            }
            when (val result = apiClient.testCustomProvider(
                config.baseUrl,
                config.modelName,
                config.apiKey,
                config.protocol,
                config.endpointMode,
            )) {
                is AppResult.Success -> passCount++
                is AppResult.Failure -> failures.add(
                    "${model.displayName}: ${result.error.code} ${result.technicalDetail.orEmpty().take(160)}"
                )
            }
        }

        if (failures.isEmpty()) {
            DiagnosticResult(
                name = "第三方模型连通性",
                status = DiagnosticStatus.PASS,
                message = "已保存第三方模型全部可用",
                detail = "通过 $passCount/${state.customModels.size}",
            )
        } else {
            DiagnosticResult(
                name = "第三方模型连通性",
                status = if (passCount > 0) DiagnosticStatus.WARN else DiagnosticStatus.FAIL,
                message = "部分第三方模型不可用",
                detail = "通过 $passCount/${state.customModels.size}\n${failures.joinToString("\n")}",
            )
        }
    }

    private suspend fun checkPythonExecutor(): DiagnosticResult = timedSuspend("Python 执行器") {
        val result = pythonSandbox.execute(
            script = "print('NB_DIAG_PY_OK')",
            workingDir = fileOperations.projectRoot().absolutePath,
            timeoutSeconds = 20,
        )
        if (result.exitCode == 0 && result.stdout.contains("NB_DIAG_PY_OK")) {
            DiagnosticResult(
                name = "Python 执行器",
                status = DiagnosticStatus.PASS,
                message = "Python 可执行",
                detail = "version=${pythonSandbox.getPythonVersion()}; execMs=${result.executionTimeMs}",
            )
        } else {
            DiagnosticResult(
                name = "Python 执行器",
                status = DiagnosticStatus.FAIL,
                message = "Python 执行失败",
                detail = result.stderr.ifBlank { result.stdout }.take(500),
            )
        }
    }

    private suspend fun checkPythonWriteAccess(): DiagnosticResult = timedSuspend("Python 非隔离写入") {
        val fileName = "nb_diag_python_write.txt"
        val target = File(fileOperations.projectRoot(), fileName)
        target.delete()
        val result = pythonSandbox.execute(
            script = """
from pathlib import Path
p = Path("$fileName")
p.write_text("NB_DIAG_WRITE_OK", encoding="utf-8")
print(p.read_text(encoding="utf-8"))
            """.trimIndent(),
            workingDir = fileOperations.projectRoot().absolutePath,
            timeoutSeconds = 20,
        )
        val content = target.takeIf { it.isFile }?.readText(Charsets.UTF_8).orEmpty()
        target.delete()
        if (result.exitCode == 0 && content == "NB_DIAG_WRITE_OK") {
            DiagnosticResult(
                name = "Python 非隔离写入",
                status = DiagnosticStatus.PASS,
                message = "Python 可在项目目录写读文件",
                detail = "写入目标位于当前项目工作目录，测试后已清理。",
            )
        } else {
            DiagnosticResult(
                name = "Python 非隔离写入",
                status = DiagnosticStatus.FAIL,
                message = "Python 写读文件失败",
                detail = result.stderr.ifBlank { result.stdout }.take(500),
            )
        }
    }

    private suspend fun checkFileOperations(): DiagnosticResult = timedSuspend("文件工具写读删") {
        val path = ".agent/diagnostics/file_tool_test.txt"
        val expected = "NB_DIAG_FILE_OK"
        val write = fileOperations.writeFile(path, expected)
        if (write is AppResult.Failure) {
            return@timedSuspend DiagnosticResult(
                name = "文件工具写读删",
                status = DiagnosticStatus.FAIL,
                message = "写入失败",
                detail = write.technicalDetail.orEmpty(),
            )
        }
        val read = fileOperations.readFile(path)
        if (read is AppResult.Failure || (read as? AppResult.Success)?.data?.content != expected) {
            fileOperations.deleteFile(path)
            return@timedSuspend DiagnosticResult(
                name = "文件工具写读删",
                status = DiagnosticStatus.FAIL,
                message = "读取校验失败",
                detail = (read as? AppResult.Failure)?.technicalDetail.orEmpty(),
            )
        }
        val delete = fileOperations.deleteFile(path)
        if (delete is AppResult.Failure) {
            return@timedSuspend DiagnosticResult(
                name = "文件工具写读删",
                status = DiagnosticStatus.FAIL,
                message = "清理失败",
                detail = delete.technicalDetail.orEmpty(),
            )
        }
        DiagnosticResult(
            name = "文件工具写读删",
            status = DiagnosticStatus.PASS,
            message = "文件写入、读取、删除均正常",
            detail = path,
        )
    }

    private suspend fun checkToolRegistry(): DiagnosticResult = timedSuspend("工具注册表") {
        val required = listOf("run_python", "read_file", "write_file", "list_directory", "web_search", "web_fetch")
        val missing = required.filterNot { toolRegistry.hasTool(it) }
        val count = toolRegistry.toolCount()
        if (missing.isEmpty()) {
            DiagnosticResult(
                name = "工具注册表",
                status = DiagnosticStatus.PASS,
                message = "核心工具已注册",
                detail = "toolCount=$count",
            )
        } else {
            DiagnosticResult(
                name = "工具注册表",
                status = DiagnosticStatus.FAIL,
                message = "核心工具缺失",
                detail = "toolCount=$count; missing=${missing.joinToString()}",
            )
        }
    }

    private suspend fun checkToolPythonBridge(): DiagnosticResult = timedSuspend("run_python 工具链") {
        val result = toolRegistry.executeSafe(
            "run_python",
            mapOf(
                "path" to "print('NB_DIAG_TOOL_OK')",
                "workingDir" to fileOperations.projectRoot().absolutePath,
            ),
        )
        if (result.success && result.output.contains("NB_DIAG_TOOL_OK")) {
            DiagnosticResult(
                name = "run_python 工具链",
                status = DiagnosticStatus.PASS,
                message = "Agent 工具到 Python 执行器链路正常",
                detail = "outputLen=${result.output.length}",
            )
        } else {
            DiagnosticResult(
                name = "run_python 工具链",
                status = DiagnosticStatus.FAIL,
                message = "run_python 工具执行失败",
                detail = (result.error ?: result.output).take(500),
            )
        }
    }

    private fun timed(name: String, block: () -> DiagnosticResult): DiagnosticResult {
        val start = System.currentTimeMillis()
        return block().copy(name = name, durationMs = System.currentTimeMillis() - start)
    }

    private suspend fun timedSuspend(name: String, block: suspend () -> DiagnosticResult): DiagnosticResult {
        val start = System.currentTimeMillis()
        return block().copy(name = name, durationMs = System.currentTimeMillis() - start)
    }
}
