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
        val reminders = reminderEngine.calculateReminderTimes(event)
        val first = reminders.minOrNull() ?: return

        val zone = TimeZone.currentSystemDefault()
        val triggerAtMillis = first.toInstant(zone).toEpochMilliseconds()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            putExtra(EXTRA_EVENT_ID, event.id)
            putExtra(EXTRA_EVENT_TITLE, event.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
    }

    override fun cancel(eventId: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager?.cancel(pendingIntent)
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "com.minst.chronoflow.SHOW_REMINDER"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
    }
}


