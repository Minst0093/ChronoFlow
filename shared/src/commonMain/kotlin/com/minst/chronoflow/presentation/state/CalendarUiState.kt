package com.minst.chronoflow.presentation.state

import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.DaySummary
import com.minst.chronoflow.domain.model.WeekSummary
import kotlinx.datetime.LocalDate

enum class LoadStatus {
    Idle,
    Loading,
    Success,
    Error,
}

data class CalendarUiState(
    val selectedDate: LocalDate,
    val selectedWeekStart: LocalDate,
    val selectedMonthStart: LocalDate,
    val loadStatus: LoadStatus = LoadStatus.Idle,
    val daySummaries: List<DaySummary> = emptyList(),
    val weekSummaries: List<WeekSummary> = emptyList(),
    val eventsOfSelectedDate: List<CalendarEvent> = emptyList(),
    val eventsOfSelectedWeek: List<CalendarEvent> = emptyList(), // 当前周的事件列表，用于周视图
    val errorMessage: String? = null,
    val showLunar: Boolean = true,
    val dateSelectorExpanded: Boolean = true,
)


