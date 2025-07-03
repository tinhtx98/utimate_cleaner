package com.mars.ultimatecleaner.data.notification.content

import android.content.Context
import com.mars.ultimatecleaner.R
import com.mars.ultimatecleaner.data.notification.model.NotificationContent
import com.mars.ultimatecleaner.data.notification.model.NotificationTemplate
import com.mars.ultimatecleaner.domain.model.StorageInfoDomain
import com.mars.ultimatecleaner.domain.repository.StorageRepository
import com.mars.ultimatecleaner.domain.repository.SystemHealthRepository
import com.mars.ultimatecleaner.domain.usecase.GetDeviceHealthUseCase
import com.mars.ultimatecleaner.domain.usecase.optimization.GetDeviceHealthUseCase
import com.mars.ultimatecleaner.ui.main.DeviceHealth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationContentManager @Inject constructor(
    private val context: Context,
    private val storageRepository: StorageRepository,
    private val systemHealthRepository: SystemHealthRepository,
    private val getDeviceHealthUseCase: GetDeviceHealthUseCase
) {

    companion object {
        private const val MAX_TITLE_LENGTH = 50
        private const val MAX_TEXT_LENGTH = 120
    }

    suspend fun generateMorningNotification(): NotificationContent = withContext(Dispatchers.IO) {
        val deviceHealth = try {
            getDeviceHealthUseCase()
        } catch (e: Exception) {
            null
        }

        val storageInfo = try {
            storageRepository.getStorageInfo()
        } catch (e: Exception) {
            null
        }

        val template = selectMorningTemplate(deviceHealth?.overallScore, storageInfo?.usagePercentage)

        NotificationContent(
            title = generateTitle(template, deviceHealth, storageInfo),
            text = generateText(template, deviceHealth, storageInfo),
            bigText = generateBigText(template, deviceHealth, storageInfo),
            icon = getNotificationIcon(deviceHealth?.overallScore, storageInfo?.usagePercentage),
            actions = generateQuickActions("morning"),
            priority = getNotificationPriority(deviceHealth?.overallScore, storageInfo?.usagePercentage)
        )
    }

    suspend fun generateEveningNotification(): NotificationContent = withContext(Dispatchers.IO) {
        val deviceHealth = try {
            getDeviceHealthUseCase()
        } catch (e: Exception) {
            null
        }

        val storageInfo = try {
            storageRepository.getStorageInfo()
        } catch (e: Exception) {
            null
        }

        val template = selectEveningTemplate(deviceHealth?.overallScore, storageInfo?.usagePercentage)

        NotificationContent(
            title = generateTitle(template, deviceHealth, storageInfo),
            text = generateText(template, deviceHealth, storageInfo),
            bigText = generateBigText(template, deviceHealth, storageInfo),
            icon = getNotificationIcon(deviceHealth?.overallScore, storageInfo?.usagePercentage),
            actions = generateQuickActions("evening"),
            priority = getNotificationPriority(deviceHealth?.overallScore, storageInfo?.usagePercentage)
        )
    }

    private fun selectMorningTemplate(healthScore: Int?, storagePercentage: Float?): NotificationTemplate {
        return when {
            // Critical issues
            healthScore != null && healthScore < 40 -> getMorningTemplates().critical.random()
            storagePercentage != null && storagePercentage > 90 -> getMorningTemplates().storageCritical.random()

            // Warning conditions
            healthScore != null && healthScore < 70 -> getMorningTemplates().warning.random()
            storagePercentage != null && storagePercentage > 80 -> getMorningTemplates().storageWarning.random()

            // Good condition
            healthScore != null && healthScore >= 80 -> getMorningTemplates().healthy.random()

            // Default motivational
            else -> getMorningTemplates().motivational.random()
        }
    }

    private fun selectEveningTemplate(healthScore: Int?, storagePercentage: Float?): NotificationTemplate {
        return when {
            // Critical issues
            healthScore != null && healthScore < 40 -> getEveningTemplates().critical.random()
            storagePercentage != null && storagePercentage > 90 -> getEveningTemplates().storageCritical.random()

            // Warning conditions
            healthScore != null && healthScore < 70 -> getEveningTemplates().warning.random()
            storagePercentage != null && storagePercentage > 80 -> getEveningTemplates().storageWarning.random()

            // Good condition
            healthScore != null && healthScore >= 80 -> getEveningTemplates().healthy.random()

            // Default maintenance
            else -> getEveningTemplates().maintenance.random()
        }
    }

    private fun generateTitle(
        template: NotificationTemplate,
        deviceHealth: DeviceHealth?,
        storageInfoDomain: StorageInfoDomain?
    ): String {
        var title = template.title

        // Replace placeholders
        deviceHealth?.let { health ->
            title = title.replace("{health_score}", health.overallScore.toString())
        }

        storageInfoDomain?.let { storage ->
            title = title.replace("{storage_percentage}", storage.usagePercentage.toInt().toString())
            title = title.replace("{free_space}", formatFileSize(storage.freeSpace))
            title = title.replace("{junk_count}", storage.junkFilesCount.toString())
        }

        return title.take(MAX_TITLE_LENGTH)
    }

    private fun generateText(
        template: NotificationTemplate,
        deviceHealth: DeviceHealth?,
        storageInfoDomain: com.mars.ultimatecleaner.domain.model.StorageInfoDomain?
    ): String {
        var text = template.text

        // Replace placeholders
        deviceHealth?.let { health ->
            text = text.replace("{health_score}", health.overallScore.toString())
            text = text.replace("{health_status}", getHealthStatus(health.overallScore))
        }

        storageInfoDomain?.let { storage ->
            text = text.replace("{storage_percentage}", storage.usagePercentage.toInt().toString())
            text = text.replace("{free_space}", formatFileSize(storage.freeSpace))
            text = text.replace("{junk_size}", formatFileSize(storage.junkFilesSize))
            text = text.replace("{junk_count}", storage.junkFilesCount.toString())
        }

        return text.take(MAX_TEXT_LENGTH)
    }

    private fun generateBigText(
        template: NotificationTemplate,
        deviceHealth: DeviceHealth?,
        storageInfoDomain: StorageInfoDomain?
    ): String {
        val insights = mutableListOf<String>()

        deviceHealth?.let { health ->
            insights.add("Device Health: ${health.overallScore}% (${getHealthStatus(health.overallScore)})")
        }

        storageInfoDomain?.let { storage ->
            insights.add("Storage: ${storage.usagePercentage.toInt()}% used (${formatFileSize(storage.freeSpace)} free)")
            if (storage.junkFilesCount > 0) {
                insights.add("Found ${storage.junkFilesCount} junk files (${formatFileSize(storage.junkFilesSize)})")
            }
        }

        val recommendations = generateRecommendations(deviceHealth, storageInfoDomain)
        if (recommendations.isNotEmpty()) {
            insights.add("\nRecommendations:")
            insights.addAll(recommendations.map { "• $it" })
        }

        return insights.joinToString("\n")
    }

    private fun generateRecommendations(
        deviceHealth: DeviceHealth?,
        storageInfoDomain: StorageInfoDomain?
    ): List<String> {
        val recommendations = mutableListOf<String>()

        storageInfoDomain?.let { storage ->
            when {
                storage.usagePercentage > 90 -> {
                    recommendations.add("Urgently clean ${formatFileSize(storage.junkFilesSize)} of junk files")
                    recommendations.add("Consider moving photos/videos to cloud storage")
                }
                storage.usagePercentage > 80 -> {
                    recommendations.add("Clean up ${storage.junkFilesCount} junk files")
                    recommendations.add("Review large files and duplicates")
                }
                storage.junkFilesCount > 100 -> {
                    recommendations.add("Clean ${storage.junkFilesCount} junk files")
                }
            }
        }

        deviceHealth?.let { health ->
            when {
                health.overallScore < 50 -> {
                    recommendations.add("Run comprehensive device optimization")
                    recommendations.add("Check for app performance issues")
                }
                health.overallScore < 80 -> {
                    recommendations.add("Perform quick optimization")
                }
            }
        }

        return recommendations.take(3) // Limit to 3 recommendations
    }

    private fun generateQuickActions(timeOfDay: String): List<Pair<String, String>> {
        return when (timeOfDay) {
            "morning" -> listOf(
                "Quick Clean" to "ACTION_QUICK_CLEAN",
                "Check Status" to "ACTION_OPEN_APP"
            )
            "evening" -> listOf(
                "Deep Clean" to "ACTION_DEEP_CLEAN",
                "View Report" to "ACTION_VIEW_REPORT"
            )
            else -> listOf(
                "Open App" to "ACTION_OPEN_APP"
            )
        }
    }

    private fun getNotificationIcon(healthScore: Int?, storagePercentage: Float?): Int {
        return when {
            /*healthScore != null && healthScore < 40 -> R.drawable.ic_notification_critical
            storagePercentage != null && storagePercentage > 90 -> R.drawable.ic_notification_storage_full
            healthScore != null && healthScore < 70 -> R.drawable.ic_notification_warning
            storagePercentage != null && storagePercentage > 80 -> R.drawable.ic_notification_storage_warning
            else -> R.drawable.ic_notification_healthy*/
            healthScore != null && healthScore < 40 -> R.drawable.ic_launcher_foreground
            storagePercentage != null && storagePercentage > 90 -> R.drawable.ic_launcher_foreground
            healthScore != null && healthScore < 70 -> R.drawable.ic_launcher_foreground
            storagePercentage != null && storagePercentage > 80 -> R.drawable.ic_launcher_foreground
            else -> R.drawable.ic_launcher_foreground
        }
    }

    private fun getNotificationPriority(healthScore: Int?, storagePercentage: Float?): Int {
        return when {
            healthScore != null && healthScore < 40 -> android.app.Notification.PRIORITY_HIGH
            storagePercentage != null && storagePercentage > 90 -> android.app.Notification.PRIORITY_HIGH
            healthScore != null && healthScore < 70 -> android.app.Notification.PRIORITY_DEFAULT
            storagePercentage != null && storagePercentage > 80 -> android.app.Notification.PRIORITY_DEFAULT
            else -> android.app.Notification.PRIORITY_LOW
        }
    }

    private fun getHealthStatus(score: Int): String {
        return when {
            score >= 80 -> "Excellent"
            score >= 60 -> "Good"
            score >= 40 -> "Fair"
            else -> "Needs Attention"
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

    private fun getMorningTemplates(): MorningTemplates {
        return MorningTemplates(
            critical = listOf(
                NotificationTemplate(
                    "🚨 Device Health Critical!",
                    "Your device health is {health_score}%. Immediate optimization needed!"
                ),
                NotificationTemplate(
                    "⚠️ Performance Alert!",
                    "Device running slow. Health score: {health_score}%. Quick fix available!"
                ),
                NotificationTemplate(
                    "🔧 Urgent Maintenance",
                    "Critical issues detected! Health: {health_score}%. Tap to fix now."
                )
            ),
            storageCritical = listOf(
                NotificationTemplate(
                    "📱 Storage Almost Full!",
                    "Only {free_space} left! {storage_percentage}% used. Clean now!"
                ),
                NotificationTemplate(
                    "🗄️ Critical Storage Alert",
                    "Storage {storage_percentage}% full! Risk of app crashes. Clean immediately!"
                ),
                NotificationTemplate(
                    "⚡ Emergency Cleanup Needed",
                    "Storage critically low: {free_space} free. Tap for instant cleanup!"
                )
            ),
            warning = listOf(
                NotificationTemplate(
                    "🌅 Morning Health Check",
                    "Device health: {health_score}%. Some optimization recommended."
                ),
                NotificationTemplate(
                    "📊 Performance Update",
                    "Health score dropped to {health_score}%. Quick tune-up available!"
                ),
                NotificationTemplate(
                    "🔄 Optimization Reminder",
                    "Health: {health_score}%. A few minutes of cleanup will help!"
                )
            ),
            storageWarning = listOf(
                NotificationTemplate(
                    "📦 Storage Getting Full",
                    "Storage {storage_percentage}% used. Found {junk_count} files to clean."
                ),
                NotificationTemplate(
                    "🧹 Cleanup Opportunity",
                    "{junk_size} of junk files found. Free up space now!"
                ),
                NotificationTemplate(
                    "📈 Storage Optimization",
                    "{storage_percentage}% storage used. Clean {junk_count} junk files?"
                )
            ),
            healthy = listOf(
                NotificationTemplate(
                    "✨ Device Looking Great!",
                    "Health: {health_score}%. Keep up the good maintenance!"
                ),
                NotificationTemplate(
                    "🎉 Excellent Performance",
                    "Health score: {health_score}%! Your device is optimized."
                ),
                NotificationTemplate(
                    "💪 Peak Performance",
                    "Health: {health_score}%. Running smooth as silk!"
                )
            ),
            motivational = listOf(
                NotificationTemplate(
                    "🌅 Good Morning!",
                    "Start fresh! Quick device check to boost your day."
                ),
                NotificationTemplate(
                    "☀️ Rise & Optimize",
                    "Good morning! A clean device for a productive day ahead."
                ),
                NotificationTemplate(
                    "🚀 Morning Boost",
                    "Ready to conquer the day? Let's optimize your device first!"
                )
            )
        )
    }

    private fun getEveningTemplates(): EveningTemplates {
        return EveningTemplates(
            critical = listOf(
                NotificationTemplate(
                    "🌙 End Day Cleanup Critical",
                    "Health: {health_score}%. Don't sleep on these issues!"
                ),
                NotificationTemplate(
                    "🔴 Evening Alert",
                    "Critical health: {health_score}%. Fix before tomorrow!"
                ),
                NotificationTemplate(
                    "⚠️ Bedtime Maintenance",
                    "Device health {health_score}%. Urgent care needed tonight!"
                )
            ),
            storageCritical = listOf(
                NotificationTemplate(
                    "🌜 Storage Crisis",
                    "Only {free_space} left! Clean before bed to avoid issues."
                ),
                NotificationTemplate(
                    "📱 Evening Storage Alert",
                    "{storage_percentage}% full! Free up space for tomorrow."
                ),
                NotificationTemplate(
                    "🗑️ Critical Cleanup Time",
                    "Storage almost full! {junk_count} files ready for cleanup."
                )
            ),
            warning = listOf(
                NotificationTemplate(
                    "🌆 Evening Tune-Up",
                    "Health: {health_score}%. Perfect time for optimization!"
                ),
                NotificationTemplate(
                    "🔧 Pre-Sleep Maintenance",
                    "Health {health_score}%. Quick cleanup for better tomorrow!"
                ),
                NotificationTemplate(
                    "📊 End-of-Day Check",
                    "Health score: {health_score}%. Ready for evening optimization?"
                )
            ),
            storageWarning = listOf(
                NotificationTemplate(
                    "🌃 Evening Cleanup",
                    "Storage {storage_percentage}% full. Clean {junk_count} files tonight?"
                ),
                NotificationTemplate(
                    "🧽 Bedtime Cleaning",
                    "Found {junk_size} to clean. Perfect time for maintenance!"
                ),
                NotificationTemplate(
                    "📦 Storage Check-In",
                    "{storage_percentage}% used. {junk_count} junk files found."
                )
            ),
            healthy = listOf(
                NotificationTemplate(
                    "✨ Evening Excellence",
                    "Health: {health_score}%! Your device is in great shape."
                ),
                NotificationTemplate(
                    "🌟 Perfect Performance",
                    "Health {health_score}%. Excellent maintenance job!"
                ),
                NotificationTemplate(
                    "💎 Optimized & Ready",
                    "Health: {health_score}%. Device ready for tomorrow!"
                )
            ),
            maintenance = listOf(
                NotificationTemplate(
                    "🌙 Evening Wind-Down",
                    "End your day right with a quick device check."
                ),
                NotificationTemplate(
                    "🛏️ Bedtime Routine",
                    "Just like you, your device needs evening care too."
                ),
                NotificationTemplate(
                    "🌃 Nightly Maintenance",
                    "A clean device for sweet dreams and smooth mornings."
                )
            )
        )
    }
}

private data class MorningTemplates(
    val critical: List<NotificationTemplate>,
    val storageCritical: List<NotificationTemplate>,
    val warning: List<NotificationTemplate>,
    val storageWarning: List<NotificationTemplate>,
    val healthy: List<NotificationTemplate>,
    val motivational: List<NotificationTemplate>
)

private data class EveningTemplates(
    val critical: List<NotificationTemplate>,
    val storageCritical: List<NotificationTemplate>,
    val warning: List<NotificationTemplate>,
    val storageWarning: List<NotificationTemplate>,
    val healthy: List<NotificationTemplate>,
    val maintenance: List<NotificationTemplate>
)