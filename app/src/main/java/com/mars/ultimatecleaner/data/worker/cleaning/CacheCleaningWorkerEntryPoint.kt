package com.mars.ultimatecleaner.data.worker.cleaning

import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import com.mars.ultimatecleaner.data.utils.FileUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CacheCleaningWorkerEntryPoint {
    fun cleaningRepository(): CleaningRepository
    fun fileUtils(): FileUtils
}
