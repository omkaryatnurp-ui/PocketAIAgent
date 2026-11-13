package com.pocketai.agent.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketai.agent.data.device.DeviceMetrics
import com.pocketai.agent.data.local.entities.AgentMessageEntity
import com.pocketai.agent.data.local.entities.AiProviderEntity
import com.pocketai.agent.data.local.entities.ProjectEntity
import com.pocketai.agent.data.local.entities.TaskRecordEntity
import com.pocketai.agent.ui.theme.*

@Composable
fun DashboardScreen(
    activeProvider: AiProviderEntity?,
    runningTasks: List<TaskRecordEntity>,
    queuedTasks: List<TaskRecordEntity>,
    recentProjects: List<ProjectEntity>,
    recentActivity: List<AgentMessageEntity>,
    deviceMetrics: DeviceMetrics,
    onNavigateToProject: (String) -> Unit,
    onNavigateToAgent: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToDeviceManager: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToNewProject: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Bar & Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pocket AI Agent",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Mobile AI Coding Workstation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = onNavigateToDeviceManager,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = CardDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "Device Manager",
                        tint = OrangeAccent
                    )
                }
            }
        }

        // Active AI Provider & Model Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToProviders() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (activeProvider?.isEnabled == true) StatusGreen else StatusYellow)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active AI Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceDark
                        ) {
                            Text(
                                text = if (activeProvider?.isFreeTier == true) "FREE TIER" else "PAID API",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeProvider?.isFreeTier == true) StatusGreen else OrangeAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = activeProvider?.displayName ?: "Free Mode (Local Rule Engine)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Model: ${activeProvider?.selectedModel ?: "local-rule-engine"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OrangeAccentLight
                    )
                }
            }
        }

        // Quick Stats: Running Tasks, Queued Tasks, Battery, Storage
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Running",
                    value = "${runningTasks.size}",
                    subtitle = "Active tasks",
                    tint = StatusGreen,
                    onClick = onNavigateToTasks
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Queued",
                    value = "${queuedTasks.size}",
                    subtitle = "Waiting tasks",
                    tint = StatusYellow,
                    onClick = onNavigateToTasks
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Battery",
                    value = "${deviceMetrics.batteryPercent}%",
                    subtitle = if (deviceMetrics.isCharging) "Charging" else "Discharging",
                    tint = if (deviceMetrics.batteryPercent > 20) StatusGreen else StatusRed,
                    onClick = onNavigateToDeviceManager
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Storage",
                    value = "${deviceMetrics.availableStorageGb}G",
                    subtitle = "Available",
                    tint = StatusBlue,
                    onClick = onNavigateToDeviceManager
                )
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToNewProject,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = CharcoalBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Project", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateToAgent,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeAccent),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch Agent", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Recent Projects Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Projects",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${recentProjects.size} total",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        if (recentProjects.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No projects created yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToNewProject,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Create Starter Project", color = CharcoalBlack)
                        }
                    }
                }
            }
        } else {
            items(recentProjects.take(3)) { project ->
                ProjectListItem(
                    project = project,
                    onClick = { onNavigateToProject(project.id) }
                )
            }
        }

        // Recent Agent Activity Section Header
        item {
            Text(
                text = "Recent Agent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        if (recentActivity.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No agent actions recorded yet. Start a task to see live progress.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(recentActivity.take(4)) { activity ->
                AgentActivityItem(activity = activity)
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = tint, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
        }
    }
}

@Composable
fun ProjectListItem(
    project: ProjectEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
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
                shape = RoundedCornerShape(10.dp),
                color = SurfaceDark,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${project.projectType} • ${project.fileCount} files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted
            )
        }
    }
}

@Composable
fun AgentActivityItem(activity: AgentMessageEntity) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = OrangeGlow,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activity.senderRole.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = OrangeAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = activity.senderRole,
                        style = MaterialTheme.typography.labelSmall,
                        color = OrangeAccentLight,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (activity.isApprovedByUser) "APPROVED" else "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activity.isApprovedByUser) StatusGreen else TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activity.content.take(120),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2
                )
            }
        }
    }
}
