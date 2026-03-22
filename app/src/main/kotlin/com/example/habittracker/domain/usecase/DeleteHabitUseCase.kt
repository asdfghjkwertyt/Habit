package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.repository.HabitRepository
import javax.inject.Inject

class DeleteHabitUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: Long) = repository.deleteHabit(habitId)
}
