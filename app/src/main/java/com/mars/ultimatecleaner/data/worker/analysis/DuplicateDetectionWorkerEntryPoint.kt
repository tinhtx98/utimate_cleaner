package com.mars.ultimatecleaner.data.worker.analysis

import com.mars.ultimatecleaner.data.algorithm.DuplicateDetector
import com.mars.ultimatecleaner.domain.repository.FileRepository
import com.mars.ultimatecleaner.domain.repository.OptimizerRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DuplicateDetectionWorkerEntryPoint {
    fun duplicateDetector(): DuplicateDetector
    fun fileRepository(): FileRepository
    fun optimizerRepository(): OptimizerRepository
}
