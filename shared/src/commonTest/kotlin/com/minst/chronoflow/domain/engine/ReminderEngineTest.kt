package com.minst.chronoflow.domain.engine

import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.EventType
import com.minst.chronoflow.domain.model.ReminderConfig
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderEngineTest {

    private val engine = DefaultReminderEngine()

    @Test
    fun `提前 N 分钟的提醒时间计算正确`() {
        val event = CalendarEvent(
            id = "e1",
            title = "测试提醒",
            description = null,
            startTime = LocalDateTime(2025, 1, 1, 10, 0),
            endTime = LocalDateTime(2025, 1, 1, 11, 0),
            type = EventType.STUDY,
            intensity = 3,
            reminder = ReminderConfig(minutesBefore = 30),
        )

        val reminders = engine.calculateReminderTimes(event)
        assertEquals(1, reminders.size)
        assertEquals(LocalDateTime(2025, 1, 1, 9, 30), reminders[0])
    }
}


