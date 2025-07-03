package com.mars.ultimatecleaner.domain.repository

import com.mars.ultimatecleaner.domain.model.FileCategory
import com.mars.ultimatecleaner.ui.main.StorageInfo

interface StorageRepository {
    suspend fun getStorageInfo(): StorageInfo
    suspend fun getCategoryBreakdown(): Map<FileCategory, Long>
    suspend fun getCacheSize(): Long
    suspend fun getTempFilesSize(): Long
    suspend fun getAppDataSize(): Long
    suspend fun getDownloadSize(): Long
    suspend fun getMediaSize(): Long
    suspend fun getDocumentSize(): Long
    suspend fun refreshStorageInfo()
}