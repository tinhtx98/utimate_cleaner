package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.OptimizationResultLegacyEntity
import com.mars.ultimatecleaner.data.database.entity.OptimizationScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OptimizationDao {

    // Optimization Results - Enhanced with the new functionality
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptimizationResult(result: OptimizationResultLegacyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptimizationResults(results: List<OptimizationResultLegacyEntity>)

    @Query("SELECT * FROM optimization_results ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getOptimizationHistory(limit: Int): List<OptimizationResultLegacyEntity>

    @Query("SELECT * FROM optimization_results ORDER BY timestamp DESC")
    suspend fun getAllOptimizationResults(): List<OptimizationResultLegacyEntity>

    @Query("DELETE FROM optimization_results")
    suspend fun clearOptimizationHistory()

    @Query("DELETE FROM optimization_results WHERE timestamp < :beforeTime")
    suspend fun deleteOldOptimizationResults(beforeTime: Long)

    // Optimization Schedules
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptimizationSchedule(schedule: OptimizationScheduleEntity)

    @Query("SELECT * FROM optimization_schedules WHERE isEnabled = 1")
    suspend fun getOptimizationSchedules(): List<OptimizationScheduleEntity>

    @Query("SELECT * FROM optimization_schedules")
    suspend fun getAllOptimizationSchedules(): List<OptimizationScheduleEntity>

    @Query("DELETE FROM optimization_schedules WHERE id = :scheduleId")
    suspend fun deleteOptimizationSchedule(scheduleId: String)

    @Update
    suspend fun updateOptimizationSchedule(schedule: OptimizationScheduleEntity)

    @Query("UPDATE optimization_schedules SET isEnabled = :enabled WHERE id = :scheduleId")
    suspend fun updateScheduleEnabledStatus(scheduleId: String, enabled: Boolean)

    @Query("UPDATE optimization_schedules SET lastRun = :lastRun, nextRun = :nextRun WHERE id = :scheduleId")
    suspend fun updateScheduleRunTimes(scheduleId: String, lastRun: Long, nextRun: Long)

    // Analytics and Statistics
    @Query("""
        SELECT COUNT(*) as totalOptimizations,
               SUM(space_saved) as totalSpaceSaved,
               SUM(files_processed) as totalFilesProcessed,
               AVG(processing_time) as averageDuration,
               MAX(timestamp) as lastOptimization
        FROM optimization_results
        WHERE status = 'SUCCESS'
    """)
    suspend fun getOptimizationStatistics(): OptimizationStatistics

    @Query("""
        SELECT operation_type, COUNT(*) as count
        FROM optimization_results
        WHERE timestamp >= :fromTime
        GROUP BY operation_type
        ORDER BY count DESC
    """)
    suspend fun getOptimizationsByType(fromTime: Long): List<OptimizationTypeCount>

    @Query("""
        SELECT * FROM optimization_results 
        WHERE status = 'SUCCESS' 
        ORDER BY space_saved DESC 
        LIMIT :limit
    """)
    suspend fun getTopOptimizationsBySpaceSaved(limit: Int): List<OptimizationResultLegacyEntity>

    @Query("""
        SELECT DATE(datetime(timestamp/1000, 'unixepoch')) as date,
               COUNT(*) as optimizations,
               SUM(space_saved) as spaceSaved
        FROM optimization_results 
        WHERE status = 'SUCCESS' AND timestamp >= :fromTime
        GROUP BY date
        ORDER BY date DESC
    """)
    suspend fun getDailyOptimizationTrends(fromTime: Long): List<DailyOptimizationTrend>

    // Utility methods
    @Transaction
    suspend fun recordOptimizationResult(
        result: OptimizationResultLegacyEntity,
        updateSchedule: Boolean = false,
        scheduleId: String? = null
    ) {
        insertOptimizationResult(result)

        if (updateSchedule && scheduleId != null) {
            val currentTime = System.currentTimeMillis()
            // Update schedule's last run time
            updateScheduleRunTimes(scheduleId, currentTime, currentTime + 24 * 60 * 60 * 1000) // Next day
        }
    }

    @Transaction
    suspend fun cleanupOldData(daysToKeep: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        deleteOldOptimizationResults(cutoffTime)
    }
}

// Data classes for complex query results
data class OptimizationStatistics(
    val totalOptimizations: Int,
    val totalSpaceSaved: Long,
    val totalFilesProcessed: Int,
    val averageDuration: Long,
    val lastOptimization: Long
)

data class OptimizationTypeCount(
    val operation_type: String,
    val count: Int
)

data class DailyOptimizationTrend(
    val date: String,
    val optimizations: Int,
    val spaceSaved: Long
)