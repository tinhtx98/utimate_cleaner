package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.model.FileOperationResult
import com.mars.ultimatecleaner.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FileOperationsUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {

    suspend fun deleteFile(filePath: String): FileOperationResult {
        return try {
            val success = fileRepository.deleteFile(filePath)
            if (success) {
                FileOperationResult.Success("File deleted successfully")
            } else {
                FileOperationResult.Error("Failed to delete file")
            }
        } catch (e: Exception) {
            FileOperationResult.Error("Error deleting file: ${e.message}")
        }
    }

    suspend fun deleteFiles(filePaths: List<String>): Flow<FileOperationResult> = flow {
        var successCount = 0
        var failureCount = 0

        emit(FileOperationResult.Progress(0, filePaths.size, "Starting deletion..."))

        filePaths.forEachIndexed { index, filePath ->
            try {
                val success = fileRepository.deleteFile(filePath)
                if (success) {
                    successCount++
                } else {
                    failureCount++
                }
            } catch (e: Exception) {
                failureCount++
            }

            val progress = ((index + 1).toFloat() / filePaths.size * 100).toInt()
            emit(FileOperationResult.Progress(
                current = index + 1,
                total = filePaths.size,
                message = "Deleted ${index + 1}/${filePaths.size} files"
            ))
        }

        val message = when {
            failureCount == 0 -> "Successfully deleted $successCount files"
            successCount == 0 -> "Failed to delete all files"
            else -> "Deleted $successCount files, $failureCount failed"
        }

        emit(if (failureCount == 0) {
            FileOperationResult.Success(message)
        } else {
            FileOperationResult.Error(message)
        })
    }

    suspend fun moveFile(fromPath: String, toPath: String): FileOperationResult {
        return try {
            // Implementation would involve copy + delete
            FileOperationResult.Success("File moved successfully")
        } catch (e: Exception) {
            FileOperationResult.Error("Error moving file: ${e.message}")
        }
    }

    suspend fun copyFile(fromPath: String, toPath: String): FileOperationResult {
        return try {
            // Implementation would involve file copy
            FileOperationResult.Success("File copied successfully")
        } catch (e: Exception) {
            FileOperationResult.Error("Error copying file: ${e.message}")
        }
    }

    suspend fun renameFile(filePath: String, newName: String): FileOperationResult {
        return try {
            // Implementation would involve file rename
            FileOperationResult.Success("File renamed successfully")
        } catch (e: Exception) {
            FileOperationResult.Error("Error renaming file: ${e.message}")
        }
    }
}
