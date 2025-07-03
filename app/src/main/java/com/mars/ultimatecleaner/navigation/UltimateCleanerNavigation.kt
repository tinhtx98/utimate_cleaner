package com.mars.ultimatecleaner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mars.ultimatecleaner.ui.screens.home.HomeScreen
import com.mars.ultimatecleaner.ui.screens.cleaner.CleanerScreen
import com.mars.ultimatecleaner.ui.screens.filemanager.FileManagerScreen
import com.mars.ultimatecleaner.ui.screens.optimizer.OptimizerScreen
import com.mars.ultimatecleaner.ui.screens.settings.SettingsScreen

@Composable
fun UltimateCleanerNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("cleaner") {
            CleanerScreen(navController = navController)
        }
        composable("filemanager") {
            FileManagerScreen(navController = navController)
        }
        composable("optimizer") {
            OptimizerScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}