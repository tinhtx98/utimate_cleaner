package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.tasks.TaskExecutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskExecutionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskExecution(execution: TaskExecutionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskExecutions(executions: List<TaskExecutionEntity>)

    @Update
    suspend fun updateTaskExecution(execution: TaskExecutionEntity)

    @Delete
    suspend fun deleteTaskExecution(execution: TaskExecutionEntity)

    @Query("SELECT * FROM task_executions WHERE id = :id")
    suspend fun getTaskExecutionById(id: String): TaskExecutionEntity?

    @Query("SELECT * FROM task_executions WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getExecutionsForTask(taskId: String): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions WHERE executionStatus = :status ORDER BY startTime DESC")
    fun getExecutionsByStatus(status: String): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions ORDER BY startTime DESC")
    fun getAllTaskExecutions(): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentTaskExecutions(limit: Int): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    fun getExecutionsInTimeRange(startTime: Long, endTime: Long): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions WHERE executionStatus = 'FAILED' ORDER BY startTime DESC")
    fun getFailedExecutions(): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions WHERE executionStatus = 'SUCCESS' ORDER BY startTime DESC")
    fun getSuccessfulExecutions(): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions WHERE executionStatus = 'RUNNING' ORDER BY startTime DESC")
    fun getRunningExecutions(): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions WHERE triggerReason = :reason ORDER BY startTime DESC")
    fun getExecutionsByTrigger(reason: String): Flow<List<TaskExecutionEntity>>

    @Query("SELECT AVG(executionDuration) FROM task_executions WHERE taskId = :taskId AND executionStatus = 'SUCCESS'")
    suspend fun getAverageExecutionTime(taskId: String): Long?

    @Query("SELECT COUNT(*) FROM task_executions WHERE taskId = :taskId AND executionStatus = :status")
    suspend fun getExecutionCountByStatus(taskId: String, status: String): Int

    @Query("SELECT SUM(spaceSaved) FROM task_executions WHERE executionStatus = 'SUCCESS' AND startTime >= :fromTime")
    suspend fun getTotalSpaceSaved(fromTime: Long): Long?

    @Query("SELECT SUM(filesProcessed) FROM task_executions WHERE executionStatus = 'SUCCESS' AND startTime >= :fromTime")
    suspend fun getTotalFilesProcessed(fromTime: Long): Int

    @Query("SELECT * FROM task_executions WHERE retryAttempt > 0 ORDER BY startTime DESC")
    fun getRetriedExecutions(): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions WHERE executionDuration > :minDuration ORDER BY executionDuration DESC")
    fun getLongRunningExecutions(minDuration: Long): Flow<List<TaskExecutionEntity>>

    @Query("DELETE FROM task_executions WHERE startTime < :cutoffTime")
    suspend fun deleteOldExecutions(cutoffTime: Long)

    @Query("DELETE FROM task_executions WHERE taskId = :taskId")
    suspend fun deleteExecutionsForTask(taskId: String)

    @Query("DELETE FROM task_executions")
    suspend fun deleteAllTaskExecutions()

    @Query("""
        SELECT executionStatus, COUNT(*) as count, AVG(executionDuration) as avgDuration,
               SUM(spaceSaved) as totalSpaceSaved, SUM(filesProcessed) as totalFilesProcessed
        FROM task_executions 
        WHERE startTime >= :fromTime
        GROUP BY executionStatus
    """)
    suspend fun getExecutionStatistics(fromTime: Long): List<ExecutionStatistics>

    @Query("""
        SELECT DATE(startTime/1000, 'unixepoch') as day,
               COUNT(*) as executionCount,
               COUNT(CASE WHEN executionStatus = 'SUCCESS' THEN 1 END) as successCount,
               COUNT(CASE WHEN executionStatus = 'FAILED' THEN 1 END) as failureCount,
               AVG(executionDuration) as avgDuration
        FROM task_executions 
        WHERE startTime >= :fromTime
        GROUP BY day
        ORDER BY day DESC
    """)
    suspend fun getDailyExecutionTrends(fromTime: Long): List<DailyExecutionTrend>

    @Transaction
    suspend fun cleanupOldExecutions(daysToKeep: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        deleteOldExecutions(cutoffTime)
    }

    @Transaction
    suspend fun markExecutionCompleted(
        executionId: String,
        status: String,
        endTime: Long,
        result: String? = null,
        errorMessage: String? = null,
        spaceSaved: Long? = null,
        filesProcessed: Int? = null
    ) {
        val execution = getTaskExecutionById(executionId)
        execution?.let {
            val duration = endTime - it.startTime
            val updatedExecution = it.copy(
                executionStatus = status,
                endTime = endTime,
                executionDuration = duration,
                result = result,
                errorMessage = errorMessage,
                spaceSaved = spaceSaved,
                filesProcessed = filesProcessed,
                progressPercentage = if (status == "SUCCESS") 100 else it.progressPercentage
            )
            updateTaskExecution(updatedExecution)
        }
    }
}

data class ExecutionStatistics(
    val executionStatus: String,
    val count: Int,
    val avgDuration: Long?,
    val totalSpaceSaved: Long?,
    val totalFilesProcessed: Int?
)

data class DailyExecutionTrend(
    val day: String,
    val executionCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val avgDuration: Long?
)
