package com.mars.ultimatecleaner.data.repository

import android.content.Context
import com.mars.ultimatecleaner.core.compression.CompressionEngine
import com.mars.ultimatecleaner.data.algorithm.*
import com.mars.ultimatecleaner.data.database.dao.*
import com.mars.ultimatecleaner.data.database.entity.toEntity
import com.mars.ultimatecleaner.data.utils.FileUtils
import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.domain.repository.OptimizerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import android.webkit.MimeTypeMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OptimizerRepositoryImpl @Inject constructor(
    private val context: Context,
    private val fileUtils: FileUtils,
    private val compressionEngine: CompressionEngine,
    private val photoAnalyzer: PhotoAnalyzer,
    private val performanceOptimizer: PerformanceOptimizer,
    private val optimizationDao: OptimizationDao,
    private val fileAnalysisDao: FileAnalysisDao
) : OptimizerRepository {

    override suspend fun optimizePerformance(type: OptimizationType): PerformanceOptimizationResult = withContext(Dispatchers.IO) {
        try {
            when (type) {
                OptimizationType.MEMORY -> {
                    val memoryResult = optimizeMemory()
                    PerformanceOptimizationResult(
                        type = type,
                        success = memoryResult.success,
                        improvements = listOf("Freed ${memoryResult.freedMemory}MB of RAM"),
                        performanceGain = memoryResult.performanceImprovement,
                        duration = memoryResult.duration
                    )
                }
                OptimizationType.CPU -> {
                    val cpuResult = optimizeCpu()
                    PerformanceOptimizationResult(
                        type = type,
                        success = cpuResult.success,
                        improvements = cpuResult.optimizations,
                        performanceGain = cpuResult.performanceImprovement,
                        duration = cpuResult.duration
                    )
                }
                OptimizationType.COMPREHENSIVE -> {
                    performComprehensiveOptimization()
                }
                else -> {
                    performanceOptimizer.quickOptimization()
                }
            }
        } catch (e: Exception) {
            PerformanceOptimizationResult(
                type = type,
                success = false,
                error = e.message,
                duration = 0L
            )
        }
    }

    override suspend fun optimizeMemory(): MemoryOptimizationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val initialMemory = performanceOptimizer.getAvailableMemory()

        try {
            // Clear system caches
            performanceOptimizer.clearSystemCache()

            // Trim background apps
            val trimmedApps = performanceOptimizer.trimBackgroundApps()

            // Force garbage collection
            performanceOptimizer.forceGarbageCollection()

            val finalMemory = performanceOptimizer.getAvailableMemory()
            val freedMemory = finalMemory - initialMemory
            val duration = System.currentTimeMillis() - startTime

            MemoryOptimizationResult(
                success = freedMemory > 0,
                freedMemory = freedMemory / (1024 * 1024), // Convert to MB
                trimmedApps = trimmedApps,
                performanceImprovement = calculatePerformanceImprovement(freedMemory),
                duration = duration
            )
        } catch (e: Exception) {
            MemoryOptimizationResult(
                success = false,
                error = e.message,
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun optimizeCpu(): CpuOptimizationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val optimizations = mutableListOf<String>()

            // Reduce CPU frequency for background apps
            val backgroundAppsOptimized = performanceOptimizer.optimizeBackgroundAppsCpu()
            if (backgroundAppsOptimized > 0) {
                optimizations.add("Optimized CPU usage for $backgroundAppsOptimized background apps")
            }

            // Clean up running processes
            val processesKilled = performanceOptimizer.killUnnecessaryProcesses()
            if (processesKilled > 0) {
                optimizations.add("Terminated $processesKilled unnecessary processes")
            }

            // Optimize system services
            val servicesOptimized = performanceOptimizer.optimizeSystemServices()
            if (servicesOptimized > 0) {
                optimizations.add("Optimized $servicesOptimized system services")
            }

            val duration = System.currentTimeMillis() - startTime

            CpuOptimizationResult(
                success = optimizations.isNotEmpty(),
                optimizations = optimizations,
                performanceImprovement = calculateCpuPerformanceImprovement(optimizations.size),
                duration = duration
            )
        } catch (e: Exception) {
            CpuOptimizationResult(
                success = false,
                error = e.message,
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    override suspend fun optimizeBattery(): BatteryOptimization = withContext(Dispatchers.IO) {
        try {
            val batteryDrainApps = performanceOptimizer.findBatteryDrainingApps()
            val locationServicesOptimized = performanceOptimizer.optimizeLocationServices()
            val backgroundSyncOptimized = performanceOptimizer.optimizeBackgroundSync()

            val recommendations = mutableListOf<String>()

            if (batteryDrainApps.isNotEmpty()) {
                recommendations.add("Found ${batteryDrainApps.size} apps consuming excessive battery")
                recommendations.addAll(batteryDrainApps.take(3).map { "Optimize ${it.name}" })
            }

            if (locationServicesOptimized) {
                recommendations.add("Optimized location services for better battery life")
            }

            if (backgroundSyncOptimized) {
                recommendations.add("Reduced background sync frequency")
            }

            BatteryOptimization(
                batteryDrainApps = batteryDrainApps,
                recommendations = recommendations,
                estimatedSavings = calculateBatterySavings(batteryDrainApps.size),
                optimizationsApplied = locationServicesOptimized || backgroundSyncOptimized
            )
        } catch (e: Exception) {
            BatteryOptimization(
                batteryDrainApps = emptyList(),
                recommendations = listOf("Failed to analyze battery usage: ${e.message}"),
                estimatedSavings = 0,
                optimizationsApplied = false
            )
        }
    }

    override suspend fun analyzePhotos(): PhotoAnalysis = withContext(Dispatchers.IO) {
        try {
            val photos = fileUtils.findImageFiles()
            val blurryPhotos = mutableListOf<BlurryPhoto>()
            val lowQualityPhotos = mutableListOf<LowQualityPhoto>()
            val similarPhotoGroups = mutableListOf<SimilarPhotoGroup>()

            photos.forEach { photoFile ->
                try {
                    // Analyze for blur
                    val blurScore = photoAnalyzer.calculateBlurScore(photoFile.absolutePath)
                    if (blurScore > 0.7) { // Threshold for blurry photos
                        blurryPhotos.add(
                            BlurryPhoto(
                                path = photoFile.absolutePath,
                                blurScore = blurScore,
                                size = photoFile.length()
                            )
                        )
                    }

                    // Analyze quality
                    val qualityScore = photoAnalyzer.calculateQualityScore(photoFile.absolutePath)
                    if (qualityScore < 0.3) { // Threshold for low quality
                        lowQualityPhotos.add(
                            LowQualityPhoto(
                                path = photoFile.absolutePath,
                                qualityScore = qualityScore,
                                issues = photoAnalyzer.identifyQualityIssues(photoFile.absolutePath)
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Skip problematic files
                }
            }

            // Find similar photos
            similarPhotoGroups.addAll(photoAnalyzer.findSimilarPhotos(photos))

            PhotoAnalysis(
                totalPhotos = photos.size,
                blurryPhotos = blurryPhotos,
                lowQualityPhotos = lowQualityPhotos,
                similarPhotoGroups = similarPhotoGroups,
                analysisTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            PhotoAnalysis(
                totalPhotos = 0,
                blurryPhotos = emptyList(),
                lowQualityPhotos = emptyList(),
                similarPhotoGroups = emptyList(),
                error = e.message,
                analysisTimestamp = System.currentTimeMillis()
            )
        }
    }

    override suspend fun optimizePhotos(): PhotoOptimizationResult = withContext(Dispatchers.IO) {
        try {
            val suggestions = mutableListOf<String>()
            var potentialSavings = 0L

            val photoAnalysis = analyzePhotos()

            // Calculate potential savings from removing blurry photos
            val blurryPhotosSavings = photoAnalysis.blurryPhotos.sumOf { it.size }
            if (blurryPhotosSavings > 0) {
                suggestions.add("Remove ${photoAnalysis.blurryPhotos.size} blurry photos to save ${formatFileSize(blurryPhotosSavings)}")
                potentialSavings += blurryPhotosSavings
            }

            // Calculate potential savings from removing low quality photos
            val lowQualityPhotosSavings = photoAnalysis.lowQualityPhotos.sumOf { it.size ?: 0L }
            if (lowQualityPhotosSavings > 0) {
                suggestions.add("Remove ${photoAnalysis.lowQualityPhotos.size} low quality photos to save ${formatFileSize(lowQualityPhotosSavings)}")
                potentialSavings += lowQualityPhotosSavings
            }

            // Calculate potential savings from removing duplicate photos
            val duplicateSavings = photoAnalysis.similarPhotoGroups.sumOf { group ->
                group.photos.drop(1).sumOf { it.size }
            }
            if (duplicateSavings > 0) {
                suggestions.add("Remove duplicate photos to save ${formatFileSize(duplicateSavings)}")
                potentialSavings += duplicateSavings
            }

            PhotoOptimizationResult(
                suggestions = suggestions,
                potentialSpaceSavings = potentialSavings,
                optimizablePhotos = photoAnalysis.blurryPhotos.size + photoAnalysis.lowQualityPhotos.size,
                duplicateGroups = photoAnalysis.similarPhotoGroups.size
            )
        } catch (e: Exception) {
            PhotoOptimizationResult(
                suggestions = listOf("Failed to analyze photos: ${e.message}"),
                potentialSpaceSavings = 0L,
                optimizablePhotos = 0,
                duplicateGroups = 0
            )
        }
    }

    override suspend fun compressPhoto(photoPath: String, quality: Int): CompressionResult = withContext(Dispatchers.IO) {
        try {
            val originalFile = File(photoPath)
            val originalSize = originalFile.length()

            val outputFile = File(originalFile.parent, "compressed_${originalFile.name}")
            val result = compressionEngine.compressImage(originalFile, outputFile, quality)

            when (result) {
                is com.mars.ultimatecleaner.core.compression.CompressionResult.Success -> {
                    CompressionResult(
                        success = true,
                        originalSize = result.originalSize,
                        compressedSize = result.compressedSize,
                        spaceSaved = result.savedBytes,
                        compressionRatio = result.compressionRatio,
                        outputPath = outputFile.absolutePath
                    )
                }
                is com.mars.ultimatecleaner.core.compression.CompressionResult.Error -> {
                    CompressionResult(
                        success = false,
                        error = result.message
                    )
                }
            }
        } catch (e: Exception) {
            CompressionResult(
                success = false,
                error = e.message
            )
        }
    }

    override suspend fun detectBlurryPhotos(): List<BlurryPhoto> = withContext(Dispatchers.IO) {
        try {
            val photos = fileUtils.findImageFiles()
            val blurryPhotos = mutableListOf<BlurryPhoto>()

            photos.forEach { photoFile ->
                try {
                    val blurScore = photoAnalyzer.calculateBlurScore(photoFile.absolutePath)
                    if (blurScore > 0.7) {
                        blurryPhotos.add(
                            BlurryPhoto(
                                path = photoFile.absolutePath,
                                blurScore = blurScore,
                                size = photoFile.length()
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Skip problematic files
                }
            }

            blurryPhotos
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun findSimilarPhotos(): List<SimilarPhotoGroup> = withContext(Dispatchers.IO) {
        try {
            val photos = fileUtils.findImageFiles()
            photoAnalyzer.findSimilarPhotos(photos)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun findLargeFiles(minimumSize: Long): List<LargeFile> = withContext(Dispatchers.IO) {
        try {
            val largeFiles = mutableListOf<LargeFile>()

            fileUtils.scanStorageDirectories { file ->
                if (file.isFile && file.length() >= minimumSize) {
                    largeFiles.add(
                        LargeFile(
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            type = fileUtils.getMimeType(file),
                            category = categorizeFileByType(file)
                        )
                    )
                }
            }

            largeFiles.sortedByDescending { it.size }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun analyzeLargeFiles(): LargeFileAnalysis = withContext(Dispatchers.IO) {
        try {
            val largeFiles = findLargeFiles()
            val categoryBreakdown = largeFiles.groupBy { it.category }
                .mapValues { (_, files) -> files.sumOf { it.size } }

            LargeFileAnalysis(
                totalLargeFiles = largeFiles.size,
                totalSize = largeFiles.sumOf { it.size },
                categoryBreakdown = categoryBreakdown,
                largestFile = largeFiles.maxByOrNull { it.size },
                averageSize = if (largeFiles.isNotEmpty()) largeFiles.sumOf { it.size } / largeFiles.size else 0L,
                analysisTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            LargeFileAnalysis(
                totalLargeFiles = 0,
                totalSize = 0L,
                categoryBreakdown = emptyMap(),
                largestFile = null,
                averageSize = 0L,
                error = e.message,
                analysisTimestamp = System.currentTimeMillis()
            )
        }
    }

    override suspend fun categorizeLargeFiles(): Map<String, List<LargeFile>> = withContext(Dispatchers.IO) {
        try {
            val largeFiles = findLargeFiles()
            largeFiles.groupBy { it.category }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun saveOptimizationResult(result: OptimizationResultSettings) = withContext(Dispatchers.IO) {
        try {
            optimizationDao.insertOptimizationResult(result.toEntity())
        } catch (e: Exception) {
            // Log error but don't fail the operation
        }
    }

    override suspend fun getOptimizationHistory(limit: Int): List<OptimizationResultSettings> = withContext(Dispatchers.IO) {
        try {
            optimizationDao.getOptimizationHistory(limit).map { it.toDomainModel() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getOptimizationStats(): OptimizationStats = withContext(Dispatchers.IO) {
        try {
            val history = optimizationDao.getAllOptimizationResults()
            val totalOptimizations = history.size
            val totalSpaceSaved = history.sumOf { it.spaceSaved }
            val totalFilesProcessed = history.sumOf { it.filesProcessed }
            val averageDuration = if (history.isNotEmpty()) history.sumOf { it.duration } / history.size else 0L
            val lastOptimization = history.maxByOrNull { it.timestamp }?.timestamp

            OptimizationStats(
                totalOptimizations = totalOptimizations,
                totalSpaceSaved = totalSpaceSaved,
                totalFilesProcessed = totalFilesProcessed,
                averageDuration = averageDuration,
                lastOptimization = lastOptimization,
                optimizationsByType = history.groupBy { OptimizationType.valueOf(it.optimizationType) }.mapValues { it.value.size }
            )
        } catch (e: Exception) {
            OptimizationStats()
        }
    }

    override suspend fun clearOptimizationHistory() = withContext(Dispatchers.IO) {
        try {
            optimizationDao.clearOptimizationHistory()
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun saveOptimizationSchedule(schedule: OptimizationSchedule) = withContext(Dispatchers.IO) {
        try {
            optimizationDao.insertOptimizationSchedule(schedule.toEntity())
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun getOptimizationSchedules(): List<OptimizationSchedule> = withContext(Dispatchers.IO) {
        try {
            optimizationDao.getOptimizationSchedules().map { it.toDomainModel() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun deleteOptimizationSchedule(scheduleId: String) = withContext(Dispatchers.IO) {
        try {
            optimizationDao.deleteOptimizationSchedule(scheduleId)
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun updateOptimizationSchedule(schedule: OptimizationSchedule) = withContext(Dispatchers.IO) {
        try {
            optimizationDao.updateOptimizationSchedule(schedule.toEntity())
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun generateOptimizationRecommendations(): List<OptimizationRecommendation> = withContext(Dispatchers.IO) {
        try {
            val recommendations = mutableListOf<OptimizationRecommendation>()

            // Analyze storage for recommendations
            val largeFiles = findLargeFiles(50 * 1024 * 1024) // 50MB threshold
            if (largeFiles.isNotEmpty()) {
                recommendations.add(
                    OptimizationRecommendation(
                        type = RecommendationType.STORAGE_CLEANUP,
                        title = "Review Large Files",
                        description = "Found ${largeFiles.size} files larger than 50MB",
                        estimatedBenefit = "Potential to free ${formatFileSize(largeFiles.sumOf { it.size })}",
                        priority = if (largeFiles.size > 10) Priority.HIGH else Priority.MEDIUM
                    )
                )
            }

            // Analyze photos for recommendations
            val photoAnalysis = analyzePhotos()
            if (photoAnalysis.blurryPhotos.isNotEmpty() || photoAnalysis.lowQualityPhotos.isNotEmpty()) {
                recommendations.add(
                    OptimizationRecommendation(
                        type = RecommendationType.PHOTO_CLEANUP,
                        title = "Clean Up Photos",
                        description = "Found ${photoAnalysis.blurryPhotos.size} blurry and ${photoAnalysis.lowQualityPhotos.size} low-quality photos",
                        estimatedBenefit = "Clean up photo collection",
                        priority = Priority.MEDIUM
                    )
                )
            }

            // Battery optimization recommendations
            val batteryOptimization = optimizeBattery()
            if (batteryOptimization.batteryDrainApps.isNotEmpty()) {
                recommendations.add(
                    OptimizationRecommendation(
                        type = RecommendationType.BATTERY_OPTIMIZATION,
                        title = "Optimize Battery Usage",
                        description = "Found ${batteryOptimization.batteryDrainApps.size} apps consuming excessive battery",
                        estimatedBenefit = "Extend battery life by ${batteryOptimization.estimatedSavings}%",
                        priority = if (batteryOptimization.estimatedSavings > 20) Priority.HIGH else Priority.MEDIUM
                    )
                )
            }

            recommendations
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun calculateOptimizationBenefit(type: OptimizationType): OptimizationBenefit = withContext(Dispatchers.IO) {
        try {
            when (type) {
                OptimizationType.QUICK -> {
                    val cacheSize = fileUtils.calculateCacheSize()
                    val tempFiles = fileUtils.findTempFiles()
                    val tempSize = tempFiles.sumOf { it.length() }

                    OptimizationBenefit(
                        estimatedSpaceSavings = cacheSize + tempSize,
                        estimatedTimeRequired = 30, // seconds
                        confidenceLevel = 0.9f,
                        benefits = listOf(
                            "Free up to ${formatFileSize(cacheSize + tempSize)}",
                            "Improve app loading times",
                            "Free up RAM"
                        )
                    )
                }
                OptimizationType.DEEP -> {
                    val largeFiles = findLargeFiles()
                    val duplicates = findSimilarPhotos()
                    val potentialSavings = largeFiles.take(10).sumOf { it.size } +
                            duplicates.sumOf { group -> group.photos.drop(1).sumOf { it.size } }

                    OptimizationBenefit(
                        estimatedSpaceSavings = potentialSavings,
                        estimatedTimeRequired = 300, // seconds
                        confidenceLevel = 0.7f,
                        benefits = listOf(
                            "Comprehensive storage cleanup",
                            "Remove duplicate files",
                            "Optimize system performance",
                            "Improve battery life"
                        )
                    )
                }
                else -> OptimizationBenefit()
            }
        } catch (e: Exception) {
            OptimizationBenefit(error = e.message)
        }
    }

    override suspend fun getDeviceOptimizationScore(): Int = withContext(Dispatchers.IO) {
        try {
            var score = 100

            // Deduct points for large files
            val largeFiles = findLargeFiles()
            score -= minOf(30, largeFiles.size * 2)

            // Deduct points for cache size
            val cacheSize = fileUtils.calculateCacheSize()
            if (cacheSize > 500 * 1024 * 1024) { // 500MB
                score -= 20
            } else if (cacheSize > 100 * 1024 * 1024) { // 100MB
                score -= 10
            }

            // Deduct points for storage usage
            val storageUsage = fileUtils.getStorageUsagePercentage()
            if (storageUsage > 90) {
                score -= 25
            } else if (storageUsage > 80) {
                score -= 15
            } else if (storageUsage > 70) {
                score -= 5
            }

            // Deduct points for photo issues
            val photoAnalysis = analyzePhotos()
            score -= minOf(15, (photoAnalysis.blurryPhotos.size + photoAnalysis.lowQualityPhotos.size) / 10)

            maxOf(0, score)
        } catch (e: Exception) {
            50 // Default middle score on error
        }
    }

    override suspend fun compressMedia(filePaths: List<String>, compressionLevel: CompressionLevel): Flow<CompressionProgressOptimization> = flow {
        try {
            var processedFiles = 0
            var totalSpaceSaved = 0L
            val totalFiles = filePaths.size

            emit(CompressionProgressOptimization(
                currentFile = "",
                processedFiles = 0,
                totalFiles = totalFiles,
                spaceSaved = 0L,
                percentage = 0
            ))

            filePaths.forEach { filePath ->
                try {
                    emit(CompressionProgressOptimization(
                        currentFile = File(filePath).name,
                        processedFiles = processedFiles,
                        totalFiles = totalFiles,
                        spaceSaved = totalSpaceSaved,
                        percentage = (processedFiles * 100) / totalFiles
                    ))

                    val coreCompressionLevel = when (compressionLevel) {
                        CompressionLevel.LOW -> com.mars.ultimatecleaner.core.compression.CompressionLevel.LOW
                        CompressionLevel.MEDIUM -> com.mars.ultimatecleaner.core.compression.CompressionLevel.MEDIUM
                        CompressionLevel.HIGH -> com.mars.ultimatecleaner.core.compression.CompressionLevel.HIGH
                        CompressionLevel.MAXIMUM -> com.mars.ultimatecleaner.core.compression.CompressionLevel.MAXIMUM
                    }

                    val result = compressionEngine.compressFile(filePath, coreCompressionLevel)
                    when (result) {
                        is com.mars.ultimatecleaner.core.compression.CompressionResult.Success -> {
                            totalSpaceSaved += result.savedBytes
                        }
                        is com.mars.ultimatecleaner.core.compression.CompressionResult.Error -> {
                            // Log error but continue
                        }
                    }

                    processedFiles++
                } catch (e: Exception) {
                    // Continue with next file
                    processedFiles++
                }
            }

            emit(CompressionProgressOptimization(
                currentFile = "",
                processedFiles = processedFiles,
                totalFiles = totalFiles,
                spaceSaved = totalSpaceSaved,
                percentage = 100,
                isComplete = true
            ))
        } catch (e: Exception) {
            emit(CompressionProgressOptimization(
                error = e.message,
                percentage = 0
            ))
        }
    }

    override suspend fun estimateCompressionSavings(filePaths: List<String>): CompressionEstimate = withContext(Dispatchers.IO) {
        try {
            var totalOriginalSize = 0L
            var estimatedCompressedSize = 0L
            var compressibleFiles = 0

            filePaths.forEach { filePath ->
                try {
                    val file = File(filePath)
                    val originalSize = file.length()
                    totalOriginalSize += originalSize

                    // Estimate compression based on file type
                    val compressionRatio = compressionEngine.estimateCompressionRatio(file)
                    estimatedCompressedSize += (originalSize * compressionRatio).toLong()
                    compressibleFiles++
                } catch (e: Exception) {
                    // Skip problematic files
                }
            }

            val estimatedSavings = totalOriginalSize - estimatedCompressedSize

            CompressionEstimate(
                totalFiles = filePaths.size,
                compressibleFiles = compressibleFiles,
                originalSize = totalOriginalSize,
                estimatedCompressedSize = estimatedCompressedSize,
                estimatedSavings = estimatedSavings,
                averageCompressionRatio = if (totalOriginalSize > 0) estimatedCompressedSize.toFloat() / totalOriginalSize.toFloat() else 0f
            )
        } catch (e: Exception) {
            CompressionEstimate(error = e.message)
        }
    }

    private suspend fun performComprehensiveOptimization(): PerformanceOptimizationResult {
        val improvements = mutableListOf<String>()
        var totalGain = 0f
        val startTime = System.currentTimeMillis()

        try {
            // Memory optimization
            val memoryResult = optimizeMemory()
            if (memoryResult.success) {
                improvements.add("Freed ${memoryResult.freedMemory}MB of RAM")
                totalGain += memoryResult.performanceImprovement
            }

            // CPU optimization
            val cpuResult = optimizeCpu()
            if (cpuResult.success) {
                improvements.addAll(cpuResult.optimizations)
                totalGain += cpuResult.performanceImprovement
            }

            // Battery optimization
            val batteryResult = optimizeBattery()
            if (batteryResult.optimizationsApplied) {
                improvements.add("Optimized battery usage")
                totalGain += 10f // Arbitrary performance gain for battery optimization
            }

            val duration = System.currentTimeMillis() - startTime

            PerformanceOptimizationResult(
                type = OptimizationType.COMPREHENSIVE,
                success = improvements.isNotEmpty(),
                improvements = improvements,
                performanceGain = totalGain,
                duration = duration
            )
        } catch (e: Exception) {
            PerformanceOptimizationResult(
                type = OptimizationType.COMPREHENSIVE,
                success = false,
                error = e.message,
                duration = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun calculatePerformanceImprovement(freedMemory: Long): Float {
        // Calculate performance improvement based on freed memory
        return minOf(30f, (freedMemory / (1024 * 1024)).toFloat()) // Max 30% improvement
    }

    private fun calculateCpuPerformanceImprovement(optimizationCount: Int): Float {
        // Calculate performance improvement based on number of optimizations
        return minOf(25f, optimizationCount * 5f) // Max 25% improvement
    }

    private fun calculateBatterySavings(drainAppCount: Int): Int {
        // Calculate estimated battery savings percentage
        return minOf(40, drainAppCount * 8) // Max 40% savings
    }

    private fun categorizeFileByType(file: File): String {
        val mimeType = fileUtils.getMimeType(file)
        return when {
            mimeType.startsWith("image/") -> "Images"
            mimeType.startsWith("video/") -> "Videos"
            mimeType.startsWith("audio/") -> "Audio"
            mimeType.startsWith("text/") || mimeType.contains("document") -> "Documents"
            mimeType.contains("application") -> "Applications"
            else -> "Other"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.1f %s".format(size, units[unitIndex])
    }
}