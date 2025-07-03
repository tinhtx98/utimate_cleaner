package com.mars.ultimatecleaner.data.database.entity.analysis

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mars.ultimatecleaner.data.database.converter.TypeConverters as Converter
import com.mars.ultimatecleaner.domain.model.PhotoAnalysis

@Entity(
    tableName = "photo_analysis",
    indices = [
        Index(value = ["analysisTimestamp"]),
        Index(value = ["totalPhotos"], name = "idx_total_photos"),
        Index(value = ["blurryPhotosCount"], name = "idx_blurry_count")
    ]
)
@TypeConverters(Converter::class)
data class PhotoAnalysisEntity(
    @PrimaryKey
    val id: String,
    val analysisTimestamp: Long,
    val totalPhotos: Int,
    val blurryPhotosCount: Int,
    val lowQualityPhotosCount: Int,
    val similarPhotoGroupsCount: Int,
    val blurryPhotosPaths: List<String>,
    val lowQualityPhotosPaths: List<String>,
    val totalBlurrySize: Long,
    val totalLowQualitySize: Long,
    val potentialSpaceSavings: Long,
    val analysisVersion: Int = 1,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): PhotoAnalysis {
        return PhotoAnalysis(
            totalPhotos = totalPhotos,
            blurryPhotos = emptyList(), // Would need separate table for detailed info
            lowQualityPhotos = emptyList(), // Would need separate table for detailed info
            similarPhotoGroups = emptyList(), // Would need separate table for detailed info
            analysisTimestamp = analysisTimestamp,
            error = error
        )
    }
}