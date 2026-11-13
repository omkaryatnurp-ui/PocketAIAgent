package com.pocketai.agent.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "agent_messages",
    indices = [Index("taskId"), Index("projectId")]
)
data class AgentMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String? = null,
    val projectId: String? = null,
    val senderRole: String, // USER, COORDINATOR, PLANNER, DEVELOPER, DEBUGGER, TESTER, REVIEWER
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isApprovedByUser: Boolean = false
)
