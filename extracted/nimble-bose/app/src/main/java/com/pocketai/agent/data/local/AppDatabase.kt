package com.pocketai.agent.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketai.agent.data.local.dao.AgentDao
import com.pocketai.agent.data.local.dao.AiProviderDao
import com.pocketai.agent.data.local.dao.ProjectDao
import com.pocketai.agent.data.local.dao.TaskDao
import com.pocketai.agent.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProjectEntity::class,
        ProjectFileEntity::class,
        AiProviderEntity::class,
        TaskRecordEntity::class,
        TaskCheckpointEntity::class,
        TaskLogEntity::class,
        AgentMessageEntity::class,
        ApiUsageEntity::class,
        PreferenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun aiProviderDao(): AiProviderDao
    abstract fun agentDao(): AgentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migration path for future versions
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocket_ai_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Default AI providers are populated here
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultProviders(database.aiProviderDao())
                    }
                }
            }

            private suspend fun populateDefaultProviders(dao: AiProviderDao) {
                val defaultProviders = listOf(
                    AiProviderEntity(
                        id = "FREE_LOCAL",
                        displayName = "Free Mode (Local Rules & Engine)",
                        isEnabled = true,
                        hasKeyConfigured = true,
                        selectedModel = "local-rule-engine",
                        availableModels = "local-rule-engine,regex-refactorer,template-generator",
                        supportsStreaming = true,
                        isFreeTier = true,
                        lastTestStatus = "SUCCESS"
                    ),
                    AiProviderEntity(
                        id = "GEMINI",
                        displayName = "Google Gemini",
                        isEnabled = true,
                        hasKeyConfigured = false,
                        selectedModel = "gemini-1.5-flash",
                        availableModels = "gemini-1.5-flash,gemini-1.5-pro,gemini-2.0-flash",
                        supportsStreaming = true,
                        isFreeTier = true
                    ),
                    AiProviderEntity(
                        id = "CLAUDE",
                        displayName = "Anthropic Claude",
                        isEnabled = true,
                        hasKeyConfigured = false,
                        selectedModel = "claude-3-5-sonnet-20241022",
                        availableModels = "claude-3-5-sonnet-20241022,claude-3-haiku-20240307,claude-3-opus-20240229",
                        supportsStreaming = true,
                        isFreeTier = false
                    ),
                    AiProviderEntity(
                        id = "DEEPSEEK",
                        displayName = "DeepSeek",
                        isEnabled = true,
                        hasKeyConfigured = false,
                        selectedModel = "deepseek-chat",
                        availableModels = "deepseek-chat,deepseek-reasoner",
                        supportsStreaming = true,
                        isFreeTier = false
                    ),
                    AiProviderEntity(
                        id = "ANTIGRAVITY",
                        displayName = "Antigravity Bridge",
                        isEnabled = true,
                        hasKeyConfigured = true,
                        selectedModel = "antigravity-coordinator",
                        availableModels = "antigravity-coordinator,antigravity-handoff",
                        supportsStreaming = false,
                        isFreeTier = true
                    )
                )
                dao.insertDefaultProviders(defaultProviders)
            }
        }
    }
}
