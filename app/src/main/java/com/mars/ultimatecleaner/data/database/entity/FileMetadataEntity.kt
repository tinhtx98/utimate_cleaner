package com.mars.ultimatecleaner.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "file_metadata",
    indices = [
        Index(value = ["path"]),
        Index(value = ["size"]),
        Index(value = ["last_modified"]),
        Index(value = ["mime_type"]),
        Index(value = ["md5_hash"]),
        Index(value = ["analysis_status"])
    ]
)
data class FileMetadataEntity(
    @PrimaryKey
    val path: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "size")
    val size: Long,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long,

    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "md5_hash")
    val md5Hash: String? = null,

    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null,

    @ColumnInfo(name = "is_media_file")
    val isMediaFile: Boolean = false,

    @ColumnInfo(name = "is_directory")
    val isDirectory: Boolean = false,

    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,

    @ColumnInfo(name = "analysis_status")
    val analysisStatus: String = "PENDING", // PENDING, ANALYZED, FAILED

    @ColumnInfo(name = "analysis_result")
    val analysisResult: Map<String, Any>? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)