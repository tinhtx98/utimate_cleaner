package com.mars.ultimatecleaner.ui.dashboard.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mars.ultimatecleaner.domain.model.StorageInfo
import com.mars.ultimatecleaner.ui.theme.StorageGreen
import com.mars.ultimatecleaner.ui.theme.StorageOrange
import com.mars.ultimatecleaner.ui.theme.StorageRed

@Composable
fun StorageStatusCard(
    storageInfo: StorageInfo,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Storage",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                StorageCircularProgress(
                    percentage = storageInfo.usagePercentage,
                    totalSpace = storageInfo.totalSpace,
                    usedSpace = storageInfo.usedSpace,
                    freeSpace = storageInfo.freeSpace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StorageMetric(
                    label = "Used",
                    value = formatFileSize(storageInfo.usedSpace),
                    color = getStorageColor(storageInfo.usagePercentage)
                )

                StorageMetric(
                    label = "Free",
                    value = formatFileSize(storageInfo.freeSpace),
                    color = Color.Gray
                )

                StorageMetric(
                    label = "Total",
                    value = formatFileSize(storageInfo.totalSpace),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StorageCircularProgress(
    percentage: Float,
    totalSpace: Long,
    usedSpace: Long,
    freeSpace: Long
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "storage_progress"
    )

    val strokeWidth = 12.dp
    val circleSize = 160.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(circleSize)
    ) {
        Canvas(
            modifier = Modifier.size(circleSize)
        ) {
            val strokeWidthPx = strokeWidth.toPx()
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension - strokeWidthPx) / 2

            // Background circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = Stroke(strokeWidthPx, cap = StrokeCap.Round)
            )

            // Progress arc
            val sweepAngle = 360f * animatedProgress
            drawArc(
                color = getStorageColor(percentage),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = Size(radius * 2, radius * 2),
                style = Stroke(strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = getStorageColor(percentage)
            )
            Text(
                text = "Used",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun StorageMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

private fun getStorageColor(percentage: Float): Color {
    return when {
        percentage >= 90 -> StorageRed
        percentage >= 75 -> StorageOrange
        else -> StorageGreen
    }
}

private fun formatFileSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return "%.1f %s".format(size, units[unitIndex])
}