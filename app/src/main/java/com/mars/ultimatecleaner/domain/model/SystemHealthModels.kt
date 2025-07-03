package com.mars.ultimatecleaner.domain.model

data class PerformanceMetricsSystem(
    val cpuUsage: Float = 0f,
    val ramUsage: Float = 0f,
    val batteryLevel: Int = 0,
    val temperature: Float = 0f,
    val availableMemory: Long = 0L,
    val networkSpeed: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class MemoryUsage(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val usagePercentage: Float,
    val buffers: Long = 0L,
    val cached: Long = 0L
)

data class BatteryInfo(
    val level: Int,
    val health: String,
    val temperature: Float,
    val voltage: Int,
    val chargingStatus: String,
    val powerSource: String,
    val estimatedTimeRemaining: Int? = null
)

data class StoragePerformance(
    val readSpeed: Float, // MB/s
    val writeSpeed: Float, // MB/s
    val healthStatus: String,
    val temperature: Float
)

data class DeviceTemperature(
    val cpuTemperature: Float,
    val batteryTemperature: Float,
    val ambientTemperature: Float,
    val thermalState: ThermalState,
    val timestamp: Long = System.currentTimeMillis()
)

data class TemperatureReading(
    val temperature: Float,
    val timestamp: Long,
    val source: String
)

data class NetworkPerformance(
    val wifiStrength: Int,
    val connectionType: String,
    val downloadSpeed: Float, // Mbps
    val uploadSpeed: Float, // Mbps
    val latency: Int, // ms
    val dataUsage: DataUsage
)

data class NetworkSpeedTest(
    val downloadSpeed: Float,
    val uploadSpeed: Float,
    val latency: Int,
    val testDuration: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class DataUsage(
    val mobileDataUsed: Long,
    val wifiDataUsed: Long,
    val totalDataUsed: Long,
    val period: String, // "daily", "monthly", etc.
    val timestamp: Long = System.currentTimeMillis()
)

data class WifiInfo(
    val ssid: String,
    val signalStrength: Int,
    val frequency: Int,
    val linkSpeed: Int,
    val securityType: String
)

data class SystemStability(
    val uptime: Long,
    val crashCount: Int,
    val lastCrash: Long?,
    val memoryErrors: Int,
    val stabilityScore: Int
)

data class CrashReport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val appName: String,
    val packageName: String,
    val crashType: String,
    val timestamp: Long,
    val stackTrace: String?,
    val deviceInfo: String?
)

data class ResourceUsageSnapshot(
    val timestamp: Long,
    val cpuUsage: Float,
    val memoryUsage: Float,
    val storageUsage: Float,
    val batteryLevel: Int,
    val networkActivity: Float
)

data class AverageResourceUsage(
    val averageCpuUsage: Float,
    val averageMemoryUsage: Float,
    val averageStorageUsage: Float,
    val averageBatteryDrain: Float,
    val peakUsageTimes: List<Long>
)

data class PerformanceUpdate(
    val cpuUsage: Float,
    val memoryUsage: Float,
    val temperature: Float,
    val batteryLevel: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class MonitoringStatus(
    val isActive: Boolean,
    val startTime: Long?,
    val monitoredComponents: List<String>,
    val samplingInterval: Long
)

data class DeviceHealth(
    val overallScore: Int, // 0-100
    val memoryInfo: MemoryInfo,
    val batteryInfo: BatteryInfo,
    val cpuInfo: CpuInfo,
    val temperature: Float,
    val runningAppsCount: Int,
    val recommendations: List<String>
)

data class MemoryInfo(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val usagePercentage: Int,
    val isLowMemory: Boolean
)

data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean,
    val health: String,
    val temperature: Float,
    val voltage: Float
)

data class CpuInfo(
    val usage: Float, // 0-100%
    val coreCount: Int,
    val frequency: Long, // Hz
    val temperature: Float
)

data class StorageInfo(
    val totalSpace: Long,
    val availableSpace: Long,
    val usedSpace: Long,
    val usagePercentage: Int,
    val breakdown: Map<String, Long>
)

enum class ThermalState {
    NOT_THROTTLING,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN
}