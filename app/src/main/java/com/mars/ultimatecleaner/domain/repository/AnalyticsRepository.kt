package com.mars.ultimatecleaner.domain.repository

import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.domain.usecase.CleaningOperation

interface AnalyticsRepository {
    suspend fun trackFeatureClick(feature: String)
    suspend fun trackCleaningOperation(operation: CleaningOperation)
    suspend fun trackFileOperation(operation: FileOperation)
    suspend fun trackError(error: String, context: String)
    suspend fun getUsageAnalytics(): UsageAnalyticsDomain
    suspend fun getCleaningStatistics(): CleaningStatistics
    suspend fun recordPerformanceMetric(metric: PerformanceMetric)
    suspend fun getUserBehaviorData(): UserBehaviorData
}