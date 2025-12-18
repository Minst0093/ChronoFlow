package com.minst.chronoflow.data.remote

import com.minst.chronoflow.domain.model.CalendarEvent

/**
 * 远程只读日历源（如网络订阅、云日历等）的抽象。
 * 目前仅提供接口和假数据 Stub。
 */
interface RemoteEventSource {

    /**
     * 从远程源拉取事件列表。
     * @param url 订阅地址或远程标识
     */
    suspend fun fetchEvents(url: String): List<CalendarEvent>
}

class StubRemoteEventSource : RemoteEventSource {
    override suspend fun fetchEvents(url: String): List<CalendarEvent> {
        // TODO: 接入真实网络和解析逻辑
        // 当前返回空列表作为占位，避免阻塞主流程。
        return emptyList()
    }
}


