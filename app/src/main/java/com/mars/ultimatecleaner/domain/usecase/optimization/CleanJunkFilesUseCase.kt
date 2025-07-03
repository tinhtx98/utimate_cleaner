package com.mars.ultimatecleaner.domain.usecase.optimization

import com.mars.ultimatecleaner.domain.model.CleaningResult
import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import javax.inject.Inject

class CleanJunkFilesUseCase @Inject constructor(
    private val cleaningRepository: CleaningRepository
) {

    suspend operator fun invoke(): CleaningResult {
        return try {
            cleaningRepository.cleanJunkFiles()
        } catch (e: Exception) {
            CleaningResult(
                success = false,
                spaceSaved = 0L,
                filesDeleted = 0,
                error = e.message
            )
        }
    }
}