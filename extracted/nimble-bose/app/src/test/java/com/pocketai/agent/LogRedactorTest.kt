package com.pocketai.agent

import com.pocketai.agent.data.security.LogRedactor
import org.junit.Assert.*
import org.junit.Test

class LogRedactorTest {

    @Test
    fun testGeminiApiKeyRedaction() {
        val raw = "Connecting with key AIzaSyD3fakeGeminiApiKey1234567890abc to Gemini"
        val redacted = LogRedactor.redact(raw)
        assertFalse(redacted.contains("AIzaSyD3fakeGeminiApiKey1234567890abc"))
        assertTrue(redacted.contains("[REDACTED_GEMINI_KEY]"))
    }

    @Test
    fun testClaudeApiKeyRedaction() {
        val raw = "Anthropic request failed: sk-ant-api03-abcdef1234567890abcdef123456"
        val redacted = LogRedactor.redact(raw)
        assertFalse(redacted.contains("sk-ant-api03-abcdef1234567890abcdef123456"))
        assertTrue(redacted.contains("[REDACTED_CLAUDE_KEY]"))
    }

    @Test
    fun testGitHubTokenRedaction() {
        val raw = "Pushing to remote with token ghp_1234567890abcdefghijklmnopqrstuv"
        val redacted = LogRedactor.redact(raw)
        assertFalse(redacted.contains("ghp_1234567890abcdefghijklmnopqrstuv"))
        assertTrue(redacted.contains("[REDACTED_GITHUB_TOKEN]"))
    }

    @Test
    fun testBearerAuthHeaderRedaction() {
        val raw = "Headers: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz"
        val redacted = LogRedactor.redact(raw)
        assertFalse(redacted.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz"))
        assertTrue(redacted.contains("[REDACTED_BEARER_TOKEN]"))
    }

    @Test
    fun testNormalTextPreserved() {
        val raw = "Compiled Main.kt successfully with 0 errors"
        val redacted = LogRedactor.redact(raw)
        assertEquals(raw, redacted)
    }
}
