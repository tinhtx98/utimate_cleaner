package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.model.StorageInfo
import com.mars.ultimatecleaner.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStorageInfoUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    operator fun invoke(): Flow<StorageInfo> {
        return storageRepository.getStorageInfo()
    }
}
