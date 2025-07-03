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

@Database(
    entities = [
        CleaningHistoryEntity::class,
        FileMetadataEntity::class,
        UserSettingsEntity::class,
        AppUsageStatsEntity::class,
        DuplicateGroupsEntity::class,
        AnalysisResultsEntity::class,
        ScheduledTasksEntity::class,
        OptimizationResultEntity::class
    ],
    version = 4,
    exportSchema = true
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
    abstract fun optimizationResultsDao(): OptimizationResultsDao

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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4
                    )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to existing tables
                database.execSQL("ALTER TABLE file_metadata ADD COLUMN analysis_status TEXT NOT NULL DEFAULT 'PENDING'")
                database.execSQL("ALTER TABLE file_metadata ADD COLUMN analysis_result TEXT")

                // Create analysis_results table
                database.execSQL("""
                    CREATE TABLE analysis_results (
                        id TEXT PRIMARY KEY NOT NULL,
                        file_path TEXT NOT NULL,
                        analysis_type TEXT NOT NULL,
                        score REAL NOT NULL,
                        status TEXT NOT NULL,
                        analysis_date INTEGER NOT NULL,
                        result_data TEXT NOT NULL,
                        recommendations TEXT,
                        confidence_level REAL NOT NULL DEFAULT 0.0,
                        processing_time INTEGER NOT NULL DEFAULT 0,
                        algorithm_version TEXT NOT NULL DEFAULT '1.0',
                        error_message TEXT,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Create indices for analysis_results
                database.execSQL("CREATE INDEX index_analysis_results_file_path ON analysis_results(file_path)")
                database.execSQL("CREATE INDEX index_analysis_results_analysis_type ON analysis_results(analysis_type)")
                database.execSQL("CREATE INDEX index_analysis_results_analysis_date ON analysis_results(analysis_date)")
                database.execSQL("CREATE INDEX index_analysis_results_score ON analysis_results(score)")
                database.execSQL("CREATE INDEX index_analysis_results_status ON analysis_results(status)")
            }
        }

        // Migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create scheduled_tasks table
                database.execSQL("""
                    CREATE TABLE scheduled_tasks (
                        id TEXT PRIMARY KEY NOT NULL,
                        task_type TEXT NOT NULL,
                        task_name TEXT NOT NULL,
                        description TEXT,
                        frequency TEXT NOT NULL,
                        frequency_value INTEGER NOT NULL DEFAULT 1,
                        next_execution INTEGER NOT NULL,
                        last_execution INTEGER,
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        parameters TEXT,
                        execution_count INTEGER NOT NULL DEFAULT 0,
                        success_count INTEGER NOT NULL DEFAULT 0,
                        failure_count INTEGER NOT NULL DEFAULT 0,
                        last_result TEXT,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Create indices for scheduled_tasks
                database.execSQL("CREATE INDEX index_scheduled_tasks_task_type ON scheduled_tasks(task_type)")
                database.execSQL("CREATE INDEX index_scheduled_tasks_next_execution ON scheduled_tasks(next_execution)")
                database.execSQL("CREATE INDEX index_scheduled_tasks_status ON scheduled_tasks(status)")
                database.execSQL("CREATE INDEX index_scheduled_tasks_frequency ON scheduled_tasks(frequency)")

                // Add notification-related columns to user_settings
                database.execSQL("ALTER TABLE user_settings ADD COLUMN is_sensitive INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE user_settings ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
            }
        }

        // Migration from version 3 to 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create optimization_results table
                database.execSQL("""
                    CREATE TABLE optimization_results (
                        id TEXT PRIMARY KEY NOT NULL,
                        operation_type TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        files_processed INTEGER NOT NULL,
                        files_optimized INTEGER NOT NULL,
                        space_saved INTEGER NOT NULL,
                        original_size INTEGER NOT NULL,
                        optimized_size INTEGER NOT NULL,
                        compression_ratio REAL NOT NULL DEFAULT 0.0,
                        processing_time INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        error_count INTEGER NOT NULL DEFAULT 0,
                        error_details TEXT,
                        settings_used TEXT,
                        performance_metrics TEXT,
                        created_at INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Create indices for optimization_results
                database.execSQL("CREATE INDEX index_optimization_results_operation_type ON optimization_results(operation_type)")
                database.execSQL("CREATE INDEX index_optimization_results_timestamp ON optimization_results(timestamp)")
                database.execSQL("CREATE INDEX index_optimization_results_space_saved ON optimization_results(space_saved)")
                database.execSQL("CREATE INDEX index_optimization_results_status ON optimization_results(status)")

                // Add performance metrics to app_usage_stats
                database.execSQL("ALTER TABLE app_usage_stats ADD COLUMN performance_metrics TEXT")
            }
        }
    }
}