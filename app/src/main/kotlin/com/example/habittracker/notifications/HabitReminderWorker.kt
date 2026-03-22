package com.example.habittracker.notifications

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.local.room.HabitTrackerDatabase

class HabitReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getLong(KEY_HABIT_ID, -1L)
        if (habitId <= 0L) {
            Log.e(TAG, "Invalid habit id for reminder worker")
            return Result.failure()
        }

        Log.d(TAG, "Worker started for habitId=$habitId")

        val db = Room.databaseBuilder(
            applicationContext,
            HabitTrackerDatabase::class.java,
            "habit_tracker.db"
        ).fallbackToDestructiveMigration().build()

        return try {
            val habit = db.habitDao().getById(habitId.toInt())
            if (habit == null || !habit.reminderEnabled) {
                Log.d(TAG, "Worker skip habitId=$habitId reason=missing_or_disabled")
                Result.success()
            } else {
                ReminderNotificationHelper.showHabitReminder(
                    context = applicationContext,
                    habitId = habitId,
                    habitName = habit.name
                )
                Log.d(TAG, "Worker posted notification for habitId=$habitId")
                Result.success()
            }
        } catch (error: Exception) {
            Log.e(TAG, "Worker failed for habitId=$habitId", error)
            Result.retry()
        } finally {
            db.close()
        }
    }

    companion object {
        const val KEY_HABIT_ID = "key_habit_id"
        private const val TAG = "HabitReminderWorker"
    }
}
