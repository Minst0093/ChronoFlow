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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.presentation.CalendarViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekViewScreen(
    viewModel: CalendarViewModel,
    onDayClick: (LocalDate) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

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
                        }
                    }
                }
            }

            // 时间轴：每小时一行
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
                            // 点击事件可以跳转到日视图
                            onDayClick(event.startTime.date)
                        },
                    )
                }
            }
        }
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
                val eventStartDate = event.startTime.date
                val eventStartHour = event.startTime.hour
                val eventEndHour = event.endTime.hour
                eventStartDate == date && hour in eventStartHour..eventEndHour
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFF5F5F5))
                    .padding(2.dp),
            ) {
                if (dayEvents.isNotEmpty()) {
                    // 简单显示：如果有事件，显示第一个事件的标题
                    val firstEvent = dayEvents.first()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                when (firstEvent.type) {
                                    com.minst.chronoflow.domain.model.EventType.STUDY -> Color(0xFFE3F2FD)
                                    com.minst.chronoflow.domain.model.EventType.WORK -> Color(0xFFFFF3E0)
                                    com.minst.chronoflow.domain.model.EventType.LIFE -> Color(0xFFE8F5E9)
                                    com.minst.chronoflow.domain.model.EventType.HEALTH -> Color(0xFFFCE4EC)
                                    com.minst.chronoflow.domain.model.EventType.ENTERTAINMENT -> Color(0xFFF3E5F5)
                                },
                            )
                            .clickable { onEventClick(firstEvent) }
                            .padding(4.dp),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        Text(
                            text = firstEvent.title,
                            fontSize = 10.sp,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

