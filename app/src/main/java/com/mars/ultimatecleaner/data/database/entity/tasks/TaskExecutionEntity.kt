package com.mars.ultimatecleaner.data.database.entity.tasks

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mars.ultimatecleaner.data.database.converter.TypeConverters as Converter
import com.mars.ultimatecleaner.domain.model.TaskExecution

@Entity(
    tableName = "task_executions",
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["executionStatus"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"]),
        Index(value = ["executionDuration"], name = "idx_execution_duration")
    ],
    foreignKeys = [
        ForeignKey(
            entity = ScheduledTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(Converter::class)
data class TaskExecutionEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val executionStatus: String, // PENDING, RUNNING, SUCCESS, FAILED, CANCELLED, TIMEOUT
    val startTime: Long,
    val endTime: Long?,
    val executionDuration: Long?,
    val triggerReason: String, // SCHEDULED, MANUAL, DEPENDENCY, RETRY
    val result: String?,
    val errorMessage: String?,
    val stackTrace: String?,
    val memoryUsed: Long?,
    val cpuUsage: Float?,
    val progressPercentage: Int,
    val itemsProcessed: Int,
    val itemsTotal: Int,
    val spaceSaved: Long?,
    val filesProcessed: Int?,
    val metrics: Map<String, String>,
    val retryAttempt: Int,
    val workerName: String?,
    val deviceBatteryLevel: Int?,
    val deviceTemperature: Float?,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): TaskExecution {
        return TaskExecution(
            id = id,
            taskId = taskId,
            executionStatus = executionStatus,
            startTime = startTime,
            endTime = endTime,
            executionDuration = executionDuration,
            triggerReason = triggerReason,
            result = result,
            errorMessage = errorMessage,
            stackTrace = stackTrace,
            memoryUsed = memoryUsed,
            cpuUsage = cpuUsage,
            progressPercentage = progressPercentage,
            itemsProcessed = itemsProcessed,
            itemsTotal = itemsTotal,
            spaceSaved = spaceSaved,
            filesProcessed = filesProcessed,
            metrics = metrics,
            retryAttempt = retryAttempt,
            workerName = workerName,
            deviceBatteryLevel = deviceBatteryLevel,
            deviceTemperature = deviceTemperature
        )
    }
}