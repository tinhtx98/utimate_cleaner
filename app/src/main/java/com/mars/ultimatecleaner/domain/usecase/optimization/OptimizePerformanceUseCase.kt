package com.mars.ultimatecleaner.domain.usecase.optimization

import com.mars.ultimatecleaner.domain.model.OptimizationType
import com.mars.ultimatecleaner.domain.model.PerformanceOptimizationResult
import com.mars.ultimatecleaner.domain.repository.OptimizerRepository
import javax.inject.Inject

class OptimizePerformanceUseCase @Inject constructor(
    private val optimizerRepository: OptimizerRepository
) {

    suspend operator fun invoke(type: OptimizationType): PerformanceOptimizationResult {
        return try {
            optimizerRepository.optimizePerformance(type)
        } catch (e: Exception) {
            PerformanceOptimizationResult(
                type = type,
                success = false,
                error = e.message ?: "Unknown error occurred",
                duration = 0L
            )
        }
    }
}