package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitFrequency
import com.example.habittracker.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddHabitUseCaseTest {

    private val repository: HabitRepository = mockk()
    private val useCase = AddHabitUseCase(repository)

    @Test
    fun `invoke adds habit and returns id`() = runTest {
        val habit = Habit(
            id = 0,
            name = "Read",
            description = "Read 10 pages",
            frequency = HabitFrequency.DAILY,
            colorHex = "#2E7D32",
            createdAtEpochDay = 100,
            archived = false
        )
        coEvery { repository.addHabit(habit) } returns 42L

        val result = useCase(habit)

        assertEquals(42L, result)
        coVerify(exactly = 1) { repository.addHabit(habit) }
    }
}
