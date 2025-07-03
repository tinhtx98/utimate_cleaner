package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.analysis.PhotoAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoAnalysis(analysis: PhotoAnalysisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoAnalysisList(analysisList: List<PhotoAnalysisEntity>)

    @Update
    suspend fun updatePhotoAnalysis(analysis: PhotoAnalysisEntity)

    @Delete
    suspend fun deletePhotoAnalysis(analysis: PhotoAnalysisEntity)

    @Query("SELECT * FROM photo_analysis WHERE id = :id")
    suspend fun getPhotoAnalysisById(id: String): PhotoAnalysisEntity?

    @Query("SELECT * FROM photo_analysis ORDER BY analysisTimestamp DESC")
    fun getAllPhotoAnalysis(): Flow<List<PhotoAnalysisEntity>>

    @Query("SELECT * FROM photo_analysis ORDER BY analysisTimestamp DESC LIMIT 1")
    suspend fun getLatestPhotoAnalysis(): PhotoAnalysisEntity?

    @Query("SELECT * FROM photo_analysis WHERE analysisTimestamp >= :startTime AND analysisTimestamp <= :endTime ORDER BY analysisTimestamp DESC")
    fun getPhotoAnalysisInRange(startTime: Long, endTime: Long): Flow<List<PhotoAnalysisEntity>>

    @Query("SELECT * FROM photo_analysis WHERE blurryPhotosCount > 0 ORDER BY analysisTimestamp DESC")
    fun getPhotoAnalysisWithBlurryPhotos(): Flow<List<PhotoAnalysisEntity>>

    @Query("SELECT * FROM photo_analysis WHERE lowQualityPhotosCount > 0 ORDER BY analysisTimestamp DESC")
    fun getPhotoAnalysisWithLowQualityPhotos(): Flow<List<PhotoAnalysisEntity>>

    @Query("SELECT * FROM photo_analysis WHERE similarPhotoGroupsCount > 0 ORDER BY analysisTimestamp DESC")
    fun getPhotoAnalysisWithSimilarGroups(): Flow<List<PhotoAnalysisEntity>>

    @Query("SELECT * FROM photo_analysis WHERE potentialSpaceSavings >= :minSavings ORDER BY potentialSpaceSavings DESC")
    fun getPhotoAnalysisWithSpaceSavings(minSavings: Long): Flow<List<PhotoAnalysisEntity>>

    @Query("SELECT SUM(blurryPhotosCount) FROM photo_analysis WHERE analysisTimestamp >= :fromTime")
    suspend fun getTotalBlurryPhotosCount(fromTime: Long): Int

    @Query("SELECT SUM(lowQualityPhotosCount) FROM photo_analysis WHERE analysisTimestamp >= :fromTime")
    suspend fun getTotalLowQualityPhotosCount(fromTime: Long): Int

    @Query("SELECT SUM(potentialSpaceSavings) FROM photo_analysis WHERE analysisTimestamp >= :fromTime")
    suspend fun getTotalPotentialSpaceSavings(fromTime: Long): Long

    @Query("SELECT AVG(totalPhotos) FROM photo_analysis WHERE analysisTimestamp >= :fromTime")
    suspend fun getAveragePhotoCount(fromTime: Long): Float?

    @Query("DELETE FROM photo_analysis WHERE analysisTimestamp < :cutoffTime")
    suspend fun deleteOldAnalysis(cutoffTime: Long)

    @Query("DELETE FROM photo_analysis")
    suspend fun deleteAllPhotoAnalysis()

    @Query("""
        SELECT analysisTimestamp, totalPhotos, blurryPhotosCount, lowQualityPhotosCount, 
               similarPhotoGroupsCount, potentialSpaceSavings 
        FROM photo_analysis 
        WHERE analysisTimestamp >= :fromTime 
        ORDER BY analysisTimestamp ASC
    """)
    suspend fun getPhotoAnalysisTrends(fromTime: Long): List<PhotoAnalysisTrend>

    @Transaction
    suspend fun cleanupOldAnalysis(daysToKeep: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        deleteOldAnalysis(cutoffTime)
    }
}

data class PhotoAnalysisTrend(
    val analysisTimestamp: Long,
    val totalPhotos: Int,
    val blurryPhotosCount: Int,
    val lowQualityPhotosCount: Int,
    val similarPhotoGroupsCount: Int,
    val potentialSpaceSavings: Long
)
