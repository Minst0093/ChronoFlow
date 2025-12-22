package com.minst.chronoflow.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

@Composable
fun SimpleDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedYear by remember { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember { mutableIntStateOf(initialDate.monthNumber) }
    var selectedDay by remember { mutableIntStateOf(initialDate.dayOfMonth) }
    // Custom calendar-style DatePicker dialog (centered title, month grid, confirm/cancel)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = "选择日期", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // month header with prev/next
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        if (selectedMonth > 1) selectedMonth -= 1 else { selectedMonth = 12; selectedYear -= 1 }
                    }) { Text("<") }
                    Text("${selectedYear}年${selectedMonth}月", fontWeight = FontWeight.SemiBold)
                    Button(onClick = {
                        if (selectedMonth < 12) selectedMonth += 1 else { selectedMonth = 1; selectedYear += 1 }
                    }) { Text(">") }
                }

                // weekday header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("日","一","二","三","四","五","六").forEach { wd ->
                        Text(wd, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
                    }
                }

                // days grid
                val firstDay = kotlinx.datetime.LocalDate(selectedYear, selectedMonth, 1)
                val firstWeekdayIndex = (firstDay.dayOfWeek.ordinal + 1) % 7 // convert to Sunday=0
                val daysInMonth = getDaysInMonth(selectedYear, selectedMonth)
                var dayCounter = 1
                for (week in 0 until 6) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (weekday in 0 until 7) {
                            val showDay = (week > 0) || (weekday >= firstWeekdayIndex)
                            if (showDay && dayCounter <= daysInMonth) {
                                val day = dayCounter
                                val isSelected = day == selectedDay
                                Box(modifier = Modifier
                                    .weight(1f)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFF0B5FFF) else Color.Transparent)
                                    .clickable {
                                        selectedDay = day
                                    }
                                    .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$day", color = if (isSelected) Color.White else Color.Black)
                                }
                                dayCounter++
                            } else {
                                Box(modifier = Modifier.weight(1f).padding(6.dp)) { /* empty cell */ }
                            }
                        }
                    }
                    if (dayCounter > daysInMonth) break
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val maxDays = getDaysInMonth(selectedYear, selectedMonth)
                val day = selectedDay.coerceIn(1, maxDays)
                val date = LocalDate(selectedYear, selectedMonth, day)
                onDateSelected(date)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun SimpleTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                // Hour column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { hour = (hour + 1) % 24 }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(String.format("%02d", hour), fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { hour = if (hour - 1 < 0) 23 else hour - 1 }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Separator
                Text(":", fontSize = 36.sp, fontWeight = FontWeight.Bold)

                // Minute column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { minute = (minute + 1) % 60 }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(String.format("%02d", minute), fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { minute = if (minute - 1 < 0) 59 else minute - 1 }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onTimeSelected(hour, minute) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 31
    }
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}


