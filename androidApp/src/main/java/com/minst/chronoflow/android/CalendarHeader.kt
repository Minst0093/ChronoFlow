package com.minst.chronoflow.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minst.chronoflow.presentation.CalendarViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import java.time.LocalDate as JavaLocalDate
import java.time.temporal.IsoFields
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.MutableState
import com.minst.chronoflow.presentation.state.CalendarUiState
import kotlinx.datetime.todayIn

/**
 * 视图模式枚举
 */
enum class ViewMode(val displayName: String) {
    MONTH("月"),
    WEEK("周"),
    DAY("日")
}

private fun kotlinx.datetime.LocalDate.toWeekOfYear(): Int {
    val j = JavaLocalDate.of(this.year, this.monthNumber, this.dayOfMonth)
    return j.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
}

// Shared sizing constants for the date selector so expanded/collapsed stay consistent
private val HEADER_TIME_LABEL_WIDTH = 40.dp
private val PER_ROW_HEIGHT = 40.dp
private val CIRCLE_SIZE = 36.dp
private val COLLAPSED_CIRCLE_SIZE = 38.dp
private val LUNAR_FONT_SIZE = 9.sp
private val LUNAR_OFFSET = (-4).dp
private val EVENT_DOT_SIZE = 6.dp
private val EVENT_DOT_OFFSET = (-1).dp
private val EXPANDED_EXTRA_PADDING = 28.dp




/**
 * 统一的日历头部组件，包含：
 * 1. 第一行：年月周信息 + 控制按钮
 * 2. 第二行：视图模式切换（年/月/周/日）
 * 3. 第三行：周日期选择器（仅在日视图显示）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarHeader(
    viewModel: CalendarViewModel,
    currentViewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    showDateSelector: Boolean = false, // 是否显示日期选择器
    expandedDateSelector: Boolean = false, // 日期选择器是否展开（月视图专用）
    onDateSelectorExpandedChange: ((Boolean) -> Unit)? = null,
    externalDragHeightPx: MutableState<Float>? = null,
    externalIsDragging: MutableState<Boolean>? = null, // 展开状态变化回调
) {
    val state by viewModel.uiState.collectAsState()
    var tabsVisible by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    // use `expandedDateSelector` param as source of truth (persisted in ViewModel)

    TopAppBar(
        title = {
            Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                // 第一行：年月周信息 + 控制按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.alignByBaseline(),
                            text = "${state.selectedDate.year}年${state.selectedDate.monthNumber}月",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            modifier = Modifier.alignByBaseline(),
                            text = "第 ${state.selectedDate.toWeekOfYear()} 周",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )

                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 展开/收起模式切换行的按钮
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .clickable(
                                    indication = LocalIndication.current,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { tabsVisible = !tabsVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { tabsVisible = !tabsVisible }) {
                                Icon(
                                    imageVector = if (tabsVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "toggle"
                                )
                            }
                        }
                        // 设置菜单按钮
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .clickable(
                                    indication = LocalIndication.current,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showSettings = !showSettings },
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { showSettings = !showSettings },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "more"
                                )
                            }

                            DropdownMenu(
                                expanded = showSettings,
                                onDismissRequest = { showSettings = false },
                                modifier = Modifier
                                    .shadow(8.dp, RoundedCornerShape(8.dp))
                                    .background(Color.White, RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (state.showLunar) "关闭农历" else "显示农历") },
                                    onClick = {
                                        viewModel.setShowLunar(!state.showLunar)
                                        showSettings = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("添加订阅") },
                                    onClick = { /* TODO: implement later */ showSettings = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("导出日程") },
                                    onClick = { /* TODO: implement later */ showSettings = false }
                                )
                            }
                        }
                    }
                }

                // 第二行：视图模式切换
                AnimatedVisibility(
                    visible = tabsVisible,
                    enter = expandVertically(animationSpec = tween(220)) + fadeIn(
                        animationSpec = tween(
                            220
                        )
                    ),
                    exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(
                        animationSpec = tween(
                            180
                        )
                    )
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFFDDDDDD))
                            .animateContentSize()
                            .padding(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ViewMode.entries.forEach { mode ->
                                val selected = mode == currentViewMode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(1.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selected) Color.White else Color.Transparent)
                                        .clickable(
                                            indication = LocalIndication.current,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { onViewModeChange(mode) }
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.displayName,
                                        fontSize = 12.sp,
                                        color = if (selected) Color.Black else Color.Gray,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // 第三行：日期选择器（根据视图模式显示不同内容）
                if (showDateSelector) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // 统一的日期选择器组件：支持展开/收起切换
                    UnifiedDateSelector(
                        viewModel = viewModel,
                        state = state,
                        currentViewMode = currentViewMode,
                        expanded = when (currentViewMode) {
                            ViewMode.MONTH -> expandedDateSelector // use param as source of truth
                            else -> false // 日视图和周视图默认收起
                        },
                        onExpandedChange = { newExpanded ->
                            onDateSelectorExpandedChange?.invoke(newExpanded)
                        },
                        externalDragHeightPx = externalDragHeightPx,
                        externalIsDragging = externalIsDragging
                    )
                }
                // (handle moved to screen content so it sits below the header area)
            }
        }
    )
}

