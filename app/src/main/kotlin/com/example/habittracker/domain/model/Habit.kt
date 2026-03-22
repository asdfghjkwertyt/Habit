package com.example.habittracker.domain.model

data class Habit(
    val id: Long,
    val name: String,
    val description: String,
    val frequency: HabitFrequency,
    val colorHex: String,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val reminderMessage: String? = null,
    val createdAtEpochDay: Long,
    val archived: Boolean
)
