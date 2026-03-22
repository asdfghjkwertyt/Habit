package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.repository.HabitRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MarkHabitCompleteUseCaseTest {

    private val repository: HabitRepository = mockk()
    private val useCase = MarkHabitCompleteUseCase(repository)

    @Test
    fun `invoke marks habit completion`() = runTest {
        coJustRun { repository.setHabitCompletion(habitId = 7L, epochDay = 200L, completed = true) }

        useCase(habitId = 7L, date = 200L, isCompleted = true)

        coVerify(exactly = 1) {
            repository.setHabitCompletion(habitId = 7L, epochDay = 200L, completed = true)
        }
    }
}
