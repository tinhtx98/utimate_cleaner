package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.model.LargeFile
import com.mars.ultimatecleaner.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DetectLargeFilesUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    operator fun invoke(thresholdMB: Long = 100): Flow<List<LargeFile>> = flow {
        fileRepository.scanLargeFiles(thresholdMB).collect { files ->
            emit(files.sortedByDescending { it.size })
        }
    }
}
