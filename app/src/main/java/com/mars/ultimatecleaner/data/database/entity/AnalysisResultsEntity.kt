package com.mars.ultimatecleaner.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "analysis_results",
    indices = [
        Index(value = ["file_path"]),
        Index(value = ["analysis_type"]),
        Index(value = ["analysis_date"]),
        Index(value = ["score"]),
        Index(value = ["status"])
    ]
)
data class AnalysisResultsEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "analysis_type")
    val analysisType: String, // BLUR_DETECTION, QUALITY_ANALYSIS, DUPLICATE_CHECK

    @ColumnInfo(name = "score")
    val score: Float,

    @ColumnInfo(name = "status")
    val status: String, // COMPLETED, FAILED, PENDING

    @ColumnInfo(name = "analysis_date")
    val analysisDate: Long,

    @ColumnInfo(name = "result_data")
    val resultData: Map<String, Any>,

    @ColumnInfo(name = "recommendations")
    val recommendations: List<String>? = null,

    @ColumnInfo(name = "confidence_level")
    val confidenceLevel: Float = 0f,

    @ColumnInfo(name = "processing_time")
    val processingTime: Long = 0L,

    @ColumnInfo(name = "algorithm_version")
    val algorithmVersion: String = "1.0",

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)