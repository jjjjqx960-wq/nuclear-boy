package com.nuclearboy.app.diagnostics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nuclearboy.app.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DiagnosticsReceiver : BroadcastReceiver() {

    @Inject lateinit var diagnostics: AppDiagnostics

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RUN_DIAGNOSTICS) return
        if (!BuildConfig.DEBUG) {
            Log.e("NuclearBoy", "[DiagReceiver] ignored in non-debug build")
            return
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.e("NuclearBoy", "[DiagReceiver] diagnostics start")
                val results = diagnostics.runAll()
                val failed = results.count { it.status == DiagnosticStatus.FAIL }
                val warn = results.count { it.status == DiagnosticStatus.WARN }
                results.forEach { result ->
                    Log.e(
                        "NuclearBoy",
                        "[DiagReceiver] ${result.status} ${result.name}: ${result.message}; durationMs=${result.durationMs}; detailLen=${result.detail.length}",
                    )
                }
                Log.e("NuclearBoy", "[DiagReceiver] diagnostics complete total=${results.size} failed=$failed warn=$warn")
            } catch (e: Exception) {
                Log.e("NuclearBoy", "[DiagReceiver] diagnostics crashed: ${e.message}", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_RUN_DIAGNOSTICS = "com.nuclearboy.app.RUN_DIAGNOSTICS"
    }
}
