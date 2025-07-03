package com.mars.ultimatecleaner.data.manager

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mars.ultimatecleaner.domain.model.AppPermission
import com.mars.ultimatecleaner.domain.model.PermissionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private val STORAGE_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        private val MEDIA_PERMISSIONS_API_33 = arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    }

    private val _permissionStatus = MutableStateFlow<Map<AppPermission, Boolean>>(emptyMap())
    val permissionStatus: StateFlow<Map<AppPermission, Boolean>> = _permissionStatus.asStateFlow()

    private var pendingPermissionCallback: ((PermissionResult) -> Unit)? = null

    init {
        updatePermissionStatus()
    }

    fun checkPermission(permission: AppPermission): Boolean {
        return when (permission) {
            AppPermission.STORAGE -> checkStoragePermission()
            AppPermission.MANAGE_STORAGE -> checkManageStoragePermission()
            AppPermission.NOTIFICATIONS -> checkNotificationPermission()
            AppPermission.CAMERA -> checkCameraPermission()
            AppPermission.VIBRATE -> checkVibratePermission()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            MEDIA_PERMISSIONS_API_33.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ prefers MANAGE_EXTERNAL_STORAGE
            checkManageStoragePermission() ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            // Legacy storage permissions
            STORAGE_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun checkManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkLegacyStoragePermission()
        }
    }

    private fun checkLegacyStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No explicit permission needed before API 33
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkVibratePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.VIBRATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(
        activity: Activity,
        permission: AppPermission,
        callback: (PermissionResult) -> Unit
    ) {
        pendingPermissionCallback = callback

        when (permission) {
            AppPermission.STORAGE -> requestStoragePermission(activity)
            AppPermission.MANAGE_STORAGE -> requestManageStoragePermission(activity)
            AppPermission.NOTIFICATIONS -> requestNotificationPermission(activity)
            AppPermission.CAMERA -> requestCameraPermission(activity)
            AppPermission.VIBRATE -> requestVibratePermission(activity)
        }
    }

    private fun requestStoragePermission(activity: Activity) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ActivityCompat.requestPermissions(activity, MEDIA_PERMISSIONS_API_33, REQUEST_MEDIA_PERMISSIONS)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // For Android 11+, guide user to MANAGE_EXTERNAL_STORAGE
                requestManageStoragePermission(activity)
            }
            else -> {
                ActivityCompat.requestPermissions(activity, STORAGE_PERMISSIONS, REQUEST_STORAGE_PERMISSIONS)
            }
        }
    }

    private fun requestManageStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                activity.startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
            } catch (e: Exception) {
                // Fallback to general settings
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
            }
        } else {
            requestLegacyStoragePermission(activity)
        }
    }

    private fun requestLegacyStoragePermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity, STORAGE_PERMISSIONS, REQUEST_STORAGE_PERMISSIONS)
    }

    private fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        } else {
            // No permission needed, call success immediately
            pendingPermissionCallback?.invoke(
                PermissionResult(isGranted = true, shouldShowRationale = false)
            )
        }
    }

    private fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    private fun requestVibratePermission(activity: Activity) {
        // VIBRATE is a normal permission, granted automatically
        pendingPermissionCallback?.invoke(
            PermissionResult(isGranted = true, shouldShowRationale = false)
        )
    }

    fun handlePermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val callback = pendingPermissionCallback ?: return
        pendingPermissionCallback = null

        val isGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        when (requestCode) {
            REQUEST_STORAGE_PERMISSIONS,
            REQUEST_MEDIA_PERMISSIONS,
            REQUEST_NOTIFICATION_PERMISSION,
            REQUEST_CAMERA_PERMISSION -> {
                callback(PermissionResult(
                    isGranted = isGranted,
                    shouldShowRationale = shouldShowRequestPermissionRationale(permissions)
                ))
            }
        }

        updatePermissionStatus()
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = pendingPermissionCallback ?: return
        pendingPermissionCallback = null

        when (requestCode) {
            REQUEST_MANAGE_STORAGE -> {
                val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    false
                }

                callback(PermissionResult(
                    isGranted = isGranted,
                    shouldShowRationale = false
                ))
            }
        }

        updatePermissionStatus()
    }

    private fun shouldShowRequestPermissionRationale(permissions: Array<String>): Boolean {
        // This would need an Activity context to work properly
        return false
    }

    fun shouldShowRationale(activity: Activity, permission: AppPermission): Boolean {
        val manifestPermissions = when (permission) {
            AppPermission.STORAGE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                MEDIA_PERMISSIONS_API_33
            } else {
                STORAGE_PERMISSIONS
            }
            AppPermission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyArray()
            }
            AppPermission.CAMERA -> arrayOf(Manifest.permission.CAMERA)
            else -> emptyArray()
        }

        return manifestPermissions.any { manifestPermission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, manifestPermission)
        }
    }

    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        activity.startActivity(intent)
    }

    private fun updatePermissionStatus() {
        val status = mapOf(
            AppPermission.STORAGE to checkStoragePermission(),
            AppPermission.MANAGE_STORAGE to checkManageStoragePermission(),
            AppPermission.NOTIFICATIONS to checkNotificationPermission(),
            AppPermission.CAMERA to checkCameraPermission(),
            AppPermission.VIBRATE to checkVibratePermission()
        )

        _permissionStatus.value = status
    }

    fun getRequiredPermissions(): List<AppPermission> {
        return listOf(
            AppPermission.STORAGE,
            AppPermission.MANAGE_STORAGE,
            AppPermission.NOTIFICATIONS
        )
    }

    fun getOptionalPermissions(): List<AppPermission> {
        return listOf(
            AppPermission.CAMERA,
            AppPermission.VIBRATE
        )
    }

    fun hasAllRequiredPermissions(): Boolean {
        return getRequiredPermissions().all { checkPermission(it) }
    }

    companion object {
        private const val REQUEST_STORAGE_PERMISSIONS = 1001
        private const val REQUEST_MEDIA_PERMISSIONS = 1002
        private const val REQUEST_MANAGE_STORAGE = 1003
        private const val REQUEST_NOTIFICATION_PERMISSION = 1004
        private const val REQUEST_CAMERA_PERMISSION = 1005
    }
}