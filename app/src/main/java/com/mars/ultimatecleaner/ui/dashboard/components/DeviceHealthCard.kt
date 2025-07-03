package com.mars.ultimatecleaner.ui.dashboard.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mars.ultimatecleaner.ui.theme.*

@Composable
fun DeviceHealthCard(
    healthScore: Int,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateIntAsState(
        targetValue = healthScore,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic),
        label = "health_score"
    )

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
                text = "Device Health",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                HealthScoreIndicator(score = animatedScore)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = getHealthStatusText(animatedScore),
                style = MaterialTheme.typography.bodyLarge,
                color = getHealthColor(animatedScore)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = getHealthDescription(animatedScore),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun HealthScoreIndicator(score: Int) {
    val strokeWidth = 8.dp
    val circleSize = 120.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(circleSize)
    ) {
        Canvas(
            modifier = Modifier.size(circleSize)
        ) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (size.minDimension - strokeWidthPx) / 2
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

            // Background circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = Stroke(strokeWidthPx)
            )

            // Score arc
            val sweepAngle = 360f * (score / 100f)
            drawArc(
                color = getHealthColor(score),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "$score",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = getHealthColor(score)
        )
    }
}

private fun getHealthColor(score: Int): Color {
    return when {
        score >= 80 -> HealthExcellent
        score >= 60 -> HealthGood
        score >= 40 -> HealthFair
        else -> HealthPoor
    }
}

private fun getHealthStatusText(score: Int): String {
    return when {
        score >= 80 -> "Excellent"
        score >= 60 -> "Good"
        score >= 40 -> "Fair"
        else -> "Needs Attention"
    }
}

private fun getHealthDescription(score: Int): String {
    return when {
        score >= 80 -> "Your device is running optimally"
        score >= 60 -> "Your device is performing well"
        score >= 40 -> "Some optimization recommended"
        else -> "Immediate optimization needed"
    }
}