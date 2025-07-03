package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.analysis.StorageAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorageAnalysis(analysis: StorageAnalysisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorageAnalysisList(analysisList: List<StorageAnalysisEntity>)

    @Update
    suspend fun updateStorageAnalysis(analysis: StorageAnalysisEntity)

    @Delete
    suspend fun deleteStorageAnalysis(analysis: StorageAnalysisEntity)

    @Query("SELECT * FROM storage_analysis WHERE id = :id")
    suspend fun getStorageAnalysisById(id: String): StorageAnalysisEntity?

    @Query("SELECT * FROM storage_analysis ORDER BY timestamp DESC")
    fun getAllStorageAnalysis(): Flow<List<StorageAnalysisEntity>>

    @Query("SELECT * FROM storage_analysis ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestStorageAnalysis(): StorageAnalysisEntity?

    @Query("SELECT * FROM storage_analysis WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getStorageAnalysisInRange(startTime: Long, endTime: Long): Flow<List<StorageAnalysisEntity>>

    @Query("SELECT * FROM storage_analysis WHERE usagePercentage >= :minPercentage ORDER BY timestamp DESC")
    fun getStorageAnalysisAboveUsage(minPercentage: Float): Flow<List<StorageAnalysisEntity>>

    @Query("SELECT * FROM storage_analysis WHERE cleanableSpace >= :minCleanable ORDER BY cleanableSpace DESC")
    fun getStorageAnalysisWithCleanableSpace(minCleanable: Long): Flow<List<StorageAnalysisEntity>>

    @Query("SELECT AVG(usagePercentage) FROM storage_analysis WHERE timestamp >= :fromTime")
    suspend fun getAverageUsagePercentage(fromTime: Long): Float?

    @Query("SELECT MAX(cleanableSpace) FROM storage_analysis WHERE timestamp >= :fromTime")
    suspend fun getMaxCleanableSpace(fromTime: Long): Long?

    @Query("SELECT COUNT(*) FROM storage_analysis WHERE timestamp >= :fromTime")
    suspend fun getAnalysisCount(fromTime: Long): Int

    @Query("DELETE FROM storage_analysis WHERE timestamp < :cutoffTime")
    suspend fun deleteOldAnalysis(cutoffTime: Long)

    @Query("DELETE FROM storage_analysis")
    suspend fun deleteAllStorageAnalysis()

    @Query("""
        SELECT timestamp, usagePercentage, cleanableSpace, totalSpace, usedSpace 
        FROM storage_analysis 
        WHERE timestamp >= :fromTime 
        ORDER BY timestamp ASC
    """)
    suspend fun getStorageTrends(fromTime: Long): List<StorageTrend>

    @Transaction
    suspend fun cleanupOldAnalysis(daysToKeep: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        deleteOldAnalysis(cutoffTime)
    }
}

data class StorageTrend(
    val timestamp: Long,
    val usagePercentage: Float,
    val cleanableSpace: Long,
    val totalSpace: Long,
    val usedSpace: Long
)
