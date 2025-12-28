package com.minst.chronoflow.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.util.isAllDayEvent
import com.minst.chronoflow.presentation.CalendarViewModel
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WeekViewScreen(
    viewModel: CalendarViewModel,
    onViewModeChange: (ViewMode) -> Unit = {},
    onDayClick: (LocalDate) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var showEditForm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CalendarHeader(
                viewModel = viewModel,
                currentViewMode = ViewMode.WEEK,
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
                viewMode = ViewMode.WEEK,
                selectedDate = state.selectedDate,
                selectedWeekStart = state.selectedWeekStart,
                events = state.eventsOfSelectedWeek,
                onEventClick = { ev -> selectedEvent = ev },
                leftTimeLabelWidth = 56.dp,
                hourHeight = 60.dp
            )

            // 使用通用时间轴事件区组件
            TimelineEventArea(
                viewMode = ViewMode.WEEK,
                selectedDate = state.selectedDate,
                selectedWeekStart = state.selectedWeekStart,
                events = state.eventsOfSelectedWeek,
                onEventClick = { event ->
                    selectedEvent = event
                }
            )


        }
    }

    // 事件详情对话框 (隐藏细节对话框当编辑表单激活时以避免冲突)
    selectedEvent?.let { event ->
        if (!showEditForm) {
            val lunarInfo = viewModel.getLunarInfo(event.startTime.date)
            EventDetailDialog(
                event = event,
                onDismiss = { selectedEvent = null },
                onEdit = {
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


// moved to EventTypeUtils.kt

