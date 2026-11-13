package com.pocketai.agent.data.provider

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

class DeepSeekProvider : AiProvider {
    override val providerId: String = "DEEPSEEK"
    override val displayName: String = "DeepSeek"
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
            return Result.failure(IllegalArgumentException("DeepSeek API key cannot be blank"))
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
        val baseUrl = customBaseUrl ?: "https://api.deepseek.com"
        val testUrl = "$baseUrl/chat/completions"

        val jsonBody = JsonObject().apply {
            addProperty("model", "deepseek-chat")
            addProperty("max_tokens", 5)
            val messages = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", "ping")
                })
            }
            add("messages", messages)
        }

        val request = Request.Builder()
            .url(testUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                    val cleanError = LogRedactor.redact(errorBody)
                    Result.failure(IOException("DeepSeek API test failed: $cleanError"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Network error connecting to DeepSeek: ${e.message}"))
        }
    }

    override fun listSupportedModels(): List<AiModelInfo> {
        return listOf(
            AiModelInfo(
                id = "deepseek-chat",
                displayName = "DeepSeek-V3 (Chat & Code)",
                providerId = providerId,
                contextWindow = 64_000,
                isFreeTier = false,
                supportsReasoning = false,
                supportsVision = false,
                description = "Economical, high capability coding and language model"
            ),
            AiModelInfo(
                id = "deepseek-reasoner",
                displayName = "DeepSeek-R1 (Deep Reasoning)",
                providerId = providerId,
                contextWindow = 64_000,
                isFreeTier = false,
                supportsReasoning = true,
                supportsVision = false,
                description = "Advanced reasoning with step-by-step chain of thought"
            )
        )
    }

    override fun streamPrompt(request: AiPromptRequest): Flow<AiPromptChunk> = callbackFlow {
        isCancelled.set(false)
        val apiKey = currentApiKey
        if (apiKey.isNullOrBlank()) {
            trySend(AiPromptChunk("", isComplete = true, errorMessage = "DeepSeek API key is not configured."))
            close()
            return@callbackFlow
        }

        val baseUrl = customBaseUrl ?: "https://api.deepseek.com"
        val endpoint = "$baseUrl/chat/completions"

        val jsonBody = JsonObject().apply {
            addProperty("model", request.modelName)
            addProperty("max_tokens", request.maxTokens)
            addProperty("temperature", request.temperature)
            addProperty("stream", true)
            val messages = JsonArray().apply {
                if (!request.systemPrompt.isNullOrBlank()) {
                    add(JsonObject().apply {
                        addProperty("role", "system")
                        addProperty("content", request.systemPrompt)
                    })
                }
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", request.prompt)
                })
            }
            add("messages", messages)
        }

        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
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
                            if (data == "[DONE]") break
                            if (data.isNotBlank()) {
                                try {
                                    val json = JsonParser.parseString(data).asJsonObject
                                    val choices = json.getAsJsonArray("choices")
                                    if (choices != null && choices.size() > 0) {
                                        val delta = choices[0].asJsonObject.getAsJsonObject("delta")
                                        val content = delta?.get("content")?.asString ?: ""
                                        if (content.isNotEmpty()) {
                                            trySend(AiPromptChunk(text = content, isComplete = false))
                                        }
                                    }
                                    json.getAsJsonObject("usage")?.let { usage ->
                                        promptToks = usage.get("prompt_tokens")?.asInt ?: promptToks
                                        compToks = usage.get("completion_tokens")?.asInt ?: compToks
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
        val apiKey = currentApiKey ?: return@withContext Result.failure(IllegalStateException("DeepSeek API key is not configured"))
        val baseUrl = customBaseUrl ?: "https://api.deepseek.com"
        val endpoint = "$baseUrl/chat/completions"

        val jsonBody = JsonObject().apply {
            addProperty("model", request.modelName)
            addProperty("max_tokens", request.maxTokens)
            addProperty("temperature", request.temperature)
            val messages = JsonArray().apply {
                if (!request.systemPrompt.isNullOrBlank()) {
                    add(JsonObject().apply {
                        addProperty("role", "system")
                        addProperty("content", request.systemPrompt)
                    })
                }
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", request.prompt)
                })
            }
            add("messages", messages)
        }

        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
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
                val choices = json.getAsJsonArray("choices")
                if (choices != null && choices.size() > 0) {
                    val message = choices[0].asJsonObject.getAsJsonObject("message")
                    val content = message?.get("content")?.asString ?: ""
                    requestsCount++
                    json.getAsJsonObject("usage")?.let { usage ->
                        totalPromptTokens += usage.get("prompt_tokens")?.asLong ?: 0L
                        totalCompletionTokens += usage.get("completion_tokens")?.asLong ?: 0L
                    }
                    Result.success(content)
                } else {
                    Result.failure(IOException("No response generated by DeepSeek"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Failed to communicate with DeepSeek: ${LogRedactor.redact(e.message)}"))
        }
    }

    override fun cancelActiveRequest() {
        isCancelled.set(true)
        activeCall?.cancel()
        activeCall = null
    }

    override fun getUsageReport(): AiUsageReport {
        // DeepSeek V3: ~$0.14 / 1M prompt, $0.28 / 1M completion
        val cost = (totalPromptTokens * 0.00000014) + (totalCompletionTokens * 0.00000028)
        return AiUsageReport(
            providerId = providerId,
            totalRequests = requestsCount,
            totalPromptTokens = totalPromptTokens,
            totalCompletionTokens = totalCompletionTokens,
            estimatedCostUsd = cost
        )
    }
}
