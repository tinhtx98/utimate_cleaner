package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.model.SystemHealth
import com.mars.ultimatecleaner.domain.repository.SystemHealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetSystemHealthUseCase @Inject constructor(
    private val systemHealthRepository: SystemHealthRepository
) {
    operator fun invoke(): Flow<SystemHealth> {
        return systemHealthRepository.getDeviceHealth().map { deviceHealth ->
            SystemHealth(
                overallScore = deviceHealth.overallScore,
                cpuUsage = deviceHealth.cpuInfo.usage,
                memoryUsage = deviceHealth.memoryInfo.usagePercentage.toFloat(),
                batteryLevel = deviceHealth.batteryInfo.level,
                temperature = deviceHealth.temperature,
                storageUsage = calculateStorageUsage(deviceHealth),
                networkSpeed = 0f, // Would be calculated from network repository
                recommendations = deviceHealth.recommendations
            )
        }
    }

    private fun calculateStorageUsage(deviceHealth: com.mars.ultimatecleaner.domain.model.DeviceHealth): Float {
        // This would be calculated from storage repository if available
        // For now, return a reasonable default
        return 65.0f
    }
}
