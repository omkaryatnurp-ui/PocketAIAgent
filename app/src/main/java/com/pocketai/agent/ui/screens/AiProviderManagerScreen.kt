package com.pocketai.agent.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketai.agent.data.local.entities.AiProviderEntity
import com.pocketai.agent.data.provider.ProviderRegistry
import com.pocketai.agent.data.security.SecureKeyStorage
import com.pocketai.agent.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiProviderManagerScreen(
    providers: List<AiProviderEntity>,
    providerRegistry: ProviderRegistry,
    secureStorage: SecureKeyStorage,
    onNavigateBack: () -> Unit,
    onSaveApiKey: (providerId: String, apiKey: String) -> Unit,
    onRemoveApiKey: (providerId: String) -> Unit,
    onSelectModel: (providerId: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var editingProvider by remember { mutableStateOf<AiProviderEntity?>(null) }
    var testingProviderId by remember { mutableStateOf<String?>(null) }
    var testResultMap by remember { mutableStateOf<Map<String, Pair<Boolean, String>>>(emptyMap()) }

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
                            text = "AI Providers",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Manage credentials, models, and quotas",
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
            // Free Tier & Quotas Notice Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Free AI Options & Transparency",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Free Mode (Local): Works offline with zero credentials.\n• Google Gemini: Free tier API keys provide up to 15 RPM without cost.\n• Anthropic & DeepSeek: Require API credits or user accounts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            items(providers) { provider ->
                val hasKey = secureStorage.hasApiKey(provider.id) || provider.isFreeTier
                val testResult = testResultMap[provider.id]

                ProviderCard(
                    provider = provider,
                    hasKey = hasKey,
                    isTesting = testingProviderId == provider.id,
                    testResult = testResult,
                    onConfigureKey = { editingProvider = provider },
                    onTestConnection = {
                        coroutineScope.launch {
                            testingProviderId = provider.id
                            val res = providerRegistry.testProvider(provider.id)
                            testingProviderId = null
                            if (res.isSuccess) {
                                testResultMap = testResultMap + (provider.id to Pair(true, "Connection Verified!"))
                            } else {
                                testResultMap = testResultMap + (provider.id to Pair(false, res.exceptionOrNull()?.message ?: "Failed"))
                            }
                        }
                    },
                    onSelectModel = { onSelectModel(provider.id) }
                )
            }
        }
    }

    // Edit API Key Dialog
    editingProvider?.let { prov ->
        var inputKey by remember { mutableStateOf(secureStorage.getApiKey(prov.id) ?: "") }

        AlertDialog(
            onDismissRequest = { editingProvider = null },
            title = { Text("Configure ${prov.displayName}", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "API Key is stored exclusively in your device's hardware-backed EncryptedSharedPreferences and is never exposed in logs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("Enter private key...") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAccent,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveApiKey(prov.id, inputKey)
                        editingProvider = null
                        Toast.makeText(context, "${prov.displayName} key saved securely", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) {
                    Text("Save Key", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (secureStorage.hasApiKey(prov.id)) {
                    TextButton(onClick = {
                        onRemoveApiKey(prov.id)
                        editingProvider = null
                        Toast.makeText(context, "Key removed", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Remove Key", color = StatusRed)
                    }
                } else {
                    TextButton(onClick = { editingProvider = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            },
            containerColor = CardDark
        )
    }
}

@Composable
private fun ProviderCard(
    provider: AiProviderEntity,
    hasKey: Boolean,
    isTesting: Boolean,
    testResult: Pair<Boolean, String>?,
    onConfigureKey: () -> Unit,
    onTestConnection: () -> Unit,
    onSelectModel: () -> Unit
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (hasKey) StatusGreen else StatusYellow)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = provider.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceDark
                ) {
                    Text(
                        text = if (provider.isFreeTier) "FREE TIER" else "PAID API",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (provider.isFreeTier) StatusGreen else OrangeAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Model: ${provider.selectedModel}",
                style = MaterialTheme.typography.bodyMedium,
                color = OrangeAccentLight
            )

            if (testResult != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = testResult.second,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (testResult.first) StatusGreen else StatusRed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!provider.isFreeTier) {
                    Button(
                        onClick = onConfigureKey,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasKey) SurfaceDark else OrangeAccent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (hasKey) "Update Key" else "Set Key",
                            color = if (hasKey) TextPrimary else CharcoalBlack,
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = !isTesting && hasKey,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = OrangeAccent)
                    } else {
                        Text("Test API", fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}
