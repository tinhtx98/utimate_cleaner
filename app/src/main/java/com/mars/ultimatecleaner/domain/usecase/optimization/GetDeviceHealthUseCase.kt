package com.mars.ultimatecleaner.domain.usecase.optimization

import com.mars.ultimatecleaner.domain.model.StorageInfo
import com.mars.ultimatecleaner.domain.repository.OptimizerRepository
import com.mars.ultimatecleaner.domain.repository.SystemHealthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetDeviceHealthUseCase @Inject constructor(
    private val optimizerRepository: OptimizerRepository,
    private val storageRepository: StorageRepository,
    private val systemHealthRepository: SystemHealthRepository
) {

    suspend operator fun invoke(): DeviceHealth = coroutineScope {
        try {
            // Run all health checks in parallel
            val optimizationScoreDeferred = async { optimizerRepository.getDeviceOptimizationScore() }
            val performanceMetricsDeferred = async { systemHealthRepository.getPerformanceMetrics() }
            val storageInfoDeferred = async { storageRepository.getStorageInfo() }
            val batteryInfoDeferred = async { systemHealthRepository.getBatteryInfo() }
            val systemStabilityDeferred = async { systemHealthRepository.getSystemStability() }

            // Await all results
            val optimizationScore = optimizationScoreDeferred.await()
            val performanceMetrics = performanceMetricsDeferred.await()
            val storageInfo = storageInfoDeferred.await()
            val batteryInfo = batteryInfoDeferred.await()
            val systemStability = systemStabilityDeferred.await()

            // Calculate individual health scores
            val storageHealth = calculateStorageHealth(storageInfo.usagePercentage, storageInfo.freeSpace)
            val performanceHealth = calculatePerformanceHealth(
                performanceMetrics.cpuUsage,
                performanceMetrics.ramUsage,
                performanceMetrics.availableMemory
            )
            val batteryHealth = calculateBatteryHealth(batteryInfo.level, batteryInfo.health)
            val stabilityHealth = calculateStabilityHealth(systemStability.crashCount, systemStability.uptime)

            // Calculate overall health score
            val overallScore = calculateOverallHealth(
                storageHealth,
                performanceHealth,
                batteryHealth,
                stabilityHealth,
                optimizationScore
            )

            // Generate recommendations
            val recommendations = generateHealthRecommendations(
                storageHealth,
                performanceHealth,
                batteryHealth,
                stabilityHealth,
                storageInfo,
                performanceMetrics,
                batteryInfo
            )

            DeviceHealth(
                overallScore = overallScore,
                storageHealth = storageHealth,
                performanceHealth = performanceHealth,
                batteryHealth = batteryHealth,
                securityHealth = 85, // Placeholder - implement security health check
                recommendations = recommendations,
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // Return default health status on error
            DeviceHealth(
                overallScore = 50,
                storageHealth = 50,
                performanceHealth = 50,
                batteryHealth = 50,
                securityHealth = 50,
                recommendations = listOf("Unable to assess device health: ${e.message}"),
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    private fun calculateStorageHealth(usagePercentage: Float, freeSpace: Long): Int {
        return when {
            usagePercentage > 95 -> 10
            usagePercentage > 90 -> 25
            usagePercentage > 85 -> 40
            usagePercentage > 80 -> 60
            usagePercentage > 70 -> 75
            usagePercentage > 60 -> 85
            else -> 95
        }
    }

    private fun calculatePerformanceHealth(cpuUsage: Float, ramUsage: Float, availableMemory: Long): Int {
        val cpuScore = when {
            cpuUsage > 90 -> 10
            cpuUsage > 80 -> 30
            cpuUsage > 70 -> 50
            cpuUsage > 60 -> 70
            cpuUsage > 40 -> 85
            else -> 95
        }

        val ramScore = when {
            ramUsage > 95 -> 10
            ramUsage > 90 -> 25
            ramUsage > 85 -> 40
            ramUsage > 80 -> 60
            ramUsage > 70 -> 75
            ramUsage > 60 -> 85
            else -> 95
        }

        return (cpuScore + ramScore) / 2
    }

    private fun calculateBatteryHealth(level: Int, health: String): Int {
        val healthScore = when (health.lowercase()) {
            "good" -> 95
            "overheat" -> 40
            "over_voltage" -> 30
            "unspecified_failure" -> 20
            "cold" -> 60
            "dead" -> 0
            else -> 70
        }

        val levelScore = when {
            level < 5 -> 20
            level < 10 -> 40
            level < 20 -> 60
            level < 30 -> 80
            else -> 95
        }

        return minOf(healthScore, levelScore)
    }

    private fun calculateStabilityHealth(crashCount: Int, uptime: Long): Int {
        val crashScore = when {
            crashCount > 10 -> 20
            crashCount > 5 -> 40
            crashCount > 2 -> 60
            crashCount > 0 -> 80
            else -> 95
        }

        val uptimeScore = if (uptime > 24 * 60 * 60 * 1000) 95 else 70 // 24 hours

        return (crashScore + uptimeScore) / 2
    }

    private fun calculateOverallHealth(
        storageHealth: Int,
        performanceHealth: Int,
        batteryHealth: Int,
        stabilityHealth: Int,
        optimizationScore: Int
    ): Int {
        // Weighted average with storage and performance having higher weights
        val weightedSum = (storageHealth * 0.25 +
                performanceHealth * 0.25 +
                batteryHealth * 0.2 +
                stabilityHealth * 0.15 +
                optimizationScore * 0.15)

        return weightedSum.toInt().coerceIn(0, 100)
    }

    private fun generateHealthRecommendations(
        storageHealth: Int,
        performanceHealth: Int,
        batteryHealth: Int,
        stabilityHealth: Int,
        storageInfo: StorageInfo,
        performanceMetrics: com.mars.ultimatecleaner.domain.model.PerformanceMetrics,
        batteryInfo: com.mars.ultimatecleaner.domain.model.BatteryInfo
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (storageHealth < 60) {
            recommendations.add("Free up storage space - ${storageInfo.usagePercentage.toInt()}% full")
        }

        if (performanceHealth < 60) {
            if (performanceMetrics.ramUsage > 85) {
                recommendations.add("Close unused apps to free up RAM")
            }
            if (performanceMetrics.cpuUsage > 80) {
                recommendations.add("Reduce CPU usage by optimizing background apps")
            }
        }

        if (batteryHealth < 60) {
            recommendations.add("Optimize battery usage to improve battery life")
        }

        if (stabilityHealth < 60) {
            recommendations.add("Check for app crashes and system stability issues")
        }

        return recommendations
    }
}