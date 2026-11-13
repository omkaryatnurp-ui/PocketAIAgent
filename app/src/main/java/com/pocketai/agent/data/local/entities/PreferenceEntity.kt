package com.pocketai.agent.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_preferences")
data class PreferenceEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
