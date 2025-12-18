package com.minst.chronoflow.domain.engine

import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.EventType
import com.minst.chronoflow.domain.model.ReminderConfig
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

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
}


