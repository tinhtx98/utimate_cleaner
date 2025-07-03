package com.mars.ultimatecleaner.domain.model

data class PerformanceOptimizationResult(
    val type: OptimizationType,
    val success: Boolean,
    val improvements: List<String> = emptyList(),
    val performanceGain: Float = 0f,
    val duration: Long = 0L,
    val error: String? = null
)

data class MemoryOptimizationResult(
    val success: Boolean,
    val freedMemory: Long = 0L, // in MB
    val trimmedApps: List<String> = emptyList(),
    val performanceImprovement: Float = 0f,
    val duration: Long = 0L,
    val error: String? = null
)

data class CpuOptimizationResult(
    val success: Boolean,
    val optimizations: List<String> = emptyList(),
    val performanceImprovement: Float = 0f,
    val duration: Long = 0L,
    val error: String? = null
)

data class BatteryOptimization(
    val batteryDrainApps: List<BatteryDrainApp> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val estimatedSavings: Int = 0, // percentage
    val optimizationsApplied: Boolean = false
)

data class BatteryDrainApp(
    val name: String,
    val packageName: String,
    val batteryUsage: Float,
    val backgroundUsage: Float
)

data class PhotoAnalysis(
    val totalPhotos: Int,
    val blurryPhotos: List<BlurryPhoto>,
    val lowQualityPhotos: List<LowQualityPhoto>,
    val similarPhotoGroups: List<SimilarPhotoGroup>,
    val analysisTimestamp: Long,
    val error: String? = null
)

data class BlurryPhoto(
    val path: String,
    val blurScore: Float,
    val size: Long
)

data class LowQualityPhoto(
    val path: String,
    val qualityScore: Float,
    val size: Long? = null,
    val issues: List<String> = emptyList()
)

data class SimilarPhotoGroup(
    val photos: List<SimilarPhoto>,
    val similarityScore: Float,
    val groupId: String
)

data class SimilarPhoto(
    val path: String,
    val size: Long,
    val lastModified: Long
)

data class PhotoOptimizationResult(
    val suggestions: List<String>,
    val potentialSpaceSavings: Long,
    val optimizablePhotos: Int,
    val duplicateGroups: Int
)

data class LargeFile(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val type: String,
    val category: String
)

data class LargeFileAnalysis(
    val totalLargeFiles: Int,
    val totalSize: Long,
    val categoryBreakdown: Map<String, Long>,
    val largestFile: LargeFile?,
    val averageSize: Long,
    val analysisTimestamp: Long,
    val error: String? = null
)

data class OptimizationResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: OptimizationType,
    val spaceSaved: Long,
    val filesProcessed: Int,
    val duration: Long,
    val improvements: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class OptimizationProgress(
    val currentStep: String,
    val progress: Int,
    val totalSteps: Int = 100,
    val startTime: Long = System.currentTimeMillis()
)

data class OptimizationRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val estimatedBenefit: String,
    val priority: Priority
)

data class OptimizationSchedule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: OptimizationType,
    val frequency: ScheduleFrequency,
    val timeOfDay: Int, // Hour in 24h format
    val isEnabled: Boolean,
    val lastRun: Long? = null,
    val nextRun: Long? = null
)

data class OptimizationStats(
    val totalOptimizations: Int = 0,
    val totalSpaceSaved: Long = 0L,
    val totalFilesProcessed: Int = 0,
    val averageDuration: Long = 0L,
    val lastOptimization: Long? = null,
    val optimizationsByType: Map<OptimizationType, Int> = emptyMap()
)

data class OptimizationBenefit(
    val estimatedSpaceSavings: Long = 0L,
    val estimatedTimeRequired: Int = 0, // seconds
    val confidenceLevel: Float = 0f,
    val benefits: List<String> = emptyList(),
    val error: String? = null
)

data class CompressionResult(
    val success: Boolean,
    val originalSize: Long = 0L,
    val compressedSize: Long = 0L,
    val spaceSaved: Long = 0L,
    val compressionRatio: Float = 0f,
    val outputPath: String? = null,
    val error: String? = null
)

data class CompressionProgressOptimization(
    val currentFile: String = "",
    val processedFiles: Int = 0,
    val totalFiles: Int = 0,
    val spaceSaved: Long = 0L,
    val percentage: Int = 0,
    val isComplete: Boolean = false,
    val error: String? = null
)

data class CompressionEstimate(
    val totalFiles: Int = 0,
    val compressibleFiles: Int = 0,
    val originalSize: Long = 0L,
    val estimatedCompressedSize: Long = 0L,
    val estimatedSavings: Long = 0L,
    val averageCompressionRatio: Float = 0f,
    val error: String? = null
)

enum class OptimizationType {
    QUICK, DEEP, COMPREHENSIVE, MEMORY, STORAGE, BATTERY, CPU
}

enum class RecommendationType {
    STORAGE_CLEANUP, PERFORMANCE_BOOST, BATTERY_OPTIMIZATION, PHOTO_CLEANUP, APP_MANAGEMENT, DUPLICATE_REMOVAL
}

enum class Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class ScheduleFrequency {
    DAILY, WEEKLY, MONTHLY, CUSTOM
}

enum class CompressionLevel {
    LOW, MEDIUM, HIGH, MAXIMUM
}