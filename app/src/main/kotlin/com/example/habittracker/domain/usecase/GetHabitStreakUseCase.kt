package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.model.HabitStreak
import com.example.habittracker.domain.repository.HabitRepository
import javax.inject.Inject

class GetHabitStreakUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: Long, todayEpochDay: Long): HabitStreak {
        return repository.getHabitStreak(habitId = habitId, todayEpochDay = todayEpochDay)
    }
}
