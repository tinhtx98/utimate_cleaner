package com.mars.ultimatecleaner.data.worker.optimization

import android.content.Context
import androidx.work.WorkerParameters
import com.mars.ultimatecleaner.data.algorithm.CompressionEngine
import com.mars.ultimatecleaner.data.repository.OptimizerRepository
import com.mars.ultimatecleaner.data.worker.base.BaseWorker
import com.mars.ultimatecleaner.domain.model.CompressionLevelDomain
import com.mars.ultimatecleaner.domain.model.WorkerResult
import com.mars.ultimatecleaner.domain.model.WorkerStatus
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class MediaCompressionWorker(
    context: Context,
    workerParams: WorkerParameters
) : BaseWorker(context, workerParams) {

    @Inject
    lateinit var compressionEngine: CompressionEngine

    @Inject
    lateinit var optimizerRepository: OptimizerRepository

    override val workerName = "Media Compression"
    override val notificationId = 4001
    override val isLongRunning = true

    override suspend fun executeWork(operationId: String): WorkerResult {
        try {
            // Get compression parameters from input
            val filePaths = inputData.getStringArray("file_paths")?.toList() ?: emptyList()
            val compressionLevelName = inputData.getString("compression_level") ?: "MEDIUM"
            val compressionLevelDomain = CompressionLevelDomain.valueOf(compressionLevelName)

            if (filePaths.isEmpty()) {
                return WorkerResult(
                    status = WorkerStatus.SUCCESS,
                    message = "No files specified for compression",
                    operationId = operationId
                )
            }

            updateProgress(10, "Starting compression...")

            var compressedFiles = 0
            var totalSpaceSaved = 0L
            var failedFiles = 0

            optimizerRepository.compressMedia(filePaths, compressionLevelDomain).collect { progress ->
                val overallProgress = 10 + (progress.percentage * 0.85).toInt()
                updateProgress(overallProgress, "Compressing: ${progress.currentFile}")

                if (progress.isComplete) {
                    compressedFiles = progress.processedFiles
                    totalSpaceSaved = progress.spaceSaved
                }
            }

            updateProgress(95, "Finalizing compression...")

            val details = mapOf(
                "compressedFiles" to compressedFiles,
                "spaceSaved" to formatFileSize(totalSpaceSaved),
                "compressionLevel" to compressionLevelDomain.displayName,
                "failedFiles" to failedFiles
            )

            val status = if (failedFiles == 0) {
                WorkerStatus.SUCCESS
            } else if (compressedFiles > 0) {
                WorkerStatus.PARTIAL_SUCCESS
            } else {
                WorkerStatus.FAILURE
            }

            val message = when (status) {
                WorkerStatus.SUCCESS ->
                    "Compressed $compressedFiles files, saved ${formatFileSize(totalSpaceSaved)}"
                WorkerStatus.PARTIAL_SUCCESS ->
                    "Compressed $compressedFiles files ($failedFiles failed), saved ${formatFileSize(totalSpaceSaved)}"
                else ->
                    "Compression failed for all files"
            }

            return WorkerResult(
                status = status,
                message = message,
                operationId = operationId,
                details = details
            )

        } catch (e: Exception) {
            return WorkerResult(
                status = WorkerStatus.FAILURE,
                message = "Media compression failed: ${e.message}",
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
        const val WORK_NAME = "media_compression_work"
    }
}