package com.mars.ultimatecleaner.data.database.entity.usage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mars.ultimatecleaner.domain.model.*

@Entity(
    tableName = "app_usage_stats",
    indices = [
        Index(value = ["packageName", "date"], unique = true),
        Index(value = ["date"]),
        Index(value = ["totalTimeInForeground"], name = "idx_usage_time"),
        Index(value = ["lastTimeUsed"], name = "idx_last_used")
    ]
)
data class AppUsageStatsEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val appName: String,
    val date: Long, // Day timestamp (start of day)
    val firstTimeStamp: Long,
    val lastTimeStamp: Long,
    val lastTimeUsed: Long,
    val totalTimeInForeground: Long,
    val totalTimeVisible: Long,
    val launchCount: Int,
    val appLaunchCount: Int,
    val totalTimeInBackground: Long,
    val dataUsageMobile: Long,
    val dataUsageWifi: Long,
    val batteryUsage: Float,
    val category: String,
    val isSystemApp: Boolean,
    val versionCode: Long,
    val installTime: Long,
    val updateTime: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): AppUsageStats {
        return AppUsageStats(
            packageName = packageName,
            appName = appName,
            date = date,
            firstTimeStamp = firstTimeStamp,
            lastTimeStamp = lastTimeStamp,
            lastTimeUsed = lastTimeUsed,
            totalTimeInForeground = totalTimeInForeground,
            totalTimeVisible = totalTimeVisible,
            launchCount = launchCount,
            appLaunchCount = appLaunchCount,
            totalTimeInBackground = totalTimeInBackground,
            dataUsageMobile = dataUsageMobile,
            dataUsageWifi = dataUsageWifi,
            batteryUsage = batteryUsage,
            category = AppCategory.valueOf(category),
            isSystemApp = isSystemApp,
            versionCode = versionCode,
            installTime = installTime,
            updateTime = updateTime
        )
    }
}

fun AppUsageStats.toEntity(): AppUsageStatsEntity {
    return AppUsageStatsEntity(
        id = "${packageName}_${date}",
        packageName = packageName,
        appName = appName,
        date = date,
        firstTimeStamp = firstTimeStamp,
        lastTimeStamp = lastTimeStamp,
        lastTimeUsed = lastTimeUsed,
        totalTimeInForeground = totalTimeInForeground,
        totalTimeVisible = totalTimeVisible,
        launchCount = launchCount,
        appLaunchCount = appLaunchCount,
        totalTimeInBackground = totalTimeInBackground,
        dataUsageMobile = dataUsageMobile,
        dataUsageWifi = dataUsageWifi,
        batteryUsage = batteryUsage,
        category = category.name,
        isSystemApp = isSystemApp,
        versionCode = versionCode,
        installTime = installTime,
        updateTime = updateTime
    )
}