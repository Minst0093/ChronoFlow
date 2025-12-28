package com.minst.chronoflow.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.minst.chronoflow.presentation.CalendarViewModel
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.util.isAllDayEvent
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthViewScreen(
    viewModel: CalendarViewModel,
    onViewModeChange: (ViewMode) -> Unit = {},
    onDayClick: (LocalDate) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    // use persisted state from viewModel so month view remembers last expand state
    val dateSelectorExpanded = state.dateSelectorExpanded

    // external drag state shared with header's selector for live linkage
    val density = LocalDensity.current
    val collapsedHeightDp = 120.dp
    val expandedHeightDp = 440.dp
    val collapsedPx = with(density) { collapsedHeightDp.toPx() }
    val expandedPx = with(density) { expandedHeightDp.toPx() }
    val externalDragHeightPx = remember { androidx.compose.runtime.mutableStateOf(if (dateSelectorExpanded) expandedPx else collapsedPx) }
    // keep externalDragHeightPx in sync when persisted state changes (e.g., navigate away/back)
    androidx.compose.runtime.LaunchedEffect(dateSelectorExpanded) {
        val target = if (dateSelectorExpanded) expandedPx else collapsedPx
        val anim = Animatable(externalDragHeightPx.value)
        anim.animateTo(target, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        externalDragHeightPx.value = anim.value
    }
    val externalIsDragging = remember { androidx.compose.runtime.mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CalendarHeader(
                viewModel = viewModel,
                currentViewMode = ViewMode.MONTH,
                onViewModeChange = onViewModeChange,
                showDateSelector = true,
                expandedDateSelector = dateSelectorExpanded,
                onDateSelectorExpandedChange = { viewModel.setDateSelectorExpanded(it) },
                externalDragHeightPx = externalDragHeightPx,
                externalIsDragging = externalIsDragging
            )
        }
        ,
        floatingActionButton = {
            CreateEventFloating(viewModel = viewModel, initialDateProvider = { state.selectedDate })
        }
    ) { padding ->
        // Simple month content: list events grouped by selectedDate as placeholder.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            // External handle positioned below the header/selector, drives external drag state for live linkage
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (dateSelectorExpanded) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(width = 48.dp, height = 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .pointerInput(Unit) {
                                var acc = 0f
                                var start = externalDragHeightPx.value
                                detectDragGestures(
                                    onDragStart = {
                                        externalIsDragging.value = true
                                        start = externalDragHeightPx.value
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        acc += dragAmount.y
                                        val new = (externalDragHeightPx.value + dragAmount.y).coerceIn(collapsedPx, expandedPx)
                                        externalDragHeightPx.value = new
                                    },
                                    onDragEnd = {
                                        // decide based on threshold relative to start drag
                                        val thresholdPx = (expandedPx - collapsedPx) * 0.25f
                                        val delta = externalDragHeightPx.value - start
                                        val toExpand = when {
                                            delta > thresholdPx -> true
                                            delta < -thresholdPx -> false
                                            else -> externalDragHeightPx.value > (collapsedPx + expandedPx) / 2f
                                        }
                                        // animate externalDragHeightPx to target so header follows smoothly
                                        scope.launch {
                                            val target = if (toExpand) expandedPx else collapsedPx
                                            val anim = Animatable(externalDragHeightPx.value)
                                            anim.animateTo(target, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                            externalDragHeightPx.value = anim.value
                                            viewModel.setDateSelectorExpanded(toExpand)
                                            externalIsDragging.value = false
                                            acc = 0f
                                        }
                                    },
                                    onDragCancel = {
                                        externalIsDragging.value = false
                                        acc = 0f
                                    }
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFDDDDDD))
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .pointerInput(Unit) {
                                var acc = 0f
                                var start = externalDragHeightPx.value
                                detectDragGestures(
                                    onDragStart = {
                                        externalIsDragging.value = true
                                        start = externalDragHeightPx.value
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        acc += dragAmount.y
                                        val new = (externalDragHeightPx.value + dragAmount.y).coerceIn(collapsedPx, expandedPx)
                                        externalDragHeightPx.value = new
                                    },
                                    onDragEnd = {
                                        val thresholdPx = (expandedPx - collapsedPx) * 0.25f
                                        val delta = externalDragHeightPx.value - start
                                        val toExpand = when {
                                            delta > thresholdPx -> true
                                            delta < -thresholdPx -> false
                                            else -> externalDragHeightPx.value > (collapsedPx + expandedPx) / 2f
                                        }
                                        scope.launch {
                                            val target = if (toExpand) expandedPx else collapsedPx
                                            val anim = Animatable(externalDragHeightPx.value)
                                            anim.animateTo(target, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                            externalDragHeightPx.value = anim.value
                                            viewModel.setDateSelectorExpanded(toExpand)
                                            externalIsDragging.value = false
                                            acc = 0f
                                        }
                                    },
                                    onDragCancel = {
                                        externalIsDragging.value = false
                                        acc = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "collapse",
                            tint = Color(0xFF666666)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Event area header: left selected date, right lunar info for selected date
            val lunarInfoForSelected = viewModel.getLunarInfo(state.selectedDate)
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${state.selectedDate.monthNumber}月${state.selectedDate.dayOfMonth}日", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(text = lunarInfoForSelected?.lunarDate ?: "", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.End)
            }

            // Event list (month event area)
            var selectedEvent: CalendarEvent? by remember { mutableStateOf(null) }
            var showEditForm by remember { mutableStateOf(false) }
            val events: List<CalendarEvent> = state.eventsOfSelectedDate
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)) {
                    items(events) { ev ->
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        val isOngoing = now >= ev.startTime && now <= ev.endTime
                        val borderColor = getEventTypeColor(ev.type).copy(alpha = 0.28f)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .border(1.dp, borderColor, shape = RoundedCornerShape(12.dp))
                                .clickable { selectedEvent = ev },
                        ) {
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // colored stripe
                                Box(modifier = Modifier
                                    .width(6.dp)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(getEventTypeColor(ev.type))
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ev.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isOngoing) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFDFF3FF))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(text = "进行中", fontSize = 12.sp, color = Color(0xFF1A73E8))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        // optional description or type label
                                        Text(text = getEventTypeLabel(ev.type), fontSize = 12.sp, color = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    val isAllDay = isAllDayEvent(ev, state.selectedDate)
                                    if (isAllDay) {
                                        androidx.compose.material3.Text(text = "全天", fontSize = 16.sp)
                                    } else {
                                        androidx.compose.material3.Text(text = String.format("%02d:%02d", ev.startTime.hour, ev.startTime.minute), fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        androidx.compose.material3.Text(text = String.format("%02d:%02d", ev.endTime.hour, ev.endTime.minute), fontSize = 14.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }

                // FAB moved to Scaffold.floatingActionButton for proper bottom-end placement
            }
            // Event detail dialog (hide when edit form is active)
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

            // Edit form
            if (showEditForm && selectedEvent != null) {
                EventFormDialog(
                    event = selectedEvent,
                    onDismiss = {
                        showEditForm = false
                        selectedEvent = null
                    },
                    onSave = { input -> viewModel.onCreateEvent(input) },
                    onUpdate = { updatedEvent -> viewModel.onUpdateEvent(updatedEvent) }
                )
            }
        }
    }
}
