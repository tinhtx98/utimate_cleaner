package com.mars.ultimatecleaner.data.worker.analysis

import android.content.Context
import androidx.work.WorkerParameters
import com.mars.ultimatecleaner.data.algorithm.DuplicateDetector
import com.mars.ultimatecleaner.domain.repository.FileRepository
import com.mars.ultimatecleaner.domain.repository.OptimizerRepository
import com.mars.ultimatecleaner.data.worker.base.BaseWorker
import com.mars.ultimatecleaner.domain.model.FileCategoryDomain
import com.mars.ultimatecleaner.domain.model.WorkerResult
import com.mars.ultimatecleaner.domain.model.WorkerStatus
import dagger.hilt.android.EntryPointAccessors

class DuplicateDetectionWorker(
    context: Context,
    workerParams: WorkerParameters
) : BaseWorker(context, workerParams) {

    // Get dependencies manually from Hilt
    private val duplicateDetector: DuplicateDetector by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            DuplicateDetectionWorkerEntryPoint::class.java
        ).duplicateDetector()
    }

    private val fileRepository: FileRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            DuplicateDetectionWorkerEntryPoint::class.java
        ).fileRepository()
    }

    private val optimizerRepository: OptimizerRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            DuplicateDetectionWorkerEntryPoint::class.java
        ).optimizerRepository()
    }

    override val workerName = "Duplicate Detection"
    override val notificationId = 3001
    override val isLongRunning = true

    override suspend fun executeWork(operationId: String): WorkerResult {
        try {
            updateProgress(5, "Gathering files for analysis...")

            // Get files to analyze based on input parameters
            val categoryFilter = inputData.getString("category")
            val files = when (categoryFilter) {
                "images" -> fileRepository.getFilesByCategory(FileCategoryDomain.PHOTOS)
                "videos" -> fileRepository.getFilesByCategory(FileCategoryDomain.VIDEOS)
                "documents" -> fileRepository.getFilesByCategory(FileCategoryDomain.DOCUMENTS)
                "audio" -> fileRepository.getFilesByCategory(FileCategoryDomain.AUDIO)
                else -> fileRepository.getFilesByCategory(FileCategoryDomain.ALL)
            }

            if (files.isEmpty()) {
                return WorkerResult(
                    status = WorkerStatus.SUCCESS,
                    message = "No files found for duplicate analysis",
                    operationId = operationId
                )
            }

            updateProgress(10, "Starting duplicate detection...")

            var duplicateGroups = 0
            var totalDuplicates = 0
            var potentialSavings = 0L

            duplicateDetector.detectDuplicates(files).collect { progress ->
                val overallProgress = 10 + (progress.percentage * 0.85).toInt()
                updateProgress(overallProgress, progress.currentOperation)

                if (progress.isComplete) {
                    duplicateGroups = progress.duplicateGroups.size
                    totalDuplicates = progress.duplicateGroups.sumOf { it.files.size - 1 }
                    potentialSavings = progress.duplicateGroups.sumOf {
                        it.totalSize - (it.files.maxOfOrNull { file -> file.size } ?: 0L)
                    }

                    // Save results to database
                    progress.duplicateGroups.forEach { group ->
                        optimizerRepository.saveDuplicateGroup(group)
                    }
                }
            }

            updateProgress(95, "Finalizing results...")

            val details = mapOf(
                "duplicateGroups" to duplicateGroups,
                "totalDuplicates" to totalDuplicates,
                "potentialSavings" to formatFileSize(potentialSavings),
                "filesAnalyzed" to files.size
            )

            val message = if (duplicateGroups > 0) {
                "Found $duplicateGroups duplicate groups with $totalDuplicates files. " +
                        "Potential savings: ${formatFileSize(potentialSavings)}"
            } else {
                "No duplicate files found"
            }

            return WorkerResult(
                status = WorkerStatus.SUCCESS,
                message = message,
                operationId = operationId,
                details = details
            )

        } catch (e: Exception) {
            return WorkerResult(
                status = WorkerStatus.FAILURE,
                message = "Duplicate detection failed: ${e.message}",
                operationId = operationId
            )
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.1f %s".format(size, units[unitIndex])
    }

    companion object {
        const val WORK_NAME = "duplicate_detection_work"
    }
}