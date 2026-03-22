package com.example.habittracker.widget

import android.content.Intent
import android.widget.RemoteViewsService

class HabitListRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitListRemoteViewsFactory(applicationContext, intent)
    }
}
