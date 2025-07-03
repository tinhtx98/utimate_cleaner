package com.mars.ultimatecleaner.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.os.StatFs
import androidx.core.content.edit
import com.mars.ultimatecleaner.data.database.dao.CleaningHistoryDao
import com.mars.ultimatecleaner.data.database.dao.FileAnalysisDao
import com.mars.ultimatecleaner.data.database.entity.CleaningHistoryEntity
import com.mars.ultimatecleaner.data.scanner.JunkFileScanner
import com.mars.ultimatecleaner.data.scanner.LargeFileScanner
import com.mars.ultimatecleaner.data.scanner.EmptyFolderScanner
import com.mars.ultimatecleaner.data.scanner.ApkScanner
import com.mars.ultimatecleaner.data.scanner.CacheScanner
import com.mars.ultimatecleaner.data.scanner.TempFileScanner
import com.mars.ultimatecleaner.data.scanner.ResidualFileScanner
import com.mars.ultimatecleaner.data.utils.FileUtils
import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleaningRepositoryImpl @Inject constructor(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val cleaningHistoryDao: CleaningHistoryDao,
    private val fileAnalysisDao: FileAnalysisDao,
    private val junkFileScanner: JunkFileScanner,
    private val largeFileScanner: LargeFileScanner,
    private val emptyFolderScanner: EmptyFolderScanner,
    private val apkScanner: ApkScanner,
    private val cacheScanner: CacheScanner,
    private val tempFileScanner: TempFileScanner,
    private val residualFileScanner: ResidualFileScanner,
    private val fileUtils: FileUtils
) : CleaningRepository {

    companion object {
        private const val KEY_LARGE_FILE_THRESHOLD = "large_file_threshold_mb"
        private const val KEY_LAST_SCAN_TIME = "last_scan_time"
        private const val KEY_TOTAL_SPACE_CLEANED = "total_space_cleaned"
        private const val KEY_TOTAL_FILES_CLEANED = "total_files_cleaned"
        private const val KEY_JUNK_CATEGORIES = "junk_categories"
        private const val DEFAULT_LARGE_FILE_THRESHOLD = 100
    }

    private var cachedJunkCategories: List<JunkCategory> = emptyList()

    override fun scanJunkFiles(): Flow<ScanProgress> = flow {
        try {
            emit(ScanProgress(0, "Initializing scan...", 0))

            val totalSteps = 7
            var currentStep = 0
            var totalFilesFound = 0
            var totalSizeFound = 0L

            val junkCategories = mutableListOf<JunkCategory>()

            // Step 1: Scan cache files
            emit(ScanProgress(++currentStep * 100 / totalSteps, "Scanning cache files...", totalFilesFound))
            val cacheFiles = cacheScanner.scanCacheFiles()
            val cacheCategory = JunkCategory(
                id = "cache_files",
                name = "Cache Files",
                description = "Temporary files stored by apps",
                files = cacheFiles.map { it.toFileItem() },
                totalSize = cacheFiles.sumOf { it.size },
                canClean = true,
                priority = CleaningPriority.HIGH,
                icon = "cache"
            )
            junkCategories.add(cacheCategory)
            totalFilesFound += cacheFiles.size
            totalSizeFound += cacheCategory.totalSize

            // Step 2: Scan temporary files
            emit(ScanProgress(++currentStep * 100 / totalSteps, "Scanning temporary files...", totalFilesFound))
            val tempFiles = tempFileScanner.scanTempFiles()
            val tempCategory = JunkCategory(
                id = "temp_files",
                name = "Temporary Files",
                description = "Temporary files that can be safely deleted",
                files = tempFiles.map { it.toFileItem() },
                totalSize = tempFiles.sumOf { it.size },
                canClean = true,
                priority = CleaningPriority.HIGH,
                icon = "temp"
            )
            junkCategories.add(tempCategory)
            totalFilesFound += tempFiles.size
            totalSizeFound += tempCategory.totalSize

            // Step 3: Scan residual files
            emit(ScanProgress(++currentStep * 100 / totalSteps, "Scanning residual files...", totalFilesFound))
            val residualFiles = residualFileScanner.scanResidualFiles()
            val residualCategory = JunkCategory(
                id = "residual_files",
                name = "Residual Files",
                description = "Files left behind by uninstalled apps",
                files = residualFiles.map { it.toFileItem() },
                totalSize = residualFiles.sumOf { it.size },
                canClean = true,
                priority = CleaningPriority.MEDIUM,
                icon = "residual"
            )
            junkCategories.add(residualCategory)
            totalFilesFound += residualFiles.size
            totalSizeFound += residualCategory.totalSize

            // Step 4: Scan empty folders
            emit(ScanProgress(++currentStep * 100 / totalSteps, "Scanning empty folders...", totalFilesFound))
            val emptyFolders = emptyFolderScanner.scanEmptyFolders()
            val emptyFolderCategory = JunkCategory(
                id = "empty_folders",
                name = "Empty Folders",
                description = "Folders that contain no files",
                files = emptyFolders.map {
                    FileItem(
                        path = it,
                        name = File(it).name,
                        size = 0L,
                        isDirectory = true,
                        lastModified = File(it).lastModified(),
                        mimeType = "folder"
                    )
                },
                totalSize = 0L,
                canClean = true,
                priority = CleaningPriority.LOW,
                icon = "folder"
            )
            junkCategories.add(emptyFolderCategory)
            totalFilesFound += emptyFolders.size

            // Step 5: Scan obsolete APK files
            emit(ScanProgress(++currentStep * 100 / totalSteps, "Scanning obsolete APK files...", totalFilesFound))
            val obsoleteApks = apkScanner.scanObsoleteApks()
            val apkCategory = JunkCategory(
                id = "obsolete_apks",
                name = "Obsolete APK Files",
                description = "APK files of apps that are already installed",
                files = obsoleteApks.map { it.toFileItem() },
                totalSize = obsoleteApks.sumOf { it.size },
                canClean = true,
                priority = CleaningPriority.MEDIUM,
                icon = "apk"
            )
            junkCategories.add(apkCategory)
            totalFilesFound += obsoleteApks.size
            totalSizeFound += apkCategory.totalSize

            // Step 6: Scan large files
            emit(ScanProgress(++currentStep * 100 / totalSteps, "Scanning large files...", totalFilesFound))
            val thresholdMB = sharedPreferences.getInt(KEY_LARGE_FILE_THRESHOLD, DEFAULT_LARGE_FILE_THRESHOLD)
            val largeFiles = largeFileScanner.scanLargeFiles(thresholdMB * 1024 * 1024L)
            val largeFileCategory = JunkCategory(
                id = "large_files",
                name = "Large Files",
                description = "Files larger than ${thresholdMB}MB",
                files = largeFiles.map { it.toFileItem() },
                totalSize = largeFiles.sumOf { it.size },
                canClean = false, // User should decide manually
                priority = CleaningPriority.LOW,
                icon = "large_file"
            )
            junkCategories.add(largeFileCategory)
            totalFilesFound += largeFiles.size
            totalSizeFound += largeFileCategory.totalSize

            // Step 7: Finalize scan
            emit(ScanProgress(++currentStep * 100 / totalSteps, "Finalizing scan...", totalFilesFound))

            // Cache results
            cachedJunkCategories = junkCategories

            // Save scan results
            saveScanResults(junkCategories)

            // Update last scan time
            sharedPreferences.edit {
                putLong(KEY_LAST_SCAN_TIME, System.currentTimeMillis())
            }

            emit(ScanProgress(100, "Scan completed!", totalFilesFound, junkCategories))

        } catch (e: Exception) {
            emit(ScanProgress(-1, "Scan failed: ${e.message}", 0))
        }
    }.flowOn(Dispatchers.IO)

    override fun cleanFiles(categories: List<String>): Flow<CleaningProgress> = flow {
        try {
            emit(CleaningProgress(0, "Starting cleanup...", 0, 0L))

            val junkCategories = getJunkCategories().filter { it.id in categories && it.canClean }
            val totalFiles = junkCategories.sumOf { it.files.size }
            val totalSize = junkCategories.sumOf { it.totalSize }

            if (totalFiles == 0) {
                emit(CleaningProgress(100, "No files to clean", 0, 0L))
                return@flow
            }

            var processedFiles = 0
            var cleanedSize = 0L
            val cleanedFiles = mutableListOf<String>()
            val errors = mutableListOf<String>()

            for (category in junkCategories) {
                emit(CleaningProgress(
                    processedFiles * 100 / totalFiles,
                    "Cleaning ${category.name}...",
                    processedFiles,
                    cleanedSize
                ))

                for (file in category.files) {
                    try {
                        val fileObj = File(file.path)
                        if (fileObj.exists()) {
                            val fileSize = if (fileObj.isDirectory) {
                                calculateDirectorySize(fileObj)
                            } else {
                                fileObj.length()
                            }

                            val deleted = if (fileObj.isDirectory) {
                                deleteDirectory(fileObj)
                            } else {
                                fileObj.delete()
                            }

                            if (deleted) {
                                cleanedFiles.add(file.path)
                                cleanedSize += fileSize
                            } else {
                                errors.add("Failed to delete: ${file.path}")
                            }
                        }

                        processedFiles++

                        // Update progress every 10 files
                        if (processedFiles % 10 == 0) {
                            emit(CleaningProgress(
                                processedFiles * 100 / totalFiles,
                                "Cleaning ${category.name}... ($processedFiles/$totalFiles)",
                                processedFiles,
                                cleanedSize
                            ))
                        }

                    } catch (e: Exception) {
                        errors.add("Error cleaning ${file.path}: ${e.message}")
                        processedFiles++
                    }
                }
            }

            // Save cleaning result
            val cleaningResult = CleaningResult(
                id = System.currentTimeMillis().toString(),
                timestamp = System.currentTimeMillis(),
                categoriesCleaned = categories,
                totalFilesProcessed = processedFiles,
                totalFilesDeleted = cleanedFiles.size,
                totalSpaceSaved = cleanedSize,
                duration = 0L, // Will be calculated
                success = errors.isEmpty(),
                errors = errors
            )

            saveCleaningResult(cleaningResult)

            // Update total statistics
            updateTotalStatistics(cleanedFiles.size, cleanedSize)

            emit(CleaningProgress(
                100,
                "Cleanup completed! Cleaned ${cleanedFiles.size} files, saved ${formatFileSize(cleanedSize)}",
                processedFiles,
                cleanedSize,
                cleaningResult
            ))

        } catch (e: Exception) {
            emit(CleaningProgress(-1, "Cleanup failed: ${e.message}", 0, 0L))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getJunkCategories(): List<JunkCategory> {
        return withContext(Dispatchers.IO) {
            if (cachedJunkCategories.isNotEmpty()) {
                cachedJunkCategories
            } else {
                // Load from database or return empty list
                loadSavedJunkCategories()
            }
        }
    }

    override suspend fun getLargeFiles(thresholdMB: Int): List<FileItem> {
        return withContext(Dispatchers.IO) {
            try {
                val threshold = thresholdMB * 1024 * 1024L
                val largeFiles = largeFileScanner.scanLargeFiles(threshold)
                largeFiles.map { it.toFileItem() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getEmptyFolders(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                emptyFolderScanner.scanEmptyFolders()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getObsoleteApkFiles(): List<FileItem> {
        return withContext(Dispatchers.IO) {
            try {
                val obsoleteApks = apkScanner.scanObsoleteApks()
                obsoleteApks.map { it.toFileItem() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getCacheFiles(): List<FileItem> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFiles = cacheScanner.scanCacheFiles()
                cacheFiles.map { it.toFileItem() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getTempFiles(): List<FileItem> {
        return withContext(Dispatchers.IO) {
            try {
                val tempFiles = tempFileScanner.scanTempFiles()
                tempFiles.map { it.toFileItem() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getResidualFiles(): List<FileItem> {
        return withContext(Dispatchers.IO) {
            try {
                val residualFiles = residualFileScanner.scanResidualFiles()
                residualFiles.map { it.toFileItem() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun setLargeFileThreshold(thresholdMB: Int) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putInt(KEY_LARGE_FILE_THRESHOLD, thresholdMB)
            }
        }
    }

    override suspend fun getCleaningHistory(): List<CleaningHistoryItem> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = cleaningHistoryDao.getAllCleaningHistory()
                entities.map { entity ->
                    CleaningHistoryItem(
                        id = entity.id,
                        timestamp = entity.timestamp,
                        operationType = entity.operationType,
                        filesProcessed = entity.filesProcessed,
                        spaceSaved = entity.spaceSaved,
                        duration = entity.duration,
                        status = entity.status,
                        details = entity.details
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun saveCleaningResult(result: CleaningResult) {
        withContext(Dispatchers.IO) {
            try {
                val entity = CleaningHistoryEntity(
                    id = result.id,
                    timestamp = result.timestamp,
                    operationType = "CLEANING",
                    filesProcessed = result.totalFilesProcessed,
                    spaceSaved = result.totalSpaceSaved,
                    duration = result.duration,
                    status = if (result.success) "SUCCESS" else "FAILED",
                    details = "Categories: ${result.categoriesCleaned.joinToString(", ")}"
                )
                cleaningHistoryDao.insertCleaningHistory(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Private helper methods
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }

    private fun deleteDirectory(directory: File): Boolean {
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
        }
        return directory.delete()
    }

    private fun formatFileSize(sizeInBytes: Long): String {
        return fileUtils.formatFileSize(sizeInBytes)
    }

    private suspend fun saveScanResults(categories: List<JunkCategory>) {
        try {
            // Save to database for future reference
            for (category in categories) {
                for (file in category.files) {
                    val analysisEntity = com.mars.ultimatecleaner.data.database.entity.analysis.FileAnalysisEntity(
                        filePath = file.path,
                        fileName = file.name,
                        fileSize = file.size,
                        fileCategory = category.id,
                        isJunkFile = category.canClean,
                        isCacheFile = category.id == "cache_files",
                        isTempFile = category.id == "temp_files",
                        isDuplicate = false,
                        analysisTimestamp = System.currentTimeMillis()
                    )
                    fileAnalysisDao.insertFileAnalysis(analysisEntity)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadSavedJunkCategories(): List<JunkCategory> {
        return try {
            val cacheFiles = fileAnalysisDao.getCacheFiles()
            val tempFiles = fileAnalysisDao.getTempFiles()
            val junkFiles = fileAnalysisDao.getJunkFiles()

            val categories = mutableListOf<JunkCategory>()

            if (cacheFiles.isNotEmpty()) {
                categories.add(JunkCategory(
                    id = "cache_files",
                    name = "Cache Files",
                    description = "Temporary files stored by apps",
                    files = cacheFiles.map { it.toFileItem() },
                    totalSize = cacheFiles.sumOf { it.fileSize },
                    canClean = true,
                    priority = CleaningPriority.HIGH,
                    icon = "cache"
                ))
            }

            if (tempFiles.isNotEmpty()) {
                categories.add(JunkCategory(
                    id = "temp_files",
                    name = "Temporary Files",
                    description = "Temporary files that can be safely deleted",
                    files = tempFiles.map { it.toFileItem() },
                    totalSize = tempFiles.sumOf { it.fileSize },
                    canClean = true,
                    priority = CleaningPriority.HIGH,
                    icon = "temp"
                ))
            }

            categories
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun updateTotalStatistics(filesCount: Int, sizeBytes: Long) {
        val totalFiles = sharedPreferences.getInt(KEY_TOTAL_FILES_CLEANED, 0)
        val totalSize = sharedPreferences.getLong(KEY_TOTAL_SPACE_CLEANED, 0L)

        sharedPreferences.edit {
            putInt(KEY_TOTAL_FILES_CLEANED, totalFiles + filesCount)
            putLong(KEY_TOTAL_SPACE_CLEANED, totalSize + sizeBytes)
        }
    }
}

// Extension function to convert scanner results to FileItem
private fun Any.toFileItem(): FileItem {
    return when (this) {
        is File -> FileItem(
            path = absolutePath,
            name = name,
            size = length(),
            isDirectory = isDirectory,
            lastModified = lastModified(),
            mimeType = if (isDirectory) "folder" else fileUtils.getMimeType(extension)
        )
        else -> throw IllegalArgumentException("Unknown type for conversion to FileItem")
    }
}

// Extension function to convert database entity to FileItem
private fun com.mars.ultimatecleaner.data.database.entity.analysis.FileAnalysisEntity.toFileItem(): FileItem {
    return FileItem(
        path = filePath,
        name = fileName,
        size = fileSize,
        isDirectory = File(filePath).isDirectory,
        lastModified = analysisTimestamp,
        mimeType = fileUtils.getMimeType(File(filePath).extension)
    )
}