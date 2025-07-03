package com.mars.ultimatecleaner.data.notification.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.mars.ultimatecleaner.data.notification.content.NotificationContentManager
import com.mars.ultimatecleaner.data.notification.manager.NotificationPreferenceManager
import com.mars.ultimatecleaner.data.notification.receiver.NotificationAlarmReceiver
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    private val context: Context,
    private val contentManager: NotificationContentManager,
    private val preferenceManager: NotificationPreferenceManager
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val MORNING_NOTIFICATION_ID = 1001
        private const val EVENING_NOTIFICATION_ID = 1002
        private const val MORNING_HOUR = 10
        private const val EVENING_HOUR = 19 // 7 PM

        private const val MORNING_REQUEST_CODE = 10001
        private const val EVENING_REQUEST_CODE = 10002

        const val WORK_TAG_MORNING = "morning_notification"
        const val WORK_TAG_EVENING = "evening_notification"
    }

    fun scheduleAllNotifications() {
        if (!preferenceManager.areNotificationsEnabled()) {
            return
        }

        scheduleMorningNotification()
        scheduleEveningNotification()
        scheduleWorkManagerBackup()
    }

    private fun scheduleMorningNotification() {
        if (!preferenceManager.isMorningNotificationEnabled()) {
            cancelMorningNotification()
            return
        }

        val morningTime = getNextScheduleTime(MORNING_HOUR)
        scheduleExactNotification(
            morningTime,
            MORNING_REQUEST_CODE,
            "morning",
            MORNING_NOTIFICATION_ID
        )
    }

    private fun scheduleEveningNotification() {
        if (!preferenceManager.isEveningNotificationEnabled()) {
            cancelEveningNotification()
            return
        }

        val eveningTime = getNextScheduleTime(EVENING_HOUR)
        scheduleExactNotification(
            eveningTime,
            EVENING_REQUEST_CODE,
            "evening",
            EVENING_NOTIFICATION_ID
        )
    }

    private fun scheduleExactNotification(
        triggerTime: Long,
        requestCode: Int,
        notificationType: String,
        notificationId: Int
    ) {
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            putExtra("notification_type", notificationType)
            putExtra("notification_id", notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback to WorkManager if exact alarms are not allowed
            scheduleWithWorkManager(notificationType, triggerTime)
        }
    }

    private fun scheduleWorkManagerBackup() {
        // Morning notification backup
        val morningConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val morningWork = PeriodicWorkRequestBuilder<DailyNotificationWorker>(1, java.util.concurrent.TimeUnit.DAYS)
            .setConstraints(morningConstraints)
            .setInitialDelay(calculateInitialDelay(MORNING_HOUR), java.util.concurrent.TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG_MORNING)
            .setInputData(workDataOf("notification_type" to "morning"))
            .build()

        workManager.enqueueUniquePeriodicWork(
            "morning_notification_backup",
            ExistingPeriodicWorkPolicy.KEEP,
            morningWork
        )

        // Evening notification backup
        val eveningWork = PeriodicWorkRequestBuilder<DailyNotificationWorker>(1, java.util.concurrent.TimeUnit.DAYS)
            .setConstraints(morningConstraints)
            .setInitialDelay(calculateInitialDelay(EVENING_HOUR), java.util.concurrent.TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG_EVENING)
            .setInputData(workDataOf("notification_type" to "evening"))
            .build()

        workManager.enqueueUniquePeriodicWork(
            "evening_notification_backup",
            ExistingPeriodicWorkPolicy.KEEP,
            eveningWork
        )
    }

    private fun scheduleWithWorkManager(notificationType: String, triggerTime: Long) {
        val delay = triggerTime - System.currentTimeMillis()

        val notificationWork = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("notification_type" to notificationType))
            .addTag("notification_fallback")
            .build()

        workManager.enqueue(notificationWork)
    }

    private fun getNextScheduleTime(hour: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the time has passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return calendar.timeInMillis
    }

    private fun calculateInitialDelay(hour: Int): Long {
        val targetTime = getNextScheduleTime(hour)
        return targetTime - System.currentTimeMillis()
    }

    fun cancelMorningNotification() {
        val intent = Intent(context, NotificationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        workManager.cancelAllWorkByTag(WORK_TAG_MORNING)
    }

    fun cancelEveningNotification() {
        val intent = Intent(context, NotificationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            EVENING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        workManager.cancelAllWorkByTag(WORK_TAG_EVENING)
    }

    fun cancelAllNotifications() {
        cancelMorningNotification()
        cancelEveningNotification()
        workManager.cancelAllWorkByTag("notification_fallback")
    }

    fun rescheduleNotifications() {
        cancelAllNotifications()
        scheduleAllNotifications()
    }

    fun updateNotificationTimes(morningHour: Int, eveningHour: Int) {
        preferenceManager.setCustomNotificationTimes(morningHour, eveningHour)
        rescheduleNotifications()
    }
}