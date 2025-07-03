package com.mars.ultimatecleaner.domain.model

data class RecentActivity(
    val id: String,
    val type: ActivityType,
    val title: String,
    val description: String,
    val timestamp: Long,
    val icon: String
) {
    enum class ActivityType {
        CLEANING,
        ANALYSIS,
        OPTIMIZATION,
        SCAN
    }
}

data class SystemHealth(
    val overallScore: Int,
    val cpuUsage: Float,
    val memoryUsage: Float,
    val batteryLevel: Int,
    val temperature: Float,
    val storageUsage: Float,
    val networkSpeed: Float,
    val recommendations: List<String>
)
