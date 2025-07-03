package com.mars.ultimatecleaner.domain.model

data class FileAnalysisResult(
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
    val qualityScore: Float?,
    val blurScore: Float?,
    val compressionRatio: Float?
)