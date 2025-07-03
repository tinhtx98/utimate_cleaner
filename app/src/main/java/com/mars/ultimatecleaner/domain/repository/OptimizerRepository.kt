package com.mars.ultimatecleaner.domain.repository

import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.flow.Flow

interface OptimizerRepository {

    // Performance Optimization
    suspend fun optimizePerformance(type: OptimizationType): PerformanceOptimizationResult
    suspend fun optimizeMemory(): MemoryOptimizationResult
    suspend fun optimizeCpu(): CpuOptimizationResult
    suspend fun optimizeBattery(): BatteryOptimization

    // Photo Analysis and Optimization
    suspend fun analyzePhotos(): PhotoAnalysis
    suspend fun optimizePhotos(): PhotoOptimizationResult
    suspend fun compressPhoto(photoPath: String, quality: Int): CompressionResult
    suspend fun detectBlurryPhotos(): List<BlurryPhoto>
    suspend fun findSimilarPhotos(): List<SimilarPhotoGroup>

    // Large File Analysis
    suspend fun findLargeFiles(minimumSize: Long = 100 * 1024 * 1024): List<LargeFile>
    suspend fun analyzeLargeFiles(): LargeFileAnalysis
    suspend fun categorizeLargeFiles(): Map<String, List<LargeFile>>

    // Optimization History
    suspend fun saveOptimizationResult(result: OptimizationResultSettings)
    suspend fun getOptimizationHistory(limit: Int = 50): List<OptimizationResultSettings>
    suspend fun getOptimizationStats(): OptimizationStats
    suspend fun clearOptimizationHistory()

    // Scheduled Optimization
    suspend fun saveOptimizationSchedule(schedule: OptimizationSchedule)
    suspend fun getOptimizationSchedules(): List<OptimizationSchedule>
    suspend fun deleteOptimizationSchedule(scheduleId: String)
    suspend fun updateOptimizationSchedule(schedule: OptimizationSchedule)

    // Analysis and Recommendations
    suspend fun generateOptimizationRecommendations(): List<OptimizationRecommendation>
    suspend fun calculateOptimizationBenefit(type: OptimizationType): OptimizationBenefit
    suspend fun getDeviceOptimizationScore(): Int

    // Media Compression
    suspend fun compressMedia(filePaths: List<String>, compressionLevelDomain: CompressionLevelDomain): Flow<CompressionProgressOptimization>
    suspend fun estimateCompressionSavings(filePaths: List<String>): CompressionEstimate
}