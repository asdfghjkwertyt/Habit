package com.example.habittracker.notifications

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.habittracker.data.local.room.HabitTrackerDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    fun scheduleHabitReminder(
        habitId: Long,
        habitName: String,
        message: String?,
        hour: Int,
        minute: Int
    ) {
        scheduleHabitReminderInternal(
            habitId = habitId,
            habitName = habitName,
            hour = hour,
            minute = minute,
            startTomorrow = false
        )
    }

    fun cancelTodayReminder(habitId: Long) {
        val db = Room.databaseBuilder(
            context,
            HabitTrackerDatabase::class.java,
            "habit_tracker.db"
        ).fallbackToDestructiveMigration().build()

        try {
            val habit = runBlocking { db.habitDao().getById(habitId.toInt()) }
            if (habit == null || !habit.reminderEnabled || habit.reminderHour == null || habit.reminderMinute == null) {
                cancelHabitReminder(habitId)
                return
            }

            Log.d(
                TAG,
                "cancelTodayReminder habitId=$habitId -> next run tomorrow at %02d:%02d".format(
                    habit.reminderHour,
                    habit.reminderMinute
                )
            )

            scheduleHabitReminderInternal(
                habitId = habitId,
                habitName = habit.name,
                hour = habit.reminderHour,
                minute = habit.reminderMinute,
                startTomorrow = true
            )
        } finally {
            db.close()
        }
    }

    private fun scheduleHabitReminderInternal(
        habitId: Long,
        habitName: String,
        hour: Int,
        minute: Int,
        startTomorrow: Boolean
    ) {
        val workManager = WorkManager.getInstance(context)
        val initialDelay = calculateInitialDelayMillis(
            hour = hour,
            minute = minute,
            startTomorrow = startTomorrow
        )

        Log.d(
            TAG,
            "schedule habitId=$habitId name=$habitName at=%02d:%02d delayMs=$initialDelay startTomorrow=$startTomorrow".format(hour, minute)
        )

        val request = PeriodicWorkRequestBuilder<HabitReminderWorker>(1, TimeUnit.DAYS)
            .setInputData(workDataOf(HabitReminderWorker.KEY_HABIT_ID to habitId))
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(REMINDER_TAG)
            .addTag(habitTag(habitId))
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName(habitId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelHabitReminder(habitId: Long) {
        Log.d(TAG, "cancel habitId=$habitId")
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(habitId))
        WorkManager.getInstance(context).cancelUniqueWork(snoozeWorkName(habitId))
    }

    fun rescheduleHabitReminder(
        habitId: Long,
        habitName: String,
        message: String?,
        hour: Int,
        minute: Int
    ) {
        Log.d(
            TAG,
            "reschedule habitId=$habitId name=$habitName at=%02d:%02d".format(hour, minute)
        )
        cancelHabitReminder(habitId)
        scheduleHabitReminder(
            habitId = habitId,
            habitName = habitName,
            message = message,
            hour = hour,
            minute = minute
        )
    }

    fun sendTestReminder(habitName: String, message: String?) {
        val testHabitId = System.currentTimeMillis()
        Log.d(TAG, "sendTestReminder name=$habitName id=$testHabitId")
        ReminderNotificationHelper.showHabitReminder(
            context = context,
            habitId = testHabitId,
            habitName = habitName
        )
    }

    fun snoozeHabitReminder(habitId: Long, minutes: Long = 15L) {
        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInputData(workDataOf(HabitReminderWorker.KEY_HABIT_ID to habitId))
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .addTag(REMINDER_TAG)
            .addTag(habitTag(habitId))
            .addTag(SNOOZE_TAG)
            .build()

        Log.d(TAG, "snooze habitId=$habitId minutes=$minutes")
        WorkManager.getInstance(context).enqueueUniqueWork(
            snoozeWorkName(habitId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun syncAllDailyReminders() {
        Log.d(TAG, "syncAllDailyReminders started")
        val db = Room.databaseBuilder(
            context,
            HabitTrackerDatabase::class.java,
            "habit_tracker.db"
        ).fallbackToDestructiveMigration().build()

        try {
            val habits = runBlocking { db.habitDao().observeAllSnapshot() }
            Log.d(TAG, "syncAllDailyReminders loaded=${habits.size}")
            habits.forEach { habit ->
                val hour = habit.reminderHour
                val minute = habit.reminderMinute
                if (habit.reminderEnabled && hour != null && minute != null) {
                    rescheduleHabitReminder(
                        habitId = habit.id.toLong(),
                        habitName = habit.name,
                        message = habit.reminderMessage,
                        hour = hour,
                        minute = minute
                    )
                } else {
                    cancelHabitReminder(habit.id.toLong())
                }
            }
            Log.d(TAG, "syncAllDailyReminders finished")
        } catch (error: Exception) {
            Log.e(TAG, "syncAllDailyReminders failed", error)
            throw error
        } finally {
            db.close()
        }
    }

    private fun calculateInitialDelayMillis(hour: Int, minute: Int, startTomorrow: Boolean): Long {
        val now = ZonedDateTime.now()
        var nextRun = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (startTomorrow || !nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }
        return Duration.between(now, nextRun).toMillis().coerceAtLeast(1L)
    }

    private fun uniqueWorkName(habitId: Long): String = "habit_reminder_work_$habitId"

    private fun snoozeWorkName(habitId: Long): String = "habit_reminder_snooze_$habitId"

    private fun habitTag(habitId: Long): String = "habit_reminder_habit_$habitId"

    companion object {
        private const val REMINDER_TAG = "habit_reminder"
        private const val SNOOZE_TAG = "habit_reminder_snooze"
        private const val TAG = "ReminderScheduler"
    }
}
