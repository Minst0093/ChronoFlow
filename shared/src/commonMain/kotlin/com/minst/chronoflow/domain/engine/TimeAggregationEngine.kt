package com.minst.chronoflow.domain.engine

import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.DaySummary
import com.minst.chronoflow.domain.model.WeekSummary
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

interface TimeAggregationEngine {
    fun aggregateByDay(events: List<CalendarEvent>): List<DaySummary>
    fun aggregateByWeek(events: List<CalendarEvent>): List<WeekSummary>
}

class DefaultTimeAggregationEngine : TimeAggregationEngine {

    override fun aggregateByDay(events: List<CalendarEvent>): List<DaySummary> {
        if (events.isEmpty()) return emptyList()

        val byDate = mutableMapOf<LocalDate, MutableList<CalendarEvent>>()
        events.forEach { event ->
            val startDate = event.startTime.date
            val endDateExclusive = event.endTime.date.plus(DatePeriod(days = 1))
            var current = startDate
            while (current < endDateExclusive) {
                byDate.getOrPut(current) { mutableListOf() }.add(event)
                current = current.plus(DatePeriod(days = 1))
            }
        }

        return byDate.entries
            .sortedBy { it.key }
            .map { (date, dayEvents) ->
                DaySummary(
                    date = date,
                    eventCount = dayEvents.size,
                    totalIntensity = dayEvents.sumOf { it.intensity },
                    hasRecurring = dayEvents.any { it.recurrence != null },
                )
            }
    }

    override fun aggregateByWeek(events: List<CalendarEvent>): List<WeekSummary> {
        if (events.isEmpty()) return emptyList()

        val byWeek = mutableMapOf<LocalDate, MutableList<CalendarEvent>>()

        events.forEach { event ->
            val startDate = event.startTime.date
            val endDateExclusive = event.endTime.date.plus(DatePeriod(days = 1))
            var current = startDate
            while (current < endDateExclusive) {
                val weekStart = current.startOfWeek()
                byWeek.getOrPut(weekStart) { mutableListOf() }.add(event)
                current = current.plus(DatePeriod(days = 1))
            }
        }

        return byWeek.entries
            .sortedBy { it.key }
            .map { (weekStart, weekEvents) ->
                val weekEnd = weekStart.plus(DatePeriod(days = 6))
                WeekSummary(
                    weekStart = weekStart,
                    weekEnd = weekEnd,
                    eventCount = weekEvents.size,
                    totalIntensity = weekEvents.sumOf { it.intensity },
                )
            }
    }

    private fun LocalDate.startOfWeek(): LocalDate {
        var current = this
        while (current.dayOfWeek != DayOfWeek.MONDAY) {
            current = current.minus(DatePeriod(days = 1))
        }
        return current
    }
}


