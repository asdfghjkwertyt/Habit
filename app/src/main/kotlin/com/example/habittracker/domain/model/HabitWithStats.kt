package com.example.habittracker.domain.model

data class HabitWithStats(
    val habit: Habit,
    val completedDays: Set<Long>,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRate: Float
)
