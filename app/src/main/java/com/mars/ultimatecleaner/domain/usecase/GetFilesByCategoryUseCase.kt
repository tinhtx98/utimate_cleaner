package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.model.FileItem
import com.mars.ultimatecleaner.domain.model.FileCategoryDomain
import com.mars.ultimatecleaner.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetFilesByCategoryUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    operator fun invoke(category: FileCategoryDomain): Flow<List<FileItem>> = flow {
        fileRepository.getFilesByCategory(category).let { files ->
            emit(files.sortedByDescending { it.lastModified })
        }
    }
}
