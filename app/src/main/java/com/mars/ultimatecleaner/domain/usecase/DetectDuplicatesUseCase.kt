package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.repository.OptimizerRepository
import com.mars.ultimatecleaner.domain.model.DuplicateAnalysisProgress
import com.mars.ultimatecleaner.domain.model.DuplicateAnalysisResult
import com.mars.ultimatecleaner.domain.model.DuplicateGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DetectDuplicatesUseCase @Inject constructor(
    private val optimizerRepository: OptimizerRepository
) {
    operator fun invoke(): Flow<DuplicateAnalysisResult> = flow {
        val startTime = System.currentTimeMillis()
        val duplicateGroups = mutableListOf<DuplicateGroup>()
        var totalSize = 0L
        var totalFiles = 0

        optimizerRepository.detectDuplicates()
            .collect { progress ->
                emit(
                    DuplicateAnalysisResult(
                        duplicateGroups = duplicateGroups.toList(),
                        totalSize = totalSize,
                        totalFiles = totalFiles,
                        analysisTime = System.currentTimeMillis() - startTime,
                        isComplete = false,
                        progress = progress
                    )
                )

                if (progress.isComplete) {
                    val groups = optimizerRepository.getDuplicateGroups()
                    duplicateGroups.addAll(groups)
                    totalSize = groups.sumOf { it.totalSize }
                    totalFiles = groups.sumOf { it.files.size }

                    emit(
                        DuplicateAnalysisResult(
                            duplicateGroups = duplicateGroups.toList(),
                            totalSize = totalSize,
                            totalFiles = totalFiles,
                            analysisTime = System.currentTimeMillis() - startTime,
                            isComplete = true,
                            progress = progress
                        )
                    )
                }
            }
    }
}

data class DuplicateAnalysisResult(
    val duplicateGroups: List<DuplicateGroup>,
    val totalSize: Long,
    val totalFiles: Int,
    val analysisTime: Long,
    val isComplete: Boolean,
    val progress: DuplicateAnalysisProgress
)