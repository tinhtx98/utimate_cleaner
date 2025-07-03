package com.mars.ultimatecleaner.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "optimization_results",
    indices = [
        Index(value = ["operation_type"]),
        Index(value = ["timestamp"]),
        Index(value = ["space_saved"]),
        Index(value = ["status"])
    ]
)
data class OptimizationResultLegacyEntity(
    @PrimaryKey
    val id: String,
    val operation_type: String,
    val timestamp: Long,
    val files_processed: Int,
    val files_optimized: Int,
    val space_saved: Long,
    val original_size: Long,
    val optimized_size: Long,
    val compression_ratio: Float = 0.0f,
    val processing_time: Long,
    val status: String,
    val error_count: Int = 0,
    val error_details: String? = null,
    val settings_used: String? = null,
    val performance_metrics: String? = null,
    val created_at: Long = System.currentTimeMillis()
)
