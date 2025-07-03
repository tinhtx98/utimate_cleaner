package com.mars.ultimatecleaner.data.database.dao

import androidx.room.*
import com.mars.ultimatecleaner.data.database.entity.settings.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings ORDER BY updated_at DESC")
    fun getAllSettings(): Flow<List<SettingsEntity>>

    @Query("SELECT * FROM settings WHERE category = :category ORDER BY updated_at DESC")
    fun getSettingsByCategory(category: String): Flow<List<SettingsEntity>>

    @Query("SELECT * FROM settings WHERE key = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): SettingsEntity?

    @Query("SELECT value FROM settings WHERE key = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Query("SELECT * FROM settings WHERE is_user_modified = :isUserModified ORDER BY updated_at DESC")
    fun getSettingsByUserModified(isUserModified: Boolean): Flow<List<SettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<SettingsEntity>)

    @Update
    suspend fun updateSetting(setting: SettingsEntity)

    @Delete
    suspend fun deleteSetting(setting: SettingsEntity)

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteSettingByKey(key: String)

    @Query("DELETE FROM settings WHERE category = :category")
    suspend fun deleteSettingsByCategory(category: String)

    @Query("DELETE FROM settings")
    suspend fun deleteAllSettings()

    @Transaction
    suspend fun insertOrUpdateSetting(setting: SettingsEntity) {
        val existing = getSettingByKey(setting.key)
        if (existing != null) {
            updateSetting(setting.copy(
                id = existing.id,
                createdAt = existing.createdAt,
                updatedAt = System.currentTimeMillis()
            ))
        } else {
            insertSetting(setting)
        }
    }
}
