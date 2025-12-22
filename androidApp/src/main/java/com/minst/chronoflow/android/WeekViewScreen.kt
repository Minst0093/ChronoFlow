package com.minst.chronoflow.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.util.isAllDayEvent
import com.minst.chronoflow.domain.model.EventType
import com.minst.chronoflow.presentation.CalendarViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WeekViewScreen(
    viewModel: CalendarViewModel,
    onDayClick: (LocalDate) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var showEditForm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "周视图 (${state.selectedWeekStart} - ${state.selectedWeekStart.plus(DatePeriod(days = 6))})",
                    )
                },
                actions = {
                    Button(onClick = { viewModel.onPreviousWeek() }) {
                        Text("上一周")
                    }
                    Button(onClick = { viewModel.onNextWeek() }) {
                        Text("下一周")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp),
        ) {
            // 表头：周一到周日
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                (0..6).forEach { dayOffset ->
                    val date = state.selectedWeekStart.plus(DatePeriod(days = dayOffset))
                    val dayName = when (date.dayOfWeek) {
                        DayOfWeek.MONDAY -> "周一"
                        DayOfWeek.TUESDAY -> "周二"
                        DayOfWeek.WEDNESDAY -> "周三"
                        DayOfWeek.THURSDAY -> "周四"
                        DayOfWeek.FRIDAY -> "周五"
                        DayOfWeek.SATURDAY -> "周六"
                        DayOfWeek.SUNDAY -> "周日"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDayClick(date) }
                            .padding(4.dp)
                            .background(
                                if (date == state.selectedDate) Color.LightGray else Color.Transparent,
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dayName,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(text = "${date.dayOfMonth}")
                            // show lunar short text if available and enabled; fallback to viewModel lookup
                            val ds = state.daySummaries.firstOrNull { it.date == date }
                            if (state.showLunar) {
                                val lunarText = ds?.lunarText ?: viewModel.getLunarInfo(date)?.lunarShort
                                if (!lunarText.isNullOrBlank()) {
                                    Text(text = lunarText, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // 时间轴：每小时一行，周切换时添加过渡动画（简单 fade/slide）
            // 全天事件行
            Row(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                (0..6).forEach { dayOffset ->
                    val date = state.selectedWeekStart.plus(DatePeriod(days = dayOffset))
                    val allDayForDay = state.eventsOfSelectedWeek.filter { ev -> isAllDayEvent(ev, date) }.filter { it.startTime.date <= date && it.endTime.date >= date }

                    Box(modifier = Modifier.weight(1f).padding(4.dp)) {
                        Row {
                            allDayForDay.take(2).forEach { ev ->
                                Card(modifier = Modifier.padding(end = 4.dp)) {
                                    Text(ev.title, modifier = Modifier.padding(6.dp), fontSize = 12.sp)
                                }
                            }
                            if (allDayForDay.size > 2) {
                                Text("+${allDayForDay.size - 2}", modifier = Modifier.align(Alignment.CenterVertically), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            AnimatedContent(targetState = state.selectedWeekStart, transitionSpec = {
                fadeIn(tween(180)).togetherWith(fadeOut(tween(180)))
            }) { _ ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items((0..23).toList()) { hour ->
                        WeekViewHourRow(
                            hour = hour,
                            weekStart = state.selectedWeekStart,
                            events = state.eventsOfSelectedWeek,
                            onEventClick = { event ->
                                // 点击事件显示详情对话框
                                selectedEvent = event
                            },
                        )
                    }
                }
            }
        }
    }

    // 事件详情对话框
    selectedEvent?.let { event ->
        val lunarInfo = viewModel.getLunarInfo(event.startTime.date)
        EventDetailDialog(
            event = event,
            onDismiss = { selectedEvent = null },
            onEdit = {
                selectedEvent = null
                showEditForm = true
            },
            onDelete = {
                viewModel.onDeleteEvent(event.id)
            },
            lunarShort = lunarInfo?.lunarShort,
            lunarFull = lunarInfo?.lunarDate,
            solarTerm = lunarInfo?.solarTerm,
        )
    }

    // 编辑事件表单
    if (showEditForm && selectedEvent != null) {
        EventFormDialog(
            event = selectedEvent,
            onDismiss = {
                showEditForm = false
                selectedEvent = null
            },
            onSave = { },
            onUpdate = { updatedEvent ->
                viewModel.onUpdateEvent(updatedEvent)
            },
        )
    }
}

@Composable
private fun WeekViewHourRow(
    hour: Int,
    weekStart: LocalDate,
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // 时间标签
        Box(
            modifier = Modifier
                .width(50.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = String.format("%02d:00", hour),
                fontWeight = FontWeight.Medium,
            )
        }

        // 七天的事件块
        (0..6).forEach { dayOffset ->
            val date = weekStart.plus(DatePeriod(days = dayOffset))
            val dayEvents = events.filter { event ->
                // exclude all-day events from hourly grid
                val startsAtMidnight = event.startTime.hour == 0 && event.startTime.minute == 0
                val endsAtEndOfDay = event.endTime.hour == 23 && event.endTime.minute == 59
                val isAllDayEvent = (event.recurrence?.allDay == true) ||
                        (event.startTime.date <= date && event.endTime.date >= date && startsAtMidnight && endsAtEndOfDay)
                if (isAllDayEvent) return@filter false
                // 判断事件与当前时间段（date hour）是否有重叠
                val tz = TimeZone.currentSystemDefault()
                val segmentStart = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, hour, 0)
                val segmentEnd = segmentStart.toInstant(tz).plus(1, DateTimeUnit.HOUR).toLocalDateTime(tz)
                event.startTime < segmentEnd && event.endTime > segmentStart
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFF5F5F5))
                    .padding(2.dp),
            ) {
                if (dayEvents.isNotEmpty()) {
                    // 更合理的并排布局：按数量平均分配宽度，支持点击与简单过渡
                    Row(modifier = Modifier.fillMaxSize()) {
                        dayEvents.forEach { event ->
                            val typeColor = getEventTypeColor(event.type)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onEventClick(event) }
                                    .padding(2.dp),
                            ) {
                                androidx.compose.animation.AnimatedContent(targetState = event.id.hashCode()) { _ ->
                                    Card(
                                        modifier = Modifier.fillMaxSize(),
                                        colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.35f))
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp)) {
                                            Text(
                                                text = event.title,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 4,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (dayEvents.size > 3) {
                        Text(
                            text = "+${dayEvents.size - 3}",
                            fontSize = 8.sp,
                            modifier = Modifier.padding(2.dp),
                        )
                    }
                }
            }
        }
    }
}

// moved to EventTypeUtils.kt

