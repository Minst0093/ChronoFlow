package com.minst.chronoflow.android

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.minst.chronoflow.data.local.database.ChronoFlowDatabase
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.domain.model.EventType
import com.minst.chronoflow.domain.model.ReminderConfig
import com.minst.chronoflow.domain.repository.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.minst.chronoflow.domain.model.RecurrenceRule
import com.minst.chronoflow.domain.model.Frequency
import kotlinx.datetime.DayOfWeek


class SqlDelightEventRepository(
    context: Context,
) : EventRepository {

    private val database: ChronoFlowDatabase
    private val appContext: Context

    init {
        val driver: SqlDriver = app.cash.sqldelight.driver.android.AndroidSqliteDriver(
            schema = ChronoFlowDatabase.Schema,
            context = context,
            name = "chronoflow.db",
        )
        database = ChronoFlowDatabase(driver)
        appContext = context
        android.util.Log.d("SqlDelightRepo", "Initialized ChronoFlowDatabase at chronoflow.db")
    }

    override suspend fun getEvents(start: LocalDate, end: LocalDate): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            // 将本地日期转换为 UTC Instant 进行查询
            // 使用系统时区确保日期范围正确
            val systemZone = TimeZone.currentSystemDefault()
            val startInstant = start.atStartOfDayIn(systemZone)
            val endLocalDateTime = LocalDateTime(end.year, end.month, end.dayOfMonth, 23, 59, 59)
            val endInstant = endLocalDateTime.toInstant(systemZone)
            
            // 转换为 ISO 8601 UTC 字符串（带 Z 后缀）
            val startStr = startInstant.toString()
            val endStr = endInstant.toString()

            // 注意：SQL 查询中 :endDate 参数先出现，所以参数顺序是 (endStr, startStr)
            val results = database.calendarEventQueries
                .selectByDateRange(endStr, startStr)
                .executeAsList()

            results.map { it.toDomainModel() }
        }

    override suspend fun saveEvent(event: CalendarEvent): Unit = withContext(Dispatchers.IO) {
        // 将 LocalDateTime 转换为 UTC Instant 存储
        // 使用系统时区进行转换，存储时统一为 UTC 时间
        val systemZone = TimeZone.currentSystemDefault()
        val startInstant = event.startTime.toInstant(systemZone)
        val endInstant = event.endTime.toInstant(systemZone)
        
        // 转换为 ISO 8601 UTC 字符串（带 Z 后缀）
        val startTimeStr = startInstant.toString()
        val endTimeStr = endInstant.toString()
        
        val existing = database.calendarEventQueries.selectById(event.id).executeAsOneOrNull()
        try {
            if (existing != null) {
                database.calendarEventQueries.updateEvent(
                    id = event.id,
                    title = event.title,
                    description = event.description,
                    startTime = startTimeStr,
                    endTime = endTimeStr,
                    type = event.type.name,
                    intensity = event.intensity.toLong(),
                    reminderMinutesBefore = event.reminder?.minutesBefore?.toLong(),
                    recurrence = recurrenceToDbString(event.recurrence),
                    allDay = if (event.allDay) 1L else 0L,
                )
                android.util.Log.d("SqlDelightRepo", "Updated event ${event.id}")
            } else {
                database.calendarEventQueries.insertEvent(
                    id = event.id,
                    title = event.title,
                    description = event.description,
                    startTime = startTimeStr,
                    endTime = endTimeStr,
                    type = event.type.name,
                    intensity = event.intensity.toLong(),
                    reminderMinutesBefore = event.reminder?.minutesBefore?.toLong(),
                    recurrence = recurrenceToDbString(event.recurrence),
                    allDay = if (event.allDay) 1L else 0L,
                )
                android.util.Log.d("SqlDelightRepo", "Inserted event ${event.id}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SqlDelightRepo", "insert/update failed: ${e.message}", e)
            // attempt lightweight migration if recurrence column missing
            val msg = e.message ?: ""
            if (msg.contains("no such column", ignoreCase = true) || msg.contains("has no column named", ignoreCase = true)) {
                try {
                    val db = appContext.openOrCreateDatabase("chronoflow.db", Context.MODE_PRIVATE, null)
                    db.execSQL("ALTER TABLE calendar_event ADD COLUMN recurrence TEXT;")
                    db.close()
                    android.util.Log.i("SqlDelightRepo", "Added recurrence column via ALTER TABLE, retrying insert")
                    // retry
            if (existing != null) {
                database.calendarEventQueries.updateEvent(
                    id = event.id,
                    title = event.title,
                    description = event.description,
                    startTime = startTimeStr,
                    endTime = endTimeStr,
                    type = event.type.name,
                    intensity = event.intensity.toLong(),
                    reminderMinutesBefore = event.reminder?.minutesBefore?.toLong(),
                    recurrence = recurrenceToDbString(event.recurrence),
                    allDay = if (event.allDay) 1L else 0L,
                )
                    } else {
                database.calendarEventQueries.insertEvent(
                    id = event.id,
                    title = event.title,
                    description = event.description,
                    startTime = startTimeStr,
                    endTime = endTimeStr,
                    type = event.type.name,
                    intensity = event.intensity.toLong(),
                    reminderMinutesBefore = event.reminder?.minutesBefore?.toLong(),
                    recurrence = recurrenceToDbString(event.recurrence),
                    allDay = if (event.allDay) 1L else 0L,
                )
                    }
                } catch (m: Exception) {
                    android.util.Log.e("SqlDelightRepo", "migration+retry failed: ${m.message}", m)
                    throw m
                }
            } else {
                throw e
            }
        }
    }

    override suspend fun deleteEvent(id: String): Unit = withContext(Dispatchers.IO) {
        database.calendarEventQueries.deleteEvent(id)
    }
}

