package com.mars.ultimatecleaner.data.database.entity.usage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mars.ultimatecleaner.domain.model.AppPerformanceMetrics

@Entity(
    tableName = "app_performance",
    indices = [
        Index(value = ["packageName", "timestamp"]),
        Index(value = ["timestamp"]),
        Index(value = ["memoryUsage"], name = "idx_memory_usage"),
        Index(value = ["cpuUsage"], name = "idx_cpu_usage")
    ]
)
data class AppPerformanceEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val memoryUsage: Long, // bytes
    val cpuUsage: Float, // percentage
    val networkUsage: Long, // bytes
    val batteryDrain: Float, // mAh
    val launchTime: Long, // milliseconds
    val crashCount: Int,
    val anrCount: Int, // Application Not Responding
    val frameDropCount: Int,
    val responseTime: Long, // milliseconds
    val performanceScore: Int, // 0-100
    val stabilityScore: Int, // 0-100
    val energyEfficiencyScore: Int, // 0-100
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): AppPerformanceMetrics {
        return AppPerformanceMetrics(
            packageName = packageName,
            appName = appName,
            timestamp = timestamp,
            memoryUsage = memoryUsage,
            cpuUsage = cpuUsage,
            networkUsage = networkUsage,
            batteryDrain = batteryDrain,
            launchTime = launchTime,
            crashCount = crashCount,
            anrCount = anrCount,
            frameDropCount = frameDropCount,
            responseTime = responseTime,
            performanceScore = performanceScore,
            stabilityScore = stabilityScore,
            energyEfficiencyScore = energyEfficiencyScore
        )
    }
}