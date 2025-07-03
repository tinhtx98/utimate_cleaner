package com.mars.ultimatecleaner.data.worker.base

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.*
import com.mars.ultimatecleaner.data.database.dao.AppUsageStatsDao
import com.mars.ultimatecleaner.data.worker.notification.WorkerNotificationManager
import com.mars.ultimatecleaner.domain.model.WorkerResult
import com.mars.ultimatecleaner.domain.model.WorkerStatus
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

abstract class BaseWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // Get dependencies manually from Hilt
    protected val notificationManager: WorkerNotificationManager by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            BaseWorkerEntryPoint::class.java
        ).notificationManager()
    }

    protected val appUsageStatsDao: AppUsageStatsDao by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            BaseWorkerEntryPoint::class.java
        ).appUsageStatsDao()
    }

    protected abstract val workerName: String
    protected abstract val notificationId: Int
    protected open val isLongRunning: Boolean = false
    protected open val maxRetryAttempts: Int = 3

    companion object {
        const val KEY_OPERATION_ID = "operation_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_MESSAGE = "message"
        const val KEY_RESULT = "result"
        const val KEY_ERROR = "error"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val operationId = inputData.getString(KEY_OPERATION_ID) ?: generateOperationId()
        val startTime = System.currentTimeMillis()

        try {
            // Track worker usage
            trackWorkerUsage(workerName, startTime)

            // Show initial notification for long-running workers
            if (isLongRunning) {
                setForeground(createInitialForegroundInfo())
            }

            // Set initial progress
            updateProgress(0, "Starting $workerName...")

            // Execute the main work
            val workerResult = executeWork(operationId)

            // Handle result
            return@withContext handleWorkerResult(workerResult, operationId, startTime)

        } catch (e: Exception) {
            handleWorkerError(e, operationId, startTime)
            return@withContext if (runAttemptCount < maxRetryAttempts) {
                Result.retry()
            } else {
                Result.failure(createErrorData(e.message ?: "Unknown error"))
            }
        }
    }

    protected abstract suspend fun executeWork(operationId: String): WorkerResult

    protected suspend fun updateProgress(progress: Int, message: String) {
        val progressData = workDataOf(
            KEY_PROGRESS to progress,
            KEY_MESSAGE to message
        )
        setProgress(progressData)

        // Update notification for long-running workers
        if (isLongRunning) {
            notificationManager.updateProgress(notificationId, progress, message)
        }
    }

    protected suspend fun setForegroundSafely(foregroundInfo: ForegroundInfo) {
        try {
            setForeground(foregroundInfo)
        } catch (e: Exception) {
            // Handle cases where foreground service cannot be started
            android.util.Log.w("BaseWorker", "Failed to set foreground: ${e.message}")
        }
    }

    private fun createInitialForegroundInfo(): ForegroundInfo {
        val notification = notificationManager.createProgressNotification(
            notificationId,
            workerName,
            "Initializing...",
            0
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private suspend fun handleWorkerResult(
        workerResult: WorkerResult,
        operationId: String,
        startTime: Long
    ): Result {
        val duration = System.currentTimeMillis() - startTime

        return when (workerResult.status) {
            WorkerStatus.SUCCESS -> {
                updateProgress(100, "Completed successfully")
                notificationManager.showCompletionNotification(
                    notificationId,
                    workerName,
                    workerResult.message ?: "Operation completed",
                    workerResult.details
                )
                trackWorkerCompletion(workerName, duration, true)
                Result.success(createSuccessData(workerResult))
            }

            WorkerStatus.PARTIAL_SUCCESS -> {
                updateProgress(100, "Completed with warnings")
                notificationManager.showWarningNotification(
                    notificationId,
                    workerName,
                    workerResult.message ?: "Operation completed with warnings",
                    workerResult.details
                )
                trackWorkerCompletion(workerName, duration, true)
                Result.success(createSuccessData(workerResult))
            }

            WorkerStatus.FAILURE -> {
                notificationManager.showErrorNotification(
                    notificationId,
                    workerName,
                    workerResult.message ?: "Operation failed"
                )
                trackWorkerCompletion(workerName, duration, false)
                Result.failure(createErrorData(workerResult.message ?: "Operation failed"))
            }

            WorkerStatus.CANCELLED -> {
                notificationManager.cancelNotification(notificationId)
                Result.failure(createErrorData("Operation was cancelled"))
            }
        }
    }

    private suspend fun handleWorkerError(
        error: Exception,
        operationId: String,
        startTime: Long
    ) {
        val duration = System.currentTimeMillis() - startTime
        val errorMessage = error.message ?: "Unknown error occurred"

        android.util.Log.e("BaseWorker", "Worker $workerName failed: $errorMessage", error)

        notificationManager.showErrorNotification(
            notificationId,
            workerName,
            "Failed: $errorMessage"
        )

        trackWorkerCompletion(workerName, duration, false)
    }

    private fun createSuccessData(result: WorkerResult): Data {
        return workDataOf(
            KEY_RESULT to result.message,
            KEY_PROGRESS to 100
        )
    }

    private fun createErrorData(error: String): Data {
        return workDataOf(
            KEY_ERROR to error,
            KEY_PROGRESS to 0
        )
    }

    private fun generateOperationId(): String {
        return "${workerName}_${System.currentTimeMillis()}"
    }

    private suspend fun trackWorkerUsage(workerName: String, startTime: Long) {
        try {
            appUsageStatsDao.incrementUsageCount(workerName)
        } catch (e: Exception) {
            // Continue without tracking if database is unavailable
        }
    }

    private suspend fun trackWorkerCompletion(
        workerName: String,
        duration: Long,
        success: Boolean
    ) {
        try {
            appUsageStatsDao.updateSessionStats(workerName, duration)
            if (!success) {
                appUsageStatsDao.incrementErrorCount(workerName)
            }
        } catch (e: Exception) {
            // Continue without tracking if database is unavailable
        }
    }

    // Helper methods for creating work constraints
    protected fun createBasicConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
    }

    protected fun createChargingConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .setRequiresStorageNotLow(true)
            .build()
    }

    protected fun createNetworkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
    }
}