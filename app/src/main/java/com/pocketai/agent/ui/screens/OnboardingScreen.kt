package com.pocketai.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketai.agent.ui.theme.*

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = CharcoalDark,
        bottomBar = {
            Surface(
                color = SurfaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onFinishOnboarding,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                ) {
                    Text(
                        text = "Enter Workstation",
                        style = MaterialTheme.typography.titleMedium,
                        color = CharcoalBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to Pocket AI",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your full-featured mobile AI coding control center.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            FeatureCard(
                icon = Icons.Default.SmartToy,
                title = "Multi-Agent Coding Team",
                description = "Planner, Developer, Debugger, Tester, and Reviewer collaborate to plan, write, and verify your code."
            )

            Spacer(modifier = Modifier.height(14.dp))

            FeatureCard(
                icon = Icons.Default.Bolt,
                title = "100% Free Mode Included",
                description = "Built-in offline rule engine and code generator works without an internet connection or API account."
            )

            Spacer(modifier = Modifier.height(14.dp))

            FeatureCard(
                icon = Icons.Default.Layers,
                title = "Connect Top AI Providers",
                description = "Bring your own API keys for Google Gemini (generous free tier), Anthropic Claude, and DeepSeek."
            )

            Spacer(modifier = Modifier.height(14.dp))

            FeatureCard(
                icon = Icons.Default.BatteryChargingFull,
                title = "Battery-Aware Background Tasks",
                description = "Run coding jobs in the background with ongoing notifications, pause/resume, and checkpoint recovery."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
