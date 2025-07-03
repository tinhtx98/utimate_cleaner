package com.mars.ultimatecleaner.ui.screens.cleaner

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mars.ultimatecleaner.ui.components.GradientCard
import com.mars.ultimatecleaner.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(navController: NavController) {
    val hapticFeedback = LocalHapticFeedback.current
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var showResults by remember { mutableStateOf(false) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            for (i in 0..100) {
                scanProgress = i / 100f
                delay(50)
            }
            isScanning = false
            showResults = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Cleaner") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scan Button
            item {
                ScanButton(
                    isScanning = isScanning,
                    progress = scanProgress,
                    onClick = {
                        if (!isScanning) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            isScanning = true
                            showResults = false
                        }
                    }
                )
            }

            // Results Section
            if (showResults) {
                item {
                    CleaningResultsCard()
                }

                item {
                    Text(
                        "Junk Files Found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(getJunkCategories()) { category ->
                    JunkCategoryItem(category)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    CleanAllButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScanButton(
    isScanning: Boolean,
    progress: Float,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isScanning) 0.95f else 1f,
        animationSpec = tween(150),
        label = "scan_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "scan_animation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        GradientCard(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .clickable { onClick() },
            gradient = Brush.radialGradient(
                colors = listOf(CleanerBlue, CleanerPurple)
            ),
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.size(80.dp),
                            color = Color.White,
                            strokeWidth = 6.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Scanning...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CleaningServices,
                            contentDescription = "Scan",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Start Scan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CleaningResultsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CleanerGreen.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Scan Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CleanerGreen
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultItem("Junk Files", "2.3 GB", CleanerRed)
                ResultItem("Cache Files", "1.8 GB", CleanerOrange)
                ResultItem("Duplicates", "856 MB", CleanerBlue)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Total: 4.96 GB can be freed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CleanerGreen
                )
            }
        }
    }
}

@Composable
fun ResultItem(title: String, size: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = size,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun JunkCategoryItem(category: JunkCategory) {
    var isExpanded by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { isSelected = it },
                    colors = CheckboxDefaults.colors(checkedColor = category.color)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.color,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${category.fileCount} files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Text(
                    text = category.size,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = category.color
                )

                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    category.subItems.forEach { subItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(40.dp))
                            Text(
                                text = subItem.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = subItem.size,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanAllButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CleanerGreen),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "Clean All",
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Clean All (4.96 GB)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// Data classes
data class JunkCategory(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val size: String,
    val fileCount: Int,
    val subItems: List<JunkSubItem>
)

data class JunkSubItem(
    val name: String,
    val size: String
)

fun getJunkCategories(): List<JunkCategory> {
    return listOf(
        JunkCategory(
            "Cache Files",
            Icons.Default.Storage,
            CleanerOrange,
            "1.8 GB",
            245,
            listOf(
                JunkSubItem("Chrome cache", "456 MB"),
                JunkSubItem("Instagram cache", "234 MB"),
                JunkSubItem("WhatsApp cache", "178 MB")
            )
        ),
        JunkCategory(
            "Temporary Files",
            Icons.Default.DeleteSweep,
            CleanerRed,
            "567 MB",
            89,
            listOf(
                JunkSubItem("Download temp", "234 MB"),
                JunkSubItem("App temp files", "333 MB")
            )
        ),
        JunkCategory(
            "APK Files",
            Icons.Default.InstallDesktop,
            CleanerBlue,
            "234 MB",
            12,
            listOf(
                JunkSubItem("Old APK files", "234 MB")
            )
        ),
        JunkCategory(
            "Empty Folders",
            Icons.Default.FolderOff,
            CleanerPurple,
            "0 MB",
            23,
            listOf(
                JunkSubItem("Empty directories", "0 MB")
            )
        )
    )
}