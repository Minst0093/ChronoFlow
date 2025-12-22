package com.minst.chronoflow.presentation

import com.minst.chronoflow.domain.engine.DefaultReminderEngine
import com.minst.chronoflow.domain.engine.DefaultTimeAggregationEngine
import com.minst.chronoflow.domain.engine.ReminderEngine
import com.minst.chronoflow.domain.engine.TimeAggregationEngine
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.repository.EventRepository
import com.minst.chronoflow.platform.NotificationScheduler
import com.minst.chronoflow.presentation.state.CalendarUiState
import com.minst.chronoflow.presentation.state.LoadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

data class CreateEventInput(
    val title: String,
    val description: String?,
    val startTime: kotlinx.datetime.LocalDateTime,
    val endTime: kotlinx.datetime.LocalDateTime,
    val type: com.minst.chronoflow.domain.model.EventType,
    val intensity: Int,
    val reminder: com.minst.chronoflow.domain.model.ReminderConfig?,
    val recurrence: com.minst.chronoflow.domain.model.RecurrenceRule? = null,
    val allDay: Boolean = false,
)

class CalendarViewModel(
    private val repository: EventRepository,
    private val aggregationEngine: TimeAggregationEngine = DefaultTimeAggregationEngine(),
    private val reminderEngine: ReminderEngine = DefaultReminderEngine(),
    private val notificationScheduler: NotificationScheduler? = null,
    private val lunarService: com.minst.chronoflow.domain.engine.LunarCalendarService? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {

    private val _uiState: MutableStateFlow<CalendarUiState> =
        MutableStateFlow(initialState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        // 初始化时加载所有视图所需的数据
        refreshForCurrentMonth()
        refreshEventsForSelectedWeek()
        refreshEventsForSelectedDate()
    }

    fun onDaySelected(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        refreshEventsForSelectedDate()
    }

    fun onPreviousWeek() {
        val currentState = _uiState.value
        val newWeekStart = currentState.selectedWeekStart.minus(DatePeriod(days = 7))
        _uiState.value = currentState.copy(selectedWeekStart = newWeekStart)
        refreshEventsForSelectedWeek()
    }

    fun onNextWeek() {
        val currentState = _uiState.value
        val newWeekStart = currentState.selectedWeekStart.plus(DatePeriod(days = 7))
        _uiState.value = currentState.copy(selectedWeekStart = newWeekStart)
        refreshEventsForSelectedWeek()
    }

    fun onPreviousMonth() {
        val currentState = _uiState.value
        val newMonthStart = currentState.selectedMonthStart.minus(DatePeriod(months = 1))
        _uiState.value = currentState.copy(selectedMonthStart = newMonthStart)
        refreshForCurrentMonth()
    }

    fun setShowLunar(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showLunar = enabled)
    }

    fun onNextMonth() {
        val currentState = _uiState.value
        val newMonthStart = currentState.selectedMonthStart.plus(DatePeriod(months = 1))
        _uiState.value = currentState.copy(selectedMonthStart = newMonthStart)
        refreshForCurrentMonth()
    }

    fun onCreateEvent(input: CreateEventInput) {
        scope.launch {
            try {
                val event = CalendarEvent(
                    id = generateId(),
                    title = input.title,
                    description = input.description,
                    startTime = input.startTime,
                    endTime = input.endTime,
                    type = input.type,
                    intensity = input.intensity,
                    reminder = input.reminder,
                    recurrence = input.recurrence,
                )
                repository.saveEvent(event)
                // 调度通知失败不应该影响事件本身的创建和 UI 更新
                runCatching { notificationScheduler?.schedule(event) }
                refreshAll()
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    loadStatus = LoadStatus.Error,
                    errorMessage = t.message,
                )
            }
        }
    }

    fun onUpdateEvent(event: CalendarEvent) {
        scope.launch {
            try {
                repository.saveEvent(event)
                // 更新通知失败同样不影响事件本身的更新
                runCatching { notificationScheduler?.schedule(event) }
                refreshAll()
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    loadStatus = LoadStatus.Error,
                    errorMessage = t.message,
                )
            }
        }
    }

    fun onDeleteEvent(id: String) {
        scope.launch {
            try {
                repository.deleteEvent(id)
                // 取消通知失败不影响事件删除与 UI 更新
                runCatching { notificationScheduler?.cancel(id) }
                refreshAll()
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    loadStatus = LoadStatus.Error,
                    errorMessage = t.message,
                )
            }
        }
    }

    private fun refreshAll() {
        refreshForCurrentMonth()
        refreshEventsForSelectedDate()
        refreshEventsForSelectedWeek()
    }

    private fun refreshForCurrentMonth() {
        scope.launch {
            val currentState = _uiState.value
            _uiState.value = _uiState.value.copy(loadStatus = LoadStatus.Loading)
            try {
                val monthStart = currentState.selectedMonthStart
                val monthEnd = monthStart.plus(DatePeriod(months = 1)).minusDays(1)
                val events = repository.getEvents(monthStart, monthEnd)
                // expand recurring events within the month window
                val expander = com.minst.chronoflow.domain.engine.DefaultRecurrenceExpander()
                val expandedEvents = events.flatMap { ev -> expander.expand(ev, monthStart, monthEnd, 500) }
                var daySummaries = aggregationEngine.aggregateByDay(expandedEvents)
                val weekSummaries = aggregationEngine.aggregateByWeek(expandedEvents)
                // enrich with lunar info (cheap synchronous calls) if service provided
                if (lunarService != null) {
                    daySummaries = daySummaries.map { ds ->
                        try {
                            val lunar = lunarService.getLunarInfo(ds.date)
                            ds.copy(hasLunar = true, lunarText = lunar.lunarShort)
                        } catch (t: Throwable) {
                            ds
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    loadStatus = LoadStatus.Success,
                    daySummaries = daySummaries,
                    weekSummaries = weekSummaries,
                    errorMessage = null,
                )
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    loadStatus = LoadStatus.Error,
                    errorMessage = t.message,
                )
            }
        }
    }

    private fun refreshEventsForSelectedDate() {
        scope.launch {
            val currentState = _uiState.value
            try {
                val date = currentState.selectedDate
                val events = repository.getEvents(date, date)
                val expander = com.minst.chronoflow.domain.engine.DefaultRecurrenceExpander()
                val expandedEvents = events.flatMap { ev -> expander.expand(ev, date, date, 200) }
                _uiState.value = _uiState.value.copy(eventsOfSelectedDate = expandedEvents)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    loadStatus = LoadStatus.Error,
                    errorMessage = t.message,
                )
            }
        }
    }

    private fun refreshEventsForSelectedWeek() {
        scope.launch {
            val currentState = _uiState.value
            try {
                val weekStart = currentState.selectedWeekStart
                val weekEnd = weekStart.plus(DatePeriod(days = 6))
                val events = repository.getEvents(weekStart, weekEnd)
                val expander = com.minst.chronoflow.domain.engine.DefaultRecurrenceExpander()
                val expandedEvents = events.flatMap { ev -> expander.expand(ev, weekStart, weekEnd, 500) }
                _uiState.value = _uiState.value.copy(eventsOfSelectedWeek = expandedEvents)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    loadStatus = LoadStatus.Error,
                    errorMessage = t.message,
                )
            }
        }
    }

    private fun initialState(): CalendarUiState {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val monthStart = today.withDayOfMonth(1)
        val weekStart = today.startOfWeek() // 对齐到周一
        return CalendarUiState(
            selectedDate = today,
            selectedWeekStart = weekStart,
            selectedMonthStart = monthStart,
        )
    }

    private fun LocalDate.startOfWeek(): LocalDate {
        var current = this
        while (current.dayOfWeek != DayOfWeek.MONDAY) {
            current = current.minus(DatePeriod(days = 1))
        }
        return current
    }

    private fun LocalDate.withDayOfMonth(day: Int): LocalDate =
        LocalDate(year, month, day)

    private fun LocalDate.minusDays(days: Int): LocalDate =
        this.minus(DatePeriod(days = days))

    private fun generateId(): String = Clock.System.now().toEpochMilliseconds().toString()

    // Expose lunar info lookup to UI (delegates to injected service).
    fun getLunarInfo(date: LocalDate): com.minst.chronoflow.domain.engine.LunarInfo? {
        return try {
            lunarService?.getLunarInfo(date)
        } catch (t: Throwable) {
            null
        }
    }
}


