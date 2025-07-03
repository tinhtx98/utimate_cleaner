package com.mars.ultimatecleaner.domain.model

data class PermissionStatus(
    val permission: AppPermission,
    val isGranted: Boolean,
    val shouldShowRationale: Boolean = false,
    val isPermanentlyDenied: Boolean = false,
    val lastChecked: Long = System.currentTimeMillis(),
    val error: String? = null
)

data class PermissionResult(
    val permission: AppPermission,
    val isGranted: Boolean,
    val shouldShowRationale: Boolean = false,
    val isPermanentlyDenied: Boolean = false,
    val userResponse: UserResponse? = null,
    val error: String? = null
)

data class PermissionRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val permission: AppPermission,
    val requestTime: Long,
    val result: PermissionResult? = null,
    val context: String? = null // Which feature requested this permission
)

data class PermissionUsageStats(
    val permission: AppPermission,
    val requestCount: Int,
    val grantedCount: Int,
    val deniedCount: Int,
    val lastUsed: Long?,
    val usageFrequency: Float,
    val associatedFeatures: List<String>
)

data class SpecialPermissionResult(
    val permission: SpecialPermission,
    val isGranted: Boolean,
    val requiresUserAction: Boolean = false,
    val actionDescription: String? = null,
    val error: String? = null
)

data class PermissionGroupValidation(
    val group: PermissionGroup,
    val allGranted: Boolean,
    val grantedPermissions: List<AppPermission>,
    val missingPermissions: List<AppPermission>,
    val criticalMissing: List<AppPermission>
)

data class FeatureAvailability(
    val feature: AppFeature,
    val isAvailable: Boolean,
    val missingPermissions: List<AppPermission>,
    val fallbackAvailable: Boolean = false,
    val limitedFunctionality: Boolean = false
)

data class PermissionSetupGuide(
    val steps: List<PermissionSetupStep>,
    val currentStep: Int = 0,
    val totalSteps: Int,
    val estimatedTime: Int, // minutes
    val isComplete: Boolean = false
)

data class PermissionSetupStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val permission: AppPermission,
    val isRequired: Boolean,
    val instructions: List<String>,
    val isCompleted: Boolean = false
)

enum class AppPermission(val manifestPermission: String, val displayName: String, val description: String) {
    STORAGE("android.permission.WRITE_EXTERNAL_STORAGE", "Storage Access", "Access device storage to clean files"),
    READ_STORAGE("android.permission.READ_EXTERNAL_STORAGE", "Read Storage", "Read files on device storage"),
    MANAGE_STORAGE("android.permission.MANAGE_EXTERNAL_STORAGE", "All Files Access", "Manage all files on device"),
    USAGE_STATS("android.permission.PACKAGE_USAGE_STATS", "Usage Statistics", "Monitor app usage for optimization"),
    DEVICE_ADMIN("android.permission.DEVICE_ADMIN", "Device Administration", "Advanced device management"),
    GET_TASKS("android.permission.GET_TASKS", "Running Apps", "Monitor running applications"),
    KILL_BACKGROUND("android.permission.KILL_BACKGROUND_PROCESSES", "Background Apps", "Manage background processes"),
    BATTERY_STATS("android.permission.BATTERY_STATS", "Battery Statistics", "Monitor battery usage"),
    SYSTEM_ALERT_WINDOW("android.permission.SYSTEM_ALERT_WINDOW", "Display Over Apps", "Show optimization alerts"),
    ACCESS_SUPERUSER("android.permission.ACCESS_SUPERUSER", "Root Access", "Advanced system optimization"),
    CAMERA("android.permission.CAMERA", "Camera", "Scan QR codes and capture images"),
    LOCATION("android.permission.ACCESS_FINE_LOCATION", "Location", "Optimize location-based services"),
    PHONE_STATE("android.permission.READ_PHONE_STATE", "Device Info", "Read device information for optimization"),
    NOTIFICATIONS("android.permission.POST_NOTIFICATIONS", "Notifications", "Send optimization reminders")
}

enum class SpecialPermission(val action: String, val displayName: String) {
    OVERLAY("android.settings.action.MANAGE_OVERLAY_PERMISSION", "Display Over Other Apps"),
    USAGE_ACCESS("android.settings.USAGE_ACCESS_SETTINGS", "Usage Access"),
    DEVICE_ADMIN("android.app.action.ADD_DEVICE_ADMIN", "Device Administrator"),
    ACCESSIBILITY("android.settings.ACCESSIBILITY_SETTINGS", "Accessibility Service"),
    NOTIFICATION_ACCESS("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS", "Notification Access"),
    ALL_FILES_ACCESS("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION", "All Files Access"),
    BATTERY_OPTIMIZATION("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", "Battery Optimization")
}

enum class PermissionGroup {
    STORAGE_ACCESS,
    SYSTEM_MONITORING,
    DEVICE_MANAGEMENT,
    MEDIA_ACCESS,
    NETWORK_ACCESS,
    LOCATION_SERVICES
}

enum class AppFeature {
    BASIC_CLEANING,
    ADVANCED_CLEANING,
    PHOTO_OPTIMIZATION,
    APP_MANAGEMENT,
    PERFORMANCE_MONITORING,
    BATTERY_OPTIMIZATION,
    DUPLICATE_DETECTION,
    STORAGE_ANALYSIS,
    SYSTEM_OPTIMIZATION,
    SCHEDULED_CLEANING
}

enum class UserResponse {
    GRANTED,
    DENIED,
    DENIED_FOREVER,
    CANCELLED
}