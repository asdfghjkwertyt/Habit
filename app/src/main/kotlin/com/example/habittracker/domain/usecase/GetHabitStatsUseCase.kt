package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.model.HabitWithStats
import com.example.habittracker.domain.repository.HabitRepository
import com.example.habittracker.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetHabitStatsUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    operator fun invoke(): Flow<List<HabitWithStats>> {
        return combine(
            repository.observeHabits(),
            repository.observeAllCompletions()
        ) { habits, completions ->
            habits.map { habit ->
                val days = completions
                    .asSequence()
                    .filter { it.habitId == habit.id && it.completed }
                    .map { it.epochDay }
                    .toSet()

                val sortedDays = days.sorted()
                val longestStreak = DateUtils.longestStreak(sortedDays)
                val currentStreak = DateUtils.currentStreak(sortedDays, DateUtils.todayEpochDay())
                val totalDays = (DateUtils.todayEpochDay() - habit.createdAtEpochDay + 1).coerceAtLeast(1)
                val completionRate = (days.size.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)

                HabitWithStats(
                    habit = habit,
                    completedDays = days,
                    currentStreak = currentStreak,
                    longestStreak = longestStreak,
                    completionRate = completionRate
                )
            }
        }
    }
}
