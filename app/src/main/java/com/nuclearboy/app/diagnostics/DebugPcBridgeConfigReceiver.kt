package com.nuclearboy.app.diagnostics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nuclearboy.app.BuildConfig
import com.nuclearboy.common.AppResult
import com.nuclearboy.remotepc.PcBridgeClient
import com.nuclearboy.remotepc.PcBridgeConfigStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 调试入口：通过 ADB 广播写入远程电脑桥接配置，便于真机端到端测试。
 *
 * adb shell am broadcast -a com.nuclearboy.app.DEBUG_SAVE_PC_BRIDGE \
 *   -n com.nuclearboy.app.debug/com.nuclearboy.app.diagnostics.DebugPcBridgeConfigReceiver \
 *   --es url "ws://192.168.1.10:7860" --es token "<token>" --ez enabled true
 *
 * 仅 debug 构建注册；日志不输出明文 token。
 */
@AndroidEntryPoint
class DebugPcBridgeConfigReceiver : BroadcastReceiver() {

    @Inject lateinit var configStore: PcBridgeConfigStore
    @Inject lateinit var bridgeClient: PcBridgeClient

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SAVE_PC_BRIDGE) return
        if (!BuildConfig.DEBUG) {
            Log.e(TAG, "ignored in non-debug build")
            return
        }

        val url = intent.getStringExtra(EXTRA_URL).orEmpty().trim()
        val token = intent.getStringExtra(EXTRA_TOKEN)?.trim().orEmpty()
        val enabled = intent.getBooleanExtra(EXTRA_ENABLED, true)

        if (url.isBlank()) {
            Log.e(TAG, "missing url extra")
            return
        }
        configStore.saveConnection(url, token.takeIf { it.isNotBlank() })
        configStore.setEnabled(enabled)
        // 可选：开关端到端加密（用于真机验证加密连接）
        if (intent.hasExtra(EXTRA_ENCRYPT)) {
            configStore.setEncryptionEnabled(intent.getBooleanExtra(EXTRA_ENCRYPT, false))
        }
        Log.e(TAG, "saved pc bridge url=$url enabled=$enabled hasToken=${token.isNotBlank()} tokenLength=${token.length} encrypt=${configStore.isEncryptionEnabled()}")

        // 保存后立即测试连接；传了 run_cli/run_prompt 时再实跑一条任务。
        // 结果打到 logcat，便于 ADB 端到端验证
        val runCli = intent.getStringExtra(EXTRA_RUN_CLI)?.trim().orEmpty()
        val runPrompt = intent.getStringExtra(EXTRA_RUN_PROMPT)?.trim().orEmpty()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (val result = bridgeClient.testConnection()) {
                    is AppResult.Success ->
                        Log.e(TAG, "test connection OK host=${result.data.host} clis=${result.data.clis}")
                    is AppResult.Failure ->
                        Log.e(TAG, "test connection FAILED code=${result.error.code} detail=${result.technicalDetail}")
                }
                if (intent.getBooleanExtra(EXTRA_LIST_TASKS, false)) {
                    when (val list = bridgeClient.listRunningTasks()) {
                        is AppResult.Success ->
                            Log.e(TAG, "task list OK count=${list.data.size} tasks=${list.data.joinToString { "${it.cli}/${it.id.take(8)}/${it.elapsedMs}ms" }}")
                        is AppResult.Failure ->
                            Log.e(TAG, "task list FAILED ${list.technicalDetail}")
                    }
                }
                if (intent.getBooleanExtra(EXTRA_TEST_FILEOPS, false)) {
                    // 真机验证只读/写文件 + 会话列表（经加密通道）
                    when (val ld = bridgeClient.listDir("")) {
                        is AppResult.Success -> Log.e(TAG, "fileops list_dir OK path=${ld.data.path} entries=${ld.data.entries.size}")
                        is AppResult.Failure -> Log.e(TAG, "fileops list_dir FAILED ${ld.technicalDetail}")
                    }
                    val wp = "D:/nbtest-fileops.txt"
                    when (val w = bridgeClient.writeFile(wp, "核弹男孩真机写测试 nbcontent")) {
                        is AppResult.Success -> Log.e(TAG, "fileops write OK ${w.data.path} ${w.data.bytes}B")
                        is AppResult.Failure -> Log.e(TAG, "fileops write FAILED ${w.technicalDetail}")
                    }
                    when (val r = bridgeClient.readFile(wp)) {
                        is AppResult.Success -> Log.e(TAG, "fileops read OK content=${r.data.content.take(60)}")
                        is AppResult.Failure -> Log.e(TAG, "fileops read FAILED ${r.technicalDetail}")
                    }
                    when (val s = bridgeClient.listSessions(3, null)) {
                        is AppResult.Success -> Log.e(TAG, "fileops list_sessions OK count=${s.data.size} first=${s.data.firstOrNull()?.preview?.take(30)}")
                        is AppResult.Failure -> Log.e(TAG, "fileops list_sessions FAILED ${s.technicalDetail}")
                    }
                }
                if (runCli.isNotBlank() && runPrompt.isNotBlank()) {
                    val runSession = intent.getStringExtra(EXTRA_RUN_SESSION)?.trim().orEmpty()
                    Log.e(TAG, "test run start cli=$runCli promptLen=${runPrompt.length} resume=${runSession.take(8)}")
                    when (val run = bridgeClient.runCliTask(
                        cli = runCli, prompt = runPrompt,
                        sessionId = runSession.takeIf { it.isNotBlank() },
                    )) {
                        is AppResult.Success ->
                            Log.e(TAG, "test run OK exit=${run.data.exitCode} ${run.data.durationMs}ms session=${run.data.sessionId} result=${run.data.result.take(200)}")
                        is AppResult.Failure ->
                            Log.e(TAG, "test run FAILED code=${run.error.code} detail=${run.technicalDetail}")
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "NuclearBoyDebugPcBridge"
        const val ACTION_SAVE_PC_BRIDGE = "com.nuclearboy.app.DEBUG_SAVE_PC_BRIDGE"
        const val EXTRA_URL = "url"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_ENABLED = "enabled"
        const val EXTRA_ENCRYPT = "encrypt"
        const val EXTRA_TEST_FILEOPS = "test_fileops"
        const val EXTRA_RUN_CLI = "run_cli"
        const val EXTRA_RUN_PROMPT = "run_prompt"
        const val EXTRA_RUN_SESSION = "run_session"
        const val EXTRA_LIST_TASKS = "list_tasks"
    }
}
