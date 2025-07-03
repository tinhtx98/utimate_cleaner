package com.mars.ultimatecleaner.data.algorithm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import com.mars.ultimatecleaner.data.utils.ImageUtils
import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoAnalyzer @Inject constructor(
    private val imageUtils: ImageUtils
) {

    companion object {
        private const val BLUR_THRESHOLD = 100.0 // Laplacian variance threshold
        private const val QUALITY_THRESHOLD = 0.6f // Overall quality threshold
        private const val MIN_RESOLUTION = 320 * 240 // Minimum resolution for quality photos
        private const val FACE_BONUS = 0.2f // Bonus for photos with faces
    }

    fun analyzePhotos(photos: List<FileItem>): Flow<PhotoAnalysisProgress> = flow {
        var analyzedPhotos = 0
        val totalPhotos = photos.size
        val blurryPhotos = mutableListOf<PhotoItemDomain>()
        val lowQualityPhotos = mutableListOf<PhotoItemDomain>()

        emit(PhotoAnalysisProgress(0f, "Initializing photo analysis...", 0, totalPhotos))

        try {
            for (photo in photos) {
                try {
                    val analysis = analyzePhoto(photo.path)

                    if (analysis.isBlurry) {
                        blurryPhotos.add(analysis)
                    }

                    if (analysis.isLowQuality) {
                        lowQualityPhotos.add(analysis)
                    }

                    analyzedPhotos++
                    val progress = (analyzedPhotos.toFloat() / totalPhotos) * 100f

                    emit(PhotoAnalysisProgress(
                        progress,
                        "Analyzing: ${photo.name}",
                        analyzedPhotos,
                        totalPhotos,
                        blurryPhotos = blurryPhotos.toList(),
                        lowQualityPhotos = lowQualityPhotos.toList()
                    ))

                    yield()

                } catch (e: Exception) {
                    analyzedPhotos++
                    // Continue with next photo
                }
            }

            emit(PhotoAnalysisProgress(
                100f,
                "Analysis completed",
                analyzedPhotos,
                totalPhotos,
                blurryPhotos = blurryPhotos,
                lowQualityPhotos = lowQualityPhotos,
                isComplete = true
            ))

        } catch (e: Exception) {
            emit(PhotoAnalysisProgress(
                0f,
                "Analysis failed: ${e.message}",
                analyzedPhotos,
                totalPhotos,
                isComplete = true
            ))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun analyzePhoto(photoPath: String): PhotoItemDomain {
        val file = File(photoPath)
        val bitmap = BitmapFactory.decodeFile(photoPath)
            ?: throw Exception("Cannot decode image")

        try {
            // Calculate blur score using Laplacian variance
            val blurScore = calculateBlurScore(bitmap)

            // Calculate overall quality score
            val qualityScore = calculateQualityScore(bitmap, photoPath)

            // Get image resolution
            val resolution = "${bitmap.width}x${bitmap.height}"

            // Generate thumbnail if needed
            val thumbnailPath = generateThumbnail(photoPath)

            return PhotoItemDomain(
                path = photoPath,
                name = file.name,
                size = file.length(),
                lastModified = file.lastModified(),
                blurScore = blurScore,
                qualityScore = qualityScore,
                thumbnailPath = thumbnailPath,
                resolution = resolution,
                isBlurry = blurScore < BLUR_THRESHOLD,
                isLowQuality = qualityScore < QUALITY_THRESHOLD
            )

        } finally {
            bitmap.recycle()
        }
    }

    private fun calculateBlurScore(bitmap: Bitmap): Float {
        // Convert to grayscale for edge detection
        val grayscaleBitmap = imageUtils.convertToGrayscale(bitmap)

        try {
            // Apply Laplacian operator for edge detection
            val laplacianVariance = imageUtils.calculateLaplacianVariance(grayscaleBitmap)
            return laplacianVariance.toFloat()

        } finally {
            grayscaleBitmap.recycle()
        }
    }

    private fun calculateQualityScore(bitmap: Bitmap, photoPath: String): Float {
        var score = 0f
        var factors = 0

        // Resolution factor
        val resolution = bitmap.width * bitmap.height
        if (resolution >= MIN_RESOLUTION) {
            score += when {
                resolution >= 1920 * 1080 -> 1.0f // Full HD+
                resolution >= 1280 * 720 -> 0.8f // HD
                resolution >= 640 * 480 -> 0.6f // VGA
                else -> 0.4f
            }
        } else {
            score += 0.2f // Very low resolution
        }
        factors++

        // Brightness and contrast
        val brightnessScore = imageUtils.calculateBrightness(bitmap)
        val contrastScore = imageUtils.calculateContrast(bitmap)

        score += (brightnessScore + contrastScore) / 2f
        factors++

        // Color distribution
        val colorScore = imageUtils.calculateColorDistribution(bitmap)
        score += colorScore
        factors++

        // Face detection bonus
        val faceCount = imageUtils.detectFaces(bitmap)
        if (faceCount > 0) {
            score += FACE_BONUS
        }

        // EXIF data quality indicators
        try {
            val exifScore = analyzeExifData(photoPath)
            score += exifScore
            factors++
        } catch (e: Exception) {
            // EXIF not available, don't penalize
        }

        return (score / factors).coerceIn(0f, 1f)
    }

    private fun analyzeExifData(photoPath: String): Float {
        val exif = ExifInterface(photoPath)
        var score = 0.5f // Base score

        // Camera make and model (professional cameras get bonus)
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)

        if (make != null && model != null) {
            when {
                isProfessionalCamera(make, model) -> score += 0.3f
                isGoodCamera(make, model) -> score += 0.2f
                else -> score += 0.1f
            }
        }

        // ISO sensitivity
        val iso = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0)
        when {
            iso in 100..400 -> score += 0.2f // Good ISO range
            iso in 400..800 -> score += 0.1f // Acceptable
            iso > 1600 -> score -= 0.1f // High ISO, likely noise
        }

        // Flash usage
        val flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, 0)
        if (flash != 0) {
            score += 0.1f // Flash was used, likely better exposure
        }

        return score.coerceIn(0f, 1f)
    }

    private fun isProfessionalCamera(make: String, model: String): Boolean {
        val professionalBrands = listOf("Canon", "Nikon", "Sony", "Fujifilm", "Leica")
        val professionalKeywords = listOf("EOS", "D850", "D750", "A7", "X-T", "GFX")

        return professionalBrands.any { make.contains(it, ignoreCase = true) } &&
                professionalKeywords.any { model.contains(it, ignoreCase = true) }
    }

    private fun isGoodCamera(make: String, model: String): Boolean {
        val goodBrands = listOf("Canon", "Nikon", "Sony", "Samsung", "Google", "Apple")
        return goodBrands.any { make.contains(it, ignoreCase = true) }
    }

    private suspend fun generateThumbnail(photoPath: String): String? {
        return try {
            imageUtils.generateThumbnail(photoPath, 150, 150)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun detectBlurryPhotos(photos: List<FileItem>): List<PhotoItemDomain> {
        return photos.mapNotNull { photo ->
            try {
                val analysis = analyzePhoto(photo.path)
                if (analysis.isBlurry) analysis else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun detectLowQualityPhotos(photos: List<FileItem>): List<PhotoItemDomain> {
        return photos.mapNotNull { photo ->
            try {
                val analysis = analyzePhoto(photo.path)
                if (analysis.isLowQuality) analysis else null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getPhotoQualityReport(photoPath: String): PhotoQualityResult {
        val bitmap = BitmapFactory.decodeFile(photoPath)
            ?: throw Exception("Cannot decode image")

        try {
            val blurScore = calculateBlurScore(bitmap)
            val qualityScore = calculateQualityScore(bitmap, photoPath)
            val brightness = imageUtils.calculateBrightness(bitmap)
            val contrast = imageUtils.calculateContrast(bitmap)

            return PhotoQualityResult(
                path = photoPath,
                blurScore = blurScore,
                qualityScore = qualityScore,
                brightness = brightness,
                contrast = contrast,
                isBlurry = blurScore < BLUR_THRESHOLD,
                isLowQuality = qualityScore < QUALITY_THRESHOLD
            )

        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Public wrapper for calculating blur score
     */
    fun calculateBlurScore(imagePath: String): Float {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                val score = calculateBlurScore(bitmap)
                bitmap.recycle()
                score
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Public wrapper for calculating quality score
     */
    fun calculateQualityScore(imagePath: String, photoPath: String): Float {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                val score = calculateQualityScore(bitmap, photoPath)
                bitmap.recycle()
                score
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Identify quality issues for a photo
     */
    fun identifyQualityIssues(imagePath: String): List<String> {
        val issues = mutableListOf<String>()
        
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                val blurScore = calculateBlurScore(bitmap)
                val qualityScore = calculateQualityScore(bitmap, imagePath)
                
                if (blurScore > BLUR_THRESHOLD) {
                    issues.add("Image is blurry")
                }
                
                if (qualityScore < QUALITY_THRESHOLD) {
                    issues.add("Low overall quality")
                }
                
                if (bitmap.width * bitmap.height < MIN_RESOLUTION) {
                    issues.add("Low resolution")
                }
                
                bitmap.recycle()
            } else {
                issues.add("Cannot decode image")
            }
        } catch (e: Exception) {
            issues.add("Error analyzing image: ${e.message}")
        }
        
        return issues
    }

    /**
     * Public wrapper for finding similar photos
     */
    suspend fun findSimilarPhotos(photos: List<File>): List<SimilarPhotoGroup> = withContext(Dispatchers.IO) {
        try {
            val similarGroups = mutableListOf<SimilarPhotoGroup>()
            val photoHashes = mutableMapOf<File, String>()

            // Calculate hash for each photo
            photos.forEach { photo ->
                try {
                    val hash = calculateImageHash(photo.absolutePath)
                    if (hash.isNotEmpty()) {
                        photoHashes[photo] = hash
                    }
                } catch (e: Exception) {
                    // Skip problematic files
                }
            }

            // Group similar photos by hash
            val groupedByHash = photoHashes.entries.groupBy { it.value }

            groupedByHash.forEach { (hash, entries) ->
                if (entries.size > 1) {
                    val photos = entries.map { (file, _) ->
                        SimilarPhoto(
                            path = file.absolutePath,
                            size = file.length(),
                            lastModified = file.lastModified()
                        )
                    }

                    similarGroups.add(
                        SimilarPhotoGroup(
                            photos = photos,
                            similarityScore = 0.95f, // High similarity for identical hashes
                            groupId = "group_${hash.take(8)}"
                        )
                    )
                }
            }

            similarGroups
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Calculate a simple hash for an image to detect duplicates
     */
    private fun calculateImageHash(imagePath: String): String {
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 8 // Reduce size for faster processing
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = BitmapFactory.decodeFile(imagePath, options)
            if (bitmap != null) {
                val hash = calculatePerceptualHash(bitmap)
                bitmap.recycle()
                hash
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Calculate perceptual hash for duplicate detection
     */
    private fun calculatePerceptualHash(bitmap: Bitmap): String {
        try {
            // Resize to 8x8 for simplicity
            val resized = Bitmap.createScaledBitmap(bitmap, 8, 8, false)

            // Convert to grayscale and calculate average
            var total = 0
            val pixels = IntArray(64)
            resized.getPixels(pixels, 0, 8, 0, 0, 8, 8)

            for (pixel in pixels) {
                val gray = (android.graphics.Color.red(pixel) +
                        android.graphics.Color.green(pixel) +
                        android.graphics.Color.blue(pixel)) / 3
                total += gray
            }

            val average = total / 64

            // Create hash based on whether each pixel is above or below average
            val hash = StringBuilder()
            for (pixel in pixels) {
                val gray = (android.graphics.Color.red(pixel) +
                        android.graphics.Color.green(pixel) +
                        android.graphics.Color.blue(pixel)) / 3
                hash.append(if (gray > average) "1" else "0")
            }

            resized.recycle()
            return hash.toString()
        } catch (e: Exception) {
            return ""
        }
    }
}