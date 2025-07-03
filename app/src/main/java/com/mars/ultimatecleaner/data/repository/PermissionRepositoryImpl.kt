package com.mars.ultimatecleaner.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.mars.ultimatecleaner.data.database.dao.PermissionDao
import com.mars.ultimatecleaner.data.database.entity.permission.*
import com.mars.ultimatecleaner.data.manager.PermissionManager
import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.domain.repository.PermissionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager,
    private val permissionDao: PermissionDao,
    private val sharedPreferences: SharedPreferences
) : PermissionRepository {

    override suspend fun checkPermission(permission: AppPermission): PermissionStatus {
        return withContext(Dispatchers.IO) {
            try {
                val isGranted = when (permission) {
                    AppPermission.STORAGE -> checkStoragePermission()
                    AppPermission.MEDIA_IMAGES -> checkMediaPermission()
                    AppPermission.MEDIA_VIDEO -> checkMediaPermission()
                    AppPermission.MEDIA_AUDIO -> checkMediaPermission()
                    AppPermission.MANAGE_EXTERNAL_STORAGE -> checkManageExternalStoragePermission()
                    AppPermission.USAGE_STATS -> checkUsageStatsPermission()
                    AppPermission.DEVICE_ADMIN -> checkDeviceAdminPermission()
                    AppPermission.NOTIFICATION_POLICY -> checkNotificationPolicyPermission()
                }

                val shouldShowRationale = permissionManager.shouldShowRationale(permission)
                val isPermanentlyDenied = isPermissionPermanentlyDenied(permission)

                PermissionStatus(
                    permission = permission,
                    isGranted = isGranted,
                    shouldShowRationale = shouldShowRationale,
                    isPermanentlyDenied = isPermanentlyDenied,
                    lastChecked = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                PermissionStatus(
                    permission = permission,
                    isGranted = false,
                    error = e.message
                )
            }
        }
    }

    override suspend fun getAllPermissions(): Map<AppPermission, Boolean> {
        return withContext(Dispatchers.IO) {
            AppPermission.values().associateWith { permission ->
                checkPermission(permission).isGranted
            }
        }
    }

    override suspend fun hasRequiredPermissions(): Boolean {
        return withContext(Dispatchers.IO) {
            val requiredPermissions = listOf(
                AppPermission.STORAGE,
                AppPermission.MEDIA_IMAGES,
                AppPermission.MEDIA_VIDEO,
                AppPermission.MEDIA_AUDIO
            )

            requiredPermissions.all { permission ->
                checkPermission(permission).isGranted
            }
        }
    }

    override suspend fun getMissingPermissions(): List<AppPermission> {
        return withContext(Dispatchers.IO) {
            AppPermission.values().filter { permission ->
                !checkPermission(permission).isGranted
            }
        }
    }

    override suspend fun getCriticalMissingPermissions(): List<AppPermission> {
        return withContext(Dispatchers.IO) {
            val criticalPermissions = listOf(
                AppPermission.STORAGE,
                AppPermission.MEDIA_IMAGES,
                AppPermission.MEDIA_VIDEO
            )

            criticalPermissions.filter { permission ->
                !checkPermission(permission).isGranted
            }
        }
    }

    override suspend fun shouldShowRationale(permission: AppPermission): Boolean {
        return withContext(Dispatchers.IO) {
            permissionManager.shouldShowRationale(permission)
        }
    }

    override suspend fun recordPermissionRequest(permission: AppPermission, result: PermissionResult) {
        withContext(Dispatchers.IO) {
            try {
                val requestEntity = PermissionRequestEntity(
                    permission = permission.name,
                    isGranted = result.isGranted,
                    shouldShowRationale = result.shouldShowRationale,
                    isPermanentlyDenied = result.isPermanentlyDenied,
                    timestamp = System.currentTimeMillis(),
                    userResponse = result.userResponse?.name
                )
                permissionDao.insertPermissionRequest(requestEntity)

                // Update permission stats
                updatePermissionStats(permission, result)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getPermissionRequestHistory(): List<PermissionRequest> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = permissionDao.getPermissionRequestHistory()
                entities.map { entity ->
                    PermissionRequest(
                        id = entity.id,
                        permission = AppPermission.valueOf(entity.permission),
                        requestTime = entity.timestamp,
                        result = PermissionResult(
                            permission = AppPermission.valueOf(entity.permission),
                            isGranted = entity.isGranted,
                            shouldShowRationale = entity.shouldShowRationale,
                            isPermanentlyDenied = entity.isPermanentlyDenied,
                            userResponse = entity.userResponse?.let { UserResponse.valueOf(it) }
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override suspend fun isPermissionPermanentlyDenied(permission: AppPermission): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val denialCount = sharedPreferences.getInt("${permission.name}_denial_count", 0)
                val lastDenialTime = sharedPreferences.getLong("${permission.name}_last_denial", 0)
                val currentTime = System.currentTimeMillis()

                // Consider permanently denied if denied more than 2 times in the last 24 hours
                denialCount >= 2 && (currentTime - lastDenialTime) < 24 * 60 * 60 * 1000
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun trackPermissionUsage(permission: AppPermission, feature: String) {
        withContext(Dispatchers.IO) {
            try {
                val usageEntity = PermissionUsageEntity(
                    permission = permission.name,
                    feature = feature,
                    timestamp = System.currentTimeMillis()
                )
                permissionDao.insertPermissionUsage(usageEntity)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getPermissionUsageStats(): Map<AppPermission, PermissionUsageStats> {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val monthAgo = now - 30 * 24 * 60 * 60 * 1000L

                AppPermission.values().associateWith { permission ->
                    val requests = permissionDao.getPermissionRequestsInPeriod(permission.name, monthAgo, now)
                    val usages = permissionDao.getPermissionUsagesInPeriod(permission.name, monthAgo, now)

                    val grantedCount = requests.count { it.isGranted }
                    val deniedCount = requests.count { !it.isGranted }

                    PermissionUsageStats(
                        permission = permission,
                        requestCount = requests.size,
                        grantedCount = grantedCount,
                        deniedCount = deniedCount,
                        usageCount = usages.size,
                        lastUsed = usages.maxOfOrNull { it.timestamp },
                        mostUsedFeature = usages.groupBy { it.feature }
                            .maxByOrNull { it.value.size }?.key
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
    }

    override suspend fun recordPermissionDenial(permission: AppPermission, reason: String) {
        withContext(Dispatchers.IO) {
            try {
                val denialEntity = PermissionDenialEntity(
                    permission = permission.name,
                    reason = reason,
                    timestamp = System.currentTimeMillis()
                )
                permissionDao.insertPermissionDenial(denialEntity)

                // Update denial count
                val currentCount = sharedPreferences.getInt("${permission.name}_denial_count", 0)
                sharedPreferences.edit()
                    .putInt("${permission.name}_denial_count", currentCount + 1)
                    .putLong("${permission.name}_last_denial", System.currentTimeMillis())
                    .apply()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getPermissionDenialReasons(): Map<AppPermission, List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val monthAgo = now - 30 * 24 * 60 * 60 * 1000L

                AppPermission.values().associateWith { permission ->
                    val denials = permissionDao.getPermissionDenialsInPeriod(permission.name, monthAgo, now)
                    denials.map { it.reason }.distinct()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
    }

    override suspend fun checkSpecialPermission(permission: SpecialPermission): Boolean {
        return withContext(Dispatchers.IO) {
            when (permission) {
                SpecialPermission.MANAGE_EXTERNAL_STORAGE -> checkManageExternalStoragePermission()
                SpecialPermission.USAGE_STATS -> checkUsageStatsPermission()
                SpecialPermission.DEVICE_ADMIN -> checkDeviceAdminPermission()
                SpecialPermission.NOTIFICATION_POLICY -> checkNotificationPolicyPermission()
                SpecialPermission.OVERLAY -> checkOverlayPermission()
                SpecialPermission.WRITE_SETTINGS -> checkWriteSettingsPermission()
            }
        }
    }

    override suspend fun requestSpecialPermission(permission: SpecialPermission): SpecialPermissionResult {
        return withContext(Dispatchers.IO) {
            try {
                val isGranted = checkSpecialPermission(permission)
                SpecialPermissionResult(
                    permission = permission,
                    isGranted = isGranted,
                    requiresManualSetup = !isGranted
                )
            } catch (e: Exception) {
                SpecialPermissionResult(
                    permission = permission,
                    isGranted = false,
                    requiresManualSetup = true,
                    error = e.message
                )
            }
        }
    }

    override suspend fun getSpecialPermissionStatus(): Map<SpecialPermission, Boolean> {
        return withContext(Dispatchers.IO) {
            SpecialPermission.values().associateWith { permission ->
                checkSpecialPermission(permission)
            }
        }
    }

    override suspend fun openPermissionSettings(permission: AppPermission) {
        withContext(Dispatchers.IO) {
            permissionManager.openPermissionSettings(permission)
        }
    }

    override suspend fun openAppSettings() {
        withContext(Dispatchers.IO) {
            permissionManager.openAppSettings()
        }
    }

    override suspend fun getPermissionDescription(permission: AppPermission): String {
        return when (permission) {
            AppPermission.STORAGE -> "Access device storage to scan and manage files"
            AppPermission.MEDIA_IMAGES -> "Access photos and images for analysis and optimization"
            AppPermission.MEDIA_VIDEO -> "Access videos for compression and duplicate detection"
            AppPermission.MEDIA_AUDIO -> "Access audio files for organization and cleanup"
            AppPermission.MANAGE_EXTERNAL_STORAGE -> "Full access to external storage for comprehensive file management"
            AppPermission.USAGE_STATS -> "Monitor app usage to suggest uninstallation of unused apps"
            AppPermission.DEVICE_ADMIN -> "Device administration for advanced system optimization"
            AppPermission.NOTIFICATION_POLICY -> "Manage notification settings for better performance"
        }
    }

    override suspend fun getPermissionRationale(permission: AppPermission): String {
        return when (permission) {
            AppPermission.STORAGE -> "UltimateCleaner needs storage access to scan your device for junk files, duplicates, and large files that can be safely removed to free up space."
            AppPermission.MEDIA_IMAGES -> "Access to photos is needed to detect blurry images, duplicates, and screenshots that can be cleaned up to save storage space."
            AppPermission.MEDIA_VIDEO -> "Video access allows the app to find duplicate videos, compress large videos, and organize your media collection."
            AppPermission.MEDIA_AUDIO -> "Audio access helps organize music files, remove duplicate songs, and clean up audio caches."
            AppPermission.MANAGE_EXTERNAL_STORAGE -> "Full storage access enables comprehensive file management and deep system cleaning capabilities."
            AppPermission.USAGE_STATS -> "Usage statistics help identify rarely used apps that can be suggested for removal to free up space."
            AppPermission.DEVICE_ADMIN -> "Device admin permissions allow advanced system optimizations and automated maintenance tasks."
            AppPermission.NOTIFICATION_POLICY -> "Notification management helps optimize system performance by controlling background processes."
        }
    }

    override suspend fun validatePermissionGroup(group: PermissionGroup): PermissionGroupValidation {
        return withContext(Dispatchers.IO) {
            val permissions = getPermissionsForGroup(group)
            val grantedPermissions = permissions.filter { checkPermission(it).isGranted }
            val missingPermissions = permissions.filter { !checkPermission(it).isGranted }

            PermissionGroupValidation(
                group = group,
                isFullyGranted = missingPermissions.isEmpty(),
                grantedPermissions = grantedPermissions,
                missingPermissions = missingPermissions,
                criticalMissing = missingPermissions.filter { isCriticalPermission(it) }
            )
        }
    }

    override suspend fun getRequiredPermissionsForFeature(feature: AppFeature): List<AppPermission> {
        return when (feature) {
            AppFeature.FILE_MANAGER -> listOf(AppPermission.STORAGE, AppPermission.MEDIA_IMAGES, AppPermission.MEDIA_VIDEO, AppPermission.MEDIA_AUDIO)
            AppFeature.JUNK_CLEANER -> listOf(AppPermission.STORAGE, AppPermission.MANAGE_EXTERNAL_STORAGE)
            AppFeature.DUPLICATE_FINDER -> listOf(AppPermission.STORAGE, AppPermission.MEDIA_IMAGES, AppPermission.MEDIA_VIDEO, AppPermission.MEDIA_AUDIO)
            AppFeature.PHOTO_OPTIMIZER -> listOf(AppPermission.MEDIA_IMAGES, AppPermission.STORAGE)
            AppFeature.APP_MANAGER -> listOf(AppPermission.USAGE_STATS, AppPermission.DEVICE_ADMIN)
            AppFeature.SYSTEM_OPTIMIZER -> listOf(AppPermission.DEVICE_ADMIN, AppPermission.NOTIFICATION_POLICY)
        }
    }

    override suspend fun checkFeatureAvailability(feature: AppFeature): FeatureAvailability {
        return withContext(Dispatchers.IO) {
            val requiredPermissions = getRequiredPermissionsForFeature(feature)
            val grantedPermissions = requiredPermissions.filter { checkPermission(it).isGranted }
            val missingPermissions = requiredPermissions.filter { !checkPermission(it).isGranted }

            FeatureAvailability(
                feature = feature,
                isAvailable = missingPermissions.isEmpty(),
                hasPartialAccess = grantedPermissions.isNotEmpty(),
                requiredPermissions = requiredPermissions,
                grantedPermissions = grantedPermissions,
                missingPermissions = missingPermissions
            )
        }
    }

    override suspend fun generatePermissionSetupGuide(): PermissionSetupGuide {
        return withContext(Dispatchers.IO) {
            val allPermissions = AppPermission.values()
            val grantedPermissions = allPermissions.filter { checkPermission(it).isGranted }
            val missingPermissions = allPermissions.filter { !checkPermission(it).isGranted }

            val steps = missingPermissions.map { permission ->
                PermissionSetupStep(
                    permission = permission,
                    title = "Grant ${permission.name} Permission",
                    description = getPermissionDescription(permission),
                    rationale = getPermissionRationale(permission),
                    isCritical = isCriticalPermission(permission),
                    isCompleted = false
                )
            }

            PermissionSetupGuide(
                totalSteps = steps.size,
                completedSteps = 0,
                steps = steps,
                estimatedTimeMinutes = steps.size * 2,
                isCompleted = missingPermissions.isEmpty()
            )
        }
    }

    override suspend fun getNextRequiredPermission(): AppPermission? {
        return withContext(Dispatchers.IO) {
            val criticalPermissions = listOf(
                AppPermission.STORAGE,
                AppPermission.MEDIA_IMAGES,
                AppPermission.MEDIA_VIDEO,
                AppPermission.MEDIA_AUDIO
            )

            criticalPermissions.firstOrNull { !checkPermission(it).isGranted }
                ?: AppPermission.values().firstOrNull { !checkPermission(it).isGranted }
        }
    }

    override suspend fun markPermissionSetupComplete() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit()
                .putBoolean("permission_setup_complete", true)
                .putLong("permission_setup_completed_at", System.currentTimeMillis())
                .apply()
        }
    }

    override suspend fun isPermissionSetupComplete(): Boolean {
        return withContext(Dispatchers.IO) {
            sharedPreferences.getBoolean("permission_setup_complete", false) && hasRequiredPermissions()
        }
    }

    // Private helper methods
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            checkManageExternalStoragePermission()
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            checkStoragePermission()
        }
    }

    private fun checkManageExternalStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            checkStoragePermission()
        }
    }

    private fun checkUsageStatsPermission(): Boolean {
        return permissionManager.hasUsageStatsPermission()
    }

    private fun checkDeviceAdminPermission(): Boolean {
        return permissionManager.hasDeviceAdminPermission()
    }

    private fun checkNotificationPolicyPermission(): Boolean {
        return permissionManager.hasNotificationPolicyPermission()
    }

    private fun checkOverlayPermission(): Boolean {
        return permissionManager.hasOverlayPermission()
    }

    private fun checkWriteSettingsPermission(): Boolean {
        return permissionManager.hasWriteSettingsPermission()
    }

    private fun updatePermissionStats(permission: AppPermission, result: PermissionResult) {
        val key = "${permission.name}_requests"
        val currentCount = sharedPreferences.getInt(key, 0)
        sharedPreferences.edit()
            .putInt(key, currentCount + 1)
            .apply()

        if (result.isGranted) {
            sharedPreferences.edit()
                .putLong("${permission.name}_granted_at", System.currentTimeMillis())
                .apply()
        }
    }

    private fun getPermissionsForGroup(group: PermissionGroup): List<AppPermission> {
        return when (group) {
            PermissionGroup.STORAGE -> listOf(AppPermission.STORAGE, AppPermission.MANAGE_EXTERNAL_STORAGE)
            PermissionGroup.MEDIA -> listOf(AppPermission.MEDIA_IMAGES, AppPermission.MEDIA_VIDEO, AppPermission.MEDIA_AUDIO)
            PermissionGroup.SYSTEM -> listOf(AppPermission.DEVICE_ADMIN, AppPermission.NOTIFICATION_POLICY, AppPermission.USAGE_STATS)
        }
    }

    private fun isCriticalPermission(permission: AppPermission): Boolean {
        return when (permission) {
            AppPermission.STORAGE,
            AppPermission.MEDIA_IMAGES,
            AppPermission.MEDIA_VIDEO,
            AppPermission.MEDIA_AUDIO -> true
            else -> false
        }
    }
}