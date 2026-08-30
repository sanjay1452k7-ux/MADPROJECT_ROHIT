package com.cit.attendance.domain.model

import java.time.LocalDateTime

/** Categories drive the icon and accent used by the notification component. */
enum class NotificationType {
    ATTENDANCE_MARKED,
    ABSENCE_RECORDED,
    LOW_ATTENDANCE_WARNING,
    ANNOUNCEMENT,
    SYSTEM,
}

/** An in-app notification (Section 32). */
data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: LocalDateTime,
    val isRead: Boolean,
)
