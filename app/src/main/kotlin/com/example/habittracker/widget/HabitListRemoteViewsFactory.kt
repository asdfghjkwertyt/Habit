package com.example.habittracker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.habittracker.R
import kotlinx.coroutines.runBlocking

class HabitListRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private var habits: List<HabitWidgetListItemUiModel> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        habits = runBlocking {
            HabitWidgetViewModel(context).loadAllWidgetHabits()
        }
    }

    override fun onDestroy() {
        habits = emptyList()
    }

    override fun getCount(): Int = habits.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position !in habits.indices) {
            return RemoteViews(context.packageName, R.layout.widget_list_item)
        }

        val model = habits[position]
        val views = RemoteViews(context.packageName, R.layout.widget_list_item)
        views.setTextViewText(R.id.widget_item_name, model.habitName)
        views.setTextViewText(
            R.id.widget_item_status,
            if (model.completedToday) "Completed" else "Not completed"
        )
        views.setTextViewText(R.id.widget_item_streak, "Streak ${model.streakCount}")
        views.setTextViewText(R.id.widget_item_complete_button, if (model.completedToday) "Done" else "Complete")
        views.setBoolean(R.id.widget_item_complete_button, "setEnabled", !model.completedToday)

        val fillInIntent = Intent().apply {
            action = ACTION_MARK_COMPLETED
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_HABIT_ID, model.habitId)
        }
        views.setOnClickFillInIntent(R.id.widget_item_complete_button, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_name, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews = RemoteViews(context.packageName, R.layout.widget_list_item)

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = habits.getOrNull(position)?.habitId ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
