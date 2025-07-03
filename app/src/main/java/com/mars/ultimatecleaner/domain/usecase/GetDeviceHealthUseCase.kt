package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.model.DeviceHealth
import com.mars.ultimatecleaner.domain.repository.SystemHealthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDeviceHealthUseCase @Inject constructor(
    private val systemHealthRepository: SystemHealthRepository
) {
    operator fun invoke(): Flow<DeviceHealth> {
        return systemHealthRepository.getDeviceHealth()
    }
}
