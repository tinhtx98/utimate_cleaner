package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {

    // App Usage Analytics
    @Query("""
        SELECT 
            DATE(date/1000, 'unixepoch') as day,
            SUM(totalTimeInForeground) as totalUsageTime,
            COUNT(DISTINCT packageName) as uniqueAppsUsed,
            AVG(batteryUsage) as avgBatteryUsage,
            SUM(dataUsageMobile + dataUsageWifi) as totalDataUsage
        FROM app_usage_stats 
        WHERE date >= :fromDate
        GROUP BY day
        ORDER BY day DESC
        LIMIT :limit
    """)
    suspend fun getDailyUsageAnalytics(fromDate: Long, limit: Int): List<DailyUsageAnalytics>

    @Query("""
        SELECT 
            packageName,
            appName,
            SUM(totalTimeInForeground) as totalUsageTime,
            COUNT(*) as usageDays,
            AVG(batteryUsage) as avgBatteryUsage,
            SUM(dataUsageMobile + dataUsageWifi) as totalDataUsage
        FROM app_usage_stats 
        WHERE date >= :fromDate
        GROUP BY packageName
        ORDER BY totalUsageTime DESC
        LIMIT :limit
    """)
    suspend fun getTopAppsAnalytics(fromDate: Long, limit: Int): List<AppUsageAnalytics>

    // Storage Analytics
    @Query("""
        SELECT 
            DATE(timestamp/1000, 'unixepoch') as day,
            AVG(usagePercentage) as avgUsagePercentage,
            AVG(cleanableSpace) as avgCleanableSpace,
            MAX(cleanableSpace) as maxCleanableSpace,
            AVG(totalSpace) as avgTotalSpace
        FROM storage_analysis 
        WHERE timestamp >= :fromDate
        GROUP BY day
        ORDER BY day DESC
        LIMIT :limit
    """)
    suspend fun getDailyStorageAnalytics(fromDate: Long, limit: Int): List<DailyStorageAnalytics>

    // Cleaning Analytics
    @Query("""
        SELECT 
            DATE(timestamp/1000, 'unixepoch') as day,
            COUNT(*) as cleaningCount,
            SUM(space_saved) as totalSpaceSaved,
            AVG(space_saved) as avgSpaceSaved,
            COUNT(DISTINCT operation_type) as uniqueOperations
        FROM cleaning_history 
        WHERE timestamp >= :fromDate
        GROUP BY day
        ORDER BY day DESC
        LIMIT :limit
    """)
    suspend fun getDailyCleaningAnalytics(fromDate: Long, limit: Int): List<DailyCleaningAnalytics>

    @Query("""
        SELECT 
            operation_type,
            COUNT(*) as operationCount,
            SUM(space_saved) as totalSpaceSaved,
            AVG(space_saved) as avgSpaceSaved,
            MAX(space_saved) as maxSpaceSaved
        FROM cleaning_history 
        WHERE timestamp >= :fromDate
        GROUP BY operation_type
        ORDER BY totalSpaceSaved DESC
    """)
    suspend fun getCleaningOperationAnalytics(fromDate: Long): List<CleaningOperationAnalytics>

    // Photo Analytics
    @Query("""
        SELECT 
            DATE(analysisTimestamp/1000, 'unixepoch') as day,
            AVG(totalPhotos) as avgTotalPhotos,
            AVG(blurryPhotosCount) as avgBlurryPhotos,
            AVG(lowQualityPhotosCount) as avgLowQualityPhotos,
            AVG(potentialSpaceSavings) as avgPotentialSavings
        FROM photo_analysis 
        WHERE analysisTimestamp >= :fromDate
        GROUP BY day
        ORDER BY day DESC
        LIMIT :limit
    """)
    suspend fun getDailyPhotoAnalytics(fromDate: Long, limit: Int): List<DailyPhotoAnalytics>

    // Task Execution Analytics
    @Query("""
        SELECT 
            DATE(startTime/1000, 'unixepoch') as day,
            COUNT(*) as totalExecutions,
            COUNT(CASE WHEN executionStatus = 'SUCCESS' THEN 1 END) as successCount,
            COUNT(CASE WHEN executionStatus = 'FAILED' THEN 1 END) as failureCount,
            AVG(executionDuration) as avgExecutionTime,
            SUM(spaceSaved) as totalSpaceSaved
        FROM task_executions 
        WHERE startTime >= :fromDate
        GROUP BY day
        ORDER BY day DESC
        LIMIT :limit
    """)
    suspend fun getDailyTaskAnalytics(fromDate: Long, limit: Int): List<DailyTaskAnalytics>

    // Overall System Analytics
    @Query("""
        SELECT 
            COUNT(DISTINCT packageName) as totalAppsTracked,
            SUM(totalTimeInForeground) as totalUsageTime,
            AVG(batteryUsage) as avgBatteryUsage
        FROM app_usage_stats 
        WHERE date >= :fromDate
    """)
    suspend fun getOverallUsageStats(fromDate: Long): OverallUsageStats?

    @Query("""
        SELECT 
            COUNT(*) as totalCleanings,
            SUM(space_saved) as totalSpaceSaved,
            AVG(space_saved) as avgSpaceSaved,
            COUNT(DISTINCT operation_type) as uniqueOperationTypes
        FROM cleaning_history 
        WHERE timestamp >= :fromDate
    """)
    suspend fun getOverallCleaningStats(fromDate: Long): OverallCleaningStats?

    @Query("""
        SELECT 
            COUNT(*) as totalAnalyses,
            AVG(usagePercentage) as avgStorageUsage,
            SUM(cleanableSpace) as totalCleanableSpace,
            AVG(cleanableSpace) as avgCleanableSpace
        FROM storage_analysis 
        WHERE timestamp >= :fromDate
    """)
    suspend fun getOverallStorageStats(fromDate: Long): OverallStorageStats?

    // Weekly and Monthly aggregations
    @Query("""
        SELECT 
            strftime('%Y-W%W', datetime(date/1000, 'unixepoch')) as week,
            SUM(totalTimeInForeground) as totalUsageTime,
            COUNT(DISTINCT packageName) as uniqueAppsUsed,
            AVG(batteryUsage) as avgBatteryUsage
        FROM app_usage_stats 
        WHERE date >= :fromDate
        GROUP BY week
        ORDER BY week DESC
        LIMIT :limit
    """)
    suspend fun getWeeklyUsageAnalytics(fromDate: Long, limit: Int): List<WeeklyUsageAnalytics>

    @Query("""
        SELECT 
            strftime('%Y-%m', datetime(timestamp/1000, 'unixepoch')) as month,
            COUNT(*) as cleaningCount,
            SUM(space_saved) as totalSpaceSaved,
            AVG(space_saved) as avgSpaceSaved
        FROM cleaning_history 
        WHERE timestamp >= :fromDate
        GROUP BY month
        ORDER BY month DESC
        LIMIT :limit
    """)
    suspend fun getMonthlyCleaningAnalytics(fromDate: Long, limit: Int): List<MonthlyCleaningAnalytics>
}

