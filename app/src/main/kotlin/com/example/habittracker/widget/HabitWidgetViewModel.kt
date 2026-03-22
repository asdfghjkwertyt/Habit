package com.example.habittracker.widget

import android.content.Context
import com.example.habittracker.notifications.ReminderScheduler
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.repository.HabitRepository
import com.example.habittracker.util.DateUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

internal data class HabitWidgetUiModel(
    val habitId: Long,
    val habitName: String,
    val completedToday: Boolean,
    val streakCount: Int,
    val habitIndex: Int,
    val habitCount: Int
)

internal data class HabitWidgetListItemUiModel(
    val habitId: Long,
    val habitName: String,
    val completedToday: Boolean,
    val streakCount: Int
)

internal class HabitWidgetViewModel(context: Context) {

    private val appContext = context.applicationContext

    private val repository: HabitRepository = EntryPointAccessors
        .fromApplication(appContext, WidgetEntryPoint::class.java)
        .habitRepository()

    suspend fun loadWidgetHabit(habitIndex: Int): HabitWidgetUiModel? {
        val habits = repository.observeHabits().first().filterNot { it.archived }
        if (habits.isEmpty()) return null

        val safeIndex = habitIndex.mod(habits.size)
        val selectedHabit = habits[safeIndex]
        val completedToday = isCompletedToday(selectedHabit)
        val streak = repository.getHabitStreak(selectedHabit.id, DateUtils.todayEpochDay())

        return HabitWidgetUiModel(
            habitId = selectedHabit.id,
            habitName = selectedHabit.name,
            completedToday = completedToday,
            streakCount = streak.current,
            habitIndex = safeIndex,
            habitCount = habits.size
        )
    }

    suspend fun markHabitCompletedToday(habitId: Long) {
        repository.setHabitCompletion(
            habitId = habitId,
            epochDay = DateUtils.todayEpochDay(),
            completed = true
        )

        // Skip today's reminder once completion is recorded from widget.
        ReminderScheduler(appContext).cancelTodayReminder(habitId)
    }

    suspend fun loadAllWidgetHabits(): List<HabitWidgetListItemUiModel> {
        val habits = repository.observeHabits().first().filterNot { it.archived }
        if (habits.isEmpty()) return emptyList()

        val today = DateUtils.todayEpochDay()
        val completions = repository.observeAllCompletions().first()

        return habits.map { habit ->
            val completedToday = completions.any {
                it.habitId == habit.id && it.epochDay == today && it.completed
            }
            val streak = repository.getHabitStreak(habit.id, today)
            HabitWidgetListItemUiModel(
                habitId = habit.id,
                habitName = habit.name,
                completedToday = completedToday,
                streakCount = streak.current
            )
        }
    }

    private suspend fun isCompletedToday(habit: Habit): Boolean {
        val today = DateUtils.todayEpochDay()
        return repository.observeAllCompletions().first().any {
            it.habitId == habit.id && it.epochDay == today && it.completed
        }
    }
}
