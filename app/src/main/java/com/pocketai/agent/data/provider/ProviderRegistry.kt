package com.pocketai.agent.data.provider

import android.content.Context
import com.pocketai.agent.data.security.SecureKeyStorage

class ProviderRegistry(context: Context) {
    private val secureStorage = SecureKeyStorage(context)

    private val providers = mapOf<String, AiProvider>(
        "FREE_LOCAL" to FreeModeProvider(),
        "GEMINI" to GeminiProvider(),
        "CLAUDE" to ClaudeProvider(),
        "DEEPSEEK" to DeepSeekProvider(),
        "ANTIGRAVITY" to AntigravityProvider()
    )

    fun getProvider(providerId: String): AiProvider {
        val found = providers[providerId.uppercase()]
        return found ?: providers["FREE_LOCAL"]!!
    }

    fun getAllProviders(): List<AiProvider> = providers.values.toList()

    suspend fun getActiveConfiguredProvider(preferredId: String?): AiProvider {
        if (!preferredId.isNullOrBlank()) {
            val pref = getProvider(preferredId)
            if (pref.isFreeTier || secureStorage.hasApiKey(pref.providerId)) {
                // Initialize key if needed
                val key = secureStorage.getApiKey(pref.providerId)
                if (key != null) pref.connect(key)
                return pref
            }
        }

        // Check Gemini if configured
        if (secureStorage.hasApiKey("GEMINI")) {
            val gemini = getProvider("GEMINI")
            gemini.connect(secureStorage.getApiKey("GEMINI")!!)
            return gemini
        }

        // Fallback to Free Mode rule engine
        return getProvider("FREE_LOCAL")
    }

    suspend fun testProvider(providerId: String): Result<Boolean> {
        val p = getProvider(providerId)
        val key = secureStorage.getApiKey(providerId)
        if (!p.isFreeTier && key.isNullOrBlank()) {
            return Result.failure(IllegalStateException("API key not configured for ${p.displayName}"))
        }
        if (key != null) {
            p.connect(key)
        }
        return p.testConnection()
    }
}
