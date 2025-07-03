package com.mars.ultimatecleaner.data.worker.base

import com.mars.ultimatecleaner.data.database.dao.AppUsageStatsDao
import com.mars.ultimatecleaner.data.worker.notification.WorkerNotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BaseWorkerEntryPoint {
    fun notificationManager(): WorkerNotificationManager
    fun appUsageStatsDao(): AppUsageStatsDao
}
