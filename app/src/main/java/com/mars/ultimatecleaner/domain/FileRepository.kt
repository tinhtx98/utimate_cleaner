package com.mars.ultimatecleaner.domain.repository

import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    suspend fun getFilesByCategory(category: FileCategory): List<FileItem>
    suspend fun searchFiles(query: String, category: FileCategory? = null): List<FileItem>
    suspend fun getFileMetadata(path: String): FileMetadata?
    suspend fun deleteFiles(paths: List<String>): FileOperationResult
    suspend fun moveFiles(sourcePaths: List<String>, destinationPath: String): FileOperationResult
    suspend fun copyFiles(sourcePaths: List<String>, destinationPath: String): FileOperationResult
    suspend fun renameFile(oldPath: String, newName: String): FileOperationResult
    suspend fun createFolder(parentPath: String, folderName: String): FileOperationResult
    suspend fun getStorageInfo(): StorageInfo
    suspend fun getThumbnail(filePath: String): String?
    suspend fun getFilesByPath(path: String): List<FileItem>
    suspend fun calculateDirectorySize(path: String): Long
    fun observeFileChanges(path: String): Flow<List<FileItem>>
}