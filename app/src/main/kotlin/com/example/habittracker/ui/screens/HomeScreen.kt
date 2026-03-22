package com.example.habittracker.ui.screens

import android.graphics.Color.parseColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitWithStats
import com.example.habittracker.ui.theme.ThemeMode
import com.example.habittracker.ui.viewmodel.HomeLayoutMode
import com.example.habittracker.ui.viewmodel.HabitUiState
import com.example.habittracker.ui.viewmodel.HabitUiStatus
import com.example.habittracker.util.DateUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: HabitUiState,
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onToggleHabitCompletion: (Long, Boolean) -> Unit,
    onToggleHabitReminder: (Long, Boolean) -> Unit,
    onLayoutModeChange: (HomeLayoutMode) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRefresh: () -> Unit,
    onRefreshReminderDiagnostics: () -> Unit,
    onDismissLevelUpMessage: () -> Unit,
    onRetry: () -> Unit
) {
    val today = DateUtils.todayEpochDay()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh
    )

    LaunchedEffect(state.levelUpMessage) {
        if (state.levelUpMessage != null) {
            delay(3600)
            onDismissLevelUpMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Home")
                        Text(
                            text = "${state.homeLayoutMode.name} layout · ${state.themeMode.name} theme",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onThemeModeChange(nextThemeMode(state.themeMode)) }) {
                        val icon = when (state.themeMode) {
                            ThemeMode.System -> Icons.Default.Contrast
                            ThemeMode.Light -> Icons.Default.Brightness7
                            ThemeMode.Dark -> Icons.Default.Brightness4
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Change theme mode"
                        )
                    }
                    IconButton(onClick = { onLayoutModeChange(nextLayoutMode(state.homeLayoutMode)) }) {
                        val icon = when (state.homeLayoutMode) {
                            HomeLayoutMode.Auto -> Icons.Default.Tune
                            HomeLayoutMode.List -> Icons.AutoMirrored.Filled.ViewList
                            HomeLayoutMode.Grid -> Icons.Default.ViewModule
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Change layout mode"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHabit) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add habit")
            }
        }
    ) { padding ->
        when (state.status) {
            HabitUiStatus.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text("Loading habits...", modifier = Modifier.padding(top = 12.dp))
                }
            }

            HabitUiStatus.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Could not load habits", style = MaterialTheme.typography.headlineSmall)
                    Text(state.errorMessage ?: "Unknown error", modifier = Modifier.padding(top = 6.dp))
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Retry")
                    }
                }
            }

            HabitUiStatus.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .pullRefresh(pullRefreshState)
                ) {
                    if (state.habits.isEmpty()) {
                        EmptyHomeState(modifier = Modifier.fillMaxSize())
                    } else {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            AnimatedVisibility(
                                visible = state.levelUpMessage != null,
                                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                                exit = fadeOut() + scaleOut(targetScale = 0.92f)
                            ) {
                                LevelUpCelebrationCard(
                                    message = state.levelUpMessage.orEmpty(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (state.levelUpMessage != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            GamificationCard(
                                state = state,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            ReminderDiagnosticsCard(
                                state = state,
                                onRefreshReminderDiagnostics = onRefreshReminderDiagnostics,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                val useGrid = when (state.homeLayoutMode) {
                                    HomeLayoutMode.Auto -> maxWidth >= 600.dp
                                    HomeLayoutMode.List -> false
                                    HomeLayoutMode.Grid -> true
                                }

                                if (useGrid) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 280.dp),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(0.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(state.habits, key = { it.id }) { habit ->
                                            val stat = state.stats.firstOrNull { it.habit.id == habit.id }
                                            HabitHomeItem(
                                                habit = habit,
                                                stats = stat,
                                                isCompletedToday = stat?.completedDays?.contains(today) == true,
                                                onReminderEnabledChange = { enabled ->
                                                    onToggleHabitReminder(habit.id, enabled)
                                                },
                                                onReminderTimeClick = {
                                                    onEditHabit(habit.id)
                                                },
                                                onEdit = { onEditHabit(habit.id) },
                                                onCheckedChange = { checked ->
                                                    onToggleHabitCompletion(habit.id, checked)
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(0.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(state.habits, key = { it.id }) { habit ->
                                            val stat = state.stats.firstOrNull { it.habit.id == habit.id }
                                            HabitHomeItem(
                                                habit = habit,
                                                stats = stat,
                                                isCompletedToday = stat?.completedDays?.contains(today) == true,
                                                onReminderEnabledChange = { enabled ->
                                                    onToggleHabitReminder(habit.id, enabled)
                                                },
                                                onReminderTimeClick = {
                                                    onEditHabit(habit.id)
                                                },
                                                onEdit = { onEditHabit(habit.id) },
                                                onCheckedChange = { checked ->
                                                    onToggleHabitCompletion(habit.id, checked)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PullRefreshIndicator(
                        refreshing = state.isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelUpCelebrationCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun GamificationCard(
    state: HabitUiState,
    modifier: Modifier = Modifier
) {
    val game = state.gamification
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Your Progress", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Level ${game.levelIndex}: ${game.levelName}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${game.totalXp} XP total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            LinearProgressIndicator(
                progress = { game.levelProgress },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (game.xpToNextLevel == 0) {
                    "Max level reached. Keep the streak alive!"
                } else {
                    "${game.xpToNextLevel} XP to next level"
                },
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.small) {
                    Text(
                        text = "+${game.streakBonusXp} streak XP",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.small) {
                    Text(
                        text = "-${game.penaltyXp} miss penalty",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (game.dailyQuestBonusXp > 0) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.small) {
                        Text(
                            text = "+${game.dailyQuestBonusXp} quest XP",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (game.weeklyQuestBonusXp > 0) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.small) {
                        Text(
                            text = "+${game.weeklyQuestBonusXp} weekly chain XP",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Text(
                text = if (game.missedHabits == 0) {
                    "Perfect consistency in the recent window."
                } else {
                    "${game.missedHabits} missed check-ins recently. Complete today to recover momentum."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            HorizontalDivider()

            val quest = state.dailyQuest
            Text(quest.title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = quest.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            LinearProgressIndicator(
                progress = {
                    if (quest.target <= 0) 0f
                    else (quest.progress.toFloat() / quest.target.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (quest.completed) {
                    "Quest complete. +${quest.rewardXp} XP awarded today"
                } else {
                    "${quest.progress}/${quest.target} progress · Reward ${quest.rewardXp} XP"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            val weekly = state.weeklyQuestChain
            Text("Weekly Quest Chain", style = MaterialTheme.typography.titleSmall)
            LinearProgressIndicator(
                progress = {
                    if (weekly.targetDays <= 0) 0f
                    else (weekly.completedDaysThisWeek.toFloat() / weekly.targetDays.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (weekly.completed) {
                    "Weekly chain complete. +${weekly.rewardXp} XP bonus unlocked"
                } else {
                    "${weekly.completedDaysThisWeek}/${weekly.targetDays} quest days this week · ${weekly.currentStreakDays} day streak"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderDiagnosticsCard(
    state: HabitUiState,
    onRefreshReminderDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reminder Diagnostics", style = MaterialTheme.typography.titleSmall)
                Button(onClick = onRefreshReminderDiagnostics) {
                    Text(if (state.reminderDiagnosticsLoading) "Refreshing..." else "Refresh")
                }
            }

            if (state.reminderDiagnostics.isEmpty()) {
                Text(
                    text = "No diagnostics yet. Tap Refresh.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.reminderDiagnostics.forEachIndexed { index, item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${item.habitName} (#${item.habitId})",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "Enabled: ${item.reminderEnabled} · Time: ${item.reminderTimeLabel}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Work: ${item.uniqueWorkName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item.workStates.ifEmpty { listOf("NONE") }.forEach { stateLabel ->
                                WorkStateBadge(stateLabel)
                            }
                        }
                        Text(
                            text = "Attempts: ${item.runAttemptCounts.ifEmpty { listOf(0) }.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index < state.reminderDiagnostics.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkStateBadge(state: String) {
    val normalized = state.uppercase()
    val background = when (normalized) {
        "ENQUEUED" -> Color(0xFFE6F4EA)
        "RUNNING" -> Color(0xFFE8F0FE)
        "SUCCEEDED" -> Color(0xFFE6FFFA)
        "FAILED" -> Color(0xFFFFEBEE)
        "CANCELLED" -> Color(0xFFF3E5F5)
        "BLOCKED" -> Color(0xFFFFF4E5)
        else -> Color(0xFFECEFF1)
    }
    val content = when (normalized) {
        "ENQUEUED" -> Color(0xFF1B5E20)
        "RUNNING" -> Color(0xFF0D47A1)
        "SUCCEEDED" -> Color(0xFF004D40)
        "FAILED" -> Color(0xFFB71C1C)
        "CANCELLED" -> Color(0xFF4A148C)
        "BLOCKED" -> Color(0xFFE65100)
        else -> Color(0xFF37474F)
    }

    Surface(
        color = background,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = normalized,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun nextLayoutMode(current: HomeLayoutMode): HomeLayoutMode {
    return when (current) {
        HomeLayoutMode.Auto -> HomeLayoutMode.List
        HomeLayoutMode.List -> HomeLayoutMode.Grid
        HomeLayoutMode.Grid -> HomeLayoutMode.Auto
    }
}

private fun nextThemeMode(current: ThemeMode): ThemeMode {
    return when (current) {
        ThemeMode.System -> ThemeMode.Light
        ThemeMode.Light -> ThemeMode.Dark
        ThemeMode.Dark -> ThemeMode.System
    }
}

@Composable
private fun EmptyHomeState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No habits yet", style = MaterialTheme.typography.headlineSmall)
        Text("Tap + to add your first habit", modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun HabitHomeItem(
    habit: Habit,
    stats: HabitWithStats?,
    isCompletedToday: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeClick: () -> Unit,
    onEdit: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    val streakCount = stats?.currentStreak ?: 0
    val accentColor = runCatching { Color(parseColor(habit.colorHex)) }
        .getOrElse { MaterialTheme.colorScheme.primary }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = accentColor.copy(alpha = 0.18f)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(accentColor)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                if (habit.description.isNotBlank()) {
                    Text(habit.description, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "Streak: $streakCount day${if (streakCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (habit.reminderEnabled) "Reminder ON" else "Reminder OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = habit.reminderEnabled,
                        onCheckedChange = onReminderEnabledChange
                    )
                    Text(
                        text = formatReminderTimeLabel(habit),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onReminderTimeClick)
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit habit"
                )
            }

            Checkbox(
                checked = isCompletedToday,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

private fun formatReminderTimeLabel(habit: Habit): String {
    val hour = habit.reminderHour
    val minute = habit.reminderMinute
    return if (hour != null && minute != null) {
        String.format(Locale.US, "%02d:%02d", hour, minute)
    } else {
        "Set time"
    }
}
