package com.pocketai.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketai.agent.data.device.DeviceMetrics
import com.pocketai.agent.ui.theme.*

@Composable
fun DeviceManagerScreen(
    metrics: DeviceMetrics,
    onNavigateBack: () -> Unit,
    onNavigateToBatterySettings: () -> Unit
) {
    Scaffold(
        containerColor = CharcoalDark,
        topBar = {
            Surface(color = SurfaceDark, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Device Resource Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Real-time battery, memory, storage & thermal metrics",
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
            // Battery Status Card
            item {
                MetricLargeCard(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery Level",
                    value = "${metrics.batteryPercent}%",
                    subtitle = if (metrics.isCharging) "Connected to Power (Charging)" else "Discharging normally",
                    color = if (metrics.batteryPercent > 20) StatusGreen else StatusRed,
                    progress = metrics.batteryPercent / 100f
                )
            }

            // Temperature & Thermal Condition
            item {
                MetricLargeCard(
                    icon = Icons.Default.Thermostat,
                    title = "Device Temperature",
                    value = "${metrics.batteryTemperatureCelsius}°C",
                    subtitle = if (metrics.batteryTemperatureCelsius < 38f) "Nominal Operating Range" else "Elevated Thermal State",
                    color = if (metrics.batteryTemperatureCelsius < 38f) StatusGreen else StatusYellow,
                    progress = (metrics.batteryTemperatureCelsius / 50f).coerceIn(0f, 1f)
                )
            }

            // RAM Usage
            item {
                val usedRam = metrics.totalRamMb - metrics.availableRamMb
                val ramPct = if (metrics.totalRamMb > 0) (usedRam.toFloat() / metrics.totalRamMb) else 0.5f
                MetricLargeCard(
                    icon = Icons.Default.Memory,
                    title = "Memory (RAM)",
                    value = "${usedRam} MB / ${metrics.totalRamMb} MB",
                    subtitle = "${metrics.availableRamMb} MB available",
                    color = StatusBlue,
                    progress = ramPct
                )
            }

            // Disk Storage
            item {
                MetricLargeCard(
                    icon = Icons.Default.Storage,
                    title = "Internal Storage",
                    value = "${metrics.availableStorageGb} GB Free",
                    subtitle = "Safe for projects & checkpoints",
                    color = OrangeAccent,
                    progress = 0.7f
                )
            }

            // Network
            item {
                MetricLargeCard(
                    icon = Icons.Default.Wifi,
                    title = "Network Connectivity",
                    value = metrics.networkType,
                    subtitle = if (metrics.isWifiConnected) "Eligible for large file sync & build downloads" else "Restricted under Wi-Fi Only Mode",
                    color = if (metrics.isWifiConnected) StatusGreen else StatusYellow,
                    progress = 1.0f
                )
            }

            // Settings Shortcut Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Background Execution Modes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Configure Balanced, Performance, Battery Saver, Charging Only, Wi-Fi Only, and Thermal Protection modes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToBatterySettings,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Configure Battery & Background Modes", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricLargeCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    progress: Float
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }

                Text(text = value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = color,
                trackColor = SurfaceDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
