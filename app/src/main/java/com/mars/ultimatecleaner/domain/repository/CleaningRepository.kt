package com.mars.ultimatecleaner.domain.repository

import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.ui.screens.cleaner.CleaningResult
import kotlinx.coroutines.flow.Flow

interface CleaningRepository {
    fun scanJunkFiles(): Flow<ScanProgressDomain>
    fun cleanFiles(categories: List<String>): Flow<CleaningProgressDomain>
    suspend fun getJunkCategories(): List<JunkCategoryDomain>
    suspend fun getLargeFiles(thresholdMB: Int): List<FileItem>
    suspend fun getEmptyFolders(): List<String>
    suspend fun getObsoleteApkFiles(): List<FileItem>
    suspend fun getCacheFiles(): List<FileItem>
    suspend fun getTempFiles(): List<FileItem>
    suspend fun getResidualFiles(): List<FileItem>
    suspend fun setLargeFileThreshold(thresholdMB: Int)
    suspend fun getCleaningHistory(): List<CleaningHistoryItemDomain>
    suspend fun saveCleaningResult(result: CleaningResult)
}