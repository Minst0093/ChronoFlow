package com.minst.chronoflow.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidNotificationScheduler.ACTION_SHOW_REMINDER) return

        val eventId = intent.getStringExtra(AndroidNotificationScheduler.EXTRA_EVENT_ID) ?: return
        val title = intent.getStringExtra(AndroidNotificationScheduler.EXTRA_EVENT_TITLE) ?: "事件提醒"

        val channelId = "chrono_flow_reminder"
        createChannelIfNeeded(context, channelId)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ChronoFlow 提醒")
            .setContentText(title)
            .setAutoCancel(true)
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


