package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings ORDER BY category, setting_key")
    fun getAllSettings(): Flow<List<UserSettingsEntity>>

    @Query("SELECT * FROM user_settings WHERE category = :category ORDER BY setting_key")
    fun getSettingsByCategory(category: String): Flow<List<UserSettingsEntity>>

    @Query("SELECT * FROM user_settings WHERE setting_key = :key")
    suspend fun getSetting(key: String): UserSettingsEntity?

    @Query("SELECT setting_value FROM user_settings WHERE setting_key = :key")
    suspend fun getSettingValue(key: String): String?

    @Query("SELECT * FROM user_settings WHERE is_default = 0")
    fun getCustomSettings(): Flow<List<UserSettingsEntity>>

    @Query("SELECT * FROM user_settings WHERE is_sensitive = 1")
    suspend fun getSensitiveSettings(): List<UserSettingsEntity>

    @Query("SELECT DISTINCT category FROM user_settings ORDER BY category")
    fun getSettingCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: UserSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<UserSettingsEntity>)

    @Update
    suspend fun updateSetting(setting: UserSettingsEntity)

    @Query("UPDATE user_settings SET setting_value = :value, last_updated = :lastUpdated WHERE setting_key = :key")
    suspend fun updateSettingValue(key: String, value: String, lastUpdated: Long)

    @Delete
    suspend fun deleteSetting(setting: UserSettingsEntity)

    @Query("DELETE FROM user_settings WHERE setting_key = :key")
    suspend fun deleteSettingByKey(key: String)

    @Query("DELETE FROM user_settings WHERE category = :category")
    suspend fun deleteSettingsByCategory(category: String)

    @Query("DELETE FROM user_settings WHERE is_default = 0")
    suspend fun deleteCustomSettings()

    @Query("DELETE FROM user_settings")
    suspend fun deleteAllSettings()

    @Transaction
    suspend fun upsertSetting(key: String, value: String, dataType: String, category: String = "GENERAL") {
        val currentTime = System.currentTimeMillis()
        val existing = getSetting(key)

        if (existing != null) {
            updateSettingValue(key, value, currentTime)
        } else {
            insertSetting(
                UserSettingsEntity(
                    settingKey = key,
                    settingValue = value,
                    dataType = dataType,
                    category = category,
                    lastUpdated = currentTime,
                    createdAt = currentTime
                )
            )
        }
    }

    @Transaction
    suspend fun resetToDefaults() {
        deleteCustomSettings()
        // Insert default settings here if needed
    }
}