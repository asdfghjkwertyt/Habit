package com.example.habittracker.domain.repository

import com.example.habittracker.domain.model.AchievementProgress
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCompletion
import com.example.habittracker.domain.model.HabitStreak
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeHabitById(habitId: Long): Flow<Habit?>
    fun observeCompletions(habitId: Long): Flow<List<HabitCompletion>>
    fun observeAllCompletions(): Flow<List<HabitCompletion>>
    fun observeAchievementProgress(): Flow<List<AchievementProgress>>
    suspend fun addHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habitId: Long)
    suspend fun setHabitCompletion(habitId: Long, epochDay: Long, completed: Boolean)
    suspend fun setHabitNote(habitId: Long, epochDay: Long, note: String?)
    suspend fun getHabitStreak(habitId: Long, todayEpochDay: Long): HabitStreak
    suspend fun syncAchievementProgress(items: List<AchievementProgress>)
    suspend fun updateAchievementTarget(key: String, target: Int)
}
