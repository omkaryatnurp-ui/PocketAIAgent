package com.pocketai.agent.data.local.dao

import androidx.room.*
import com.pocketai.agent.data.local.entities.AiProviderEntity
import com.pocketai.agent.data.local.entities.ApiUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiProviderDao {
    @Query("SELECT * FROM ai_providers ORDER BY isEnabled DESC, id ASC")
    fun getAllProviders(): Flow<List<AiProviderEntity>>

    @Query("SELECT * FROM ai_providers WHERE isEnabled = 1")
    fun getEnabledProviders(): Flow<List<AiProviderEntity>>

    @Query("SELECT * FROM ai_providers WHERE id = :id")
    suspend fun getProviderById(id: String): AiProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: AiProviderEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultProviders(providers: List<AiProviderEntity>)

    @Update
    suspend fun updateProvider(provider: AiProviderEntity)

    @Query("UPDATE ai_providers SET hasKeyConfigured = :hasKey WHERE id = :id")
    suspend fun updateKeyConfigured(id: String, hasKey: Boolean)

    @Query("UPDATE ai_providers SET selectedModel = :model WHERE id = :id")
    suspend fun updateSelectedModel(id: String, model: String)

    @Query("UPDATE ai_providers SET lastTestedAt = :timestamp, lastTestStatus = :status WHERE id = :id")
    suspend fun updateTestStatus(id: String, timestamp: Long, status: String)

    // API Usage
    @Query("SELECT * FROM api_usage ORDER BY timestamp DESC")
    fun getAllUsage(): Flow<List<ApiUsageEntity>>

    @Insert
    suspend fun insertUsage(usage: ApiUsageEntity)

    @Query("SELECT SUM(estimatedCostUsd) FROM api_usage")
    fun getTotalEstimatedCost(): Flow<Double?>

    @Query("SELECT SUM(promptTokens + completionTokens) FROM api_usage")
    fun getTotalTokens(): Flow<Long?>

    @Query("DELETE FROM api_usage")
    suspend fun clearUsageHistory()
}
