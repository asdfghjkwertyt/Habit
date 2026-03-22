package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.model.HabitCompletion
import com.example.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHabitCompletionsUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    operator fun invoke(habitId: Long): Flow<List<HabitCompletion>> = repository.observeCompletions(habitId)
}
