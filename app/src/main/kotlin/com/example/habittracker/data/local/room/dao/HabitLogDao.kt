package com.example.habittracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.habittracker.data.local.room.entity.HabitLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: HabitLog): Long

    @Update
    suspend fun update(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE id = :logId")
    suspend fun deleteById(logId: Int)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteByHabitAndDate(habitId: Int, date: Long)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun observeLogsByHabit(habitId: Int): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs")
    fun observeAllLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getByHabitAndDate(habitId: Int, date: Long): HabitLog?

    @Query(
        """
        WITH RECURSIVE
        completed(date) AS (
            SELECT DISTINCT date
            FROM habit_logs
            WHERE habitId = :habitId
              AND isCompleted = 1
        ),
        streaks(start_day, current_day, length) AS (
            SELECT c.date, c.date, 1
            FROM completed c
            WHERE NOT EXISTS (
                SELECT 1 FROM completed p WHERE p.date = c.date - 1
            )
            UNION ALL
            SELECT s.start_day, c.date, s.length + 1
            FROM streaks s
            JOIN completed c ON c.date = s.current_day + 1
        )
        SELECT COALESCE(MAX(length), 0) FROM streaks
        """
    )
    suspend fun getLongestStreak(habitId: Int): Int

    @Query(
        """
        WITH RECURSIVE streak(day) AS (
            SELECT :todayEpochDay
            UNION ALL
            SELECT day - 1
            FROM streak
            WHERE EXISTS (
                SELECT 1
                FROM habit_logs
                WHERE habitId = :habitId
                  AND date = day
                  AND isCompleted = 1
            )
        )
        SELECT COUNT(*) - 1 FROM streak
        """
    )
    suspend fun getCurrentStreak(habitId: Int, todayEpochDay: Long): Int

    @Transaction
    suspend fun upsertCompletion(habitId: Int, date: Long, isCompleted: Boolean) {
        val existing = getByHabitAndDate(habitId = habitId, date = date)
        when {
            existing == null && isCompleted -> {
                insert(
                    HabitLog(
                        habitId = habitId,
                        date = date,
                        isCompleted = true,
                        note = null
                    )
                )
            }

            existing != null -> {
                update(existing.copy(isCompleted = isCompleted))
            }
        }
    }

    @Transaction
    suspend fun upsertNote(habitId: Int, date: Long, note: String?) {
        val normalizedNote = note?.trim().orEmpty().ifBlank { null }
        val existing = getByHabitAndDate(habitId = habitId, date = date)
        when {
            existing == null && normalizedNote != null -> {
                insert(
                    HabitLog(
                        habitId = habitId,
                        date = date,
                        isCompleted = false,
                        note = normalizedNote
                    )
                )
            }

            existing != null -> {
                update(existing.copy(note = normalizedNote))
            }
        }
    }
}
