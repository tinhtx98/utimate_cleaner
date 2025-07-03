package com.mars.ultimatecleaner.data.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityUtils @Inject constructor(
    private val context: Context
) {

    companion object {
        private val PROTECTED_DIRECTORIES = listOf(
            "/system", "/proc", "/dev", "/sys", "/root", "/sbin",
            "/data/system", "/data/misc", "/vendor", "/boot",
            "/recovery", "/cache/recovery", "/android_secure"
        )

        private val PROTECTED_FILE_PATTERNS = listOf(
            "*.so", "*.dex", "*.odex", "*.art", "*.apk",
            "build.prop", "default.prop", "*.rc", "init*"
        )

        private val SAFE_DELETE_EXTENSIONS = listOf(
            ".tmp", ".temp", ".log", ".cache", ".bak", ".old",
            ".thumb", ".thumbnail", ".part", ".crdownload"
        )

        private val MALWARE_INDICATORS = listOf(
            "trojan", "virus", "malware", "backdoor", "rootkit",
            "keylogger", "spyware", "adware", "worm"
        )

        private val SYSTEM_PACKAGES = listOf(
            "android", "com.android", "com.google.android",
            "system", "framework", "telephony"
        )
    }

    fun isSafeToDelete(file: File): Boolean {
        return !isSystemFile(file) &&
                !isProtectedFile(file) &&
                !isCurrentlyInUse(file) &&
                !isUserCriticalFile(file) &&
                hasDeletePermission(file)
    }

    fun isSafeToRead(file: File): Boolean {
        return file.exists() &&
                file.canRead() &&
                !isSystemProtected(file) &&
                !isSuspiciousFile(file)
    }

    fun isSafeToScan(directory: File): Boolean {
        return directory.exists() &&
                directory.isDirectory &&
                directory.canRead() &&
                !isSystemDirectory(directory) &&
                !isExternallyMounted(directory)
    }

    fun isDirectorySafeToScan(directory: File): Boolean {
        val path = directory.absolutePath.lowercase(Locale.getDefault())

        // Check if it's a protected system directory
        if (PROTECTED_DIRECTORIES.any { path.startsWith(it) }) {
            return false
        }

        // Check if we have read permission
        if (!directory.canRead()) {
            return false
        }

        // Check if it's an external storage that might be unsafe
        if (isExternallyMounted(directory)) {
            return false
        }

        return true
    }

    fun isSystemDirectory(directory: File): Boolean {
        val path = directory.absolutePath.lowercase(Locale.getDefault())
        return PROTECTED_DIRECTORIES.any { path.startsWith(it) }
    }

    private fun isSystemFile(file: File): Boolean {
        val path = file.absolutePath.lowercase(Locale.getDefault())

        // Check if file is in system directories
        if (PROTECTED_DIRECTORIES.any { path.startsWith(it) }) {
            return true
        }

        // Check if file matches system file patterns
        val fileName = file.name.lowercase(Locale.getDefault())
        return PROTECTED_FILE_PATTERNS.any { pattern ->
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

    private fun isProtectedFile(file: File): Boolean {
        val path = file.absolutePath.lowercase(Locale.getDefault())

        // Protect important user directories
        val protectedUserPaths = listOf(
            "/dcim/camera/", "/pictures/screenshots/", "/pictures/camera/",
            "/documents/", "/download/", "/music/", "/movies/"
        )

        // Check if file is in protected user directories and is recent
        val isInProtectedPath = protectedUserPaths.any { path.contains(it) }
        if (isInProtectedPath && isRecentFile(file, 1)) { // Less than 1 day old
            return true
        }

        // Protect files that are currently being written to
        if (file.name.endsWith(".part") || file.name.endsWith(".crdownload")) {
            return true
        }

        return false
    }

    private fun isCurrentlyInUse(file: File): Boolean {
        try {
            // Check if file is locked by trying to rename it to itself
            val parent = file.parentFile ?: return false
            val tempName = "${file.name}.${System.currentTimeMillis()}.temp"
            val tempFile = File(parent, tempName)

            if (file.renameTo(tempFile)) {
                tempFile.renameTo(file)
                return false
            }
            return true
        } catch (e: Exception) {
            return true // Assume in use if we can't check
        }
    }

    private fun isUserCriticalFile(file: File): Boolean {
        val fileName = file.name.lowercase(Locale.getDefault())
        val criticalFileNames = listOf(
            "contacts.db", "messages.db", "calendar.db", "photos.db",
            "settings.db", "accounts.db", "bookmarks.db"
        )

        return criticalFileNames.any { fileName.contains(it) }
    }

    private fun hasDeletePermission(file: File): Boolean {
        try {
            val parent = file.parentFile ?: return false
            return parent.canWrite()
        } catch (e: Exception) {
            return false
        }
    }

    private fun isSystemProtected(file: File): Boolean {
        val path = file.absolutePath

        // Check if file is in system-protected areas
        return path.startsWith("/system/") ||
                path.startsWith("/proc/") ||
                path.startsWith("/dev/") ||
                path.contains("/.android_secure/")
    }

    private fun isSuspiciousFile(file: File): Boolean {
        val fileName = file.name.lowercase(Locale.getDefault())

        // Check for malware indicators
        if (MALWARE_INDICATORS.any { fileName.contains(it) }) {
            return true
        }

        // Check for hidden executable files
        if (fileName.startsWith(".") && (fileName.endsWith(".sh") || fileName.endsWith(".bin"))) {
            return true
        }

        // Check for files with suspicious extensions in unusual locations
        val suspiciousExtensions = listOf(".exe", ".bat", ".cmd", ".scr", ".pif")
        if (suspiciousExtensions.any { fileName.endsWith(it) }) {
            return true
        }

        return false
    }

    private fun isExternallyMounted(directory: File): Boolean {
        val path = directory.absolutePath

        // Check if directory is on external storage that might be unsafe
        val externalStoragePath = Environment.getExternalStorageDirectory().absolutePath

        return path.startsWith(externalStoragePath) &&
                path != externalStoragePath &&
                !path.startsWith("$externalStoragePath/Android/data/${context.packageName}")
    }

    private fun isRecentFile(file: File, daysThreshold: Int): Boolean {
        val ageInMs = System.currentTimeMillis() - file.lastModified()
        val daysOld = ageInMs / (24 * 60 * 60 * 1000)
        return daysOld < daysThreshold
    }

    fun validateOperation(operation: String, files: List<File>): ValidationResult {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        for (file in files) {
            when (operation) {
                "DELETE" -> {
                    if (!isSafeToDelete(file)) {
                        issues.add("Cannot safely delete: ${file.name}")
                    }
                    if (isUserCriticalFile(file)) {
                        warnings.add("Deleting important file: ${file.name}")
                    }
                }
                "MOVE", "COPY" -> {
                    if (!isSafeToRead(file)) {
                        issues.add("Cannot access file: ${file.name}")
                    }
                }
            }
        }

        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            warnings = warnings
        )
    }

    fun isPackageSystemApp(packageName: String): Boolean {
        return SYSTEM_PACKAGES.any { packageName.startsWith(it) }
    }

    fun createSecureBackupPath(originalPath: String): String {
        val backupDir = File(context.cacheDir, "secure_backup")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val originalFile = File(originalPath)
        return File(backupDir, "${timestamp}_${originalFile.name}").absolutePath
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val warnings: List<String>
)