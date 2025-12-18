package com.minst.chronoflow.data.remote

import com.minst.chronoflow.domain.model.CalendarEvent

/**
 * 负责日历事件的 ICS 文本解析与导出。
 * 当前仅提供接口和最小 Stub，避免阻塞主流程。
 */
interface IcsService {

    /**
     * 将 iCalendar (.ics) 文本解析为内部的 CalendarEvent 列表。
     */
    fun parseIcs(text: String): List<CalendarEvent>

    /**
     * 将一组事件导出为 iCalendar (.ics) 文本。
     */
    fun exportIcs(events: List<CalendarEvent>): String
}

class StubIcsService : IcsService {
    override fun parseIcs(text: String): List<CalendarEvent> {
        // TODO: 实现 RFC 5545 解析，这里先返回空列表作为占位
        return emptyList()
    }

    override fun exportIcs(events: List<CalendarEvent>): String {
        // TODO: 实现 iCalendar 导出逻辑，这里先抛出占位异常
        throw NotImplementedError("ICS export is not implemented yet.")
    }
}


