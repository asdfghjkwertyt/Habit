package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.model.AchievementProgress
import com.example.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAchievementProgressUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    operator fun invoke(): Flow<List<AchievementProgress>> = repository.observeAchievementProgress()
}
