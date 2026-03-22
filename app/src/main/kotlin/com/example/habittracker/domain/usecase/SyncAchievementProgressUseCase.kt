package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.model.AchievementProgress
import com.example.habittracker.domain.repository.HabitRepository
import javax.inject.Inject

class SyncAchievementProgressUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(items: List<AchievementProgress>) {
        repository.syncAchievementProgress(items)
    }
}
