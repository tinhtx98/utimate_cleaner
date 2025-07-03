package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.FileAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileAnalysis(analysis: FileAnalysisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileAnalyses(analyses: List<FileAnalysisEntity>)

    @Query("SELECT * FROM file_analysis WHERE filePath = :filePath")
    suspend fun getFileAnalysis(filePath: String): FileAnalysisEntity?

    @Query("SELECT * FROM file_analysis ORDER BY analysisTimestamp DESC LIMIT :limit")
    suspend fun getRecentFileAnalyses(limit: Int): List<FileAnalysisEntity>

    @Query("SELECT * FROM file_analysis WHERE fileType = :type ORDER BY analysisTimestamp DESC")
    suspend fun getFileAnalysesByType(type: String): List<FileAnalysisEntity>

    @Query("SELECT * FROM file_analysis WHERE isDuplicate = 1")
    suspend fun getDuplicateFiles(): List<FileAnalysisEntity>

    @Query("SELECT * FROM file_analysis WHERE isBlurry = 1")
    suspend fun getBlurryFiles(): List<FileAnalysisEntity>

    @Query("SELECT * FROM file_analysis WHERE isLowQuality = 1")
    suspend fun getLowQualityFiles(): List<FileAnalysisEntity>

    @Query("SELECT * FROM file_analysis WHERE analysisTimestamp >= :timestamp")
    suspend fun getFileAnalysesSince(timestamp: Long): List<FileAnalysisEntity>

    @Query("DELETE FROM file_analysis WHERE filePath = :filePath")
    suspend fun deleteFileAnalysis(filePath: String)

    @Query("DELETE FROM file_analysis WHERE analysisTimestamp < :timestamp")
    suspend fun deleteOldAnalyses(timestamp: Long)

    @Query("DELETE FROM file_analysis")
    suspend fun clearAllAnalyses()

    @Query("SELECT COUNT(*) FROM file_analysis")
    suspend fun getAnalysisCount(): Int

    @Query("SELECT COUNT(*) FROM file_analysis WHERE isDuplicate = 1")
    suspend fun getDuplicateCount(): Int

    @Query("SELECT COUNT(*) FROM file_analysis WHERE isBlurry = 1")
    suspend fun getBlurryCount(): Int

    @Query("SELECT COUNT(*) FROM file_analysis WHERE isLowQuality = 1")
    suspend fun getLowQualityCount(): Int

    // Flow for reactive updates
    @Query("SELECT * FROM file_analysis ORDER BY analysisTimestamp DESC")
    fun getAllFileAnalysesFlow(): Flow<List<FileAnalysisEntity>>

    @Query("SELECT * FROM file_analysis WHERE isDuplicate = 1")
    fun getDuplicateFilesFlow(): Flow<List<FileAnalysisEntity>>
}