// Data classes for analytics results
data class DailyUsageAnalytics(
    val day: String,
    val totalUsageTime: Long,
    val uniqueAppsUsed: Int,
    val avgBatteryUsage: Float,
    val totalDataUsage: Long
)

data class AppUsageAnalytics(
    val packageName: String,
    val appName: String,
    val totalUsageTime: Long,
    val usageDays: Int,
    val avgBatteryUsage: Float,
    val totalDataUsage: Long
)

data class DailyStorageAnalytics(
    val day: String,
    val avgUsagePercentage: Float,
    val avgCleanableSpace: Long,
    val maxCleanableSpace: Long,
    val avgTotalSpace: Long
)

data class DailyCleaningAnalytics(
    val day: String,
    val cleaningCount: Int,
    val totalSpaceSaved: Long,
    val avgSpaceSaved: Long,
    val uniqueOperations: Int
)

data class CleaningOperationAnalytics(
    @ColumnInfo(name = "operation_type") val operationType: String,
    val operationCount: Int,
    val totalSpaceSaved: Long,
    val avgSpaceSaved: Long,
    val maxSpaceSaved: Long
)

data class DailyPhotoAnalytics(
    val day: String,
    val avgTotalPhotos: Float,
    val avgBlurryPhotos: Float,
    val avgLowQualityPhotos: Float,
    val avgPotentialSavings: Float
)

data class DailyTaskAnalytics(
    val day: String,
    val totalExecutions: Int,
    val successCount: Int,
    val failureCount: Int,
    val avgExecutionTime: Long,
    val totalSpaceSaved: Long
)

data class OverallUsageStats(
    val totalAppsTracked: Int,
    val totalUsageTime: Long,
    val avgBatteryUsage: Float
)

data class OverallCleaningStats(
    val totalCleanings: Int,
    val totalSpaceSaved: Long,
    val avgSpaceSaved: Long,
    val uniqueOperationTypes: Int
)

data class OverallStorageStats(
    val totalAnalyses: Int,
    val avgStorageUsage: Float,
    val totalCleanableSpace: Long,
    val avgCleanableSpace: Long
)

data class WeeklyUsageAnalytics(
    val week: String,
    val totalUsageTime: Long,
    val uniqueAppsUsed: Int,
    val avgBatteryUsage: Float
)

data class MonthlyCleaningAnalytics(
    val month: String,
    val cleaningCount: Int,
    val totalSpaceSaved: Long,
    val avgSpaceSaved: Long
)
