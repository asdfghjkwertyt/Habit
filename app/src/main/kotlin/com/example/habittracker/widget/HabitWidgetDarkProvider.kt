package com.example.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class HabitWidgetDarkProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { appWidgetId ->
                updateSingleWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_MARK_COMPLETED -> {
                val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                if (habitId > 0L) {
                    Log.d("HabitWidgetDarkProvider", "ACTION_MARK_COMPLETED habitId=$habitId")
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            HabitWidgetViewModel(context).markHabitCompletedToday(habitId)
                            WidgetRefreshCoordinator.refreshAll(context)
                        }
                        pendingResult.finish()
                    }
                }
            }

            ACTION_NEXT_HABIT -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    Log.d("HabitWidgetDarkProvider", "ACTION_NEXT_HABIT widgetId=$widgetId")
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            val nextIndex = loadWidgetIndex(context, widgetId) + 1
                            saveWidgetIndex(context, widgetId, nextIndex)
                            updateWidget(context, widgetId)
                        }
                        pendingResult.finish()
                    }
                }
            }

            AppWidgetManager.ACTION_APPWIDGET_DELETED -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    clearWidgetIndex(context, widgetId)
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "habit_widget_dark_prefs"
        private const val PREF_INDEX_PREFIX = "habit_dark_index_"

        private fun updateWidget(context: Context, appWidgetId: Int) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }

        private fun updateSingleWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val model = runBlocking {
                val viewModel = HabitWidgetViewModel(context)
                val index = loadWidgetIndex(context, appWidgetId)
                viewModel.loadWidgetHabit(index)
            }

            val views = RemoteViews(context.packageName, R.layout.widget_layout_dark)
            views.setOnClickPendingIntent(R.id.widget_root, appOpenPendingIntent(context, appWidgetId))

            if (model == null) {
                views.setTextViewText(R.id.widget_habit_name, "No habits yet")
                views.setTextViewText(R.id.widget_status, "Create a habit to get started")
                views.setTextViewText(R.id.widget_streak, "Streak: 0")
                views.setTextViewText(R.id.widget_complete_button, "Mark Complete")
                views.setBoolean(R.id.widget_complete_button, "setEnabled", false)
                views.setViewVisibility(R.id.widget_next_button, View.GONE)
            } else {
                saveWidgetIndex(context, appWidgetId, model.habitIndex)

                val statusText = if (model.completedToday) "Completed today" else "Not completed today"
                val streakText = "Streak: ${model.streakCount}"
                val completeButtonText = if (model.completedToday) "Completed" else "Mark Complete"

                views.setTextViewText(R.id.widget_habit_name, model.habitName)
                views.setTextViewText(R.id.widget_status, statusText)
                views.setTextViewText(R.id.widget_streak, streakText)
                views.setTextViewText(R.id.widget_complete_button, completeButtonText)
                views.setBoolean(R.id.widget_complete_button, "setEnabled", !model.completedToday)
                views.setViewVisibility(R.id.widget_next_button, if (model.habitCount > 1) View.VISIBLE else View.GONE)

                views.setOnClickPendingIntent(
                    R.id.widget_complete_button,
                    markCompletePendingIntent(context, appWidgetId, model.habitId)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_next_button,
                    nextHabitPendingIntent(context, appWidgetId)
                )
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun appOpenPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                appWidgetId + 20_000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun markCompletePendingIntent(context: Context, appWidgetId: Int, habitId: Long): PendingIntent {
            val intent = Intent(context, HabitWidgetDarkProvider::class.java).apply {
                action = ACTION_MARK_COMPLETED
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_HABIT_ID, habitId)
            }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId + 20_000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun nextHabitPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, HabitWidgetDarkProvider::class.java).apply {
                action = ACTION_NEXT_HABIT
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId + 30_000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun loadWidgetIndex(context: Context, appWidgetId: Int): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt("$PREF_INDEX_PREFIX$appWidgetId", 0)
        }

        private fun saveWidgetIndex(context: Context, appWidgetId: Int, index: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt("$PREF_INDEX_PREFIX$appWidgetId", index).apply()
        }

        private fun clearWidgetIndex(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove("$PREF_INDEX_PREFIX$appWidgetId").apply()
        }
    }
}
