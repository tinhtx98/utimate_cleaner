package com.mars.ultimatecleaner.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mars.ultimatecleaner.data.database.converter.TypeConverters as Converter
import com.mars.ultimatecleaner.domain.model.OptimizationResult
import com.mars.ultimatecleaner.domain.model.OptimizationType

@Entity(
    tableName = "optimization_results",
    indices = [
        Index(value = ["optimizationType"]),
        Index(value = ["timestamp"]),
        Index(value = ["success"]),
        Index(value = ["spaceSaved"], name = "idx_space_saved"),
        Index(value = ["duration"], name = "idx_duration"),
        Index(value = ["filesProcessed"], name = "idx_files_processed")
    ]
)
@TypeConverters(Converter::class)
data class OptimizationResultEntity(
    @PrimaryKey
    val id: String,
    val optimizationType: String,
    val success: Boolean,
    val spaceSaved: Long,
    val filesProcessed: Int,
    val duration: Long,
    val timestamp: Long,
    val improvements: List<String>,
    val performanceGain: Float,
    val errorMessage: String? = null,
    val beforeMetrics: String? = null, // JSON string of metrics before optimization
    val afterMetrics: String? = null, // JSON string of metrics after optimization
    val deviceInfo: String? = null, // JSON string of device info during optimization
    val userId: String? = null, // For multi-user scenarios
    val version: Int = 1, // For data migration compatibility
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): OptimizationResult {
        return OptimizationResult(
            id = id,
            type = OptimizationType.valueOf(optimizationType),
            spaceSaved = spaceSaved,
            filesProcessed = filesProcessed,
            duration = duration,
            improvements = improvements,
            timestamp = timestamp
        )
    }
}

// Extension function to convert domain model to entity
fun OptimizationResult.toEntity(): OptimizationResultEntity {
    return OptimizationResultEntity(
        id = id,
        optimizationType = type.name,
        success = true, // Assuming success if no error
        spaceSaved = spaceSaved,
        filesProcessed = filesProcessed,
        duration = duration,
        timestamp = timestamp,
        improvements = improvements,
        performanceGain = 0f // Default value, can be enhanced
    )
}