package com.pocketai.agent.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "task_checkpoints",
    foreignKeys = [
        ForeignKey(
            entity = TaskRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class TaskCheckpointEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val checkpointIndex: Int,
    val stageName: String,
    val statePayload: String, // Serialized JSON of progress snapshot
    val timestamp: Long = System.currentTimeMillis()
)
