package com.mars.ultimatecleaner.data.notification.model

data class NotificationContent(
    val title: String,
    val text: String,
    val bigText: String,
    val icon: Int,
    val actions: List<Pair<String, String>>, // title to action key
    val priority: Int
)

data class NotificationTemplate(
    val title: String,
    val text: String,
    val category: String = "general",
    val urgencyLevel: UrgencyLevel = UrgencyLevel.NORMAL
)

enum class UrgencyLevel {
    LOW, NORMAL, HIGH, CRITICAL
}

data class NotificationSettingsData(
    val areNotificationsEnabled: Boolean = true,
    val morningNotificationsEnabled: Boolean = true,
    val eveningNotificationsEnabled: Boolean = true,
    val morningHour: Int = 10,
    val eveningHour: Int = 19,
    val weekendNotificationsEnabled: Boolean = true,
    val criticalAlertsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val lastNotificationTime: Long = 0L,
    val notificationFrequency: NotificationFrequency = NotificationFrequency.DAILY
)

enum class NotificationFrequency {
    DAILY, EVERY_OTHER_DAY, WEEKLY, CUSTOM
}

data class NotificationAnalytics(
    val notificationId: Int,
    val type: String,
    val title: String,
    val showTime: Long,
    val clickTime: Long? = null,
    val action: String? = null,
    val dismissed: Boolean = false
)