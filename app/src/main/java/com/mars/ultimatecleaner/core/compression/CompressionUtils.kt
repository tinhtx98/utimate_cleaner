package com.mars.ultimatecleaner.core.compression

import android.webkit.MimeTypeMap
import java.io.File
import java.text.DecimalFormat

object CompressionUtils {

    private val sizeFormatter = DecimalFormat("#.##")

    fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "${sizeFormatter.format(size)} ${units[unitIndex]}"
    }

    fun formatCompressionRatio(ratio: Float): String {
        return "${sizeFormatter.format(ratio * 100)}%"
    }

    fun getCompressionTypeFromFile(file: File): CompressionType {
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())

        return when {
            mimeType?.startsWith("image/") == true -> CompressionType.IMAGE
            mimeType?.startsWith("video/") == true -> CompressionType.VIDEO
            mimeType?.startsWith("audio/") == true -> CompressionType.AUDIO
            mimeType?.startsWith("application/") == true -> {
                when (file.extension.lowercase()) {
                    "zip", "rar", "7z", "tar", "gz" -> CompressionType.ARCHIVE
                    "pdf", "doc", "docx", "txt", "rtf" -> CompressionType.DOCUMENT
                    else -> CompressionType.OTHER
                }
            }
            else -> CompressionType.OTHER
        }
    }

    fun isCompressible(file: File): Boolean {
        val nonCompressibleExtensions = setOf(
            "zip", "rar", "7z", "gz", "tar", "bz2",
            "mp3", "mp4", "avi", "mkv", "mov",
            "jpg", "jpeg", "png", "gif", "webp"
        )

        return !nonCompressibleExtensions.contains(file.extension.lowercase())
    }

    fun calculateCompressionTime(fileSize: Long, throughput: Long): Long {
        return if (throughput > 0) {
            (fileSize * 1000) / throughput
        } else 0L
    }

    fun getRecommendedCompressionConfig(
        files: List<File>,
        targetSavings: Float = 0.3f
    ): CompressionConfig {
        val totalSize = files.sumOf { it.length() }
        val averageFileSize = if (files.isNotEmpty()) totalSize / files.size else 0L

        return when {
            averageFileSize < 100 * 1024 -> { // Small files
                CompressionConfig(
                    imageQuality = 90,
                    maxImageDimension = 1920,
                    videoQuality = VideoQuality.HIGH,
                    zipCompressionLevel = 4,
                    compressionStrategy = CompressionStrategy.LIGHT
                )
            }
            averageFileSize < 1024 * 1024 -> { // Medium files
                CompressionConfig(
                    imageQuality = 85,
                    maxImageDimension = 1920,
                    videoQuality = VideoQuality.MEDIUM,
                    zipCompressionLevel = 6,
                    compressionStrategy = CompressionStrategy.MEDIUM
                )
            }
            else -> { // Large files
                CompressionConfig(
                    imageQuality = 75,
                    maxImageDimension = 1280,
                    videoQuality = VideoQuality.LOW,
                    zipCompressionLevel = 9,
                    compressionStrategy = CompressionStrategy.AGGRESSIVE
                )
            }
        }
    }
}