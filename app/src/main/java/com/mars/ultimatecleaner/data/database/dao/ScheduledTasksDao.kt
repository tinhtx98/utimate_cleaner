package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.ScheduledTasksEntity
import com.mars.ultimatecleaner.data.database.entity.tasks.TaskExecutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledTasksDao {

    // Scheduled Task Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTask(task: ScheduledTasksEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTasks(tasks: List<ScheduledTasksEntity>)

    @Update
    suspend fun updateScheduledTask(task: ScheduledTasksEntity)

    @Delete
    suspend fun deleteScheduledTask(task: ScheduledTasksEntity)

    @Query("DELETE FROM scheduled_tasks WHERE id = :taskId")
    suspend fun deleteScheduledTaskById(taskId: String)

    @Query("SELECT * FROM scheduled_tasks WHERE id = :taskId")
    suspend fun getScheduledTask(taskId: String): ScheduledTasksEntity?

    @Query("SELECT * FROM scheduled_tasks ORDER BY created_at DESC")
    fun getAllScheduledTasks(): Flow<List<ScheduledTasksEntity>>

    @Query("SELECT * FROM scheduled_tasks WHERE status = 'ENABLED' ORDER BY created_at DESC")
    fun getEnabledScheduledTasks(): Flow<List<ScheduledTasksEntity>>

    @Query("SELECT * FROM scheduled_tasks WHERE task_type = :taskType ORDER BY next_execution ASC")
    suspend fun getTasksByType(taskType: String): List<ScheduledTasksEntity>

    @Query("SELECT * FROM scheduled_tasks WHERE status = 'ENABLED' AND next_execution <= :currentTime ORDER BY created_at DESC")
    suspend fun getTasksDueForExecution(currentTime: Long): List<ScheduledTasksEntity>

    @Query("SELECT * FROM scheduled_tasks WHERE frequency != 'ONCE' AND status = 'ENABLED'")
    suspend fun getRecurringTasks(): List<ScheduledTasksEntity>

    @Query("UPDATE scheduled_tasks SET status = :status WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String)

    @Query("UPDATE scheduled_tasks SET next_execution = :nextTime WHERE id = :taskId")
    suspend fun updateNextExecutionTime(taskId: String, nextTime: Long)

    @Query("UPDATE scheduled_tasks SET last_execution = :lastTime, execution_count = execution_count + 1 WHERE id = :taskId")
    suspend fun updateLastExecutionTime(taskId: String, lastTime: Long)

    @Query("UPDATE scheduled_tasks SET success_count = success_count + 1 WHERE id = :taskId")
    suspend fun incrementSuccessCount(taskId: String)

    @Query("UPDATE scheduled_tasks SET failure_count = failure_count + 1 WHERE id = :taskId")
    suspend fun incrementFailureCount(taskId: String)

    // @Query("UPDATE scheduled_tasks SET retryCount = :retryCount WHERE id = :taskId")
    // suspend fun updateRetryCount(taskId: String, retryCount: Int)

    // Task Execution Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskExecution(execution: TaskExecutionEntity)

    @Update
    suspend fun updateTaskExecution(execution: TaskExecutionEntity)

    @Query("SELECT * FROM task_executions WHERE id = :executionId")
    suspend fun getTaskExecution(executionId: String): TaskExecutionEntity?

    @Query("SELECT * FROM task_executions WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getTaskExecutions(taskId: String): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions WHERE taskId = :taskId ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentTaskExecutions(taskId: String, limit: Int): List<TaskExecutionEntity>

    @Query("SELECT * FROM task_executions WHERE executionStatus = :status ORDER BY startTime DESC")
    suspend fun getExecutionsByStatus(status: String): List<TaskExecutionEntity>

    @Query("SELECT * FROM task_executions WHERE executionStatus = 'RUNNING' ORDER BY startTime ASC")
    suspend fun getRunningExecutions(): List<TaskExecutionEntity>

    @Query("SELECT * FROM task_executions WHERE executionStatus = 'FAILED' AND retryAttempt < :maxRetries ORDER BY startTime ASC")
    suspend fun getFailedExecutionsForRetry(maxRetries: Int): List<TaskExecutionEntity>

    @Query("SELECT * FROM task_executions WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    suspend fun getExecutionsInTimeRange(startTime: Long, endTime: Long): List<TaskExecutionEntity>

    @Query("UPDATE task_executions SET executionStatus = :status, endTime = :endTime, executionDuration = :duration WHERE id = :executionId")
    suspend fun updateExecutionStatus(executionId: String, status: String, endTime: Long, duration: Long)

    @Query("UPDATE task_executions SET progressPercentage = :progress, itemsProcessed = :processed WHERE id = :executionId")
    suspend fun updateExecutionProgress(executionId: String, progress: Int, processed: Int)

    @Query("DELETE FROM task_executions WHERE startTime < :beforeTime")
    suspend fun deleteOldExecutions(beforeTime: Long)

    // Analytics and Reporting
    // @Query("""
    //     SELECT taskType, 
    //            COUNT(*) as totalExecutions,
    //            SUM(CASE WHEN executionStatus = 'SUCCESS' THEN 1 ELSE 0 END) as successfulExecutions,
    //            SUM(CASE WHEN executionStatus = 'FAILED' THEN 1 ELSE 0 END) as failedExecutions,
    //            AVG(executionDuration) as avgExecutionTime,
    //            MAX(executionDuration) as maxExecutionTime,
    //            SUM(COALESCE(spaceSaved, 0)) as totalSpaceSaved,
    //            SUM(COALESCE(filesProcessed, 0)) as totalFilesProcessed
    //     FROM task_executions 
    //     WHERE startTime >= :fromTime 
    //     GROUP BY taskType
    //     ORDER BY totalExecutions DESC
    // """)
    // suspend fun getTaskTypeStatistics(fromTime: Long): List<TaskTypeStatistics>

    @Query("""
        SELECT DATE(datetime(startTime/1000, 'unixepoch')) as executionDate,
               COUNT(*) as totalExecutions,
               SUM(CASE WHEN executionStatus = 'SUCCESS' THEN 1 ELSE 0 END) as successfulExecutions,
               AVG(executionDuration) as avgDuration,
               SUM(COALESCE(spaceSaved, 0)) as totalSpaceSaved
        FROM task_executions 
        WHERE startTime >= :fromTime
        GROUP BY executionDate
        ORDER BY executionDate DESC
    """)
    suspend fun getDailyExecutionStatistics(fromTime: Long): List<DailyExecutionStatistics>

    // @Query("""
    //     SELECT t.id, t.taskName, t.taskType, t.successCount, t.failureCount, t.averageExecutionTime,
    //            (CAST(t.successCount AS FLOAT) / CAST(t.executionCount AS FLOAT)) * 100 as successRate
    //     FROM scheduled_tasks t
    //     WHERE t.executionCount > 0
    //     ORDER BY successRate DESC, t.successCount DESC
    // """)
    // suspend fun getTaskPerformanceReport(): List<TaskPerformanceReport>

    // @Query("SELECT COUNT(*) FROM scheduled_tasks WHERE isEnabled = 1")
    // suspend fun getEnabledTaskCount(): Int

    @Query("SELECT COUNT(*) FROM task_executions WHERE executionStatus = 'RUNNING'")
    suspend fun getRunningTaskCount(): Int

    @Query("SELECT SUM(COALESCE(spaceSaved, 0)) FROM task_executions WHERE startTime >= :fromTime AND executionStatus = 'SUCCESS'")
    suspend fun getTotalSpaceSaved(fromTime: Long): Long?

    @Query("SELECT SUM(COALESCE(filesProcessed, 0)) FROM task_executions WHERE startTime >= :fromTime AND executionStatus = 'SUCCESS'")
    suspend fun getTotalFilesProcessed(fromTime: Long): Int?

    @Transaction
    suspend fun cleanupOldTaskData(daysToKeep: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        deleteOldExecutions(cutoffTime)
    }

    @Transaction
    suspend fun completeTaskExecution(
        executionId: String,
        status: String,
        result: String?,
        spaceSaved: Long?,
        filesProcessed: Int?,
        errorMessage: String?
    ) {
        val endTime = System.currentTimeMillis()
        val execution = getTaskExecution(executionId)
        execution?.let {
            val duration = endTime - it.startTime
            updateExecutionStatus(executionId, status, endTime, duration)

            // Update task statistics
            if (status == "SUCCESS") {
                incrementSuccessCount(it.taskId)
            } else {
                incrementFailureCount(it.taskId)
            }
        }
    }
}

data class TaskTypeStatistics(
    val taskType: String,
    val totalExecutions: Int,
    val successfulExecutions: Int,
    val failedExecutions: Int,
    val avgExecutionTime: Long,
    val maxExecutionTime: Long,
    val totalSpaceSaved: Long,
    val totalFilesProcessed: Int
)

data class DailyExecutionStatistics(
    val executionDate: String,
    val totalExecutions: Int,
    val successfulExecutions: Int,
    val avgDuration: Long,
    val totalSpaceSaved: Long
)

data class TaskPerformanceReport(
    val id: String,
    val taskName: String,
    val taskType: String,
    val successCount: Int,
    val failureCount: Int,
    val averageExecutionTime: Long,
    val successRate: Float
)