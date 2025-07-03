package com.mars.ultimatecleaner.ui.dashboard.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mars.ultimatecleaner.domain.model.*

@Composable
fun CriticalAlertsSection(
    alerts: List<CriticalAlert>,
    onAlertAction: (CriticalAlert, AlertAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Alerts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
            visible = alerts.isNotEmpty(),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = alerts,
                    key = { it.id }
                ) { alert ->
                    AlertCard(
                        alert = alert,
                        onAction = { action -> onAlertAction(alert, action) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: CriticalAlert,
    onAction: (AlertAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        AlertSeverity.INFO -> MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        AlertSeverity.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getAlertIcon(alert.type),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { onAction(AlertAction.DISMISS) }
                ) {
                    Text("Dismiss")
                }

                Button(
                    onClick = { onAction(AlertAction.FIX_NOW) }
                ) {
                    Text(alert.actionText)
                }
            }
        }
    }
}

private fun getAlertIcon(type: AlertType): ImageVector {
    return when (type) {
        AlertType.LOW_STORAGE -> Icons.Default.Storage
        AlertType.STORAGE_WARNING -> Icons.Default.Warning
        AlertType.PERFORMANCE_ISSUE -> Icons.Default.Speed
        AlertType.PERMISSION_REQUIRED -> Icons.Default.Security
        AlertType.SECURITY_WARNING -> Icons.Default.Shield
        AlertType.MAINTENANCE_DUE -> Icons.Default.Build
    }
}