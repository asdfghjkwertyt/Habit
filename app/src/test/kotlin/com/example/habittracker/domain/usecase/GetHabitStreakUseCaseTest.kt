package com.example.habittracker.domain.usecase

import com.example.habittracker.domain.model.HabitStreak
import com.example.habittracker.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetHabitStreakUseCaseTest {

    private val repository: HabitRepository = mockk()
    private val useCase = GetHabitStreakUseCase(repository)

    @Test
    fun `invoke returns streak from repository`() = runTest {
        val expected = HabitStreak(current = 4, longest = 9)
        coEvery { repository.getHabitStreak(habitId = 3L, todayEpochDay = 400L) } returns expected

        val result = useCase(habitId = 3L, todayEpochDay = 400L)

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.getHabitStreak(habitId = 3L, todayEpochDay = 400L) }
    }
}
