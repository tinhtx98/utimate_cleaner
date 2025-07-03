package com.mars.ultimatecleaner.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mars.ultimatecleaner.R
import com.mars.ultimatecleaner.domain.model.AppTheme
import com.mars.ultimatecleaner.domain.model.CleaningAggressiveness
import com.mars.ultimatecleaner.ui.screens.settings.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = viewModel::resetToDefaults) {
                    Icon(Icons.Default.Email, contentDescription = "Reset to defaults")
                }
            }
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // General Settings
                item {
                    SettingsGroup(title = "General") {
                        ListPreference(
                            title = "Theme",
                            subtitle = "Choose app appearance",
                            selectedValue = uiState.appSettings.theme.displayName,
                            options = AppTheme.values().map { it.displayName },
                            onValueChange = { value ->
                                val theme = AppTheme.values().find { it.displayName == value }
                                theme?.let { viewModel.updateTheme(it) }
                            },
                            icon = Icons.Default.Email
                        )

                        ListPreference(
                            title = "Language",
                            subtitle = "App language",
                            selectedValue = uiState.appSettings.language,
                            options = listOf("English", "Español", "Français", "Deutsch"),
                            onValueChange = viewModel::updateLanguage,
                            icon = Icons.Default.Email
                        )

                        SwitchPreference(
                            title = "Auto-start",
                            subtitle = "Start app automatically when device boots",
                            checked = uiState.appSettings.autoStartEnabled,
                            onCheckedChange = viewModel::updateAutoStart,
                            icon = Icons.Default.Email
                        )
                    }
                }

                // Notification Settings
                item {
                    SettingsGroup(title = "Notifications") {
                        SwitchPreference(
                            title = "Enable Notifications",
                            subtitle = "Receive device status reminders",
                            checked = uiState.notificationSettings.areNotificationsEnabled,
                            onCheckedChange = viewModel::updateNotificationsEnabled,
                            icon = Icons.Default.Notifications
                        )

                        if (uiState.notificationSettings.areNotificationsEnabled) {
                            SwitchPreference(
                                title = "Morning Reminders",
                                subtitle = "10:00 AM daily reminder",
                                checked = uiState.notificationSettings.morningNotificationsEnabled,
                                onCheckedChange = viewModel::updateMorningNotification,
                                icon = Icons.Default.Email
                            )

                            SwitchPreference(
                                title = "Evening Reminders",
                                subtitle = "7:00 PM daily reminder",
                                checked = uiState.notificationSettings.eveningNotificationsEnabled,
                                onCheckedChange = viewModel::updateEveningNotification,
                                icon = Icons.Default.Email
                            )

                            TimePickerPreference(
                                title = "Notification Times",
                                subtitle = "Customize reminder schedule",
                                morningHour = uiState.notificationSettings.morningHour,
                                eveningHour = uiState.notificationSettings.eveningHour,
                                onTimeChange = viewModel::updateNotificationTimes,
                                icon = Icons.Default.Email
                            )
                        }
                    }
                }

                // Cleaning Settings
                item {
                    SettingsGroup(title = "Cleaning") {
                        SwitchPreference(
                            title = "Auto Cleaning",
                            subtitle = "Automatically clean junk files",
                            checked = uiState.appSettings.isAutoCleaningEnabled,
                            onCheckedChange = viewModel::updateAutoCleaningEnabled,
                            icon = Icons.Default.Email
                        )

                        ListPreference(
                            title = "Cleaning Aggressiveness",
                            subtitle = "How thoroughly to clean",
                            selectedValue = uiState.appSettings.cleaningAggressiveness.displayName,
                            options = CleaningAggressiveness.values().map { it.displayName },
                            onValueChange = { value ->
                                val level = CleaningAggressiveness.values().find { it.displayName == value }
                                level?.let { viewModel.updateCleaningAggressiveness(it) }
                            },
                            icon = Icons.Default.Email
                        )

                        SafetyOptionsPreference(
                            title = "Safety Options",
                            subtitle = "Configure cleaning safety measures",
                            safetyOptions = uiState.appSettings.safetyOptions,
                            onOptionsChange = viewModel::updateSafetyOptions,
                            icon = Icons.Default.Email
                        )
                    }
                }

                // Privacy Settings
                item {
                    SettingsGroup(title = "Privacy") {
                        SwitchPreference(
                            title = "Analytics",
                            subtitle = "Help improve the app with usage data",
                            checked = uiState.appSettings.analyticsEnabled,
                            onCheckedChange = viewModel::updateAnalyticsEnabled,
                            icon = Icons.Default.Email
                        )

                        SwitchPreference(
                            title = "Crash Reporting",
                            subtitle = "Send crash reports to developers",
                            checked = uiState.appSettings.crashReportingEnabled,
                            onCheckedChange = viewModel::updateCrashReportingEnabled,
                            icon = Icons.Default.Email
                        )

                        SwitchPreference(
                            title = "Cloud Backup",
                            subtitle = "Backup settings to cloud",
                            checked = uiState.appSettings.cloudBackupEnabled,
                            onCheckedChange = viewModel::updateCloudBackupEnabled,
                            icon = Icons.Default.Email
                        )
                    }
                }

                // Advanced Settings
                item {
                    SettingsGroup(title = "Advanced") {
                        SwitchPreference(
                            title = "Developer Mode",
                            subtitle = "Enable advanced debugging features",
                            checked = uiState.appSettings.developerModeEnabled,
                            onCheckedChange = viewModel::updateDeveloperMode,
                            icon = Icons.Default.Email
                        )

                        SettingsItem(
                            title = "Export Settings",
                            subtitle = "Save current settings to file",
                            onClick = {
                                val settings = viewModel.exportSettings()
                                // Handle export
                            },
                            icon = Icons.Default.Email
                        )

                        SettingsItem(
                            title = "Import Settings",
                            subtitle = "Load settings from file",
                            onClick = {
                                // Show file picker
                            },
                            icon = Icons.Default.Email
                        )
                    }
                }

                // About Section
                item {
                    SettingsGroup(title = "About") {
                        SettingsItem(
                            title = "Version",
                            subtitle = "1.0.0 (Build 1)",
                            onClick = { },
                            icon = Icons.Default.Info
                        )

                        SettingsItem(
                            title = "Privacy Policy",
                            subtitle = "Read our privacy policy",
                            onClick = { /* Open privacy policy */ },
                            icon = Icons.Default.Email
                        )

                        SettingsItem(
                            title = "Terms of Service",
                            subtitle = "Read terms and conditions",
                            onClick = { /* Open terms */ },
                            icon = Icons.Default.Email
                        )
                    }
                }
            }
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error snackbar
            viewModel.dismissError()
        }
    }
}