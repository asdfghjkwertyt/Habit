package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.repository.HabitRepository
import javax.inject.Inject

class UpdateAchievementTargetUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(key: String, target: Int) {
        repository.updateAchievementTarget(key = key, target = target)
    }
}
