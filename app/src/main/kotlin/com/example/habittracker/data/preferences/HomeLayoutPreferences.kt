package com.example.habittracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.habittracker.ui.viewmodel.HomeLayoutMode
import com.example.habittracker.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "home_ui_preferences")

class HomeLayoutPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val layoutModeKey = stringPreferencesKey("home_layout_mode")
    private val selectedTopLevelRouteKey = stringPreferencesKey("selected_top_level_route")
    private val selectedHabitIdKey = longPreferencesKey("selected_habit_id")
    private val themeModeKey = stringPreferencesKey("app_theme_mode")
    private val analyticsRangeKey = stringPreferencesKey("analytics_range")
    private val analyticsHabitIdKey = longPreferencesKey("analytics_habit_id")
    private val analyticsHeatmapScrollKey = intPreferencesKey("analytics_heatmap_scroll")
    private val analyticsHabitFilterScrollKey = intPreferencesKey("analytics_habit_filter_scroll")

    val layoutMode: Flow<HomeLayoutMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[layoutModeKey]
        raw?.let { value ->
            runCatching { HomeLayoutMode.valueOf(value) }.getOrDefault(HomeLayoutMode.Auto)
        } ?: HomeLayoutMode.Auto
    }

    val selectedTopLevelRoute: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[selectedTopLevelRouteKey] ?: DEFAULT_TOP_LEVEL_ROUTE
    }

    val selectedHabitId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[selectedHabitIdKey]
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[themeModeKey]
        raw?.let { value ->
            runCatching { ThemeMode.valueOf(value) }.getOrDefault(ThemeMode.System)
        } ?: ThemeMode.System
    }

    val analyticsRange: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[analyticsRangeKey] ?: DEFAULT_ANALYTICS_RANGE
    }

    val analyticsHabitId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[analyticsHabitIdKey]
    }

    val analyticsHeatmapScroll: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[analyticsHeatmapScrollKey] ?: 0
    }

    val analyticsHabitFilterScroll: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[analyticsHabitFilterScrollKey] ?: 0
    }

    suspend fun setLayoutMode(mode: HomeLayoutMode) {
        context.dataStore.edit { prefs ->
            prefs[layoutModeKey] = mode.name
        }
    }

    suspend fun setSelectedTopLevelRoute(route: String) {
        context.dataStore.edit { prefs ->
            prefs[selectedTopLevelRouteKey] = route
        }
    }

    suspend fun setSelectedHabitId(habitId: Long?) {
        context.dataStore.edit { prefs ->
            if (habitId == null) {
                prefs.remove(selectedHabitIdKey)
            } else {
                prefs[selectedHabitIdKey] = habitId
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    suspend fun setAnalyticsRange(range: String) {
        context.dataStore.edit { prefs ->
            prefs[analyticsRangeKey] = range
        }
    }

    suspend fun setAnalyticsHabitId(habitId: Long?) {
        context.dataStore.edit { prefs ->
            if (habitId == null) {
                prefs.remove(analyticsHabitIdKey)
            } else {
                prefs[analyticsHabitIdKey] = habitId
            }
        }
    }

    suspend fun setAnalyticsHeatmapScroll(offset: Int) {
        context.dataStore.edit { prefs ->
            prefs[analyticsHeatmapScrollKey] = offset.coerceAtLeast(0)
        }
    }

    suspend fun setAnalyticsHabitFilterScroll(offset: Int) {
        context.dataStore.edit { prefs ->
            prefs[analyticsHabitFilterScrollKey] = offset.coerceAtLeast(0)
        }
    }

    private companion object {
        const val DEFAULT_TOP_LEVEL_ROUTE = "habits"
        const val DEFAULT_ANALYTICS_RANGE = "30D"
    }
}
