package com.pocketai.agent.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketai.agent.ui.theme.*

@Composable
fun SettingsScreen(
    onNavigateToProviders: () -> Unit,
    onNavigateToUsage: () -> Unit,
    onNavigateToBattery: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToGitHub: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToDeviceManager: () -> Unit
) {
    var editorFontSize by remember { mutableStateOf(13) }
    var editorTabSize by remember { mutableStateOf(4) }

    Scaffold(
        containerColor = CharcoalDark,
        topBar = {
            Surface(color = SurfaceDark, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Workstation preferences and system modules",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            // Module Links Section Header
            item {
                Text(
                    text = "Modules & Integrations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            item {
                SettingsNavRow(
                    icon = Icons.Default.SmartToy,
                    title = "AI Providers & Models",
                    subtitle = "Gemini, Claude, DeepSeek, Antigravity, Free Mode",
                    onClick = onNavigateToProviders
                )
            }

            item {
                SettingsNavRow(
                    icon = Icons.Default.BarChart,
                    title = "API Usage & Quotas",
                    subtitle = "Token consumption and estimated billing costs",
                    onClick = onNavigateToUsage
                )
            }

            item {
                SettingsNavRow(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery & Background Modes",
                    subtitle = "Balanced, Performance, Battery Saver, Wi-Fi Only",
                    onClick = onNavigateToBattery
                )
            }

            item {
                SettingsNavRow(
                    icon = Icons.Default.Notifications,
                    title = "Notification Settings",
                    subtitle = "Task start, completion, failure, and approval alerts",
                    onClick = onNavigateToNotifications
                )
            }

            item {
                SettingsNavRow(
                    icon = Icons.Default.Code,
                    title = "GitHub Integration",
                    subtitle = "Repositories, commit history, push confirmation",
                    onClick = onNavigateToGitHub
                )
            }

            item {
                SettingsNavRow(
                    icon = Icons.Default.Lock,
                    title = "Security & Privacy",
                    subtitle = "Hardware Keystore, log redaction, emergency wipe",
                    onClick = onNavigateToSecurity
                )
            }

            item {
                SettingsNavRow(
                    icon = Icons.Default.Memory,
                    title = "Device Resource Manager",
                    subtitle = "Real-time battery, RAM, temperature, and storage",
                    onClick = onNavigateToDeviceManager
                )
            }

            // Editor Preferences
            item {
                Text(
                    text = "Editor Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Font Size", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(12, 13, 14, 16).forEach { size ->
                                    FilterChip(
                                        selected = editorFontSize == size,
                                        onClick = { editorFontSize = size },
                                        label = { Text("${size}sp") }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tab Indentation", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(2, 4).forEach { spaces ->
                                    FilterChip(
                                        selected = editorTabSize == spaces,
                                        onClick = { editorTabSize = spaces },
                                        label = { Text("$spaces spaces") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // About Section
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pocket AI Agent",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OrangeAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.0.0 (Production Build)\n" +
                                    "Target: Android 14 (API 34) • Min: Android 8.0 (API 26)\n" +
                                    "Engine: Jetpack Compose, Material 3, Room, WorkManager",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceDark,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