/**
 * 统一的日期选择器组件：支持展开（全月网格）和收起（一周选择器）两种模式
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun UnifiedDateSelector(
    viewModel: CalendarViewModel,
    state: CalendarUiState,
    currentViewMode: ViewMode,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    externalDragHeightPx: androidx.compose.runtime.MutableState<Float>? = null,
    externalIsDragging: androidx.compose.runtime.MutableState<Boolean>? = null,
) {
    // Custom draggable expand/collapse container for the date selector.
    // The container animates its height between collapsed and expanded and exposes drag gestures
    // so the user can swipe up/down to collapse/expand similar to the design.
    val densityLocal = androidx.compose.ui.platform.LocalDensity.current
    val collapsedHeightDp = 120.dp
    // compute expanded height dynamically so we display 5-6 rows using the same sizes as the collapsed week
    // use selectedMonthStart so external month switches update expanded height calculation
    val firstOfMonthForHeight = remember(state.selectedMonthStart) {
        state.selectedMonthStart
    }
    val jFirstForHeight = remember(firstOfMonthForHeight) {
        JavaLocalDate.of(firstOfMonthForHeight.year, firstOfMonthForHeight.monthNumber, firstOfMonthForHeight.dayOfMonth)
    }
    val daysToSundayForHeight = remember(jFirstForHeight) { jFirstForHeight.dayOfWeek.value % 7 }
    val daysInMonthForHeight = remember(jFirstForHeight) { jFirstForHeight.lengthOfMonth() }
    val totalCellsForHeight = daysToSundayForHeight + daysInMonthForHeight
    val computedRows = remember(totalCellsForHeight) { ((totalCellsForHeight + 6) / 7) } // ceil division
    val visibleRows = remember(computedRows) { if (computedRows < 5) 5 else computedRows } // ensure at least 5 rows
    // match collapsed row sizing: make slightly more compact than before (includes day + lunar + spacer)
    // keep consistent between collapsed and expanded: use shared `PER_ROW_HEIGHT` etc.
    val weekdayLabelsHeight = 20.dp
    val expandedHeightDp = 440.dp
    val collapsedH = with(densityLocal) { collapsedHeightDp.toPx() }
    val expandedH = with(densityLocal) { expandedHeightDp.toPx() }
    val heightAnim = remember { androidx.compose.animation.core.Animatable(if (expanded) expandedH else collapsedH) }
    val localDragHeight = remember { androidx.compose.runtime.mutableStateOf(if (expanded) expandedH else collapsedH) }
    val localIsDragging = remember { androidx.compose.runtime.mutableStateOf(false) }
    val dragHeightState = externalDragHeightPx ?: localDragHeight
    val isDraggingState = externalIsDragging ?: localIsDragging

    // coroutine scope for launching suspension functions from callbacks
    val scope = rememberCoroutineScope()

    // If external drag controller is provided, react only to the external controller (no animation based on `expanded` here).
    if (externalDragHeightPx != null) {
        // follow external drag height: snap during active dragging for immediate feedback,
        // animate to the external value when dragging ends to keep motion smooth.
        androidx.compose.runtime.LaunchedEffect(externalDragHeightPx.value, externalIsDragging?.value) {
            if (externalIsDragging?.value == true) {
                heightAnim.snapTo(dragHeightState.value)
                } else {
                // animate to the external value (which may itself be animating) to avoid abrupt jumps
                heightAnim.animateTo(dragHeightState.value, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            }
        }
    } else {
        // no external controller: control animation from `expanded` param
        androidx.compose.runtime.LaunchedEffect(expanded) {
            val target = if (expanded) expandedH else collapsedH
            heightAnim.animateTo(target, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    // choose current height: live drag height when dragging, otherwise anim value
    val currentHeight = if (isDraggingState.value) dragHeightState.value else heightAnim.value
    // If external drag state is not provided, handle drag gestures locally.
    var baseModifier = Modifier.fillMaxWidth().height(with(densityLocal) { currentHeight.toDp() })
        if (externalDragHeightPx == null) {
        baseModifier = baseModifier.pointerInput(Unit) {
            var startDragHeight = 0f
            var startWasExpanded = expanded
            // direction-aware drag: only consume vertical movement so horizontal swipes can pass through
            detectDragGestures(
                onDragStart = {
                    isDraggingState.value = true
                    startDragHeight = dragHeightState.value
                    startWasExpanded = expanded
                },
                onDrag = { change, dragAmount ->
                    // determine dominant axis per movement; prefer vertical only when clearly vertical
                    val absX = kotlin.math.abs(dragAmount.x)
                    val absY = kotlin.math.abs(dragAmount.y)
                    if (absY > absX && absY > 4f) {
                        // vertical gesture — consume and update height
                        change.consume()
                        val new = (dragHeightState.value + dragAmount.y).coerceIn(collapsedH, expandedH)
                        dragHeightState.value = new
                    } else {
                        // horizontal or tiny movement — do not consume so inner horizontal handlers can respond
                    }
                },
                onDragEnd = {
                    // finalize only based on the last drag height (if user was vertically interacting)
                    val threshold = (expandedH - collapsedH) * 0.25f
                    val delta = dragHeightState.value - startDragHeight
                    val toExpand = if (startWasExpanded) {
                        delta < -threshold
                    } else {
                        delta > threshold
                    }
                    val finalTarget = if (toExpand) expandedH else if (!toExpand && startWasExpanded) expandedH else collapsedH
                    val willExpand = toExpand || (startWasExpanded && !toExpand)
                    scope.launch {
                        heightAnim.snapTo(dragHeightState.value)
                        heightAnim.animateTo(finalTarget, animationSpec = tween(220))
                        onExpandedChange(willExpand)
                    }
                    isDraggingState.value = false
                },
                onDragCancel = {
                    val finalTarget = if (expanded) expandedH else collapsedH
                    scope.launch {
                        heightAnim.snapTo(dragHeightState.value)
                        heightAnim.animateTo(finalTarget, animationSpec = tween(220))
                        onExpandedChange(expanded)
                    }
                    isDraggingState.value = false
                }
            )
        }
    }

    // make header area opaque so hidden content cannot visually bleed through
    Box(modifier = baseModifier.background(Color.White)) {
        // cross-fade like effect by interpolating alpha based on height fraction
        val fraction = ((currentHeight - collapsedH) / (expandedH - collapsedH)).coerceIn(0f, 1f)

        // Render only one of the views at a time (no overlapping composition) to avoid touch interception.
        // Use a high threshold so the expanded view is only composed when mostly expanded,
        // preventing the expanded content from being present under the collapsed view.
        val EXPANDED_COMPOSE_THRESHOLD = 0.95f
        val expandedVisible = fraction > EXPANDED_COMPOSE_THRESHOLD
        if (!expandedVisible) {
            // show collapsed week view only
            CollapsedWeekView(
                viewModel = viewModel,
                state = state,
                currentViewMode = currentViewMode,
                onExpand = {
                    scope.launch {
                        heightAnim.animateTo(expandedH, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                        onExpandedChange(true)
                    }
                }
            )
        } else {
            // show expanded month view only
            ExpandedMonthView(
                viewModel = viewModel,
                state = state,
                onCollapse = {
                    scope.launch {
                        heightAnim.animateTo(collapsedH, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                        onExpandedChange(false)
                    }
                }
            )
        }
        // (handle moved outside selector; shown only in month view below)
    }
}

/**
 * 展开的全月视图（网格布局）
 */
