package com.minst.chronoflow.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String? = null,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val type: EventType,
    val intensity: Int,
    val reminder: ReminderConfig? = null,
) {
    init {
        require(startTime < endTime) { "startTime must be before endTime" }
        require(intensity in 1..5) { "intensity must be in 1..5" }
    }
}

enum class EventType {
    STUDY,
    WORK,
    LIFE,
    HEALTH,
    ENTERTAINMENT,
}

data class ReminderConfig(
    val minutesBefore: Int,
)

data class DaySummary(
    val date: LocalDate,
    val eventCount: Int,
    val totalIntensity: Int,
)

data class WeekSummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val eventCount: Int,
    val totalIntensity: Int,
)


