package com.pocketai.agent.data.provider

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.pocketai.agent.data.security.LogRedactor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ClaudeProvider : AiProvider {
    override val providerId: String = "CLAUDE"
    override val displayName: String = "Anthropic Claude"
    override val isFreeTier: Boolean = false

    private var currentApiKey: String? = null
    private var customBaseUrl: String? = null
    private var activeCall: Call? = null
    private val isCancelled = AtomicBoolean(false)

    private var requestsCount = 0
    private var totalPromptTokens = 0L
    private var totalCompletionTokens = 0L

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun connect(apiKey: String, customEndpoint: String?): Result<Unit> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Claude API key cannot be blank"))
        }
        currentApiKey = apiKey.trim()
        customBaseUrl = customEndpoint?.trim()?.takeIf { it.isNotBlank() }
        return Result.success(Unit)
    }

    override suspend fun disconnect(): Result<Unit> {
        cancelActiveRequest()
        currentApiKey = null
        return Result.success(Unit)
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        val apiKey = currentApiKey ?: return@withContext Result.failure(IllegalStateException("API key not configured"))
        val baseUrl = customBaseUrl ?: "https://api.anthropic.com/v1"
        val testUrl = "$baseUrl/messages"

        val jsonBody = JsonObject().apply {
            addProperty("model", "claude-3-haiku-20240307")
            addProperty("max_tokens", 5)
            val messages = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", "Ping test")
                })
            }
            add("messages", messages)
        }

        val request = Request.Builder()
            .url(testUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                    val cleanError = LogRedactor.redact(errorBody)
                    Result.failure(IOException("Claude API test failed: $cleanError"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Network error connecting to Claude: ${e.message}"))
        }
    }

    override fun listSupportedModels(): List<AiModelInfo> {
        return listOf(
            AiModelInfo(
                id = "claude-3-5-sonnet-20241022",
                displayName = "Claude 3.5 Sonnet (State-of-the-Art Coding)",
                providerId = providerId,
                contextWindow = 200_000,
                isFreeTier = false,
                supportsReasoning = true,
                supportsVision = true,
                description = "Industry standard for code architecture, debugging, and review"
            ),
            AiModelInfo(
                id = "claude-3-haiku-20240307",
                displayName = "Claude 3 Haiku (Fast & Cost Effective)",
                providerId = providerId,
                contextWindow = 200_000,
                isFreeTier = false,
                supportsReasoning = false,
                supportsVision = true,
                description = "Near-instant responses for quick syntax checks and small edits"
            ),
            AiModelInfo(
                id = "claude-3-opus-20240229",
                displayName = "Claude 3 Opus (Deep Analysis)",
                providerId = providerId,
                contextWindow = 200_000,
                isFreeTier = false,
                supportsReasoning = true,
                supportsVision = true,
                description = "Maximum intelligence for solving hard architectural problems"
            )
        )
    }

    override fun streamPrompt(request: AiPromptRequest): Flow<AiPromptChunk> = callbackFlow {
        isCancelled.set(false)
        val apiKey = currentApiKey
        if (apiKey.isNullOrBlank()) {
            trySend(AiPromptChunk("", isComplete = true, errorMessage = "Claude API key is not configured."))
            close()
            return@callbackFlow
        }

        val baseUrl = customBaseUrl ?: "https://api.anthropic.com/v1"
        val endpoint = "$baseUrl/messages"

        val jsonBody = JsonObject().apply {
            addProperty("model", request.modelName)
            addProperty("max_tokens", request.maxTokens)
            addProperty("stream", true)
            if (!request.systemPrompt.isNullOrBlank()) {
                addProperty("system", request.systemPrompt)
            }
            val messages = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", request.prompt)
                })
            }
            add("messages", messages)
        }

        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(httpRequest)
        activeCall = call

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!isCancelled.get()) {
                    trySend(AiPromptChunk("", isComplete = true, errorMessage = LogRedactor.redact(e.message ?: "Connection failed")))
                }
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        val err = response.body?.string() ?: "HTTP ${response.code}"
                        trySend(AiPromptChunk("", isComplete = true, errorMessage = LogRedactor.redact(err)))
                        close()
                        return
                    }

                    val source = response.body?.byteStream()
                    if (source == null) {
                        trySend(AiPromptChunk("", isComplete = true, errorMessage = "Empty response body"))
                        close()
                        return
                    }

                    val reader = BufferedReader(InputStreamReader(source))
                    var line: String?
                    var promptToks = 0
                    var compToks = 0

                    while (reader.readLine().also { line = it } != null) {
                        if (isCancelled.get()) break
                        val currentLine = line ?: continue
                        if (currentLine.startsWith("data: ")) {
                            val data = currentLine.removePrefix("data: ").trim()
                            if (data.isNotBlank()) {
                                try {
                                    val json = JsonParser.parseString(data).asJsonObject
                                    val type = json.get("type")?.asString
                                    if (type == "content_block_delta") {
                                        val delta = json.getAsJsonObject("delta")
                                        val text = delta?.get("text")?.asString ?: ""
                                        if (text.isNotEmpty()) {
                                            trySend(AiPromptChunk(text = text, isComplete = false))
                                        }
                                    } else if (type == "message_start") {
                                        val msg = json.getAsJsonObject("message")
                                        val usage = msg?.getAsJsonObject("usage")
                                        promptToks = usage?.get("input_tokens")?.asInt ?: promptToks
                                    } else if (type == "message_delta") {
                                        val usage = json.getAsJsonObject("usage")
                                        compToks = usage?.get("output_tokens")?.asInt ?: compToks
                                    }
                                } catch (ignored: Exception) {}
                            }
                        }
                    }

                    requestsCount++
                    totalPromptTokens += promptToks
                    totalCompletionTokens += compToks

                    trySend(
                        AiPromptChunk(
                            text = "",
                            isComplete = true,
                            promptTokens = promptToks,
                            completionTokens = compToks
                        )
                    )
                    close()
                } catch (e: Exception) {
                    if (!isCancelled.get()) {
                        trySend(AiPromptChunk("", isComplete = true, errorMessage = LogRedactor.redact(e.message ?: "Stream error")))
                    }
                    close()
                } finally {
                    response.close()
                }
            }
        })

        awaitClose {
            call.cancel()
            activeCall = null
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun sendPrompt(request: AiPromptRequest): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = currentApiKey ?: return@withContext Result.failure(IllegalStateException("Claude API key is not configured"))
        val baseUrl = customBaseUrl ?: "https://api.anthropic.com/v1"
        val endpoint = "$baseUrl/messages"

        val jsonBody = JsonObject().apply {
            addProperty("model", request.modelName)
            addProperty("max_tokens", request.maxTokens)
            if (!request.systemPrompt.isNullOrBlank()) {
                addProperty("system", request.systemPrompt)
            }
            val messages = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", request.prompt)
                })
            }
            add("messages", messages)
        }

        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext Result.failure(IOException(LogRedactor.redact(errorBody)))
                }

                val responseBody = response.body?.string() ?: ""
                val json = JsonParser.parseString(responseBody).asJsonObject
                val contentArray = json.getAsJsonArray("content")
                if (contentArray != null && contentArray.size() > 0) {
                    val textBuilder = StringBuilder()
                    for (i in 0 until contentArray.size()) {
                        val item = contentArray[i].asJsonObject
                        if (item.get("type")?.asString == "text") {
                            textBuilder.append(item.get("text")?.asString ?: "")
                        }
                    }
                    requestsCount++
                    val usage = json.getAsJsonObject("usage")
                    if (usage != null) {
                        totalPromptTokens += usage.get("input_tokens")?.asLong ?: 0L
                        totalCompletionTokens += usage.get("output_tokens")?.asLong ?: 0L
                    }
                    Result.success(textBuilder.toString())
                } else {
                    Result.failure(IOException("No response generated by Claude"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Failed to communicate with Claude: ${LogRedactor.redact(e.message)}"))
        }
    }

    override fun cancelActiveRequest() {
        isCancelled.set(true)
        activeCall?.cancel()
        activeCall = null
    }

    override fun getUsageReport(): AiUsageReport {
        // Approximate Claude 3.5 Sonnet: $3 / 1M prompt, $15 / 1M output
        val cost = (totalPromptTokens * 0.000003) + (totalCompletionTokens * 0.000015)
        return AiUsageReport(
            providerId = providerId,
            totalRequests = requestsCount,
            totalPromptTokens = totalPromptTokens,
            totalCompletionTokens = totalCompletionTokens,
            estimatedCostUsd = cost
        )
    }
}
