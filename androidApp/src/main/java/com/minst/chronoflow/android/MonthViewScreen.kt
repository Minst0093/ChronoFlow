package com.minst.chronoflow.android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.DaySummary
import com.minst.chronoflow.presentation.CalendarViewModel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthViewScreen(
    viewModel: CalendarViewModel,
    onDayClick: (LocalDate) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showEventList by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val month = state.selectedMonthStart
                    Text(text = "${month.year}年${month.monthNumber}月")
                },
                actions = {
                    Button(onClick = { viewModel.onPreviousMonth() }) {
                        Text("上个月")
                    }
                    Button(onClick = { viewModel.onNextMonth() }) {
                        Text("下个月")
                    }
                    Button(onClick = { viewModel.setShowLunar(!state.showLunar) }) {
                        Text(if (state.showLunar) "隐藏农历" else "显示农历")
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
                listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").forEach { dayName ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = dayName,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // 日历网格
            val monthStart = state.selectedMonthStart
            val nextMonthStart = monthStart.plus(DatePeriod(months = 1))
            val monthEnd = nextMonthStart.minus(DatePeriod(days = 1))
            val firstDayOfWeek = monthStart.dayOfWeek
            val daysInMonth = monthEnd.dayOfMonth

            // 计算需要显示的所有日期（包括上个月末尾和下个月开头）
            val daysToShow = mutableListOf<LocalDate?>()
            
            // 上个月末尾的日期
            val daysBeforeMonth = when (firstDayOfWeek) {
                DayOfWeek.MONDAY -> 0
                DayOfWeek.TUESDAY -> 1
                DayOfWeek.WEDNESDAY -> 2
                DayOfWeek.THURSDAY -> 3
                DayOfWeek.FRIDAY -> 4
                DayOfWeek.SATURDAY -> 5
                DayOfWeek.SUNDAY -> 6
            }
            for (i in daysBeforeMonth - 1 downTo 0) {
                daysToShow.add(monthStart.minus(DatePeriod(days = i + 1)))
            }

            // 当月的日期
            for (day in 1..daysInMonth) {
                daysToShow.add(LocalDate(monthStart.year, monthStart.month, day))
            }

            // 下个月开头的日期（补齐到7的倍数）
            val remainingDays = 7 - (daysToShow.size % 7)
            if (remainingDays < 7) {
                for (i in 1..remainingDays) {
                    daysToShow.add(monthStart.plus(DatePeriod(months = 1)).plus(DatePeriod(days = i - 1)))
                }
            }

            // 创建日期到DaySummary的映射
            val daySummaryMap = state.daySummaries.associateBy { it.date }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(daysToShow) { date ->
                    if (date != null) {
                        val summary = daySummaryMap[date]
                        val isCurrentMonth = date.month == monthStart.month
                        val isSelected = date == state.selectedDate

                        MonthViewDayCell(
                            date = date,
                            summary = summary,
                            isCurrentMonth = isCurrentMonth,
                            isSelected = isSelected,
                            showLunar = state.showLunar,
                            viewModel = viewModel,
                            onClick = {
                                viewModel.onDaySelected(date)
                                selectedDate = date
                                showEventList = true
                                onDayClick(date)
                            },
                        )
                    } else {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }
                }
            }
        }
    }

    // 显示选中日期的事件列表
    if (showEventList && selectedDate != null) {
        val dayEvents = state.eventsOfSelectedDate.filter { event ->
            LocalDate(event.startTime.year, event.startTime.monthNumber, event.startTime.dayOfMonth) == selectedDate
        }
        
        ModalBottomSheet(
            onDismissRequest = { showEventList = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "${selectedDate!!.year}年${selectedDate!!.monthNumber}月${selectedDate!!.dayOfMonth}日",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                if (dayEvents.isEmpty()) {
                    Text(
                        text = "这一天没有事件",
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(dayEvents.sortedBy { it.startTime }) { event ->
                            EventListItem(
                                event = event,
                                onClick = {
                                    showEventList = false
                                    onDayClick(selectedDate!!)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventListItem(
    event: CalendarEvent,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Column {
            Text(
                text = event.title,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = String.format(
                    "%02d:%02d - %02d:%02d",
                    event.startTime.hour,
                    event.startTime.minute,
                    event.endTime.hour,
                    event.endTime.minute,
                ),
                fontSize = 12.sp,
                color = Color.Gray,
            )
        }
    }
}

@Composable
private fun MonthViewDayCell(
    date: LocalDate,
    summary: DaySummary?,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    showLunar: Boolean,
    viewModel: com.minst.chronoflow.presentation.CalendarViewModel,
    onClick: () -> Unit,
) {
    val eventCount = summary?.eventCount ?: 0
    val intensity = summary?.totalIntensity ?: 0

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.Blue else Color.Gray,
            )
            .background(
                color = when {
                    !isCurrentMonth -> Color(0xFFF5F5F5)
                    eventCount == 0 -> Color.White
                    eventCount <= 2 -> Color(0xFFE8F5E9)
                    eventCount <= 5 -> Color(0xFFFFF3E0)
                    else -> Color(0xFFFFEBEE)
                },
            )
            .padding(4.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column {
            Text(
                text = "${date.dayOfMonth}",
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentMonth) Color.Black else Color.Gray,
            )
            // lunar short text (fallback to viewModel lookup when summary missing)
            if (showLunar) {
                val lunarText = summary?.lunarText ?: viewModel.getLunarInfo(date)?.lunarShort
                if (!lunarText.isNullOrBlank()) {
                    Text(
                        text = lunarText,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
                if (summary?.hasRecurring == true) {
                    Text("🔁", fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                }
            if (eventCount > 0) {
                // 显示事件密度指示（用点或数字）
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    repeat(minOf(eventCount, 5)) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when {
                                        intensity >= 20 -> Color.Red
                                        intensity >= 15 -> Color(0xFFFF9800)
                                        intensity >= 10 -> Color(0xFFFFC107)
                                        else -> Color(0xFF4CAF50)
                                    },
                                )
                                .padding(2.dp),
                        ) {
                            // 小圆点或数字
                        }
                    }
                    if (eventCount > 5) {
                        Text(
                        text = "+${eventCount - 5}",
                        fontSize = 8.sp,
                        )
                    }
                }
            }
        }
    }
}

