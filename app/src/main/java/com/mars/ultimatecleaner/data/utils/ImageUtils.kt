package com.mars.ultimatecleaner.data.utils

import android.graphics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

@Singleton
class ImageUtils @Inject constructor() {

    suspend fun generateThumbnail(imagePath: String, width: Int, height: Int): String? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)

            options.inSampleSize = calculateInSampleSize(options, width, height)
            options.inJustDecodeBounds = false

            val bitmap = BitmapFactory.decodeFile(imagePath, options) ?: return@withContext null

            val thumbnail = Bitmap.createScaledBitmap(bitmap, width, height, true)
            bitmap.recycle()

            val thumbnailPath = createThumbnailPath(imagePath)
            FileOutputStream(thumbnailPath).use { out ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            thumbnail.recycle()

            thumbnailPath
        } catch (e: Exception) {
            null
        }
    }

    fun calculateLaplacianVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var sum = 0.0
        var sumSquared = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = getGrayValue(pixels[y * width + x])
                val top = getGrayValue(pixels[(y - 1) * width + x])
                val bottom = getGrayValue(pixels[(y + 1) * width + x])
                val left = getGrayValue(pixels[y * width + (x - 1)])
                val right = getGrayValue(pixels[y * width + (x + 1)])

                val laplacian = abs(4 * center - top - bottom - left - right)
                sum += laplacian
                sumSquared += laplacian * laplacian
                count++
            }
        }

        val mean = sum / count
        return (sumSquared / count) - (mean * mean)
    }

    fun computePerceptualHash(bitmap: Bitmap): String {
        // Simple perceptual hash using DCT-like approach
        val resized = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val grayValues = DoubleArray(32 * 32)

        for (y in 0 until 32) {
            for (x in 0 until 32) {
                val pixel = resized.getPixel(x, y)
                grayValues[y * 32 + x] = getGrayValue(pixel).toDouble()
            }
        }
        resized.recycle()

        // Compute average
        val average = grayValues.average()

        // Generate hash
        val hash = StringBuilder()
        for (value in grayValues) {
            hash.append(if (value > average) '1' else '0')
        }

        return hash.toString()
    }

    fun calculateImageQuality(bitmap: Bitmap): Float {
        var qualityScore = 0f
        var factors = 0

        // Resolution factor
        val resolution = bitmap.width * bitmap.height
        qualityScore += when {
            resolution >= 1920 * 1080 -> 1.0f
            resolution >= 1280 * 720 -> 0.8f
            resolution >= 640 * 480 -> 0.6f
            else -> 0.4f
        }
        factors++

        // Brightness analysis
        qualityScore += calculateBrightness(bitmap)
        factors++

        // Contrast analysis
        qualityScore += calculateContrast(bitmap)
        factors++

        return qualityScore / factors
    }

    fun calculateBrightness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        var totalBrightness = 0L
        var pixelCount = 0

        for (x in 0 until width step 4) { // Sample every 4th pixel for performance
            for (y in 0 until height step 4) {
                val pixel = bitmap.getPixel(x, y)
                totalBrightness += getGrayValue(pixel)
                pixelCount++
            }
        }

        val averageBrightness = totalBrightness.toFloat() / pixelCount

        // Optimal brightness is around 128, score based on how close we are
        return 1.0f - (abs(averageBrightness - 128f) / 128f)
    }

    fun calculateContrast(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val grayValues = mutableListOf<Int>()

        for (x in 0 until width step 4) {
            for (y in 0 until height step 4) {
                val pixel = bitmap.getPixel(x, y)
                grayValues.add(getGrayValue(pixel))
            }
        }

        if (grayValues.isEmpty()) return 0f

        val min = grayValues.minOrNull() ?: 0
        val max = grayValues.maxOrNull() ?: 255

        val contrast = (max - min).toFloat() / 255f
        return contrast.coerceIn(0f, 1f)
    }

    fun calculateColorDistribution(bitmap: Bitmap): Float {
        val colorHistogram = IntArray(256) { 0 }
        val width = bitmap.width
        val height = bitmap.height

        for (x in 0 until width step 4) {
            for (y in 0 until height step 4) {
                val pixel = bitmap.getPixel(x, y)
                val gray = getGrayValue(pixel)
                colorHistogram[gray]++
            }
        }

        // Calculate entropy as a measure of color distribution
        var entropy = 0.0
        val totalPixels = colorHistogram.sum()

        for (count in colorHistogram) {
            if (count > 0) {
                val probability = count.toDouble() / totalPixels
                entropy -= probability * kotlin.math.ln(probability)
            }
        }

        // Normalize entropy (max entropy for 8-bit grayscale is ln(256))
        return (entropy / kotlin.math.ln(256.0)).toFloat()
    }

    fun detectFaces(bitmap: Bitmap): Int {
        // Simplified face detection - in a real implementation,
        // you'd use ML Kit or similar
        return 0
    }

    fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(grayBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return grayBitmap
    }

    private fun getGrayValue(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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

    private fun createThumbnailPath(originalPath: String): String {
        val originalFile = File(originalPath)
        val thumbnailDir = File(originalFile.parent, ".thumbnails")
        if (!thumbnailDir.exists()) {
            thumbnailDir.mkdirs()
        }

        val nameWithoutExtension = originalFile.nameWithoutExtension
        return File(thumbnailDir, "${nameWithoutExtension}_thumb.jpg").absolutePath
    }
}