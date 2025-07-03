package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.repository.FileRepository
import com.mars.ultimatecleaner.domain.model.StorageInfo
import com.mars.ultimatecleaner.domain.model.FileCategory
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AnalyzeStorageUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(): StorageInfo = coroutineScope {
        val storageInfo = fileRepository.getStorageInfo()

        val categoryBreakdown = mutableMapOf<String, Long>()

        // Analyze each category in parallel
        val photoSize = async { calculateCategorySize(FileCategory.PHOTOS) }
        val videoSize = async { calculateCategorySize(FileCategory.VIDEOS) }
        val documentSize = async { calculateCategorySize(FileCategory.DOCUMENTS) }
        val audioSize = async { calculateCategorySize(FileCategory.AUDIO) }
        val downloadSize = async { calculateCategorySize(FileCategory.DOWNLOADS) }
        val appSize = async { calculateCategorySize(FileCategory.APPS) }

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

    private suspend fun calculateCategorySize(category: FileCategory): Long {
        return try {
            fileRepository.getFilesByCategory(category).sumOf { it.size }
        } catch (e: Exception) {
            0L
        }
    }
}