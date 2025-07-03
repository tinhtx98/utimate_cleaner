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
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            // Reschedule all notifications after boot
            val rescheduleWork = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                .setInputData(workDataOf("action" to "reschedule_all"))
                .addTag("boot_reschedule")
                .build()

            WorkManager.getInstance(context).enqueue(rescheduleWork)
        }
    }
}