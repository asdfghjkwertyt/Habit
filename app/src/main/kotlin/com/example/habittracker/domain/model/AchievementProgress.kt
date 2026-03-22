package com.example.habittracker.domain.model

enum class AchievementCategory {
    Goal,
    Challenge,
    Badge
}

data class AchievementProgress(
    val key: String,
    val category: AchievementCategory,
    val title: String,
    val description: String,
    val target: Int,
    val progress: Int,
    val achieved: Boolean,
    val updatedAtMillis: Long,
    val achievedAtMillis: Long?
)
