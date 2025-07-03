package com.mars.ultimatecleaner.data.scanner

import android.content.Context
import com.mars.ultimatecleaner.domain.model.CacheFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheScanner @Inject constructor(
    private val context: Context
) {

    fun scanCacheFiles(): Flow<List<CacheFile>> = flow {
        val cacheFiles = mutableListOf<CacheFile>()

        // Scan app cache directory
        scanCacheDirectory(context.cacheDir, cacheFiles, "App Cache")

        // Scan external cache directory
        context.externalCacheDir?.let { externalCache ->
            scanCacheDirectory(externalCache, cacheFiles, "External Cache")
        }

        // Scan system cache directories
        val systemCacheDirs = listOf(
            File("/data/dalvik-cache"),
            File("/cache"),
            File("/data/tombstones")
        )

        systemCacheDirs.forEach { dir ->
            if (dir.exists() && dir.canRead()) {
                scanCacheDirectory(dir, cacheFiles, "System Cache")
            }
        }

        emit(cacheFiles)
    }

    private fun scanCacheDirectory(directory: File, cacheFiles: MutableList<CacheFile>, category: String) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile) {
                cacheFiles.add(
                    CacheFile(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        category = category,
                        lastModified = file.lastModified()
                    )
                )
            } else if (file.isDirectory) {
                scanCacheDirectory(file, cacheFiles, category)
            }
        }
    }
}
