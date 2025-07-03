package com.mars.ultimatecleaner.data.datasource

import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {

    // Cleaning History Operations
    fun getAllCleaningHistory(): Flow<List<CleaningHistoryItemDomain>>
    suspend fun insertCleaningHistory(cleaningHistory: CleaningHistoryItemDomain)
    fun getTotalSpaceSavedSince(startTime: Long): Flow<Long>

    // File Metadata Operations
    suspend fun getFileMetadata(path: String): FileMetadata?
    suspend fun insertFileMetadata(fileMetadata: FileMetadata)
    fun searchFiles(query: String): Flow<List<FileItem>>

    // User Settings Operations
    suspend fun getSetting(key: String): String?
    suspend fun setSetting(key: String, value: String, dataType: String)
    fun getAllSettings(): Flow<Map<String, String>>

    // Duplicate Groups Operations
    fun getUnresolvedDuplicateGroups(): Flow<List<DuplicateGroup>>
    suspend fun insertDuplicateGroup(duplicateGroup: DuplicateGroup)
    suspend fun resolveDuplicateGroup(id: String, action: String, keepFilePath: String?)

    // Usage Stats Operations
    suspend fun incrementFeatureUsage(featureName: String)
    suspend fun recordSessionTime(featureName: String, sessionTime: Long)
    fun getUsageAnalytics(): Flow<UsageAnalyticsDomain>
}
