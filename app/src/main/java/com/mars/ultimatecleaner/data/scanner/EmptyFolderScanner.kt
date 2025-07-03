package com.mars.ultimatecleaner.data.scanner

import android.content.Context
import com.mars.ultimatecleaner.domain.model.EmptyFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmptyFolderScanner @Inject constructor(
    private val context: Context
) {

    fun scanEmptyFolders(): Flow<List<EmptyFolder>> = flow {
        val emptyFolders = mutableListOf<EmptyFolder>()

        // Scan internal storage
        scanDirectoryForEmptyFolders(context.filesDir, emptyFolders)

        // Scan external storage
        context.getExternalFilesDir(null)?.let { externalDir ->
            scanDirectoryForEmptyFolders(externalDir, emptyFolders)
        }

        emit(emptyFolders)
    }

    private fun scanDirectoryForEmptyFolders(directory: File, emptyFolders: MutableList<EmptyFolder>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (isDirectoryEmpty(file)) {
                    emptyFolders.add(
                        EmptyFolder(
                            path = file.absolutePath,
                            name = file.name,
                            lastModified = file.lastModified()
                        )
                    )
                } else {
                    scanDirectoryForEmptyFolders(file, emptyFolders)
                }
            }
        }
    }

    private fun isDirectoryEmpty(directory: File): Boolean {
        val files = directory.listFiles() ?: return true
        return files.isEmpty() || files.all { it.isDirectory && isDirectoryEmpty(it) }
    }
}
