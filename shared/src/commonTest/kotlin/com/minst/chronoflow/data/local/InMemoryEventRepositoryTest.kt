package com.minst.chronoflow.data.local

import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.EventType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryEventRepositoryTest {

    private val repository = InMemoryEventRepository()

    @Test
    fun `保存和查询事件`() = runTest {
        val event = CalendarEvent(
            id = "e1",
            title = "测试事件",
            description = "描述",
            startTime = LocalDateTime(2025, 1, 1, 10, 0),
            endTime = LocalDateTime(2025, 1, 1, 11, 0),
            type = EventType.STUDY,
            intensity = 3,
        )

        repository.saveEvent(event)
        val events = repository.getEvents(
            LocalDate(2025, 1, 1),
            LocalDate(2025, 1, 1),
        )

        assertEquals(1, events.size)
        assertEquals("e1", events[0].id)
        assertEquals("测试事件", events[0].title)
    }

    @Test
    fun `更新已存在的事件`() = runTest {
        val event1 = CalendarEvent(
            id = "e1",
            title = "原始标题",
            startTime = LocalDateTime(2025, 1, 1, 10, 0),
            endTime = LocalDateTime(2025, 1, 1, 11, 0),
            type = EventType.STUDY,
            intensity = 3,
        )

        val event2 = CalendarEvent(
            id = "e1",
            title = "更新标题",
            startTime = LocalDateTime(2025, 1, 1, 10, 0),
            endTime = LocalDateTime(2025, 1, 1, 11, 0),
            type = EventType.WORK,
            intensity = 4,
        )

        repository.saveEvent(event1)
        repository.saveEvent(event2)

        val events = repository.getEvents(
            LocalDate(2025, 1, 1),
            LocalDate(2025, 1, 1),
        )

        assertEquals(1, events.size)
        assertEquals("更新标题", events[0].title)
        assertEquals(EventType.WORK, events[0].type)
        assertEquals(4, events[0].intensity)
    }

    @Test
    fun `删除事件`() = runTest {
        val event = CalendarEvent(
            id = "e1",
            title = "待删除事件",
            startTime = LocalDateTime(2025, 1, 1, 10, 0),
            endTime = LocalDateTime(2025, 1, 1, 11, 0),
            type = EventType.STUDY,
            intensity = 3,
        )

        repository.saveEvent(event)
        repository.deleteEvent("e1")

        val events = repository.getEvents(
            LocalDate(2025, 1, 1),
            LocalDate(2025, 1, 1),
        )

        assertEquals(0, events.size)
    }

    @Test
    fun `查询跨天事件`() = runTest {
        val event = CalendarEvent(
            id = "e1",
            title = "跨天事件",
            startTime = LocalDateTime(2025, 1, 1, 23, 0),
            endTime = LocalDateTime(2025, 1, 2, 1, 0),
            type = EventType.WORK,
            intensity = 3,
        )

        repository.saveEvent(event)

        // 查询第一天
        val day1Events = repository.getEvents(
            LocalDate(2025, 1, 1),
            LocalDate(2025, 1, 1),
        )
        assertEquals(1, day1Events.size)

        // 查询第二天
        val day2Events = repository.getEvents(
            LocalDate(2025, 1, 2),
            LocalDate(2025, 1, 2),
        )
        assertEquals(1, day2Events.size)

        // 查询日期范围
        val rangeEvents = repository.getEvents(
            LocalDate(2025, 1, 1),
            LocalDate(2025, 1, 2),
        )
        assertEquals(1, rangeEvents.size)
    }

    @Test
    fun `查询日期范围`() = runTest {
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
                startTime = LocalDateTime(2025, 1, 3, 14, 0),
                endTime = LocalDateTime(2025, 1, 3, 15, 0),
                type = EventType.WORK,
                intensity = 3,
            ),
            CalendarEvent(
                id = "e3",
                title = "事件3",
                startTime = LocalDateTime(2025, 1, 5, 9, 0),
                endTime = LocalDateTime(2025, 1, 5, 10, 0),
                type = EventType.LIFE,
                intensity = 1,
            ),
        )

        events.forEach { repository.saveEvent(it) }

        // 查询 1月1日到1月3日
        val result = repository.getEvents(
            LocalDate(2025, 1, 1),
            LocalDate(2025, 1, 3),
        )

        assertEquals(2, result.size)
    }
}

