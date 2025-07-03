package com.mars.ultimatecleaner.data.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.webkit.MimeTypeMap
import com.mars.ultimatecleaner.domain.model.JunkFile
import com.mars.ultimatecleaner.domain.model.FileOperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.pow

@Singleton
class FileUtils @Inject constructor(
    private val securityUtils: SecurityUtils
) {

    companion object {
        private val TEMP_EXTENSIONS = listOf(
            ".tmp", ".temp", ".bak", ".backup", ".old", ".log",
            ".crash", ".dmp", ".trace", ".pid", ".lock", ".swp", ".part"
        )

        private val CACHE_DIR_NAMES = listOf(
            "cache", ".cache", "tmp", ".tmp", "temp", ".temp",
            ".thumbnails", "thumbs", ".thumbs", ".nomedia"
        )

        private val JUNK_FILE_PATTERNS = listOf(
            "~*", "*.tmp", "*.temp", "*.bak", "*.old", "*.log",
            "*.pid", "*.lock", "*.swp", "core.*", "*.crash", "Thumbs.db", ".DS_Store"
        )

        private val SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

        private val RESIDUAL_INDICATORS = listOf(
            "uninstall", "backup", "cache", "temp", "log"
        )
    }

    private val sizeCache = ConcurrentHashMap<String, Pair<Long, Long>>() // path -> (size, timestamp)
    private val mimeTypeMap = MimeTypeMap.getSingleton()

    // Complete the findResidualFiles method
    fun findResidualFiles(context: Context): List<JunkFile> {
        val residualFiles = mutableListOf<JunkFile>()
        val packageManager = context.packageManager
        val installedPackages = getInstalledPackageNames(packageManager)

        try {
            // Check Android/data and Android/obb directories
            val androidDataDir = File(Environment.getExternalStorageDirectory(), "Android/data")
            val androidObbDir = File(Environment.getExternalStorageDirectory(), "Android/obb")

            listOf(androidDataDir, androidObbDir).forEach { directory ->
                if (directory.exists() && directory.canRead()) {
                    directory.listFiles()?.forEach { packageDir ->
                        if (packageDir.isDirectory &&
                            !installedPackages.contains(packageDir.name) &&
                            securityUtils.isSafeToDelete(packageDir)) {

                            // Add all files in uninstalled app directories
                            packageDir.walkTopDown()
                                .filter { it.isFile }
                                .forEach { file ->
                                    residualFiles.add(
                                        createJunkFile(file, "Residual file from uninstalled app: ${packageDir.name}")
                                    )
                                }
                        }
                    }
                }
            }

            // Check for residual files in common directories
            val commonResidualDirs = listOf(
                File(Environment.getExternalStorageDirectory(), "Download"),
                File(Environment.getExternalStorageDirectory(), "Documents"),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ).filter { it.exists() && it.canRead() }

            commonResidualDirs.forEach { directory ->
                findResidualFilesInDirectory(directory, installedPackages, residualFiles)
            }

        } catch (e: Exception) {
            // Handle errors gracefully
        }

        return residualFiles
    }

    private fun findResidualFilesInDirectory(
        directory: File,
        installedPackages: Set<String>,
        residualFiles: MutableList<JunkFile>
    ) {
        try {
            directory.walkTopDown()
                .maxDepth(3)
                .filter { it.isFile }
                .filter { file ->
                    isLikelyResidualFile(file, installedPackages) &&
                            securityUtils.isSafeToDelete(file)
                }
                .forEach { file ->
                    residualFiles.add(
                        createJunkFile(file, "Likely residual file")
                    )
                }
        } catch (e: Exception) {
            // Continue with other directories
        }
    }

    private fun isLikelyResidualFile(file: File, installedPackages: Set<String>): Boolean {
        val fileName = file.name.lowercase(Locale.getDefault())
        val filePath = file.absolutePath.lowercase(Locale.getDefault())

        // Check if filename contains package names of uninstalled apps
        for (packageName in getAllKnownPackageNames()) {
            if (!installedPackages.contains(packageName) &&
                (fileName.contains(packageName) || filePath.contains(packageName))) {
                return true
            }
        }

        // Check for common residual file patterns
        return RESIDUAL_INDICATORS.any { indicator ->
            fileName.contains(indicator) && isOldFile(file, 7) // Older than 7 days
        }
    }

    private fun getAllKnownPackageNames(): List<String> {
        // This could be expanded with a database of known package names
        return listOf(
            "com.whatsapp", "com.facebook", "com.instagram", "com.twitter",
            "com.google.android.gms", "com.android.chrome", "com.spotify.music"
        )
    }

    private fun getInstalledPackageNames(packageManager: PackageManager): Set<String> {
        return try {
            packageManager.getInstalledPackages(0).map { it.packageName }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // File safety and validation methods
    fun isSafeToDelete(file: File): Boolean {
        return securityUtils.isSafeToDelete(file) &&
                !isSystemCriticalFile(file) &&
                !isUserImportantFile(file)
    }

    private fun isSystemCriticalFile(file: File): Boolean {
        val path = file.absolutePath.lowercase(Locale.getDefault())
        val criticalPaths = listOf(
            "/system/", "/proc/", "/dev/", "/sys/", "/root/",
            "/data/system/", "/data/misc/", "/vendor/", "/boot/",
            "/recovery/", "/cache/recovery/"
        )

        return criticalPaths.any { path.startsWith(it) }
    }

    private fun isUserImportantFile(file: File): Boolean {
        val path = file.absolutePath.lowercase(Locale.getDefault())
        val importantPaths = listOf(
            "/dcim/camera/", "/pictures/", "/documents/", "/music/"
        )

        val importantExtensions = listOf(
            ".jpg", ".jpeg", ".png", ".mp4", ".pdf", ".doc", ".docx"
        )

        val hasImportantPath = importantPaths.any { path.contains(it) }
        val hasImportantExtension = importantExtensions.any {
            file.name.lowercase(Locale.getDefault()).endsWith(it)
        }

        return hasImportantPath && hasImportantExtension && !isOldFile(file, 30)
    }

    // File operation methods
    suspend fun copyFile(source: File, destination: File): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) {
                return@withContext FileOperationResult(
                    false, 0, 1, "Source file does not exist", listOf(source.absolutePath)
                )
            }

            if (!securityUtils.isSafeToRead(source)) {
                return@withContext FileOperationResult(
                    false, 0, 1, "No permission to read source file", listOf(source.absolutePath)
                )
            }

            destination.parentFile?.mkdirs()

            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }

            // Verify copy integrity
            if (source.length() == destination.length()) {
                FileOperationResult(true, 1, 0)
            } else {
                destination.delete()
                FileOperationResult(
                    false, 0, 1, "Copy verification failed", listOf(source.absolutePath)
                )
            }

        } catch (e: Exception) {
            FileOperationResult(
                false, 0, 1, e.message ?: "Copy failed", listOf(source.absolutePath)
            )
        }
    }

    suspend fun moveFile(source: File, destination: File): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) {
                return@withContext FileOperationResult(
                    false, 0, 1, "Source file does not exist", listOf(source.absolutePath)
                )
            }

            destination.parentFile?.mkdirs()

            // Try direct rename first (fastest)
            if (source.renameTo(destination)) {
                return@withContext FileOperationResult(true, 1, 0)
            }

            // Fall back to copy and delete
            val copyResult = copyFile(source, destination)
            if (copyResult.isSuccess) {
                if (source.delete()) {
                    FileOperationResult(true, 1, 0)
                } else {
                    destination.delete() // Clean up the copy
                    FileOperationResult(
                        false, 0, 1, "Failed to delete source after copy", listOf(source.absolutePath)
                    )
                }
            } else {
                copyResult
            }

        } catch (e: Exception) {
            FileOperationResult(
                false, 0, 1, e.message ?: "Move failed", listOf(source.absolutePath)
            )
        }
    }

    suspend fun deleteFilesSafely(files: List<File>): FileOperationResult = withContext(Dispatchers.IO) {
        var successCount = 0
        var failedCount = 0
        val failedFiles = mutableListOf<String>()

        for (file in files) {
            try {
                if (securityUtils.isSafeToDelete(file) && file.exists()) {
                    if (file.delete()) {
                        successCount++
                    } else {
                        failedCount++
                        failedFiles.add(file.absolutePath)
                    }
                } else {
                    failedCount++
                    failedFiles.add(file.absolutePath)
                }
            } catch (e: Exception) {
                failedCount++
                failedFiles.add(file.absolutePath)
            }
        }

        FileOperationResult(
            isSuccess = failedCount == 0,
            successCount = successCount,
            failedCount = failedCount,
            errorMessage = if (failedCount > 0) "Failed to delete $failedCount files" else null,
            failedFiles = failedFiles
        )
    }

    // Directory size calculation with caching
    fun calculateDirectorySize(directory: File, useCache: Boolean = true): Long {
        val path = directory.absolutePath
        val currentTime = System.currentTimeMillis()

        if (useCache) {
            val cached = sizeCache[path]
            if (cached != null && (currentTime - cached.second) < 300000) { // 5 minutes cache
                return cached.first
            }
        }

        val size = calculateDirectorySizeRecursive(directory)
        sizeCache[path] = Pair(size, currentTime)
        return size
    }

    private fun calculateDirectorySizeRecursive(directory: File): Long {
        var size = 0L

        try {
            if (directory.exists() && directory.canRead()) {
                directory.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        try {
                            size += file.length()
                        } catch (e: Exception) {
                            // Continue with other files
                        }
                    }
            }
        } catch (e: Exception) {
            // Return partial result
        }

        return size
    }

    // File type detection and utilities
    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase(Locale.getDefault())
        return mimeTypeMap.getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    fun getFileCategory(file: File): String {
        val mimeType = getMimeType(file)
        return when {
            mimeType.startsWith("image/") -> "Images"
            mimeType.startsWith("video/") -> "Videos"
            mimeType.startsWith("audio/") -> "Audio"
            mimeType.startsWith("text/") || mimeType.contains("document") -> "Documents"
            mimeType.contains("application") -> "Apps"
            else -> "Other"
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val adjustedSize = bytes / 1024.0.pow(digitGroups.toDouble())

        return DecimalFormat("#,##0.#").format(adjustedSize) + " " + SIZE_UNITS[digitGroups]
    }

    // Helper methods
    private fun createJunkFile(file: File, reason: String): JunkFile {
        return JunkFile(
            path = file.absolutePath,
            name = file.name,
            size = file.length(),
            lastModified = file.lastModified(),
            canDelete = isSafeToDelete(file),
            deleteReason = reason
        )
    }

    private fun isTempFile(file: File): Boolean {
        val name = file.name.lowercase(Locale.getDefault())
        val extension = "." + file.extension.lowercase(Locale.getDefault())

        return TEMP_EXTENSIONS.contains(extension) ||
                name.startsWith("tmp") ||
                name.startsWith("temp") ||
                name.contains(".tmp.") ||
                matchesJunkPattern(name)
    }

    private fun matchesJunkPattern(fileName: String): Boolean {
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

    private fun isOldFile(file: File, daysThreshold: Int): Boolean {
        val ageInMs = System.currentTimeMillis() - file.lastModified()
        val daysOld = ageInMs / (24 * 60 * 60 * 1000)
        return daysOld >= daysThreshold
    }

    private fun isEmptyDirectory(directory: File): Boolean {
        return try {
            val files = directory.listFiles()
            files?.isEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    private fun isSystemOrImportantDirectory(directory: File): Boolean {
        val path = directory.absolutePath.lowercase(Locale.getDefault())
        val protectedPaths = listOf(
            "/system", "/android_secure", "/dcim", "/pictures",
            "/documents", "/music", "/movies", "/download"
        )

        return protectedPaths.any { path.contains(it) }
    }

    fun clearSizeCache() {
        sizeCache.clear()
    }
}