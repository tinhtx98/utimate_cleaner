package com.mars.ultimatecleaner.domain.usecase

import com.mars.ultimatecleaner.domain.model.RecentActivity
import com.mars.ultimatecleaner.domain.repository.CleaningRepository
import com.mars.ultimatecleaner.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetRecentActivityUseCase @Inject constructor(
    private val cleaningRepository: CleaningRepository,
    private val analyticsRepository: AnalyticsRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<RecentActivity>> {
        return combine(
            cleaningRepository.getRecentCleaningHistory(limit),
            analyticsRepository.getRecentAnalytics(limit)
        ) { cleaningHistory, analytics ->
            val activities = mutableListOf<RecentActivity>()

            // Add cleaning activities
            cleaningHistory.forEach { cleaning ->
                activities.add(
                    RecentActivity(
                        id = cleaning.id,
                        type = RecentActivity.ActivityType.CLEANING,
                        title = "Cleaned ${cleaning.operationType}",
                        description = "Freed up ${formatFileSize(cleaning.spaceSaved)}",
                        timestamp = cleaning.timestamp,
                        icon = getCleaningIcon(cleaning.operationType)
                    )
                )
            }

            // Add analytics activities
            analytics.forEach { analytic ->
                activities.add(
                    RecentActivity(
                        id = analytic.id,
                        type = RecentActivity.ActivityType.ANALYSIS,
                        title = "System Analysis",
                        description = analytic.summary,
                        timestamp = analytic.timestamp,
                        icon = "analysis"
                    )
                )
            }

            activities.sortedByDescending { it.timestamp }.take(limit)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.1f %s".format(size, units[unitIndex])
    }

    private fun getCleaningIcon(operationType: String): String {
        return when (operationType.lowercase()) {
            "cache" -> "cache_clean"
            "junk" -> "junk_clean"
            "duplicate" -> "duplicate_clean"
            "large_files" -> "large_files"
            else -> "clean"
        }
    }
}
