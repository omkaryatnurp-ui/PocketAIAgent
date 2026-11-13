package com.pocketai.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketai.agent.ui.theme.*

@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit
) {
    var taskStarted by remember { mutableStateOf(true) }
    var taskCompleted by remember { mutableStateOf(true) }
    var taskFailed by remember { mutableStateOf(true) }
    var buildStatus by remember { mutableStateOf(true) }
    var approvalRequired by remember { mutableStateOf(true) }
    var usageLimitAlerts by remember { mutableStateOf(true) }
    var taskPauseResume by remember { mutableStateOf(true) }

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
                            text = "Notification Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Configure alerts for tasks, builds, and approvals",
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notification Transparency",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Background AI coding tasks show ongoing foreground notifications with interactive Pause, Resume, and Cancel actions. If notification permissions are disabled, background progress updates will not appear on your lock screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                NotificationToggleCard(
                    title = "Task Started Alerts",
                    description = "Notify when a scheduled or multi-agent task starts running",
                    checked = taskStarted,
                    onCheckedChange = { taskStarted = it }
                )
            }

            item {
                NotificationToggleCard(
                    title = "Task Completed Alerts",
                    description = "Alert when an agent finishes writing code and testing",
                    checked = taskCompleted,
                    onCheckedChange = { taskCompleted = it }
                )
            }

            item {
                NotificationToggleCard(
                    title = "Task Failure Alerts",
                    description = "Notify immediately if a network error or runtime exception occurs",
                    checked = taskFailed,
                    onCheckedChange = { taskFailed = it }
                )
            }

            item {
                NotificationToggleCard(
                    title = "Build & Test Completed",
                    description = "Alert when Gradle or local unit test checks pass or fail",
                    checked = buildStatus,
                    onCheckedChange = { buildStatus = it }
                )
            }

            item {
                NotificationToggleCard(
                    title = "Approval Required",
                    description = "High priority notification when Planner Agent awaits user approval",
                    checked = approvalRequired,
                    onCheckedChange = { approvalRequired = it }
                )
            }

            item {
                NotificationToggleCard(
                    title = "API Usage & Quota Warnings",
                    description = "Alert when approaching free-tier rate limits or token budgets",
                    checked = usageLimitAlerts,
                    onCheckedChange = { usageLimitAlerts = it }
                )
            }

            item {
                NotificationToggleCard(
                    title = "Pause & Resume Actions",
                    description = "Provide interactive Pause and Resume buttons in notification shade",
                    checked = taskPauseResume,
                    onCheckedChange = { taskPauseResume = it }
                )
            }
        }
    }
}

@Composable
private fun NotificationToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CharcoalDark,
                    checkedTrackColor = OrangeAccent
                )
            )
        }
    }
}
