package com.example.habittracker.ui.viewmodel

import com.example.habittracker.data.preferences.HomeLayoutPreferences
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCompletion
import com.example.habittracker.domain.model.HabitFrequency
import com.example.habittracker.domain.model.HabitStreak
import com.example.habittracker.domain.model.HabitWithStats
import com.example.habittracker.domain.usecase.AddHabitUseCase
import com.example.habittracker.domain.usecase.DeleteHabitUseCase
import com.example.habittracker.domain.usecase.GetAllCompletionsUseCase
import com.example.habittracker.domain.usecase.GetHabitByIdUseCase
import com.example.habittracker.domain.usecase.GetHabitCompletionsUseCase
import com.example.habittracker.domain.usecase.GetHabitStatsUseCase
import com.example.habittracker.domain.usecase.GetHabitStreakUseCase
import com.example.habittracker.domain.usecase.GetHabitsUseCase
import com.example.habittracker.domain.usecase.HabitUseCases
import com.example.habittracker.domain.usecase.MarkHabitCompleteUseCase
import com.example.habittracker.domain.usecase.SetHabitNoteUseCase
import com.example.habittracker.domain.usecase.UpdateHabitUseCase
import com.example.habittracker.notifications.ReminderScheduler
import com.example.habittracker.testutil.MainDispatcherRule
import com.example.habittracker.ui.theme.ThemeMode
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HabitViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val addHabitUseCase: AddHabitUseCase = mockk()
    private val updateHabitUseCase: UpdateHabitUseCase = mockk(relaxed = true)
    private val deleteHabitUseCase: DeleteHabitUseCase = mockk(relaxed = true)
    private val markHabitCompleteUseCase: MarkHabitCompleteUseCase = mockk(relaxed = true)
    private val setHabitNoteUseCase: SetHabitNoteUseCase = mockk(relaxed = true)
    private val getHabitStreakUseCase: GetHabitStreakUseCase = mockk()
    private val getHabitByIdUseCase: GetHabitByIdUseCase = mockk(relaxed = true)
    private val getAllCompletionsUseCase: GetAllCompletionsUseCase = mockk()
    private val getHabitCompletionsUseCase: GetHabitCompletionsUseCase = mockk(relaxed = true)
    private val getHabitStatsUseCase: GetHabitStatsUseCase = mockk()
    private val getHabitsUseCase: GetHabitsUseCase = mockk()

    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val preferences: HomeLayoutPreferences = mockk()

    private fun createViewModel(
        habitsFlow: MutableStateFlow<List<Habit>> = MutableStateFlow(emptyList()),
        statsFlow: MutableStateFlow<List<HabitWithStats>> = MutableStateFlow(emptyList()),
        completionsFlow: MutableStateFlow<List<HabitCompletion>> = MutableStateFlow(emptyList())
    ): HabitViewModel {
        every { getHabitsUseCase.invoke() } returns habitsFlow
        every { getHabitStatsUseCase.invoke() } returns statsFlow
        every { getAllCompletionsUseCase.invoke() } returns completionsFlow
        every { preferences.layoutMode } returns MutableStateFlow(HomeLayoutMode.Auto)
        every { preferences.selectedTopLevelRoute } returns MutableStateFlow("habits")
        every { preferences.selectedHabitId } returns MutableStateFlow<Long?>(null)
        every { preferences.themeMode } returns MutableStateFlow(ThemeMode.System)
        coJustRun { preferences.setSelectedHabitId(any()) }
        coJustRun { preferences.setLayoutMode(any()) }
        coJustRun { preferences.setSelectedTopLevelRoute(any()) }
        coJustRun { preferences.setThemeMode(any()) }
        coEvery { getHabitStreakUseCase.invoke(any(), any()) } returns HabitStreak(1, 2)
        coEvery { addHabitUseCase.invoke(any()) } returns 101L

        val useCases = HabitUseCases(
            getHabits = getHabitsUseCase,
            addHabit = addHabitUseCase,
            updateHabit = updateHabitUseCase,
            deleteHabit = deleteHabitUseCase,
            markHabitComplete = markHabitCompleteUseCase,
            setHabitNote = setHabitNoteUseCase,
            getHabitStreak = getHabitStreakUseCase,
            getHabitById = getHabitByIdUseCase,
            getAllCompletions = getAllCompletionsUseCase,
            getHabitCompletions = getHabitCompletionsUseCase,
            getHabitStats = getHabitStatsUseCase
        )

        return HabitViewModel(useCases, reminderScheduler, preferences)
    }

    @Test
    fun `addHabit calls use case with trimmed fields`() {
        val viewModel = createViewModel()

        viewModel.addHabit(
            name = "  Reading  ",
            description = " 20 min ",
            frequency = HabitFrequency.DAILY,
            colorHex = "#2E7D32",
            reminderEnabled = false,
            reminderHour = null,
            reminderMinute = null,
            reminderMessage = null
        )
        Thread.sleep(120)

        coVerify(timeout = 1500, exactly = 1) {
            addHabitUseCase.invoke(match {
                it.name == "Reading" &&
                    it.description == "20 min" &&
                    it.frequency == HabitFrequency.DAILY &&
                    it.colorHex == "#2E7D32"
            })
        }
    }

    @Test
    fun `toggleHabitCompletion marks habit complete when day is not completed`() {
        val habit = Habit(
            id = 1,
            name = "Meditate",
            description = "",
            frequency = HabitFrequency.DAILY,
            colorHex = "#1565C0",
            createdAtEpochDay = 10,
            archived = false
        )
        val stats = HabitWithStats(
            habit = habit,
            completedDays = emptySet(),
            currentStreak = 0,
            longestStreak = 0,
            completionRate = 0f
        )
        val viewModel = createViewModel(
            habitsFlow = MutableStateFlow(listOf(habit)),
            statsFlow = MutableStateFlow(listOf(stats))
        )
        Thread.sleep(120)

        viewModel.toggleHabitCompletion(habitId = 1L, epochDay = 99L)
        Thread.sleep(150)

        coVerify(timeout = 1500, exactly = 1) { markHabitCompleteUseCase.invoke(1L, 99L, true) }
    }

    @Test
    fun `calculateStreaks updates streak map`() {
        coEvery { getHabitStreakUseCase.invoke(2L, any()) } returns HabitStreak(current = 5, longest = 12)
        val viewModel = createViewModel()

        viewModel.calculateStreaks(2L)
        Thread.sleep(150)

        coVerify(timeout = 1500, exactly = 1) { getHabitStreakUseCase.invoke(2L, any()) }
    }
}
