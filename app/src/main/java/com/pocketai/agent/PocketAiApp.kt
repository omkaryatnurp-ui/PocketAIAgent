package com.pocketai.agent

import android.app.Application
import com.pocketai.agent.data.local.AppDatabase
import com.pocketai.agent.data.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PocketAiApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)

        // Initialize notification channels
        NotificationHelper(this)

        // Pre-warm database
        applicationScope.launch {
            database.aiProviderDao().getAllProviders()
        }
    }
}
