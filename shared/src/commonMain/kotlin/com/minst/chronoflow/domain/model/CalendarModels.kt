package com.minst.chronoflow.domain.model

import kotlinx.datetime.DayOfWeek
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
    val recurrence: RecurrenceRule? = null,
    val allDay: Boolean = false,
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

enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

/**
 * RecurrenceRule represents a simplified recurrence model suitable for persistence as JSON/text.
 * - freq: recurrence frequency
 * - interval: step (e.g. 2 = every 2 units)
 * - byDay: optional list of weekdays for WEEKLY rules
 * - count / until: mutually exclusive end conditions (either count or until may be non-null)
 * - allDay: whether occurrences are all-day
 */
data class RecurrenceRule(
    val freq: Frequency,
    val interval: Int = 1,
    val byDay: List<DayOfWeek>? = null,
    val count: Int? = null,
    val until: LocalDateTime? = null,
    val allDay: Boolean = false,
)

data class DaySummary(
    val date: LocalDate,
    val eventCount: Int,
    val totalIntensity: Int,
    val hasRecurring: Boolean = false,
    // Minimal lunar display fields for UI consumption.
    // `lunarText` is a short string for month/day (e.g. "初一") and `hasLunar` indicates presence.
    val hasLunar: Boolean = false,
    val lunarText: String? = null,
)

data class WeekSummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val eventCount: Int,
    val totalIntensity: Int,
)


