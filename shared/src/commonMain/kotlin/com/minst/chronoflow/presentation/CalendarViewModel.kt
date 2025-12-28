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
import kotlinx.coroutines.withContext
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
        val currentState = _uiState.value
        val newWeekStart = date.startOfWeek()
        _uiState.value = currentState.copy(selectedDate = date, selectedWeekStart = newWeekStart)
        // refresh both day and week views so collapsed week reflects the newly selected date's week
        refreshEventsForSelectedDate()
        refreshEventsForSelectedWeek()
    }

    fun onPreviousWeek() {
        val currentState = _uiState.value
        val newWeekStart = currentState.selectedWeekStart.minus(DatePeriod(days = 7))
        // 将selectedDate设置为新周的中间日期（周三），以便CalendarHeader正确显示年月周信息
        val newSelectedDate = newWeekStart.plus(DatePeriod(days = 2))
        _uiState.value = currentState.copy(selectedWeekStart = newWeekStart, selectedDate = newSelectedDate)
        refreshEventsForSelectedWeek()
    }

    fun onNextWeek() {
        val currentState = _uiState.value
        val newWeekStart = currentState.selectedWeekStart.plus(DatePeriod(days = 7))
        // 将selectedDate设置为新周的中间日期（周三），以便CalendarHeader正确显示年月周信息
        val newSelectedDate = newWeekStart.plus(DatePeriod(days = 2))
        _uiState.value = currentState.copy(selectedWeekStart = newWeekStart, selectedDate = newSelectedDate)
        refreshEventsForSelectedWeek()
    }

    fun onPreviousMonth() {
        val currentState = _uiState.value
        val newMonthStart = currentState.selectedMonthStart.minus(DatePeriod(months = 1))
        // preserve day-of-month if possible (e.g., 31 -> next month may have fewer days)
        val oldDay = currentState.selectedDate.dayOfMonth
        val nextMonthStart = newMonthStart.plus(DatePeriod(months = 1))
        val lastDayOfNewMonth = nextMonthStart.minus(DatePeriod(days = 1)).dayOfMonth
        val newDay = kotlin.math.min(oldDay, lastDayOfNewMonth)
        val newSelectedDate = newMonthStart.withDayOfMonth(newDay)
        _uiState.value = currentState.copy(selectedMonthStart = newMonthStart, selectedDate = newSelectedDate)
        refreshForCurrentMonth()
        refreshEventsForSelectedDate()
    }

    fun setShowLunar(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showLunar = enabled)
    }

    fun setDateSelectorExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(dateSelectorExpanded = expanded)
    }

    fun onNextMonth() {
        val currentState = _uiState.value
        val newMonthStart = currentState.selectedMonthStart.plus(DatePeriod(months = 1))
        val oldDay = currentState.selectedDate.dayOfMonth
        val nextMonthStart = newMonthStart.plus(DatePeriod(months = 1))
        val lastDayOfNewMonth = nextMonthStart.minus(DatePeriod(days = 1)).dayOfMonth
        val newDay = kotlin.math.min(oldDay, lastDayOfNewMonth)
        val newSelectedDate = newMonthStart.withDayOfMonth(newDay)
        _uiState.value = currentState.copy(selectedMonthStart = newMonthStart, selectedDate = newSelectedDate)
        refreshForCurrentMonth()
        refreshEventsForSelectedDate()
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

                // perform CPU-heavy expansion and aggregation off the main thread
                val (expandedEvents, daySummaries, weekSummaries) = withContext(Dispatchers.Default) {
                    val expander = com.minst.chronoflow.domain.engine.DefaultRecurrenceExpander()
                    val expanded = events.flatMap { ev -> expander.expand(ev, monthStart, monthEnd, 500) }
                    val ds = aggregationEngine.aggregateByDay(expanded)
                    val ws = aggregationEngine.aggregateByWeek(expanded)
                    Triple(expanded, ds, ws)
                }

                // enrich with lunar info (cheap synchronous calls) if service provided
                var enrichedDaySummaries = daySummaries
                if (lunarService != null) {
                    enrichedDaySummaries = enrichedDaySummaries.map { ds ->
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
                    daySummaries = enrichedDaySummaries,
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

                // expand recurrences off main thread
                val expandedEvents = withContext(Dispatchers.Default) {
                    val expander = com.minst.chronoflow.domain.engine.DefaultRecurrenceExpander()
                    events.flatMap { ev -> expander.expand(ev, date, date, 200) }
                }

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

                val expandedEvents = withContext(Dispatchers.Default) {
                    val expander = com.minst.chronoflow.domain.engine.DefaultRecurrenceExpander()
                    events.flatMap { ev -> expander.expand(ev, weekStart, weekEnd, 500) }
                }

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


