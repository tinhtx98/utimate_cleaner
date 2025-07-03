package com.mars.ultimatecleaner.domain.usecase.optimization

import com.mars.ultimatecleaner.domain.model.DuplicateGroup
import com.mars.ultimatecleaner.domain.repository.FileRepository
import kotlinx.coroutines.flow.toList
import javax.inject.Inject

class FindDuplicatesUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {

    suspend operator fun invoke(): List<DuplicateGroup> {
        return try {
            // Use the storage root directory for scanning
            val storageRoot = android.os.Environment.getExternalStorageDirectory()
            val duplicateDetectionFlow = fileRepository.findDuplicates(storageRoot)

            // Collect all progress updates and return the final result
            val progressList = duplicateDetectionFlow.toList()
            val finalProgress = progressList.lastOrNull()

            finalProgress?.duplicateGroups ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}