package com.mars.ultimatecleaner.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mars.ultimatecleaner.R
import com.mars.ultimatecleaner.domain.model.AppPermission
import com.mars.ultimatecleaner.ui.dashboard.components.*
import com.mars.ultimatecleaner.ui.dashboard.widgets.*
import com.mars.ultimatecleaner.ui.main.component.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(), onPermissionRequest: (AppPermission) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startPeriodicUpdates()
    }

    PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = {
        viewModel.refreshData()
    }, content = {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(title = {
                Text(stringResource(R.string.app_name))
            }, actions = {
                IconButton(onClick = { viewModel.refreshData() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh, contentDescription = "Refresh"
                    )
                }
            })

            // Main Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Critical Alerts Section
                if (uiState.criticalAlerts.isNotEmpty()) {
                    item {
                        CriticalAlertsSection(
                            alerts = uiState.criticalAlerts, onAlertAction = viewModel::handleAlertAction
                        )
                    }
                }

                // Device Health Overview
                item {
                    DeviceHealthCard(
                        healthScore = uiState.deviceHealth.overallScore,
                        isLoading = uiState.isLoading,
                        onClick = { /* Navigate to detailed health view */ })
                }

                // Storage Status
                item {
                    StorageStatusCard(
                        storageInfoDomain = uiState.storageInfo,
                        isLoading = uiState.isLoading,
                        onClick = { /* Navigate to storage details */ })
                }

                // Storage Breakdown Chart
                item {
                    StorageBreakdownCard(
                        categoryBreakdown = uiState.storageInfo.categoryBreakdown, isLoading = uiState.isLoading
                    )
                }

                // Quick Actions
                item {
                    QuickActionsSection(
                        actions = uiState.quickActions, onActionClick = viewModel::executeQuickAction
                    )
                }

                // Performance Metrics
                item {
                    PerformanceMetricsCard(
                        metrics = uiState.performanceMetrics, isLoading = uiState.isLoading
                    )
                }

                // Recent Activity
                if (uiState.recentActivity.isNotEmpty()) {
                    item {
                        RecentActivityCard(
                            activities = uiState.recentActivity, onViewAll = { /* Navigate to activity history */ })
                    }
                }

                // Background Tasks
                if (uiState.backgroundTasks.isNotEmpty()) {
                    item {
                        BackgroundTasksCard(
                            tasks = uiState.backgroundTasks, onTaskCancel = viewModel::cancelBackgroundTask
                        )
                    }
                }
            }
        }
    })

    // Permission Dialog
    uiState.permissionRequest?.let { permission ->
        PermissionRequestDialog(
            permission = permission,
            onRequest = { onPermissionRequest(permission) },
            onDismiss = { viewModel.dismissPermissionRequest() })
    }

    // Loading Overlay
    if (uiState.isInitialLoading) {
        LoadingOverlay()
    }
}