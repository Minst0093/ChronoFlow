package com.minst.chronoflow.domain.engine

import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.EventType
import com.minst.chronoflow.domain.model.ReminderConfig
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun `没有提醒配置时返回空列表`() {
        val event = CalendarEvent(
            id = "e1",
            title = "无提醒事件",
            startTime = LocalDateTime(2025, 1, 1, 10, 0),
            endTime = LocalDateTime(2025, 1, 1, 11, 0),
            type = EventType.LIFE,
            intensity = 2,
            reminder = null,
        )

        val reminders = engine.calculateReminderTimes(event)
        assertTrue(reminders.isEmpty())
    }

    @Test
    fun `提醒时间为0或负数时返回空列表`() {
        val event1 = CalendarEvent(
            id = "e1",
            title = "零分钟提醒",
            startTime = LocalDateTime(2025, 1, 1, 10, 0),
            endTime = LocalDateTime(2025, 1, 1, 11, 0),
            type = EventType.WORK,
            intensity = 3,
            reminder = ReminderConfig(minutesBefore = 0),
        )

        val event2 = CalendarEvent(
            id = "e2",
            title = "负数提醒",
            startTime = LocalDateTime(2025, 1, 1, 10, 0),
            endTime = LocalDateTime(2025, 1, 1, 11, 0),
            type = EventType.WORK,
            intensity = 3,
            reminder = ReminderConfig(minutesBefore = -10),
        )

        assertTrue(engine.calculateReminderTimes(event1).isEmpty())
        assertTrue(engine.calculateReminderTimes(event2).isEmpty())
    }

    @Test
    fun `跨天的提醒时间计算正确`() {
        val event = CalendarEvent(
            id = "e1",
            title = "跨天提醒",
            startTime = LocalDateTime(2025, 1, 2, 1, 0), // 第二天凌晨1点
            endTime = LocalDateTime(2025, 1, 2, 2, 0),
            type = EventType.STUDY,
            intensity = 3,
            reminder = ReminderConfig(minutesBefore = 30), // 提前30分钟 = 第一天23:30
        )

        val reminders = engine.calculateReminderTimes(event)
        assertEquals(1, reminders.size)
        assertEquals(LocalDateTime(2025, 1, 1, 23, 30), reminders[0])
    }

    @Test
    fun `不同提前时间的提醒计算`() {
        val testCases = listOf(
            Pair(5, LocalDateTime(2025, 1, 1, 9, 55)),
            Pair(10, LocalDateTime(2025, 1, 1, 9, 50)),
            Pair(60, LocalDateTime(2025, 1, 1, 9, 0)),
        )

        testCases.forEach { (minutesBefore, expectedTime) ->
            val event = CalendarEvent(
                id = "e1",
                title = "测试",
                startTime = LocalDateTime(2025, 1, 1, 10, 0),
                endTime = LocalDateTime(2025, 1, 1, 11, 0),
                type = EventType.STUDY,
                intensity = 3,
                reminder = ReminderConfig(minutesBefore = minutesBefore),
            )

            val reminders = engine.calculateReminderTimes(event)
            assertEquals(1, reminders.size)
            assertEquals(expectedTime, reminders[0])
        }
    }

    @Test
    fun `recurring event returns future reminders`() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val start = now.plus(DatePeriod(days = 1))
        val event = CalendarEvent(
            id = "e_rec",
            title = "重复提醒测试",
            description = null,
            startTime = start,
            endTime = start.plus(DatePeriod(days = 0)),
            type = EventType.WORK,
            intensity = 3,
            reminder = ReminderConfig(minutesBefore = 10),
            recurrence = com.minst.chronoflow.domain.model.RecurrenceRule(freq = com.minst.chronoflow.domain.model.Frequency.DAILY, interval = 1, count = 3)
        )

        val reminders = engine.calculateReminderTimes(event)
        assertTrue(reminders.isNotEmpty())
    }
}


