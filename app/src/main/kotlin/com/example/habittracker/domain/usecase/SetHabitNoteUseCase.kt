package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.repository.HabitRepository
import javax.inject.Inject

class SetHabitNoteUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: Long, epochDay: Long, note: String?) {
        repository.setHabitNote(habitId = habitId, epochDay = epochDay, note = note)
    }
}
