package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.usage.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageStatsDao {

    // App Usage Stats Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsageStats(stats: AppUsageStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsageStatsList(statsList: List<AppUsageStatsEntity>)

    @Update
    suspend fun updateAppUsageStats(stats: AppUsageStatsEntity)

    @Delete
    suspend fun deleteAppUsageStats(stats: AppUsageStatsEntity)

    @Query("DELETE FROM app_usage_stats WHERE date < :beforeDate")
    suspend fun deleteOldUsageStats(beforeDate: Long)

    // Query Operations
    @Query("SELECT * FROM app_usage_stats WHERE packageName = :packageName ORDER BY date DESC")
    fun getUsageStatsForApp(packageName: String): Flow<List<AppUsageStatsEntity>>

    @Query("SELECT * FROM app_usage_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY totalTimeInForeground DESC")
    fun getUsageStatsInDateRange(startDate: Long, endDate: Long): Flow<List<AppUsageStatsEntity>>

    @Query("SELECT * FROM app_usage_stats WHERE date = :date ORDER BY totalTimeInForeground DESC")
    suspend fun getUsageStatsForDate(date: Long): List<AppUsageStatsEntity>

    @Query("SELECT * FROM app_usage_stats WHERE date >= :fromDate ORDER BY totalTimeInForeground DESC LIMIT :limit")
    suspend fun getTopUsedApps(fromDate: Long, limit: Int): List<AppUsageStatsEntity>

    @Query("SELECT * FROM app_usage_stats WHERE batteryUsage > :minBatteryUsage AND date >= :fromDate ORDER BY batteryUsage DESC")
    suspend fun getBatteryDrainingApps(minBatteryUsage: Float, fromDate: Long): List<AppUsageStatsEntity>

    @Query("SELECT * FROM app_usage_stats WHERE (dataUsageMobile + dataUsageWifi) > :minDataUsage AND date >= :fromDate ORDER BY (dataUsageMobile + dataUsageWifi) DESC")
    suspend fun getDataHungryApps(minDataUsage: Long, fromDate: Long): List<AppUsageStatsEntity>

    @Query("SELECT AVG(totalTimeInForeground) FROM app_usage_stats WHERE packageName = :packageName AND date >= :fromDate")
    suspend fun getAverageUsageTime(packageName: String, fromDate: Long): Long?

    @Query("SELECT SUM(totalTimeInForeground) FROM app_usage_stats WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalUsageTime(startDate: Long, endDate: Long): Long?

    @Query("SELECT COUNT(DISTINCT packageName) FROM app_usage_stats WHERE date >= :fromDate")
    suspend fun getUniqueAppsUsedCount(fromDate: Long): Int

    // App Sessions Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSession(session: AppSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSessions(sessions: List<AppSessionEntity>)

    @Query("SELECT * FROM app_sessions WHERE packageName = :packageName AND startTime >= :fromTime ORDER BY startTime DESC")
    fun getAppSessions(packageName: String, fromTime: Long): Flow<List<AppSessionEntity>>

    @Query("SELECT * FROM app_sessions WHERE startTime BETWEEN :startTime AND :endTime ORDER BY sessionDuration DESC")
    suspend fun getSessionsInRange(startTime: Long, endTime: Long): List<AppSessionEntity>

    @Query("SELECT * FROM app_sessions WHERE sessionDuration > :minDuration ORDER BY sessionDuration DESC LIMIT :limit")
    suspend fun getLongestSessions(minDuration: Long, limit: Int): List<AppSessionEntity>

    @Query("DELETE FROM app_sessions WHERE startTime < :beforeTime")
    suspend fun deleteOldSessions(beforeTime: Long)

    // App Performance Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppPerformance(performance: AppPerformanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppPerformanceList(performanceList: List<AppPerformanceEntity>)

    @Query("SELECT * FROM app_performance WHERE packageName = :packageName ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getAppPerformanceHistory(packageName: String, limit: Int): List<AppPerformanceEntity>

    @Query("SELECT * FROM app_performance WHERE timestamp >= :fromTime ORDER BY performanceScore ASC")
    suspend fun getPoorPerformingApps(fromTime: Long): List<AppPerformanceEntity>

    @Query("SELECT AVG(performanceScore) FROM app_performance WHERE packageName = :packageName AND timestamp >= :fromTime")
    suspend fun getAveragePerformanceScore(packageName: String, fromTime: Long): Float?

    @Query("SELECT * FROM app_performance WHERE crashCount > 0 AND timestamp >= :fromTime ORDER BY crashCount DESC")
    suspend fun getAppsWithCrashes(fromTime: Long): List<AppPerformanceEntity>

    @Query("DELETE FROM app_performance WHERE timestamp < :beforeTime")
    suspend fun deleteOldPerformanceData(beforeTime: Long)

    // Complex Analytics Queries
    @Query("""
        SELECT packageName, appName, SUM(totalTimeInForeground) as totalUsage, 
               AVG(batteryUsage) as avgBatteryUsage, SUM(dataUsageMobile + dataUsageWifi) as totalDataUsage
        FROM app_usage_stats 
        WHERE date >= :fromDate 
        GROUP BY packageName 
        ORDER BY totalUsage DESC 
        LIMIT :limit
    """)
    suspend fun getAppUsageSummary(fromDate: Long, limit: Int): List<AppUsageSummary>

    @Query("""
        SELECT strftime('%Y-%m-%d', datetime(date/1000, 'unixepoch')) as day,
               SUM(totalTimeInForeground) as totalUsage,
               COUNT(DISTINCT packageName) as uniqueApps
        FROM app_usage_stats 
        WHERE date >= :fromDate
        GROUP BY day
        ORDER BY day DESC
    """)
    suspend fun getDailyUsageTrends(fromDate: Long): List<DailyUsageTrend>

    @Transaction
    suspend fun cleanupOldData(daysToKeep: Int) {
        val cutoffDate = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        deleteOldUsageStats(cutoffDate)
        deleteOldSessions(cutoffDate)
        deleteOldPerformanceData(cutoffDate)
    }

    // Additional methods for AnalyticsRepository compatibility
    @Query("SELECT COUNT(*) FROM app_sessions WHERE startTime >= :fromTime AND startTime <= :toTime")
    suspend fun getSessionCount(fromTime: Long, toTime: Long): Int

    @Query("SELECT AVG(sessionDuration) FROM app_sessions WHERE startTime >= :fromTime AND startTime <= :toTime")
    suspend fun getAverageSessionDuration(fromTime: Long, toTime: Long): Long?
}

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val totalUsage: Long,
    val avgBatteryUsage: Float,
    val totalDataUsage: Long
)

data class DailyUsageTrend(
    val day: String,
    val totalUsage: Long,
    val uniqueApps: Int
)