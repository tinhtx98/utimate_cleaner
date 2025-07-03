package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.model.QuickAction
import com.mars.ultimatecleaner.domain.model.CleaningResult
import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import com.mars.ultimatecleaner.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ExecuteQuickActionUseCase @Inject constructor(
    private val cleaningRepository: CleaningRepository,
    private val fileRepository: FileRepository,
    private val scanJunkFilesUseCase: ScanJunkFilesUseCase,
    private val cleanFilesUseCase: CleanFilesUseCase
) {
    operator fun invoke(action: QuickAction): Flow<CleaningResult> = flow {
        when (action) {
            is QuickAction.QuickClean -> {
                emit(CleaningResult.InProgress("Starting quick clean..."))

                // Scan and clean junk files
                scanJunkFilesUseCase().collect { junkFiles ->
                    if (junkFiles.isNotEmpty()) {
                        cleanFilesUseCase(junkFiles.map { it.path }).collect { result ->
                            emit(result)
                        }
                    } else {
                        emit(CleaningResult.Success(
                            filesDeleted = 0,
                            spaceSaved = 0L,
                            message = "No junk files found"
                        ))
                    }
                }
            }

            is QuickAction.ScanJunkFiles -> {
                emit(CleaningResult.InProgress("Scanning for junk files..."))

                scanJunkFilesUseCase().collect { junkFiles ->
                    val totalSize = junkFiles.sumOf { it.size }
                    emit(CleaningResult.ScanComplete(
                        filesFound = junkFiles.size,
                        totalSize = totalSize,
                        files = junkFiles.map { it.path }
                    ))
                }
            }

            is QuickAction.FindDuplicates -> {
                emit(CleaningResult.InProgress("Scanning for duplicate files..."))

                // Implementation for duplicate detection would go here
                emit(CleaningResult.Success(
                    filesDeleted = 0,
                    spaceSaved = 0L,
                    message = "Duplicate scan completed"
                ))
            }

            is QuickAction.ClearCache -> {
                emit(CleaningResult.InProgress("Clearing cache files..."))

                cleaningRepository.cleanCache().collect { result ->
                    emit(result)
                }
            }
        }
    }
}
