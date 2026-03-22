package com.example.habittracker.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievement_progress")
data class AchievementProgressEntity(
    @PrimaryKey val key: String,
    val category: String,
    val title: String,
    val description: String,
    val target: Int,
    val progress: Int,
    val achieved: Boolean,
    val updatedAtMillis: Long,
    val achievedAtMillis: Long?
)
