package com.mars.ultimatecleaner.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "duplicate_groups",
    indices = [
        Index(value = ["file_hash"]),
        Index(value = ["total_size"]),
        Index(value = ["file_count"]),
        Index(value = ["analysis_date"])
    ]
)
data class DuplicateGroupsEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "file_hash")
    val fileHash: String,

    @ColumnInfo(name = "file_paths")
    val filePaths: List<String>,

    @ColumnInfo(name = "file_sizes")
    val fileSizes: List<Long>,

    @ColumnInfo(name = "total_size")
    val totalSize: Long,

    @ColumnInfo(name = "file_count")
    val fileCount: Int,

    @ColumnInfo(name = "potential_savings")
    val potentialSavings: Long,

    @ColumnInfo(name = "keep_file_path")
    val keepFilePath: String? = null,

    @ColumnInfo(name = "file_type")
    val fileType: String,

    @ColumnInfo(name = "analysis_date")
    val analysisDate: Long,

    @ColumnInfo(name = "is_resolved")
    val isResolved: Boolean = false,

    @ColumnInfo(name = "resolved_date")
    val resolvedDate: Long? = null,

    @ColumnInfo(name = "resolution_action")
    val resolutionAction: String? = null, // DELETED, IGNORED, MERGED

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)