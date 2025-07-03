package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.analysis.StorageAnalysisEntity
import com.mars.ultimatecleaner.data.database.entity.analysis.PhotoAnalysisEntity
import com.mars.ultimatecleaner.data.database.entity.FileAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisResultsDao {

    // Storage Analysis Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorageAnalysis(analysis: StorageAnalysisEntity)

    @Query("SELECT * FROM storage_analysis ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestStorageAnalysis(): StorageAnalysisEntity?

    @Query("SELECT * FROM storage_analysis ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getStorageAnalysisHistory(limit: Int): List<StorageAnalysisEntity>

    @Query("SELECT * FROM storage_analysis WHERE timestamp >= :fromTime ORDER BY timestamp DESC")
    fun observeStorageAnalysis(fromTime: Long): Flow<List<StorageAnalysisEntity>>

    @Query("DELETE FROM storage_analysis WHERE timestamp < :beforeTime")
    suspend fun deleteOldStorageAnalysis(beforeTime: Long)

    // Photo Analysis Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoAnalysis(analysis: PhotoAnalysisEntity)

    @Query("SELECT * FROM photo_analysis ORDER BY analysisTimestamp DESC LIMIT 1")
    suspend fun getLatestPhotoAnalysis(): PhotoAnalysisEntity?

    @Query("SELECT * FROM photo_analysis ORDER BY analysisTimestamp DESC LIMIT :limit")
    suspend fun getPhotoAnalysisHistory(limit: Int): List<PhotoAnalysisEntity>

    @Query("SELECT SUM(potentialSpaceSavings) FROM photo_analysis WHERE analysisTimestamp >= :fromTime")
    suspend fun getTotalPhotoSpaceSavings(fromTime: Long): Long?

    @Query("DELETE FROM photo_analysis WHERE analysisTimestamp < :beforeTime")
    suspend fun deleteOldPhotoAnalysis(beforeTime: Long)

    // File Analysis Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileAnalysis(analysis: FileAnalysisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileAnalysisList(analysisList: List<FileAnalysisEntity>)

    @Update
    suspend fun updateFileAnalysis(analysis: FileAnalysisEntity)

    @Query("SELECT * FROM file_analysis WHERE filePath = :filePath")
    suspend fun getFileAnalysis(filePath: String): FileAnalysisEntity?

    // @Query("SELECT * FROM file_analysis WHERE fileCategory = :category ORDER BY analysisTimestamp DESC")
    // suspend fun getFileAnalysisByCategory(category: String): List<FileAnalysisEntity>

    @Query("SELECT * FROM file_analysis WHERE isDuplicate = 1 ORDER BY fileSize DESC")
    suspend fun getDuplicateFiles(): List<FileAnalysisEntity>

    // @Query("SELECT * FROM file_analysis WHERE isJunkFile = 1 ORDER BY fileSize DESC")
    // suspend fun getJunkFiles(): List<FileAnalysisEntity>

    // @Query("SELECT * FROM file_analysis WHERE isCacheFile = 1 ORDER BY fileSize DESC")
    // suspend fun getCacheFiles(): List<FileAnalysisEntity>

    // @Query("SELECT * FROM file_analysis WHERE isTempFile = 1 ORDER BY fileSize DESC")
    // suspend fun getTempFiles(): List<FileAnalysisEntity>

    @Query("SELECT * FROM file_analysis WHERE fileSize > :minSize ORDER BY fileSize DESC LIMIT :limit")
    suspend fun getLargeFiles(minSize: Long, limit: Int): List<FileAnalysisEntity>

    @Query("SELECT * FROM file_analysis WHERE qualityScore IS NOT NULL AND qualityScore < :maxScore ORDER BY qualityScore ASC")
    suspend fun getLowQualityFiles(maxScore: Float): List<FileAnalysisEntity>

    @Query("SELECT * FROM file_analysis WHERE blurScore IS NOT NULL AND blurScore > :minScore ORDER BY blurScore DESC")
    suspend fun getBlurryFiles(minScore: Float): List<FileAnalysisEntity>

    @Query("SELECT COUNT(*) FROM file_analysis WHERE contentHash = :hash")
    suspend fun getDuplicateCountByHash(hash: String): Int

    @Query("SELECT * FROM file_analysis WHERE contentHash = :hash ORDER BY analysisTimestamp ASC")
    suspend fun getFilesByHash(hash: String): List<FileAnalysisEntity>

    @Query("DELETE FROM file_analysis WHERE filePath = :filePath")
    suspend fun deleteFileAnalysis(filePath: String)

    @Query("DELETE FROM file_analysis WHERE analysisTimestamp < :beforeTime")
    suspend fun deleteOldFileAnalysis(beforeTime: Long)

    // Analytics and Summary Queries
    // @Query("""
    //     SELECT fileCategory, COUNT(*) as count, SUM(fileSize) as totalSize
    //     FROM file_analysis 
    //     WHERE analysisTimestamp >= :fromTime
    //     GROUP BY fileCategory
    //     ORDER BY totalSize DESC
    // """)
    // suspend fun getFileCategorySummary(fromTime: Long): List<FileCategorySummary>

    // @Query("""
    //     SELECT 
    //         SUM(CASE WHEN isDuplicate = 1 THEN fileSize ELSE 0 END) as duplicateSize,
    //         SUM(CASE WHEN isJunkFile = 1 THEN fileSize ELSE 0 END) as junkSize,
    //         SUM(CASE WHEN isCacheFile = 1 THEN fileSize ELSE 0 END) as cacheSize,
    //         SUM(CASE WHEN isTempFile = 1 THEN fileSize ELSE 0 END) as tempSize,
    //         COUNT(CASE WHEN isDuplicate = 1 THEN 1 END) as duplicateCount,
    //         COUNT(CASE WHEN isJunkFile = 1 THEN 1 END) as junkCount,
    //         COUNT(CASE WHEN isCacheFile = 1 THEN 1 END) as cacheCount,
    //         COUNT(CASE WHEN isTempFile = 1 THEN 1 END) as tempCount
    //     FROM file_analysis 
    //     WHERE analysisTimestamp >= :fromTime
    // """)
    // suspend fun getCleanupOpportunitySummary(fromTime: Long): CleanupOpportunitySummary

    @Query("""
        SELECT mimeType, COUNT(*) as count, SUM(fileSize) as totalSize, AVG(fileSize) as avgSize
        FROM file_analysis 
        WHERE analysisTimestamp >= :fromTime
        GROUP BY mimeType
        ORDER BY totalSize DESC
        LIMIT :limit
    """)
    suspend fun getMimeTypeSummary(fromTime: Long, limit: Int): List<MimeTypeSummary>

    @Transaction
    suspend fun cleanupOldAnalysisData(daysToKeep: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        deleteOldStorageAnalysis(cutoffTime)
        deleteOldPhotoAnalysis(cutoffTime)
        deleteOldFileAnalysis(cutoffTime)
    }
}

data class FileCategorySummary(
    val fileCategory: String,
    val count: Int,
    val totalSize: Long
)

data class CleanupOpportunitySummary(
    val duplicateSize: Long,
    val junkSize: Long,
    val cacheSize: Long,
    val tempSize: Long,
    val duplicateCount: Int,
    val junkCount: Int,
    val cacheCount: Int,
    val tempCount: Int
)

data class MimeTypeSummary(
    val mimeType: String,
    val count: Int,
    val totalSize: Long,
    val avgSize: Long
)