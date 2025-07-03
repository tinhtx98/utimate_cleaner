package com.mars.ultimatecleaner.domain.model

import java.io.File

data class FileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
    val category: FileCategory,
    val isDirectory: Boolean = false,
    val permissions: FilePermissions? = null
)

data class FileMetadata(
    val path: String,
    val basicInfo: FileInfo,
    val exifData: ExifData? = null,
    val contentHash: String? = null,
    val contentAnalysis: ContentAnalysis? = null
)

data class ExifData(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val dateTime: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val orientation: Int? = null,
    val flash: String? = null,
    val focalLength: String? = null,
    val iso: String? = null
)

data class ContentAnalysis(
    val fileSignature: String,
    val isCorrupted: Boolean,
    val estimatedQuality: Float,
    val contentType: String,
    val additionalInfo: Map<String, String> = emptyMap()
)

data class FilePermissions(
    val readable: Boolean,
    val writable: Boolean,
    val executable: Boolean,
    val hidden: Boolean
)

data class FileScanProgress(
    val currentDirectory: String,
    val filesScanned: Int,
    val totalFiles: Int,
    val foundFiles: List<FileInfo> = emptyList(),
    val percentage: Int = 0,
    val isComplete: Boolean = false
)

data class SearchCriteria(
    val name: String? = null,
    val extension: String? = null,
    val mimeType: String? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val category: FileCategory? = null
)

data class OrganizationSuggestion(
    val suggestedStructure: DirectoryStructure,
    val movePlan: List<FileMove>,
    val estimatedBenefit: String,
    val confidence: Float
)

data class DirectoryStructure(
    val basePath: String,
    val directories: Map<String, List<String>> // directory name to subdirectories
)

data class OrganizationPlan(
    val moves: List<FileMove>,
    val directoriesToCreate: List<String>,
    val backupLocation: String? = null
)

data class FileMove(
    val sourcePath: String,
    val destinationPath: String,
    val reason: String
)

data class OrganizationProgress(
    val currentOperation: String,
    val processedFiles: Int,
    val totalFiles: Int,
    val successfulMoves: Int,
    val failedMoves: Int,
    val percentage: Int = 0,
    val isComplete: Boolean = false,
    val errors: List<String> = emptyList()
)

data class DuplicateDetectionProgress(
    val currentDirectory: String,
    val filesAnalyzed: Int,
    val totalFiles: Int,
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val percentage: Int = 0,
    val isComplete: Boolean = false
)

data class DuplicateGroup(
    val id: String = java.util.UUID.randomUUID().toString(),
    val files: List<DuplicateFile>,
    val totalSize: Long,
    val duplicateSize: Long, // Size of all duplicates except the original
    val detectionMethod: DuplicateDetectionMethod
)

data class DuplicateFile(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val hash: String? = null,
    val isOriginal: Boolean = false
)

data class SimilarityScore(
    val score: Float, // 0.0 to 1.0
    val method: String,
    val factors: Map<String, Float> = emptyMap()
)

data class FileOperationResult(
    val success: Boolean,
    val sourcePath: String,
    val destinationPath: String? = null,
    val error: String? = null,
    val bytesProcessed: Long = 0L
)

data class FileIntegrityResult(
    val isValid: Boolean,
    val checksum: String? = null,
    val issues: List<String> = emptyList(),
    val confidence: Float = 1.0f
)

data class SignatureVerificationResult(
    val isValid: Boolean,
    val expectedSignature: String,
    val actualSignature: String,
    val signatureType: String
)

data class DeletedFileInfo(
    val originalPath: String,
    val fileName: String,
    val size: Long,
    val deletedTimestamp: Long,
    val recoverySector: Long? = null,
    val recoveryProbability: Float = 0f
)

data class FileRecoveryResult(
    val success: Boolean,
    val recoveredPath: String? = null,
    val originalSize: Long = 0L,
    val recoveredSize: Long = 0L,
    val integrityCheck: Boolean = false,
    val error: String? = null
)

data class DirectoryStats(
    val totalFiles: Int,
    val totalDirectories: Int,
    val totalSize: Long,
    val largestFile: FileInfo? = null,
    val newestFile: FileInfo? = null,
    val oldestFile: FileInfo? = null,
    val fileTypeDistribution: Map<String, Int>
)

data class FileTypeStats(
    val mimeType: String,
    val count: Int,
    val totalSize: Long,
    val averageSize: Long,
    val percentage: Float
)

enum class FileCategory {
    IMAGES, VIDEOS, AUDIO, DOCUMENTS, APPLICATIONS, ARCHIVES, SYSTEM, OTHER
}

enum class DuplicateDetectionMethod {
    HASH, SIZE, NAME, CONTENT, METADATA
}