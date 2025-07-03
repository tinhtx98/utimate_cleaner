package com.mars.ultimatecleaner.ui.main

import com.mars.ultimatecleaner.domain.model.*

data class MainUiState(
    val isInitialLoading: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val deviceHealth: DeviceHealth = DeviceHealth(),
    val storageInfo: StorageInfo = StorageInfo(),
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics(),
    val quickActions: List<QuickAction> = emptyList(),
    val criticalAlerts: List<CriticalAlert> = emptyList(),
    val recentActivity: List<CleaningActivity> = emptyList(),
    val backgroundTasks: List<BackgroundTask> = emptyList(),
    val permissionRequest: AppPermission? = null,
    val lastUpdated: Long = 0L,
    val error: String? = null
)

data class DeviceHealth(
    val overallScore: Int = 0,
    val storageHealth: Int = 0,
    val performanceHealth: Int = 0,
    val batteryHealth: Int = 0,
    val securityHealth: Int = 0,
    val recommendations: List<String> = emptyList()
)

data class StorageInfo(
    val totalSpace: Long = 0L,
    val usedSpace: Long = 0L,
    val freeSpace: Long = 0L,
    val usagePercentage: Float = 0f,
    val categoryBreakdown: Map<String, Long> = emptyMap(),
    val junkFilesSize: Long = 0L,
    val junkFilesCount: Int = 0
)

data class PerformanceMetrics(
    val cpuUsage: Float = 0f,
    val ramUsage: Float = 0f,
    val batteryLevel: Int = 0,
    val temperature: Float = 0f,
    val availableMemory: Long = 0L,
    val networkSpeed: Float = 0f
)

data class QuickAction(
    val id: String,
    val type: QuickActionType,
    val title: String,
    val description: String,
    val icon: Int,
    val isEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val progress: Int = 0
)

enum class QuickActionType {
    CACHE_CLEANUP,
    JUNK_CLEANUP,
    DUPLICATE_SCAN,
    PHOTO_CLEANUP,
    OPTIMIZE_PERFORMANCE,
    BATTERY_OPTIMIZATION
}

data class CriticalAlert(
    val id: String,
    val type: AlertType,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val actionText: String,
    val requiredPermission: AppPermission? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AlertType {
    LOW_STORAGE,
    STORAGE_WARNING,
    PERFORMANCE_ISSUE,
    PERMISSION_REQUIRED,
    SECURITY_WARNING,
    MAINTENANCE_DUE
}

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

enum class AlertAction {
    DISMISS,
    FIX_NOW,
    REMIND_LATER
}

data class CleaningActivity(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val spaceSaved: Long,
    val filesAffected: Int
)

data class BackgroundTask(
    val id: String,
    val title: String,
    val progress: Int,
    val isIndeterminate: Boolean = false,
    val canCancel: Boolean = true
)