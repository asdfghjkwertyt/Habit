package com.example.habittracker.data.repository

import android.graphics.Color
import com.example.habittracker.data.local.room.dao.AchievementDao
import com.example.habittracker.data.local.room.dao.HabitDao
import com.example.habittracker.data.local.room.dao.HabitLogDao
import com.example.habittracker.domain.model.AchievementCategory
import com.example.habittracker.domain.model.AchievementProgress
import com.example.habittracker.data.local.room.entity.HabitFrequency
import com.example.habittracker.domain.model.HabitFrequency as DomainHabitFrequency
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCompletion
import com.example.habittracker.domain.model.HabitStreak
import com.example.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HabitRepositoryImpl @Inject constructor(
    private val achievementDao: AchievementDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao
) : HabitRepository {

    override fun observeHabits(): Flow<List<Habit>> {
        return habitDao.observeAll().map { habits -> habits.map { it.toDomain() } }
    }

    override fun observeHabitById(habitId: Long): Flow<Habit?> {
        return habitDao.observeById(habitId.toInt()).map { it?.toDomain() }
    }

    override fun observeCompletions(habitId: Long): Flow<List<HabitCompletion>> {
        return habitLogDao.observeLogsByHabit(habitId.toInt()).map { items ->
            items.map {
                HabitCompletion(
                    habitId = it.habitId.toLong(),
                    epochDay = it.date,
                    completed = it.isCompleted,
                    note = it.note
                )
            }
        }
    }

    override fun observeAllCompletions(): Flow<List<HabitCompletion>> {
        return habitLogDao.observeAllLogs().map { items ->
            items.map {
                HabitCompletion(
                    habitId = it.habitId.toLong(),
                    epochDay = it.date,
                    completed = it.isCompleted,
                    note = it.note
                )
            }
        }
    }

    override fun observeAchievementProgress(): Flow<List<AchievementProgress>> {
        return achievementDao.observeAll().map { items ->
            items.map {
                AchievementProgress(
                    key = it.key,
                    category = runCatching { AchievementCategory.valueOf(it.category) }
                        .getOrDefault(AchievementCategory.Badge),
                    title = it.title,
                    description = it.description,
                    target = it.target,
                    progress = it.progress,
                    achieved = it.achieved,
                    updatedAtMillis = it.updatedAtMillis,
                    achievedAtMillis = it.achievedAtMillis
                )
            }
        }
    }

    override suspend fun addHabit(habit: Habit): Long = habitDao.insert(habit.toEntity())

    override suspend fun updateHabit(habit: Habit) = habitDao.update(habit.toEntity())

    override suspend fun deleteHabit(habitId: Long) = habitDao.deleteById(habitId.toInt())

    override suspend fun setHabitCompletion(habitId: Long, epochDay: Long, completed: Boolean) {
        habitLogDao.upsertCompletion(habitId = habitId.toInt(), date = epochDay, isCompleted = completed)
    }

    override suspend fun setHabitNote(habitId: Long, epochDay: Long, note: String?) {
        habitLogDao.upsertNote(habitId = habitId.toInt(), date = epochDay, note = note)
    }

    override suspend fun getHabitStreak(habitId: Long, todayEpochDay: Long): HabitStreak {
        val current = habitLogDao.getCurrentStreak(habitId = habitId.toInt(), todayEpochDay = todayEpochDay)
        val longest = habitLogDao.getLongestStreak(habitId = habitId.toInt())
        return HabitStreak(current = current, longest = longest)
    }

    override suspend fun syncAchievementProgress(items: List<AchievementProgress>) {
        achievementDao.upsertAll(
            items.map {
                com.example.habittracker.data.local.room.entity.AchievementProgressEntity(
                    key = it.key,
                    category = it.category.name,
                    title = it.title,
                    description = it.description,
                    target = it.target,
                    progress = it.progress,
                    achieved = it.achieved,
                    updatedAtMillis = it.updatedAtMillis,
                    achievedAtMillis = it.achievedAtMillis
                )
            }
        )
    }

    override suspend fun updateAchievementTarget(key: String, target: Int) {
        achievementDao.updateTarget(
            key = key,
            target = target.coerceAtLeast(1),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    private fun com.example.habittracker.data.local.room.entity.Habit.toDomain() = Habit(
        id = id.toLong(),
        name = name,
        description = description,
        frequency = when (frequency) {
            HabitFrequency.DAILY -> DomainHabitFrequency.DAILY
            HabitFrequency.WEEKLY -> DomainHabitFrequency.WEEKLY
        },
        colorHex = "#%06X".format(0xFFFFFF and color),
        reminderEnabled = reminderEnabled,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        reminderMessage = reminderMessage,
        createdAtEpochDay = createdDate,
        archived = false
    )

    private fun Habit.toEntity() = com.example.habittracker.data.local.room.entity.Habit(
        id = id.toInt(),
        name = name,
        description = description,
        frequency = when (frequency) {
            DomainHabitFrequency.DAILY -> HabitFrequency.DAILY
            DomainHabitFrequency.WEEKLY -> HabitFrequency.WEEKLY
        },
        createdDate = createdAtEpochDay,
        color = parseColorSafely(colorHex),
        reminderEnabled = reminderEnabled,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        reminderMessage = reminderMessage?.trim().orEmpty().ifBlank { null }
    )

    private fun parseColorSafely(colorHex: String): Int {
        return runCatching { Color.parseColor(colorHex) }.getOrDefault(Color.parseColor("#2E7D32"))
    }
}
