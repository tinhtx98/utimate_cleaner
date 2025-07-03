package com.mars.ultimatecleaner.data.datasource

import android.content.Context
import android.os.Environment
import com.mars.ultimatecleaner.domain.model.FileInfo
import com.mars.ultimatecleaner.domain.model.FileCategoryDomain
import com.mars.ultimatecleaner.domain.model.FilePermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSystemDataSource @Inject constructor(
    private val context: Context
) {

    suspend fun scanDirectory(path: String): List<FileInfo> = withContext(Dispatchers.IO) {
        val directory = File(path)
        val fileList = mutableListOf<FileInfo>()

        if (!directory.exists() || !directory.canRead()) {
            return@withContext emptyList()
        }

        directory.listFiles()?.forEach { file ->
            try {
                val fileInfo = FileInfo(
                    path = file.absolutePath,
                    name = file.name,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                    mimeType = getMimeType(file),
                    category = getFileCategory(file),
                    isDirectory = file.isDirectory,
                    permissions = FilePermissions(
                        readable = file.canRead(),
                        writable = file.canWrite(),
                        executable = file.canExecute(),
                        hidden = file.isHidden
                    )
                )
                fileList.add(fileInfo)
            } catch (e: Exception) {
                // Ignore files that can't be accessed
            }
        }

        fileList
    }

    suspend fun searchFiles(query: String, searchPath: String? = null): List<FileInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileInfo>()
        val searchDir = if (searchPath != null) File(searchPath) else getDefaultSearchDirectory()

        searchInDirectory(searchDir, query.lowercase(), results)
        results
    }

    private fun searchInDirectory(directory: File, query: String, results: MutableList<FileInfo>) {
        if (!directory.exists() || !directory.canRead()) return

        directory.listFiles()?.forEach { file ->
            try {
                if (file.name.lowercase().contains(query)) {
                    val fileInfo = FileInfo(
                        path = file.absolutePath,
                        name = file.name,
                        size = if (file.isFile) file.length() else 0L,
                        lastModified = file.lastModified(),
                        mimeType = getMimeType(file),
                        category = getFileCategory(file),
                        isDirectory = file.isDirectory,
                        permissions = FilePermissions(
                            readable = file.canRead(),
                            writable = file.canWrite(),
                            executable = file.canExecute(),
                            hidden = file.isHidden
                        )
                    )
                    results.add(fileInfo)
                }

                if (file.isDirectory && results.size < 1000) { // Limit results
                    searchInDirectory(file, query, results)
                }
            } catch (e: Exception) {
                // Ignore files that can't be accessed
            }
        }
    }

    suspend fun getFileDetails(path: String): FileInfo? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null

        try {
            FileInfo(
                path = file.absolutePath,
                name = file.name,
                size = if (file.isFile) file.length() else 0L,
                lastModified = file.lastModified(),
                mimeType = getMimeType(file),
                category = getFileCategory(file),
                isDirectory = file.isDirectory,
                permissions = FilePermissions(
                    readable = file.canRead(),
                    writable = file.canWrite(),
                    executable = file.canExecute(),
                    hidden = file.isHidden
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.isDirectory) {
                deleteDirectoryRecursively(file)
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun deleteDirectoryRecursively(directory: File): Boolean {
        if (!directory.exists()) return true

        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDirectoryRecursively(file)
            } else {
                file.delete()
            }
        }

        return directory.delete()
    }

    private fun getDefaultSearchDirectory(): File {
        return Environment.getExternalStorageDirectory() ?: context.filesDir
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image"
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> "video"
            "mp3", "wav", "flac", "aac", "ogg", "m4a" -> "audio"
            "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx" -> "document"
            "zip", "rar", "7z", "tar", "gz" -> "archive"
            "apk" -> "application"
            else -> "other"
        }
    }

    private fun getFileCategory(file: File): FileCategoryDomain {
        return when (getMimeType(file)) {
            "image" -> FileCategoryDomain.PHOTOS
            "video" -> FileCategoryDomain.VIDEOS
            "audio" -> FileCategoryDomain.AUDIO
            "document" -> FileCategoryDomain.DOCUMENTS
            "application" -> FileCategoryDomain.APPS
            else -> FileCategoryDomain.ALL
        }
    }
}
