package com.minst.chronoflow.data.local

import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.repository.EventRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate

class InMemoryEventRepository : EventRepository {

    private val events = mutableListOf<CalendarEvent>()
    private val mutex = Mutex()

    override suspend fun getEvents(start: LocalDate, end: LocalDate): List<CalendarEvent> =
        mutex.withLock {
            events.filter { event ->
                val eventStartDate = event.startTime.date
                val eventEndDate = event.endTime.date
                // If event is recurring, include it if it starts on or before the window end
                if (event.recurrence != null) {
                    eventStartDate <= end
                } else {
                    // 与 [start, end] 区间有交集的事件
                    !(eventEndDate < start || eventStartDate > end)
                }
            }
        }

    override suspend fun saveEvent(event: CalendarEvent) {
        mutex.withLock {
            val index = events.indexOfFirst { it.id == event.id }
            if (index >= 0) {
                events[index] = event
            } else {
                events += event
            }
        }
    }

    override suspend fun deleteEvent(id: String) {
        mutex.withLock {
            events.removeAll { it.id == id }
        }
    }
}


