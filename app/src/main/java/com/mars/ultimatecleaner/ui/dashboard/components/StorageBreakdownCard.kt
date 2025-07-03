package com.mars.ultimatecleaner.ui.dashboard.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StorageBreakdownCard(
    categoryBreakdown: Map<String, Long>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Storage Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pie Chart
                    PieChart(
                        data = categoryBreakdown,
                        modifier = Modifier.size(120.dp)
                    )

                    // Legend
                    CategoryLegend(
                        categories = categoryBreakdown,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PieChart(
    data: Map<String, Long>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum().toFloat()
    val colors = getCategoryColors()

    val animatedValues = data.map { (category, size) ->
        val targetValue = if (total > 0) size.toFloat() / total else 0f
        category to animateFloatAsState(
            targetValue = targetValue,
            animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
            label = "pie_${category}"
        ).value
    }.toMap()

    Canvas(modifier = modifier) {
        if (total > 0) {
            drawPieChart(animatedValues, colors)
        }
    }
}

private fun DrawScope.drawPieChart(
    data: Map<String, Float>,
    colors: Map<String, Color>
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2

    var startAngle = 0f

    data.forEach { (category, percentage) ->
        val sweepAngle = 360f * percentage
        val color = colors[category] ?: Color.Gray

        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(
                center.x - radius,
                center.y - radius
            ),
            size = Size(radius * 2, radius * 2)
        )

        startAngle += sweepAngle
    }
}

@Composable
private fun CategoryLegend(
    categories: Map<String, Long>,
    modifier: Modifier = Modifier
) {
    val colors = getCategoryColors()
    val total = categories.values.sum()

    Column(
        modifier = modifier.padding(start = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (category, size) ->
            val percentage = if (total > 0) (size.toFloat() / total * 100).toInt() else 0

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(colors[category] ?: Color.Gray)
                )

                Column {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$percentage% • ${formatFileSize(size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

private fun getCategoryColors(): Map<String, Color> {
    return mapOf(
        "Images" to Color(0xFF2196F3),
        "Videos" to Color(0xFF9C27B0),
        "Audio" to Color(0xFF4CAF50),
        "Documents" to Color(0xFFFF9800),
        "Apps" to Color(0xFFF44336),
        "Other" to Color(0xFF607D8B)
    )
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