package com.pocketai.agent.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "api_usage",
    indices = [Index("providerId")]
)
data class ApiUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val providerId: String,
    val modelName: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val estimatedCostUsd: Double,
    val timestamp: Long = System.currentTimeMillis()
)
