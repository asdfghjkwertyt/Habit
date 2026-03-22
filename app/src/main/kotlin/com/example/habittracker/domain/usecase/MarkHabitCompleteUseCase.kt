package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.repository.HabitRepository
import javax.inject.Inject

class MarkHabitCompleteUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: Long, date: Long, isCompleted: Boolean) {
        repository.setHabitCompletion(habitId = habitId, epochDay = date, completed = isCompleted)
    }
}
