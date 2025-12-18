package com.minst.chronoflow.domain.repository

import com.minst.chronoflow.domain.model.CalendarEvent
import kotlinx.datetime.LocalDate

interface EventRepository {
    suspend fun getEvents(start: LocalDate, end: LocalDate): List<CalendarEvent>
    suspend fun saveEvent(event: CalendarEvent)
    suspend fun deleteEvent(id: String)
}


