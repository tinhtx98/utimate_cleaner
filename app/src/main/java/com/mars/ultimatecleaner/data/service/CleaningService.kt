package com.mars.ultimatecleaner.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mars.ultimatecleaner.R
import com.mars.ultimatecleaner.data.algorithm.FileScanner
import com.mars.ultimatecleaner.data.utils.FileUtils
import com.mars.ultimatecleaner.data.utils.SecurityUtils
import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class CleaningService : Service() {

    @Inject
    lateinit var fileScanner: FileScanner

    @Inject
    lateinit var fileUtils: FileUtils

    @Inject
    lateinit var securityUtils: SecurityUtils

    private val binder = CleaningBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeOperations = ConcurrentHashMap<String, Job>()

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "cleaning_service_channel"
        private const val CHANNEL_NAME = "Cleaning Service"
    }

    inner class CleaningBinder : Binder() {
        fun getService(): CleaningService = this@CleaningService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    fun startCleaningOperation(
        operationId: String,
        categories: List<String>,
        options: CleaningOptions = CleaningOptions()
    ): Flow<CleaningProgressDomain> = flow {
        val notification = createProgressNotification("Starting cleanup...")
        startForeground(NOTIFICATION_ID, notification)

        var totalFiles = 0
        var processedFiles = 0
        var deletedFiles = 0
        var spaceSaved = 0L
        var failedFiles = 0
        val startTime = System.currentTimeMillis()

        emit(CleaningProgressDomain(0f, "Initializing cleanup...", 0, 0, 0L))

        try {
            // Phase 1: Scan for files to clean
            emit(CleaningProgressDomain(5f, "Scanning for files...", 0, 0, 0L))

            val filesToClean = mutableListOf<JunkFileDomain>()

            for (category in categories) {
                val categoryFiles = when (category) {
                    "cache" -> getCacheFiles()
                    "temp" -> getTempFiles()
                    "apk" -> getObsoleteApkFiles()
                    "empty_folders" -> getEmptyFolders()
                    "large_files" -> getLargeFiles(options.largeFileThresholdMB)
                    "residual" -> getResidualFiles()
                    else -> emptyList()
                }
                filesToClean.addAll(categoryFiles)
            }

            totalFiles = filesToClean.size
            emit(CleaningProgressDomain(10f, "Found $totalFiles files to clean", 0, totalFiles, 0L))

            // Phase 2: Clean files
            for ((index, fileToClean) in filesToClean.withIndex()) {
                if (!isActive) break // Check if operation was cancelled

                try {
                    val file = File(fileToClean.path)

                    if (securityUtils.isSafeToDelete(file) && file.exists()) {
                        val fileSize = file.length()

                        if (options.createBackup) {
                            createBackup(file)
                        }

                        if (file.delete()) {
                            deletedFiles++
                            spaceSaved += fileSize
                        } else {
                            failedFiles++
                        }
                    } else {
                        failedFiles++
                    }

                } catch (e: Exception) {
                    failedFiles++
                }

                processedFiles++
                val progress = 10f + ((processedFiles.toFloat() / totalFiles) * 85f)
                val currentFile = fileToClean.name

                emit(CleaningProgressDomain(
                    progress,
                    "Cleaning: $currentFile",
                    processedFiles,
                    totalFiles,
                    spaceSaved,
                    currentFile
                ))

                // Update notification periodically
                if (processedFiles % 10 == 0) {
                    val notification = createProgressNotification(
                        "Cleaned $deletedFiles files • ${fileUtils.formatFileSize(spaceSaved)} freed"
                    )
                    startForeground(NOTIFICATION_ID, notification)
                }

                yield() // Allow other coroutines to run
            }

            // Phase 3: Final cleanup and reporting
            emit(CleaningProgressDomain(95f, "Finalizing cleanup...", processedFiles, totalFiles, spaceSaved))

            // Clean up empty parent directories
            if (options.removeEmptyDirectories) {
                cleanupEmptyDirectories(filesToClean)
            }

            val duration = System.currentTimeMillis() - startTime
            val notification = createCompletionNotification(deletedFiles, spaceSaved, failedFiles)
            startForeground(NOTIFICATION_ID, notification)

            emit(CleaningProgressDomain(
                100f,
                "Cleanup completed",
                processedFiles,
                totalFiles,
                spaceSaved,
                isComplete = true
            ))

        } catch (e: Exception) {
            emit(CleaningProgressDomain(
                0f,
                "Cleanup failed: ${e.message}",
                processedFiles,
                totalFiles,
                spaceSaved,
                isComplete = true
            ))
        } finally {
            stopForeground(true)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun getCacheFiles(): List<JunkFileDomain> {
        return withContext(Dispatchers.IO) {
            val cacheFiles = mutableListOf<JunkFileDomain>()

            // App cache directories
            val cacheDirectories = listOf(
                applicationContext.cacheDir,
                applicationContext.externalCacheDir
            ).filterNotNull()

            // System cache directories
            val systemCacheDirs = fileUtils.findCacheDirectories()

            (cacheDirectories + systemCacheDirs).forEach { cacheDir ->
                if (cacheDir.exists() && cacheDir.canRead()) {
                    cacheDir.walkTopDown()
                        .filter { it.isFile && securityUtils.isSafeToDelete(it) }
                        .forEach { file ->
                            cacheFiles.add(
                                JunkFileDomain(
                                    path = file.absolutePath,
                                    name = file.name,
                                    size = file.length(),
                                    lastModified = file.lastModified(),
                                    canDelete = true,
                                    deleteReason = "Cache file"
                                )
                            )
                        }
                }
            }

            cacheFiles
        }
    }

    private suspend fun getTempFiles(): List<JunkFileDomain> {
        return withContext(Dispatchers.IO) {
            fileUtils.findTempFiles()
        }
    }

    private suspend fun getObsoleteApkFiles(): List<JunkFileDomain> {
        return withContext(Dispatchers.IO) {
            fileUtils.findObsoleteApkFiles(applicationContext)
        }
    }

    private suspend fun getEmptyFolders(): List<JunkFileDomain> {
        return withContext(Dispatchers.IO) {
            fileUtils.findEmptyFolders()
        }
    }

    private suspend fun getLargeFiles(thresholdMB: Long): List<JunkFileDomain> {
        return withContext(Dispatchers.IO) {
            fileUtils.findLargeFiles(thresholdMB * 1024 * 1024)
        }
    }

    private suspend fun getResidualFiles(): List<JunkFileDomain> {
        return withContext(Dispatchers.IO) {
            fileUtils.findResidualFiles(applicationContext)
        }
    }

    private fun createBackup(file: File) {
        try {
            val backupDir = File(applicationContext.cacheDir, "backup")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val backupFile = File(backupDir, "${System.currentTimeMillis()}_${file.name}")
            file.copyTo(backupFile)
        } catch (e: Exception) {
            // Backup failed, but continue with deletion
        }
    }

    private suspend fun cleanupEmptyDirectories(cleanedFiles: List<JunkFileDomain>) {
        val parentDirectories = cleanedFiles
            .map { File(it.path).parentFile }
            .filterNotNull()
            .distinct()

        for (dir in parentDirectories) {
            try {
                if (dir.exists() && dir.isDirectory &&
                    dir.listFiles()?.isEmpty() == true &&
                    securityUtils.isSafeToDelete(dir)) {
                    dir.delete()
                }
            } catch (e: Exception) {
                // Continue with other directories
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows cleaning progress"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createProgressNotification(content: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cleaning)
            .setContentTitle("Cleaning in progress")
            .setContentText(content)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

    private fun createCompletionNotification(deletedFiles: Int, spaceSaved: Long, failedFiles: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cleaning_complete)
            .setContentTitle("Cleaning completed")
            .setContentText("Deleted $deletedFiles files • ${fileUtils.formatFileSize(spaceSaved)} freed")
            .setAutoCancel(true)
            .build()

    fun cancelOperation(operationId: String) {
        activeOperations[operationId]?.cancel()
        activeOperations.remove(operationId)
    }
}

data class CleaningOptions(
    val createBackup: Boolean = false,
    val removeEmptyDirectories: Boolean = true,
    val largeFileThresholdMB: Long = 100L,
    val skipSystemFiles: Boolean = true,
    val skipRecentFiles: Boolean = true,
    val recentFilesDays: Int = 7
)