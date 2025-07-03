package com.mars.ultimatecleaner.di

import com.mars.ultimatecleaner.data.repository.*
import com.mars.ultimatecleaner.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(
        fileRepositoryImpl: FileRepositoryImpl
    ): FileRepository

    @Binds
    @Singleton
    abstract fun bindCleaningRepository(
        cleaningRepositoryImpl: CleaningRepositoryImpl
    ): CleaningRepository

    @Binds
    @Singleton
    abstract fun bindOptimizerRepository(
        optimizerRepositoryImpl: OptimizerRepositoryImpl
    ): OptimizerRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(
        permissionRepositoryImpl: PermissionRepositoryImpl
    ): PermissionRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        analyticsRepositoryImpl: AnalyticsRepositoryImpl
    ): AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindStorageRepository(
        storageRepositoryImpl: StorageRepositoryImpl
    ): StorageRepository

    @Binds
    @Singleton
    abstract fun bindSystemHealthRepository(
        systemHealthRepositoryImpl: SystemHealthRepositoryImpl
    ): SystemHealthRepository
}