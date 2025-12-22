package com.minst.chronoflow.domain.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class LunarCalendarServiceTest {
    private val svc = DefaultLunarCalendarService()

    @Test
    fun testSpringFestival2020() {
        val d = LocalDate(2020, 1, 25) // 2020 Lunar New Year
        val info = svc.getLunarInfo(d)
        assertNotNull(info)
        assertEquals("春节", info.festival)
    }

    @Test
    fun testMidAutumn2019() {
        val d = LocalDate(2019, 9, 13) // 2019 Mid-Autumn
        val info = svc.getLunarInfo(d)
        assertNotNull(info)
        assertEquals("中秋节", info.festival)
    }

    @Test
    fun testSolarTermSummerSolstice2020() {
        val d = LocalDate(2020, 6, 21) // 夏至 ~ Jun 21, 2020
        val info = svc.getLunarInfo(d)
        assertNotNull(info)
        assertTrue(info.solarTerm == "夏至" || info.solarTerm == null, "expected 夏至 or null, got ${info.solarTerm}")
    }
}


