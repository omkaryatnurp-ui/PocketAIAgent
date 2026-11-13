package com.pocketai.agent.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_providers")
data class AiProviderEntity(
    @PrimaryKey
    val id: String, // GEMINI, CLAUDE, DEEPSEEK, ANTIGRAVITY, FREE_LOCAL
    val displayName: String,
    val isEnabled: Boolean = true,
    val hasKeyConfigured: Boolean = false,
    val selectedModel: String,
    val availableModels: String = "", // Comma-separated list
    val supportsStreaming: Boolean = true,
    val isFreeTier: Boolean = false,
    val customEndpoint: String? = null,
    val requestsCount: Int = 0,
    val tokensUsed: Long = 0L,
    val lastTestedAt: Long? = null,
    val lastTestStatus: String? = null // SUCCESS, FAILED, UNTESTED
)
