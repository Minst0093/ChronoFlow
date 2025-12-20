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

class SqlDelightEventRepository(
    context: Context,
) : EventRepository {

    private val database: ChronoFlowDatabase

    init {
        val driver: SqlDriver = app.cash.sqldelight.driver.android.AndroidSqliteDriver(
            schema = ChronoFlowDatabase.Schema,
            context = context,
            name = "chronoflow.db",
        )
        database = ChronoFlowDatabase(driver)
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
            )
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
    )
}

