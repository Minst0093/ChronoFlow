package com.minst.chronoflow.domain.util

import com.minst.chronoflow.domain.model.CalendarEvent
import kotlinx.datetime.LocalDate

/**
 * Returns true if the given event should be considered an "all-day" event for the given date.
 * Criteria:
 * - event.allDay == true (explicit persisted flag), OR
 * - recurrence.allDay == true (recurrence rule marks allDay), OR
 * - event start is at 00:00 and end is at 23:59 and it overlaps the date.
 */
fun isAllDayEvent(event: CalendarEvent, date: LocalDate): Boolean {
    // explicit model flag
    if (event.allDay) return true

    // recurrence-marked all-day
    if (event.recurrence?.allDay == true) return true

    // heuristic: starts at 00:00 and ends at 23:59 and spans the date
    val startsAtMidnight = event.startTime.hour == 0 && event.startTime.minute == 0
    val endsAtEndOfDay = event.endTime.hour == 23 && event.endTime.minute == 59
    val spansDate = event.startTime.date <= date && event.endTime.date >= date
    return startsAtMidnight && endsAtEndOfDay && spansDate
}


