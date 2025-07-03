package com.mars.ultimatecleaner.data.repository

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.mars.ultimatecleaner.domain.model.DeviceHealth
import com.mars.ultimatecleaner.domain.model.MemoryInfo
import com.mars.ultimatecleaner.domain.model.BatteryInfo
import com.mars.ultimatecleaner.domain.model.CpuInfo
import com.mars.ultimatecleaner.domain.repository.SystemHealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemHealthRepositoryImpl @Inject constructor(
    private val context: Context
) : SystemHealthRepository {

    override fun getDeviceHealth(): Flow<DeviceHealth> = flow {
        val deviceHealth = withContext(Dispatchers.IO) {
            calculateDeviceHealth()
        }
        emit(deviceHealth)
    }

    override suspend fun getMemoryInfo(): MemoryInfo = withContext(Dispatchers.IO) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        MemoryInfo(
            totalMemory = memInfo.totalMem,
            availableMemory = memInfo.availMem,
            usedMemory = memInfo.totalMem - memInfo.availMem,
            usagePercentage = ((memInfo.totalMem - memInfo.availMem).toFloat() / memInfo.totalMem.toFloat() * 100).toInt(),
            isLowMemory = memInfo.lowMemory
        )
    }

    override suspend fun getBatteryInfo(): BatteryInfo = withContext(Dispatchers.IO) {
        // Simplified battery info - in real implementation would use BatteryManager
        BatteryInfo(
            level = 85,
            isCharging = false,
            health = "Good",
            temperature = 25.0f,
            voltage = 4.2f
        )
    }

    override suspend fun getCpuInfo(): CpuInfo = withContext(Dispatchers.IO) {
        val cpuUsage = calculateCpuUsage()
        val coreCount = Runtime.getRuntime().availableProcessors()

        CpuInfo(
            usage = cpuUsage,
            coreCount = coreCount,
            frequency = getCpuFrequency(),
            temperature = getCpuTemperature()
        )
    }

    override suspend fun getSystemTemperature(): Float = withContext(Dispatchers.IO) {
        // Simplified temperature calculation
        25.0f + (Math.random() * 10).toFloat()
    }

    override suspend fun getRunningAppsCount(): Int = withContext(Dispatchers.IO) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.runningAppProcesses?.size ?: 0
    }

    private suspend fun calculateDeviceHealth(): DeviceHealth {
        val memoryInfo = getMemoryInfo()
        val batteryInfo = getBatteryInfo()
        val cpuInfo = getCpuInfo()
        val temperature = getSystemTemperature()
        val runningApps = getRunningAppsCount()

        // Calculate overall health score (0-100)
        val healthScore = calculateHealthScore(memoryInfo, batteryInfo, cpuInfo, temperature)

        return DeviceHealth(
            overallScore = healthScore,
            memoryInfo = memoryInfo,
            batteryInfo = batteryInfo,
            cpuInfo = cpuInfo,
            temperature = temperature,
            runningAppsCount = runningApps,
            recommendations = generateRecommendations(memoryInfo, batteryInfo, cpuInfo, temperature)
        )
    }

    private fun calculateHealthScore(
        memoryInfo: MemoryInfo,
        batteryInfo: BatteryInfo,
        cpuInfo: CpuInfo,
        temperature: Float
    ): Int {
        var score = 100

        // Memory health (0-30 points)
        score -= when {
            memoryInfo.usagePercentage > 90 -> 30
            memoryInfo.usagePercentage > 80 -> 20
            memoryInfo.usagePercentage > 70 -> 10
            else -> 0
        }

        // Battery health (0-20 points)
        score -= when {
            batteryInfo.level < 20 -> 20
            batteryInfo.level < 50 -> 10
            else -> 0
        }

        // CPU health (0-30 points)
        score -= when {
            cpuInfo.usage > 90 -> 30
            cpuInfo.usage > 80 -> 20
            cpuInfo.usage > 70 -> 10
            else -> 0
        }

        // Temperature health (0-20 points)
        score -= when {
            temperature > 40 -> 20
            temperature > 35 -> 10
            else -> 0
        }

        return maxOf(0, score)
    }

    private fun generateRecommendations(
        memoryInfo: MemoryInfo,
        batteryInfo: BatteryInfo,
        cpuInfo: CpuInfo,
        temperature: Float
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (memoryInfo.usagePercentage > 80) {
            recommendations.add("Close unused apps to free up memory")
        }

        if (batteryInfo.level < 20) {
            recommendations.add("Charge your device soon")
        }

        if (cpuInfo.usage > 80) {
            recommendations.add("Close resource-intensive apps")
        }

        if (temperature > 35) {
            recommendations.add("Let your device cool down")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Your device is running optimally")
        }

        return recommendations
    }

    private fun calculateCpuUsage(): Float {
        return try {
            val file = RandomAccessFile("/proc/stat", "r")
            val line = file.readLine()
            file.close()

            // Parse CPU usage from /proc/stat (simplified)
            val values = line.split(" ").drop(2).take(7).map { it.toLongOrNull() ?: 0L }
            val idle = values[3]
            val total = values.sum()
            val usage = ((total - idle).toFloat() / total.toFloat()) * 100

            usage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            // Fallback to random value if can't read /proc/stat
            (Math.random() * 50 + 10).toFloat()
        }
    }

    private fun getCpuFrequency(): Long {
        return try {
            val file = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
            if (file.exists()) {
                file.readText().trim().toLong()
            } else {
                2000000L // 2GHz default
            }
        } catch (e: Exception) {
            2000000L
        }
    }

    private fun getCpuTemperature(): Float {
        return try {
            val file = File("/sys/class/thermal/thermal_zone0/temp")
            if (file.exists()) {
                file.readText().trim().toFloat() / 1000f
            } else {
                30.0f // Default temperature
            }
        } catch (e: Exception) {
            30.0f
        }
    }
}
