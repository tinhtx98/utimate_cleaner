package com.mars.ultimatecleaner.ui.screens.optimizer

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mars.ultimatecleaner.R
import com.mars.ultimatecleaner.ui.screens.optimizer.components.*
import com.mars.ultimatecleaner.ui.screens.optimizer.tabs.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizerScreen(
    viewModel: OptimizerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val optimizationProgress by viewModel.optimizationProgress.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text(stringResource(R.string.optimizer_title)) },
            actions = {
                IconButton(
                    onClick = { /* Navigate to settings */ }
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Quick") },
                icon = { Icon(Icons.Default.Speed, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Deep") },
                icon = { Icon(Icons.Default.Psychology, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Tools") },
                icon = { Icon(Icons.Default.Build, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("History") },
                icon = { Icon(Icons.Default.History, contentDescription = null) }
            )
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> QuickOptimizerTab(
                uiState = uiState,
                onQuickOptimization = viewModel::performQuickOptimization,
                onDismissResult = viewModel::dismissOptimizationResult
            )
            1 -> DeepOptimizerTab(
                uiState = uiState,
                onDeepOptimization = viewModel::performDeepOptimization,
                onScheduleOptimization = viewModel::scheduleOptimization
            )
            2 -> ToolsTab(
                uiState = uiState,
                onFindDuplicates = viewModel::findDuplicates,
                onAnalyzePhotos = viewModel::analyzePhotos,
                onOptimizeBattery = viewModel::optimizeBattery
            )
            3 -> HistoryTab(
                optimizationHistory = uiState.optimizationHistory
            )
        }
    }

    // Optimization Progress Dialog
    optimizationProgress?.let { progress ->
        OptimizationProgressDialog(
            progress = progress,
            onDismiss = { /* Only allow dismiss when complete */ }
        )
    }

    // Optimization Result Dialog
    uiState.lastOptimizationResult?.let { result ->
        OptimizationResultDialog(
            result = result,
            onDismiss = viewModel::dismissOptimizationResult,
            onViewDetails = { /* Navigate to detailed results */ }
        )
    }

    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar
            viewModel.dismissError()
        }
    }

    // Loading Overlay
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}