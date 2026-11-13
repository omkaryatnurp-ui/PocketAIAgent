package com.pocketai.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketai.agent.data.provider.AiModelInfo
import com.pocketai.agent.data.provider.ProviderRegistry
import com.pocketai.agent.ui.theme.*

@Composable
fun ModelSelectorScreen(
    providerRegistry: ProviderRegistry,
    onNavigateBack: () -> Unit,
    onSelectModel: (providerId: String, modelId: String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val allModels = remember {
        providerRegistry.getAllProviders().flatMap { it.listSupportedModels() }
    }

    val filteredModels = allModels.filter { model ->
        when (selectedFilter) {
            "FREE" -> model.isFreeTier
            "REASONING" -> model.supportsReasoning
            "VISION" -> model.supportsVision
            else -> true
        }
    }

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
                            text = "Model Selector",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Choose specialized coding & reasoning engines",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "FREE", "REASONING", "VISION").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeAccent,
                            selectedLabelColor = CharcoalBlack,
                            containerColor = CardDark,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filteredModels) { model ->
                    ModelCard(
                        model = model,
                        onClick = {
                            onSelectModel(model.providerId, model.id)
                            onNavigateBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: AiModelInfo,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceDark
                ) {
                    Text(
                        text = if (model.isFreeTier) "FREE TIER" else "PAID API",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (model.isFreeTier) StatusGreen else OrangeAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "ID: ${model.id} • Context: ${model.contextWindow / 1000}k tokens",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            if (model.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (model.supportsReasoning) {
                    CapabilityBadge("Deep Reasoning")
                }
                if (model.supportsVision) {
                    CapabilityBadge("Multimodal / Vision")
                }
            }
        }
    }
}

@Composable
private fun CapabilityBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = SurfaceDark
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = OrangeAccentLight,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
