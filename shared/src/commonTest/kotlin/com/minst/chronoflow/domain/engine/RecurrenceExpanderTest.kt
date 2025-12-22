package com.minst.chronoflow.domain.engine

import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.EventType
import com.minst.chronoflow.domain.model.RecurrenceRule
import com.minst.chronoflow.domain.model.Frequency
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class RecurrenceExpanderTest {

    private val expander = DefaultRecurrenceExpander()

    @Test
    fun `daily recurrence expands expected count`() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val start = now.plus(DatePeriod(days = 1))
        val end = start.plus(DatePeriod(days = 0))

        val event = CalendarEvent(
            id = "e1",
            title = "daily",
            description = null,
            startTime = start,
            endTime = end,
            type = EventType.WORK,
            intensity = 3,
            reminder = null,
            recurrence = RecurrenceRule(freq = Frequency.DAILY, interval = 1, count = 3)
        )

        val windowStart = start.date
        val windowEnd = windowStart.plus(DatePeriod(days = 10))
        val occ = expander.expand(event, windowStart, windowEnd, 10)
        assertEquals(3, occ.size)
    }

    @Test
    fun `weekly recurrence byDay expands within window`() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val start = now.plus(DatePeriod(days = 1))
        val end = start.plus(DatePeriod(days = 0))

        val event = CalendarEvent(
            id = "e2",
            title = "weekly",
            description = null,
            startTime = start,
            endTime = end,
            type = EventType.WORK,
            intensity = 3,
            reminder = null,
            recurrence = RecurrenceRule(freq = Frequency.WEEKLY, interval = 1, byDay = listOf(start.date.dayOfWeek), count = 2)
        )

        val windowStart = start.date
        val windowEnd = windowStart.plus(DatePeriod(days = 14))
        val occ = expander.expand(event, windowStart, windowEnd, 10)
        assertEquals(2, occ.size)
    }
}


