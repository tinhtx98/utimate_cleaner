package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.model.EmptyFolder
import com.mars.ultimatecleaner.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FindEmptyFoldersUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    operator fun invoke(): Flow<List<EmptyFolder>> = flow {
        fileRepository.scanEmptyFolders().collect { folders ->
            emit(folders.sortedBy { it.name })
        }
    }
}
