package com.mars.ultimatecleaner.data.notification.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mars.ultimatecleaner.R
import com.mars.ultimatecleaner.data.notification.model.NotificationContent
import com.mars.ultimatecleaner.data.notification.receiver.NotificationActionReceiver
import com.mars.ultimatecleaner.ui.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartNotificationManager @Inject constructor(
    private val context: Context,
    private val analyticsManager: NotificationAnalyticsManager
) {

    companion object {
        private const val CHANNEL_DAILY_REMINDERS = "daily_reminders"
        private const val CHANNEL_CRITICAL_ALERTS = "critical_alerts"
        private const val CHANNEL_MAINTENANCE = "maintenance"

        private const val GROUP_DAILY_NOTIFICATIONS = "daily_notifications"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_DAILY_REMINDERS,
                    "Daily Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily device health and maintenance reminders"
                    setShowBadge(true)
                    enableLights(true)
                    enableVibration(true)
                },

                NotificationChannel(
                    CHANNEL_CRITICAL_ALERTS,
                    "Critical Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical device issues requiring immediate attention"
                    setShowBadge(true)
                    enableLights(true)
                    enableVibration(true)
                },

                NotificationChannel(
                    CHANNEL_MAINTENANCE,
                    "Maintenance",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Routine maintenance and optimization suggestions"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                }
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(channels)
        }
    }

    fun showScheduledNotification(
        notificationId: Int,
        content: NotificationContent,
        notificationType: String
    ) {
        val channelId = when {
            content.priority == android.app.Notification.PRIORITY_HIGH -> CHANNEL_CRITICAL_ALERTS
            notificationType.contains("maintenance") -> CHANNEL_MAINTENANCE
            else -> CHANNEL_DAILY_REMINDERS
        }

        val notification = buildNotification(content, channelId, notificationId, notificationType)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            analyticsManager.trackNotificationShown(notificationId, notificationType, content.title)
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            analyticsManager.trackNotificationError(notificationId, "permission_denied", e.message)
        }
    }

    private fun buildNotification(
        content: NotificationContent,
        channelId: String,
        notificationId: Int,
        notificationType: String
    ): android.app.Notification {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_id", notificationId)
            putExtra("notification_type", notificationType)
        }

        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(content.icon)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.bigText))
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_DAILY_NOTIFICATIONS)
            .setPriority(content.priority)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        // Add large icon if available
        try {
            val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_large)
            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }
        } catch (e: Exception) {
            // Continue without large icon
        }

        // Add action buttons
        content.actions.forEachIndexed { index, (actionTitle, actionKey) ->
            val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                putExtra("action", actionKey)
                putExtra("notification_id", notificationId)
                putExtra("notification_type", notificationType)
            }

            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId * 10 + index,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(
                getActionIcon(actionKey),
                actionTitle,
                actionPendingIntent
            )
        }

        // Add custom sound for critical notifications
        if (content.priority == android.app.Notification.PRIORITY_HIGH) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
        }

        return builder.build()
    }

    private fun getActionIcon(actionKey: String): Int {
        return when (actionKey) {
            "ACTION_QUICK_CLEAN" -> R.drawable.ic_clean_action
            "ACTION_DEEP_CLEAN" -> R.drawable.ic_deep_clean_action
            "ACTION_OPEN_APP" -> R.drawable.ic_open_action
            "ACTION_VIEW_REPORT" -> R.drawable.ic_report_action
            else -> R.drawable.ic_action_default
        }
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isChannelEnabled(channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        return areNotificationsEnabled()
    }
}