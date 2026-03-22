package com.example.habittracker.domain.model

data class HabitCompletion(
    val habitId: Long,
    val epochDay: Long,
    val completed: Boolean,
    val note: String? = null
)
