package com.mars.ultimatecleaner.domain.repository

import com.mars.ultimatecleaner.domain.model.*

interface PermissionRepository {

    // Permission Status
    suspend fun checkPermission(permission: AppPermission): PermissionStatus
    suspend fun getAllPermissions(): Map<AppPermission, Boolean>
    suspend fun hasRequiredPermissions(): Boolean
    suspend fun getMissingPermissions(): List<AppPermission>
    suspend fun getCriticalMissingPermissions(): List<AppPermission>

    // Permission Requests
    suspend fun shouldShowRationale(permission: AppPermission): Boolean
    suspend fun recordPermissionRequest(permission: AppPermission, result: PermissionResult)
    suspend fun getPermissionRequestHistory(): List<PermissionRequest>
    suspend fun isPermissionPermanentlyDenied(permission: AppPermission): Boolean

    // Permission Analytics
    suspend fun trackPermissionUsage(permission: AppPermission, feature: String)
    suspend fun getPermissionUsageStats(): Map<AppPermission, PermissionUsageStats>
    suspend fun recordPermissionDenial(permission: AppPermission, reason: String)
    suspend fun getPermissionDenialReasons(): Map<AppPermission, List<String>>

    // Special Permissions
    suspend fun checkSpecialPermission(permission: SpecialPermission): Boolean
    suspend fun requestSpecialPermission(permission: SpecialPermission): SpecialPermissionResult
    suspend fun getSpecialPermissionStatus(): Map<SpecialPermission, Boolean>

    // Permission Management
    suspend fun openPermissionSettings(permission: AppPermission)
    suspend fun openAppSettings()
    suspend fun getPermissionDescription(permission: AppPermission): String
    suspend fun getPermissionRationale(permission: AppPermission): String

    // Permission Validation
    suspend fun validatePermissionGroup(group: PermissionGroup): PermissionGroupValidation
    suspend fun getRequiredPermissionsForFeature(feature: AppFeature): List<AppPermission>
    suspend fun checkFeatureAvailability(feature: AppFeature): FeatureAvailability

    // Guided Setup
    suspend fun generatePermissionSetupGuide(): PermissionSetupGuide
    suspend fun getNextRequiredPermission(): AppPermission?
    suspend fun markPermissionSetupComplete()
    suspend fun isPermissionSetupComplete(): Boolean
}