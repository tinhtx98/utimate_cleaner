package com.mars.ultimatecleaner.data.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mars.ultimatecleaner.data.notification.scheduler.DailyNotificationWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationType = intent.getStringExtra("notification_type") ?: "unknown"
        val notificationId = intent.getIntExtra("notification_id", 0)

        // Use WorkManager to handle the notification in the background
        val notificationWork = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
            .setInputData(workDataOf(
                "notification_type" to notificationType,
                "notification_id" to notificationId
            ))
            .addTag("scheduled_notification")
            .build()

        WorkManager.getInstance(context).enqueue(notificationWork)

        // Reschedule for tomorrow
        rescheduleNextAlarm(context, notificationType)
    }

    private fun rescheduleNextAlarm(context: Context, notificationType: String) {
        // This would typically be handled by the NotificationScheduler
        // but we can trigger a reschedule work request here as well
        val rescheduleWork = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
            .setInputData(workDataOf("action" to "reschedule", "type" to notificationType))
            .addTag("reschedule_notification")
            .build()

        WorkManager.getInstance(context).enqueue(rescheduleWork)
    }
}