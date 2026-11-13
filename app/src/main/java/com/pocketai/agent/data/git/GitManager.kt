package com.pocketai.agent.data.git

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.pocketai.agent.data.security.LogRedactor
import com.pocketai.agent.data.security.SecureKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GitRepoInfo(
    val name: String,
    val fullName: String,
    val description: String?,
    val defaultBranch: String,
    val isPrivate: Boolean,
    val cloneUrl: String
)

data class GitCommitInfo(
    val hash: String,
    val message: String,
    val author: String,
    val timestamp: Long
)

class GitManager(
    private val secureStorage: SecureKeyStorage
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val localCommits = mutableListOf<GitCommitInfo>()

    init {
        // Seed initial commit
        localCommits.add(
            GitCommitInfo(
                hash = "a1b2c3d",
                message = "Initial project commit from Pocket AI",
                author = "Pocket Developer",
                timestamp = System.currentTimeMillis() - 3600_000
            )
        )
    }

    fun getLocalCommits(): List<GitCommitInfo> = localCommits.toList()

    fun commitChanges(message: String, author: String = "Pocket Developer"): GitCommitInfo {
        val hash = java.util.UUID.randomUUID().toString().substring(0, 7)
        val commit = GitCommitInfo(
            hash = hash,
            message = message.trim(),
            author = author,
            timestamp = System.currentTimeMillis()
        )
        localCommits.add(0, commit)
        return commit
    }

    suspend fun listUserRepositories(): Result<List<GitRepoInfo>> = withContext(Dispatchers.IO) {
        val token = secureStorage.getGitHubToken()
            ?: return@withContext Result.failure(IllegalStateException("GitHub personal access token not configured"))

        val request = Request.Builder()
            .url("https://api.github.com/user/repos?sort=updated&per_page=30")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext Result.failure(IOException(LogRedactor.redact(err)))
                }

                val body = response.body?.string() ?: "[]"
                val jsonArray = JsonParser.parseString(body).asJsonArray
                val list = mutableListOf<GitRepoInfo>()

                for (i in 0 until jsonArray.size()) {
                    val item = jsonArray[i].asJsonObject
                    list.add(
                        GitRepoInfo(
                            name = item.get("name")?.asString ?: "",
                            fullName = item.get("full_name")?.asString ?: "",
                            description = item.get("description")?.let { if (it.isJsonNull) null else it.asString },
                            defaultBranch = item.get("default_branch")?.asString ?: "main",
                            isPrivate = item.get("private")?.asBoolean ?: false,
                            cloneUrl = item.get("clone_url")?.asString ?: ""
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(IOException("Failed to connect to GitHub: ${LogRedactor.redact(e.message)}"))
        }
    }

    suspend fun createPullRequest(
        repoFullName: String,
        title: String,
        head: String,
        base: String,
        body: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val token = secureStorage.getGitHubToken()
            ?: return@withContext Result.failure(IllegalStateException("GitHub token not configured"))

        val json = JsonObject().apply {
            addProperty("title", title)
            addProperty("head", head)
            addProperty("base", base)
            addProperty("body", body)
        }

        val request = Request.Builder()
            .url("https://api.github.com/repos/$repoFullName/pulls")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respJson = JsonParser.parseString(response.body?.string() ?: "{}").asJsonObject
                    val htmlUrl = respJson.get("html_url")?.asString ?: "PR Created"
                    Result.success(htmlUrl)
                } else {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    Result.failure(IOException(LogRedactor.redact(err)))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(LogRedactor.redact(e.message)))
        }
    }

    fun generateDiff(originalText: String, modifiedText: String): String {
        val origLines = originalText.lines()
        val modLines = modifiedText.lines()
        val diffBuilder = StringBuilder()

        diffBuilder.append("--- Original\n+++ Modified\n")
        var i = 0
        var j = 0

        while (i < origLines.size || j < modLines.size) {
            val orig = origLines.getOrNull(i)
            val mod = modLines.getOrNull(j)

            when {
                orig == mod -> {
                    diffBuilder.append("  ").append(orig).append("\n")
                    i++
                    j++
                }
                orig != null && (mod == null || !modLines.contains(orig)) -> {
                    diffBuilder.append("- ").append(orig).append("\n")
                    i++
                }
                mod != null -> {
                    diffBuilder.append("+ ").append(mod).append("\n")
                    j++
                }
            }
        }
        return diffBuilder.toString()
    }
}
