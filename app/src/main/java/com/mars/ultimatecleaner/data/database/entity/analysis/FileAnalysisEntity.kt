package com.mars.ultimatecleaner.data.database.entity.analysis

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mars.ultimatecleaner.data.database.converter.TypeConverters as Converter
import com.mars.ultimatecleaner.domain.model.FileAnalysisResult

@Entity(
    tableName = "file_analysis",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["analysisTimestamp"]),
        Index(value = ["fileCategory"], name = "idx_file_category"),
        Index(value = ["fileSize"], name = "idx_file_size")
    ]
)
@TypeConverters(Converter::class)
data class FileAnalysisEntity(
    @PrimaryKey
    val id: String,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val fileCategory: String,
    val mimeType: String,
    val lastModified: Long,
    val analysisTimestamp: Long,
    val contentHash: String?,
    val isDuplicate: Boolean,
    val isJunkFile: Boolean,
    val isCacheFile: Boolean,
    val isTempFile: Boolean,
    val isSystemFile: Boolean,
    val metadata: Map<String, String>,
    val qualityScore: Float? = null,
    val blurScore: Float? = null,
    val compressionRatio: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): FileAnalysisResult {
        return FileAnalysisResult(
            filePath = filePath,
            fileName = fileName,
            fileSize = fileSize,
            fileCategory = fileCategory,
            mimeType = mimeType,
            lastModified = lastModified,
            analysisTimestamp = analysisTimestamp,
            contentHash = contentHash,
            isDuplicate = isDuplicate,
            isJunkFile = isJunkFile,
            isCacheFile = isCacheFile,
            isTempFile = isTempFile,
            isSystemFile = isSystemFile,
            metadata = metadata,
            qualityScore = qualityScore,
            blurScore = blurScore,
            compressionRatio = compressionRatio
        )
    }
}