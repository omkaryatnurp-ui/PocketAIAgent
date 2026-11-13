package com.pocketai.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketai.agent.data.local.entities.AgentMessageEntity
import com.pocketai.agent.data.local.entities.AiProviderEntity
import com.pocketai.agent.data.local.entities.ProjectEntity
import com.pocketai.agent.data.provider.AiPromptRequest
import com.pocketai.agent.data.provider.ProviderRegistry
import com.pocketai.agent.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AiAgentScreen(
    providers: List<AiProviderEntity>,
    projects: List<ProjectEntity>,
    providerRegistry: ProviderRegistry,
    onNavigateToProviders: () -> Unit,
    onLaunchMultiAgentWorkflow: (projectId: String?, prompt: String, providerId: String, modelName: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedProviderId by remember { mutableStateOf(providers.firstOrNull()?.id ?: "FREE_LOCAL") }
    var selectedProjectId by remember { mutableStateOf<String?>(projects.firstOrNull()?.id) }
    var reasoningEffort by remember { mutableStateOf("medium") } // low, medium, high

    var promptInput by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var currentStreamingText by remember { mutableStateOf("") }

    val chatMessages = remember {
        mutableStateListOf(
            AgentMessageEntity(
                senderRole = "COORDINATOR",
                content = "Welcome to Pocket AI Coding Agent. Select your project and ask any coding task, architecture design, or bug fix."
            )
        )
    }

    val listState = rememberLazyListState()

    val currentProvider = providers.find { it.id == selectedProviderId } ?: providers.firstOrNull()

    fun sendMessage() {
        if (promptInput.isBlank() || isStreaming) return
        val userPrompt = promptInput.trim()
        promptInput = ""

        chatMessages.add(AgentMessageEntity(senderRole = "USER", content = userPrompt))

        coroutineScope.launch {
            isStreaming = true
            currentStreamingText = ""
            listState.animateScrollToItem(chatMessages.size)

            val provider = providerRegistry.getActiveConfiguredProvider(selectedProviderId)
            val model = currentProvider?.selectedModel ?: "local-rule-engine"

            try {
                provider.streamPrompt(
                    AiPromptRequest(
                        prompt = userPrompt,
                        systemPrompt = "You are a professional software engineer assisting the developer on an Android mobile workstation.",
                        modelName = model,
                        reasoningEffort = reasoningEffort
                    )
                ).collectLatest { chunk ->
                    if (chunk.errorMessage != null) {
                        chatMessages.add(
                            AgentMessageEntity(
                                senderRole = "COORDINATOR",
                                content = "Error: ${chunk.errorMessage}"
                            )
                        )
                    } else if (chunk.isComplete) {
                        if (currentStreamingText.isNotEmpty()) {
                            chatMessages.add(
                                AgentMessageEntity(
                                    senderRole = "DEVELOPER",
                                    content = currentStreamingText
                                )
                            )
                        }
                        currentStreamingText = ""
                    } else {
                        currentStreamingText += chunk.text
                    }
                }
            } catch (e: Exception) {
                chatMessages.add(
                    AgentMessageEntity(
                        senderRole = "COORDINATOR",
                        content = "Failed to communicate with provider: ${e.message}"
                    )
                )
            } finally {
                isStreaming = false
                listState.animateScrollToItem(chatMessages.size)
            }
        }
    }

    Scaffold(
        containerColor = CharcoalDark,
        topBar = {
            Surface(color = SurfaceDark, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Interactive AI Agent",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Model: ${currentProvider?.selectedModel ?: "local-rule-engine"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OrangeAccent
                            )
                        }

                        Button(
                            onClick = onNavigateToProviders,
                            colors = ButtonDefaults.buttonColors(containerColor = CardDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Change Engine", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Target project selector chip row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Project:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        if (projects.isEmpty()) {
                            Text("None (Standalone Mode)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        } else {
                            projects.take(3).forEach { p ->
                                FilterChip(
                                    selected = selectedProjectId == p.id,
                                    onClick = { selectedProjectId = p.id },
                                    label = { Text(p.name, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OrangeAccent,
                                        selectedLabelColor = CharcoalBlack,
                                        containerColor = CardDark,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = SurfaceDark, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            placeholder = { Text("Ask coding agent or feature request...", color = TextMuted) },
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeAccent,
                                unfocusedBorderColor = BorderDark,
                                focusedContainerColor = CardDark,
                                unfocusedContainerColor = CardDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { sendMessage() },
                            enabled = promptInput.isNotBlank() && !isStreaming,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = OrangeAccent)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = CharcoalBlack)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (promptInput.isNotBlank()) {
                                    val prompt = promptInput
                                    promptInput = ""
                                    onLaunchMultiAgentWorkflow(
                                        selectedProjectId,
                                        prompt,
                                        selectedProviderId,
                                        currentProvider?.selectedModel ?: "local-rule-engine"
                                    )
                                }
                            },
                            enabled = promptInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Launch Team Workflow", color = OrangeAccent, fontSize = 12.sp)
                        }

                        if (isStreaming) {
                            TextButton(onClick = {
                                val provider = providerRegistry.getProvider(selectedProviderId)
                                provider.cancelActiveRequest()
                                isStreaming = false
                            }) {
                                Text("Stop Generating", color = StatusRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
        ) {
            items(chatMessages) { message ->
                ChatMessageBubble(message = message)
            }

            if (isStreaming && currentStreamingText.isNotEmpty()) {
                item {
                    ChatMessageBubble(
                        message = AgentMessageEntity(
                            senderRole = "DEVELOPER",
                            content = currentStreamingText
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: AgentMessageEntity) {
    val isUser = message.senderRole == "USER"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = OrangeGlow,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = message.senderRole.take(1),
                        color = OrangeAccent,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) OrangeAccentDark else CardDark
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Text(
                        text = message.senderRole,
                        style = MaterialTheme.typography.labelSmall,
                        color = OrangeAccentLight,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}
