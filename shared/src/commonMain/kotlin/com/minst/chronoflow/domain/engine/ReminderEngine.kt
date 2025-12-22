package com.minst.chronoflow.domain.engine

import com.minst.chronoflow.domain.model.CalendarEvent
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.minst.chronoflow.domain.engine.DefaultRecurrenceExpander
import kotlinx.datetime.Clock
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod

interface ReminderEngine {
    fun calculateReminderTimes(event: CalendarEvent): List<LocalDateTime>
}

class DefaultReminderEngine : ReminderEngine {
    override fun calculateReminderTimes(event: CalendarEvent): List<LocalDateTime> {
        val config = event.reminder ?: return emptyList()
        if (config.minutesBefore <= 0) return emptyList()

        val zone = TimeZone.currentSystemDefault()

        // For recurring events, expand upcoming occurrences and compute reminder times for each occurrence
        if (event.recurrence != null) {
            val expander = DefaultRecurrenceExpander()
            val now = Clock.System.now().toLocalDateTime(zone)
            val windowStart = now.date
            val windowEnd = windowStart.plus(DatePeriod(months = 6))
            val occurrences = expander.expand(event, windowStart, windowEnd, 50)
            return occurrences.mapNotNull { occ ->
                val occInstant = occ.startTime.toInstant(zone)
                val reminderInstant = occInstant.minus(config.minutesBefore.toLong(), DateTimeUnit.MINUTE)
                val reminderLocal = reminderInstant.toLocalDateTime(zone)
                if (reminderInstant.toEpochMilliseconds() > Clock.System.now().toEpochMilliseconds()) reminderLocal else null
            }
        }

        // Single (non-recurring) event
        val eventStartInstant = event.startTime.toInstant(zone)
        val reminderInstant = eventStartInstant.minus(config.minutesBefore.toLong(), DateTimeUnit.MINUTE)
        return listOf(reminderInstant.toLocalDateTime(zone))
    }
}


