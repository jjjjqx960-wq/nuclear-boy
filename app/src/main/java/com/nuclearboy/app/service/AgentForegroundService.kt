package com.nuclearboy.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.nuclearboy.app.MainActivity

/**
 * Foreground service that keeps the AI agent alive during background processing.
 * Started when AI begins thinking, stopped when response is ready.
 */
class AgentForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("action") ?: "thinking"
        val projectName = intent?.getStringExtra("project") ?: ""
        android.util.Log.e("NuclearBoy", "[FGService] onStartCommand — action=$action, project=$projectName")

        if (action == "stop") {
            android.util.Log.e("NuclearBoy", "[FGService] onStartCommand — stopping foreground service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createChannel()
        val notification = buildNotification(projectName, action)
        try {
            if (action == "ready") {
                // 回复已就绪：不再需要前台服务身份——把通知留在通知栏（DETACH 让它脱离前台服务
                // 生命周期继续显示），服务本身随即停止。之前这里也会调用 startForeground()，
                // 导致每轮对话答完后服务仍以前台服务身份常驻，一直到进程被系统杀掉。
                stopForeground(STOP_FOREGROUND_DETACH)
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification)
                stopSelf()
            } else {
                ServiceCompat.startForeground(
                    this, NOTIFICATION_ID, notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
                )
            }
            android.util.Log.e("NuclearBoy", "[FGService] onStartCommand — handled action=$action")
        } catch (e: Exception) {
            android.util.Log.e("NuclearBoy", "[FGService] onStartCommand — FAILED: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(project: String, action: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(if (project.isNotEmpty()) "核弹男孩 · $project" else "核弹男孩")
        .setContentText(if (action == "ready") "回复已就绪" else "AI 正在思考…")
        .setOngoing(action == "thinking")
        .setContentIntent(if (action == "ready") openAppIntent() else null)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "AI 处理状态", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    companion object {
        private const val CHANNEL_ID = "nb_agent_fg"
        private const val NOTIFICATION_ID = 4202

        fun start(context: Context, project: String? = null) {
            android.util.Log.e("NuclearBoy", "[FGService] companion start — project=$project")
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                putExtra("action", "thinking")
                putExtra("project", project ?: "")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun update(context: Context, summary: String, project: String? = null) {
            android.util.Log.e("NuclearBoy", "[FGService] companion update — summaryLen=${summary.length}, project=$project")
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                putExtra("action", "ready")
                putExtra("project", project ?: "")
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("NuclearBoy", "[FGService] companion update FAILED — ${e.message}")
                // Service might already be stopped
            }
        }

        fun stop(context: Context) {
            android.util.Log.e("NuclearBoy", "[FGService] companion stop")
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                putExtra("action", "stop")
            }
            context.startService(intent)
        }
    }
}
