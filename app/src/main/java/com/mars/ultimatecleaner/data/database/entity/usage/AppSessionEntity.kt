package com.mars.ultimatecleaner.data.database.entity.usage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mars.ultimatecleaner.domain.model.AppSession

@Entity(
    tableName = "app_sessions",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"]),
        Index(value = ["sessionDuration"], name = "idx_session_duration")
    ],
    foreignKeys = [
        ForeignKey(
            entity = AppUsageStatsEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AppSessionEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val appName: String,
    val startTime: Long,
    val endTime: Long?,
    val sessionDuration: Long,
    val isBackground: Boolean,
    val interruptionCount: Int,
    val memoryUsageMB: Float,
    val cpuUsagePercent: Float,
    val networkBytesUsed: Long,
    val batteryDrainMah: Float,
    val screenBrightness: Int,
    val deviceTemperature: Float,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): AppSession {
        return AppSession(
            id = id,
            packageName = packageName,
            appName = appName,
            startTime = startTime,
            endTime = endTime,
            sessionDuration = sessionDuration,
            isBackground = isBackground,
            interruptionCount = interruptionCount,
            memoryUsageMB = memoryUsageMB,
            cpuUsagePercent = cpuUsagePercent,
            networkBytesUsed = networkBytesUsed,
            batteryDrainMah = batteryDrainMah,
            screenBrightness = screenBrightness,
            deviceTemperature = deviceTemperature
        )
    }
}