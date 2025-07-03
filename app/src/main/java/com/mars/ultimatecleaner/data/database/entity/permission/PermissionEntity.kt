package com.mars.ultimatecleaner.data.database.entity.permission

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permission_history")
data class PermissionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "permission_type")
    val permissionType: String,

    @ColumnInfo(name = "is_granted")
    val isGranted: Boolean,

    @ColumnInfo(name = "request_reason")
    val requestReason: String?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "app_version")
    val appVersion: String? = null,

    @ColumnInfo(name = "device_info")
    val deviceInfo: String? = null
)
