package com.mars.ultimatecleaner.domain.model

import com.mars.ultimatecleaner.data.notification.model.NotificationSettingsData
import com.mars.ultimatecleaner.ui.main.DeviceHealth

data class SettingsUiState(
    val isLoading: Boolean = false,
    val appSettings: AppSettings = AppSettings(),
    val notificationSettings: NotificationSettingsData = NotificationSettingsData(),
    val permissions: Map<AppPermission, Boolean> = emptyMap(),
    val error: String? = null
)

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "English",
    val autoStartEnabled: Boolean = false,
    val isAutoCleaningEnabled: Boolean = true,
    val autoCleanCache: Boolean = true,
    val autoCleanTemp: Boolean = true,
    val autoCleanApk: Boolean = false,
    val autoCleanEmptyFolders: Boolean = true,
    val cleaningAggressiveness: CleaningAggressiveness = CleaningAggressiveness.MODERATE,
    val safetyOptions: SafetyOptions = SafetyOptions(),
    val analyticsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,
    val cloudBackupEnabled: Boolean = false,
    val developerModeEnabled: Boolean = false
)

enum class AppTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}

enum class CleaningAggressiveness(val displayName: String) {
    CONSERVATIVE("Conservative"),
    MODERATE("Moderate"),
    AGGRESSIVE("Aggressive")
}

data class SafetyOptions(
    val createBackupBeforeClean: Boolean = true,
    val requireConfirmation: Boolean = true,
    val protectRecentFiles: Boolean = true,
    val enableFileRecovery: Boolean = true,
    val whitelistSystemFiles: Boolean = true
)

data class OptimizerUiState(
    val isLoading: Boolean = false,
    val isOptimizing: Boolean = false,
    val isAnalyzing: Boolean = false,
    val deviceHealth: DeviceHealth = DeviceHealth(),
    val storageAnalysis: StorageAnalysis? = null,
    val recommendations: List<OptimizationRecommendation> = emptyList(),
    val optimizationHistory: List<OptimizationResultSettings> = emptyList(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val photoAnalysis: PhotoAnalysis? = null,
    val batteryOptimization: BatteryOptimization? = null,
    val scheduledOptimizations: List<OptimizationSchedule> = emptyList(),
    val lastOptimizationResultSettings: OptimizationResultSettings? = null,
    val error: String? = null
)

data class OptimizationProgress(
    val currentStep: String,
    val progress: Int,
    val totalSteps: Int = 100,
    val startTime: Long = System.currentTimeMillis()
)

data class OptimizationResultSettings(
    val type: OptimizationType,
    val spaceSaved: Long,
    val filesProcessed: Int,
    val duration: Long,
    val improvements: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class OptimizationRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val estimatedBenefit: String,
    val priority: Priority
)

enum class OptimizationType {
    QUICK, DEEP, COMPREHENSIVE, MEMORY, STORAGE, BATTERY
}

enum class RecommendationType {
    STORAGE_CLEANUP, PERFORMANCE_BOOST, BATTERY_OPTIMIZATION, PHOTO_CLEANUP, APP_MANAGEMENT
}

enum class Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class StorageAnalysis(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val usagePercentage: Float,
    val cacheSize: Long,
    val junkFilesSize: Long,
    val cleanableSpace: Long,
    val categoryBreakdown: Map<String, Long>
)

data class OptimizationSchedule(
    val type: OptimizationType,
    val frequency: ScheduleFrequency,
    val timeOfDay: Int, // Hour in 24h format
    val isEnabled: Boolean
)
data class NotificationSettings(
    val areNotificationsEnabled: Boolean = true,
    val morningNotificationsEnabled: Boolean = true,
    val eveningNotificationsEnabled: Boolean = true,
    val morningHour: Int = 10,
    val eveningHour: Int = 19,
    val weekendNotificationsEnabled: Boolean = true,
    val criticalAlertsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val lastNotificationTime: Long = 0L,
    val notificationFrequency: NotificationFrequency = NotificationFrequency.DAILY
)

enum class NotificationFrequency {
    DAILY, EVERY_OTHER_DAY, WEEKLY, CUSTOM
}

enum class ScheduleFrequency {
    DAILY, WEEKLY, MONTHLY
}