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

class GeminiProvider : AiProvider {
    override val providerId: String = "GEMINI"
    override val displayName: String = "Google Gemini"
    override val isFreeTier: Boolean = true

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

    private val gson = Gson()

    override suspend fun connect(apiKey: String, customEndpoint: String?): Result<Unit> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Gemini API key cannot be blank"))
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
        val baseUrl = customBaseUrl ?: "https://generativelanguage.googleapis.com/v1beta"
        val testUrl = "$baseUrl/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val jsonBody = JsonObject().apply {
            val contents = JsonArray().apply {
                val turn = JsonObject().apply {
                    val parts = JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", "Ping test. Reply with 'OK'.") })
                    }
                    add("parts", parts)
                }
                add(turn)
            }
            add("contents", contents)
            val config = JsonObject().apply {
                addProperty("maxOutputTokens", 5)
            }
            add("generationConfig", config)
        }

        val request = Request.Builder()
            .url(testUrl)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                    val cleanError = LogRedactor.redact(errorBody)
                    Result.failure(IOException("Gemini API test failed: $cleanError"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Network error connecting to Gemini: ${e.message}"))
        }
    }

    override fun listSupportedModels(): List<AiModelInfo> {
        return listOf(
            AiModelInfo(
                id = "gemini-1.5-flash",
                displayName = "Gemini 1.5 Flash (Fast & Free Tier)",
                providerId = providerId,
                contextWindow = 1_000_000,
                isFreeTier = true,
                supportsReasoning = false,
                supportsVision = true,
                description = "High speed, multimodal lightweight model, free tier up to 15 RPM"
            ),
            AiModelInfo(
                id = "gemini-1.5-pro",
                displayName = "Gemini 1.5 Pro (Complex Reasoning)",
                providerId = providerId,
                contextWindow = 2_000_000,
                isFreeTier = true,
                supportsReasoning = true,
                supportsVision = true,
                description = "2M token context, superior coding and architectural planning"
            ),
            AiModelInfo(
                id = "gemini-2.0-flash",
                displayName = "Gemini 2.0 Flash (Next-Gen Speed)",
                providerId = providerId,
                contextWindow = 1_000_000,
                isFreeTier = true,
                supportsReasoning = true,
                supportsVision = true,
                description = "Next-generation multimodal model with ultra-low latency"
            )
        )
    }

    override fun streamPrompt(request: AiPromptRequest): Flow<AiPromptChunk> = callbackFlow {
        isCancelled.set(false)
        val apiKey = currentApiKey
        if (apiKey.isNullOrBlank()) {
            trySend(AiPromptChunk("", isComplete = true, errorMessage = "Gemini API key is not configured."))
            close()
            return@callbackFlow
        }

        val baseUrl = customBaseUrl ?: "https://generativelanguage.googleapis.com/v1beta"
        val endpoint = "$baseUrl/models/${request.modelName}:streamGenerateContent?alt=sse&key=$apiKey"

        val jsonBody = JsonObject().apply {
            if (!request.systemPrompt.isNullOrBlank()) {
                val sysInst = JsonObject().apply {
                    val parts = JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", request.systemPrompt) })
                    }
                    add("parts", parts)
                }
                add("system_instruction", sysInst)
            }
            val contents = JsonArray().apply {
                val turn = JsonObject().apply {
                    val parts = JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", request.prompt) })
                    }
                    add("parts", parts)
                }
                add(turn)
            }
            add("contents", contents)
            val config = JsonObject().apply {
                addProperty("temperature", request.temperature)
                addProperty("maxOutputTokens", request.maxTokens)
            }
            add("generationConfig", config)
        }

        val httpRequest = Request.Builder()
            .url(endpoint)
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
                                    // Parse candidates text
                                    val candidates = json.getAsJsonArray("candidates")
                                    if (candidates != null && candidates.size() > 0) {
                                        val cand = candidates[0].asJsonObject
                                        val content = cand.getAsJsonObject("content")
                                        val parts = content?.getAsJsonArray("parts")
                                        if (parts != null && parts.size() > 0) {
                                            val partText = parts[0].asJsonObject.get("text")?.asString ?: ""
                                            if (partText.isNotEmpty()) {
                                                trySend(AiPromptChunk(text = partText, isComplete = false))
                                            }
                                        }
                                    }
                                    // Usage metadata
                                    json.getAsJsonObject("usageMetadata")?.let { meta ->
                                        promptToks = meta.get("promptTokenCount")?.asInt ?: promptToks
                                        compToks = meta.get("candidatesTokenCount")?.asInt ?: compToks
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
        val apiKey = currentApiKey ?: return@withContext Result.failure(IllegalStateException("Gemini API key is not configured"))
        val baseUrl = customBaseUrl ?: "https://generativelanguage.googleapis.com/v1beta"
        val endpoint = "$baseUrl/models/${request.modelName}:generateContent?key=$apiKey"

        val jsonBody = JsonObject().apply {
            if (!request.systemPrompt.isNullOrBlank()) {
                val sysInst = JsonObject().apply {
                    val parts = JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", request.systemPrompt) })
                    }
                    add("parts", parts)
                }
                add("system_instruction", sysInst)
            }
            val contents = JsonArray().apply {
                val turn = JsonObject().apply {
                    val parts = JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", request.prompt) })
                    }
                    add("parts", parts)
                }
                add(turn)
            }
            add("contents", contents)
            val config = JsonObject().apply {
                addProperty("temperature", request.temperature)
                addProperty("maxOutputTokens", request.maxTokens)
            }
            add("generationConfig", config)
        }

        val httpRequest = Request.Builder()
            .url(endpoint)
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
                val candidates = json.getAsJsonArray("candidates")
                if (candidates != null && candidates.size() > 0) {
                    val cand = candidates[0].asJsonObject
                    val content = cand.getAsJsonObject("content")
                    val parts = content?.getAsJsonArray("parts")
                    val textBuilder = StringBuilder()
                    if (parts != null) {
                        for (i in 0 until parts.size()) {
                            textBuilder.append(parts[i].asJsonObject.get("text")?.asString ?: "")
                        }
                    }
                    requestsCount++
                    Result.success(textBuilder.toString())
                } else {
                    Result.failure(IOException("No response generated by Gemini model"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Failed to communicate with Gemini: ${LogRedactor.redact(e.message)}"))
        }
    }

    override fun cancelActiveRequest() {
        isCancelled.set(true)
        activeCall?.cancel()
        activeCall = null
    }

    override fun getUsageReport(): AiUsageReport {
        // Free tier estimates $0 cost
        return AiUsageReport(
            providerId = providerId,
            totalRequests = requestsCount,
            totalPromptTokens = totalPromptTokens,
            totalCompletionTokens = totalCompletionTokens,
            estimatedCostUsd = 0.0
        )
    }
}
