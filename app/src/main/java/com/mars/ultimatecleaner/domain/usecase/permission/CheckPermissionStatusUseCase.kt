package com.mars.ultimatecleaner.domain.usecase.permission

import com.mars.ultimatecleaner.domain.model.AppPermission
import com.mars.ultimatecleaner.domain.model.PermissionStatus
import com.mars.ultimatecleaner.domain.repository.PermissionRepository
import javax.inject.Inject

class CheckPermissionStatusUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository
) {

    suspend operator fun invoke(permission: AppPermission): PermissionStatus {
        return try {
            permissionRepository.checkPermission(permission)
        } catch (e: Exception) {
            PermissionStatus(
                permission = permission,
                isGranted = false,
                error = e.message
            )
        }
    }
}