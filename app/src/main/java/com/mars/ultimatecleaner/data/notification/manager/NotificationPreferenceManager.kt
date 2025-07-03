package com.mars.ultimatecleaner.data.notification.manager

import android.content.Context
import android.content.SharedPreferences
import com.mars.ultimatecleaner.data.notification.model.NotificationFrequency
import com.mars.ultimatecleaner.data.notification.model.NotificationSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPreferenceManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "notification_preferences"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_MORNING_ENABLED = "morning_enabled"
        private const val KEY_EVENING_ENABLED = "evening_enabled"
        private const val KEY_MORNING_HOUR = "morning_hour"
        private const val KEY_EVENING_HOUR = "evening_hour"
        private const val KEY_WEEKEND_ENABLED = "weekend_enabled"
        private const val KEY_CRITICAL_ALERTS_ENABLED = "critical_alerts_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_LAST_NOTIFICATION_TIME = "last_notification_time"
        private const val KEY_NOTIFICATION_FREQUENCY = "notification_frequency"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun areNotificationsEnabled(): Boolean {
        return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun isMorningNotificationEnabled(): Boolean {
        return preferences.getBoolean(KEY_MORNING_ENABLED, true)
    }

    fun isEveningNotificationEnabled(): Boolean {
        return preferences.getBoolean(KEY_EVENING_ENABLED, true)
    }

    fun getMorningHour(): Int {
        return preferences.getInt(KEY_MORNING_HOUR, 10)
    }

    fun getEveningHour(): Int {
        return preferences.getInt(KEY_EVENING_HOUR, 19)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun setMorningNotificationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_MORNING_ENABLED, enabled).apply()
    }

    fun setEveningNotificationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_EVENING_ENABLED, enabled).apply()
    }

    fun setCustomNotificationTimes(morningHour: Int, eveningHour: Int) {
        preferences.edit()
            .putInt(KEY_MORNING_HOUR, morningHour)
            .putInt(KEY_EVENING_HOUR, eveningHour)
            .apply()
    }

    fun getNotificationSettings(): NotificationSettings {
        return NotificationSettings(
            areNotificationsEnabled = areNotificationsEnabled(),
            morningNotificationsEnabled = isMorningNotificationEnabled(),
            eveningNotificationsEnabled = isEveningNotificationEnabled(),
            morningHour = getMorningHour(),
            eveningHour = getEveningHour(),
            weekendNotificationsEnabled = preferences.getBoolean(KEY_WEEKEND_ENABLED, true),
            criticalAlertsEnabled = preferences.getBoolean(KEY_CRITICAL_ALERTS_ENABLED, true),
            soundEnabled = preferences.getBoolean(KEY_SOUND_ENABLED, true),
            vibrationEnabled = preferences.getBoolean(KEY_VIBRATION_ENABLED, true),
            lastNotificationTime = preferences.getLong(KEY_LAST_NOTIFICATION_TIME, 0L),
            notificationFrequency = NotificationFrequency.valueOf(
                preferences.getString(KEY_NOTIFICATION_FREQUENCY, NotificationFrequency.DAILY.name)
                    ?: NotificationFrequency.DAILY.name
            )
        )
    }

    fun updateLastNotificationTime() {
        preferences.edit().putLong(KEY_LAST_NOTIFICATION_TIME, System.currentTimeMillis()).apply()
    }
}