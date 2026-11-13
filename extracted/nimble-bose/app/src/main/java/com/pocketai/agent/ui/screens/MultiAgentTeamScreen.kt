package com.pocketai.agent.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketai.agent.data.agent.AgentRole
import com.pocketai.agent.data.local.entities.AgentMessageEntity
import com.pocketai.agent.data.local.entities.TaskRecordEntity
import com.pocketai.agent.ui.theme.*

@Composable
fun MultiAgentTeamScreen(
    task: TaskRecordEntity?,
    messages: List<AgentMessageEntity>,
    onNavigateBack: () -> Unit,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
    onApprovePlan: (String) -> Unit,
    onNavigateToProjectEditor: (projectId: String) -> Unit
) {
    if (task == null) {
        Box(modifier = Modifier.fillMaxSize().background(CharcoalDark), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OrangeAccent)
        }
        return
    }

    val isPaused = task.status == "PAUSED"
    val isRunning = task.status == "RUNNING" || task.status == "STARTING"
    val isCompleted = task.status == "COMPLETED"

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
                            text = "Multi-Agent Team",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = OrangeAccent,
                            maxLines = 1
                        )
                    }

                    if (isRunning) {
                        IconButton(onClick = { onPauseTask(task.id) }) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = StatusYellow)
                        }
                    } else if (isPaused) {
                        IconButton(onClick = { onResumeTask(task.id) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = StatusGreen)
                        }
                    }

                    if (isRunning || isPaused) {
                        IconButton(onClick = { onCancelTask(task.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = StatusRed)
                        }
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
            // Task Status & Progress Bar Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pipeline Status: ${task.status}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = when (task.status) {
                                    "COMPLETED" -> StatusGreen
                                    "FAILED" -> StatusRed
                                    "PAUSED" -> StatusYellow
                                    else -> OrangeAccent
                                }
                            )
                            Text(
                                text = "${task.progressPercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { task.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = OrangeAccent,
                            trackColor = SurfaceDark
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Stage: ${task.currentStage}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Agents Pipeline Stepper Badges Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("PLANNER", "DEVELOPER", "DEBUGGER", "TESTER", "REVIEWER").forEach { roleName ->
                        val hasDone = messages.any { it.senderRole == roleName }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = if (hasDone) OrangeAccent else SurfaceDark,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = roleName.take(1),
                                        color = if (hasDone) CharcoalBlack else TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = roleName.take(4),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasDone) TextPrimary else TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Text(
                    text = "Agent Deliverables & Diffs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            items(messages) { message ->
                AgentDeliverableCard(
                    message = message,
                    onApprove = { onApprovePlan(message.id) }
                )
            }

            // Post-Completion Review Actions
            if (isCompleted && task.projectId != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Task Finalized",
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "All multi-agent stages passed. You can now open the project in the Code Editor to inspect or edit the generated code.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onNavigateToProjectEditor(task.projectId) },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Code in Editor", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentDeliverableCard(
    message: AgentMessageEntity,
    onApprove: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = OrangeGlow,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = message.senderRole.take(1),
                                color = OrangeAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${message.senderRole} Output",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = TextMuted
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CodeBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                if (message.senderRole == "PLANNER" && !message.isApprovedByUser) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Approve Implementation Plan", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
