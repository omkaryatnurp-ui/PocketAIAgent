package com.pocketai.agent.data.security

import java.util.regex.Pattern

object LogRedactor {
    // Patterns for matching API keys, tokens, Bearer auth, etc.
    private val PATTERNS = listOf(
        // Gemini / Google AI Studio API key (AIza followed by 30+ chars)
        Pattern.compile("AIza[-0-9A-Za-z_]{30,45}"),
        // Anthropic Claude API key
        Pattern.compile("sk-ant-api[-0-9A-Za-z_]{20,}"),
        // OpenAI / DeepSeek API key
        Pattern.compile("sk-[0-9A-Za-z]{20,}"),
        // GitHub Personal Access Token (classic & fine-grained)
        Pattern.compile("ghp_[0-9A-Za-z]{30,40}"),
        Pattern.compile("github_pat_[-0-9A-Za-z_]{50,}"),
        // Bearer Authorization Header
        Pattern.compile("(?i)(bearer\\s+)[a-zA-Z0-9_\\-\\.~+/]+=*"),
        // Key parameter in URLs or JSON
        Pattern.compile("(?i)(\"?(?:api[-_]?key|access[-_]?token|password|secret)\"?\\s*[:=]\\s*\"?)([^\"&\\s]+)(\"?)")
    )

    /**
     * Redacts any API keys, credentials, or tokens found within the text.
     */
    fun redact(input: String?): String {
        if (input == null) return ""
        var redacted = input

        // Mask basic patterns
        redacted = PATTERNS[0].matcher(redacted).replaceAll("[REDACTED_GEMINI_KEY]")
        redacted = PATTERNS[1].matcher(redacted).replaceAll("[REDACTED_CLAUDE_KEY]")
        redacted = PATTERNS[2].matcher(redacted).replaceAll("[REDACTED_API_KEY]")
        redacted = PATTERNS[3].matcher(redacted).replaceAll("[REDACTED_GITHUB_TOKEN]")
        redacted = PATTERNS[4].matcher(redacted).replaceAll("[REDACTED_GITHUB_PAT]")
        redacted = PATTERNS[5].matcher(redacted).replaceAll("$1[REDACTED_BEARER_TOKEN]")
        redacted = PATTERNS[6].matcher(redacted).replaceAll("$1[REDACTED]$3")

        return redacted
    }
}
