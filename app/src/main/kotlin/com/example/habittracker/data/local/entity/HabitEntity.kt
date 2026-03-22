package com.example.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val colorHex: String,
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val createdAtEpochDay: Long,
    val archived: Boolean = false
)
