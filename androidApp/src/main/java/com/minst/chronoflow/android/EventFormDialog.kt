package com.minst.chronoflow.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.EventType
import com.minst.chronoflow.domain.model.ReminderConfig
import com.minst.chronoflow.domain.model.RecurrenceRule
import com.minst.chronoflow.domain.model.Frequency
import com.minst.chronoflow.domain.engine.DefaultRecurrenceExpander
import com.minst.chronoflow.presentation.CreateEventInput
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormDialog(
    event: CalendarEvent? = null,
    initialDate: LocalDate? = null,
    onDismiss: () -> Unit,
    onSave: (CreateEventInput) -> Unit,
    onUpdate: (CalendarEvent) -> Unit,
) {
    val isEditMode = event != null

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val defaultStartTime = when {
        event != null -> event.startTime
        initialDate != null -> {
            val today = now.date
            if (initialDate == today) now else LocalDateTime(initialDate.year, initialDate.monthNumber, initialDate.dayOfMonth, 9, 0)
        }
        else -> now
    }
    val defaultEndTime = event?.endTime ?: run {
        val tz = TimeZone.currentSystemDefault()
        defaultStartTime.toInstant(tz).plus(1, DateTimeUnit.HOUR).toLocalDateTime(tz)
    }

    var title by remember { mutableStateOf(event?.title ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var startTime by remember { mutableStateOf(defaultStartTime) }
    var endTime by remember { mutableStateOf(defaultEndTime) }
    var selectedType by remember { mutableStateOf(event?.type ?: EventType.LIFE) }
    var intensity by remember { mutableIntStateOf(event?.intensity ?: 3) }
    var hasReminder by remember { mutableStateOf(event?.reminder != null) }
    var reminderMinutes by remember { mutableIntStateOf(event?.reminder?.minutesBefore ?: 10) }
    // Recurrence UI state (中文选项)
    var recurrenceOption by remember { mutableStateOf(event?.recurrence?.let { "自定义" } ?: "无") }
    var recurrenceEndType by remember { mutableStateOf("永不") } // "永不" | "直到" | "次数"
    var recurrenceCount by remember { mutableIntStateOf(event?.recurrence?.count ?: 1) }
    var recurrenceUntil by remember { mutableStateOf<LocalDate?>(event?.recurrence?.until?.date) }
    var recurrenceInterval by remember { mutableIntStateOf(event?.recurrence?.interval ?: 1) }
    var recurrenceError by remember { mutableStateOf<String?>(null) }
    // (recurrence rule computation and preview below; computedRecurrenceRule/nextPreview defined later)

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showUntilDatePicker by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var reminderError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isAllDay by remember { mutableStateOf(event?.recurrence?.allDay ?: false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // compute recurrence rule from UI state so both dialog body and confirmButton can use it
    val computedRecurrenceRule: RecurrenceRule? = remember(recurrenceOption, recurrenceEndType, recurrenceCount, recurrenceUntil, recurrenceInterval, startTime, endTime, isAllDay) {
        when (recurrenceOption) {
            "无" -> null
            "每天" -> RecurrenceRule(freq = Frequency.DAILY, interval = recurrenceInterval, allDay = isAllDay, count = if (recurrenceEndType == "次数") recurrenceCount else null, until = recurrenceUntil?.let { kotlinx.datetime.LocalDateTime(it.year, it.monthNumber, it.dayOfMonth, startTime.hour, startTime.minute) })
            "每周" -> RecurrenceRule(freq = Frequency.WEEKLY, interval = recurrenceInterval, byDay = listOf(startTime.date.dayOfWeek), allDay = isAllDay, count = if (recurrenceEndType == "次数") recurrenceCount else null, until = recurrenceUntil?.let { kotlinx.datetime.LocalDateTime(it.year, it.monthNumber, it.dayOfMonth, startTime.hour, startTime.minute) })
            "隔周" -> RecurrenceRule(freq = Frequency.WEEKLY, interval = 2, byDay = listOf(startTime.date.dayOfWeek), allDay = isAllDay, count = if (recurrenceEndType == "次数") recurrenceCount else null, until = recurrenceUntil?.let { kotlinx.datetime.LocalDateTime(it.year, it.monthNumber, it.dayOfMonth, startTime.hour, startTime.minute) })
            "自定义" -> RecurrenceRule(freq = Frequency.DAILY, interval = recurrenceInterval, allDay = isAllDay, count = if (recurrenceEndType == "次数") recurrenceCount else null, until = recurrenceUntil?.let { kotlinx.datetime.LocalDateTime(it.year, it.monthNumber, it.dayOfMonth, startTime.hour, startTime.minute) })
            else -> null
        }
    }

    // Preview helper (available to both dialog body and confirm button)
    val nextPreview = remember(computedRecurrenceRule, startTime, endTime) {
        if (computedRecurrenceRule == null) null else {
            val expander = DefaultRecurrenceExpander()
            val tempEvent = CalendarEvent(id = "preview", title = title.ifBlank { "预览事件" }, description = null, startTime = startTime, endTime = endTime, type = selectedType, intensity = intensity, reminder = null, recurrence = computedRecurrenceRule)
            val zone = TimeZone.currentSystemDefault()
            val nowDate = Clock.System.now().toLocalDateTime(zone).date
            val list = expander.expand(tempEvent, nowDate, nowDate.plus(kotlinx.datetime.DatePeriod(months = 6)), 1)
            list.firstOrNull()
        }
    }

    val dialogText: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Snackbar host inside dialog
            SnackbarHost(hostState = snackbarHostState)
            // Title wrapped in white card
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = if (it.isBlank()) "标题不能为空" else null
                        },
                        label = { Text("日程名称 *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = titleError != null,
                        singleLine = true,
                    )
                }
            }

                // Main card: all-day / time / timezone / conflict
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = isAllDay, onCheckedChange = { checked ->
                                isAllDay = checked
                                if (checked) {
                                    val d = startTime.date
                                    startTime = LocalDateTime(d.year, d.monthNumber, d.dayOfMonth, 0, 0)
                                    endTime = LocalDateTime(d.year, d.monthNumber, d.dayOfMonth, 23, 59)
                                }
                            })
                            Text("全天", modifier = Modifier.padding(start = 8.dp), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        }

                        // Time rows (pills) - label on left, date+time pills on right (aligned, consistent)
                        if (!isAllDay) {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                                Text("开始时间", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .clickable { showStartDatePicker = true }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text("${startTime.year}-${startTime.monthNumber.toString().padStart(2,'0')}-${startTime.dayOfMonth.toString().padStart(2,'0')}", color = Color.Black)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .clickable { showStartTimePicker = true }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(String.format("%02d:%02d", startTime.hour, startTime.minute), color = Color.Black)
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                                Text("结束时间", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .clickable { showEndDatePicker = true }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text("${endTime.year}-${endTime.monthNumber.toString().padStart(2,'0')}-${endTime.dayOfMonth.toString().padStart(2,'0')}", color = Color.Black)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .clickable { showEndTimePicker = true }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(String.format("%02d:%02d", endTime.hour, endTime.minute), color = Color.Black)
                                    }
                                }
                            }
                        } else {
                            // Single date row for all-day: label left, date pill + weekday aligned right
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("日期", style = MaterialTheme.typography.labelMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .clickable { showStartDatePicker = true }) {
                                        Text("${startTime.year}-${startTime.monthNumber.toString().padStart(2,'0')}-${startTime.dayOfMonth.toString().padStart(2,'0')}", color = Color.Black)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(when (startTime.date.dayOfWeek) {
                                        kotlinx.datetime.DayOfWeek.MONDAY -> "周一"
                                        kotlinx.datetime.DayOfWeek.TUESDAY -> "周二"
                                        kotlinx.datetime.DayOfWeek.WEDNESDAY -> "周三"
                                        kotlinx.datetime.DayOfWeek.THURSDAY -> "周四"
                                        kotlinx.datetime.DayOfWeek.FRIDAY -> "周五"
                                        kotlinx.datetime.DayOfWeek.SATURDAY -> "周六"
                                        kotlinx.datetime.DayOfWeek.SUNDAY -> "周日"
                                    }, color = Color.Gray)
                                }
                            }
                        }

                        // (timezone and conflict badge removed for cleaner UI)
                    }
                }

                // (remove duplicate original date/time rows — using carded pills above)

            // Recurrence selector wrapped in white Card (collapsed behavior)
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    var recurrenceExpanded by remember { mutableStateOf(false) }
                    Row(modifier = Modifier.fillMaxWidth().clickable { recurrenceExpanded = !recurrenceExpanded }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("重复", style = MaterialTheme.typography.labelMedium)
                        Box(modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(recurrenceOption, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (recurrenceExpanded) {
                        Column(modifier = Modifier.selectableGroup()) {
                            val options = listOf("无", "每天", "每周", "隔周", "自定义")
                            val perRow = 3
                            options.chunked(perRow).forEach { rowOpts ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowOpts.forEach { opt ->
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .selectable(selected = recurrenceOption == opt, onClick = { recurrenceOption = opt })
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = recurrenceOption == opt, onClick = { recurrenceOption = opt })
                                            Text(opt, modifier = Modifier.padding(start = 6.dp))
                                        }
                                    }
                                    if (rowOpts.size < perRow) {
                                        repeat(perRow - rowOpts.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }

                    // End condition (hidden when recurrence == "无") — make collapsible like Recurrence
                    if (recurrenceOption != "无") {
                        Spacer(modifier = Modifier.height(8.dp))
                        var endExpanded by remember { mutableStateOf(false) }
                        Row(modifier = Modifier.fillMaxWidth().clickable { endExpanded = !endExpanded }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("结束条件", style = MaterialTheme.typography.labelMedium)
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                val endSummary = when (recurrenceEndType) {
                                    "永不" -> "永不"
                                    "直到" -> ("直到" + recurrenceUntil?.toString()) ?: "选择日期"
                                    "次数" -> "重复${recurrenceCount}次"
                                    else -> recurrenceEndType
                                }
                                Text(endSummary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        if (endExpanded) {
                            val endOptions = listOf("永不", "直到", "次数")
                            val perRowEnd = 3
                            endOptions.chunked(perRowEnd).forEach { rowOpts ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowOpts.forEach { eo ->
                                        Row(modifier = Modifier
                                            .weight(1f)
                                            .selectable(selected = recurrenceEndType == eo, onClick = { recurrenceEndType = eo })
                                            .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = recurrenceEndType == eo, onClick = { recurrenceEndType = eo })
                                            Text(eo, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
                                        }
                                    }
                                    if (rowOpts.size < perRowEnd) {
                                        repeat(perRowEnd - rowOpts.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (recurrenceEndType == "次数") {
                                OutlinedTextField(value = recurrenceCount.toString(), onValueChange = { recurrenceCount = it.toIntOrNull() ?: 0 }, label = { Text("次数") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(120.dp))
                            } else if (recurrenceEndType == "直到") {
                                // interactive until date picker
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showUntilDatePicker = true }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("直到日期", style = MaterialTheme.typography.labelMedium)
                                    Text(recurrenceUntil?.toString() ?: "选择日期", style = MaterialTheme.typography.bodyLarge)
                                }
                                if (showUntilDatePicker) {
                                    val initial = recurrenceUntil ?: LocalDate(startTime.year, startTime.monthNumber, startTime.dayOfMonth)
                                    SimpleDatePickerDialog(initialDate = initial, onDateSelected = { d ->
                                        recurrenceUntil = d
                                        showUntilDatePicker = false
                                    }, onDismiss = { showUntilDatePicker = false })
                                }
                            }
                        }
                    }
                }
            }

            // Interval control for custom/biweekly
                if (recurrenceOption == "隔周") {
                    recurrenceInterval = 2
                } else if (recurrenceOption == "自定义") {
                    // hide custom interval controls for All-day
                    if (!isAllDay) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("间隔", modifier = Modifier.padding(end = 8.dp))
                        Slider(value = recurrenceInterval.toFloat(), onValueChange = { recurrenceInterval = it.toInt().coerceAtLeast(1) }, valueRange = 1f..10f, modifier = Modifier.width(160.dp))
                        Text("${recurrenceInterval}x")
                    }
                }
            }

            // Next occurrence preview (compute recurrence at top-level so confirm button can access)

            if (timeError != null) {
                Text(timeError!!, color = MaterialTheme.colorScheme.error)
            }

            // Description wrapped in white Card for consistent UI
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("描述（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 5,
                    )
                }
            }

            // Event type selector wrapped in white Card
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("事件类型", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    val types = EventType.values().toList()
                    val perRow = 3
                    types.chunked(perRow).forEach { rowTypes ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTypes.forEach { type ->
                                val isSelected = selectedType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) getEventTypeColor(type).copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable { selectedType = type }
                                        .padding(vertical = 8.dp, horizontal = 8.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(getEventTypeColor(type))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(getEventTypeLabel(type), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                            if (rowTypes.size < perRow) {
                                repeat(perRow - rowTypes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            // Intensity wrapped in Card
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("强度: $intensity", style = MaterialTheme.typography.labelMedium)
                    Slider(value = intensity.toFloat(), onValueChange = { intensity = it.toInt() }, valueRange = 1f..5f, steps = 3)
                }
            }

            // Reminder section wrapped in Card
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasReminder, onCheckedChange = { hasReminder = it })
                        Text("设置提醒", modifier = Modifier.padding(start = 8.dp))
                    }

                    if (hasReminder) {
                        OutlinedTextField(
                            value = reminderMinutes.toString(),
                            onValueChange = {
                                val v = it.toIntOrNull()
                                if (v != null) {
                                    reminderMinutes = v
                                    reminderError = if (v <= 0) "请输入大于 0 的分钟数" else null
                                } else {
                                    reminderError = "请输入有效数字"
                                }
                            },
                            label = { Text("提前分钟数") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        if (reminderError != null) {
                            Text(reminderError!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "编辑事件" else "创建事件") },
        text = {
            Box(modifier = Modifier.scale(1.05f)) {
                dialogText()
            }
        },
        confirmButton = {
            val isFormValid = title.isNotBlank() && endTime > startTime && (!hasReminder || (reminderMinutes > 0 && reminderError == null))
            Button(onClick = {
                // 客户端校验 (redundant guard)
                titleError = if (title.isBlank()) "标题不能为空" else null
                timeError = if (endTime <= startTime) "结束时间必须晚于开始时间" else null
                if (!isFormValid) return@Button

                val reminder = if (hasReminder && reminderMinutes > 0) ReminderConfig(reminderMinutes) else null
                isSaving = true
                coroutineScope.launch {
                    try {
                        if (isEditMode && event != null) {
                            val updated = event.copy(title = title, description = description.ifBlank { null }, startTime = startTime, endTime = endTime, type = selectedType, intensity = intensity, reminder = reminder, recurrence = computedRecurrenceRule, allDay = isAllDay)
                            onUpdate(updated)
                        } else {
                            val input = CreateEventInput(title = title, description = description.ifBlank { null }, startTime = startTime, endTime = endTime, type = selectedType, intensity = intensity, reminder = reminder, recurrence = computedRecurrenceRule, allDay = isAllDay)
                            onSave(input)
                        }
                        snackbarHostState.showSnackbar("保存成功")
                        onDismiss()
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("保存失败：${e.message}")
                    } finally {
                        isSaving = false
                    }
                }
            }, enabled = isFormValid && !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存中...")
                } else {
                    Text("保存")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    if (showStartDatePicker) {
        SimpleDatePickerDialog(initialDate = LocalDate(startTime.year, startTime.monthNumber, startTime.dayOfMonth), onDateSelected = { d -> startTime = LocalDateTime(d.year, d.monthNumber, d.dayOfMonth, startTime.hour, startTime.minute); showStartDatePicker = false; showStartTimePicker = true }, onDismiss = { showStartDatePicker = false })
    }
    if (showStartTimePicker) {
        SimpleTimePickerDialog(initialHour = startTime.hour, initialMinute = startTime.minute, onTimeSelected = { h, m -> startTime = LocalDateTime(startTime.year, startTime.monthNumber, startTime.dayOfMonth, h, m); showStartTimePicker = false }, onDismiss = { showStartTimePicker = false })
    }
    if (showEndDatePicker) {
        SimpleDatePickerDialog(initialDate = LocalDate(endTime.year, endTime.monthNumber, endTime.dayOfMonth), onDateSelected = { d -> endTime = LocalDateTime(d.year, d.monthNumber, d.dayOfMonth, endTime.hour, endTime.minute); showEndDatePicker = false; showEndTimePicker = true }, onDismiss = { showEndDatePicker = false })
    }
    if (showEndTimePicker) {
        SimpleTimePickerDialog(initialHour = endTime.hour, initialMinute = endTime.minute, onTimeSelected = { h, m -> endTime = LocalDateTime(endTime.year, endTime.monthNumber, endTime.dayOfMonth, h, m); showEndTimePicker = false }, onDismiss = { showEndTimePicker = false })
    }
}


private fun formatDateTime(dateTime: LocalDateTime): String {
    return "${dateTime.year}-${dateTime.monthNumber.toString().padStart(2, '0')}-${dateTime.dayOfMonth.toString().padStart(2, '0')} ${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"

}

// getEventTypeLabel moved to EventTypeUtils.kt