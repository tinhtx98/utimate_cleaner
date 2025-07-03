package com.mars.ultimatecleaner.data.scanner

import android.content.Context
import com.mars.ultimatecleaner.domain.model.TempFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TempFileScanner @Inject constructor(
    private val context: Context
) {

    fun scanTempFiles(): Flow<List<TempFile>> = flow {
        val tempFiles = mutableListOf<TempFile>()

        // Common temp file extensions
        val tempExtensions = setOf("tmp", "temp", "log", "bak", "old", "~")

        // Scan app directories
        scanDirectoryForTempFiles(context.filesDir, tempFiles, tempExtensions)
        context.getExternalFilesDir(null)?.let { externalDir ->
            scanDirectoryForTempFiles(externalDir, tempFiles, tempExtensions)
        }

        // Scan system temp directories
        val tempDirs = listOf(
            File("/tmp"),
            File("/data/local/tmp"),
            File("/sdcard/.temp"),
            File("/sdcard/temp")
        )

        tempDirs.forEach { dir ->
            if (dir.exists() && dir.canRead()) {
                scanDirectoryForTempFiles(dir, tempFiles, tempExtensions)
            }
        }

        emit(tempFiles)
    }

    private fun scanDirectoryForTempFiles(
        directory: File,
        tempFiles: MutableList<TempFile>,
        tempExtensions: Set<String>
    ) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile) {
                val isTempFile = tempExtensions.contains(file.extension.lowercase()) ||
                        file.name.startsWith("temp") ||
                        file.name.startsWith("tmp") ||
                        file.name.endsWith("~") ||
                        file.name.contains(".tmp.")

                if (isTempFile) {
                    tempFiles.add(
                        TempFile(
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            lastModified = file.lastModified()
                        )
                    )
                }
            } else if (file.isDirectory) {
                scanDirectoryForTempFiles(file, tempFiles, tempExtensions)
            }
        }
    }
}
