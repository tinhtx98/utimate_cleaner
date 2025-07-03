package com.mars.ultimatecleaner.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mars.ultimatecleaner.ui.components.StorageChart
import com.mars.ultimatecleaner.ui.components.GradientCard
import com.mars.ultimatecleaner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Ultimate Cleaner",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(
                    onClick = { navController.navigate("settings") }
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Overview
            item {
                StorageOverviewCard()
            }

            // Quick Actions
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                QuickActionsGrid(
                    onCleanClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate("cleaner")
                    },
                    onFileManagerClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate("filemanager")
                    },
                    onOptimizerClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate("optimizer")
                    },
                    onSettingsClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate("settings")
                    }
                )
            }

            // Smart Suggestions
            item {
                SmartSuggestionsCard()
            }

            // Recent Activity
            item {
                RecentActivityCard()
            }

            // System Health
            item {
                SystemHealthCard()
            }
        }
    }
}

@Composable
fun StorageOverviewCard() {
    val animatedProgress by animateFloatAsState(
        targetValue = 0.65f,
        animationSpec = tween(1000),
        label = "storage_progress"
    )

    GradientCard(
        gradient = Brush.horizontalGradient(
            colors = listOf(CleanerBlue, CleanerPurple)
        )
    ) {
        Column {
            Text(
                "Storage Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Used: 26.5 GB",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        "Free: 14.2 GB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.size(80.dp),
                        color = Color.White,
                        strokeWidth = 8.dp
                    )
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsGrid(
    onCleanClick: () -> Unit,
    onFileManagerClick: () -> Unit,
    onOptimizerClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val actions = listOf(
        QuickAction("Clean", Icons.Default.CleaningServices, CleanerGreen, onCleanClick),
        QuickAction("Files", Icons.Default.Folder, CleanerBlue, onFileManagerClick),
        QuickAction("Optimize", Icons.Default.Tune, CleanerOrange, onOptimizerClick),
        QuickAction("Settings", Icons.Default.Settings, CleanerPurple, onSettingsClick)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { action ->
            QuickActionCard(
                action = action,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionCard(
    action: QuickAction,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = action.color,
        animationSpec = tween(300),
        label = "action_color"
    )

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { action.onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = animatedColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = animatedColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = animatedColor
            )
        }
    }
}

@Composable
fun SmartSuggestionsCard() {
    val suggestions = listOf(
        "Clear 2.3 GB of cache files",
        "Delete 156 duplicate photos",
        "Remove 23 unused APK files"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = "Smart Suggestions",
                    tint = CleanerOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Smart Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            suggestions.forEach { suggestion ->
                SuggestionItem(suggestion)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SuggestionItem(suggestion: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(CleanerGreen, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RecentActivityCard() {
    val activities = listOf(
        RecentActivity("Cleaned cache files", "2 hours ago", "1.2 GB freed"),
        RecentActivity("Deleted duplicates", "Yesterday", "856 MB freed"),
        RecentActivity("Optimized photos", "2 days ago", "345 MB freed")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            activities.forEach { activity ->
                RecentActivityItem(activity)
                if (activity != activities.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun RecentActivityItem(activity: RecentActivity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(CleanerBlue.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = CleanerBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.action,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = activity.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Text(
            text = activity.result,
            style = MaterialTheme.typography.bodySmall,
            color = CleanerGreen,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SystemHealthCard() {
    val healthItems = listOf(
        HealthItem("Storage", 65, CleanerOrange),
        HealthItem("Memory", 45, CleanerGreen),
        HealthItem("Battery", 80, CleanerRed),
        HealthItem("Performance", 85, CleanerBlue)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "System Health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            healthItems.forEach { item ->
                HealthIndicator(item)
                if (item != healthItems.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun HealthIndicator(item: HealthItem) {
    val animatedProgress by animateFloatAsState(
        targetValue = item.percentage / 100f,
        animationSpec = tween(1000),
        label = "health_progress"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp)
        )

        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = item.color
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${item.percentage}%",
            style = MaterialTheme.typography.bodySmall,
            color = item.color,
            fontWeight = FontWeight.Medium
        )
    }
}

// Data classes
data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

data class RecentActivity(
    val action: String,
    val time: String,
    val result: String
)

data class HealthItem(
    val name: String,
    val percentage: Int,
    val color: Color
)