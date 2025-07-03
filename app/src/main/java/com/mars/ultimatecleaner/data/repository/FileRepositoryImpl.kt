package com.mars.ultimatecleaner.data.repository

import com.mars.ultimatecleaner.domain.repository.FileRepository
import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.data.datasource.FileSystemDataSource
import com.mars.ultimatecleaner.data.datasource.LocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val fileSystemDataSource: FileSystemDataSource,
    private val localDataSource: LocalDataSource
) : FileRepository {

    override suspend fun getFilesByCategory(category: FileCategoryDomain): List<FileItem> {
        return withContext(Dispatchers.IO) {
            when (category) {
                FileCategoryDomain.PHOTOS -> getMediaFiles(listOf("jpg", "jpeg", "png", "gif", "bmp", "webp"))
                FileCategoryDomain.VIDEOS -> getMediaFiles(listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm"))
                FileCategoryDomain.DOCUMENTS -> getMediaFiles(listOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx"))
                FileCategoryDomain.AUDIO -> getMediaFiles(listOf("mp3", "wav", "flac", "aac", "ogg", "m4a"))
                FileCategoryDomain.DOWNLOADS -> getDownloadFiles()
                FileCategoryDomain.APPS -> getApkFiles()
                FileCategoryDomain.ALL -> getAllFiles()
            }
        }
    }

    override suspend fun searchFiles(query: String, category: FileCategoryDomain?): List<FileItem> {
        return withContext(Dispatchers.IO) {
            val files = if (category != null) {
                getFilesByCategory(category)
            } else {
                getAllFiles()
            }

            files.filter { file ->
                file.name.contains(query, ignoreCase = true) ||
                        file.path.contains(query, ignoreCase = true)
            }
        }
    }

    override suspend fun getFileMetadata(path: String): FileMetadata? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists()) return@withContext null

                val mimeType = getMimeType(file.extension)
                val md5Hash = if (file.length() < 100 * 1024 * 1024) { // Only for files < 100MB
                    calculateMD5(file)
                } else null

                FileMetadata(
                    path = path,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    lastAccessed = file.lastModified(), // Android doesn't provide last accessed
                    mimeType = mimeType,
                    md5Hash = md5Hash,
                    isMediaFile = isMediaFile(mimeType)
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun deleteFiles(paths: List<String>): FileOperationResult {
        return withContext(Dispatchers.IO) {
            var successCount = 0
            var failedCount = 0
            val failedFiles = mutableListOf<String>()

            paths.forEach { path ->
                try {
                    val file = File(path)
                    if (file.exists() && file.delete()) {
                        successCount++
                    } else {
                        failedCount++
                        failedFiles.add(path)
                    }
                } catch (e: Exception) {
                    failedCount++
                    failedFiles.add(path)
                }
            }

            FileOperationResult(
                isSuccess = failedCount == 0,
                successCount = successCount,
                failedCount = failedCount,
                errorMessage = if (failedCount > 0) "Failed to delete $failedCount files" else null,
                failedFiles = failedFiles
            )
        }
    }

    override suspend fun moveFiles(sourcePaths: List<String>, destinationPath: String): FileOperationResult {
        return withContext(Dispatchers.IO) {
            var successCount = 0
            var failedCount = 0
            val failedFiles = mutableListOf<String>()

            sourcePaths.forEach { sourcePath ->
                try {
                    val sourceFile = File(sourcePath)
                    val destinationFile = File(destinationPath, sourceFile.name)

                    if (sourceFile.exists() && sourceFile.renameTo(destinationFile)) {
                        successCount++
                    } else {
                        failedCount++
                        failedFiles.add(sourcePath)
                    }
                } catch (e: Exception) {
                    failedCount++
                    failedFiles.add(sourcePath)
                }
            }

            FileOperationResult(
                isSuccess = failedCount == 0,
                successCount = successCount,
                failedCount = failedCount,
                errorMessage = if (failedCount > 0) "Failed to move $failedCount files" else null,
                failedFiles = failedFiles
            )
        }
    }

    override suspend fun copyFiles(sourcePaths: List<String>, destinationPath: String): FileOperationResult {
        return withContext(Dispatchers.IO) {
            var successCount = 0
            var failedCount = 0
            val failedFiles = mutableListOf<String>()

            sourcePaths.forEach { sourcePath ->
                try {
                    val sourceFile = File(sourcePath)
                    val destinationFile = File(destinationPath, sourceFile.name)

                    if (sourceFile.exists()) {
                        sourceFile.copyTo(destinationFile, overwrite = true)
                        successCount++
                    } else {
                        failedCount++
                        failedFiles.add(sourcePath)
                    }
                } catch (e: Exception) {
                    failedCount++
                    failedFiles.add(sourcePath)
                }
            }

            FileOperationResult(
                isSuccess = failedCount == 0,
                successCount = successCount,
                failedCount = failedCount,
                errorMessage = if (failedCount > 0) "Failed to copy $failedCount files" else null,
                failedFiles = failedFiles
            )
        }
    }

    override suspend fun renameFile(oldPath: String, newName: String): FileOperationResult {
        return withContext(Dispatchers.IO) {
            try {
                val oldFile = File(oldPath)
                val newFile = File(oldFile.parent, newName)

                if (oldFile.exists() && oldFile.renameTo(newFile)) {
                    FileOperationResult(
                        isSuccess = true,
                        successCount = 1,
                        failedCount = 0
                    )
                } else {
                    FileOperationResult(
                        isSuccess = false,
                        successCount = 0,
                        failedCount = 1,
                        errorMessage = "Failed to rename file",
                        failedFiles = listOf(oldPath)
                    )
                }
            } catch (e: Exception) {
                FileOperationResult(
                    isSuccess = false,
                    successCount = 0,
                    failedCount = 1,
                    errorMessage = e.message ?: "Rename failed",
                    failedFiles = listOf(oldPath)
                )
            }
        }
    }

    override suspend fun createFolder(parentPath: String, folderName: String): FileOperationResult {
        return withContext(Dispatchers.IO) {
            try {
                val newFolder = File(parentPath, folderName)

                if (newFolder.mkdirs() || newFolder.exists()) {
                    FileOperationResult(
                        isSuccess = true,
                        successCount = 1,
                        failedCount = 0
                    )
                } else {
                    FileOperationResult(
                        isSuccess = false,
                        successCount = 0,
                        failedCount = 1,
                        errorMessage = "Failed to create folder"
                    )
                }
            } catch (e: Exception) {
                FileOperationResult(
                    isSuccess = false,
                    successCount = 0,
                    failedCount = 1,
                    errorMessage = e.message ?: "Create folder failed"
                )
            }
        }
    }

    override suspend fun getStorageInfo(): StorageInfoDomain {
        return withContext(Dispatchers.IO) {
            fileSystemDataSource.getStorageInfo()
        }
    }

    override suspend fun getThumbnail(filePath: String): String? {
        return withContext(Dispatchers.IO) {
            fileSystemDataSource.generateThumbnail(filePath)
        }
    }

    override suspend fun getFilesByPath(path: String): List<FileItem> {
        return withContext(Dispatchers.IO) {
            fileSystemDataSource.getFilesByPath(path)
        }
    }

    override suspend fun calculateDirectorySize(path: String): Long {
        return withContext(Dispatchers.IO) {
            fileSystemDataSource.calculateDirectorySize(path)
        }
    }

    override fun observeFileChanges(path: String): Flow<List<FileItem>> = flow {
        // Implement file watching logic
        emit(getFilesByPath(path))
    }

    // Helper methods
    private suspend fun getMediaFiles(extensions: List<String>): List<FileItem> {
        return fileSystemDataSource.getMediaFiles(extensions)
    }

    private suspend fun getDownloadFiles(): List<FileItem> {
        return fileSystemDataSource.getDownloadFiles()
    }

    private suspend fun getApkFiles(): List<FileItem> {
        return fileSystemDataSource.getApkFiles()
    }

    private suspend fun getAllFiles(): List<FileItem> {
        return fileSystemDataSource.getAllFiles()
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    private fun isMediaFile(mimeType: String): Boolean {
        return mimeType.startsWith("image/") ||
                mimeType.startsWith("video/") ||
                mimeType.startsWith("audio/")
    }

    private fun calculateMD5(file: File): String {
        val md5 = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                md5.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return md5.digest().joinToString("") { "%02x".format(it) }
    }
}