@Composable
private fun ExpandedMonthView(
    viewModel: CalendarViewModel,
    state: CalendarUiState,
    onCollapse: () -> Unit
) 
{
    Column {
        // Top weekday labels row (日 - 六) — month view should NOT be offset
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val labels = listOf("日", "一", "二", "三", "四", "五", "六")
            labels.forEach { wd ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = wd, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))

        // The month grid is controlled via drag gestures (no static header here).
        // compute first date to render so grid starts on Sunday and we render 5-6 rows depending on month
        // use selectedMonthStart so horizontal month swipes reflect immediately
        val firstOfMonth = remember(state.selectedMonthStart) {
            state.selectedMonthStart
        }
        val gridStart = remember(firstOfMonth) {
            val jFirst = JavaLocalDate.of(firstOfMonth.year, firstOfMonth.monthNumber, firstOfMonth.dayOfMonth)
            // Java DayOfWeek: Monday=1 ... Sunday=7 -> we want offset to Sunday (0..6)
            val daysToSunday = jFirst.dayOfWeek.value % 7
            firstOfMonth.minus(DatePeriod(days = daysToSunday))
        }

        // compute number of rows needed for this month (min 5, max 6)
        val jFirst = remember(firstOfMonth) {
            JavaLocalDate.of(firstOfMonth.year, firstOfMonth.monthNumber, firstOfMonth.dayOfMonth)
        }
        val daysToSunday = remember(jFirst) { jFirst.dayOfWeek.value % 7 }
        val daysInMonth = remember(jFirst) { jFirst.lengthOfMonth() }
        val totalCells = daysToSunday + daysInMonth
        val rows = remember(totalCells) { ((totalCells + 6) / 7).coerceIn(5, 6) }
        val dates = remember(gridStart, rows) {
            (0 until rows * 7).map { gridStart.plus(DatePeriod(days = it)) }
        }

        // month grid: 7 columns, no left offset for month view
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }
            val monthDragOffset = remember { androidx.compose.runtime.mutableStateOf(0f) }
            val monthSettle = remember { androidx.compose.animation.core.Animatable(0f) }
            val monthScope = rememberCoroutineScope()
            val thresholdPx = maxWidthPx * 0.25f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                val absX = kotlin.math.abs(dragAmount.x)
                                val absY = kotlin.math.abs(dragAmount.y)
                                // prefer horizontal when clearly horizontal
                                if (absX > absY && absX > 4f) {
                                    change.consume()
                                    monthDragOffset.value = (monthDragOffset.value + dragAmount.x).coerceIn(-maxWidthPx, maxWidthPx)
                                }
                            },
                            onDragEnd = {
                                val v = monthDragOffset.value
                                monthScope.launch {
                                    when {
                                        v > thresholdPx -> {
                                            // swipe right -> previous month
                                            viewModel.onPreviousMonth()
                                            monthSettle.snapTo(v)
                                            monthSettle.animateTo(0f, tween(260, easing = EaseOutCubic))
                                        }
                                        v < -thresholdPx -> {
                                            // swipe left -> next month
                                            viewModel.onNextMonth()
                                            monthSettle.snapTo(v)
                                            monthSettle.animateTo(0f, tween(260, easing = EaseOutCubic))
                                        }
                                        else -> {
                                            monthSettle.snapTo(v)
                                            monthSettle.animateTo(0f, tween(200, easing = EaseOutCubic))
                                        }
                                    }
                                    monthDragOffset.value = 0f
                                    monthSettle.snapTo(0f)
                                }
                            },
                            onDragCancel = {
                                monthScope.launch {
                                    monthSettle.snapTo(monthDragOffset.value)
                                    monthSettle.animateTo(0f, tween(200))
                                    monthDragOffset.value = 0f
                                }
                            }
                        )
                    }
            ) {
                val previewMonthOffset = monthSettle.value.takeIf { it != 0f } ?: monthDragOffset.value
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .offset { IntOffset(previewMonthOffset.roundToInt(), 0) }
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    items(dates) { date ->
                        val inMonth = date.monthNumber == state.selectedDate.monthNumber
                        val isSelected = date == state.selectedDate
                        val lunarShort = if (state.showLunar) viewModel.getLunarInfo(date)?.lunarShort else null
                        val hasEventsDay = state.daySummaries.any { it.date == date && it.eventCount > 0 }

                        if (!inMonth) {
                            // render empty placeholder for out-of-month days
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .height(PER_ROW_HEIGHT),
                                contentAlignment = Alignment.Center
                            ) {
                                // keep spacing but show nothing
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // keep row height consistent with collapsed week (compact)
                                    Box(
                                        modifier = Modifier
                                            .height(PER_ROW_HEIGHT)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(CIRCLE_SIZE)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(if (isSelected) Color.Blue else Color.Transparent)
                                                .clickable(
                                                    indication = LocalIndication.current,
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ) {
                                                    val immediate = (monthSettle.value == 0f && monthDragOffset.value == 0f)
                                                    if (immediate) {
                                                        viewModel.onDaySelected(date)
                                                    } else {
                                                        // schedule selection after swipe settle to ensure selectedMonthStart is synced
                                                        monthScope.launch {
                                                            // wait until settle completes (safety timeout)
                                                            val start = System.currentTimeMillis()
                                                            while ((monthSettle.value != 0f || monthDragOffset.value != 0f) && (System.currentTimeMillis() - start) < 2000L) {
                                                                kotlinx.coroutines.delay(40)
                                                            }
                                                            viewModel.onDaySelected(date)
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${date.dayOfMonth}",
                                                color = if (isSelected) Color.White else Color.Black,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }

                                    if (!lunarShort.isNullOrBlank()) {
                                        Text(text = lunarShort, fontSize = LUNAR_FONT_SIZE, color = Color.Gray, modifier = Modifier.offset(y = LUNAR_OFFSET))
                                    } else {
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    Box(modifier = Modifier.height(10.dp), contentAlignment = Alignment.Center) {
                                        if (hasEventsDay) {
                                            Box(
                                                modifier = Modifier
                                                    .size(EVENT_DOT_SIZE)
                                                    .offset(y = EVENT_DOT_OFFSET)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color.Blue)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 收起的一周视图（带手势展开功能）
 */
@Composable
private fun CollapsedWeekView(
    viewModel: CalendarViewModel,
    state: CalendarUiState,
    currentViewMode: ViewMode,
    onExpand: () -> Unit
) {
    Column {
        // 周标签行
        // 与事件区左侧时间轴宽度对齐（与 TimelineEventArea 的 leftTimeLabelWidth 保持一致）
        val headerTimeLabelWidth = if (currentViewMode == ViewMode.MONTH) 0.dp else HEADER_TIME_LABEL_WIDTH
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(headerTimeLabelWidth))
            val labels = listOf("日", "一", "二", "三", "四", "五", "六")
            labels.forEach { wd ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = wd, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 一周日期选择器（带拖拽切换和点击展开功能）
        val weekStart = state.selectedWeekStart
        val selectedDateForDisplay = state.selectedDate
        // UI labels start on Sunday; internal weekStart is Monday-based, so compute a Sunday-first display start
        // Ensure the displayed Sunday-first week contains the selectedDate (handles case when selectedDate is Sunday)
        val displayWeekStart = remember(weekStart, selectedDateForDisplay) {
            val candidate = weekStart.minus(DatePeriod(days = 1)) // default Sunday before Monday-based weekStart
            val sundayOfWeek = weekStart.plus(DatePeriod(days = 6))
            if (selectedDateForDisplay == sundayOfWeek) {
                // selected date is the Sunday at end of the Monday-based week -> show that Sunday as the first column
                selectedDateForDisplay
            } else {
                candidate
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }
        // drag offset in pixels (state-driven for immediate updates without launching coroutines per move)
        val dragOffsetPx = remember { androidx.compose.runtime.mutableStateOf(0f) }
        val settleAnim = remember { androidx.compose.animation.core.Animatable(0f) }
        val scope = rememberCoroutineScope()
        val thresholdPx = maxWidthPx * 0.25f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var acc = 0f
                detectDragGestures(
                    onDrag = { change: PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset ->
                        val absX = kotlin.math.abs(dragAmount.x)
                        val absY = kotlin.math.abs(dragAmount.y)
                        // prefer horizontal only if clearly horizontal and not a vertical gesture
                        if (absX > absY && absX > 4f) {
                            change.consume()
                            dragOffsetPx.value = (dragOffsetPx.value + dragAmount.x).coerceIn(-maxWidthPx, maxWidthPx)
                        } else {
                            // not horizontal enough — let other handlers process
                        }
                    },
                    onDragEnd = {
                        val v = dragOffsetPx.value
                        scope.launch {
                            when {
                                v > thresholdPx -> {
                                    // optimistic update: trigger previous week immediately and animate back to zero
                                    viewModel.onPreviousWeek()
                                    settleAnim.snapTo(v)
                                    settleAnim.animateTo(0f, tween(260, easing = EaseOutCubic))
                                }
                                v < -thresholdPx -> {
                                    viewModel.onNextWeek()
                                    settleAnim.snapTo(v)
                                    settleAnim.animateTo(0f, tween(260, easing = EaseOutCubic))
                                }
                                else -> {
                                    settleAnim.snapTo(v)
                                    settleAnim.animateTo(0f, tween(200, easing = EaseOutCubic))
                                }
                            }
                            // reset drag state
                            dragOffsetPx.value = 0f
                            settleAnim.snapTo(0f)
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            settleAnim.snapTo(dragOffsetPx.value)
                            settleAnim.animateTo(0f, tween(200))
                            dragOffsetPx.value = 0f
                        }
                    }
                )
                }
        ) {
            // render current week row and a preview row (next/previous) during drag
            val previewOffsetPx = settleAnim.value.takeIf { it != 0f } ?: dragOffsetPx.value
            val isDraggingRight = previewOffsetPx > 0f
            val isDraggingLeft = previewOffsetPx < 0f

            // 预计算预览周的起始日（基于 displayWeekStart）
            val previewWeekStart = remember(displayWeekStart, isDraggingLeft, isDraggingRight) {
                when {
                    isDraggingLeft -> displayWeekStart.plus(DatePeriod(days = 7))
                    isDraggingRight -> displayWeekStart.minus(DatePeriod(days = 7))
                    else -> displayWeekStart
                }
            }

            // 当前周行
            Row(
                modifier = Modifier
                    .offset { IntOffset(previewOffsetPx.roundToInt(), 0) }
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(headerTimeLabelWidth))
                for (i in 0..6) {
                    val date = displayWeekStart.plus(DatePeriod(days = i))
                    val isSelected = date == state.selectedDate
                    val lunarShort = if (state.showLunar) viewModel.getLunarInfo(date)?.lunarShort else null
                    val hasEventsDay = state.daySummaries.any { it.date == date && it.eventCount > 0 }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .height(PER_ROW_HEIGHT)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(COLLAPSED_CIRCLE_SIZE)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) Color.Blue else Color.Transparent)
                                    .clickable(
                                        indication = LocalIndication.current,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { viewModel.onDaySelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${date.dayOfMonth}",
                                    color = if (isSelected) Color.White else Color.Black,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        if (!lunarShort.isNullOrBlank()) {
                            Text(text = lunarShort, fontSize = LUNAR_FONT_SIZE, color = Color.Gray, modifier = Modifier.offset(y = LUNAR_OFFSET))
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Box(modifier = Modifier.height(10.dp), contentAlignment = Alignment.Center) {
                            if (hasEventsDay) {
                                Box(
                                    modifier = Modifier
                                        .size(EVENT_DOT_SIZE)
                                        .offset(y = EVENT_DOT_OFFSET)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.Blue)
                                )
                            }
                        }
                    }
                }
            }

            // 预览周行（半透明）
            val previewOffset = remember(previewOffsetPx, maxWidthPx) {
                when {
                    isDraggingLeft -> previewOffsetPx + maxWidthPx
                    isDraggingRight -> previewOffsetPx - maxWidthPx
                    else -> 0f
                }
            }

            val previewAlpha = remember(previewOffsetPx, thresholdPx) {
                val progress = (previewOffsetPx.absoluteValue / thresholdPx).coerceIn(0f, 1f)
                0.4f + (progress * 0.4f)
            }

            Row(
                modifier = Modifier
                    .offset { IntOffset(previewOffset.roundToInt(), 0) }
                    .fillMaxWidth()
                    .alpha(previewAlpha)
            ) {
                Spacer(modifier = Modifier.width(headerTimeLabelWidth))
                for (i in 0..6) {
                    val date = previewWeekStart.plus(DatePeriod(days = i))
                    val isSelected = false
                    val lunarShort = if (state.showLunar) viewModel.getLunarInfo(date)?.lunarShort else null
                    val hasEventsDay = state.daySummaries.any { it.date == date && it.eventCount > 0 }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .height(PER_ROW_HEIGHT)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(COLLAPSED_CIRCLE_SIZE)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.Transparent),
                            )
                            Text(
                                text = "${date.dayOfMonth}",
                                color = Color.Gray,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        if (!lunarShort.isNullOrBlank()) {
                            Text(text = lunarShort, fontSize = LUNAR_FONT_SIZE, color = Color.Gray, modifier = Modifier.offset(y = LUNAR_OFFSET))
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Box(modifier = Modifier.height(10.dp), contentAlignment = Alignment.Center) {
                            if (hasEventsDay) {
                                Box(
                                    modifier = Modifier
                                        .size(EVENT_DOT_SIZE)
                                        .offset(y = EVENT_DOT_OFFSET)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.Gray)
                                )
                            }
                        }
                    }
                }
            }

            // 在月视图下不提供点击展开提示；展开/收起通过拖拽手势控制（可保留视觉提示如果需要）
        }
    }
    }
}

