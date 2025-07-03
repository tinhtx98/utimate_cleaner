package com.mars.ultimatecleaner.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mars.ultimatecleaner.domain.model.SafetyOptions

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )

            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )

            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun ListPreference(
    title: String,
    subtitle: String? = null,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsItem(
        title = title,
        subtitle = subtitle ?: selectedValue,
        onClick = { showDialog = true },
        icon = icon,
        modifier = modifier
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(option)
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = option == selectedValue,
                                onClick = {
                                    onValueChange(option)
                                    showDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TimePickerPreference(
    title: String,
    subtitle: String,
    morningHour: Int,
    eveningHour: Int,
    onTimeChange: (Int, Int) -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsItem(
        title = title,
        subtitle = subtitle,
        onClick = { showDialog = true },
        icon = icon,
        modifier = modifier
    )

    if (showDialog) {
        TimePickerDialog(
            morningHour = morningHour,
            eveningHour = eveningHour,
            onTimeChange = onTimeChange,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun SafetyOptionsPreference(
    title: String,
    subtitle: String,
    safetyOptions: SafetyOptions,
    onOptionsChange: (SafetyOptions) -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsItem(
        title = title,
        subtitle = subtitle,
        onClick = { showDialog = true },
        icon = icon,
        modifier = modifier
    )

    if (showDialog) {
        SafetyOptionsDialog(
            safetyOptions = safetyOptions,
            onOptionsChange = onOptionsChange,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun TimePickerDialog(
    morningHour: Int,
    eveningHour: Int,
    onTimeChange: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tempMorningHour by remember { mutableStateOf(morningHour) }
    var tempEveningHour by remember { mutableStateOf(eveningHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Times") },
        text = {
            Column {
                Text("Morning Notification")
                Slider(
                    value = tempMorningHour.toFloat(),
                    onValueChange = { tempMorningHour = it.toInt() },
                    valueRange = 6f..12f,
                    steps = 5
                )
                Text("${tempMorningHour}:00")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Evening Notification")
                Slider(
                    value = tempEveningHour.toFloat(),
                    onValueChange = { tempEveningHour = it.toInt() },
                    valueRange = 17f..23f,
                    steps = 5
                )
                Text("${tempEveningHour}:00")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeChange(tempMorningHour, tempEveningHour)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SafetyOptionsDialog(
    safetyOptions: SafetyOptions,
    onOptionsChange: (SafetyOptions) -> Unit,
    onDismiss: () -> Unit
) {
    var tempOptions by remember { mutableStateOf(safetyOptions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Safety Options") },
        text = {
            Column {
                SwitchPreference(
                    title = "Create Backup Before Clean",
                    checked = tempOptions.createBackupBeforeClean,
                    onCheckedChange = {
                        tempOptions = tempOptions.copy(createBackupBeforeClean = it)
                    }
                )

                SwitchPreference(
                    title = "Require Confirmation",
                    checked = tempOptions.requireConfirmation,
                    onCheckedChange = {
                        tempOptions = tempOptions.copy(requireConfirmation = it)
                    }
                )

                SwitchPreference(
                    title = "Protect Recent Files",
                    checked = tempOptions.protectRecentFiles,
                    onCheckedChange = {
                        tempOptions = tempOptions.copy(protectRecentFiles = it)
                    }
                )

                SwitchPreference(
                    title = "Enable File Recovery",
                    checked = tempOptions.enableFileRecovery,
                    onCheckedChange = {
                        tempOptions = tempOptions.copy(enableFileRecovery = it)
                    }
                )

                SwitchPreference(
                    title = "Whitelist System Files",
                    checked = tempOptions.whitelistSystemFiles,
                    onCheckedChange = {
                        tempOptions = tempOptions.copy(whitelistSystemFiles = it)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOptionsChange(tempOptions)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}