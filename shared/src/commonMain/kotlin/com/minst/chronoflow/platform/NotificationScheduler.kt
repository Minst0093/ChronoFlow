package com.minst.chronoflow.platform

import com.minst.chronoflow.domain.model.CalendarEvent

interface NotificationScheduler {
    fun schedule(event: CalendarEvent)
    fun cancel(eventId: String)
}


