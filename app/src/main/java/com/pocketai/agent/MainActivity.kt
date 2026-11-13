package com.pocketai.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.pocketai.agent.data.device.DeviceManager
import com.pocketai.agent.data.git.GitManager
import com.pocketai.agent.data.project.WorkspaceManager
import com.pocketai.agent.data.provider.ProviderRegistry
import com.pocketai.agent.data.security.SecureKeyStorage
import com.pocketai.agent.ui.navigation.AppNavigation
import com.pocketai.agent.ui.theme.PocketAIAgentTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PocketAiApp
        val database = app.database
        val secureStorage = SecureKeyStorage(this)
        val providerRegistry = ProviderRegistry(this)
        val workspaceManager = WorkspaceManager(this, database)
        val gitManager = GitManager(secureStorage)
        val deviceManager = DeviceManager(this)

        setContent {
            PocketAIAgentTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    database = database,
                    providerRegistry = providerRegistry,
                    secureStorage = secureStorage,
                    workspaceManager = workspaceManager,
                    gitManager = gitManager,
                    deviceManager = deviceManager
                )
            }
        }
    }
}
