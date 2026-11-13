package com.pocketai.agent.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tasks",
    indices = [Index("projectId"), Index("status")]
)
data class TaskRecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val projectId: String? = null,
    val title: String,
    val prompt: String,
    val assignedAgentRole: String = "COORDINATOR", // PLANNER, DEVELOPER, DEBUGGER, TESTER, REVIEWER, COORDINATOR
    val providerId: String = "FREE_LOCAL",
    val modelName: String = "local-rule-engine",
    val status: String = "QUEUED", // QUEUED, STARTING, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
    val priority: Int = 2, // 1 = Low, 2 = Normal, 3 = High
    val progressPercent: Int = 0,
    val currentStage: String = "Initialized",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val resultSummary: String? = null
)
