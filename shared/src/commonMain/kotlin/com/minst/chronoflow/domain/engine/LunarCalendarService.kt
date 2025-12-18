package com.minst.chronoflow.domain.engine

import kotlinx.datetime.LocalDate

/**
 * 农历与节气服务，只读查询接口。
 */
interface LunarCalendarService {
    fun getLunarInfo(date: LocalDate): LunarInfo
}

data class LunarInfo(
    val lunarDate: String,
    val festival: String?,
    val solarTerm: String?,
)

class StubLunarCalendarService : LunarCalendarService {
    override fun getLunarInfo(date: LocalDate): LunarInfo {
        // TODO: 接入真实农历算法或第三方库
        // 当前简单返回公历日期字符串作为占位。
        return LunarInfo(
            lunarDate = "农历占位(${date})",
            festival = null,
            solarTerm = null,
        )
    }
}


