package com.mars.ultimatecleaner.core.compression

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompressionEngine @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val MAX_IMAGE_DIMENSION = 1920
        private const val DEFAULT_JPEG_QUALITY = 85
        private const val HIGH_JPEG_QUALITY = 95
        private const val LOW_JPEG_QUALITY = 70
        private const val WEBP_QUALITY = 80

        // File size thresholds
        private const val SMALL_FILE_THRESHOLD = 100 * 1024 // 100KB
        private const val MEDIUM_FILE_THRESHOLD = 1024 * 1024 // 1MB
        private const val LARGE_FILE_THRESHOLD = 10 * 1024 * 1024 // 10MB
    }

    // Image Compression
    suspend fun compressImage(
        inputFile: File,
        outputFile: File,
        quality: Int = DEFAULT_JPEG_QUALITY,
        maxWidth: Int = MAX_IMAGE_DIMENSION,
        maxHeight: Int = MAX_IMAGE_DIMENSION
    ): CompressionResult = withContext(Dispatchers.IO) {
        try {
            val originalSize = inputFile.length()

            // Decode image with proper sampling
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(inputFile.absolutePath, options)

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false

            val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath, options)
                ?: return@withContext CompressionResult.Error("Failed to decode image")

            // Compress and save
            val outputStream = FileOutputStream(outputFile)
            val format = getImageFormat(inputFile.extension)
            val compressed = bitmap.compress(format, quality, outputStream)

            outputStream.close()
            bitmap.recycle()

            if (compressed) {
                val compressedSize = outputFile.length()
                CompressionResult.Success(
                    originalSize = originalSize,
                    compressedSize = compressedSize,
                    compressionRatio = (originalSize - compressedSize).toFloat() / originalSize.toFloat(),
                    savedBytes = originalSize - compressedSize
                )
            } else {
                CompressionResult.Error("Failed to compress image")
            }
        } catch (e: Exception) {
            CompressionResult.Error("Image compression failed: ${e.message}")
        }
    }

    // Batch Image Compression
    suspend fun compressImages(
        imageFiles: List<File>,
        outputDirectory: File,
        quality: Int = DEFAULT_JPEG_QUALITY,
        progressCallback: ((Int, Int) -> Unit)? = null
    ): BatchCompressionResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<CompressionResult>()
        var totalOriginalSize = 0L
        var totalCompressedSize = 0L
        var successCount = 0
        var errorCount = 0

        imageFiles.forEachIndexed { index, inputFile ->
            try {
                val outputFile = File(outputDirectory, "compressed_${inputFile.name}")
                val result = compressImage(inputFile, outputFile, quality)

                results.add(result)

                when (result) {
                    is CompressionResult.Success -> {
                        totalOriginalSize += result.originalSize
                        totalCompressedSize += result.compressedSize
                        successCount++
                    }
                    is CompressionResult.Error -> {
                        errorCount++
                    }
                }

                progressCallback?.invoke(index + 1, imageFiles.size)
            } catch (e: Exception) {
                results.add(CompressionResult.Error("Failed to compress ${inputFile.name}: ${e.message}"))
                errorCount++
            }
        }

        BatchCompressionResult(
            individualResults = results,
            totalOriginalSize = totalOriginalSize,
            totalCompressedSize = totalCompressedSize,
            totalSavedBytes = totalOriginalSize - totalCompressedSize,
            successCount = successCount,
            errorCount = errorCount,
            compressionRatio = if (totalOriginalSize > 0) {
                (totalOriginalSize - totalCompressedSize).toFloat() / totalOriginalSize.toFloat()
            } else 0f
        )
    }

    // Video Compression (Basic)
    suspend fun compressVideo(
        inputFile: File,
        outputFile: File,
        quality: VideoQuality = VideoQuality.MEDIUM
    ): CompressionResult = withContext(Dispatchers.IO) {
        try {
            val originalSize = inputFile.length()

            // This is a simplified implementation
            // In a real app, you'd use MediaMetadataRetriever and FFmpeg or similar
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(inputFile.absolutePath)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0

            retriever.release()

            // Simulate compression (in reality, you'd use actual video compression libraries)
            val compressionFactor = when (quality) {
                VideoQuality.HIGH -> 0.8f
                VideoQuality.MEDIUM -> 0.6f
                VideoQuality.LOW -> 0.4f
            }

            val estimatedCompressedSize = (originalSize * compressionFactor).toLong()

            CompressionResult.Success(
                originalSize = originalSize,
                compressedSize = estimatedCompressedSize,
                compressionRatio = 1f - compressionFactor,
                savedBytes = originalSize - estimatedCompressedSize
            )
        } catch (e: Exception) {
            CompressionResult.Error("Video compression failed: ${e.message}")
        }
    }

    // File Compression (ZIP)
    suspend fun compressFiles(
        inputFiles: List<File>,
        outputZipFile: File,
        compressionLevel: Int = 6,
        progressCallback: ((Int, Int) -> Unit)? = null
    ): CompressionResult = withContext(Dispatchers.IO) {
        try {
            var totalOriginalSize = 0L

            val zipOutputStream = ZipOutputStream(FileOutputStream(outputZipFile))
            zipOutputStream.setLevel(compressionLevel)

            inputFiles.forEachIndexed { index, file ->
                if (file.exists() && file.isFile) {
                    totalOriginalSize += file.length()

                    val zipEntry = ZipEntry(file.name)
                    zipOutputStream.putNextEntry(zipEntry)

                    val inputStream = FileInputStream(file)
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } > 0) {
                        zipOutputStream.write(buffer, 0, bytesRead)
                    }

                    inputStream.close()
                    zipOutputStream.closeEntry()

                    progressCallback?.invoke(index + 1, inputFiles.size)
                }
            }

            zipOutputStream.close()

            val compressedSize = outputZipFile.length()

            CompressionResult.Success(
                originalSize = totalOriginalSize,
                compressedSize = compressedSize,
                compressionRatio = (totalOriginalSize - compressedSize).toFloat() / totalOriginalSize.toFloat(),
                savedBytes = totalOriginalSize - compressedSize
            )
        } catch (e: Exception) {
            CompressionResult.Error("File compression failed: ${e.message}")
        }
    }

    // GZIP Compression
    suspend fun compressFileGzip(
        inputFile: File,
        outputFile: File
    ): CompressionResult = withContext(Dispatchers.IO) {
        try {
            val originalSize = inputFile.length()

            val inputStream = FileInputStream(inputFile)
            val outputStream = GZIPOutputStream(FileOutputStream(outputFile))

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } > 0) {
                outputStream.write(buffer, 0, bytesRead)
            }

            inputStream.close()
            outputStream.close()

            val compressedSize = outputFile.length()

            CompressionResult.Success(
                originalSize = originalSize,
                compressedSize = compressedSize,
                compressionRatio = (originalSize - compressedSize).toFloat() / originalSize.toFloat(),
                savedBytes = originalSize - compressedSize
            )
        } catch (e: Exception) {
            CompressionResult.Error("GZIP compression failed: ${e.message}")
        }
    }

    // Decompression
    suspend fun decompressGzip(
        inputFile: File,
        outputFile: File
    ): CompressionResult = withContext(Dispatchers.IO) {
        try {
            val compressedSize = inputFile.length()

            val inputStream = GZIPInputStream(FileInputStream(inputFile))
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } > 0) {
                outputStream.write(buffer, 0, bytesRead)
            }

            inputStream.close()
            outputStream.close()

            val decompressedSize = outputFile.length()

            CompressionResult.Success(
                originalSize = compressedSize,
                compressedSize = decompressedSize,
                compressionRatio = (decompressedSize - compressedSize).toFloat() / compressedSize.toFloat(),
                savedBytes = decompressedSize - compressedSize
            )
        } catch (e: Exception) {
            CompressionResult.Error("GZIP decompression failed: ${e.message}")
        }
    }

    // Extract ZIP files
    suspend fun extractZip(
        zipFile: File,
        outputDirectory: File,
        progressCallback: ((Int, Int) -> Unit)? = null
    ): CompressionResult = withContext(Dispatchers.IO) {
        try {
            val zipInputStream = ZipInputStream(FileInputStream(zipFile))
            var totalExtractedSize = 0L
            var entryCount = 0

            // First pass: count entries
            val tempZipInputStream = ZipInputStream(FileInputStream(zipFile))
            var totalEntries = 0
            while (tempZipInputStream.nextEntry != null) {
                totalEntries++
            }
            tempZipInputStream.close()

            // Second pass: extract files
            var entry: ZipEntry?
            while (zipInputStream.nextEntry.also { entry = it } != null) {
                entry?.let { zipEntry ->
                    val outputFile = File(outputDirectory, zipEntry.name)

                    if (zipEntry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()

                        val outputStream = FileOutputStream(outputFile)
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int

                        while (zipInputStream.read(buffer).also { bytesRead = it } > 0) {
                            outputStream.write(buffer, 0, bytesRead)
                        }

                        outputStream.close()
                        totalExtractedSize += outputFile.length()
                    }

                    entryCount++
                    progressCallback?.invoke(entryCount, totalEntries)
                }
            }

            zipInputStream.close()

            CompressionResult.Success(
                originalSize = zipFile.length(),
                compressedSize = totalExtractedSize,
                compressionRatio = (totalExtractedSize - zipFile.length()).toFloat() / zipFile.length().toFloat(),
                savedBytes = totalExtractedSize - zipFile.length()
            )
        } catch (e: Exception) {
            CompressionResult.Error("ZIP extraction failed: ${e.message}")
        }
    }

    // Utility Functions
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun getImageFormat(extension: String): Bitmap.CompressFormat {
        return when (extension.lowercase()) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
    }

    fun getOptimalQuality(fileSize: Long): Int {
        return when {
            fileSize < SMALL_FILE_THRESHOLD -> HIGH_JPEG_QUALITY
            fileSize < MEDIUM_FILE_THRESHOLD -> DEFAULT_JPEG_QUALITY
            else -> LOW_JPEG_QUALITY
        }
    }

    fun getCompressionStrategy(fileSize: Long): CompressionStrategy {
        return when {
            fileSize < SMALL_FILE_THRESHOLD -> CompressionStrategy.LIGHT
            fileSize < MEDIUM_FILE_THRESHOLD -> CompressionStrategy.MEDIUM
            fileSize < LARGE_FILE_THRESHOLD -> CompressionStrategy.AGGRESSIVE
            else -> CompressionStrategy.MAXIMUM
        }
    }

    // Estimate compression savings
    fun estimateCompressionSavings(
        files: List<File>,
        compressionType: CompressionType
    ): EstimatedSavings {
        var totalSize = 0L
        var estimatedSavings = 0L

        files.forEach { file ->
            totalSize += file.length()

            val savingsRatio = when (compressionType) {
                CompressionType.IMAGE -> when (file.extension.lowercase()) {
                    "jpg", "jpeg" -> 0.15f
                    "png" -> 0.30f
                    "bmp" -> 0.70f
                    else -> 0.20f
                }
                CompressionType.VIDEO -> 0.40f
                CompressionType.DOCUMENT -> 0.25f
                CompressionType.ARCHIVE -> 0.50f
                CompressionType.AUDIO -> 0.30f
                else -> 0.20f
            }

            estimatedSavings += (file.length() * savingsRatio).toLong()
        }

        return EstimatedSavings(
            totalSize = totalSize,
            estimatedSavings = estimatedSavings,
            estimatedFinalSize = totalSize - estimatedSavings,
            compressionRatio = estimatedSavings.toFloat() / totalSize.toFloat()
        )
    }
}

// Data Classes
sealed class CompressionResult {
    data class Success(
        val originalSize: Long,
        val compressedSize: Long,
        val compressionRatio: Float,
        val savedBytes: Long
    ) : CompressionResult()

    data class Error(val message: String) : CompressionResult()
}

data class BatchCompressionResult(
    val individualResults: List<CompressionResult>,
    val totalOriginalSize: Long,
    val totalCompressedSize: Long,
    val totalSavedBytes: Long,
    val successCount: Int,
    val errorCount: Int,
    val compressionRatio: Float
)

data class EstimatedSavings(
    val totalSize: Long,
    val estimatedSavings: Long,
    val estimatedFinalSize: Long,
    val compressionRatio: Float
)

// Enums
enum class VideoQuality {
    HIGH, MEDIUM, LOW
}

enum class CompressionStrategy {
    LIGHT, MEDIUM, AGGRESSIVE, MAXIMUM
}

enum class CompressionType {
    IMAGE, VIDEO, DOCUMENT, ARCHIVE, AUDIO, OTHER
}