package com.mars.ultimatecleaner.domain.model

data class AppUsageStats(
    val packageName: String,
    val appName: String,
    val date: Long,
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
    val category: AppCategory,
    val isSystemApp: Boolean,
    val versionCode: Long,
    val installTime: Long,
    val updateTime: Long
)

data class AppSession(
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
    val deviceTemperature: Float
)

data class AppPerformanceMetrics(
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val memoryUsage: Long,
    val cpuUsage: Float,
    val networkUsage: Long,
    val batteryDrain: Float,
    val launchTime: Long,
    val crashCount: Int,
    val anrCount: Int,
    val frameDropCount: Int,
    val responseTime: Long,
    val performanceScore: Int,
    val stabilityScore: Int,
    val energyEfficiencyScore: Int
)

enum class AppCategory {
    PRODUCTIVITY, SOCIAL, ENTERTAINMENT, GAMES, UTILITIES, SYSTEM, COMMUNICATION, OTHER
}