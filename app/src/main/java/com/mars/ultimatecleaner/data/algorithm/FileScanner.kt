package com.mars.ultimatecleaner.data.algorithm

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.data.utils.FileUtils
import com.mars.ultimatecleaner.data.utils.HashUtils
import com.mars.ultimatecleaner.data.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileScanner @Inject constructor(
    private val context: Context,
    private val fileUtils: FileUtils,
    private val hashUtils: HashUtils,
    private val securityUtils: SecurityUtils
) {

    companion object {
        private const val MIN_SCAN_INTERVAL = 1000L // 1 second between yield calls
        private val CACHE_DIRECTORIES = listOf(
            "cache", ".cache", "tmp", ".tmp", "temp", ".temp",
            ".thumbnails", "thumbs", ".thumbs"
        )
        private val TEMP_FILE_EXTENSIONS = listOf(
            ".tmp", ".temp", ".bak", ".backup", ".old", ".log",
            ".crash", ".dmp", ".trace", ".pid"
        )
        private val APK_EXTENSIONS = listOf(".apk", ".xapk", ".apks")
        private val JUNK_FILE_PATTERNS = listOf(
            "~*", "*.tmp", "*.temp", "*.bak", "*.old", "*.log",
            "*.pid", "*.lock", "*.swp", "core.*", "*.crash"
        )
    }

    fun scanForJunkFiles(): Flow<ScanProgress> = flow {
        var totalFiles = 0
        var scannedFiles = 0
        var lastYieldTime = System.currentTimeMillis()

        val junkCategories = mutableMapOf<String, MutableList<JunkFile>>()

        // Initialize categories
        junkCategories["cache"] = mutableListOf()
        junkCategories["temp"] = mutableListOf()
        junkCategories["residual"] = mutableListOf()
        junkCategories["apk"] = mutableListOf()
        junkCategories["empty_folders"] = mutableListOf()
        junkCategories["large_files"] = mutableListOf()

        emit(ScanProgress(0f, "Initializing scan...", 0, 0))

        try {
            // Get scannable directories
            val scannableDirectories = getScannableDirectories()

            // Estimate total files for progress tracking
            totalFiles = estimateTotalFiles(scannableDirectories)

            emit(ScanProgress(5f, "Scanning directories...", 0, totalFiles))

            for (directory in scannableDirectories) {
                if (securityUtils.isDirectorySafeToScan(directory)) {
                    scanDirectory(
                        directory = directory,
                        junkCategories = junkCategories,
                        onProgress = { currentFile ->
                            scannedFiles++
                            val currentTime = System.currentTimeMillis()

                            if (currentTime - lastYieldTime > MIN_SCAN_INTERVAL) {
                                val progress = (scannedFiles.toFloat() / totalFiles * 85) + 5
                                emit(ScanProgress(
                                    progress.coerceAtMost(90f),
                                    "Scanning: ${currentFile.name}",
                                    scannedFiles,
                                    totalFiles
                                ))
                                lastYieldTime = currentTime
                            }
                        }
                    )
                }
                yield() // Allow other coroutines to run
            }

            emit(ScanProgress(95f, "Finalizing scan results...", scannedFiles, totalFiles))

            // Final processing and categorization
            processJunkCategories(junkCategories)

            emit(ScanProgress(100f, "Scan completed", scannedFiles, totalFiles, isComplete = true))

        } catch (e: Exception) {
            emit(ScanProgress(0f, "Scan failed: ${e.message}", scannedFiles, totalFiles, error = e.message))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun scanDirectory(
        directory: File,
        junkCategories: MutableMap<String, MutableList<JunkFile>>,
        onProgress: (File) -> Unit
    ) {
        if (!directory.exists() || !directory.canRead()) return

        try {
            val files = directory.listFiles() ?: return

            for (file in files) {
                onProgress(file)

                when {
                    file.isDirectory -> {
                        handleDirectory(file, junkCategories)
                        if (file.canRead() && !securityUtils.isSystemDirectory(file)) {
                            scanDirectory(file, junkCategories, onProgress)
                        }
                    }
                    file.isFile -> handleFile(file, junkCategories)
                }

                yield() // Allow cancellation
            }
        } catch (e: SecurityException) {
            // Log but continue scanning other directories
        }
    }

    private fun handleDirectory(directory: File, junkCategories: MutableMap<String, MutableList<JunkFile>>) {
        // Check for cache directories
        if (isCacheDirectory(directory)) {
            val cacheFiles = getCacheFiles(directory)
            junkCategories["cache"]?.addAll(cacheFiles)
        }

        // Check for empty directories
        if (isEmptyDirectory(directory) && securityUtils.isSafeToDelete(directory)) {
            junkCategories["empty_folders"]?.add(
                JunkFile(
                    path = directory.absolutePath,
                    name = directory.name,
                    size = 0L,
                    lastModified = directory.lastModified(),
                    canDelete = true,
                    deleteReason = "Empty directory"
                )
            )
        }
    }

    private fun handleFile(file: File, junkCategories: MutableMap<String, MutableList<JunkFile>>) {
        when {
            isTempFile(file) -> {
                junkCategories["temp"]?.add(createJunkFile(file, "Temporary file"))
            }
            isApkFile(file) -> {
                if (isObsoleteApk(file)) {
                    junkCategories["apk"]?.add(createJunkFile(file, "Obsolete APK file"))
                }
            }
            isLargeFile(file) -> {
                junkCategories["large_files"]?.add(createJunkFile(file, "Large file"))
            }
            isResidualFile(file) -> {
                junkCategories["residual"]?.add(createJunkFile(file, "Residual file"))
            }
        }
    }

    private fun isCacheDirectory(directory: File): Boolean {
        val name = directory.name.lowercase(Locale.getDefault())
        return CACHE_DIRECTORIES.any { cacheDir ->
            name.contains(cacheDir) || name.endsWith(cacheDir)
        }
    }

    private fun getCacheFiles(cacheDirectory: File): List<JunkFile> {
        val cacheFiles = mutableListOf<JunkFile>()

        try {
            cacheDirectory.walkTopDown()
                .filter { it.isFile && securityUtils.isSafeToDelete(it) }
                .forEach { file ->
                    cacheFiles.add(createJunkFile(file, "Cache file"))
                }
        } catch (e: Exception) {
            // Continue if we can't read some cache files
        }

        return cacheFiles
    }

    private fun isEmptyDirectory(directory: File): Boolean {
        return try {
            val files = directory.listFiles()
            files?.isEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    private fun isTempFile(file: File): Boolean {
        val name = file.name.lowercase(Locale.getDefault())
        val extension = file.extension.lowercase(Locale.getDefault())

        return TEMP_FILE_EXTENSIONS.contains(".$extension") ||
                name.startsWith("tmp") ||
                name.startsWith("temp") ||
                name.contains(".tmp.") ||
                isJunkFilePattern(name)
    }

    private fun isJunkFilePattern(fileName: String): Boolean {
        return JUNK_FILE_PATTERNS.any { pattern ->
            when {
                pattern.startsWith("*") && pattern.endsWith("*") -> {
                    val content = pattern.substring(1, pattern.length - 1)
                    fileName.contains(content)
                }
                pattern.startsWith("*") -> {
                    fileName.endsWith(pattern.substring(1))
                }
                pattern.endsWith("*") -> {
                    fileName.startsWith(pattern.substring(0, pattern.length - 1))
                }
                else -> fileName == pattern
            }
        }
    }

    private fun isApkFile(file: File): Boolean {
        val extension = file.extension.lowercase(Locale.getDefault())
        return APK_EXTENSIONS.contains(".$extension")
    }

    private fun isObsoleteApk(file: File): Boolean {
        // Check if APK is already installed or if it's an old version
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(file.absolutePath, 0)

            if (packageInfo != null) {
                try {
                    val installedInfo = packageManager.getPackageInfo(packageInfo.packageName, 0)
                    // APK is obsolete if installed version is newer or same
                    installedInfo.versionCode >= packageInfo.versionCode
                } catch (e: Exception) {
                    // Package not installed, not obsolete
                    false
                }
            } else {
                // Invalid APK file
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isLargeFile(file: File): Boolean {
        val threshold = getLargeFileThreshold()
        return file.length() > threshold
    }

    private fun getLargeFileThreshold(): Long {
        // Default to 100MB, should be configurable
        return 100L * 1024 * 1024
    }

    private fun isResidualFile(file: File): Boolean {
        // Check if file belongs to uninstalled app
        val path = file.absolutePath
        val packageManager = context.packageManager

        // Common patterns for app residual files
        val residualPatterns = listOf(
            "/Android/data/",
            "/Android/obb/",
            "/.android_secure/"
        )

        return residualPatterns.any { pattern ->
            if (path.contains(pattern)) {
                val packageName = extractPackageNameFromPath(path, pattern)
                packageName != null && !isPackageInstalled(packageName, packageManager)
            } else {
                false
            }
        }
    }

    private fun extractPackageNameFromPath(path: String, pattern: String): String? {
        val startIndex = path.indexOf(pattern)
        if (startIndex == -1) return null

        val afterPattern = path.substring(startIndex + pattern.length)
        val endIndex = afterPattern.indexOf('/')

        return if (endIndex != -1) {
            afterPattern.substring(0, endIndex)
        } else {
            afterPattern
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: android.content.pm.PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun createJunkFile(file: File, reason: String): JunkFile {
        return JunkFile(
            path = file.absolutePath,
            name = file.name,
            size = file.length(),
            lastModified = file.lastModified(),
            canDelete = securityUtils.isSafeToDelete(file),
            deleteReason = reason
        )
    }

    private fun getScannableDirectories(): List<File> {
        val directories = mutableListOf<File>()

        // External storage
        val externalStorage = Environment.getExternalStorageDirectory()
        if (externalStorage.exists() && externalStorage.canRead()) {
            directories.add(externalStorage)
        }

        // App-specific directories
        context.getExternalFilesDirs(null)?.forEach { dir ->
            if (dir != null && dir.exists() && dir.canRead()) {
                directories.add(dir)
            }
        }

        // Cache directories
        context.externalCacheDirs?.forEach { dir ->
            if (dir != null && dir.exists() && dir.canRead()) {
                directories.add(dir)
            }
        }

        return directories
    }

    private fun estimateTotalFiles(directories: List<File>): Int {
        var totalFiles = 0

        directories.forEach { directory ->
            try {
                if (directory.exists() && directory.canRead()) {
                    totalFiles += directory.walkTopDown()
                        .take(10000) // Limit estimation to avoid long delays
                        .count { it.isFile }
                }
            } catch (e: Exception) {
                // Continue with other directories
            }
        }

        return maxOf(totalFiles, 1000) // Minimum estimate
    }

    private fun processJunkCategories(junkCategories: MutableMap<String, MutableList<JunkFile>>) {
        // Remove duplicates and sort by size
        junkCategories.values.forEach { files ->
            files.distinctBy { it.path }
            files.sortByDescending { it.size }
        }
    }

    fun getStorageInfo(): StorageInfo {
        val externalStorage = Environment.getExternalStorageDirectory()
        val statFs = StatFs(externalStorage.absolutePath)

        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val availableBlocks = statFs.availableBlocksLong

        val totalSpace = totalBlocks * blockSize
        val freeSpace = availableBlocks * blockSize
        val usedSpace = totalSpace - freeSpace

        val usagePercentage = if (totalSpace > 0) {
            (usedSpace.toFloat() / totalSpace.toFloat()) * 100f
        } else 0f

        return StorageInfo(
            totalSpace = totalSpace,
            usedSpace = usedSpace,
            freeSpace = freeSpace,
            usagePercentage = usagePercentage,
            categoryBreakdown = getCategoryBreakdown()
        )
    }

    private fun getCategoryBreakdown(): Map<String, Long> {
        val breakdown = mutableMapOf<String, Long>()

        // Use MediaStore to get file type statistics
        val projection = arrayOf(
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        try {
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val size = cursor.getLong(sizeColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: "unknown"

                    val category = categorizeByMimeType(mimeType)
                    breakdown[category] = breakdown.getOrDefault(category, 0L) + size
                }
            }
        } catch (e: Exception) {
            // Fallback to manual calculation if MediaStore fails
        }

        return breakdown
    }

    private fun categorizeByMimeType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "Images"
            mimeType.startsWith("video/") -> "Videos"
            mimeType.startsWith("audio/") -> "Audio"
            mimeType.startsWith("text/") || mimeType.contains("document") -> "Documents"
            mimeType.contains("application") -> "Apps"
            else -> "Other"
        }
    }
}