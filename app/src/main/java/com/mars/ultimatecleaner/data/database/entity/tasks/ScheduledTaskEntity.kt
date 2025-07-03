package com.mars.ultimatecleaner.data.database.entity.tasks

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mars.ultimatecleaner.data.database.converter.TypeConverters as Converter
import com.mars.ultimatecleaner.domain.model.ScheduledTask

@Entity(
    tableName = "scheduled_tasks",
    indices = [
        Index(value = ["taskType"]),
        Index(value = ["isEnabled"]),
        Index(value = ["nextExecutionTime"], name = "idx_next_execution"),
        Index(value = ["priority"], name = "idx_priority")
    ]
)
@TypeConverters(Converter::class)
data class ScheduledTaskEntity(
    @PrimaryKey
    val id: String,
    val taskType: String,
    val taskName: String,
    val description: String,
    val isEnabled: Boolean,
    val priority: Int, // 1-10, 10 being highest
    val scheduleType: String, // DAILY, WEEKLY, MONTHLY, CUSTOM
    val cronExpression: String?,
    val intervalMinutes: Long?,
    val nextExecutionTime: Long,
    val lastExecutionTime: Long?,
    val executionCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val averageExecutionTime: Long,
    val maxExecutionTime: Long,
    val parameters: Map<String, String>,
    val dependencies: List<String>, // Task IDs that must complete first
    val retryCount: Int,
    val maxRetries: Int,
    val timeoutMinutes: Int,
    val isRecurring: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String = "system"
) {
    fun toDomainModel(): ScheduledTask {
        return ScheduledTask(
            id = id,
            taskType = taskType,
            taskName = taskName,
            description = description,
            isEnabled = isEnabled,
            priority = priority,
            scheduleType = scheduleType,
            cronExpression = cronExpression,
            intervalMinutes = intervalMinutes,
            nextExecutionTime = nextExecutionTime,
            lastExecutionTime = lastExecutionTime,
            executionCount = executionCount,
            successCount = successCount,
            failureCount = failureCount,
            averageExecutionTime = averageExecutionTime,
            maxExecutionTime = maxExecutionTime,
            parameters = parameters,
            dependencies = dependencies,
            retryCount = retryCount,
            maxRetries = maxRetries,
            timeoutMinutes = timeoutMinutes,
            isRecurring = isRecurring,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}