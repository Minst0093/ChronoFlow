package com.minst.chronoflow.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.minst.chronoflow.domain.engine.DefaultReminderEngine
import com.minst.chronoflow.domain.engine.ReminderEngine
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.platform.NotificationScheduler
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class AndroidNotificationScheduler(
    private val context: Context,
    private val reminderEngine: ReminderEngine = DefaultReminderEngine(),
) : NotificationScheduler {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun schedule(event: CalendarEvent) {
        android.util.Log.d("AndroidNotificationScheduler", "schedule() called for event: ${event.id}, title: ${event.title}, reminder: ${event.reminder}")

        // 先取消旧的提醒（如果存在）
        cancel(event.id)

        val reminders = reminderEngine.calculateReminderTimes(event)
        android.util.Log.d("AndroidNotificationScheduler", "Calculated ${reminders.size} reminder times for event ${event.id}")
        if (reminders.isEmpty()) {
            android.util.Log.d("AndroidNotificationScheduler", "No reminders to schedule for event ${event.id}")
            return
        }

        val zone = TimeZone.currentSystemDefault()
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

        // 为每个提醒时间创建Alarm
        reminders.forEachIndexed { index, reminderTime ->
            val triggerAtMillis = reminderTime.toInstant(zone).toEpochMilliseconds()

            android.util.Log.d("AndroidNotificationScheduler", "Reminder $index: time=$reminderTime, millis=$triggerAtMillis, now=$now")

            // 只调度未来的提醒
            if (triggerAtMillis > now) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ACTION_SHOW_REMINDER
                    putExtra(EXTRA_EVENT_ID, event.id)
                    putExtra(EXTRA_EVENT_TITLE, event.title)
                    putExtra(EXTRA_EVENT_START_TIME, event.startTime.toString())
                }

                // 使用 event.id.hashCode() + index 作为 requestCode，确保每个提醒点都有唯一的PendingIntent
                val requestCode = event.id.hashCode() * 31 + index
                android.util.Log.d("AndroidNotificationScheduler", "Scheduling reminder for event=${event.id} at ${triggerAtMillis} (requestCode=$requestCode)")
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val alarmManager = alarmManager
                if (alarmManager == null) {
                    android.util.Log.e("AndroidNotificationScheduler", "AlarmManager is null!")
                    return@forEachIndexed
                }

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent,
                    )
                    android.util.Log.d("AndroidNotificationScheduler", "Successfully scheduled alarm for event ${event.id} at $triggerAtMillis")
                } catch (e: Exception) {
                    android.util.Log.e("AndroidNotificationScheduler", "Failed to schedule alarm for event ${event.id}", e)
                }
            } else {
                android.util.Log.d("AndroidNotificationScheduler", "Skipping past reminder for event ${event.id}: $triggerAtMillis <= $now")
            }
        }
    }

    override fun cancel(eventId: String) {
        // 取消该事件的所有提醒（扩大到最多支持100个提醒点以兼容重复事件的多次调度）
        for (index in 0..99) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_SHOW_REMINDER
            }
            val requestCode = eventId.hashCode() + index
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager?.cancel(pendingIntent)
        }
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "com.minst.chronoflow.SHOW_REMINDER"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_EVENT_START_TIME = "event_start_time"
    }
}


