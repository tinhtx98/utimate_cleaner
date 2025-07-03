package com.mars.ultimatecleaner.domain.repository

import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SystemHealthRepository {

    // Performance Metrics
    suspend fun getPerformanceMetrics(): PerformanceMetricsSystem
    fun observePerformanceMetrics(): Flow<PerformanceMetricsSystem>
    suspend fun getCpuUsage(): Float
    suspend fun getMemoryUsage(): MemoryUsage
    suspend fun getBatteryInfo(): BatteryInfo
    suspend fun getStoragePerformance(): StoragePerformance

    // Device Temperature
    suspend fun getDeviceTemperature(): DeviceTemperature
    fun observeTemperature(): Flow<DeviceTemperature>
    suspend fun getThermalState(): ThermalState
    suspend fun getTemperatureHistory(hours: Int = 24): List<TemperatureReading>

    // Network Performance
    suspend fun getNetworkPerformance(): NetworkPerformance
    suspend fun testNetworkSpeed(): NetworkSpeedTest
    suspend fun getDataUsage(): DataUsage
    suspend fun getWifiInfo(): WifiInfo

    // System Stability
    suspend fun getSystemStability(): SystemStability
    suspend fun getCrashHistory(): List<CrashReport>
    suspend fun getAppCrashStats(): Map<String, Int>
    suspend fun recordCrash(crashReport: CrashReport)

    // Resource Usage
    suspend fun getResourceUsageHistory(days: Int = 7): List<ResourceUsageSnapshot>
    suspend fun saveResourceSnapshot(snapshot: ResourceUsageSnapshot)
    suspend fun getAverageResourceUsage(): AverageResourceUsage

    // Health Scoring
    suspend fun calculateOverallHealthScore(): Int
    suspend fun calculatePerformanceScore(): Int
    suspend fun calculateStabilityScore(): Int
    suspend fun calculateBatteryHealthScore(): Int

    // Monitoring
    fun startPerformanceMonitoring(): Flow<PerformanceUpdate>
    suspend fun stopPerformanceMonitoring()
    suspend fun getMonitoringStatus(): MonitoringStatus
}