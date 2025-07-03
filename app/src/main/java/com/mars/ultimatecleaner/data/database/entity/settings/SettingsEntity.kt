package com.mars.ultimatecleaner.data.database.entity.settings

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "data_type")
    val dataType: String, // STRING, BOOLEAN, INT, LONG, FLOAT

    @ColumnInfo(name = "default_value")
    val defaultValue: String?,

    @ColumnInfo(name = "is_user_modified")
    val isUserModified: Boolean = false,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
