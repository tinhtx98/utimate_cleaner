package com.mars.ultimatecleaner.domain.model

// Core Models
data class FileItem(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
    val isDirectory: Boolean = false,
    val thumbnailPath: String? = null,
    val extension: String = "",
    val isHidden: Boolean = false,
    val canRead: Boolean = true,
    val canWrite: Boolean = true,
    val canExecute: Boolean = false
)

data class FileMetadata(
    val path: String,
    val size: Long,
    val lastModified: Long,
    val lastAccessed: Long,
    val mimeType: String,
    val md5Hash: String? = null,
    val isMediaFile: Boolean = false,
    val mediaMetadata: MediaMetadata? = null
)

data class MediaMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val duration: Long? = null,
    val bitrate: Int? = null,
    val format: String? = null
)

data class StorageInfoDomain(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val usagePercentage: Float,
    val categoryBreakdown: Map<String, Long>
)

// Cleaning Models
data class ScanProgressDomain(
    val percentage: Float,
    val currentCategory: String,
    val scannedFiles: Int,
    val totalFiles: Int,
    val currentFile: String = "",
    val isComplete: Boolean = false
)

data class CleaningProgressDomain(
    val percentage: Float,
    val currentCategory: String,
    val cleanedFiles: Int,
    val totalFiles: Int,
    val spaceSaved: Long,
    val currentFile: String = "",
    val isComplete: Boolean = false
)

data class JunkCategoryDomain(
    val id: String,
    val name: String,
    val description: String,
    val size: Long,
    val fileCount: Int,
    val isSelected: Boolean = true,
    val subCategories: List<JunkSubCategoryDomain> = emptyList(),
    val priority: CleaningPriorityDomain = CleaningPriorityDomain.MEDIUM
)

data class JunkSubCategoryDomain(
    val name: String,
    val size: Long,
    val fileCount: Int,
    val files: List<JunkFileDomain> = emptyList()
)

data class JunkFileDomain(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val canDelete: Boolean = true,
    val deleteReason: String = ""
)

data class CleaningHistoryItemDomain(
    val id: String,
    val timestamp: Long,
    val operation: String,
    val spaceSaved: Long,
    val filesDeleted: Int,
    val duration: Long,
    val categories: List<String>
)

// Optimizer Models
data class DuplicateGroup(
    val id: String,
    val files: List<FileItem>,
    val totalSize: Long,
    val hash: String,
    val keepFile: String? = null // Path of file to keep
)

data class PhotoItemDomain(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val blurScore: Float,
    val qualityScore: Float,
    val thumbnailPath: String?,
    val resolution: String,
    val isBlurry: Boolean = false,
    val isLowQuality: Boolean = false
)

data class PhotoQualityResult(
    val path: String,
    val blurScore: Float,
    val qualityScore: Float,
    val brightness: Float,
    val contrast: Float,
    val isBlurry: Boolean,
    val isLowQuality: Boolean
)

data class CompressionResult(
    val originalPath: String,
    val compressedPath: String,
    val originalSize: Long,
    val compressedSize: Long,
    val spaceSaved: Long,
    val compressionRatio: Float,
    val quality: Int,
    val isSuccessful: Boolean,
    val errorMessage: String? = null
)

// Operation Results
data class FileOperationResult(
    val isSuccess: Boolean,
    val successCount: Int,
    val failedCount: Int,
    val errorMessage: String? = null,
    val failedFiles: List<String> = emptyList()
)

data class OperationResult(
    val isSuccess: Boolean,
    val message: String? = null,
    val errorCode: String? = null
)

// Analytics Models
data class UsageAnalyticsDomain(
    val totalCleaningOperations: Int,
    val totalSpaceSaved: Long,
    val totalFilesDeleted: Int,
    val averageCleaningFrequency: Int,
    val mostUsedFeature: String,
    val lastCleaningDate: Long,
    val appUsageTime: Long,
    val featuresUsed: Map<String, Int>
)

data class CleaningStatistics(
    val totalCacheCleared: Long,
    val totalDuplicatesRemoved: Int,
    val totalLargeFilesFound: Int,
    val totalEmptyFoldersRemoved: Int,
    val averageCleaningSize: Long,
    val cleaningFrequency: Map<String, Int>
)

data class PerformanceMetric(
    val operation: String,
    val duration: Long,
    val memoryUsage: Long,
    val timestamp: Long
)

data class UserBehaviorData(
    val sessionCount: Int,
    val averageSessionDuration: Long,
    val featureUsagePattern: Map<String, Int>,
    val errorCount: Int,
    val crashCount: Int
)

// Enums
enum class FileCategoryDomain {
    PHOTOS, VIDEOS, DOCUMENTS, AUDIO, DOWNLOADS, APPS, ALL
}

enum class CleaningPriorityDomain {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class CompressionLevelDomain(val quality: Int, val displayName: String) {
    LOW(40, "High Compression"),
    MEDIUM(60, "Balanced"),
    HIGH(80, "Low Compression"),
    LOSSLESS(100, "Lossless")
}

// Progress Models
data class DuplicateAnalysisProgress(
    val percentage: Float,
    val currentOperation: String,
    val processedFiles: Int,
    val totalFiles: Int,
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val isComplete: Boolean = false
)

data class PhotoAnalysisProgress(
    val percentage: Float,
    val currentOperation: String,
    val analyzedPhotos: Int,
    val totalPhotos: Int,
    val blurryPhotos: List<PhotoItemDomain> = emptyList(),
    val lowQualityPhotos: List<PhotoItemDomain> = emptyList(),
    val isComplete: Boolean = false
)

data class CompressionProgress(
    val percentage: Int,
    val currentFile: String,
    val processedFiles: Int,
    val totalFiles: Int,
    val spaceSaved: Long,
    val isComplete: Boolean = false
)