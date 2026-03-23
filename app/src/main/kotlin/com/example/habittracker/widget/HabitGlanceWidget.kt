package com.example.habittracker.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.Dp
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.repository.HabitRepository
import com.example.habittracker.util.DateUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

private const val MAX_WIDGET_HABITS = 3
private const val PROGRESS_WINDOW_DAYS = 7

internal data class HabitGlanceUiModel(
    val habitId: Long,
    val name: String,
    val completedToday: Boolean,
    val streakCount: Int,
    val completionRate: Float
)

private val HabitIdParamKey = ActionParameters.Key<Long>("habit_id")

private class HabitGlanceRepository(context: Context) {
    private val repository: HabitRepository = EntryPointAccessors
        .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        .habitRepository()

    suspend fun getHabits(): List<Habit> = repository.observeHabits().first().filterNot { it.archived }
    suspend fun getAllCompletions() = repository.observeAllCompletions().first()
    suspend fun getStreak(habitId: Long, epochDay: Long) = repository.getHabitStreak(habitId, epochDay)
    suspend fun setCompletionToday(habitId: Long, epochDay: Long) = repository.setHabitCompletion(habitId, epochDay, true)
}

private suspend fun loadGlanceHabits(context: Context): List<HabitGlanceUiModel> {
    val repository = HabitGlanceRepository(context)
    val habits = repository.getHabits().take(MAX_WIDGET_HABITS)
    if (habits.isEmpty()) return emptyList()

    val today = DateUtils.todayEpochDay()
    val priorDays = DateUtils.lastNDays(PROGRESS_WINDOW_DAYS)
    val allCompletions = repository.getAllCompletions()

    return habits.map { habit ->
        val habitCompletions = allCompletions.filter { it.habitId == habit.id }
        val doneDays = priorDays.count { day -> habitCompletions.any { c -> c.epochDay == day && c.completed } }
        val completedToday = habitCompletions.any { it.epochDay == today && it.completed }
        val streak = repository.getStreak(habit.id, today)

        HabitGlanceUiModel(
            habitId = habit.id,
            name = habit.name,
            completedToday = completedToday,
            streakCount = streak.current,
            completionRate = doneDays.toFloat() / PROGRESS_WINDOW_DAYS
        )
    }
}

class HabitGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, glanceId: GlanceId) {
        val items = loadGlanceHabits(context)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(Dp(12f))
            ) {
                Text(
                    text = "Habit Tracker"
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (items.isEmpty()) {
                    Text(text = "No habits yet. Add one in app.")
                } else {
                    items.forEach { habit ->
                        Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            Row(modifier = GlanceModifier.fillMaxWidth()) {
                                Text(text = habit.name)
                            }

                            Spacer(modifier = GlanceModifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = habit.completionRate,
                                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                                trackColor = ColorProvider(Color(0xFFE5EBF6)),
                                progressColor = ColorProvider(Color(0xFF4C72E8))
                            )

                            Spacer(modifier = GlanceModifier.height(6.dp))

                            Button(
                                text = if (habit.completedToday) "✓ Done" else "Complete",
                                onClick = actionRunCallback<CompleteHabitAction>(
                                    parameters = actionParametersOf(HabitIdParamKey to habit.habitId)
                                ),
                                colors = ButtonDefaults.buttonColors(backgroundColor = ColorProvider(Color(0xFF4C72E8))),
                                enabled = !habit.completedToday,
                                modifier = GlanceModifier.fillMaxWidth().height(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

class HabitGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitGlanceWidget()
}

class CompleteHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[HabitIdParamKey] ?: return
        HabitGlanceRepository(context).setCompletionToday(habitId, DateUtils.todayEpochDay())
        HabitGlanceWidget().updateAll(context)
    }
}
