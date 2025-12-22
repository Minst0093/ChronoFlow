package com.minst.chronoflow.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.EventType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@Composable
fun EventDetailDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    lunarShort: String? = null,
    lunarFull: String? = null,
    solarTerm: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            // lunar / solar term (if provided)
            if (!lunarFull.isNullOrBlank() || !solarTerm.isNullOrBlank() || !lunarShort.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (!lunarFull.isNullOrBlank()) {
                            Text(lunarFull, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (!solarTerm.isNullOrBlank()) {
                            Text(solarTerm, style = MaterialTheme.typography.labelMedium)
                        }
                        if (!lunarShort.isNullOrBlank()) {
                            Text(lunarShort, style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.Gray)
                        }
                    }
                }
            }

            // 时间信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "开始时间",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            formatDateTime(event.startTime),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "结束时间",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            formatDateTime(event.endTime),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                // 描述
                if (!event.description.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "描述",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                event.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                // 类型和强度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "类型",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(getEventTypeColor(event.type))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    getEventTypeLabel(event.type),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "强度",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${event.intensity}/5",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                // 提醒信息
                if (event.reminder != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "提醒",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "提前 ${event.reminder?.minutesBefore ?: 0} 分钟",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onEdit) {
                    Text("编辑")
                }
                Button(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                ) {
                    Text("删除")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

private fun formatDateTime(dateTime: LocalDateTime): String {
    return "${dateTime.year}-${dateTime.monthNumber.toString().padStart(2, '0')}-${dateTime.dayOfMonth.toString().padStart(2, '0')} " +
            "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
}

// moved to EventTypeUtils.kt

