package com.example.habittracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.example.habittracker.data.local.room.HabitTrackerDatabase
import com.example.habittracker.util.DateUtils
import kotlinx.coroutines.runBlocking

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId <= 0L) return
        val snoozeMinutes = intent.getLongExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)

        val scheduler = ReminderScheduler(context)
        when (action) {
            ACTION_MARK_DONE -> {
                handleMarkDone(context, habitId, scheduler)
            }

            ACTION_SNOOZE -> {
                handleSnooze(habitId, scheduler, snoozeMinutes)
            }

            else -> return
        }

        ReminderNotificationHelper.dismissHabitReminder(context, habitId)
        Log.d(TAG, "Handled action=$action for habitId=$habitId")
    }

    private fun handleMarkDone(context: Context, habitId: Long, scheduler: ReminderScheduler) {
        val db = Room.databaseBuilder(
            context,
            HabitTrackerDatabase::class.java,
            "habit_tracker.db"
        ).fallbackToDestructiveMigration().build()

        try {
            runBlocking {
                db.habitLogDao().upsertCompletion(
                    habitId = habitId.toInt(),
                    date = DateUtils.todayEpochDay(),
                    isCompleted = true
                )
            }
            scheduler.cancelTodayReminder(habitId)
        } catch (error: Exception) {
            Log.e(TAG, "Failed mark-done action for habitId=$habitId", error)
        } finally {
            db.close()
        }
    }

    private fun handleSnooze(habitId: Long, scheduler: ReminderScheduler, snoozeMinutes: Long) {
        runCatching {
            scheduler.snoozeHabitReminder(habitId = habitId, minutes = snoozeMinutes)
        }.onFailure { error ->
            Log.e(TAG, "Failed snooze action for habitId=$habitId", error)
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.example.habittracker.notifications.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.example.habittracker.notifications.ACTION_SNOOZE"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

        private const val DEFAULT_SNOOZE_MINUTES = 15L
        private const val TAG = "ReminderActionReceiver"
    }
}