private fun com.minst.chronoflow.data.local.database.Calendar_event.toDomainModel(): CalendarEvent {
    // 从 UTC Instant 字符串转换回 LocalDateTime（在系统时区）
    // 数据库存储的是 UTC 时间，读取时转换回本地时间
    val systemZone = TimeZone.currentSystemDefault()
    val startInstant = Instant.parse(start_time)
    val endInstant = Instant.parse(end_time)
    val startLocal = startInstant.toLocalDateTime(systemZone)
    val endLocal = endInstant.toLocalDateTime(systemZone)
    
    return CalendarEvent(
        id = id,
        title = title,
        description = description,
        startTime = startLocal,
        endTime = endLocal,
        type = EventType.valueOf(type),
        intensity = intensity.toInt(),
        reminder = reminder_minutes_before?.let { ReminderConfig(it.toInt()) },
        recurrence = this.recurrence?.let { parseRecurrence(it) },
        allDay = this.all_day?.let { (it != 0L) } ?: false,
    )
}

private fun recurrenceToDbString(rule: RecurrenceRule?): String? {
    if (rule == null) return null
    val parts = mutableListOf<String>()
    parts += "freq=${rule.freq.name}"
    parts += "interval=${rule.interval}"
    rule.byDay?.let { parts += "byDay=${it.joinToString(",") { d -> d.name }}" }
    rule.count?.let { parts += "count=$it" }
    rule.until?.let { parts += "until=${it.toString()}" }
    parts += "allDay=${rule.allDay}"
    return parts.joinToString(";")
}

private fun parseRecurrence(s: String?): RecurrenceRule? {
    if (s == null) return null
    val map = s.split(";").mapNotNull {
        val idx = it.indexOf('=')
        if (idx <= 0) null else Pair(it.substring(0, idx), it.substring(idx + 1))
    }.toMap()

    val freq = map["freq"]?.let { Frequency.valueOf(it) } ?: return null
    val interval = map["interval"]?.toIntOrNull() ?: 1
    val byDay = map["byDay"]?.split(",")?.mapNotNull { d ->
        try { DayOfWeek.valueOf(d) } catch (t: Throwable) { null }
    }
    val count = map["count"]?.toIntOrNull()
    val until = map["until"]?.let {
        try { kotlinx.datetime.LocalDateTime.parse(it) } catch (t: Throwable) { null }
    }
    val allDay = map["allDay"]?.toBoolean() ?: false
    return RecurrenceRule(freq = freq, interval = interval, byDay = byDay, count = count, until = until, allDay = allDay)
}

