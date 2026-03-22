package com.example.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitListWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_MARK_COMPLETED) {
            val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
            if (habitId > 0L) {
                Log.d("HabitListWidgetProvider", "ACTION_MARK_COMPLETED habitId=$habitId")
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
    }

    companion object {
        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_list_layout)

            val svcIntent = Intent(context, HabitListRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_habit_list, svcIntent)
            views.setEmptyView(R.id.widget_habit_list, R.id.widget_empty_text)

            views.setOnClickPendingIntent(R.id.widget_list_root, appOpenPendingIntent(context, appWidgetId))
            views.setPendingIntentTemplate(
                R.id.widget_habit_list,
                markCompleteTemplatePendingIntent(context, appWidgetId)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_habit_list)
        }

        private fun appOpenPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                appWidgetId + 40_000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun markCompleteTemplatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, HabitListWidgetProvider::class.java).apply {
                action = ACTION_MARK_COMPLETED
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId + 50_000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
