package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.OptimizationResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OptimizationResultsDao {

    // Basic CRUD Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptimizationResult(result: OptimizationResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptimizationResults(results: List<OptimizationResultEntity>)

    @Update
    suspend fun updateOptimizationResult(result: OptimizationResultEntity)

    @Delete
    suspend fun deleteOptimizationResult(result: OptimizationResultEntity)

    @Query("DELETE FROM optimization_results WHERE id = :resultId")
    suspend fun deleteOptimizationResultById(resultId: String)

    // Query Operations
    @Query("SELECT * FROM optimization_results WHERE id = :resultId")
    suspend fun getOptimizationResult(resultId: String): OptimizationResultEntity?

    @Query("SELECT * FROM optimization_results ORDER BY timestamp DESC")
    fun getAllOptimizationResults(): Flow<List<OptimizationResultEntity>>

    @Query("SELECT * FROM optimization_results ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentOptimizationResults(limit: Int): List<OptimizationResultEntity>

    @Query("SELECT * FROM optimization_results WHERE optimizationType = :type ORDER BY timestamp DESC")
    suspend fun getOptimizationResultsByType(type: String): List<OptimizationResultEntity>

    @Query("SELECT * FROM optimization_results WHERE optimizationType = :type ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentOptimizationResultsByType(type: String, limit: Int): List<OptimizationResultEntity>

    @Query("SELECT * FROM optimization_results WHERE success = 1 ORDER BY timestamp DESC")
    suspend fun getSuccessfulOptimizationResults(): List<OptimizationResultEntity>

    @Query("SELECT * FROM optimization_results WHERE success = 0 ORDER BY timestamp DESC")
    suspend fun getFailedOptimizationResults(): List<OptimizationResultEntity>

    @Query("SELECT * FROM optimization_results WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getOptimizationResultsInTimeRange(startTime: Long, endTime: Long): List<OptimizationResultEntity>

    @Query("SELECT * FROM optimization_results WHERE spaceSaved > :minSpaceSaved ORDER BY spaceSaved DESC")
    suspend fun getOptimizationResultsBySpaceSaved(minSpaceSaved: Long): List<OptimizationResultEntity>

    @Query("SELECT * FROM optimization_results WHERE filesProcessed > :minFiles ORDER BY filesProcessed DESC")
    suspend fun getOptimizationResultsByFilesProcessed(minFiles: Int): List<OptimizationResultEntity>

    // Aggregation and Statistics
    @Query("SELECT COUNT(*) FROM optimization_results")
    suspend fun getTotalOptimizationCount(): Int

    @Query("SELECT COUNT(*) FROM optimization_results WHERE success = 1")
    suspend fun getSuccessfulOptimizationCount(): Int

    @Query("SELECT COUNT(*) FROM optimization_results WHERE success = 0")
    suspend fun getFailedOptimizationCount(): Int

    @Query("SELECT COUNT(*) FROM optimization_results WHERE optimizationType = :type")
    suspend fun getOptimizationCountByType(type: String): Int

    @Query("SELECT SUM(spaceSaved) FROM optimization_results WHERE success = 1")
    suspend fun getTotalSpaceSaved(): Long?

    @Query("SELECT SUM(spaceSaved) FROM optimization_results WHERE success = 1 AND optimizationType = :type")
    suspend fun getTotalSpaceSavedByType(type: String): Long?

    @Query("SELECT SUM(spaceSaved) FROM optimization_results WHERE success = 1 AND timestamp >= :fromTime")
    suspend fun getTotalSpaceSavedSince(fromTime: Long): Long?

    @Query("SELECT SUM(filesProcessed) FROM optimization_results WHERE success = 1")
    suspend fun getTotalFilesProcessed(): Int?

    @Query("SELECT SUM(filesProcessed) FROM optimization_results WHERE success = 1 AND optimizationType = :type")
    suspend fun getTotalFilesProcessedByType(type: String): Int?

    @Query("SELECT SUM(filesProcessed) FROM optimization_results WHERE success = 1 AND timestamp >= :fromTime")
    suspend fun getTotalFilesProcessedSince(fromTime: Long): Int?

    @Query("SELECT AVG(duration) FROM optimization_results WHERE success = 1")
    suspend fun getAverageOptimizationDuration(): Long?

    @Query("SELECT AVG(duration) FROM optimization_results WHERE success = 1 AND optimizationType = :type")
    suspend fun getAverageOptimizationDurationByType(type: String): Long?

    @Query("SELECT MAX(spaceSaved) FROM optimization_results WHERE success = 1")
    suspend fun getMaxSpaceSaved(): Long?

    @Query("SELECT MAX(filesProcessed) FROM optimization_results WHERE success = 1")
    suspend fun getMaxFilesProcessed(): Int?

    @Query("SELECT MIN(duration) FROM optimization_results WHERE success = 1")
    suspend fun getMinOptimizationDuration(): Long?

    @Query("SELECT MAX(duration) FROM optimization_results WHERE success = 1")
    suspend fun getMaxOptimizationDuration(): Long?

    // Time-based Queries
    @Query("SELECT * FROM optimization_results WHERE timestamp >= :fromTime ORDER BY timestamp DESC")
    fun getOptimizationResultsSince(fromTime: Long): Flow<List<OptimizationResultEntity>>

    @Query("SELECT * FROM optimization_results WHERE DATE(datetime(timestamp/1000, 'unixepoch')) = DATE('now') ORDER BY timestamp DESC")
    suspend fun getTodayOptimizationResults(): List<OptimizationResultEntity>

    @Query("SELECT * FROM optimization_results WHERE timestamp >= :weekStart ORDER BY timestamp DESC")
    suspend fun getThisWeekOptimizationResults(weekStart: Long): List<OptimizationResultEntity>

    @Query("SELECT * FROM optimization_results WHERE timestamp >= :monthStart ORDER BY timestamp DESC")
    suspend fun getThisMonthOptimizationResults(monthStart: Long): List<OptimizationResultEntity>

    // Advanced Analytics
    @Query("""
        SELECT optimizationType, 
               COUNT(*) as totalCount,
               SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successCount,
               SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) as failureCount,
               SUM(CASE WHEN success = 1 THEN spaceSaved ELSE 0 END) as totalSpaceSaved,
               SUM(CASE WHEN success = 1 THEN filesProcessed ELSE 0 END) as totalFilesProcessed,
               AVG(CASE WHEN success = 1 THEN duration ELSE NULL END) as avgDuration,
               MAX(CASE WHEN success = 1 THEN spaceSaved ELSE 0 END) as maxSpaceSaved,
               (SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as successRate
        FROM optimization_results 
        WHERE timestamp >= :fromTime
        GROUP BY optimizationType
        ORDER BY totalSpaceSaved DESC
    """)
    suspend fun getOptimizationStatsByType(fromTime: Long): List<OptimizationTypeStats>

    @Query("""
        SELECT DATE(datetime(timestamp/1000, 'unixepoch')) as date,
               COUNT(*) as totalOptimizations,
               SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successfulOptimizations,
               SUM(CASE WHEN success = 1 THEN spaceSaved ELSE 0 END) as totalSpaceSaved,
               SUM(CASE WHEN success = 1 THEN filesProcessed ELSE 0 END) as totalFilesProcessed,
               AVG(CASE WHEN success = 1 THEN duration ELSE NULL END) as avgDuration
        FROM optimization_results 
        WHERE timestamp >= :fromTime
        GROUP BY date
        ORDER BY date DESC
    """)
    suspend fun getDailyOptimizationStats(fromTime: Long): List<DailyOptimizationStats>

    @Query("""
        SELECT strftime('%H', datetime(timestamp/1000, 'unixepoch')) as hour,
               COUNT(*) as optimizationCount,
               AVG(CASE WHEN success = 1 THEN spaceSaved ELSE 0 END) as avgSpaceSaved
        FROM optimization_results 
        WHERE timestamp >= :fromTime
        GROUP BY hour
        ORDER BY hour
    """)
    suspend fun getHourlyOptimizationDistribution(fromTime: Long): List<HourlyOptimizationStats>

    @Query("""
        SELECT *
        FROM optimization_results 
        WHERE success = 1 
        ORDER BY spaceSaved DESC 
        LIMIT :limit
    """)
    suspend fun getTopOptimizationsBySpaceSaved(limit: Int): List<OptimizationResultEntity>

    @Query("""
        SELECT *
        FROM optimization_results 
        WHERE success = 1 
        ORDER BY filesProcessed DESC 
        LIMIT :limit
    """)
    suspend fun getTopOptimizationsByFilesProcessed(limit: Int): List<OptimizationResultEntity>

    @Query("""
        SELECT *
        FROM optimization_results 
        WHERE success = 1 
        ORDER BY duration ASC 
        LIMIT :limit
    """)
    suspend fun getFastestOptimizations(limit: Int): List<OptimizationResultEntity>

    // Cleanup Operations
    @Query("DELETE FROM optimization_results WHERE timestamp < :beforeTime")
    suspend fun deleteOptimizationResultsOlderThan(beforeTime: Long)

    @Query("DELETE FROM optimization_results WHERE success = 0 AND timestamp < :beforeTime")
    suspend fun deleteFailedOptimizationResultsOlderThan(beforeTime: Long)

    @Query("DELETE FROM optimization_results WHERE optimizationType = :type AND timestamp < :beforeTime")
    suspend fun deleteOptimizationResultsByTypeOlderThan(type: String, beforeTime: Long)

    @Transaction
    suspend fun cleanupOldOptimizationResults(daysToKeep: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        deleteOptimizationResultsOlderThan(cutoffTime)
    }

    @Transaction
    suspend fun cleanupFailedOptimizationResults(daysToKeep: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        deleteFailedOptimizationResultsOlderThan(cutoffTime)
    }

    // Batch Operations
    @Transaction
    suspend fun insertOptimizationResultBatch(results: List<OptimizationResultEntity>) {
        results.chunked(100).forEach { batch ->
            insertOptimizationResults(batch)
        }
    }

    @Transaction
    suspend fun getOptimizationSummary(): OptimizationSummary {
        val totalCount = getTotalOptimizationCount()
        val successCount = getSuccessfulOptimizationCount()
        val failureCount = getFailedOptimizationCount()
        val totalSpaceSaved = getTotalSpaceSaved() ?: 0L
        val totalFilesProcessed = getTotalFilesProcessed() ?: 0
        val avgDuration = getAverageOptimizationDuration() ?: 0L
        val maxSpaceSaved = getMaxSpaceSaved() ?: 0L
        val maxFilesProcessed = getMaxFilesProcessed() ?: 0

        return OptimizationSummary(
            totalOptimizations = totalCount,
            successfulOptimizations = successCount,
            failedOptimizations = failureCount,
            successRate = if (totalCount > 0) (successCount.toFloat() / totalCount.toFloat()) * 100 else 0f,
            totalSpaceSaved = totalSpaceSaved,
            totalFilesProcessed = totalFilesProcessed,
            averageDuration = avgDuration,
            maxSpaceSaved = maxSpaceSaved,
            maxFilesProcessed = maxFilesProcessed
        )
    }

    // Search and Filter Operations
    @Query("""
        SELECT * FROM optimization_results 
        WHERE (optimizationType LIKE '%' || :searchTerm || '%' OR 
               improvements LIKE '%' || :searchTerm || '%' OR
               errorMessage LIKE '%' || :searchTerm || '%')
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun searchOptimizationResults(searchTerm: String, limit: Int): List<OptimizationResultEntity>

    @Query("""
        SELECT * FROM optimization_results 
        WHERE success = :success 
        AND optimizationType = :type 
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp DESC
    """)
    suspend fun getFilteredOptimizationResults(
        success: Boolean,
        type: String,
        startTime: Long,
        endTime: Long
    ): List<OptimizationResultEntity>

    // Performance Monitoring
    @Query("SELECT COUNT(*) FROM optimization_results WHERE duration > :thresholdMs")
    suspend fun getSlowOptimizationCount(thresholdMs: Long): Int

    @Query("SELECT * FROM optimization_results WHERE duration > :thresholdMs ORDER BY duration DESC")
    suspend fun getSlowOptimizations(thresholdMs: Long): List<OptimizationResultEntity>

    @Query("""
        SELECT AVG(duration) as avgDuration,
               COUNT(*) as count,
               optimizationType
        FROM optimization_results 
        WHERE success = 1 AND timestamp >= :fromTime
        GROUP BY optimizationType
        ORDER BY avgDuration DESC
    """)
    suspend fun getPerformanceMetricsByType(fromTime: Long): List<OptimizationPerformanceMetrics>
}

// Data classes for complex query results
data class OptimizationTypeStats(
    val optimizationType: String,
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val totalSpaceSaved: Long,
    val totalFilesProcessed: Int,
    val avgDuration: Long,
    val maxSpaceSaved: Long,
    val successRate: Float
)

data class DailyOptimizationStats(
    val date: String,
    val totalOptimizations: Int,
    val successfulOptimizations: Int,
    val totalSpaceSaved: Long,
    val totalFilesProcessed: Int,
    val avgDuration: Long
)

data class HourlyOptimizationStats(
    val hour: String,
    val optimizationCount: Int,
    val avgSpaceSaved: Long
)

data class OptimizationSummary(
    val totalOptimizations: Int,
    val successfulOptimizations: Int,
    val failedOptimizations: Int,
    val successRate: Float,
    val totalSpaceSaved: Long,
    val totalFilesProcessed: Int,
    val averageDuration: Long,
    val maxSpaceSaved: Long,
    val maxFilesProcessed: Int
)

data class OptimizationPerformanceMetrics(
    val avgDuration: Long,
    val count: Int,
    val optimizationType: String
)