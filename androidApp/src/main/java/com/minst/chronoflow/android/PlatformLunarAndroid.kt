package com.minst.chronoflow.android

import com.minst.chronoflow.domain.engine.LunarCalendarService
import com.minst.chronoflow.domain.engine.DefaultLunarCalendarService
import kotlinx.datetime.LocalDate
import com.minst.chronoflow.domain.engine.LunarInfo
import java.time.ZoneId
import java.util.Date
import cn.hutool.core.date.ChineseDate

// Strong-typed Hutool usage with fallback to DefaultLunarCalendarService.
fun createPlatformLunarService(): LunarCalendarService {
    val fallback = DefaultLunarCalendarService()
    return object : LunarCalendarService {
        override fun getLunarInfo(date: LocalDate): com.minst.chronoflow.domain.engine.LunarInfo {
            return try {
                // convert kotlinx LocalDate to java.util.Date at system default zone start of day
                val jLocal = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
                val instant = jLocal.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val utilDate = Date.from(instant)

                val lunar = ChineseDate(utilDate)
                var dayStr = try { lunar.chineseDay } catch (_: Throwable) { null }
                val monthStr = try { lunar.chineseMonthName } catch (_: Throwable) { null }
                if (dayStr == "初一") dayStr = monthStr
                val solarTermStr = try { lunar.term } catch (_: Throwable) { null }
                val festivalStr = try { lunar.festivals } catch (_: Throwable) { null }
                val full = try { lunar.toString() } catch (_: Throwable) { "${monthStr ?: ""}${dayStr ?: ""}" }

                // For month/week short display prefer lunar day (or month) over solar term
                val short = solarTermStr?.takeIf { it.isNotBlank() }
                    ?: festivalStr?.takeIf { it.isNotBlank() }
                    ?: dayStr?.takeIf { it.isNotBlank() }
                    ?: ""
                LunarInfo(lunarDate = full ?: "农历", lunarShort = short, festival = festivalStr, solarTerm = solarTermStr)
            } catch (t: Throwable) {
                // If any API mismatch or runtime issue occurs, fallback to local implementation
                fallback.getLunarInfo(date)
            }
        }
    }
}

 


