package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import com.mars.ultimatecleaner.domain.model.ScanProgress
import com.mars.ultimatecleaner.domain.model.ScanResult
import com.mars.ultimatecleaner.domain.model.JunkCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import javax.inject.Inject

class ScanJunkFilesUseCase @Inject constructor(
    private val cleaningRepository: CleaningRepository
) {
    operator fun invoke(): Flow<ScanResult> = flow {
        val startTime = System.currentTimeMillis()
        val junkCategories = mutableListOf<JunkCategory>()
        var totalSize = 0L
        var totalFiles = 0

        cleaningRepository.scanJunkFiles()
            .collect { progress ->
                emit(
                    ScanResult(
                        junkCategories = junkCategories.toList(),
                        totalSize = totalSize,
                        totalFiles = totalFiles,
                        scanDuration = System.currentTimeMillis() - startTime,
                        isComplete = progress.isComplete,
                        progress = progress
                    )
                )

                if (progress.isComplete) {
                    // Final scan complete, get all categories
                    val categories = cleaningRepository.getJunkCategories()
                    junkCategories.addAll(categories)
                    totalSize = categories.sumOf { it.size }
                    totalFiles = categories.sumOf { it.fileCount }

                    emit(
                        ScanResult(
                            junkCategories = junkCategories.toList(),
                            totalSize = totalSize,
                            totalFiles = totalFiles,
                            scanDuration = System.currentTimeMillis() - startTime,
                            isComplete = true,
                            progress = progress
                        )
                    )
                }
            }
    }
}

data class ScanResult(
    val junkCategories: List<JunkCategory>,
    val totalSize: Long,
    val totalFiles: Int,
    val scanDuration: Long,
    val isComplete: Boolean,
    val progress: ScanProgress
)