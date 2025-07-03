package com.mars.ultimatecleaner.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mars.ultimatecleaner.domain.model.*
import com.mars.ultimatecleaner.domain.repository.*
import com.mars.ultimatecleaner.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val cleaningRepository: CleaningRepository,
    private val systemHealthRepository: SystemHealthRepository,
    private val permissionRepository: PermissionRepository,
    private val getDeviceHealthUseCase: GetDeviceHealthUseCase,
    private val getStorageInfoUseCase: GetStorageInfoUseCase,
    private val getQuickActionsUseCase: GetQuickActionsUseCase,
    private val executeQuickActionUseCase: ExecuteQuickActionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val isLoading = _uiState.map { it.isLoading }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    init {
        loadInitialData()
        observeBackgroundTasks()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInitialLoading = true)

            try {
                // Load all dashboard data in parallel
                val deviceHealth = getDeviceHealthUseCase()
                val storageInfo = getStorageInfoUseCase()
                val quickActions = getQuickActionsUseCase()
                val performanceMetrics = systemHealthRepository.getPerformanceMetrics()
                val recentActivity = cleaningRepository.getRecentActivity(limit = 5)
                val criticalAlerts = generateCriticalAlerts(deviceHealth, storageInfo)

                _uiState.value = _uiState.value.copy(
                    deviceHealth = deviceHealth,
                    storageInfo = storageInfo,
                    quickActions = quickActions,
                    performanceMetrics = performanceMetrics,
                    recentActivity = recentActivity,
                    criticalAlerts = criticalAlerts,
                    isInitialLoading = false,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isInitialLoading = false,
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)

            try {
                val currentState = _uiState.value

                // Refresh all data
                val deviceHealth = getDeviceHealthUseCase()
                val storageInfo = getStorageInfoUseCase()
                val quickActions = getQuickActionsUseCase()
                val performanceMetrics = systemHealthRepository.getPerformanceMetrics()
                val criticalAlerts = generateCriticalAlerts(deviceHealth, storageInfo)

                _uiState.value = currentState.copy(
                    deviceHealth = deviceHealth,
                    storageInfo = storageInfo,
                    quickActions = quickActions,
                    performanceMetrics = performanceMetrics,
                    criticalAlerts = criticalAlerts,
                    isRefreshing = false,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message
                )
            }
        }
    }

    fun startPeriodicUpdates() {
        viewModelScope.launch {
            // Update every 30 seconds
            kotlinx.coroutines.delay(30000)
            while (true) {
                if (!_uiState.value.isRefreshing && !_uiState.value.isInitialLoading) {
                    updateLiveMetrics()
                }
                kotlinx.coroutines.delay(30000)
            }
        }
    }

    private suspend fun updateLiveMetrics() {
        try {
            val currentState = _uiState.value
            val updatedMetrics = systemHealthRepository.getPerformanceMetrics()
            val updatedStorageInfo = getStorageInfoUseCase()

            _uiState.value = currentState.copy(
                performanceMetrics = updatedMetrics,
                storageInfo = updatedStorageInfo,
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // Silent update failure - don't disrupt user experience
        }
    }

    fun executeQuickAction(action: QuickAction) {
        viewModelScope.launch {
            try {
                val result = executeQuickActionUseCase(action)

                if (result.isSuccess) {
                    // Refresh relevant data after successful action
                    when (action.type) {
                        QuickActionType.CACHE_CLEANUP -> refreshStorageData()
                        QuickActionType.JUNK_CLEANUP -> refreshStorageData()
                        QuickActionType.OPTIMIZE_PERFORMANCE -> refreshPerformanceData()
                        else -> refreshData()
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = result.errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }

    private suspend fun refreshStorageData() {
        val updatedStorageInfo = getStorageInfoUseCase()
        val updatedDeviceHealth = getDeviceHealthUseCase()
        val criticalAlerts = generateCriticalAlerts(updatedDeviceHealth, updatedStorageInfo)

        _uiState.value = _uiState.value.copy(
            storageInfo = updatedStorageInfo,
            deviceHealth = updatedDeviceHealth,
            criticalAlerts = criticalAlerts
        )
    }

    private suspend fun refreshPerformanceData() {
        val updatedMetrics = systemHealthRepository.getPerformanceMetrics()
        val updatedDeviceHealth = getDeviceHealthUseCase()

        _uiState.value = _uiState.value.copy(
            performanceMetrics = updatedMetrics,
            deviceHealth = updatedDeviceHealth
        )
    }

    fun handleAlertAction(alert: CriticalAlert, action: AlertAction) {
        viewModelScope.launch {
            when (action) {
                AlertAction.DISMISS -> dismissAlert(alert)
                AlertAction.FIX_NOW -> executeAlertFix(alert)
                AlertAction.REMIND_LATER -> scheduleAlertReminder(alert)
            }
        }
    }

    private fun dismissAlert(alert: CriticalAlert) {
        val currentAlerts = _uiState.value.criticalAlerts.toMutableList()
        currentAlerts.remove(alert)
        _uiState.value = _uiState.value.copy(criticalAlerts = currentAlerts)
    }

    private suspend fun executeAlertFix(alert: CriticalAlert) {
        try {
            when (alert.type) {
                AlertType.LOW_STORAGE -> {
                    val cleanupAction = QuickAction(
                        id = "emergency_cleanup",
                        type = QuickActionType.JUNK_CLEANUP,
                        title = "Emergency Cleanup",
                        description = "Free up storage space",
                        icon = R.drawable.ic_cleaning,
                        isEnabled = true
                    )
                    executeQuickAction(cleanupAction)
                }
                AlertType.PERFORMANCE_ISSUE -> {
                    val optimizeAction = QuickAction(
                        id = "optimize_performance",
                        type = QuickActionType.OPTIMIZE_PERFORMANCE,
                        title = "Optimize Performance",
                        description = "Boost device performance",
                        icon = R.drawable.ic_speed,
                        isEnabled = true
                    )
                    executeQuickAction(optimizeAction)
                }
                AlertType.PERMISSION_REQUIRED -> {
                    requestPermission(alert.requiredPermission!!)
                }
                else -> {
                    // Handle other alert types
                }
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }

    private fun scheduleAlertReminder(alert: CriticalAlert) {
        // Schedule reminder for later
        dismissAlert(alert)
    }

    private fun requestPermission(permission: AppPermission) {
        _uiState.value = _uiState.value.copy(permissionRequest = permission)
    }

    fun onPermissionResult(result: PermissionResult) {
        _uiState.value = _uiState.value.copy(permissionRequest = null)

        if (result.isGranted) {
            // Refresh data after permission granted
            refreshData()
        }
    }

    fun dismissPermissionRequest() {
        _uiState.value = _uiState.value.copy(permissionRequest = null)
    }

    private fun observeBackgroundTasks() {
        viewModelScope.launch {
            // Observe background task progress
            // This would be connected to WorkManager observables
            kotlinx.coroutines.delay(1000) // Placeholder
        }
    }

    fun cancelBackgroundTask(taskId: String) {
        viewModelScope.launch {
            try {
                // Cancel the background task
                val updatedTasks = _uiState.value.backgroundTasks.filter { it.id != taskId }
                _uiState.value = _uiState.value.copy(backgroundTasks = updatedTasks)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun generateCriticalAlerts(
        deviceHealth: DeviceHealth,
        storageInfo: StorageInfo
    ): List<CriticalAlert> {
        val alerts = mutableListOf<CriticalAlert>()

        // Low storage alert
        if (storageInfo.usagePercentage > 90) {
            alerts.add(
                CriticalAlert(
                    id = "low_storage",
                    type = AlertType.LOW_STORAGE,
                    title = "Storage Almost Full",
                    message = "Only ${storageInfo.freeSpace} left. Clean up now to prevent issues.",
                    severity = AlertSeverity.CRITICAL,
                    actionText = "Clean Now"
                )
            )
        } else if (storageInfo.usagePercentage > 80) {
            alerts.add(
                CriticalAlert(
                    id = "storage_warning",
                    type = AlertType.STORAGE_WARNING,
                    title = "Storage Running Low",
                    message = "Consider cleaning up files to free space.",
                    severity = AlertSeverity.WARNING,
                    actionText = "Optimize"
                )
            )
        }

        // Performance alerts
        if (deviceHealth.overallScore < 50) {
            alerts.add(
                CriticalAlert(
                    id = "performance_issue",
                    type = AlertType.PERFORMANCE_ISSUE,
                    title = "Performance Issues Detected",
                    message = "Your device performance is below optimal. Run optimization.",
                    severity = AlertSeverity.WARNING,
                    actionText = "Optimize"
                )
            )
        }

        // Permission alerts
        if (!permissionRepository.hasRequiredPermissions()) {
            alerts.add(
                CriticalAlert(
                    id = "permissions_needed",
                    type = AlertType.PERMISSION_REQUIRED,
                    title = "Permissions Required",
                    message = "Grant storage permissions for full functionality.",
                    severity = AlertSeverity.INFO,
                    actionText = "Grant",
                    requiredPermission = AppPermission.STORAGE
                )
            )
        }

        return alerts
    }
}