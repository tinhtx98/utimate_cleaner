package com.mars.ultimatecleaner.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.mars.ultimatecleaner.data.database.converter.TypeConverters as AppTypeConverters
import com.mars.ultimatecleaner.data.database.dao.*
import com.mars.ultimatecleaner.data.database.entity.*
import com.mars.ultimatecleaner.data.database.entity.usage.AppUsageStatsEntity
import com.mars.ultimatecleaner.data.database.entity.usage.AppSessionEntity
import com.mars.ultimatecleaner.data.database.entity.usage.AppPerformanceEntity
import com.mars.ultimatecleaner.data.database.entity.analysis.StorageAnalysisEntity
import com.mars.ultimatecleaner.data.database.entity.analysis.PhotoAnalysisEntity
import com.mars.ultimatecleaner.data.database.entity.tasks.TaskExecutionEntity

@Database(
    entities = [
        CleaningHistoryEntity::class,
        FileMetadataEntity::class,
        UserSettingsEntity::class,
        AppUsageStatsEntity::class,
        DuplicateGroupsEntity::class,
        AnalysisResultsEntity::class,
        ScheduledTasksEntity::class,
        FileAnalysisEntity::class,
        OptimizationScheduleEntity::class,
        AppSessionEntity::class,
        AppPerformanceEntity::class,
        StorageAnalysisEntity::class,
        PhotoAnalysisEntity::class,
        TaskExecutionEntity::class,
        OptimizationResultLegacyEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cleaningHistoryDao(): CleaningHistoryDao
    abstract fun fileMetadataDao(): FileMetadataDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun appUsageStatsDao(): AppUsageStatsDao
    abstract fun duplicateGroupsDao(): DuplicateGroupsDao
    abstract fun analysisResultsDao(): AnalysisResultsDao
    abstract fun scheduledTasksDao(): ScheduledTasksDao
    abstract fun fileAnalysisDao(): FileAnalysisDao
    abstract fun optimizationDao(): OptimizationDao

    companion object {
        private const val DATABASE_NAME = "ultimate_cleaner_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}