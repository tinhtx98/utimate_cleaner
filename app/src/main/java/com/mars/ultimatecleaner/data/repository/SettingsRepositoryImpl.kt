package com.mars.ultimatecleaner.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.mars.ultimatecleaner.data.database.dao.SettingsDao
import com.mars.ultimatecleaner.data.database.entity.settings.SettingsEntity
import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val settingsDao: SettingsDao
) : SettingsRepository {

    companion object {
        private const val KEY_SCHEDULED_CLEANING_ENABLED = "scheduled_cleaning_enabled"
        private const val KEY_SCHEDULED_CLEANING_FREQUENCY = "scheduled_cleaning_frequency"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NOTIFICATION_FREQUENCY = "notification_frequency"
        private const val KEY_THEME = "app_theme"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_AUTO_DELETE_EMPTY_FOLDERS = "auto_delete_empty_folders"
        private const val KEY_AUTO_DELETE_DUPLICATES = "auto_delete_duplicates"
        private const val KEY_LARGE_FILE_THRESHOLD = "large_file_threshold_mb"
        private const val KEY_COMPRESSION_QUALITY = "compression_quality"
        private const val KEY_SCREENSHOT_RETENTION_DAYS = "screenshot_retention_days"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_AUTO_CLEANUP_ENABLED = "auto_cleanup_enabled"
        private const val KEY_SMART_SUGGESTIONS_ENABLED = "smart_suggestions_enabled"
        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
        private const val KEY_CRASH_REPORTING_ENABLED = "crash_reporting_enabled"
    }

    override suspend fun getSettings(): AppSettings {
        return withContext(Dispatchers.IO) {
            try {
                AppSettings(
                    scheduledCleaningEnabled = sharedPreferences.getBoolean(KEY_SCHEDULED_CLEANING_ENABLED, false),
                    scheduledCleaningFrequency = CleaningFrequency.valueOf(
                        sharedPreferences.getString(KEY_SCHEDULED_CLEANING_FREQUENCY, CleaningFrequency.WEEKLY.name) ?: CleaningFrequency.WEEKLY.name
                    ),
                    notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true),
                    notificationFrequency = NotificationFrequency.valueOf(
                        sharedPreferences.getString(KEY_NOTIFICATION_FREQUENCY, NotificationFrequency.DAILY.name) ?: NotificationFrequency.DAILY.name
                    ),
                    theme = AppTheme.valueOf(
                        sharedPreferences.getString(KEY_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
                    ),
                    language = AppLanguage.valueOf(
                        sharedPreferences.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
                    ),
                    autoDeleteEmptyFolders = sharedPreferences.getBoolean(KEY_AUTO_DELETE_EMPTY_FOLDERS, false),
                    autoDeleteDuplicates = sharedPreferences.getBoolean(KEY_AUTO_DELETE_DUPLICATES, false),
                    largeFileThresholdMB = sharedPreferences.getInt(KEY_LARGE_FILE_THRESHOLD, 100),
                    compressionQuality = sharedPreferences.getInt(KEY_COMPRESSION_QUALITY, 80),
                    screenshotRetentionDays = sharedPreferences.getInt(KEY_SCREENSHOT_RETENTION_DAYS, 30),
                    isFirstLaunch = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true),
                    lastBackupTime = sharedPreferences.getLong(KEY_LAST_BACKUP_TIME, 0L),
                    autoCleanupEnabled = sharedPreferences.getBoolean(KEY_AUTO_CLEANUP_ENABLED, false),
                    smartSuggestionsEnabled = sharedPreferences.getBoolean(KEY_SMART_SUGGESTIONS_ENABLED, true),
                    analyticsEnabled = sharedPreferences.getBoolean(KEY_ANALYTICS_ENABLED, true),
                    crashReportingEnabled = sharedPreferences.getBoolean(KEY_CRASH_REPORTING_ENABLED, true)
                )
            } catch (e: Exception) {
                AppSettings.getDefault()
            }
        }
    }

    override suspend fun setScheduledCleaningEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putBoolean(KEY_SCHEDULED_CLEANING_ENABLED, enabled)
            }
            saveSettingToDatabase("scheduled_cleaning_enabled", enabled.toString())
        }
    }

    override suspend fun setScheduledCleaningFrequency(frequency: CleaningFrequency) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putString(KEY_SCHEDULED_CLEANING_FREQUENCY, frequency.name)
            }
            saveSettingToDatabase("scheduled_cleaning_frequency", frequency.name)
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            }
            saveSettingToDatabase("notifications_enabled", enabled.toString())
        }
    }

    override suspend fun setNotificationFrequency(frequency: NotificationFrequency) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putString(KEY_NOTIFICATION_FREQUENCY, frequency.name)
            }
            saveSettingToDatabase("notification_frequency", frequency.name)
        }
    }

    override suspend fun setTheme(theme: AppTheme) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putString(KEY_THEME, theme.name)
            }
            saveSettingToDatabase("app_theme", theme.name)
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putString(KEY_LANGUAGE, language.name)
            }
            saveSettingToDatabase("app_language", language.name)
        }
    }

    override suspend fun setAutoDeleteEmptyFolders(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putBoolean(KEY_AUTO_DELETE_EMPTY_FOLDERS, enabled)
            }
            saveSettingToDatabase("auto_delete_empty_folders", enabled.toString())
        }
    }

    override suspend fun setAutoDeleteDuplicates(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putBoolean(KEY_AUTO_DELETE_DUPLICATES, enabled)
            }
            saveSettingToDatabase("auto_delete_duplicates", enabled.toString())
        }
    }

    override suspend fun setLargeFileThreshold(thresholdMB: Int) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putInt(KEY_LARGE_FILE_THRESHOLD, thresholdMB)
            }
            saveSettingToDatabase("large_file_threshold_mb", thresholdMB.toString())
        }
    }

    override suspend fun setCompressionQuality(quality: Int) {
        withContext(Dispatchers.IO) {
            val clampedQuality = quality.coerceIn(10, 100)
            sharedPreferences.edit {
                putInt(KEY_COMPRESSION_QUALITY, clampedQuality)
            }
            saveSettingToDatabase("compression_quality", clampedQuality.toString())
        }
    }

    override suspend fun setScreenshotRetentionDays(days: Int) {
        withContext(Dispatchers.IO) {
            val clampedDays = days.coerceIn(1, 365)
            sharedPreferences.edit {
                putInt(KEY_SCREENSHOT_RETENTION_DAYS, clampedDays)
            }
            saveSettingToDatabase("screenshot_retention_days", clampedDays.toString())
        }
    }

    override suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                // Clear app cache directories
                val cacheDir = context.cacheDir
                val externalCacheDir = context.externalCacheDir

                clearDirectory(cacheDir)
                externalCacheDir?.let { clearDirectory(it) }

                // Clear database cache
                settingsDao.clearOldSettings(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)

                // Update last cache clear time
                sharedPreferences.edit {
                    putLong("last_cache_clear", System.currentTimeMillis())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun exportSettings(): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val settings = getSettings()
                val json = Json.encodeToString(settings)

                val exportDir = File(context.getExternalFilesDir(null), "exports")
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }

                val timestamp = System.currentTimeMillis()
                val exportFile = File(exportDir, "ultimatecleaner_settings_$timestamp.json")

                FileOutputStream(exportFile).use { output ->
                    output.write(json.toByteArray())
                }

                ExportResult(
                    success = true,
                    filePath = exportFile.absolutePath,
                    fileSize = exportFile.length(),
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                ExportResult(
                    success = false,
                    error = e.message ?: "Export failed"
                )
            }
        }
    }

    override suspend fun importSettings(filePath: String) {
        withContext(Dispatchers.IO) {
            try {
                val importFile = File(filePath)
                if (!importFile.exists()) {
                    throw IllegalArgumentException("Import file does not exist")
                }

                val json = FileInputStream(importFile).use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                }

                val settings = Json.decodeFromString<AppSettings>(json)

                // Apply imported settings
                setScheduledCleaningEnabled(settings.scheduledCleaningEnabled)
                setScheduledCleaningFrequency(settings.scheduledCleaningFrequency)
                setNotificationsEnabled(settings.notificationsEnabled)
                setNotificationFrequency(settings.notificationFrequency)
                setTheme(settings.theme)
                setLanguage(settings.language)
                setAutoDeleteEmptyFolders(settings.autoDeleteEmptyFolders)
                setAutoDeleteDuplicates(settings.autoDeleteDuplicates)
                setLargeFileThreshold(settings.largeFileThresholdMB)
                setCompressionQuality(settings.compressionQuality)
                setScreenshotRetentionDays(settings.screenshotRetentionDays)

                // Update import timestamp
                sharedPreferences.edit {
                    putLong("last_import_time", System.currentTimeMillis())
                }

            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to import settings: ${e.message}")
            }
        }
    }

    override suspend fun resetToDefaults() {
        withContext(Dispatchers.IO) {
            try {
                val defaultSettings = AppSettings.getDefault()

                sharedPreferences.edit {
                    clear()
                    putBoolean(KEY_SCHEDULED_CLEANING_ENABLED, defaultSettings.scheduledCleaningEnabled)
                    putString(KEY_SCHEDULED_CLEANING_FREQUENCY, defaultSettings.scheduledCleaningFrequency.name)
                    putBoolean(KEY_NOTIFICATIONS_ENABLED, defaultSettings.notificationsEnabled)
                    putString(KEY_NOTIFICATION_FREQUENCY, defaultSettings.notificationFrequency.name)
                    putString(KEY_THEME, defaultSettings.theme.name)
                    putString(KEY_LANGUAGE, defaultSettings.language.name)
                    putBoolean(KEY_AUTO_DELETE_EMPTY_FOLDERS, defaultSettings.autoDeleteEmptyFolders)
                    putBoolean(KEY_AUTO_DELETE_DUPLICATES, defaultSettings.autoDeleteDuplicates)
                    putInt(KEY_LARGE_FILE_THRESHOLD, defaultSettings.largeFileThresholdMB)
                    putInt(KEY_COMPRESSION_QUALITY, defaultSettings.compressionQuality)
                    putInt(KEY_SCREENSHOT_RETENTION_DAYS, defaultSettings.screenshotRetentionDays)
                    putBoolean(KEY_SMART_SUGGESTIONS_ENABLED, defaultSettings.smartSuggestionsEnabled)
                    putBoolean(KEY_ANALYTICS_ENABLED, defaultSettings.analyticsEnabled)
                    putBoolean(KEY_CRASH_REPORTING_ENABLED, defaultSettings.crashReportingEnabled)
                    putLong("reset_timestamp", System.currentTimeMillis())
                }

                // Clear database settings
                settingsDao.clearAllSettings()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun observeSettings(): Flow<AppSettings> = flow {
        emit(getSettings())
    }.flowOn(Dispatchers.IO)

    private suspend fun saveSettingToDatabase(key: String, value: String) {
        try {
            val settingEntity = SettingsEntity(
                key = key,
                value = value,
                timestamp = System.currentTimeMillis()
            )
            settingsDao.insertOrUpdateSetting(settingEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearDirectory(directory: File) {
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    clearDirectory(file)
                }
                file.delete()
            }
        }
    }
}