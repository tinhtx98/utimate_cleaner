package com.mars.ultimatecleaner.data.worker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mars.ultimatecleaner.R
import com.mars.ultimatecleaner.ui.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerNotificationManager @Inject constructor(
    private val context: Context
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_PROGRESS = "worker_progress"
        private const val CHANNEL_COMPLETION = "worker_completion"
        private const val CHANNEL_ERROR = "worker_error"
        private const val CHANNEL_MAINTENANCE = "worker_maintenance"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_PROGRESS,
                "Background Operations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of background cleaning and analysis operations"
                setShowBadge(false)
            },

            NotificationChannel(
                CHANNEL_COMPLETION,
                "Operation Results",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows completion status of background operations"
                setShowBadge(true)
            },

            NotificationChannel(
                CHANNEL_ERROR,
                "Operation Errors",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows errors from background operations"
                setShowBadge(true)
            },

            NotificationChannel(
                CHANNEL_MAINTENANCE,
                "Maintenance Operations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows maintenance and optimization operations"
                setShowBadge(false)
            }
        )

        notificationManager.createNotificationChannels(channels)
    }

    fun createProgressNotification(
        notificationId: Int,
        title: String,
        content: String,
        progress: Int
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_cleaning)
            .setContentTitle(title)
            .setContentText(content)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    fun updateProgress(notificationId: Int, progress: Int, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_cleaning)
            .setContentTitle("Background Operation")
            .setContentText(message)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showCompletionNotification(
        notificationId: Int,
        title: String,
        message: String,
        details: Map<String, Any>? = null
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle("$title Completed")
            .setContentText(message)
            .setStyle(createBigTextStyle(message, details))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showWarningNotification(
        notificationId: Int,
        title: String,
        message: String,
        details: Map<String, Any>? = null
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("$title Completed with Warnings")
            .setContentText(message)
            .setStyle(createBigTextStyle(message, details))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showErrorNotification(
        notificationId: Int,
        title: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ERROR)
            .setSmallIcon(R.drawable.ic_error)
            .setContentTitle("$title Failed")
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showMaintenanceNotification(
        notificationId: Int,
        title: String,
        message: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_MAINTENANCE)
            .setSmallIcon(R.drawable.ic_maintenance)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun createBigTextStyle(message: String, details: Map<String, Any>?): NotificationCompat.BigTextStyle {
        val bigText = StringBuilder(message)

        details?.let { detailMap ->
            bigText.append("\n\nDetails:")
            detailMap.forEach { (key, value) ->
                bigText.append("\n• $key: $value")
            }
        }

        return NotificationCompat.BigTextStyle().bigText(bigText.toString())
    }
}