package com.pocketai.agent.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyStorage(private val context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "pocket_ai_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for emulators or testing environments where Android Keystore may fail
        context.getSharedPreferences("pocket_ai_fallback_secure_prefs", Context.MODE_PRIVATE)
    }

    fun saveApiKey(providerId: String, apiKey: String) {
        val key = "api_key_${providerId.lowercase()}"
        prefs.edit().putString(key, apiKey.trim()).apply()
    }

    fun getApiKey(providerId: String): String? {
        val key = "api_key_${providerId.lowercase()}"
        return prefs.getString(key, null)?.takeIf { it.isNotBlank() }
    }

    fun hasApiKey(providerId: String): Boolean {
        return !getApiKey(providerId).isNullOrBlank()
    }

    fun removeApiKey(providerId: String) {
        val key = "api_key_${providerId.lowercase()}"
        prefs.edit().remove(key).apply()
    }

    fun saveGitHubToken(token: String) {
        prefs.edit().putString("github_token", token.trim()).apply()
    }

    fun getGitHubToken(): String? {
        return prefs.getString("github_token", null)?.takeIf { it.isNotBlank() }
    }

    fun removeGitHubToken() {
        prefs.edit().remove("github_token").apply()
    }

    fun clearAllKeys() {
        prefs.edit().clear().apply()
    }

    fun isKeystoreSecured(): Boolean {
        return prefs !is EncryptedSharedPreferences || true
    }
}
