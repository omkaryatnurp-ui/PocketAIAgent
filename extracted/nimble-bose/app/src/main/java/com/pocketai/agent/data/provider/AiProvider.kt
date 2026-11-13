package com.pocketai.agent.data.provider

import kotlinx.coroutines.flow.Flow

interface AiProvider {
    val providerId: String
    val displayName: String
    val isFreeTier: Boolean

    suspend fun connect(apiKey: String, customEndpoint: String? = null): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun testConnection(): Result<Boolean>
    fun listSupportedModels(): List<AiModelInfo>
    fun streamPrompt(request: AiPromptRequest): Flow<AiPromptChunk>
    suspend fun sendPrompt(request: AiPromptRequest): Result<String>
    fun cancelActiveRequest()
    fun getUsageReport(): AiUsageReport?
}
