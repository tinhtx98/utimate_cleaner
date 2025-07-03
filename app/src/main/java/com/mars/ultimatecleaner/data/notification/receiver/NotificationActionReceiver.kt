package com.mars.ultimatecleaner.data.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mars.ultimatecleaner.data.worker.cleaning.CacheCleaningWorker
import com.mars.ultimatecleaner.data.worker.cleaning.AutoCleaningWorker
import com.mars.ultimatecleaner.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action") ?: return
        val notificationId = intent.getIntExtra("notification_id", 0)
        val notificationType = intent.getStringExtra("notification_type") ?: "unknown"

        // Track action in analytics
        // analyticsManager.trackNotificationAction(notificationId, action)

        when (action) {
            "ACTION_QUICK_CLEAN" -> {
                // Start quick cleaning
                val quickCleanWork = OneTimeWorkRequestBuilder<CacheCleaningWorker>()
                    .addTag("notification_triggered")
                    .build()
                WorkManager.getInstance(context).enqueue(quickCleanWork)
            }

            "ACTION_DEEP_CLEAN" -> {
                // Start comprehensive cleaning
                val deepCleanWork = OneTimeWorkRequestBuilder<AutoCleaningWorker>()
                    .addTag("notification_triggered")
                    .build()
                WorkManager.getInstance(context).enqueue(deepCleanWork)
            }

            "ACTION_OPEN_APP" -> {
                // Open main app
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("from_notification", true)
                    putExtra("notification_type", notificationType)
                }
                context.startActivity(appIntent)
            }

            "ACTION_VIEW_REPORT" -> {
                // Open app to reports section
                val reportIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "reports")
                    putExtra("from_notification", true)
                }
                context.startActivity(reportIntent)
            }
        }

        // Cancel the notification after action
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(notificationId)
    }
}