package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import com.mars.ultimatecleaner.domain.repository.OptimizerRepository
import com.mars.ultimatecleaner.domain.repository.AnalyticsRepository
import com.mars.ultimatecleaner.domain.model.SmartSuggestion
import com.mars.ultimatecleaner.domain.model.SuggestionPriority
import com.mars.ultimatecleaner.domain.model.SuggestionAction
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetSmartSuggestionsUseCase @Inject constructor(
    private val cleaningRepository: CleaningRepository,
    private val optimizerRepository: OptimizerRepository,
    private val analyticsRepository: AnalyticsRepository
) {
    suspend operator fun invoke(): List<SmartSuggestion> = coroutineScope {
        val suggestions = mutableListOf<SmartSuggestion>()

        // Get various types of suggestions in parallel
        val cacheFilesDeferred = async { getCacheFilesSuggestion() }
        val duplicatesDeferred = async { getDuplicatesSuggestion() }
        val largeFilesDeferred = async { getLargeFilesSuggestion() }
        val emptyFoldersDeferred = async { getEmptyFoldersSuggestion() }
        val screenshotsDeferred = async { getScreenshotsSuggestion() }
        val blurryPhotosDeferred = async { getBlurryPhotosSuggestion() }

        // Collect all suggestions
        cacheFilesDeferred.await()?.let { suggestions.add(it) }
        duplicatesDeferred.await()?.let { suggestions.add(it) }
        largeFilesDeferred.await()?.let { suggestions.add(it) }
        emptyFoldersDeferred.await()?.let { suggestions.add(it) }
        screenshotsDeferred.await()?.let { suggestions.add(it) }
        blurryPhotosDeferred.await()?.let { suggestions.add(it) }

        // Sort by priority and potential savings
        suggestions.sortedWith(
            compareByDescending<SmartSuggestion> { it.priority.ordinal }
                .thenByDescending { it.potentialSavings }
        )
    }

    private suspend fun getCacheFilesSuggestion(): SmartSuggestion? {
        return try {
            val cacheFiles = cleaningRepository.getCacheFiles()
            val totalSize = cacheFiles.sumOf { it.size }

            if (totalSize > 50 * 1024 * 1024) { // More than 50MB
                SmartSuggestion(
                    id = "cache_files",
                    title = "Clear Cache Files",
                    description = "Remove ${cacheFiles.size} cache files to free up space",
                    potentialSavings = totalSize,
                    priority = when {
                        totalSize > 500 * 1024 * 1024 -> SuggestionPriority.HIGH
                        totalSize > 200 * 1024 * 1024 -> SuggestionPriority.MEDIUM
                        else -> SuggestionPriority.LOW
                    },
                    actionType = SuggestionAction.CLEAN_CACHE
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getDuplicatesSuggestion(): SmartSuggestion? {
        return try {
            val duplicateGroups = optimizerRepository.getDuplicateGroups()
            val totalSize = duplicateGroups.sumOf { it.totalSize }
            val duplicateCount = duplicateGroups.sumOf { it.files.size - 1 }

            if (duplicateCount > 0) {
                SmartSuggestion(
                    id = "duplicates",
                    title = "Remove Duplicate Files",
                    description = "Delete $duplicateCount duplicate files",
                    potentialSavings = totalSize,
                    priority = when {
                        duplicateCount > 100 -> SuggestionPriority.HIGH
                        duplicateCount > 20 -> SuggestionPriority.MEDIUM
                        else -> SuggestionPriority.LOW
                    },
                    actionType = SuggestionAction.DELETE_DUPLICATES
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getLargeFilesSuggestion(): SmartSuggestion? {
        return try {
            val largeFiles = cleaningRepository.getLargeFiles(100) // 100MB threshold
            val totalSize = largeFiles.sumOf { it.size }

            if (largeFiles.isNotEmpty()) {
                SmartSuggestion(
                    id = "large_files",
                    title = "Review Large Files",
                    description = "Found ${largeFiles.size} files larger than 100MB",
                    potentialSavings = totalSize,
                    priority = SuggestionPriority.MEDIUM,
                    actionType = SuggestionAction.REMOVE_LARGE_FILES
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getEmptyFoldersSuggestion(): SmartSuggestion? {
        return try {
            val emptyFolders = cleaningRepository.getEmptyFolders()

            if (emptyFolders.size > 5) {
                SmartSuggestion(
                    id = "empty_folders",
                    title = "Remove Empty Folders",
                    description = "Delete ${emptyFolders.size} empty folders",
                    potentialSavings = 0L,
                    priority = SuggestionPriority.LOW,
                    actionType = SuggestionAction.CLEAN_CACHE
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getScreenshotsSuggestion(): SmartSuggestion? {
        return try {
            val oldScreenshots = optimizerRepository.getOldScreenshots(30)
            val totalSize = oldScreenshots.sumOf { it.size }

            if (oldScreenshots.isNotEmpty()) {
                SmartSuggestion(
                    id = "old_screenshots",
                    title = "Delete Old Screenshots",
                    description = "Remove ${oldScreenshots.size} screenshots older than 30 days",
                    potentialSavings = totalSize,
                    priority = SuggestionPriority.MEDIUM,
                    actionType = SuggestionAction.DELETE_DUPLICATES
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getBlurryPhotosSuggestion(): SmartSuggestion? {
        return try {
            val blurryPhotos = optimizerRepository.getBlurryPhotos()
            val totalSize = blurryPhotos.sumOf { it.size }

            if (blurryPhotos.isNotEmpty()) {
                SmartSuggestion(
                    id = "blurry_photos",
                    title = "Remove Blurry Photos",
                    description = "Delete ${blurryPhotos.size} blurry or low-quality photos",
                    potentialSavings = totalSize,
                    priority = SuggestionPriority.MEDIUM,
                    actionType = SuggestionAction.OPTIMIZE_PHOTOS
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}