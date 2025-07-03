package com.mars.ultimatecleaner.data.worker.cleaning

import android.content.Context
import androidx.work.WorkerParameters
import com.mars.ultimatecleaner.data.algorithm.FileScanner
import com.mars.ultimatecleaner.data.repository.CleaningRepository
import com.mars.ultimatecleaner.data.repository.SettingsRepository
import com.mars.ultimatecleaner.data.worker.base.BaseWorker
import com.mars.ultimatecleaner.domain.model.WorkerResult
import com.mars.ultimatecleaner.domain.model.WorkerStatus
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class AutoCleaningWorker(
    context: Context,
    workerParams: WorkerParameters
) : BaseWorker(context, workerParams) {

    @Inject
    lateinit var cleaningRepository: CleaningRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var fileScanner: FileScanner

    override val workerName = "Auto Cleaning"
    override val notificationId = 2001
    override val isLongRunning = true

    override suspend fun executeWork(operationId: String): WorkerResult {
        try {
            val settings = settingsRepository.getSettings()

            // Check if auto-cleaning is enabled
            if (!settings.isAutoCleaningEnabled) {
                return WorkerResult(
                    status = WorkerStatus.SUCCESS,
                    message = "Auto-cleaning is disabled",
                    operationId = operationId
                )
            }

            updateProgress(10, "Scanning for junk files...")

            var totalSpaceSaved = 0L
            var totalFilesDeleted = 0
            val categoriesToClean = mutableListOf<String>()

            // Determine what to clean based on settings
            if (settings.autoCleanCache) categoriesToClean.add("cache")
            if (settings.autoCleanTemp) categoriesToClean.add("temp")
            if (settings.autoCleanApk) categoriesToClean.add("apk")
            if (settings.autoCleanEmptyFolders) categoriesToClean.add("empty_folders")

            if (categoriesToClean.isEmpty()) {
                return WorkerResult(
                    status = WorkerStatus.SUCCESS,
                    message = "No categories selected for auto-cleaning",
                    operationId = operationId
                )
            }

            updateProgress(20, "Starting cleanup operations...")

            // Perform cleaning for each category
            cleaningRepository.cleanFiles(categoriesToClean).collect { progress ->
                val overallProgress = 20 + (progress.percentage * 0.75).toInt()
                updateProgress(overallProgress, progress.currentCategory)

                if (progress.isComplete) {
                    totalSpaceSaved = progress.spaceSaved
                    totalFilesDeleted = progress.cleanedFiles
                }
            }

            updateProgress(95, "Finalizing cleanup...")

            val details = mapOf(
                "spaceSaved" to formatFileSize(totalSpaceSaved),
                "filesDeleted" to totalFilesDeleted,
                "categories" to categoriesToClean.joinToString(", ")
            )

            return WorkerResult(
                status = WorkerStatus.SUCCESS,
                message = "Cleaned $totalFilesDeleted files, saved ${formatFileSize(totalSpaceSaved)}",
                operationId = operationId,
                details = details
            )

        } catch (e: Exception) {
            return WorkerResult(
                status = WorkerStatus.FAILURE,
                message = "Auto-cleaning failed: ${e.message}",
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
        const val WORK_NAME = "auto_cleaning_work"
    }
}