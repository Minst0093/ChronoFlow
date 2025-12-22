package com.minst.chronoflow.android

import androidx.compose.ui.graphics.Color

fun getEventTypeColor(type: com.minst.chronoflow.domain.model.EventType): Color {
    return when (type) {
        com.minst.chronoflow.domain.model.EventType.STUDY -> Color(0xFF2196F3)
        com.minst.chronoflow.domain.model.EventType.WORK -> Color(0xFFFF9800)
        com.minst.chronoflow.domain.model.EventType.LIFE -> Color(0xFF4CAF50)
        com.minst.chronoflow.domain.model.EventType.HEALTH -> Color(0xFFE91E63)
        com.minst.chronoflow.domain.model.EventType.ENTERTAINMENT -> Color(0xFF9C27B0)
    }
}

fun getEventTypeLabel(type: com.minst.chronoflow.domain.model.EventType): String {
    return when (type) {
        com.minst.chronoflow.domain.model.EventType.STUDY -> "学习"
        com.minst.chronoflow.domain.model.EventType.WORK -> "工作"
        com.minst.chronoflow.domain.model.EventType.LIFE -> "生活"
        com.minst.chronoflow.domain.model.EventType.HEALTH -> "健康"
        com.minst.chronoflow.domain.model.EventType.ENTERTAINMENT -> "娱乐"
    }
}


