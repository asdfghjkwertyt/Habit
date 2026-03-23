package com.example.habittracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.example.habittracker.R

internal object WidgetRefreshCoordinator {

    suspend fun refreshAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        refreshProvider(context, appWidgetManager, HabitWidgetProvider::class.java)
        refreshProvider(context, appWidgetManager, HabitWidgetDarkProvider::class.java)
        refreshProvider(context, appWidgetManager, HabitListWidgetProvider::class.java)
        HabitGlanceWidget().updateAll(context)

        val listComponent = ComponentName(context, HabitListWidgetProvider::class.java)
        val listIds = appWidgetManager.getAppWidgetIds(listComponent)
        if (listIds.isNotEmpty()) {
            appWidgetManager.notifyAppWidgetViewDataChanged(listIds, R.id.widget_habit_list)
        }
    }

    private fun refreshProvider(
        context: Context,
        appWidgetManager: AppWidgetManager,
        providerClass: Class<*>
    ) {
        val component = ComponentName(context, providerClass)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
        if (appWidgetIds.isEmpty()) return

        val updateIntent = Intent(context, providerClass).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        context.sendBroadcast(updateIntent)
    }
}
