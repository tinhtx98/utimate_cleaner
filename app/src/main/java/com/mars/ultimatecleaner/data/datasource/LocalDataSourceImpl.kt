package com.mars.ultimatecleaner.data.datasource

import com.mars.ultimatecleaner.data.database.dao.*
import com.mars.ultimatecleaner.data.database.entity.*
import com.mars.ultimatecleaner.data.database.entity.usage.AppUsageStatsEntity
import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataSourceImpl @Inject constructor(
    private val cleaningHistoryDao: CleaningHistoryDao,
    private val fileMetadataDao: FileMetadataDao,
    private val userSettingsDao: UserSettingsDao,
    private val appUsageStatsDao: AppUsageStatsDao,
    private val duplicateGroupsDao: DuplicateGroupsDao,
    private val analysisResultsDao: AnalysisResultsDao,
    private val scheduledTasksDao: ScheduledTasksDao,
    private val optimizationResultsDao: OptimizationResultsDao
) : LocalDataSource {

    // Cleaning History Operations
    override fun getAllCleaningHistory(): Flow<List<CleaningHistoryItem>> {
        return cleaningHistoryDao.getAllCleaningHistory().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun insertCleaningHistory(cleaningHistory: CleaningHistoryItem) {
        cleaningHistoryDao.insertCleaningHistory(cleaningHistory.toEntity())
    }

    override fun getTotalSpaceSavedSince(startTime: Long): Flow<Long> {
        return cleaningHistoryDao.getTotalSpaceSavedSince(startTime).map { it ?: 0L }
    }

    // File Metadata Operations
    override suspend fun getFileMetadata(path: String): FileMetadata? {
        return fileMetadataDao.getFileMetadata(path)?.toDomainModel()
    }

    override suspend fun insertFileMetadata(fileMetadata: FileMetadata) {
        fileMetadataDao.insertFileMetadata(fileMetadata.toEntity())
    }

    override fun searchFiles(query: String): Flow<List<FileItem>> {
        return fileMetadataDao.searchFiles(query).map { entities ->
            entities.map { it.toFileItem() }
        }
    }

    // User Settings Operations
    override suspend fun getSetting(key: String): String? {
        return userSettingsDao.getSettingValue(key)
    }

    override suspend fun setSetting(key: String, value: String, dataType: String) {
        userSettingsDao.upsertSetting(key, value, dataType)
    }

    override fun getAllSettings(): Flow<Map<String, String>> {
        return userSettingsDao.getAllSettings().map { entities ->
            entities.associate { it.settingKey to it.settingValue }
        }
    }

    // Duplicate Groups Operations
    override fun getUnresolvedDuplicateGroups(): Flow<List<DuplicateGroup>> {
        return duplicateGroupsDao.getUnresolvedDuplicateGroups().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun insertDuplicateGroup(duplicateGroup: DuplicateGroup) {
        duplicateGroupsDao.insertDuplicateGroup(duplicateGroup.toEntity())
    }

    override suspend fun resolveDuplicateGroup(id: String, action: String, keepFilePath: String?) {
        duplicateGroupsDao.resolveDuplicateGroup(id, action, keepFilePath)
    }

    // Usage Stats Operations
    override suspend fun incrementFeatureUsage(featureName: String) {
        appUsageStatsDao.incrementUsageCount(featureName)
    }

    override suspend fun recordSessionTime(featureName: String, sessionTime: Long) {
        appUsageStatsDao.updateSessionStats(featureName, sessionTime)
    }

    override fun getUsageAnalytics(): Flow<UsageAnalytics> {
        return appUsageStatsDao.getAllUsageStats().map { entities ->
            entities.toUsageAnalytics()
        }
    }
}

// Extension functions for converting between entities and domain models
private fun CleaningHistoryEntity.toDomainModel(): CleaningHistoryItem {
    return CleaningHistoryItem(
        id = id,
        timestamp = timestamp,
        operation = operationType,
        spaceSaved = spaceSaved,
        filesDeleted = filesDeleted,
        duration = durationMs,
        categories = categoriesCleaned
    )
}

private fun CleaningHistoryItem.toEntity(): CleaningHistoryEntity {
    return CleaningHistoryEntity(
        id = id,
        timestamp = timestamp,
        operationType = operation,
        categoriesCleaned = categories,
        filesDeleted = filesDeleted,
        spaceSaved = spaceSaved,
        durationMs = duration,
        status = "SUCCESS"
    )
}

private fun FileMetadataEntity.toDomainModel(): FileMetadata {
    return FileMetadata(
        path = path,
        size = size,
        lastModified = lastModified,
        lastAccessed = lastAccessed,
        mimeType = mimeType,
        md5Hash = md5Hash,
        isMediaFile = isMediaFile
    )
}

private fun FileMetadata.toEntity(): FileMetadataEntity {
    return FileMetadataEntity(
        path = path,
        name = path.substringAfterLast('/'),
        size = size,
        lastModified = lastModified,
        lastAccessed = lastAccessed,
        mimeType = mimeType,
        md5Hash = md5Hash,
        isMediaFile = isMediaFile
    )
}

private fun FileMetadataEntity.toFileItem(): FileItem {
    return FileItem(
        path = path,
        name = name,
        size = size,
        lastModified = lastModified,
        mimeType = mimeType,
        isDirectory = isDirectory,
        thumbnailPath = thumbnailPath
    )
}

private fun DuplicateGroupsEntity.toDomainModel(): DuplicateGroup {
    return DuplicateGroup(
        id = id,
        files = filePaths.mapIndexed { index, path ->
            FileItem(
                path = path,
                name = path.substringAfterLast('/'),
                size = fileSizes.getOrNull(index) ?: 0L,
                lastModified = 0L,
                mimeType = fileType,
                isDirectory = false
            )
        },
        totalSize = totalSize,
        hash = fileHash,
        keepFile = keepFilePath
    )
}

private fun DuplicateGroup.toEntity(): DuplicateGroupsEntity {
    return DuplicateGroupsEntity(
        id = id,
        fileHash = hash,
        filePaths = files.map { it.path },
        fileSizes = files.map { it.size },
        totalSize = totalSize,
        fileCount = files.size,
        potentialSavings = totalSize - (files.maxOfOrNull { it.size } ?: 0L),
        keepFilePath = keepFile,
        fileType = files.firstOrNull()?.mimeType ?: "unknown",
        analysisDate = System.currentTimeMillis()
    )
}

private fun List<AppUsageStatsEntity>.toUsageAnalytics(): UsageAnalytics {
    return UsageAnalytics(
        totalCleaningOperations = this.find { it.featureName == "cleaning" }?.usageCount ?: 0,
        totalSpaceSaved = 0L, // This would come from cleaning history
        totalFilesDeleted = 0,
        averageCleaningFrequency = 0,
        mostUsedFeature = this.maxByOrNull { it.usageCount }?.featureName ?: "",
        lastCleaningDate = this.find { it.featureName == "cleaning" }?.lastUsed ?: 0L,
        appUsageTime = this.sumOf { it.totalTime },
        featuresUsed = this.associate { it.featureName to it.usageCount }
    )
}