package com.mars.ultimatecleaner.di

import android.content.Context
import androidx.room.Room
import com.mars.ultimatecleaner.data.database.AppDatabase
import com.mars.ultimatecleaner.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideCleaningHistoryDao(database: AppDatabase): CleaningHistoryDao {
        return database.cleaningHistoryDao()
    }

    @Provides
    fun provideFileMetadataDao(database: AppDatabase): FileMetadataDao {
        return database.fileMetadataDao()
    }

    @Provides
    fun provideUserSettingsDao(database: AppDatabase): UserSettingsDao {
        return database.userSettingsDao()
    }

    @Provides
    fun provideAppUsageStatsDao(database: AppDatabase): AppUsageStatsDao {
        return database.appUsageStatsDao()
    }

    @Provides
    fun provideDuplicateGroupsDao(database: AppDatabase): DuplicateGroupsDao {
        return database.duplicateGroupsDao()
    }

    @Provides
    fun provideAnalysisResultsDao(database: AppDatabase): AnalysisResultsDao {
        return database.analysisResultsDao()
    }

    @Provides
    fun provideScheduledTasksDao(database: AppDatabase): ScheduledTasksDao {
        return database.scheduledTasksDao()
    }

    @Provides
    fun provideFileAnalysisDao(database: AppDatabase): FileAnalysisDao {
        return database.fileAnalysisDao()
    }

    @Provides
    fun provideOptimizationDao(database: AppDatabase): OptimizationDao {
        return database.optimizationDao()
    }
}