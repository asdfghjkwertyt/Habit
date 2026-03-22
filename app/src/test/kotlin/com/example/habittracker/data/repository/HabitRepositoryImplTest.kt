package com.example.habittracker.data.repository

import com.example.habittracker.data.local.room.dao.HabitDao
import com.example.habittracker.data.local.room.dao.HabitLogDao
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitFrequency
import io.mockk.every
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HabitRepositoryImplTest {

    private val habitDao: HabitDao = mockk()
    private val habitLogDao: HabitLogDao = mockk()
    private val repository = HabitRepositoryImpl(habitDao, habitLogDao)

    @Before
    fun setUp() {
        mockkStatic(android.graphics.Color::class)
        every { android.graphics.Color.parseColor(any()) } returns 0x2E7D32
    }

    @After
    fun tearDown() {
        unmockkStatic(android.graphics.Color::class)
    }

    @Test
    fun `addHabit inserts mapped habit`() = runTest {
        val habit = Habit(
            id = 0,
            name = "Workout",
            description = "30 minutes",
            frequency = HabitFrequency.DAILY,
            colorHex = "#2E7D32",
            reminderEnabled = true,
            reminderHour = 7,
            reminderMinute = 30,
            reminderMessage = "Move now",
            createdAtEpochDay = 100,
            archived = false
        )
        coEvery { habitDao.insert(any()) } returns 11L

        val id = repository.addHabit(habit)

        assertEquals(11L, id)
        coVerify(exactly = 1) {
            habitDao.insert(match {
                it.name == "Workout" &&
                    it.description == "30 minutes" &&
                    it.frequency.name == "DAILY" &&
                    it.createdDate == 100L &&
                    it.reminderEnabled &&
                    it.reminderHour == 7 &&
                    it.reminderMinute == 30 &&
                    it.reminderMessage == "Move now"
            })
        }
    }

    @Test
    fun `setHabitCompletion delegates to log dao`() = runTest {
        coJustRun { habitLogDao.upsertCompletion(habitId = 5, date = 123L, isCompleted = true) }

        repository.setHabitCompletion(habitId = 5L, epochDay = 123L, completed = true)

        coVerify(exactly = 1) { habitLogDao.upsertCompletion(habitId = 5, date = 123L, isCompleted = true) }
    }

    @Test
    fun `getHabitStreak combines current and longest values`() = runTest {
        coEvery { habitLogDao.getCurrentStreak(habitId = 2, todayEpochDay = 700L) } returns 3
        coEvery { habitLogDao.getLongestStreak(habitId = 2) } returns 8

        val streak = repository.getHabitStreak(habitId = 2L, todayEpochDay = 700L)

        assertEquals(3, streak.current)
        assertEquals(8, streak.longest)
    }
}
