package com.minst.chronoflow.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidNotificationScheduler.ACTION_SHOW_REMINDER) return
        android.util.Log.d("ReminderReceiver", "onReceive intent=${intent.action}")

        val eventId = intent.getStringExtra(AndroidNotificationScheduler.EXTRA_EVENT_ID) ?: return
        val title = intent.getStringExtra(AndroidNotificationScheduler.EXTRA_EVENT_TITLE) ?: "事件提醒"
        val startTimeStr = intent.getStringExtra(AndroidNotificationScheduler.EXTRA_EVENT_START_TIME)

        val channelId = "chrono_flow_reminder"
        createChannelIfNeeded(context, channelId)

        // 创建点击通知后跳转到MainActivity的Intent
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("event_id", eventId)
            if (startTimeStr != null) {
                try {
                    val startTime = LocalDateTime.parse(startTimeStr)
                    val date = LocalDate(startTime.year, startTime.monthNumber, startTime.dayOfMonth)
                    putExtra("selected_date", date.toString())
                } catch (e: Exception) {
                    // 忽略解析错误
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ChronoFlow 提醒")
            .setContentText(title)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context)
            .notify(eventId.hashCode(), notification)
    }

    private fun createChannelIfNeeded(context: Context, channelId: String) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
        val existing = manager.getNotificationChannel(channelId)
        if (existing != null) return

        val channel = NotificationChannel(
            channelId,
            "日程提醒",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }
}


