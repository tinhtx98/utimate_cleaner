package com.mars.ultimatecleaner.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cleaning_history",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["operation_type"]),
        Index(value = ["space_saved"]),
        Index(value = ["status"])
    ]
)
data class CleaningHistoryEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "operation_type")
    val operationType: String,

    @ColumnInfo(name = "categories_cleaned")
    val categoriesCleaned: List<String>,

    @ColumnInfo(name = "files_deleted")
    val filesDeleted: Int,

    @ColumnInfo(name = "space_saved")
    val spaceSaved: Long,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "status")
    val status: String, // SUCCESS, FAILED, PARTIAL

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "details")
    val details: Map<String, Any>? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)