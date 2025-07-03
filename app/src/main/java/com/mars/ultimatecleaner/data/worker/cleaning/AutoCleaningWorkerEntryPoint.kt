package com.mars.ultimatecleaner.data.worker.cleaning

import com.mars.ultimatecleaner.data.algorithm.FileScanner
import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import com.mars.ultimatecleaner.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutoCleaningWorkerEntryPoint {
    fun cleaningRepository(): CleaningRepository
    fun settingsRepository(): SettingsRepository
    fun fileScanner(): FileScanner
}
