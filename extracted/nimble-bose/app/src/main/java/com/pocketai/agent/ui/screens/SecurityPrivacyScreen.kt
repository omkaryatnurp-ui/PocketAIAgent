package com.pocketai.agent.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketai.agent.data.security.LogRedactor
import com.pocketai.agent.data.security.SecureKeyStorage
import com.pocketai.agent.ui.theme.*

@Composable
fun SecurityPrivacyScreen(
    secureStorage: SecureKeyStorage,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    var testInput by remember { mutableStateOf("Testing key: AIzaSyD3fakeGeminiApiKey1234567890abc") }
    val redactedOutput = remember(testInput) { LogRedactor.redact(testInput) }

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
                            text = "Security & Privacy",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Hardware Keystore, log redaction & zero telemetry",
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
            // Security Checklist Cards
            item {
                SecurityAuditCard(
                    icon = Icons.Default.Lock,
                    title = "Android Keystore-Backed Encryption",
                    description = "All API keys and GitHub tokens are encrypted with AES-256 GCM using master keys stored securely in the device hardware Keystore.",
                    isPassed = true
                )
            }

            item {
                SecurityAuditCard(
                    icon = Icons.Default.VisibilityOff,
                    title = "Automated Log Redaction Engine",
                    description = "Every terminal log, task log, and debug record is sanitized with regex patterns before storage to prevent credential leakage.",
                    isPassed = true
                )
            }

            item {
                SecurityAuditCard(
                    icon = Icons.Default.Security,
                    title = "Local Execution Sandboxing",
                    description = "Terminal commands cannot escape the designated project workspace folder. Destructive operations require explicit user confirmation.",
                    isPassed = true
                )
            }

            item {
                SecurityAuditCard(
                    icon = Icons.Default.CloudOff,
                    title = "Zero Telemetry / Offline First",
                    description = "Pocket AI Agent collects zero telemetry, zero analytics, and zero crash reports. Code is transmitted only to the AI provider you explicitly choose.",
                    isPassed = true
                )
            }

            // Live Log Redaction Demonstration
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Live Log Redaction Tester",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Type any key string below to test real-time masking:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = testInput,
                            onValueChange = { testInput = it },
                            label = { Text("Raw Log Text") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeAccent,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Redacted Output in Database:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CodeBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = redactedOutput,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = StatusGreen,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Emergency Wipe Button
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Emergency Key Wipe",
                            style = MaterialTheme.typography.titleMedium,
                            color = StatusRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Instantly clears all saved API keys and GitHub credentials from EncryptedSharedPreferences.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showWipeConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Wipe All Credentials", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            title = { Text("Confirm Credential Wipe", color = StatusRed) },
            text = { Text("Are you sure you want to delete all stored API keys and tokens?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        secureStorage.clearAllKeys()
                        showWipeConfirmDialog = false
                        Toast.makeText(context, "All credentials wiped successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Wipe Now", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }
}

@Composable
private fun SecurityAuditCard(
    icon: ImageVector,
    title: String,
    description: String,
    isPassed: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Surface(shape = RoundedCornerShape(4.dp), color = SurfaceDark) {
                        Text(
                            text = if (isPassed) "ACTIVE" else "WARN",
                            fontSize = 10.sp,
                            color = if (isPassed) StatusGreen else StatusYellow,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}
