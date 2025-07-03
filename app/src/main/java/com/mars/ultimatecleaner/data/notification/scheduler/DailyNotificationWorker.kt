package com.mars.ultimatecleaner.data.notification.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mars.ultimatecleaner.data.notification.content.NotificationContentManager
import com.mars.ultimatecleaner.data.notification.manager.NotificationPreferenceManager
import com.mars.ultimatecleaner.data.notification.manager.SmartNotificationManager
import dagger.hilt.android.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DailyNotificationWorker(
    @ApplicationContext private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    interface DailyNotificationWorkerEntryPoint {
        fun notificationContentManager(): NotificationContentManager
        fun smartNotificationManager(): SmartNotificationManager
        fun notificationPreferenceManager(): NotificationPreferenceManager
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                DailyNotificationWorkerEntryPoint::class.java
            )

            val contentManager = entryPoint.notificationContentManager()
            val notificationManager = entryPoint.smartNotificationManager()
            val preferenceManager = entryPoint.notificationPreferenceManager()

            val notificationType = inputData.getString("notification_type") ?: "unknown"

            // Check if notifications are still enabled
            if (!preferenceManager.areNotificationsEnabled()) {
                return@withContext Result.success()
            }

            // Check specific type enabled
            val isTypeEnabled = when (notificationType) {
                "morning" -> preferenceManager.isMorningNotificationEnabled()
                "evening" -> preferenceManager.isEveningNotificationEnabled()
                else -> true
            }

            if (!isTypeEnabled) {
                return@withContext Result.success()
            }

            // Generate appropriate notification content
            val content = when (notificationType) {
                "morning" -> contentManager.generateMorningNotification()
                "evening" -> contentManager.generateEveningNotification()
                else -> contentManager.generateMorningNotification() // fallback
            }

            // Determine notification ID
            val notificationId = when (notificationType) {
                "morning" -> 1001
                "evening" -> 1002
                else -> 1000
            }

            // Show notification
            notificationManager.showScheduledNotification(
                notificationId,
                content,
                notificationType
            )

            // Schedule next notification (for exact timing)
            val scheduler = entryPoint.notificationContentManager() // This should be NotificationScheduler
            // scheduler.scheduleNextNotification(notificationType)

            Result.success()

        } catch (e: Exception) {
            // Log error and retry
            android.util.Log.e("DailyNotificationWorker", "Failed to show notification", e)

            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}