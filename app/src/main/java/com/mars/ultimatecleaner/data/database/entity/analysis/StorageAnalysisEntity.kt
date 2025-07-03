package com.mars.ultimatecleaner.data.database.entity.analysis

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mars.ultimatecleaner.data.database.converter.TypeConverters as Converter
import com.mars.ultimatecleaner.domain.model.StorageAnalysis

@Entity(
    tableName = "storage_analysis",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["usagePercentage"], name = "idx_usage_percentage"),
        Index(value = ["cleanableSpace"], name = "idx_cleanable_space")
    ]
)
@TypeConverters(Converter::class)
data class StorageAnalysisEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val usagePercentage: Float,
    val cacheSize: Long,
    val junkFilesSize: Long,
    val cleanableSpace: Long,
    val categoryBreakdown: Map<String, Long>,
    val largeFilesCount: Int,
    val duplicateFilesCount: Int,
    val emptyFoldersCount: Int,
    val analysisVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): StorageAnalysis {
        return StorageAnalysis(
            totalSpace = totalSpace,
            usedSpace = usedSpace,
            freeSpace = freeSpace,
            usagePercentage = usagePercentage,
            cacheSize = cacheSize,
            junkFilesSize = junkFilesSize,
            cleanableSpace = cleanableSpace,
            categoryBreakdown = categoryBreakdown
        )
    }
}