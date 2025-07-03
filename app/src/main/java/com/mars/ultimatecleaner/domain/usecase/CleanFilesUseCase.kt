package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import com.mars.ultimatecleaner.domain.repository.AnalyticsRepository
import com.mars.ultimatecleaner.domain.model.CleaningProgressDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CleanFilesUseCase @Inject constructor(
    private val cleaningRepository: CleaningRepository,
    private val analyticsRepository: AnalyticsRepository
) {
    operator fun invoke(categories: List<String>): Flow<CleaningResult> = flow {
        val startTime = System.currentTimeMillis()
        var totalSpaceSaved = 0L
        var cleanedFiles = 0
        var failedFiles = 0

        cleaningRepository.cleanFiles(categories)
            .collect { progress ->
                emit(
                    CleaningResult(
                        cleanedCategories = categories,
                        totalSpaceSaved = totalSpaceSaved,
                        cleanedFiles = cleanedFiles,
                        failedFiles = failedFiles,
                        cleaningDuration = System.currentTimeMillis() - startTime,
                        isComplete = progress.isComplete,
                        progress = progress
                    )
                )

                if (progress.isComplete) {
                    totalSpaceSaved = progress.spaceSaved
                    cleanedFiles = progress.cleanedFiles

                    // Track the cleaning operation
                    val operation = CleaningOperation(
                        categories = categories,
                        spaceSaved = totalSpaceSaved,
                        filesDeleted = cleanedFiles,
                        duration = System.currentTimeMillis() - startTime,
                        timestamp = System.currentTimeMillis()
                    )

                    analyticsRepository.trackCleaningOperation(operation)

                    // Save the cleaning result
                    val result = CleaningResult(
                        cleanedCategories = categories,
                        totalSpaceSaved = totalSpaceSaved,
                        cleanedFiles = cleanedFiles,
                        failedFiles = failedFiles,
                        cleaningDuration = System.currentTimeMillis() - startTime,
                        isComplete = true,
                        progress = progress
                    )

                    cleaningRepository.saveCleaningResult(result)

                    emit(result)
                }
            }
    }
}

data class CleaningResult(
    val cleanedCategories: List<String>,
    val totalSpaceSaved: Long,
    val cleanedFiles: Int,
    val failedFiles: Int,
    val cleaningDuration: Long,
    val isComplete: Boolean,
    val progress: CleaningProgressDomain
)

data class CleaningOperation(
    val categories: List<String>,
    val spaceSaved: Long,
    val filesDeleted: Int,
    val duration: Long,
    val timestamp: Long
)