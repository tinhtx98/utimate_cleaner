package com.mars.ultimatecleaner.data.algorithm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mars.ultimatecleaner.data.utils.HashUtils
import com.mars.ultimatecleaner.data.utils.ImageUtils
import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateDetector @Inject constructor(
    private val hashUtils: HashUtils,
    private val imageUtils: ImageUtils
) {

    private val hashCache = ConcurrentHashMap<String, String>()
    private val perceptualHashCache = ConcurrentHashMap<String, String>()

    fun detectDuplicates(files: List<FileItem>): Flow<DuplicateAnalysisProgress> = flow {
        var processedFiles = 0
        val totalFiles = files.size
        val duplicateGroups = mutableListOf<DuplicateGroup>()

        emit(DuplicateAnalysisProgress(0f, "Initializing duplicate detection...", 0, totalFiles))

        try {
            // Phase 1: Quick filtering by size
            emit(DuplicateAnalysisProgress(5f, "Filtering files by size...", 0, totalFiles))
            val candidatesBySize = groupFilesBySize(files)

            // Phase 2: Content hashing for exact duplicates
            emit(DuplicateAnalysisProgress(10f, "Computing file hashes...", 0, totalFiles))
            val exactDuplicates = findExactDuplicates(candidatesBySize) { progress ->
                processedFiles = progress
                val percentage = 10f + (progress.toFloat() / totalFiles * 40f)
                emit(DuplicateAnalysisProgress(
                    percentage,
                    "Hashing file ${progress}/${totalFiles}",
                    progress,
                    totalFiles
                ))
            }

            duplicateGroups.addAll(exactDuplicates)

            // Phase 3: Perceptual hashing for similar images
            emit(DuplicateAnalysisProgress(50f, "Analyzing similar images...", processedFiles, totalFiles))
            val imageFiles = files.filter { isImageFile(it) }
            val similarImages = findSimilarImages(imageFiles) { progress ->
                val percentage = 50f + (progress.toFloat() / imageFiles.size * 30f)
                emit(DuplicateAnalysisProgress(
                    percentage,
                    "Analyzing image ${progress}/${imageFiles.size}",
                    processedFiles + progress,
                    totalFiles
                ))
            }

            duplicateGroups.addAll(similarImages)

            // Phase 4: Video duplicate detection
            emit(DuplicateAnalysisProgress(80f, "Analyzing videos...", processedFiles, totalFiles))
            val videoFiles = files.filter { isVideoFile(it) }
            val duplicateVideos = findDuplicateVideos(videoFiles) { progress ->
                val percentage = 80f + (progress.toFloat() / videoFiles.size * 15f)
                emit(DuplicateAnalysisProgress(
                    percentage,
                    "Analyzing video ${progress}/${videoFiles.size}",
                    processedFiles + progress,
                    totalFiles
                ))
            }

            duplicateGroups.addAll(duplicateVideos)

            // Phase 5: Final processing
            emit(DuplicateAnalysisProgress(95f, "Processing results...", totalFiles, totalFiles))

            val optimizedGroups = optimizeDuplicateGroups(duplicateGroups)

            emit(DuplicateAnalysisProgress(
                100f,
                "Analysis completed",
                totalFiles,
                totalFiles,
                duplicateGroups = optimizedGroups,
                isComplete = true
            ))

        } catch (e: Exception) {
            emit(DuplicateAnalysisProgress(
                0f,
                "Analysis failed: ${e.message}",
                processedFiles,
                totalFiles,
                isComplete = true
            ))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun groupFilesBySize(files: List<FileItem>): Map<Long, List<FileItem>> {
        return files
            .filter { it.size > 0 } // Exclude empty files
            .groupBy { it.size }
            .filter { it.value.size > 1 } // Only groups with multiple files
    }

    private suspend fun findExactDuplicates(
        candidatesBySize: Map<Long, List<FileItem>>,
        onProgress: suspend (Int) -> Unit
    ): List<DuplicateGroup> {
        val duplicateGroups = mutableListOf<DuplicateGroup>()
        var processedFiles = 0

        for ((size, filesWithSameSize) in candidatesBySize) {
            if (filesWithSameSize.size < 2) continue

            val hashedGroups = mutableMapOf<String, MutableList<FileItem>>()

            for (file in filesWithSameSize) {
                try {
                    val hash = getOrComputeHash(file.path)
                    hashedGroups.getOrPut(hash) { mutableListOf() }.add(file)

                    processedFiles++
                    onProgress(processedFiles)
                    yield()

                } catch (e: Exception) {
                    // Skip files that can't be hashed
                    processedFiles++
                    onProgress(processedFiles)
                }
            }

            // Create duplicate groups for files with same hash
            hashedGroups.values
                .filter { it.size > 1 }
                .forEach { duplicateFiles ->
                    val group = DuplicateGroup(
                        id = generateGroupId(),
                        files = duplicateFiles,
                        totalSize = duplicateFiles.sumOf { it.size },
                        hash = hashedGroups.entries.first { it.value == duplicateFiles }.key,
                        keepFile = selectBestFileToKeep(duplicateFiles)
                    )
                    duplicateGroups.add(group)
                }
        }

        return duplicateGroups
    }

    private suspend fun findSimilarImages(
        imageFiles: List<FileItem>,
        onProgress: suspend (Int) -> Unit
    ): List<DuplicateGroup> {
        val duplicateGroups = mutableListOf<DuplicateGroup>()
        val perceptualHashes = mutableMapOf<String, String>()
        var processedFiles = 0

        // Compute perceptual hashes
        for (file in imageFiles) {
            try {
                val perceptualHash = getOrComputePerceptualHash(file.path)
                perceptualHashes[file.path] = perceptualHash

                processedFiles++
                onProgress(processedFiles)
                yield()

            } catch (e: Exception) {
                processedFiles++
                onProgress(processedFiles)
            }
        }

        // Find similar images by comparing perceptual hashes
        val processedPaths = mutableSetOf<String>()

        for (file1 in imageFiles) {
            if (file1.path in processedPaths) continue

            val hash1 = perceptualHashes[file1.path] ?: continue
            val similarFiles = mutableListOf(file1)

            for (file2 in imageFiles) {
                if (file2.path == file1.path || file2.path in processedPaths) continue

                val hash2 = perceptualHashes[file2.path] ?: continue

                if (calculateHammingDistance(hash1, hash2) <= SIMILARITY_THRESHOLD) {
                    similarFiles.add(file2)
                    processedPaths.add(file2.path)
                }
            }

            if (similarFiles.size > 1) {
                val group = DuplicateGroup(
                    id = generateGroupId(),
                    files = similarFiles,
                    totalSize = similarFiles.sumOf { it.size },
                    hash = hash1,
                    keepFile = selectBestImageToKeep(similarFiles)
                )
                duplicateGroups.add(group)
            }

            processedPaths.add(file1.path)
        }

        return duplicateGroups
    }

    private suspend fun findDuplicateVideos(
        videoFiles: List<FileItem>,
        onProgress: suspend (Int) -> Unit
    ): List<DuplicateGroup> {
        val duplicateGroups = mutableListOf<DuplicateGroup>()
        var processedFiles = 0

        // Group by duration and resolution first
        val videoMetadata = mutableMapOf<String, VideoMetadata>()

        for (file in videoFiles) {
            try {
                val metadata = extractVideoMetadata(file.path)
                videoMetadata[file.path] = metadata

                processedFiles++
                onProgress(processedFiles)
                yield()

            } catch (e: Exception) {
                processedFiles++
                onProgress(processedFiles)
            }
        }

        // Group by similar metadata
        val metadataGroups = videoFiles
            .mapNotNull { file -> videoMetadata[file.path]?.let { file to it } }
            .groupBy { (_, metadata) -> "${metadata.duration}_${metadata.resolution}" }
            .filter { it.value.size > 1 }

        // For each group, compare frame samples
        for ((_, filesWithMetadata) in metadataGroups) {
            val files = filesWithMetadata.map { it.first }
            val frameSimilarGroups = findSimilarVideosByFrames(files)
            duplicateGroups.addAll(frameSimilarGroups)
        }

        return duplicateGroups
    }

    private suspend fun getOrComputeHash(filePath: String): String {
        return hashCache.getOrPut(filePath) {
            hashUtils.computeMD5Hash(File(filePath))
        }
    }

    private suspend fun getOrComputePerceptualHash(imagePath: String): String {
        return perceptualHashCache.getOrPut(imagePath) {
            computePerceptualHash(imagePath)
        }
    }

    private fun computePerceptualHash(imagePath: String): String {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: throw Exception("Cannot decode image")
        return imageUtils.computePerceptualHash(bitmap)
    }

    private fun calculateHammingDistance(hash1: String, hash2: String): Int {
        if (hash1.length != hash2.length) return Int.MAX_VALUE

        return hash1.zip(hash2).count { (c1, c2) -> c1 != c2 }
    }

    private fun selectBestFileToKeep(files: List<FileItem>): String {
        // Priority: Largest file, then newest, then best location
        return files
            .sortedWith(
                compareByDescending<FileItem> { it.size }
                    .thenByDescending { it.lastModified }
                    .thenBy { getPathPriority(it.path) }
            )
            .first()
            .path
    }

    private fun selectBestImageToKeep(files: List<FileItem>): String {
        // For images, consider resolution and quality
        return files
            .sortedWith(
                compareByDescending<FileItem> { getImageQualityScore(it.path) }
                    .thenByDescending { it.size }
                    .thenByDescending { it.lastModified }
            )
            .first()
            .path
    }

    private fun getPathPriority(path: String): Int {
        return when {
            path.contains("/DCIM/") -> 1 // Camera photos
            path.contains("/Pictures/") -> 2 // Pictures folder
            path.contains("/Download/") -> 3 // Downloads
            path.contains("/WhatsApp/") -> 4 // WhatsApp media
            else -> 5 // Other locations
        }
    }

    private fun getImageQualityScore(imagePath: String): Float {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                val resolution = bitmap.width * bitmap.height
                val quality = imageUtils.calculateImageQuality(bitmap)
                resolution * quality
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun isImageFile(file: FileItem): Boolean {
        return file.mimeType.startsWith("image/")
    }

    private fun isVideoFile(file: FileItem): Boolean {
        return file.mimeType.startsWith("video/")
    }

    private fun extractVideoMetadata(videoPath: String): VideoMetadata {
        // This would use MediaMetadataRetriever or similar
        return VideoMetadata(
            duration = 0L,
            resolution = "unknown",
            bitrate = 0,
            format = "unknown"
        )
    }

    private suspend fun findSimilarVideosByFrames(files: List<FileItem>): List<DuplicateGroup> {
        // Implementation would extract and compare frame samples
        return emptyList()
    }

    private fun optimizeDuplicateGroups(groups: List<DuplicateGroup>): List<DuplicateGroup> {
        // Remove groups with only one file after processing
        return groups.filter { it.files.size > 1 }
    }

    private fun generateGroupId(): String {
        return "dup_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }

    companion object {
        private const val SIMILARITY_THRESHOLD = 5 // Hamming distance threshold for similar images
    }
}

data class VideoMetadata(
    val duration: Long,
    val resolution: String,
    val bitrate: Int,
    val format: String
)