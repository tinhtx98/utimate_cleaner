package com.mars.ultimatecleaner.domain.repository

import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.flow.Flow

interface OptimizerRepository {
    fun detectDuplicates(): Flow<DuplicateAnalysisProgress>
    fun analyzePhotos(): Flow<PhotoAnalysisProgress>
    fun compressMedia(files: List<String>, level: CompressionLevel): Flow<CompressionProgress>
    suspend fun getInstalledApps(): List<AppInfo>
    suspend fun uninstallApp(packageName: String): OperationResult
    suspend fun getOldScreenshots(daysThreshold: Int): List<ScreenshotItem>
    suspend fun getDuplicateGroups(): List<DuplicateGroup>
    suspend fun getBlurryPhotos(): List<PhotoItem>
    suspend fun getLowQualityPhotos(): List<PhotoItem>
    suspend fun calculateFileHash(filePath: String): String
    suspend fun analyzeImageQuality(filePath: String): PhotoQualityResult
    suspend fun compressImage(filePath: String, quality: Int): CompressionResult
    suspend fun compressVideo(filePath: String, quality: Int): CompressionResult
}