package com.mars.ultimatecleaner.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_analysis")
data class FileAnalysisEntity(
    @PrimaryKey
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val fileType: String,
    val mimeType: String? = null,
    val lastModified: Long,
    val analysisTimestamp: Long,
    
    // Analysis results
    val isDuplicate: Boolean = false,
    val duplicateGroup: String? = null,
    val isBlurry: Boolean = false,
    val blurScore: Float? = null,
    val isLowQuality: Boolean = false,
    val qualityScore: Float? = null,
    val qualityIssues: String? = null, // JSON string of issues
    
    // Hash for duplicate detection
    val fileHash: String? = null,
    val contentHash: String? = null,
    
    // Additional metadata
    val analysisVersion: Int = 1,
    val needsReanalysis: Boolean = false,
    val analysisError: String? = null
)
