package com.mars.ultimatecleaner.domain.repository

import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.flow.Flow

interface CleaningRepository {
    fun scanJunkFiles(): Flow<ScanProgress>
    fun cleanFiles(categories: List<String>): Flow<CleaningProgress>
    suspend fun getJunkCategories(): List<JunkCategory>
    suspend fun getLargeFiles(thresholdMB: Int): List<FileItem>
    suspend fun getEmptyFolders(): List<String>
    suspend fun getObsoleteApkFiles(): List<FileItem>
    suspend fun getCacheFiles(): List<FileItem>
    suspend fun getTempFiles(): List<FileItem>
    suspend fun getResidualFiles(): List<FileItem>
    suspend fun setLargeFileThreshold(thresholdMB: Int)
    suspend fun getCleaningHistory(): List<CleaningHistoryItem>
    suspend fun saveCleaningResult(result: CleaningResult)
}