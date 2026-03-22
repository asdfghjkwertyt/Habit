package com.example.habittracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.habittracker.ui.components.HabitCard
import com.example.habittracker.ui.viewmodel.HabitUiStatus
import com.example.habittracker.ui.viewmodel.HabitUiState
import com.example.habittracker.util.DateUtils
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HabitListScreen(
    state: HabitUiState,
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    onToggleToday: (Long, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit
) {
    val today = DateUtils.todayEpochDay()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Habits")
                        state.lastRefreshedAtMillis?.let { refreshedAt ->
                            Text(
                                text = "Updated ${formatRelativeRefreshed(refreshedAt, state.currentTimeMillis)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHabit) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
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
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Text("Loading habits...", modifier = Modifier.padding(top = 16.dp))
                }
            }

            HabitUiStatus.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Something went wrong", style = MaterialTheme.typography.headlineSmall)
                    Text(state.errorMessage ?: "Unable to load habits")
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Retry")
                    }
                }
            }

            HabitUiStatus.Success -> {
                if (state.habits.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No habits yet", style = MaterialTheme.typography.headlineSmall)
                        Text("Tap + to create your first habit.")
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .pullRefresh(pullRefreshState)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.habits, key = { it.id }) { habit ->
                                val stat = state.stats.firstOrNull { it.habit.id == habit.id }
                                HabitCard(
                                    habit = habit,
                                    stats = stat,
                                    isCompletedToday = stat?.completedDays?.contains(today) == true,
                                    onToggleToday = { done -> onToggleToday(habit.id, done) },
                                    onEdit = { onEditHabit(habit.id) },
                                    onDelete = { onDeleteHabit(habit.id) }
                                )
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
}

private fun formatRelativeRefreshed(epochMillis: Long, nowMillis: Long): String {
    val now = Instant.ofEpochMilli(nowMillis)
    val then = Instant.ofEpochMilli(epochMillis)
    val duration = Duration.between(then, now).coerceAtLeast(Duration.ZERO)

    val seconds = duration.seconds
    return when {
        seconds < 10 -> "just now"
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86_400}d ago"
    }
}
