package com.minst.chronoflow.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.util.isAllDayEvent
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlin.math.roundToInt

/**
 * 通用时间轴事件区组件
 *
 * - 支持日视图和周视图两种模式
 * - 周视图将可见区域分为 7 列（从 `selectedWeekStart` 开始）
 * - 自动将跨天事件切分为每日段并对同一日期内重叠事件进行列分配
 */
@Composable
fun AllDayEventArea(
    viewMode: ViewMode,
    selectedDate: LocalDate,
    selectedWeekStart: LocalDate,
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit = {},
    leftTimeLabelWidth: Dp = 56.dp,
    hourHeight: Dp = 60.dp,
) {
    BoxWithConstraints {
        val density = LocalDensity.current
        val totalWidth = maxWidth
        val visibleDays: List<LocalDate> = when (viewMode) {
            ViewMode.DAY -> listOf(selectedDate)
            ViewMode.WEEK -> List(7) { idx -> selectedWeekStart.plus(DatePeriod(days = idx)) }
            else -> listOf(selectedDate)
        }
        val dayAreaWidth = totalWidth - leftTimeLabelWidth
        val dayColumnWidth = if (visibleDays.isNotEmpty()) dayAreaWidth / visibleDays.size else dayAreaWidth

        // build segments at day granularity for all-day events
        data class AllDaySeg(val event: CalendarEvent, val startIndex: Int, val endIndex: Int)

        val segments = remember(events, visibleDays) {
            val list = mutableListOf<AllDaySeg>()
            for (ev in events) {
                // find intersection range with visible days
                var firstIndex = -1
                var lastIndex = -1
                for ((idx, day) in visibleDays.withIndex()) {
                    if (!isAllDayEvent(ev, day)) continue
                    if (firstIndex == -1) firstIndex = idx
                    lastIndex = idx
                }
                if (firstIndex != -1 && lastIndex >= firstIndex) {
                    list.add(AllDaySeg(ev, firstIndex, lastIndex))
                }
            }
            list
        }

        // assign vertical rows to avoid overlap (greedy)
        val rows = mutableListOf<MutableList<AllDaySeg>>() // each row is list of segments
        for (seg in segments) {
            var placed = false
            for (row in rows) {
                // check overlap with last segment in row
                val last = row.lastOrNull()
                if (last == null || seg.startIndex > last.endIndex) {
                    row.add(seg)
                    placed = true
                    break
                }
            }
            if (!placed) {
                rows.add(mutableListOf(seg))
            }
        }

        val rowHeight = 36.dp
        val verticalSpacing = 6.dp
        val containerHeight = (rows.size * (rowHeight + verticalSpacing)).coerceAtLeast(rowHeight)

        // background area
        Box(modifier = Modifier.height(containerHeight).fillMaxWidth()) {
            // axis gap (left) with "全天" label
            Box(
                modifier = Modifier
                    .width(leftTimeLabelWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                androidx.compose.material3.Text(
                    text = "全天",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // render segments
            for ((rowIndex, row) in rows.withIndex()) {
                for (seg in row) {
                    val xOffset = leftTimeLabelWidth + dayColumnWidth * seg.startIndex + 4.dp
                    val segWidth = dayColumnWidth * (seg.endIndex - seg.startIndex + 1) - 8.dp
                    val yOffset = (rowIndex * (rowHeight + verticalSpacing))

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .width(segWidth)
                            .height(rowHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(getEventTypeColor(seg.event.type).copy(alpha = 0.18f))
                            .clickable { onEventClick(seg.event) }
                            .padding(6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        androidx.compose.material3.Text(
                            text = if (seg.event.title.length > 30) seg.event.title.take(30) + "…" else seg.event.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            // vertical separators aligned with timeline (draw over background)
            val separatorColor = Color(0xFFEEEEEE)
            val densityLocal = density
            val separatorPxPositions = remember(visibleDays, dayColumnWidth, leftTimeLabelWidth, densityLocal) {
                val set = mutableSetOf<Int>()
                for (i in 1 until visibleDays.size) {
                    val xDp = leftTimeLabelWidth + dayColumnWidth * i
                    val xPx = with(densityLocal) { xDp.toPx().roundToInt() }
                    set.add(xPx)
                }
                set.toList().sorted()
            }
            for (xPx in separatorPxPositions) {
                val xDpUnique = with(density) { xPx.toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = xDpUnique - 0.5.dp)
                        .height(containerHeight)
                        .width(1.dp)
                        .background(separatorColor.copy(alpha = 0.95f))
                )
            }
            // left axis separator to align with timeline's left axis
            Box(
                modifier = Modifier
                    .offset(x = leftTimeLabelWidth)
                    .height(containerHeight)
                    .width(0.5.dp)
                    .background(Color(0xFFDDDDDD))
            )
            // bottom separator to distinguish all-day area from regular timeline (span full width)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(Color(0xFFDDDDDD))
            )
        }
    }
}

@Composable
fun TimelineEventArea(
    viewMode: ViewMode,
    selectedDate: LocalDate,
    selectedWeekStart: LocalDate,
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit = {},
    enableAutoScroll: Boolean = false,
    hourHeight: Dp = 60.dp, // 每小时高度，可调整
    leftTimeLabelWidth: Dp = 56.dp,
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        val totalWidth = maxWidth
        val contentHeight = hourHeight * 24
        val density = LocalDensity.current

        // 计算每分钟的高度（px），用于将时间转换为偏移
        val minuteHeightPx = with(density) { (hourHeight.toPx()) / 60f }

        // 生成当前视图需要渲染的日期列表（1 或 7 天）
        val visibleDays: List<LocalDate> = when (viewMode) {
            ViewMode.DAY -> listOf(selectedDate)
            ViewMode.WEEK -> List(7) { idx -> selectedWeekStart.plus(DatePeriod(days = idx)) }
            else -> listOf(selectedDate)
        }

        // 日区域宽度（除去左侧时间列）
        val dayAreaWidth = totalWidth - leftTimeLabelWidth
        val dayColumnWidth = if (visibleDays.isNotEmpty()) dayAreaWidth / visibleDays.size else dayAreaWidth

        // current datetime for indicator (compute early so left labels can decide overlap)
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val today = LocalDate(now.year, now.monthNumber, now.dayOfMonth)
        val currentMinuteOfDay = now.hour * 60 + now.minute
        val indicatorYDp = with(density) { (currentMinuteOfDay * minuteHeightPx).toDp() }
        val currentTimeCardHeight = 22.dp
        val currentTimeCardTop = indicatorYDp - currentTimeCardHeight / 2

        // 背景时间网格和时间标签
        Row(modifier = Modifier.fillMaxWidth()) {
            // 左侧时间轴（显示小时标签），并在需要时隐藏与当前时间标签重叠的小时数字
            Column(modifier = Modifier.width(leftTimeLabelWidth)) {
                for (hour in 0..23) {
                    // place the hour label at the top of the hour slot (aligned with the hour grid line)
                    val hourTop = hourHeight * hour
                    val labelTop = hourTop + 2.dp
                    val labelBottom = labelTop + 12.dp
                    val cardTop = currentTimeCardTop
                    val cardBottom = currentTimeCardTop + currentTimeCardHeight
                    val overlapWithCard = !(cardBottom < labelTop || cardTop > labelBottom)

                    Box(
                        modifier = Modifier
                        .height(hourHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        if (!overlapWithCard) {
                            androidx.compose.material3.Text(
                                text = String.format("%02d:00", hour),
                                fontSize = 10.sp,
                                color = Color(0xFF777777),
                                modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                    )
                }
                    }
                }
                // （不在此处绘制轴分隔线，改为在 overlays 中统一绘制以保证层级）
            }

            // 右侧时间内容区域（空白网格、当前日期高亮）
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight)
            ) {
                // hour rows (visual only)
                for (hour in 0..23) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, with(density) { (hourHeight * hour).toPx().roundToInt() }) }
                            .height(1.dp)
                            .fillMaxWidth()
                            .background(Color(0xFFEEEEEE))
                    )
                }
            }
        }

        // 事件布局：先把事件按日拆分成段，然后对每个日期内部做列分配
        data class EventSegment(
            val event: CalendarEvent,
            val date: LocalDate,
            val startMinute: Int,
            val endMinute: Int
        )

        val segments = remember(events, visibleDays) {
            val list = mutableListOf<EventSegment>()
            for (ev in events) {
                // 对于每个可见日期，计算事件在该日的时间段（若有交集）
                for ((dayIndex, day) in visibleDays.withIndex()) {
                    // skip all-day events — they are rendered in the dedicated all-day area
                    if (isAllDayEvent(ev, day)) continue
                    val dayStart = day.atStartOfDay().date // placeholder, not used directly
                    val evStartDate = ev.startTime.date
                    val evEndDate = ev.endTime.date

                    val intersects = !(ev.endTime.date < day || ev.startTime.date > day)
                    if (!intersects) continue

                    val segStartMinute = if (ev.startTime.date == day) {
                        ev.startTime.hour * 60 + ev.startTime.minute
                    } else {
                        0
                    }
                    val segEndMinute = if (ev.endTime.date == day) {
                        ev.endTime.hour * 60 + ev.endTime.minute
                    } else {
                        24 * 60
                    }

                    if (segEndMinute > 0 && segStartMinute < segEndMinute) {
                        list.add(EventSegment(ev, day, segStartMinute, segEndMinute))
                    }
                }
            }
            list
        }

        // 按日期分组并为每组做列分配（处理重叠）
        val segmentsByDate = segments.groupBy { it.date }

        for ((date, segs) in segmentsByDate) {
            val dayIndex = visibleDays.indexOf(date).coerceAtLeast(0)
            // sort by start
            val sorted = segs.sortedWith(compareBy<EventSegment> { it.startMinute }.thenBy { it.endMinute })

            // greedy column assignment
            val columnEnds = mutableListOf<Int>() // endMinute per column
            val assignment = mutableMapOf<EventSegment, Int>()
            for (seg in sorted) {
                var placed = false
                for (col in columnEnds.indices) {
                    if (seg.startMinute >= columnEnds[col]) {
                        assignment[seg] = col
                        columnEnds[col] = seg.endMinute
                        placed = true
                        break
                    }
                }
                if (!placed) {
                    // new column
                    assignment[seg] = columnEnds.size
                    columnEnds.add(seg.endMinute)
                }
            }

            val columnsCount = columnEnds.size.coerceAtLeast(1)

            // render each segment box
            for (seg in sorted) {
                val colIndex = assignment[seg] ?: 0
                val xOffsetDp = leftTimeLabelWidth + dayColumnWidth * dayIndex + (dayColumnWidth * colIndex / columnsCount) + 4.dp
                val segTopPx = seg.startMinute * minuteHeightPx
                val segHeightPx = (seg.endMinute - seg.startMinute) * minuteHeightPx
                val segTopDp = with(density) { segTopPx.toDp() }
                val segHeightDp = with(density) { segHeightPx.toDp() } - 4.dp

                val boxWidth = dayColumnWidth * (1f / columnsCount) - 8.dp

                // render
                Box(
                    modifier = Modifier
                        .offset { IntOffset(with(density) { xOffsetDp.toPx().roundToInt() }, with(density) { segTopDp.toPx().roundToInt() }) }
                        .height(segHeightDp)
                        .width(boxWidth)
                        .clip(RoundedCornerShape(8.dp))
                        .background(getEventTypeColor(seg.event.type).copy(alpha = 0.18f))
                        .clickable { onEventClick(seg.event) }
                        .padding(6.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column {
                        androidx.compose.material3.Text(
                            text = if (seg.event.title.length > 20) seg.event.title.take(20) + "…" else seg.event.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color.Black,
                        )
                        androidx.compose.material3.Text(
                            text = String.format("%02d:%02d", seg.event.startTime.hour, seg.event.startTime.minute),
                            fontSize = 10.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
        // --- overlays on top: 分栏线（避免被事件覆盖）和当前时间指示（置于最顶层）
        // vertical separators between day columns (skip 0 which is axis separator already drawn)
        val separatorColor = Color(0xFFEEEEEE)
        // axis separator: separate time axis and event area (draw first so current time card sits above)
        Box(
            modifier = Modifier
                .offset(x = leftTimeLabelWidth)
                .height(contentHeight)
                .width(0.5.dp)
                .background(Color(0xFFDDDDDD))
        )

        // Compute pixel-aligned unique separator positions to avoid rounding-caused overlaps
        val separatorPxPositions = remember(visibleDays, dayColumnWidth, leftTimeLabelWidth, density) {
            val set = mutableSetOf<Int>()
            for (i in 1 until visibleDays.size) {
                val xDp = leftTimeLabelWidth + dayColumnWidth * i
                val xPx = with(density) { xDp.toPx().roundToInt() }
                set.add(xPx)
            }
            set.toList().sorted()
        }

        for (xPx in separatorPxPositions) {
            val xDpUnique = with(density) { xPx.toDp() }
            Box(
                modifier = Modifier
                    .offset(x = xDpUnique - 0.5.dp) // center a 1.dp line on the pixel for crisper alignment
                    .height(contentHeight)
                    .width(1.dp)
                    .background(separatorColor.copy(alpha = 0.95f))
            )
        }

        // full faint horizontal current-time baseline (across all day columns)
        Box(
            modifier = Modifier
                .offset(x = leftTimeLabelWidth, y = indicatorYDp)
                .height(1.dp)
                .width(dayAreaWidth)
                .background(Color(0xFF3A78FF).copy(alpha = 0.18f))
        )

        // current time indicator (draw on top). 如果当前日期可见，则只在该列内部绘制水平线以避免遮挡其他列事件
        val todayIndexForIndicator = visibleDays.indexOf(today)
        if (todayIndexForIndicator >= 0) {
            val indicatorX = with(density) { (leftTimeLabelWidth + dayColumnWidth * todayIndexForIndicator).toPx().roundToInt() }
            val indicatorWidth = with(density) { dayColumnWidth.toPx().roundToInt() }
            // horizontal line limited to the column
            Box(
                modifier = Modifier
                    .offset(x = leftTimeLabelWidth + dayColumnWidth * todayIndexForIndicator, y = indicatorYDp)
                    .height(1.dp)
                    .width(dayColumnWidth)
                    .background(Color(0xFF3A78FF).copy(alpha = 0.9f))
            )

            // time label in left axis area (on top)
            val cardWidth = 48.dp
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A78FF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .offset(x = leftTimeLabelWidth - cardWidth, y = currentTimeCardTop)
                    .width(cardWidth)
                    .height(currentTimeCardHeight)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Text(
                        text = String.format("%02d:%02d", now.hour, now.minute),
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }
    }
}

// small helper to create a LocalDateTime at start of day (avoid adding additional dependencies)
private fun LocalDate.atStartOfDay() = kotlinx.datetime.LocalDateTime(this.year, this.monthNumber, this.dayOfMonth, 0, 0)

