package com.mars.ultimatecleaner.data.worker.cleaning

import android.content.Context
import androidx.work.WorkerParameters
import com.mars.ultimatecleaner.data.repository.CleaningRepository
import com.mars.ultimatecleaner.data.utils.FileUtils
import com.mars.ultimatecleaner.data.worker.base.BaseWorker
import com.mars.ultimatecleaner.domain.model.WorkerResult
import com.mars.ultimatecleaner.domain.model.WorkerStatus
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class CacheCleaningWorker(
    context: Context,
    workerParams: WorkerParameters
) : BaseWorker(context, workerParams) {

    @Inject
    lateinit var cleaningRepository: CleaningRepository

    @Inject
    lateinit var fileUtils: FileUtils

    override val workerName = "Cache Cleaning"
    override val notificationId = 2002
    override val isLongRunning = true

    override suspend fun executeWork(operationId: String): WorkerResult {
        try {
            updateProgress(10, "Scanning cache files...")

            val cacheFiles = cleaningRepository.getCacheFiles()
            val totalCacheSize = cacheFiles.sumOf { it.size }

            if (cacheFiles.isEmpty()) {
                return WorkerResult(
                    status = WorkerStatus.SUCCESS,
                    message = "No cache files found to clean",
                    operationId = operationId
                )
            }

            updateProgress(30, "Cleaning cache files...")

            var spaceSaved = 0L
            var filesDeleted = 0

            cleaningRepository.cleanFiles(listOf("cache")).collect { progress ->
                val overallProgress = 30 + (progress.percentage * 0.65).toInt()
                updateProgress(overallProgress, "Cleaning: ${progress.currentFile}")

                if (progress.isComplete) {
                    spaceSaved = progress.spaceSaved
                    filesDeleted = progress.cleanedFiles
                }
            }

            // Clean app-specific cache
            updateProgress(95, "Cleaning app cache...")
            val appCacheCleared = clearAppCache()

            val details = mapOf(
                "spaceSaved" to formatFileSize(spaceSaved),
                "filesDeleted" to filesDeleted,
                "appCacheCleared" to appCacheCleared
            )

            return WorkerResult(
                status = WorkerStatus.SUCCESS,
                message = "Cache cleaned successfully. Freed ${formatFileSize(spaceSaved)}",
                operationId = operationId,
                details = details
            )

        } catch (e: Exception) {
            return WorkerResult(
                status = WorkerStatus.FAILURE,
                message = "Cache cleaning failed: ${e.message}",
                operationId = operationId
            )
        }
    }

    private suspend fun clearAppCache(): Boolean {
        return try {
            val cacheDir = applicationContext.cacheDir
            val externalCacheDir = applicationContext.externalCacheDir

            var cleared = false

            cacheDir?.let { dir ->
                if (dir.exists()) {
                    fileUtils.deleteFilesSafely(dir.listFiles()?.toList() ?: emptyList())
                    cleared = true
                }
            }

            externalCacheDir?.let { dir ->
                if (dir.exists()) {
                    fileUtils.deleteFilesSafely(dir.listFiles()?.toList() ?: emptyList())
                    cleared = true
                }
            }

            cleared
        } catch (e: Exception) {
            false
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
        const val WORK_NAME = "cache_cleaning_work"
    }
}