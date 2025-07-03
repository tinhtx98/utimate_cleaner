package com.mars.ultimatecleaner.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mars.ultimatecleaner.data.notification.manager.NotificationPreferenceManager
import com.mars.ultimatecleaner.data.notification.scheduler.NotificationScheduler
import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.domain.repository.SettingsRepository
import com.mars.ultimatecleaner.domain.repository.PermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val permissionRepository: PermissionRepository,
    private val notificationPreferenceManager: NotificationPreferenceManager,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val appSettings = settingsRepository.getSettings()
                val notificationSettings = notificationPreferenceManager.getNotificationSettings()
                val permissions = permissionRepository.getAllPermissions()

                _uiState.value = _uiState.value.copy(
                    appSettings = appSettings,
                    notificationSettings = notificationSettings,
                    permissions = permissions,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // General Settings
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.appSettings.copy(theme = theme)
                settingsRepository.updateSettings(updatedSettings)
                _uiState.value = _uiState.value.copy(appSettings = updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.appSettings.copy(language = language)
                settingsRepository.updateSettings(updatedSettings)
                _uiState.value = _uiState.value.copy(appSettings = updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateAutoStart(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.appSettings.copy(autoStartEnabled = enabled)
                settingsRepository.updateSettings(updatedSettings)
                _uiState.value = _uiState.value.copy(appSettings = updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    // Notification Settings
    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                notificationPreferenceManager.setNotificationsEnabled(enabled)
                val updatedNotificationSettings = _uiState.value.notificationSettings.copy(
                    areNotificationsEnabled = enabled
                )
                _uiState.value = _uiState.value.copy(notificationSettings = updatedNotificationSettings)

                if (enabled) {
                    notificationScheduler.scheduleAllNotifications()
                } else {
                    notificationScheduler.cancelAllNotifications()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateMorningNotification(enabled: Boolean) {
        viewModelScope.launch {
            try {
                notificationPreferenceManager.setMorningNotificationEnabled(enabled)
                val updatedNotificationSettings = _uiState.value.notificationSettings.copy(
                    morningNotificationsEnabled = enabled
                )
                _uiState.value = _uiState.value.copy(notificationSettings = updatedNotificationSettings)
                notificationScheduler.rescheduleNotifications()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateEveningNotification(enabled: Boolean) {
        viewModelScope.launch {
            try {
                notificationPreferenceManager.setEveningNotificationEnabled(enabled)
                val updatedNotificationSettings = _uiState.value.notificationSettings.copy(
                    eveningNotificationsEnabled = enabled
                )
                _uiState.value = _uiState.value.copy(notificationSettings = updatedNotificationSettings)
                notificationScheduler.rescheduleNotifications()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateNotificationTimes(morningHour: Int, eveningHour: Int) {
        viewModelScope.launch {
            try {
                notificationPreferenceManager.setCustomNotificationTimes(morningHour, eveningHour)
                val updatedNotificationSettings = _uiState.value.notificationSettings.copy(
                    morningHour = morningHour,
                    eveningHour = eveningHour
                )
                _uiState.value = _uiState.value.copy(notificationSettings = updatedNotificationSettings)
                notificationScheduler.updateNotificationTimes(morningHour, eveningHour)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    // Cleaning Settings
    fun updateAutoCleaningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.appSettings.copy(isAutoCleaningEnabled = enabled)
                settingsRepository.updateSettings(updatedSettings)
                _uiState.value = _uiState.value.copy(appSettings = updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateCleaningAggressiveness(level: CleaningAggressiveness) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.appSettings.copy(cleaningAggressiveness = level)
                settingsRepository.updateSettings(updatedSettings)
                _uiState.value = _uiState.value.copy(appSettings = updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateSafetyOptions(options: SafetyOptions) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.appSettings.copy(safetyOptions = options)
                settingsRepository.updateSettings(updatedSettings)
                _uiState.value = _uiState.value.copy(appSettings = updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    // Privacy Settings
    fun updateAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.appSettings.copy(analyticsEnabled = enabled)
                settingsRepository.updateSettings(updatedSettings)
                _uiState.value = _uiState.value.copy(appSettings = updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateCrashReportingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.appSettings.copy(crashReportingEnabled = enabled)
                settingsRepository.updateSettings(updatedSettings)
                _uiState.value = _uiState.value.copy(appSettings = updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateCloudBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.appSettings.copy(cloudBackupEnabled = enabled)
                settingsRepository.updateSettings(updatedSettings)
                _uiState.value = _uiState.value.copy(appSettings = updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    // Advanced Settings
    fun updateDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.appSettings.copy(developerModeEnabled = enabled)
                settingsRepository.updateSettings(updatedSettings)
                _uiState.value = _uiState.value.copy(appSettings = updatedSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun exportSettings(): String? {
        return try {
            settingsRepository.exportSettings()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
            null
        }
    }

    fun importSettings(settingsJson: String) {
        viewModelScope.launch {
            try {
                settingsRepository.importSettings(settingsJson)
                loadSettings() // Refresh settings
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                settingsRepository.resetToDefaults()
                notificationPreferenceManager.setNotificationsEnabled(true)
                notificationPreferenceManager.setMorningNotificationEnabled(true)
                notificationPreferenceManager.setEveningNotificationEnabled(true)
                loadSettings()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}