package com.example.habittracker.ui.navigation

sealed class NavRoutes(val route: String) {
    data object Habits : NavRoutes("habits")
    data object Calendar : NavRoutes("calendar")
    data object Stats : NavRoutes("stats")
    data object Quest : NavRoutes("quest")
    data object AddHabit : NavRoutes("add_habit")
    data object EditHabit : NavRoutes("edit_habit/{habitId}") {
        fun create(habitId: Long): String = "edit_habit/$habitId"
    }
}
