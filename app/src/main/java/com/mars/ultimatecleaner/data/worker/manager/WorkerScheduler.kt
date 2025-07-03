package com.mars.ultimatecleaner.data.worker.manager

import android.content.Context
import androidx.work.*
import com.mars.ultimatecleaner.data.worker.analysis.DuplicateDetectionWorker
import com.mars.ultimatecleaner.data.worker.analysis.PhotoAnalysisWorker
import com.mars.ultimatecleaner.data.worker.cleaning.AutoCleaningWorker
import com.mars.ultimatecleaner.data.worker.cleaning.CacheCleaningWorker
import com.mars.ultimatecleaner.data.worker.optimization.MediaCompressionWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerScheduler @Inject constructor(
    private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    // Scheduled cleaning operations
    fun scheduleAutoCleaningDaily() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val dailyCleaningRequest = PeriodicWorkRequestBuilder<AutoCleaningWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS) // Wait 1 hour after scheduling
            .addTag("auto_cleaning")
            .addTag("daily")
            .build()

        workManager.enqueueUniquePeriodicWork(
            AutoCleaningWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyCleaningRequest
        )
    }

    fun scheduleWeeklyCacheCleanup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(true)
            .setRequiresBatteryNotLow(true)
            .build()

        val weeklyCacheCleanup = PeriodicWorkRequestBuilder<CacheCleaningWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(6, TimeUnit.HOURS)
            .addTag("cache_cleaning")
            .addTag("weekly")
            .build()

        workManager.enqueueUniquePeriodicWork(
            CacheCleaningWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            weeklyCacheCleanup
        )
    }

    // Analysis operations
    fun scheduleDuplicateDetection(category: String? = null) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .setRequiresBatteryNotLow(true)
            .build()

        val inputData = workDataOf("category" to category)

        val duplicateDetectionRequest = OneTimeWorkRequestBuilder<DuplicateDetectionWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("duplicate_detection")
            .addTag("analysis")
            .build()

        workManager.enqueueUniqueWork(
            DuplicateDetectionWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            duplicateDetectionRequest
        )
    }

    fun schedulePhotoAnalysis() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false) // Can run without charging for photos
            .setRequiresBatteryNotLow(true)
            .build()

        val photoAnalysisRequest = OneTimeWorkRequestBuilder<PhotoAnalysisWorker>()
            .setConstraints(constraints)
            .addTag("photo_analysis")
            .addTag("analysis")
            .build()

        workManager.enqueueUniqueWork(
            PhotoAnalysisWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            photoAnalysisRequest
        )
    }

    // Optimization operations
    fun scheduleMediaCompression(
        filePaths: List<String>,
        compressionLevel: String = "MEDIUM"
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(true) // Compression is resource intensive
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val inputData = workDataOf(
            "file_paths" to filePaths.toTypedArray(),
            "compression_level" to compressionLevel
        )

        val compressionRequest = OneTimeWorkRequestBuilder<MediaCompressionWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("media_compression")
            .addTag("optimization")
            .build()

        workManager.enqueueUniqueWork(
            MediaCompressionWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            compressionRequest
        )
    }

    // Expedited operations (immediate execution)
    fun scheduleExpeditedCacheCleanup() {
        val expeditedRequest = OneTimeWorkRequestBuilder<CacheCleaningWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("cache_cleaning")
            .addTag("expedited")
            .build()

        workManager.enqueue(expeditedRequest)
    }

    // Worker management
    fun cancelAllWork() {
        workManager.cancelAllWork()
    }

    fun cancelWorkByTag(tag: String) {
        workManager.cancelAllWorkByTag(tag)
    }

    fun cancelWorkByName(workName: String) {
        workManager.cancelUniqueWork(workName)
    }

    fun getWorkInfoByTag(tag: String) = workManager.getWorkInfosByTag(tag)

    fun getWorkInfoByName(workName: String) = workManager.getWorkInfosForUniqueWork(workName)

    // Monitor work progress
    fun observeWorkProgress(workName: String) = workManager.getWorkInfosForUniqueWorkLiveData(workName)

    fun observeWorkByTag(tag: String) = workManager.getWorkInfosByTagLiveData(tag)

    // Constraints helpers
    fun createLowResourceConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
    }

    fun createHighResourceConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
    }

    fun createNetworkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    }
}