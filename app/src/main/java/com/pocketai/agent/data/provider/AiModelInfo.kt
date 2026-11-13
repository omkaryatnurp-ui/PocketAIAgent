package com.pocketai.agent.data.provider

data class AiModelInfo(
    val id: String,
    val displayName: String,
    val providerId: String,
    val contextWindow: Int,
    val isFreeTier: Boolean,
    val supportsReasoning: Boolean = false,
    val supportsVision: Boolean = false,
    val description: String = ""
)

data class AiPromptRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val modelName: String,
    val temperature: Float = 0.2f,
    val maxTokens: Int = 4096,
    val reasoningEffort: String? = null // low, medium, high
)

data class AiPromptChunk(
    val text: String,
    val isComplete: Boolean = false,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val finishReason: String? = null,
    val errorMessage: String? = null
)

data class AiUsageReport(
    val providerId: String,
    val totalRequests: Int,
    val totalPromptTokens: Long,
    val totalCompletionTokens: Long,
    val estimatedCostUsd: Double
)
