package com.minst.chronoflow.domain.engine

import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.EventType
import com.minst.chronoflow.domain.model.ReminderConfig
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimeAggregationEngineTest {

    private val engine = DefaultTimeAggregationEngine()

    @Test
    fun `跨两天的事件会出现在两天的聚合中`() {
        val event = CalendarEvent(
            id = "e1",
            title = "跨天事件",
            description = null,
            startTime = LocalDateTime(2025, 1, 1, 23, 0),
            endTime = LocalDateTime(2025, 1, 2, 1, 0),
            type = EventType.WORK,
            intensity = 3,
            reminder = null,
        )

        val result = engine.aggregateByDay(listOf(event))
        assertEquals(2, result.size)
        assertEquals(1, result[0].eventCount)
        assertEquals(1, result[1].eventCount)
    }

    @Test
    fun `跨多天的事件会出现在所有相关天的聚合中`() {
        val event = CalendarEvent(
            id = "e1",
            title = "跨三天事件",
            description = null,
            startTime = LocalDateTime(2025, 1, 1, 10, 0),
            endTime = LocalDateTime(2025, 1, 3, 15, 0),
            type = EventType.WORK,
            intensity = 4,
            reminder = null,
        )

        val result = engine.aggregateByDay(listOf(event))
        assertEquals(3, result.size)
        result.forEach { summary ->
            assertEquals(1, summary.eventCount)
            assertEquals(4, summary.totalIntensity)
        }
    }

    @Test
    fun `没有事件时返回空列表`() {
        val result = engine.aggregateByDay(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `多个事件在同一天时正确聚合`() {
        val events = listOf(
            CalendarEvent(
                id = "e1",
                title = "事件1",
                startTime = LocalDateTime(2025, 1, 1, 10, 0),
                endTime = LocalDateTime(2025, 1, 1, 11, 0),
                type = EventType.STUDY,
                intensity = 2,
            ),
            CalendarEvent(
                id = "e2",
                title = "事件2",
                startTime = LocalDateTime(2025, 1, 1, 14, 0),
                endTime = LocalDateTime(2025, 1, 1, 15, 0),
                type = EventType.WORK,
                intensity = 3,
            ),
        )

        val result = engine.aggregateByDay(events)
        assertEquals(1, result.size)
        assertEquals(2, result[0].eventCount)
        assertEquals(5, result[0].totalIntensity) // 2 + 3
    }

    @Test
    fun `周聚合以周一为起始`() {
        // 2025-01-01 是周三
        val event = CalendarEvent(
            id = "e1",
            title = "周三事件",
            startTime = LocalDateTime(2025, 1, 1, 10, 0),
            endTime = LocalDateTime(2025, 1, 1, 11, 0),
            type = EventType.LIFE,
            intensity = 2,
        )

        val result = engine.aggregateByWeek(listOf(event))
        assertEquals(1, result.size)
        // 应该属于 2024-12-30（周一）那一周
        assertEquals(DayOfWeek.MONDAY, result[0].weekStart.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, result[0].weekEnd.dayOfWeek)
    }

    @Test
    fun `跨周的事件会出现在多个周的聚合中`() {
        // 从周日到周一的事件
        val event = CalendarEvent(
            id = "e1",
            title = "跨周事件",
            startTime = LocalDateTime(2025, 1, 5, 23, 0), // 周日
            endTime = LocalDateTime(2025, 1, 6, 1, 0), // 周一
            type = EventType.WORK,
            intensity = 3,
        )

        val result = engine.aggregateByWeek(listOf(event))
        assertEquals(2, result.size)
        result.forEach { summary ->
            assertEquals(1, summary.eventCount)
            assertEquals(3, summary.totalIntensity)
        }
    }

    @Test
    fun `周聚合统计正确`() {
        val events = listOf(
            CalendarEvent(
                id = "e1",
                title = "事件1",
                startTime = LocalDateTime(2025, 1, 1, 10, 0), // 周三
                endTime = LocalDateTime(2025, 1, 1, 11, 0),
                type = EventType.STUDY,
                intensity = 2,
            ),
            CalendarEvent(
                id = "e2",
                title = "事件2",
                startTime = LocalDateTime(2025, 1, 2, 14, 0), // 周四
                endTime = LocalDateTime(2025, 1, 2, 15, 0),
                type = EventType.WORK,
                intensity = 3,
            ),
        )

        val result = engine.aggregateByWeek(events)
        assertEquals(1, result.size)
        assertEquals(2, result[0].eventCount)
        assertEquals(5, result[0].totalIntensity)
    }
}


