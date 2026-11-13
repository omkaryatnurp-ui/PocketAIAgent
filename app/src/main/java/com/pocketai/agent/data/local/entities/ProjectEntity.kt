package com.pocketai.agent.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val projectType: String = "ANDROID", // ANDROID, KOTLIN, JAVA, PYTHON, WEB, NODEJS, RUST
    val workspacePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val gitBranch: String = "main",
    val gitRemoteUrl: String? = null,
    val fileCount: Int = 0,
    val totalSizeBytes: Long = 0L
)
