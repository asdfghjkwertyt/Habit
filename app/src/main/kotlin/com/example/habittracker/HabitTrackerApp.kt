package com.example.habittracker

import android.app.Application
import com.example.habittracker.notifications.ReminderNotificationHelper
import com.example.habittracker.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class HabitTrackerApp : Application() {

	@Inject
	lateinit var reminderScheduler: ReminderScheduler

	override fun onCreate() {
		super.onCreate()
		ReminderNotificationHelper.createNotificationChannel(this)

		CoroutineScope(Dispatchers.IO).launch {
			reminderScheduler.syncAllDailyReminders()
		}
	}
}
