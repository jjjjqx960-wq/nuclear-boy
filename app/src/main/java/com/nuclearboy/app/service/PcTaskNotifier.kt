package com.nuclearboy.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nuclearboy.app.MainActivity

/**
 * 远程电脑任务完成提醒。
 *
 * 远程 CLI 任务常耗时数分钟，用户很可能切到别的 App 等结果——任务结束（成功或
 * 失败）时发一条系统通知把人叫回来，借鉴 claudecodeui 的 run-completed 推送。
 * 实时进度仍走聊天工具卡片（[com.nuclearboy.common.ToolProgressBus]），这里只补
 * "后台也能知道好了没"的那一步。
 */
object PcTaskNotifier {

    private const val CHANNEL_ID = "nb_pc_task_done"
    private const val BASE_NOTIFICATION_ID = 4300
    private const val MAX_SUMMARY_LEN = 240

    /** 不同任务用递增 ID，避免后一条覆盖前一条未读提醒。 */
    private var seq = 0

    /**
     * 发一条任务完成提醒。无通知权限（Android 13+ 未授权）时静默跳过，不影响任务结果。
     *
     * @param cli 执行的 CLI 名（claude/codex/opencode）
     * @param success 任务是否成功（退出码 0）
     * @param summary 结果摘要（会截断）
     * @param durationMs 耗时毫秒
     */
    fun notifyComplete(
        context: Context,
        cli: String,
        success: Boolean,
        summary: String,
        durationMs: Long,
    ) {
        if (!hasPermission(context)) return
        createChannel(context)

        val seconds = (durationMs / 1000).coerceAtLeast(0)
        val title = if (success) "电脑端 $cli 搞定了 ✨" else "电脑端 $cli 没跑成 😕"
        val body = buildString {
            append("用时 ${seconds}s")
            val trimmed = summary.trim()
            if (trimmed.isNotEmpty()) {
                append(" · ")
                append(if (trimmed.length > MAX_SUMMARY_LEN) trimmed.take(MAX_SUMMARY_LEN) + "…" else trimmed)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val id = BASE_NOTIFICATION_ID + (seq++ % 50)
        NotificationManagerCompatSafe.notify(context, id, notification)
    }

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID, "电脑任务完成提醒", NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "远程电脑 CLI 任务结束时提醒" }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/** 包一层 try/catch：极端情况下 notify 仍可能抛（厂商 ROM 限额），不让它影响任务。 */
private object NotificationManagerCompatSafe {
    fun notify(context: Context, id: Int, notification: android.app.Notification) {
        runCatching {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(id, notification)
        }
    }
}
