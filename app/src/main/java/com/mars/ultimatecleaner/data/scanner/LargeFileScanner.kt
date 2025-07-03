package com.mars.ultimatecleaner.data.scanner

import android.content.Context
import com.mars.ultimatecleaner.domain.model.LargeFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LargeFileScanner @Inject constructor(
    private val context: Context
) {

    fun scanLargeFiles(thresholdMB: Long = 100): Flow<List<LargeFile>> = flow {
        val largeFiles = mutableListOf<LargeFile>()
        val thresholdBytes = thresholdMB * 1024 * 1024

        // Scan internal storage
        scanDirectoryForLargeFiles(context.filesDir, largeFiles, thresholdBytes)

        // Scan external storage
        context.getExternalFilesDir(null)?.let { externalDir ->
            scanDirectoryForLargeFiles(externalDir, largeFiles, thresholdBytes)
        }

        emit(largeFiles)
    }

    private fun scanDirectoryForLargeFiles(directory: File, largeFiles: MutableList<LargeFile>, threshold: Long) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.length() > threshold) {
                largeFiles.add(
                    LargeFile(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        mimeType = getMimeType(file)
                    )
                )
            } else if (file.isDirectory) {
                scanDirectoryForLargeFiles(file, largeFiles, threshold)
            }
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "mp4", "avi", "mkv", "mov" -> "video"
            "jpg", "jpeg", "png", "gif" -> "image"
            "mp3", "wav", "aac", "flac" -> "audio"
            "pdf" -> "document"
            "zip", "rar", "7z" -> "archive"
            else -> "other"
        }
    }
}
