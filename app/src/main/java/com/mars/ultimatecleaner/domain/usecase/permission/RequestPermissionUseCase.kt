package com.mars.ultimatecleaner.domain.usecase.permission

import com.mars.ultimatecleaner.domain.model.AppPermission
import com.mars.ultimatecleaner.domain.model.PermissionResult
import com.mars.ultimatecleaner.domain.repository.PermissionRepository
import javax.inject.Inject

class RequestPermissionUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository
) {

    suspend operator fun invoke(permission: AppPermission): PermissionResult {
        return try {
            // Check if permission is already granted
            val status = permissionRepository.checkPermission(permission)
            if (status.isGranted) {
                return PermissionResult(
                    permission = permission,
                    isGranted = true,
                    shouldShowRationale = false
                )
            }

            // Check if we should show rationale
            val shouldShowRationale = permissionRepository.shouldShowRationale(permission)

            // Check if permission is permanently denied
            val isPermanentlyDenied = permissionRepository.isPermissionPermanentlyDenied(permission)

            PermissionResult(
                permission = permission,
                isGranted = false,
                shouldShowRationale = shouldShowRationale,
                isPermanentlyDenied = isPermanentlyDenied
            )
        } catch (e: Exception) {
            PermissionResult(
                permission = permission,
                isGranted = false,
                error = e.message
            )
        }
    }
}