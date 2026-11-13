package com.pocketai.agent

import com.pocketai.agent.data.agent.AgentRole
import com.pocketai.agent.data.provider.AiPromptRequest
import com.pocketai.agent.data.provider.FreeModeProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FreeModeProviderTest {

    private lateinit var provider: FreeModeProvider

    @Before
    fun setup() {
        provider = FreeModeProvider()
    }

    @Test
    fun testProviderMetadata() {
        assertEquals("FREE_LOCAL", provider.providerId)
        assertTrue(provider.isFreeTier)
        val models = provider.listSupportedModels()
        assertTrue(models.isNotEmpty())
        assertTrue(models.any { it.id == "local-rule-engine" })
    }

    @Test
    fun testPlannerAgentGeneratesStructuredPlan() = runBlocking {
        val request = AiPromptRequest(
            prompt = "Build a user login screen in Jetpack Compose",
            systemPrompt = AgentRole.PLANNER.systemInstruction,
            modelName = "local-rule-engine"
        )
        val result = provider.sendPrompt(request).getOrThrow()
        assertTrue("Plan should contain numbered steps or milestones", result.contains("Plan") || result.contains("1."))
    }

    @Test
    fun testDeveloperAgentGeneratesCode() = runBlocking {
        val request = AiPromptRequest(
            prompt = "Create a Compose view for notes list",
            systemPrompt = AgentRole.DEVELOPER.systemInstruction,
            modelName = "local-rule-engine"
        )
        val result = provider.sendPrompt(request).getOrThrow()
        assertTrue(result.contains("@Composable") || result.contains("class") || result.contains("fun"))
    }

    @Test
    fun testDebuggerAgentProducesFixes() = runBlocking {
        val request = AiPromptRequest(
            prompt = "NullPointerException when reading file stream",
            systemPrompt = AgentRole.DEBUGGER.systemInstruction,
            modelName = "local-rule-engine"
        )
        val result = provider.sendPrompt(request).getOrThrow()
        assertTrue(result.contains("Fix") || result.contains("try") || result.contains("Analysis"))
    }

    @Test
    fun testStreamingOutputProducesChunks() = runBlocking {
        val request = AiPromptRequest(
            prompt = "Generate starter code",
            modelName = "local-rule-engine"
        )
        val chunks = provider.streamPrompt(request).toList()
        assertTrue(chunks.size > 1)
        assertTrue(chunks.last().isComplete)
    }
}
