package com.mars.ultimatecleaner.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_tasks",
    indices = [
        Index(value = ["task_type"]),
        Index(value = ["next_execution"]),
        Index(value = ["status"]),
        Index(value = ["frequency"])
    ]
)
data class ScheduledTasksEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "task_type")
    val taskType: String, // CLEAN_CACHE, DUPLICATE_SCAN, PHOTO_ANALYSIS

    @ColumnInfo(name = "task_name")
    val taskName: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "frequency")
    val frequency: String, // DAILY, WEEKLY, MONTHLY, CUSTOM

    @ColumnInfo(name = "frequency_value")
    val frequencyValue: Int = 1,

    @ColumnInfo(name = "next_execution")
    val nextExecution: Long,

    @ColumnInfo(name = "last_execution")
    val lastExecution: Long? = null,

    @ColumnInfo(name = "status")
    val status: String = "ACTIVE", // ACTIVE, PAUSED, DISABLED, COMPLETED

    @ColumnInfo(name = "parameters")
    val parameters: Map<String, Any>? = null,

    @ColumnInfo(name = "execution_count")
    val executionCount: Int = 0,

    @ColumnInfo(name = "success_count")
    val successCount: Int = 0,

    @ColumnInfo(name = "failure_count")
    val failureCount: Int = 0,

    @ColumnInfo(name = "last_result")
    val lastResult: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)