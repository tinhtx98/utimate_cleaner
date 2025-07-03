package com.mars.ultimatecleaner.core.compression

data class CompressionConfig(
    val imageQuality: Int = 85,
    val maxImageDimension: Int = 1920,
    val videoQuality: VideoQuality = VideoQuality.MEDIUM,
    val zipCompressionLevel: Int = 6,
    val enableProgressCallback: Boolean = true,
    val preserveMetadata: Boolean = false,
    val createBackup: Boolean = true,
    val compressionStrategy: CompressionStrategy = CompressionStrategy.MEDIUM
)