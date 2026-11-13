package com.pocketai.agent.data.local.dao

import androidx.room.*
import com.pocketai.agent.data.local.entities.AgentMessageEntity
import com.pocketai.agent.data.local.entities.PreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    @Query("SELECT * FROM agent_messages WHERE taskId = :taskId ORDER BY timestamp ASC")
    fun getMessagesForTask(taskId: String): Flow<List<AgentMessageEntity>>

    @Query("SELECT * FROM agent_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getMessagesForProject(projectId: String): Flow<List<AgentMessageEntity>>

    @Query("SELECT * FROM agent_messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAgentMessages(limit: Int = 10): Flow<List<AgentMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AgentMessageEntity)

    @Query("UPDATE agent_messages SET isApprovedByUser = :approved WHERE id = :id")
    suspend fun updateMessageApproval(id: String, approved: Boolean)

    @Query("DELETE FROM agent_messages WHERE taskId = :taskId")
    suspend fun deleteMessagesForTask(taskId: String)

    // App Preferences
    @Query("SELECT value FROM app_preferences WHERE `key` = :key")
    suspend fun getPreference(key: String): String?

    @Query("SELECT value FROM app_preferences WHERE `key` = :key")
    fun observePreference(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(pref: PreferenceEntity)
}
