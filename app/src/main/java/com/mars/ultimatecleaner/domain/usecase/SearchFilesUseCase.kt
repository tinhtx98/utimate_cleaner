package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.model.FileItem
import com.mars.ultimatecleaner.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SearchFilesUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    operator fun invoke(query: String, searchPath: String? = null): Flow<List<FileItem>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }

        fileRepository.searchFiles(query, searchPath).let { files ->
            emit(files.sortedWith(
                compareByDescending<FileItem> { file ->
                    // Prioritize exact matches
                    file.name.equals(query, ignoreCase = true)
                }.thenByDescending { file ->
                    // Then files that start with query
                    file.name.startsWith(query, ignoreCase = true)
                }.thenByDescending { file ->
                    // Then by relevance (contains query)
                    file.name.contains(query, ignoreCase = true)
                }.thenByDescending { file ->
                    // Finally by last modified
                    file.lastModified
                }
            ))
        }
    }
}
