package com.minst.chronoflow.domain.engine

import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.Frequency
import com.minst.chronoflow.domain.model.RecurrenceRule
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit

interface RecurrenceExpander {
    /**
     * Expand a single event into occurrences within [windowStart, windowEnd] (inclusive).
     * Returns a list of CalendarEvent occurrences (copies) with derived ids.
     */
    fun expand(event: CalendarEvent, windowStart: LocalDate, windowEnd: LocalDate, maxOccurrences: Int = 100): List<CalendarEvent>
}

class DefaultRecurrenceExpander : RecurrenceExpander {
    override fun expand(event: CalendarEvent, windowStart: LocalDate, windowEnd: LocalDate, maxOccurrences: Int): List<CalendarEvent> {
        val rule: RecurrenceRule = event.recurrence ?: return listOfNotNull(if (event.startTime.date <= windowEnd && event.endTime.date >= windowStart) event else null)

        val occurrences = mutableListOf<CalendarEvent>()

        // Helper to add occurrence if it intersects window and respects until/count
        var added = 0

        when (rule.freq) {
            Frequency.DAILY -> {
                var currentStart = event.startTime
                var currentEnd = event.endTime
                while (currentStart.date <= windowEnd && added < maxOccurrences) {
                        if (currentStart.date >= windowStart && currentEnd.date >= windowStart && currentStart.date <= windowEnd) {
                        occurrences.add(event.copy(id = event.id, startTime = currentStart, endTime = currentEnd))
                        added++
                        if (rule.count != null && added >= rule.count) break
                    }
                    if (rule.until != null && currentStart > rule.until) break
                    val zone = TimeZone.currentSystemDefault()
                    currentStart = currentStart.toInstant(zone).plus(rule.interval.toLong(), DateTimeUnit.DAY, zone).toLocalDateTime(zone)
                    currentEnd = currentEnd.toInstant(zone).plus(rule.interval.toLong(), DateTimeUnit.DAY, zone).toLocalDateTime(zone)
                }
            }
            Frequency.WEEKLY -> {
                // If byDay is provided, generate per-week occurrences; otherwise repeat on same weekday
                val byDays: List<DayOfWeek> = rule.byDay ?: listOf(event.startTime.date.dayOfWeek)

                // Find the first week base (start of week containing event.startTime)
                var weekBase = event.startTime.date

                // We'll iterate weeks by interval, and for each week create occurrences for byDays
                var weekIndex = 0
                while (weekBase <= windowEnd && added < maxOccurrences) {
                    // For each weekday in byDays, create a candidate occurrence in this week
                    for (dow in byDays) {
                        // Compute date for this week's desired day
                        // Move from weekBase to that day: shift by difference in DayOfWeek values
                        val dayDiff = dow.ordinal - weekBase.dayOfWeek.ordinal
                        val candidateDate = weekBase.plus(DatePeriod(days = dayDiff))
                        val candidateStart = LocalDateTime(candidateDate.year, candidateDate.monthNumber, candidateDate.dayOfMonth, event.startTime.hour, event.startTime.minute)

                        // compute duration between original start and end in minutes and apply to candidateStart
                        val zone = TimeZone.currentSystemDefault()
                        val durationMillis = event.endTime.toInstant(zone).toEpochMilliseconds() - event.startTime.toInstant(zone).toEpochMilliseconds()
                        val durationMinutes = (durationMillis / 60000)
                        val candidateEnd = candidateStart.toInstant(zone).plus(durationMinutes, DateTimeUnit.MINUTE, zone).toLocalDateTime(zone)

                        if (candidateStart.date >= windowStart && candidateStart.date <= windowEnd) {
                            if (rule.until != null && candidateStart > rule.until) continue
                            occurrences.add(event.copy(id = event.id, startTime = candidateStart, endTime = candidateEnd))
                            added++
                            if (rule.count != null && added >= rule.count) break
                            if (added >= maxOccurrences) break
                        }
                    }
                    if (rule.count != null && added >= rule.count) break
                    // advance weekBase by interval weeks
                    weekBase = weekBase.plus(DatePeriod(days = 7 * rule.interval))
                    weekIndex++
                }
            }
            Frequency.MONTHLY -> {
                var currentStart = event.startTime
                var currentEnd = event.endTime
                while (currentStart.date <= windowEnd && added < maxOccurrences) {
                        if (currentStart.date >= windowStart && currentStart.date <= windowEnd) {
                        occurrences.add(event.copy(id = event.id, startTime = currentStart, endTime = currentEnd))
                        added++
                        if (rule.count != null && added >= rule.count) break
                    }
                    if (rule.until != null && currentStart > rule.until) break
                    // add months by operating on the date portion and reconstructing LocalDateTime
                    val nextStartDate = currentStart.date.plus(DatePeriod(months = rule.interval))
                    currentStart = LocalDateTime(nextStartDate.year, nextStartDate.monthNumber, nextStartDate.dayOfMonth, currentStart.hour, currentStart.minute)
                    val nextEndDate = currentEnd.date.plus(DatePeriod(months = rule.interval))
                    currentEnd = LocalDateTime(nextEndDate.year, nextEndDate.monthNumber, nextEndDate.dayOfMonth, currentEnd.hour, currentEnd.minute)
                }
            }
            Frequency.YEARLY -> {
                var currentStart = event.startTime
                var currentEnd = event.endTime
                while (currentStart.date <= windowEnd && added < maxOccurrences) {
                        if (currentStart.date >= windowStart && currentStart.date <= windowEnd) {
                        occurrences.add(event.copy(id = event.id, startTime = currentStart, endTime = currentEnd))
                        added++
                        if (rule.count != null && added >= rule.count) break
                    }
                    if (rule.until != null && currentStart > rule.until) break
                    // add years by operating on the date portion and reconstructing LocalDateTime
                    val nextStartDateY = currentStart.date.plus(DatePeriod(years = rule.interval))
                    currentStart = LocalDateTime(nextStartDateY.year, nextStartDateY.monthNumber, nextStartDateY.dayOfMonth, currentStart.hour, currentStart.minute)
                    val nextEndDateY = currentEnd.date.plus(DatePeriod(years = rule.interval))
                    currentEnd = LocalDateTime(nextEndDateY.year, nextEndDateY.monthNumber, nextEndDateY.dayOfMonth, currentEnd.hour, currentEnd.minute)
                }
            }
        }

        return occurrences
    }
}


