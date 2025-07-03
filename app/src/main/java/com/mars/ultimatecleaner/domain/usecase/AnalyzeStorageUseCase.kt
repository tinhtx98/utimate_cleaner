package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.repository.FileRepository
import com.mars.ultimatecleaner.domain.model.StorageInfoDomain
import com.mars.ultimatecleaner.domain.model.FileCategoryDomain
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AnalyzeStorageUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(): StorageInfoDomain = coroutineScope {
        val storageInfo = fileRepository.getStorageInfo()

        val categoryBreakdown = mutableMapOf<String, Long>()

        // Analyze each category in parallel
        val photoSize = async { calculateCategorySize(FileCategoryDomain.PHOTOS) }
        val videoSize = async { calculateCategorySize(FileCategoryDomain.VIDEOS) }
        val documentSize = async { calculateCategorySize(FileCategoryDomain.DOCUMENTS) }
        val audioSize = async { calculateCategorySize(FileCategoryDomain.AUDIO) }
        val downloadSize = async { calculateCategorySize(FileCategoryDomain.DOWNLOADS) }
        val appSize = async { calculateCategorySize(FileCategoryDomain.APPS) }

        categoryBreakdown["Photos"] = photoSize.await()
        categoryBreakdown["Videos"] = videoSize.await()
        categoryBreakdown["Documents"] = documentSize.await()
        categoryBreakdown["Audio"] = audioSize.await()
        categoryBreakdown["Downloads"] = downloadSize.await()
        categoryBreakdown["Apps"] = appSize.await()

        val totalCategorized = categoryBreakdown.values.sum()
        val otherSize = maxOf(0, storageInfo.usedSpace - totalCategorized)
        categoryBreakdown["Other"] = otherSize

        storageInfo.copy(categoryBreakdown = categoryBreakdown)
    }

    private suspend fun calculateCategorySize(category: FileCategoryDomain): Long {
        return try {
            fileRepository.getFilesByCategory(category).sumOf { it.size }
        } catch (e: Exception) {
            0L
        }
    }
}