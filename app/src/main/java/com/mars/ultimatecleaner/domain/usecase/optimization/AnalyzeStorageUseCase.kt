package com.mars.ultimatecleaner.domain.usecase.optimization

import com.mars.ultimatecleaner.domain.model.StorageAnalysis
import com.mars.ultimatecleaner.domain.repository.OptimizerRepository
import com.mars.ultimatecleaner.domain.repository.StorageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class AnalyzeStorageUseCase @Inject constructor(
    private val storageRepository: StorageRepository,
    private val optimizerRepository: OptimizerRepository
) {

    suspend operator fun invoke(): StorageAnalysis = coroutineScope {
        try {
            // Run storage analysis operations in parallel
            val storageInfoDeferred = async { storageRepository.getStorageInfo() }
            val categoryBreakdownDeferred = async { storageRepository.getCategoryBreakdown() }
            val largeFilesDeferred = async { optimizerRepository.findLargeFiles(50 * 1024 * 1024) } // 50MB+
            val duplicatesDeferred = async { optimizerRepository.findSimilarPhotos() }

            // Await all results
            val storageInfo = storageInfoDeferred.await()
            val categoryBreakdown = categoryBreakdownDeferred.await()
            val largeFiles = largeFilesDeferred.await()
            val duplicates = duplicatesDeferred.await()

            // Calculate cleanable space
            val cacheSize = storageRepository.getCacheSize()
            val tempFilesSize = storageRepository.getTempFilesSize()
            val duplicateSize = duplicates.sumOf { group ->
                group.photos.drop(1).sumOf { it.size }
            }
            val cleanableSpace = cacheSize + tempFilesSize + duplicateSize

            StorageAnalysis(
                totalSpace = storageInfo.totalSpace,
                usedSpace = storageInfo.usedSpace,
                freeSpace = storageInfo.freeSpace,
                usagePercentage = storageInfo.usagePercentage,
                cacheSize = cacheSize,
                junkFilesSize = storageInfo.junkFilesSize,
                cleanableSpace = cleanableSpace,
                categoryBreakdown = categoryBreakdown
            )
        } catch (e: Exception) {
            // Return empty analysis on error
            StorageAnalysis(
                totalSpace = 0L,
                usedSpace = 0L,
                freeSpace = 0L,
                usagePercentage = 0f,
                cacheSize = 0L,
                junkFilesSize = 0L,
                cleanableSpace = 0L,
                categoryBreakdown = emptyMap()
            )
        }
    }
}