package com.mars.ultimatecleaner.core.compression

data class CompressionMetrics(
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val filesProcessed: Int,
    val totalOriginalSize: Long,
    val totalCompressedSize: Long,
    val totalSavedBytes: Long,
    val averageCompressionRatio: Float,
    val throughputBytesPerSecond: Long,
    val successRate: Float,
    val errorCount: Int,
    val compressionType: CompressionType
) {
    companion object {
        fun calculate(
            startTime: Long,
            endTime: Long,
            results: List<CompressionResult>,
            compressionType: CompressionType
        ): CompressionMetrics {
            val duration = endTime - startTime
            val successResults = results.filterIsInstance<CompressionResult.Success>()
            val errorResults = results.filterIsInstance<CompressionResult.Error>()

            val totalOriginalSize = successResults.sumOf { it.originalSize }
            val totalCompressedSize = successResults.sumOf { it.compressedSize }
            val totalSavedBytes = successResults.sumOf { it.savedBytes }

            val averageCompressionRatio = if (successResults.isNotEmpty()) {
                successResults.map { it.compressionRatio }.average().toFloat()
            } else 0f

            val throughput = if (duration > 0) {
                (totalOriginalSize * 1000) / duration
            } else 0L

            val successRate = if (results.isNotEmpty()) {
                (successResults.size.toFloat() / results.size.toFloat()) * 100f
            } else 0f

            return CompressionMetrics(
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                filesProcessed = results.size,
                totalOriginalSize = totalOriginalSize,
                totalCompressedSize = totalCompressedSize,
                totalSavedBytes = totalSavedBytes,
                averageCompressionRatio = averageCompressionRatio,
                throughputBytesPerSecond = throughput,
                successRate = successRate,
                errorCount = errorResults.size,
                compressionType = compressionType
            )
        }
    }
}