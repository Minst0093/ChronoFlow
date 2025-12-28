package com.minst.chronoflow.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.util.isAllDayEvent
import com.minst.chronoflow.presentation.CalendarViewModel
import java.time.LocalDate as JavaLocalDate
import java.time.temporal.IsoFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen(
    viewModel: CalendarViewModel,
    onViewModeChange: (ViewMode) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    // create handled by shared helper FAB/dialog
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var showEditForm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CalendarHeader(
                viewModel = viewModel,
                currentViewMode = ViewMode.DAY,
                onViewModeChange = onViewModeChange,
                showDateSelector = true,
            )
        },
        floatingActionButton = {
            CreateEventFloating(viewModel = viewModel, initialDateProvider = { state.selectedDate })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFFFFFF))
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        ) {

            // 全天事件区域（使用与时间轴完全对齐的共享实现）
            AllDayEventArea(
                viewMode = ViewMode.DAY,
                selectedDate = state.selectedDate,
                selectedWeekStart = state.selectedDate,
                events = state.eventsOfSelectedDate,
                onEventClick = { ev -> selectedEvent = ev },
                leftTimeLabelWidth = 56.dp,
                hourHeight = 60.dp
            )

            // 使用通用时间轴事件区组件
            TimelineEventArea(
                viewMode = ViewMode.DAY,
                selectedDate = state.selectedDate,
                selectedWeekStart = state.selectedDate, // 对于日视图，selectedWeekStart可以传selectedDate
                events = state.eventsOfSelectedDate,
                onEventClick = { event ->
                    selectedEvent = event
                },
                enableAutoScroll = true
            )
        }

        // creation dialog handled by CreateEventFloating

        // 事件详情对话框 (隐藏细节对话框当编辑表单激活时以避免冲突)
        selectedEvent?.let { event ->
            if (!showEditForm) {
                val lunarInfo = viewModel.getLunarInfo(event.startTime.date)
                EventDetailDialog(
                    event = event,
                    onDismiss = { selectedEvent = null },
                    onEdit = {
                        // 打开编辑表单；保留 selectedEvent 直到表单显示完成的同步阶段
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
    fun EventCard(
        event: CalendarEvent,
        currentHour: Int,
        onClick: () -> Unit,
    ) {
        val eventStartHour = event.startTime.hour
        val eventStartMinute = event.startTime.minute
        val eventEndHour = event.endTime.hour
        val eventEndMinute = event.endTime.minute

        val typeColor = getEventTypeColor(event.type)
        val isUpcoming = currentHour >= 0 && eventStartHour > currentHour

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = typeColor.copy(alpha = 0.2f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 时间显示
                Column(
                    modifier = Modifier.width(80.dp),
                ) {
                    Text(
                        text = String.format("%02d:%02d", eventStartHour, eventStartMinute),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = String.format("%02d:%02d", eventEndHour, eventEndMinute),
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }

                // 事件信息
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = event.title,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!event.description.isNullOrBlank()) {
                        Text(
                            text = event.description ?: "",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 2,
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 类型标签
                        Box(
                            modifier = Modifier
                                .background(typeColor.copy(alpha = 0.3f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = getEventTypeLabel(event.type),
                                fontSize = 10.sp,
                            )
                        }
                        // 强度显示
                        Text(
                            text = "强度: ${event.intensity}/5",
                            fontSize = 10.sp,
                            color = Color.Gray,
                        )
                        // 提醒图标
                        if (event.reminder != null) {
                            Text(
                                text = "⏰",
                                fontSize = 10.sp,
                            )
                        }
                        // 即将到来标记
                        if (isUpcoming) {
                            Text(
                                text = "即将到来",
                                fontSize = 10.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    // helper functions moved to EventTypeUtils.kt
}



private fun kotlinx.datetime.LocalDate.toWeekOfYear(): Int {
    val j = JavaLocalDate.of(this.year, this.monthNumber, this.dayOfMonth)
    return j.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
}
