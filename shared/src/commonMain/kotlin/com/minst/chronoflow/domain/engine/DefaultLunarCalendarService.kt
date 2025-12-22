package com.minst.chronoflow.domain.engine

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.floor

/**
 * Default local implementation of LunarCalendarService.
 * - Supports years 1900..2050
 * - Uses classic lookup arrays (lunarInfo and solar term info) and common algorithms.
 * - Provides a small in-memory cache keyed by LocalDate for fast repeated queries.
 *
 * Note: This implementation aims for correctness for common dates (春节、中秋、二十四节气等)
 * within 1900..2050. See DEV_NOTES for limitations.
 */
class DefaultLunarCalendarService(
    private val cacheSize: Int = 1024
) : LunarCalendarService {

    // LRU-like cache (simple eviction when size exceeded)
    private val cache = LinkedHashMap<LocalDate, LunarInfo>()

    // lunarInfo table for years 1900..2050 (classic compact encoding)
    private val lunarInfo = intArrayOf(
        0x04bd8,0x04ae0,0x0a570,0x054d5,0x0d260,0x0d950,0x16554,0x056a0,0x09ad0,0x055d2,
        0x04ae0,0x0a5b6,0x0a4d0,0x0d250,0x1d255,0x0b540,0x0d6a0,0x0ada2,0x095b0,0x14977,
        0x04970,0x0a4b0,0x0b4b5,0x06a50,0x06d40,0x1ab54,0x02b60,0x09570,0x052f2,0x04970,
        0x06566,0x0d4a0,0x0ea50,0x06e95,0x05ad0,0x02b60,0x186e3,0x092e0,0x1c8d7,0x0c950,
        0x0d4a0,0x1d8a6,0x0b550,0x056a0,0x1a5b4,0x025d0,0x092d0,0x0d2b2,0x0a950,0x0b557,
        0x06ca0,0x0b550,0x15355,0x04da0,0x0a5d0,0x14573,0x052d0,0x0a9a8,0x0e950,0x06aa0,
        0x0aea6,0x0ab50,0x04b60,0x0aae4,0x0a570,0x05260,0x0f263,0x0d950,0x05b57,0x056a0,
        0x096d0,0x04dd5,0x04ad0,0x0a4d0,0x0d4d4,0x0d250,0x0d558,0x0b540,0x0b5a0,0x195a6,
        0x095b0,0x049b0,0x0a974,0x0a4b0,0x0b27a,0x06a50,0x06d40,0x0af46,0x0ab60,0x09570,
        0x04af5,0x04970,0x064b0,0x074a3,0x0ea50,0x06b58,0x05ac0,0x0ab60,0x096d5,0x092e0,
        0x0c960,0x0d954,0x0d4a0,0x0da50,0x07552,0x056a0,0x0abb7,0x025d0,0x092d0,0x0cab5,
        0x0a950,0x0b4a0,0x0baa4,0x0ad50,0x055d9,0x04ba0,0x0a5b0,0x15176,0x052b0,0x0a930,
        0x07954,0x06aa0,0x0ad50,0x05b52,0x04b60,0x0a6e6,0x0a4e0,0x0d260,0x0ea65,0x0d530,
        0x05aa0,0x076a3,0x096d0,0x04bd7,0x04ad0,0x0a4d0,0x1d0b6,0x0d250,0x0d520,0x0dd45,
        0x0b5a0,0x056d0,0x055b2,0x049b0,0x0a577,0x0a4b0,0x0aa50,0x1b255,0x06d20,0x0ada0,
        0x14b63
    )

    private val solarTermNames = arrayOf(
        "小寒","大寒","立春","雨水","惊蛰","春分","清明","谷雨",
        "立夏","小满","芒种","夏至","小暑","大暑","立秋","处暑",
        "白露","秋分","寒露","霜降","立冬","小雪","大雪","冬至"
    )

    private val sTermInfo = intArrayOf(
        0,21208,42467,63836,85337,107014,128867,150921,173149,195551,218072,240693,
        263343,285989,308563,331033,353350,375494,397447,419210,440795,462224,483532,504758
    )

    private val solarFestivalMap = mapOf(
        Pair(1,1) to "元旦"
    )

    private val lunarFestivalMap = mapOf(
        Pair(1,1) to "春节",
        Pair(1,15) to "元宵节",
        Pair(5,5) to "端午节",
        Pair(7,7) to "七夕",
        Pair(8,15) to "中秋节",
        Pair(9,9) to "重阳节"
    )

    override fun getLunarInfo(date: LocalDate): LunarInfo {
        synchronized(cache) {
            cache[date]?.let { return it }
        }

        val lunar = convertSolarToLunar(date)
        val solarTerm = getSolarTermForDate(date)
        val festival = lunarFestivalMap[Pair(lunar.lunarMonth, lunar.lunarDay)]
        val dayNames = arrayOf("","初一","初二","初三","初四","初五","初六","初七","初八","初九","初十",
            "十一","十二","十三","十四","十五","十六","十七","十八","十九","二十",
            "廿一","廿二","廿三","廿四","廿五","廿六","廿七","廿八","廿九","三十")
        // For month/week short display: prefer festival name when present, otherwise show lunar day.
        val lunarShort = festival ?: dayNames.getOrElse(lunar.lunarDay) { lunar.lunarDay.toString() }
        val lunarDateText = buildLunarText(lunar) // full-ish text
        val info = LunarInfo(lunarDate = "农历$lunarDateText", lunarShort = lunarShort, festival = festival, solarTerm = solarTerm)

        synchronized(cache) {
            cache[date] = info
            if (cache.size > cacheSize) {
                val it = cache.keys.iterator()
                if (it.hasNext()) {
                    it.next()
                    it.remove()
                }
            }
        }
        return info
    }

    private data class LunarDate(val lunarYear: Int, val lunarMonth: Int, val lunarDay: Int, val isLeap: Boolean)

    // Build short lunar text like "初一" or "闰四月初三"
    private fun buildLunarText(ld: LunarDate): String {
        val dayNames = arrayOf("","初一","初二","初三","初四","初五","初六","初七","初八","初九","初十",
            "十一","十二","十三","十四","十五","十六","十七","十八","十九","二十",
            "廿一","廿二","廿三","廿四","廿五","廿六","廿七","廿八","廿九","三十")
        val monthNames = arrayOf("","正月","二月","三月","四月","五月","六月","七月","八月","九月","十月","冬月","腊月")
        val monthText = (if (ld.isLeap) "闰" else "") + monthNames.getOrElse(ld.lunarMonth) { "${ld.lunarMonth}月" }
        val dayText = dayNames.getOrElse(ld.lunarDay) { ld.lunarDay.toString() }
        return if (ld.lunarDay == 1) monthText else dayText
    }

    // Convert Gregorian (solar) date to lunar date for 1900..2050
    private fun convertSolarToLunar(date: LocalDate): LunarDate {
        // base date: 1900-01-31 is lunar 1900-01-01
        val baseDate = LocalDate(1900,1,31)
        var offset = date.daysUntil(baseDate).let { -it } // days since baseDate

        var year = 1900
        var lunarYearDays = yearDays(year)
        while (offset >= lunarYearDays) {
            offset -= lunarYearDays
            year++
            lunarYearDays = yearDays(year)
        }

        val leapMonth = leapMonth(year)
        var isLeap = false
        var month = 1
        var monthDays = 0
        while (true) {
            if (month == leapMonth + 1 && !isLeap) {
                // leap month
                monthDays = leapDays(year)
                isLeap = true
            } else {
                monthDays = monthDays(year, month)
            }
            if (offset < monthDays) break
            offset -= monthDays
            if (isLeap && month == leapMonth + 1) {
                isLeap = false
            } else {
                month++
            }
        }
        val day = offset + 1
        val lunarMonth = if (isLeap && month == leapMonth + 1) month - 1 else month
        return LunarDate(lunarYear = year, lunarMonth = lunarMonth, lunarDay = day.toInt(), isLeap = isLeap)
    }

    // days between two LocalDate (date2 - date1)
    private fun LocalDate.daysUntil(other: LocalDate): Long {
        // approximate by converting to epoch days via LocalDateTime at midnight UTC
        val thisInstant = LocalDateTime(this.year, this.monthNumber, this.dayOfMonth, 0, 0).toInstant(TimeZone.UTC)
        val otherInstant = LocalDateTime(other.year, other.monthNumber, other.dayOfMonth, 0, 0).toInstant(TimeZone.UTC)
        return (otherInstant.toEpochMilliseconds() - thisInstant.toEpochMilliseconds()) / (24L * 3600L * 1000L)
    }

    private fun yearDays(y: Int): Int {
        var sum = 348
        val info = lunarInfo[y - 1900]
        var m = 0x8000
        for (i in 0 until 12) {
            if ((info and m) != 0) sum += 1
            m = m shr 1
        }
        return sum + leapDays(y)
    }

    private fun leapMonth(y: Int): Int {
        return lunarInfo[y - 1900] and 0xf
    }

    private fun leapDays(y: Int): Int {
        return if (leapMonth(y) != 0) {
            if ((lunarInfo[y - 1900] and 0x10000) != 0) 30 else 29
        } else 0
    }

    private fun monthDays(y: Int, m: Int): Int {
        val info = lunarInfo[y - 1900]
        return if ((info and (0x10000 shr m)) != 0) 30 else 29
    }

    // Compute solar term for the date, return term name if matches, else null.
    private fun getSolarTermForDate(date: LocalDate): String? {
        val year = date.year
        if (year < 1900 || year > 2050) return null
        // base instant for calculations: 1900-01-06 02:05 UTC
        val base = LocalDateTime(1900,1,6,2,5).toInstant(TimeZone.UTC).toEpochMilliseconds()
        for (i in 0 until 24) {
            val ms = (base + ((31556925974.7 * (year - 1900) + sTermInfo[i] * 60000L).toLong()))
            val termDate = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).date
            if (termDate == date) return solarTermNames[i]
        }
        return null
    }
}


