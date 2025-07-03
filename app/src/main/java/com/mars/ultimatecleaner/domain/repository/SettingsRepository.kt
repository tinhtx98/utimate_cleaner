package com.mars.ultimatecleaner.domain.repository

import com.mars.ultimatecleaner.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun getSettings(): AppSettings
    suspend fun setScheduledCleaningEnabled(enabled: Boolean)
    suspend fun setScheduledCleaningFrequency(frequency: CleaningFrequency)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setNotificationFrequency(frequency: NotificationFrequency)
    suspend fun setTheme(theme: AppTheme)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setAutoDeleteEmptyFolders(enabled: Boolean)
    suspend fun setAutoDeleteDuplicates(enabled: Boolean)
    suspend fun setLargeFileThreshold(thresholdMB: Int)
    suspend fun setCompressionQuality(quality: Int)
    suspend fun setScreenshotRetentionDays(days: Int)
    suspend fun clearCache()
    suspend fun exportSettings(): ExportResult
    suspend fun importSettings(filePath: String)
    suspend fun resetToDefaults()
    fun observeSettings(): Flow<AppSettings>
}