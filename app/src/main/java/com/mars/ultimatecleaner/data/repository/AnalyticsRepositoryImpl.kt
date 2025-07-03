package com.mars.ultimatecleaner.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.mars.ultimatecleaner.data.database.dao.AnalyticsDao
import com.mars.ultimatecleaner.data.database.dao.AppUsageStatsDao
import com.mars.ultimatecleaner.data.database.entity.analytics.*
import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.domain.repository.AnalyticsRepository
import com.mars.ultimatecleaner.domain.usecase.CleaningOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val context: Context,
    private val analyticsDao: AnalyticsDao,
    private val appUsageStatsDao: AppUsageStatsDao,
    private val sharedPreferences: SharedPreferences
) : AnalyticsRepository {

    override suspend fun trackFeatureClick(feature: String) {
        withContext(Dispatchers.IO) {
            try {
                val featureClick = FeatureClickEntity(
                    feature = feature,
                    timestamp = System.currentTimeMillis(),
                    sessionId = getCurrentSessionId()
                )
                analyticsDao.insertFeatureClick(featureClick)

                // Update feature usage count
                val currentCount = sharedPreferences.getInt("feature_${feature}_count", 0)
                sharedPreferences.edit()
                    .putInt("feature_${feature}_count", currentCount + 1)
                    .apply()

            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }
    }

    override suspend fun trackCleaningOperation(operation: CleaningOperation) {
        withContext(Dispatchers.IO) {
            try {
                val cleaningEvent = CleaningEventEntity(
                    operationType = operation.type,
                    filesProcessed = operation.filesProcessed,
                    spaceSaved = operation.spaceSaved,
                    duration = operation.duration,
                    timestamp = System.currentTimeMillis(),
                    success = operation.success,
                    errorMessage = operation.errorMessage
                )
                analyticsDao.insertCleaningEvent(cleaningEvent)

                // Update cleaning statistics
                updateCleaningStats(operation)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        TODO("Not yet implemented")
    }

    override suspend fun trackFileOperation(operation: FileOperation) {
        withContext(Dispatchers.IO) {
            try {
                val fileEvent = FileOperationEntity(
                    operationType = operation.type,
                    fileCount = operation.fileCount,
                    totalSize = operation.totalSize,
                    sourcePath = operation.sourcePath,
                    destinationPath = operation.destinationPath,
                    timestamp = System.currentTimeMillis(),
                    success = operation.success,
                    errorMessage = operation.errorMessage
                )
                analyticsDao.insertFileOperation(fileEvent)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun trackError(error: String, context: String) {
        withContext(Dispatchers.IO) {
            try {
                val errorEvent = ErrorEventEntity(
                    errorMessage = error,
                    errorContext = context,
                    timestamp = System.currentTimeMillis(),
                    stackTrace = Thread.currentThread().stackTrace.contentToString()
                )
                analyticsDao.insertErrorEvent(errorEvent)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getUsageAnalytics(): UsageAnalyticsDomain {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val dayAgo = now - 24 * 60 * 60 * 1000L
                val weekAgo = now - 7 * 24 * 60 * 60 * 1000L
                val monthAgo = now - 30 * 24 * 60 * 60 * 1000L

                val dailyFeatureClicks = analyticsDao.getFeatureClicksInPeriod(dayAgo, now)
                val weeklyFeatureClicks = analyticsDao.getFeatureClicksInPeriod(weekAgo, now)
                val monthlyFeatureClicks = analyticsDao.getFeatureClicksInPeriod(monthAgo, now)

                val totalSessions = appUsageStatsDao.getSessionCount(monthAgo, now)
                val avgSessionDuration = appUsageStatsDao.getAverageSessionDuration(monthAgo, now)

                UsageAnalyticsDomain(
                    totalSessions = totalSessions,
                    averageSessionDuration = avgSessionDuration,
                    dailyActiveFeatures = dailyFeatureClicks.size,
                    weeklyActiveFeatures = weeklyFeatureClicks.size,
                    monthlyActiveFeatures = monthlyFeatureClicks.size,
                    mostUsedFeatures = getMostUsedFeatures(monthlyFeatureClicks),
                    userEngagementScore = calculateEngagementScore(totalSessions, avgSessionDuration, monthlyFeatureClicks.size)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                UsageAnalyticsDomain.getDefault()
            }
        }
    }

    override suspend fun getCleaningStatistics(): CleaningStatistics {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val monthAgo = now - 30 * 24 * 60 * 60 * 1000L

                val cleaningEvents = analyticsDao.getCleaningEventsInPeriod(monthAgo, now)
                val totalSpaceSaved = cleaningEvents.sumOf { it.spaceSaved }
                val totalFilesProcessed = cleaningEvents.sumOf { it.filesProcessed }
                val successfulOperations = cleaningEvents.count { it.success }
                val totalOperations = cleaningEvents.size

                CleaningStatistics(
                    totalSpaceSaved = totalSpaceSaved,
                    totalFilesProcessed = totalFilesProcessed,
                    totalOperations = totalOperations,
                    successfulOperations = successfulOperations,
                    failedOperations = totalOperations - successfulOperations,
                    successRate = if (totalOperations > 0) (successfulOperations.toFloat() / totalOperations) * 100 else 0f,
                    averageSpaceSavedPerOperation = if (totalOperations > 0) totalSpaceSaved / totalOperations else 0L,
                    operationsByType = cleaningEvents.groupBy { it.operationType }
                        .mapValues { it.value.size }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                CleaningStatistics.getDefault()
            }
        }
    }

    override suspend fun recordPerformanceMetric(metric: PerformanceMetric) {
        withContext(Dispatchers.IO) {
            try {
                val performanceEvent = PerformanceMetricEntity(
                    metricType = metric.type,
                    metricValue = metric.value,
                    timestamp = System.currentTimeMillis(),
                    context = metric.context
                )
                analyticsDao.insertPerformanceMetric(performanceEvent)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getUserBehaviorData(): UserBehaviorData {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val monthAgo = now - 30 * 24 * 60 * 60 * 1000L

                val featureClicks = analyticsDao.getFeatureClicksInPeriod(monthAgo, now)
                val cleaningEvents = analyticsDao.getCleaningEventsInPeriod(monthAgo, now)
                val fileOperations = analyticsDao.getFileOperationsInPeriod(monthAgo, now)

                UserBehaviorData(
                    preferredFeatures = featureClicks.groupBy { it.feature }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(5)
                        .map { it.first },
                    cleaningFrequency = calculateCleaningFrequency(cleaningEvents),
                    fileManagementPatterns = analyzeFileManagementPatterns(fileOperations),
                    peakUsageHours = calculatePeakUsageHours(featureClicks),
                    averageCleaningInterval = calculateAverageCleaningInterval(cleaningEvents),
                    userType = determineUserType(featureClicks, cleaningEvents, fileOperations)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                UserBehaviorData.getDefault()
            }
        }
    }

    private fun getCurrentSessionId(): String {
        return sharedPreferences.getString("current_session_id", "default_session") ?: "default_session"
    }

    private fun updateCleaningStats(operation: CleaningOperation) {
        val totalCleaned = sharedPreferences.getLong("total_space_cleaned", 0L)
        val totalFiles = sharedPreferences.getInt("total_files_cleaned", 0)

        sharedPreferences.edit()
            .putLong("total_space_cleaned", totalCleaned + operation.spaceSaved)
            .putInt("total_files_cleaned", totalFiles + operation.filesProcessed)
            .apply()
    }

    private fun getMostUsedFeatures(clicks: List<FeatureClickEntity>): List<String> {
        return clicks.groupBy { it.feature }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }

    private fun calculateEngagementScore(sessions: Int, avgDuration: Long, activeFeatures: Int): Float {
        // Simple engagement score calculation
        val sessionScore = minOf(sessions.toFloat() / 30f, 1f) * 40f // Max 40 points
        val durationScore = minOf(avgDuration.toFloat() / (5 * 60 * 1000f), 1f) * 30f // Max 30 points
        val featureScore = minOf(activeFeatures.toFloat() / 10f, 1f) * 30f // Max 30 points

        return sessionScore + durationScore + featureScore
    }

    private fun calculateCleaningFrequency(events: List<CleaningEventEntity>): String {
        if (events.isEmpty()) return "Never"

        val now = System.currentTimeMillis()
        val daysSinceLastCleaning = (now - events.maxOf { it.timestamp }) / (24 * 60 * 60 * 1000)

        return when {
            daysSinceLastCleaning <= 1 -> "Daily"
            daysSinceLastCleaning <= 7 -> "Weekly"
            daysSinceLastCleaning <= 30 -> "Monthly"
            else -> "Rarely"
        }
    }

    private fun analyzeFileManagementPatterns(operations: List<FileOperationEntity>): List<String> {
        val patterns = mutableListOf<String>()

        val operationTypes = operations.groupBy { it.operationType }
        val mostCommonOperation = operationTypes.maxByOrNull { it.value.size }?.key

        if (mostCommonOperation != null) {
            patterns.add("Prefers $mostCommonOperation operations")
        }

        val largeFileOperations = operations.filter { it.totalSize > 100 * 1024 * 1024 }
        if (largeFileOperations.isNotEmpty()) {
            patterns.add("Frequently works with large files")
        }

        return patterns
    }

    private fun calculatePeakUsageHours(clicks: List<FeatureClickEntity>): List<Int> {
        val hours = clicks.map {
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = it.timestamp
            calendar.get(java.util.Calendar.HOUR_OF_DAY)
        }

        return hours.groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }

    private fun calculateAverageCleaningInterval(events: List<CleaningEventEntity>): Long {
        if (events.size < 2) return 0L

        val sortedEvents = events.sortedBy { it.timestamp }
        val intervals = mutableListOf<Long>()

        for (i in 1 until sortedEvents.size) {
            intervals.add(sortedEvents[i].timestamp - sortedEvents[i-1].timestamp)
        }

        return intervals.average().toLong()
    }

    private fun determineUserType(
        clicks: List<FeatureClickEntity>,
        cleaningEvents: List<CleaningEventEntity>,
        fileOperations: List<FileOperationEntity>
    ): String {
        val totalActivity = clicks.size + cleaningEvents.size + fileOperations.size

        return when {
            totalActivity < 10 -> "Casual User"
            totalActivity < 50 -> "Regular User"
            totalActivity < 100 -> "Power User"
            else -> "Expert User"
        }
    }
}