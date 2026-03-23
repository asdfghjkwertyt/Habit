package com.example.habittracker.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.habittracker.ui.screens.CalendarScreen
import com.example.habittracker.ui.screens.HomeScreen
import com.example.habittracker.ui.screens.HabitFormScreen
import com.example.habittracker.ui.screens.QuestScreen
import com.example.habittracker.ui.screens.StatsScreen
import com.example.habittracker.ui.theme.HabitTrackerTheme
import com.example.habittracker.ui.viewmodel.HabitViewModel
import com.example.habittracker.util.DateUtils
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.example.habittracker.ui.components.AppBackground
import androidx.compose.ui.unit.dp

@Composable
fun HabitNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val viewModel: HabitViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var restoredPreferredRoute by rememberSaveable { mutableStateOf(false) }

    val topLevelRoutes = listOf(NavRoutes.Habits, NavRoutes.Calendar, NavRoutes.Stats, NavRoutes.Quest)
    val topLevelRouteStrings = topLevelRoutes.map { it.route }.toSet()

    LaunchedEffect(state.preferredTopLevelRoute, restoredPreferredRoute) {
        if (!restoredPreferredRoute) {
            val targetRoute = state.preferredTopLevelRoute
            if (targetRoute in topLevelRouteStrings && targetRoute != NavRoutes.Habits.route) {
                navController.navigate(targetRoute) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
            restoredPreferredRoute = true
        }
    }

    HabitTrackerTheme(themeMode = state.themeMode) {
        AppBackground {
            Scaffold(
                modifier = modifier,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    val backStack by navController.currentBackStackEntryAsState()
                    val currentDestination = backStack?.destination
                    val showBottomBar = currentDestination
                        ?.hierarchy
                        ?.any { destination -> destination.route in topLevelRouteStrings }
                        ?: false

                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            tonalElevation = 0.dp
                        ) {
                            topLevelRoutes.forEach { route ->
                                val selected = currentDestination?.hierarchy?.any { it.route == route.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        viewModel.setPreferredTopLevelRoute(route.route)
                                        navController.navigate(route.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        val icon = when (route) {
                                            NavRoutes.Habits -> Icons.AutoMirrored.Filled.List
                                            NavRoutes.Calendar -> Icons.Default.CalendarMonth
                                            NavRoutes.Stats -> Icons.Default.QueryStats
                                            NavRoutes.Quest -> Icons.Default.EmojiEvents
                                            else -> Icons.AutoMirrored.Filled.List
                                        }
                                        Icon(icon, contentDescription = route.route)
                                    },
                                    label = {
                                        Text(
                                            when (route) {
                                                NavRoutes.Habits -> "Habits"
                                                NavRoutes.Calendar -> "Calendar"
                                                NavRoutes.Stats -> "Stats"
                                                NavRoutes.Quest -> "Quest"
                                                else -> "Habits"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.Habits.route,
                    modifier = Modifier.padding(padding)
                ) {
            composable(NavRoutes.Habits.route) {
                HomeScreen(
                    state = state,
                    onAddHabit = {
                        navController.navigate(NavRoutes.AddHabit.route) {
                            launchSingleTop = true
                        }
                    },
                    onEditHabit = { habitId ->
                        navController.navigate(NavRoutes.EditHabit.create(habitId)) {
                            launchSingleTop = true
                        }
                    },
                    onToggleHabitCompletion = { habitId, completed ->
                        if (completed) {
                            viewModel.setCompletion(habitId, DateUtils.todayEpochDay(), true)
                        } else {
                            viewModel.toggleHabitCompletion(habitId, DateUtils.todayEpochDay())
                        }
                    },
                    onToggleHabitReminder = viewModel::setHabitReminderEnabled,
                    onLayoutModeChange = viewModel::setHomeLayoutMode,
                    onThemeModeChange = viewModel::setThemeMode,
                    onRefresh = viewModel::loadHabits,
                    onRefreshReminderDiagnostics = viewModel::refreshReminderDiagnostics,
                    onDismissLevelUpMessage = viewModel::clearLevelUpMessage,
                    onRetry = viewModel::loadHabits
                )
            }
            composable(NavRoutes.Calendar.route) {
                CalendarScreen(
                    state = state,
                    onSelectHabit = viewModel::setSelectedHabit,
                    onToggle = viewModel::setCompletion,
                    onSaveNote = viewModel::setHabitNote,
                    onRefresh = viewModel::loadHabits,
                    onRetry = viewModel::loadHabits
                )
            }
            composable(NavRoutes.Stats.route) {
                StatsScreen(
                    state = state,
                    onRetry = viewModel::loadHabits,
                    onSelectRange = viewModel::setAnalyticsRange,
                    onSelectHabit = viewModel::setAnalyticsHabit,
                    onHeatmapScrollChanged = viewModel::setAnalyticsHeatmapScroll,
                    onHabitFilterScrollChanged = viewModel::setAnalyticsHabitFilterScroll,
                    onAdjustGoalTarget = viewModel::adjustGoalTarget,
                    onAddCustomGoal = viewModel::addCustomGoal
                )
            }
            composable(NavRoutes.Quest.route) {
                QuestScreen(
                    state = state,
                    onRetry = viewModel::loadHabits,
                    onOpenHabit = { habitId ->
                        navController.navigate(NavRoutes.EditHabit.create(habitId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(NavRoutes.AddHabit.route) {
                HabitFormScreen(
                    existing = null,
                    onSave = viewModel::addHabit,
                    onUpdate = viewModel::updateHabit,
                    onTestReminder = viewModel::sendTestReminder,
                    onBack = {
                        if (!navController.navigateUp()) {
                            navController.navigate(NavRoutes.Habits.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(
                route = NavRoutes.EditHabit.route,
                arguments = listOf(navArgument("habitId") { type = NavType.LongType })
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getLong("habitId")
                val existing = state.habits.firstOrNull { it.id == habitId }
                HabitFormScreen(
                    existing = existing,
                    onSave = viewModel::addHabit,
                    onUpdate = viewModel::updateHabit,
                    onTestReminder = viewModel::sendTestReminder,
                    onBack = {
                        if (!navController.navigateUp()) {
                            navController.navigate(NavRoutes.Habits.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}
}
}
