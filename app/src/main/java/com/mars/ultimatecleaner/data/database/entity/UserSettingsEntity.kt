package com.mars.ultimatecleaner.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_settings",
    indices = [
        Index(value = ["setting_key"]),
        Index(value = ["data_type"]),
        Index(value = ["last_updated"])
    ]
)
data class UserSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "setting_key")
    val settingKey: String,

    @ColumnInfo(name = "setting_value")
    val settingValue: String,

    @ColumnInfo(name = "data_type")
    val dataType: String, // STRING, INT, LONG, BOOLEAN, FLOAT, JSON

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "category")
    val category: String = "GENERAL",

    @ColumnInfo(name = "is_sensitive")
    val isSensitive: Boolean = false,

    @ColumnInfo(name = "version")
    val version: Int = 1,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)