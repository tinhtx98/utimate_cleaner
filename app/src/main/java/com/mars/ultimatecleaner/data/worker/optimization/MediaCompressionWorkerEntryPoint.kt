package com.mars.ultimatecleaner.data.worker.optimization

import com.mars.ultimatecleaner.core.compression.CompressionEngine
import com.mars.ultimatecleaner.domain.repository.OptimizerRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MediaCompressionWorkerEntryPoint {
    fun compressionEngine(): CompressionEngine
    fun optimizerRepository(): OptimizerRepository
}
