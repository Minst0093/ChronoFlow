package com.minst.chronoflow.domain.engine

import com.minst.chronoflow.domain.model.CalendarEvent
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

interface ReminderEngine {
    fun calculateReminderTimes(event: CalendarEvent): List<LocalDateTime>
}

class DefaultReminderEngine : ReminderEngine {
    override fun calculateReminderTimes(event: CalendarEvent): List<LocalDateTime> {
        val config = event.reminder ?: return emptyList()
        if (config.minutesBefore <= 0) return emptyList()

        // 将“本地时间”解释为当前系统时区下的 Instant，在 Instant 上用 TimeBased 单位做减法
        val zone = TimeZone.currentSystemDefault()
        val eventStartInstant = event.startTime.toInstant(zone)
        val reminderInstant =
            eventStartInstant.minus(config.minutesBefore.toLong(), DateTimeUnit.MINUTE)

        // 再转换回 LocalDateTime，供上层和平台调度使用
        val reminderLocal = reminderInstant.toLocalDateTime(zone)

        // 最小实现：单一提醒点，后续可扩展为多提醒
        return listOf(reminderLocal)
    }
}


