package com.mars.ultimatecleaner.data.scanner

import android.content.Context
import com.mars.ultimatecleaner.domain.model.JunkFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JunkFileScanner @Inject constructor(
    private val context: Context
) {
    
    fun scanJunkFiles(): Flow<List<JunkFile>> = flow {
        val junkFiles = mutableListOf<JunkFile>()
        
        // Scan cache directories
        val cacheDir = context.cacheDir
        if (cacheDir.exists()) {
            scanDirectory(cacheDir, junkFiles, "Cache")
        }
        
        // Scan external cache
        context.externalCacheDir?.let { externalCache ->
            if (externalCache.exists()) {
                scanDirectory(externalCache, junkFiles, "External Cache")
            }
        }
        
        emit(junkFiles)
    }
    
    private fun scanDirectory(directory: File, junkFiles: MutableList<JunkFile>, category: String) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile) {
                junkFiles.add(
                    JunkFile(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        category = category,
                        lastModified = file.lastModified()
                    )
                )
            } else if (file.isDirectory) {
                scanDirectory(file, junkFiles, category)
            }
        }
    }
}
