package com.mars.ultimatecleaner.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.mars.ultimatecleaner.domain.model.StorageInfo
import com.mars.ultimatecleaner.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val context: Context
) : StorageRepository {

    override fun getStorageInfo(): Flow<StorageInfo> = flow {
        val storageInfo = withContext(Dispatchers.IO) {
            calculateStorageInfo()
        }
        emit(storageInfo)
    }

    override suspend fun getAvailableSpace(): Long = withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getDataDirectory().path)
        stat.availableBlocksLong * stat.blockSizeLong
    }

    override suspend fun getTotalSpace(): Long = withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getDataDirectory().path)
        stat.totalSizeBytes
    }

    override suspend fun getUsedSpace(): Long = withContext(Dispatchers.IO) {
        getTotalSpace() - getAvailableSpace()
    }

    override suspend fun getStorageBreakdown(): Map<String, Long> = withContext(Dispatchers.IO) {
        val breakdown = mutableMapOf<String, Long>()

        // Calculate storage used by different categories
        breakdown["Apps"] = calculateAppsStorage()
        breakdown["Photos"] = calculatePhotosStorage()
        breakdown["Videos"] = calculateVideosStorage()
        breakdown["Documents"] = calculateDocumentsStorage()
        breakdown["Cache"] = calculateCacheStorage()
        breakdown["Other"] = calculateOtherStorage()

        breakdown
    }

    private suspend fun calculateStorageInfo(): StorageInfo {
        val totalSpace = getTotalSpace()
        val availableSpace = getAvailableSpace()
        val usedSpace = totalSpace - availableSpace
        val usagePercentage = (usedSpace.toFloat() / totalSpace.toFloat() * 100).toInt()

        return StorageInfo(
            totalSpace = totalSpace,
            availableSpace = availableSpace,
            usedSpace = usedSpace,
            usagePercentage = usagePercentage,
            breakdown = getStorageBreakdown()
        )
    }

    private fun calculateAppsStorage(): Long {
        // Simplified calculation - in real implementation would scan app directories
        return 2L * 1024 * 1024 * 1024 // 2GB estimate
    }

    private fun calculatePhotosStorage(): Long {
        // Simplified calculation - in real implementation would scan DCIM and Pictures
        return 5L * 1024 * 1024 * 1024 // 5GB estimate
    }

    private fun calculateVideosStorage(): Long {
        // Simplified calculation - in real implementation would scan video directories
        return 3L * 1024 * 1024 * 1024 // 3GB estimate
    }

    private fun calculateDocumentsStorage(): Long {
        // Simplified calculation - in real implementation would scan document directories
        return 1L * 1024 * 1024 * 1024 // 1GB estimate
    }

    private fun calculateCacheStorage(): Long {
        // Calculate cache directory size
        var totalSize = 0L

        // App cache
        context.cacheDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                totalSize += file.length()
            }
        }

        // External cache
        context.externalCacheDir?.walkTopDown()?.forEach { file ->
            if (file.isFile) {
                totalSize += file.length()
            }
        }

        return totalSize
    }

    private fun calculateOtherStorage(): Long {
        // Simplified calculation for other files
        return 1L * 1024 * 1024 * 1024 // 1GB estimate
    }
}
