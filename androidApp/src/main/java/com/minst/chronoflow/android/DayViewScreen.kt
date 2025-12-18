package com.minst.chronoflow.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.presentation.CalendarViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen(
    viewModel: CalendarViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "ChronoFlow - 日视图 (${state.selectedDate})")
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { createQuickEvent(viewModel) },
            ) {
                Text(text = "+")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                text = "当天事件：${state.eventsOfSelectedDate.size}",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // 简化版时间轴：逐小时列出，当小时下展示属于该小时的事件标题
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items((0..23).toList()) { hour ->
                    HourRow(
                        hour = hour,
                        events = state.eventsOfSelectedDate.filter { it.startTime.hour == hour },
                        onEventClick = { event ->
                            // 简单删除操作：点击事件即删除
                            viewModel.onDeleteEvent(event.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HourRow(
    hour: Int,
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = String.format("%02d:00", hour),
            modifier = Modifier.width(56.dp),
            fontWeight = FontWeight.Medium,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (events.isEmpty()) {
                Text(
                    text = "-",
                )
            } else {
                events.forEach { event ->
                    Text(
                        text = event.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEventClick(event) },
                    )
                }
            }
        }
    }
}

private fun createQuickEvent(viewModel: CalendarViewModel) {
    val nowInstant = Clock.System.now()
    val zone = TimeZone.currentSystemDefault()
    val start = nowInstant.toLocalDateTime(zone)
    val end = nowInstant.plus(1, DateTimeUnit.HOUR).toLocalDateTime(zone)

    val input = com.minst.chronoflow.presentation.CreateEventInput(
        title = "Quick Event",
        description = null,
        startTime = start,
        endTime = end,
        type = com.minst.chronoflow.domain.model.EventType.LIFE,
        intensity = 3,
        reminder = null,
    )
    viewModel.onCreateEvent(input)
}


