package com.minst.chronoflow.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import kotlin.math.max
import java.util.PriorityQueue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
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
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen(
    viewModel: CalendarViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    var showCreateForm by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var showEditForm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Date switcher: previous | selected | next
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    val prev = state.selectedDate.minus(kotlinx.datetime.DatePeriod(days = 1))
                    val next = state.selectedDate.plus(kotlinx.datetime.DatePeriod(days = 1))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // left slot (symmetric)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                                    .clickable { viewModel.onDaySelected(prev) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = "${prev.monthNumber}/${prev.dayOfMonth}", color = Color.Black.copy(alpha = 0.7f))
                            }
                        }

                        // center slot
                        Box(modifier = Modifier.wrapContentWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = 0.06f), shape = RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${state.selectedDate.year}-${state.selectedDate.monthNumber.toString().padStart(2, '0')}-${state.selectedDate.dayOfMonth.toString().padStart(2, '0')}",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val weekday = when (state.selectedDate.dayOfWeek) {
                                        DayOfWeek.MONDAY -> "周一"
                                        DayOfWeek.TUESDAY -> "周二"
                                        DayOfWeek.WEDNESDAY -> "周三"
                                        DayOfWeek.THURSDAY -> "周四"
                                        DayOfWeek.FRIDAY -> "周五"
                                        DayOfWeek.SATURDAY -> "周六"
                                        DayOfWeek.SUNDAY -> "周日"
                                    }
                                    Text(text = weekday, fontSize = 12.sp, color = Color.Gray)
                                    // lunar info (full + term) from lunar service
                                    if (state.showLunar) {
                                        val lunarInfo = viewModel.getLunarInfo(state.selectedDate)
                                        if (!lunarInfo?.lunarDate.isNullOrBlank()) {
                                            Text(text = lunarInfo?.lunarDate ?: "", fontSize = 12.sp, color = Color.Gray)
                                        }
                                        if (!lunarInfo?.solarTerm.isNullOrBlank()) {
                                            Text(text = lunarInfo?.solarTerm ?: "", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }

                        // right slot (symmetric)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                                    .clickable { viewModel.onDaySelected(next) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = "${next.monthNumber}/${next.dayOfMonth}", color = Color.Black.copy(alpha = 0.7f))
                            }
                        }
                    }

                    
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateForm = true },
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
            // If no events, show a single hint; otherwise hide the header completely
            if (state.eventsOfSelectedDate.isEmpty()) {
                Text(
                    text = "当天没有事件，请按 + 新建时间",
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            // 改进的时间轴：以可滚动的小时网格和覆盖式事件块展示（支持分钟级、跨小时）
            // 先分离全天事件，再仅对有时间的事件做放置与渲染
            val allDayEvents = state.eventsOfSelectedDate.filter { ev -> isAllDayEvent(ev, state.selectedDate) }
            val timedEvents = state.eventsOfSelectedDate.filterNot { ev -> isAllDayEvent(ev, state.selectedDate) }
            // sort by start time, and for same start prefer longer events first (helps column placement)
            val eventsForPlacement = timedEvents.map { e ->
                val s = e.startTime.hour * 60 + e.startTime.minute
                val en = e.endTime.hour * 60 + e.endTime.minute
                Triple(e, s, en)
            }.sortedWith(compareBy({ it.second }, { - (it.third - it.second) })).map { it.first }
            val sortedEvents = eventsForPlacement
            // 更紧凑的时间轴高度
            val hourHeight = 60.dp
            val timeLabelWidth = 56.dp

            // 把小时网格与事件覆盖都放入同一个可滚动容器，确保事件随滚动同步显示
            val scrollState = rememberScrollState()
            val totalHeight = hourHeight * 24

            // Fixed all-day events row (does not scroll with timeline)
            if (allDayEvents.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("全天事件", modifier = Modifier.padding(start = 4.dp).weight(1f), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.weight(4f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        allDayEvents.forEach { ev ->
                            Card(modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { selectedEvent = ev }) {
                                Text(ev.title, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }

            BoxWithConstraints {
                val viewportHeight = this.maxHeight

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .height(totalHeight)
                            .fillMaxWidth()
                    ) {
                        // 小时网格（背景层）
                        Column {
                        for (hour in 0..23) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(hourHeight),
                                verticalAlignment = Alignment.Top,
                            ) {
                                // left label cell (background only; labels rendered in overlay to avoid duplication)
                                Box(
                                    modifier = Modifier
                                        .width(timeLabelWidth)
                                        .padding(start = 8.dp, top = 4.dp),
                                    contentAlignment = Alignment.TopStart,
                                ) {
                                    Spacer(modifier = Modifier.height(0.dp))
                                }

                                // event area cell (background for events)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    // faint horizontal guideline
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color.Gray.copy(alpha = 0.06f))
                                    )
                                }
                            }
                        }
                        }

                        // 事件覆盖层（定位到具体分钟位置），按重叠列分配宽度避免互相遮挡
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val containerMaxWidth = this.maxWidth
                                val availableWidth = containerMaxWidth - timeLabelWidth - 16.dp

                            // 构建放置元信息：贪心分配列并记录重叠时的最大列数
                            data class Placed(
                                val event: CalendarEvent,
                                val start: Int,
                                val end: Int,
                                var col: Int = 0,
                                var totalCols: Int = 1
                            )

                            // Interval partitioning to minimize columns.
                            // Sort by start asc, duration asc (shorter first preferred left).
                            val eventsForPartition = sortedEvents.map { e ->
                                val sRaw = e.startTime.hour * 60 + e.startTime.minute
                                val eRaw = e.endTime.hour * 60 + e.endTime.minute
                                val start = if (e.startTime.date < state.selectedDate) 0 else sRaw
                                val end = if (e.endTime.date > state.selectedDate) 24 * 60 else eRaw
                                Triple(e, start, end)
                            }.sortedWith(compareBy({ it.second }, { (it.third - it.second) }))

                            val placed = mutableListOf<Placed>()
                            // min-heap of Pair(endTime, colIndex)
                            val heap = PriorityQueue<Pair<Int, Int>>(compareBy { it.first })
                            var nextCol = 0

                            eventsForPartition.forEach { (event, start, end) ->
                                val clampedEnd = if (end <= start) start + 30 else end
                                // reuse column if earliest finishing column is free
                                val reusedCol = if (heap.isNotEmpty() && heap.peek().first <= start) {
                                    heap.poll().second
                                } else {
                                    val c = nextCol
                                    nextCol++
                                    c
                                }
                                // push current event's end for that column
                                heap.add(Pair(clampedEnd, reusedCol))
                                placed.add(Placed(event = event, start = start, end = clampedEnd, col = reusedCol, totalCols = 1))
                            }

                            // For each placed, compute concurrent overlap count as its totalCols
                            placed.forEach { p ->
                                val overlapping = placed.count { other -> !(other.end <= p.start || other.start >= p.end) }
                                p.totalCols = max(1, overlapping)
                            }

                            // 绘制每个事件块，按列计算宽度与水平偏移
                            placed.forEach { p ->
                                val topOffset = hourHeight * (p.start.toFloat() / 60f)
                                val blockHeight = hourHeight * ((p.end - p.start).toFloat() / 60f)
                                val eventWidth = availableWidth / p.totalCols
                                val xOffset = timeLabelWidth + 8.dp + eventWidth * p.col

                                Box(
                                    modifier = Modifier
                                        .offset(x = xOffset, y = topOffset)
                                        .width(eventWidth)
                                        .height(blockHeight)
                                        .clickable { selectedEvent = p.event }
                                        .zIndex(1f)
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxSize(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = getEventTypeColor(
                                                p.event.type
                                            ).copy(alpha = 0.35f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = p.event.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Overlay left time labels (always on top) - share same scrollState so they scroll together
                        // compute current hour for hiding the label where the badge is
                        val tzTop = TimeZone.currentSystemDefault()
                        val todayTop = Clock.System.todayIn(tzTop)
                        val nowTop = if (state.selectedDate == todayTop) Clock.System.now().toLocalDateTime(tzTop) else null
                        val currentHourForLabel = nowTop?.hour ?: -1

                        Column(
                            modifier = Modifier
                                .width(timeLabelWidth)
                                .verticalScroll(scrollState)
                                .zIndex(10f)
                        ) {
                            // (no full-height background -- left labels rendered on overlay only)
                            // overlay labels (positioned within same scrolling Column)
                            for (hour in 0..23) {
                                Box(
                                    modifier = Modifier
                                        .height(hourHeight)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    // hide the hour label if the current time badge overlaps this hour
                                    if (hour != currentHourForLabel) {
                                        Text(
                                            text = String.format("%02d:00", hour),
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // draw current time line on top layer inside same Box so it's visible
                        val tz = TimeZone.currentSystemDefault()
                        val today = Clock.System.todayIn(tz)
                        if (state.selectedDate == today) {
                            val now = Clock.System.now().toLocalDateTime(tz)
                            val currentMinutes = now.hour * 60 + now.minute
                            val currentTopDp = hourHeight * (currentMinutes.toFloat() / 60f)

                            // draw connector from badge area to main line
                            val badgeHeight = 24.dp
                            val badgeWidth = 56.dp
                            val badgeX = (timeLabelWidth - badgeWidth) / 2f
                            val lineStart = timeLabelWidth + 8.dp
                            val badgeRight = badgeX + badgeWidth
                            val connectorWidth = (lineStart - badgeRight).coerceAtLeast(0.dp)

                            // connector from badge right edge to main line (inside event area)
                            if (connectorWidth > 0.dp) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = badgeRight, y = currentTopDp)
                                        .height(2.dp)
                                        .width(connectorWidth)
                                        .background(Color.Red.copy(alpha = 0.95f))
                                        .zIndex(2f)
                                )
                            }

                            // main time line (across events)
                            Box(
                                modifier = Modifier
                                    .offset(x = lineStart, y = currentTopDp)
                                    .fillMaxWidth()
                                    .padding(end = 8.dp)
                                    .height(2.dp)
                                    .background(Color.Red.copy(alpha = 0.95f))
                                    .zIndex(2f)
                            )

                            // time badge on left axis: red rounded rect with white bold time
                            Box(
                                modifier = Modifier
                                    .offset(x = badgeX, y = currentTopDp - badgeHeight / 2f)
                                    .width(badgeWidth)
                                    .height(badgeHeight)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Red)
                                    .zIndex(3f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", now.hour, now.minute),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                        // 如果没有事件，显示提示（放在可滚动 Column 内以便与布局对齐）
                        if (sortedEvents.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(totalHeight)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "当天没有事件\n点击 + 创建第一个事件",
                                    color = Color.Gray,
                                )
                            }
                        }
                    }

                    // 当前时间线与自动滚动逻辑：当选中日期为今天时，居中显示当前时间线
                    val tz = TimeZone.currentSystemDefault()
                    val today = Clock.System.todayIn(tz)
                    if (state.selectedDate == today) {
                        val now = Clock.System.now().toLocalDateTime(tz)
                        val currentMinutes = now.hour * 60 + now.minute
                        val currentTopDp = hourHeight * (currentMinutes.toFloat() / 60f)

                        // draw current time line on top layer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = currentTopDp)
                                .height(1.dp)
                                .background(Color.Red.copy(alpha = 0.9f))
                        )

                        // 自动滚动使当前时间线位于视口中央
                        val density = LocalDensity.current
                        LaunchedEffect(state.selectedDate) {
                            // small delay to let layout settle
                            kotlinx.coroutines.delay(50)
                            val targetDp = currentTopDp - viewportHeight / 2f
                            val targetPx =
                                with(density) { targetDp.coerceAtLeast(0.dp).roundToPx() }
                            scrollState.animateScrollTo(targetPx)
                        }
                    }
                }
            }
        }

        // 创建事件表单
        if (showCreateForm) {
            EventFormDialog(
                event = null,
                initialDate = state.selectedDate,
                onDismiss = { showCreateForm = false },
                onSave = { input ->
                    viewModel.onCreateEvent(input)
                },
                onUpdate = { },
            )
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


