package com.example.habittracker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.habittracker.domain.model.HabitCompletion
import com.example.habittracker.domain.model.AchievementCategory
import com.example.habittracker.domain.model.AchievementProgress
import com.example.habittracker.util.DateUtils
import com.example.habittracker.ui.viewmodel.HabitUiStatus
import com.example.habittracker.ui.viewmodel.HabitUiState
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: HabitUiState,
    onRetry: () -> Unit,
    onSelectRange: (String) -> Unit,
    onSelectHabit: (Long?) -> Unit,
    onHeatmapScrollChanged: (Int) -> Unit,
    onHabitFilterScrollChanged: (Int) -> Unit,
    onAdjustGoalTarget: (String, Int) -> Unit,
    onAddCustomGoal: (String, Int, String) -> Unit
) {
    val completedEntries = state.completions.filter { it.completed }
    val selectedRange = AnalyticsRange.fromKey(state.analyticsRange)
    val selectedHabitId = state.analyticsHabitId

    val validSelectedHabitId = selectedHabitId?.takeIf { selectedId ->
        state.habits.any { it.id == selectedId }
    }

    val filteredCompletions = if (validSelectedHabitId == null) {
        completedEntries
    } else {
        completedEntries.filter { it.habitId == validSelectedHabitId }
    }

    val filteredStats = if (validSelectedHabitId == null) {
        state.stats
    } else {
        state.stats.filter { it.habit.id == validSelectedHabitId }
    }

    val rangeStartEpoch = DateUtils.todayEpochDay() - (selectedRange.days - 1)
    val windowCompletions = filteredCompletions.filter { it.epochDay in rangeStartEpoch..DateUtils.todayEpochDay() }

    val totalCompleted = windowCompletions.count()
    val currentStreak = filteredStats.maxOfOrNull { it.currentStreak } ?: 0
    val longestStreak = filteredStats.maxOfOrNull { it.longestStreak } ?: 0
    val weeklySeries = weeklySeries(filteredCompletions)
    val monthlySeries = monthlySeries(filteredCompletions)
    val heatmapSeries = heatmapSeries(filteredCompletions, weeks = selectedRange.heatmapWeeks)
    val completionRate = overallCompletionRatePercent(
        completions = filteredCompletions,
        habitsCount = if (validSelectedHabitId == null) state.habits.size else 1,
        daysWindow = selectedRange.days
    )

    val chartProgress by animateFloatAsState(
        targetValue = if (state.status == HabitUiStatus.Success) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "stats-chart-progress"
    )

    val filterTransition = remember { Animatable(1f) }
    LaunchedEffect(selectedRange, validSelectedHabitId) {
        filterTransition.snapTo(0f)
        filterTransition.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }
    val animatedDashboardProgress = chartProgress * filterTransition.value

    val heatmapScrollState = rememberScrollState(initial = state.analyticsHeatmapScroll)
    val habitFilterScrollState = rememberScrollState(initial = state.analyticsHabitFilterScroll)

    LaunchedEffect(heatmapScrollState) {
        snapshotFlow { heatmapScrollState.value }
            .distinctUntilChanged()
            .collect { offset -> onHeatmapScrollChanged(offset) }
    }

    LaunchedEffect(habitFilterScrollState) {
        snapshotFlow { habitFilterScrollState.value }
            .distinctUntilChanged()
            .collect { offset -> onHabitFilterScrollChanged(offset) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Analytics")
                        state.lastRefreshedAtMillis?.let { refreshedAt ->
                            Text(
                                text = "Updated ${formatRelativeRefreshed(refreshedAt, state.currentTimeMillis)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            )
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
                    Text("Loading stats...", modifier = Modifier.padding(top = 16.dp))
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
                    Text("Unable to load statistics", style = MaterialTheme.typography.headlineSmall)
                    Text(state.errorMessage ?: "Unknown error")
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Retry")
                    }
                }
            }

            HabitUiStatus.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        AnalyticsFiltersCard(
                            habits = state.habits.map { it.id to it.name },
                            selectedHabitId = validSelectedHabitId,
                            onHabitSelected = onSelectHabit,
                            selectedRange = selectedRange,
                            onRangeSelected = { onSelectRange(it.key) },
                            scrollState = habitFilterScrollState
                        )
                    }

                    item {
                        AnalyticsHeroCard(
                            totalCompleted = totalCompleted,
                            completionRate = completionRate,
                            habitsCount = if (validSelectedHabitId == null) state.habits.size else 1
                        )
                    }

                    item {
                        DashboardCard(
                            title = "Habit Insights",
                            subtitle = "Behavior patterns detected from your habit logs"
                        ) {
                            HabitInsightsCard(insights = state.habitInsights)
                        }
                    }

                    item {
                        DashboardCard(
                            title = "XP & Level Progress",
                            subtitle = "Earn XP through completions and streak consistency"
                        ) {
                            GamificationMomentumCard(
                                totalXp = state.gamification.totalXp,
                                levelName = state.gamification.levelName,
                                levelIndex = state.gamification.levelIndex,
                                levelProgress = state.gamification.levelProgress,
                                xpToNextLevel = state.gamification.xpToNextLevel,
                                streakBonusXp = state.gamification.streakBonusXp,
                                penaltyXp = state.gamification.penaltyXp,
                                missedHabits = state.gamification.missedHabits,
                                weeklyQuestBonusXp = state.gamification.weeklyQuestBonusXp,
                                dailyQuestLabel = state.dailyQuest.title,
                                dailyQuestProgress = state.dailyQuest.progress,
                                dailyQuestTarget = state.dailyQuest.target,
                                dailyQuestRewardXp = state.dailyQuest.rewardXp,
                                dailyQuestCompleted = state.dailyQuest.completed
                            )
                        }
                    }

                    item {
                        AnimatedContent(
                            targetState = StatsKpiState(
                                completionRate = completionRate,
                                totalCompleted = totalCompleted,
                                currentStreak = currentStreak,
                                longestStreak = longestStreak,
                                habitId = validSelectedHabitId,
                                range = selectedRange
                            ),
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(280)) + slideInVertically(animationSpec = tween(280)) { it / 8 })
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(220)) + slideOutVertically(animationSpec = tween(220)) { -it / 10 }
                                    )
                            },
                            label = "stats-kpi-transition"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MetricCard(
                                        title = "Completion Rate",
                                        value = "${it.completionRate}%",
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricCard(
                                        title = "${it.range.label} Volume",
                                        value = it.totalCompleted.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MetricCard(
                                        title = "Current Streak",
                                        value = it.currentStreak.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricCard(
                                        title = "Longest Streak",
                                        value = it.longestStreak.toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        DashboardCard(
                            title = "Weekly Habit Trend",
                            subtitle = "Last 7 days completion counts",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WeeklyBarChart(points = weeklySeries, animationProgress = animatedDashboardProgress)
                        }
                    }

                    item {
                        DashboardCard(
                            title = "Monthly Trend",
                            subtitle = "Six-month completion trajectory",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MonthlyLineChart(points = monthlySeries, animationProgress = animatedDashboardProgress)
                        }
                    }

                    item {
                        DashboardCard(
                            title = "Streak Comparison",
                            subtitle = "Current momentum versus best run"
                        ) {
                            StreakComparison(
                                currentStreak = currentStreak,
                                longestStreak = longestStreak,
                                animationProgress = animatedDashboardProgress
                            )
                        }
                    }

                    item {
                        DashboardCard(
                            title = "Activity Heatmap",
                            subtitle = "GitHub-style daily completion density (${selectedRange.label})"
                        ) {
                            HabitHeatmap(
                                cells = heatmapSeries,
                                animationProgress = animatedDashboardProgress,
                                scrollState = heatmapScrollState
                            )
                        }
                    }

                    item {
                        DashboardCard(
                            title = "Quest History & Weekly Chain",
                            subtitle = "Track your recent quest completions and weekly challenge streak"
                        ) {
                            QuestHistoryCard(
                                entries = state.questHistory,
                                completedDaysThisWeek = state.weeklyQuestChain.completedDaysThisWeek,
                                targetDays = state.weeklyQuestChain.targetDays,
                                currentStreakDays = state.weeklyQuestChain.currentStreakDays,
                                weeklyCompleted = state.weeklyQuestChain.completed,
                                rewardXp = state.weeklyQuestChain.rewardXp
                            )
                        }
                    }

                    item {
                        DashboardCard(
                            title = "Goals & Challenges",
                            subtitle = "Track targets and unlock achievement badges"
                        ) {
                            GoalsChallengesSection(
                                achievements = state.achievementProgress,
                                animationProgress = animatedDashboardProgress,
                                onAdjustGoalTarget = onAdjustGoalTarget,
                                onAddCustomGoal = onAddCustomGoal
                            )
                        }
                    }

                    item {
                        Text("Habit Insights", style = MaterialTheme.typography.titleLarge)
                    }

                    items(filteredStats.sortedByDescending { it.completionRate }.take(5)) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(item.habit.name, style = MaterialTheme.typography.titleLarge)
                                HorizontalDivider()
                                Text("Current streak: ${item.currentStreak}")
                                Text("Longest streak: ${item.longestStreak}")
                                Text("Completion rate: ${(item.completionRate * 100f).roundToInt()}%")
                                Text("Completed days: ${item.completedDays.size}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsChallengesSection(
    achievements: List<AchievementProgress>,
    animationProgress: Float,
    onAdjustGoalTarget: (String, Int) -> Unit,
    onAddCustomGoal: (String, Int, String) -> Unit
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var customTitle by rememberSaveable { mutableStateOf("") }
    var customTargetInput by rememberSaveable { mutableStateOf("7") }
    var customMetric by rememberSaveable { mutableStateOf("COMPLETION") }
    var inputError by rememberSaveable { mutableStateOf<String?>(null) }

    val goals = achievements.filter { it.category == AchievementCategory.Goal }
    val challenges = achievements.filter { it.category == AchievementCategory.Challenge }
    val badges = achievements.filter { it.category == AchievementCategory.Badge }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (goals.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Goals", style = MaterialTheme.typography.titleSmall)
                AssistChip(
                    onClick = { showCreateDialog = true },
                    label = { Text("New Goal") }
                )
            }
            goals.forEach { item ->
                AchievementProgressRow(
                    item = item,
                    animationProgress = animationProgress,
                    onAdjustTarget = { delta -> onAdjustGoalTarget(item.key, delta) }
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Goals", style = MaterialTheme.typography.titleSmall)
                AssistChip(
                    onClick = { showCreateDialog = true },
                    label = { Text("Create Goal") }
                )
            }
            Text(
                "No goals yet. Create one to start tracking a custom target.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (challenges.isNotEmpty()) {
            Text("Weekly Challenges", style = MaterialTheme.typography.titleSmall)
            challenges.forEach { item ->
                AchievementProgressRow(item = item, animationProgress = animationProgress)
            }
        }

        if (badges.isNotEmpty()) {
            Text("Achievement Badges", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                badges.forEach { badge ->
                    AchievementBadge(badge)
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                inputError = null
            },
            title = { Text("Create Custom Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = {
                            customTitle = it
                            inputError = null
                        },
                        label = { Text("Goal title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customTargetInput,
                        onValueChange = {
                            customTargetInput = it.filter(Char::isDigit)
                            inputError = null
                        },
                        label = { Text("Target") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Metric", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = customMetric == "COMPLETION",
                            onClick = { customMetric = "COMPLETION" },
                            label = { Text("Completions") }
                        )
                        FilterChip(
                            selected = customMetric == "STREAK",
                            onClick = { customMetric = "STREAK" },
                            label = { Text("Streak") }
                        )
                    }

                    if (inputError != null) {
                        Text(
                            text = inputError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsedTarget = customTargetInput.toIntOrNull()
                        if (parsedTarget == null || parsedTarget < 1) {
                            inputError = "Target must be at least 1"
                        } else {
                            onAddCustomGoal(customTitle, parsedTarget, customMetric)
                            customTitle = ""
                            customTargetInput = "7"
                            customMetric = "COMPLETION"
                            inputError = null
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        inputError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AchievementProgressRow(
    item: AchievementProgress,
    animationProgress: Float,
    onAdjustTarget: ((Int) -> Unit)? = null
) {
    val ratio = if (item.target <= 0) 0f else item.progress.toFloat() / item.target.toFloat()
    val animatedRatio by animateFloatAsState(
        targetValue = (ratio.coerceIn(0f, 1f)) * animationProgress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "achievement-progress-${item.key}"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.title, style = MaterialTheme.typography.labelLarge)
                if (onAdjustTarget != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = { onAdjustTarget(-1) }, label = { Text("-1") })
                        AssistChip(onClick = { onAdjustTarget(+1) }, label = { Text("+1") })
                    }
                }
            }
            Text("${item.progress}/${item.target}", style = MaterialTheme.typography.labelMedium)
        }
        Text(
            item.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { animatedRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
    }
}

@Composable
private fun AchievementBadge(item: AchievementProgress) {
    val pulse = if (item.achieved) {
        val infinite = rememberInfiniteTransition(label = "badge-pulse-${item.key}")
        infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "badge-scale-${item.key}"
        ).value
    } else {
        1f
    }

    val containerColor = if (item.achieved) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.graphicsLayer {
            scaleX = pulse
            scaleY = pulse
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(if (item.achieved) "🏆" else "🔒")
            Text(
                text = item.title.removePrefix("Badge: "),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnalyticsFiltersCard(
    habits: List<Pair<Long, String>>,
    selectedHabitId: Long?,
    onHabitSelected: (Long?) -> Unit,
    selectedRange: AnalyticsRange,
    onRangeSelected: (AnalyticsRange) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState
) {
    DashboardCard(
        title = "Filters",
        subtitle = "Drill down analytics by habit and time window"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Time Range", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsRange.entries.forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { onRangeSelected(range) },
                        label = { Text(range.label) }
                    )
                }
            }

            Text("Habit", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedHabitId == null,
                    onClick = { onHabitSelected(null) },
                    label = { Text("All Habits") }
                )

                habits.forEach { (habitId, habitName) ->
                    FilterChip(
                        selected = selectedHabitId == habitId,
                        onClick = { onHabitSelected(habitId) },
                        label = { Text(habitName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsHeroCard(totalCompleted: Int, completionRate: Int, habitsCount: Int) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.90f)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Performance Overview",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track consistency, streak performance, and daily intensity across your habits.",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("$completionRate% completion") })
                    AssistChip(onClick = {}, label = { Text("$totalCompleted completions") })
                    AssistChip(onClick = {}, label = { Text("$habitsCount habits") })
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun HabitInsightsCard(insights: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        insights.ifEmpty { listOf("Keep logging habits to unlock personalized insights.") }
            .forEach { insight ->
                Text(
                    text = "• $insight",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
    }
}

@Composable
private fun GamificationMomentumCard(
    totalXp: Int,
    levelName: String,
    levelIndex: Int,
    levelProgress: Float,
    xpToNextLevel: Int,
    streakBonusXp: Int,
    penaltyXp: Int,
    missedHabits: Int,
    weeklyQuestBonusXp: Int,
    dailyQuestLabel: String,
    dailyQuestProgress: Int,
    dailyQuestTarget: Int,
    dailyQuestRewardXp: Int,
    dailyQuestCompleted: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Level $levelIndex: $levelName",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$totalXp XP total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        LinearProgressIndicator(
            progress = { levelProgress },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = if (xpToNextLevel == 0) {
                "Top tier unlocked. You are in Pro mode."
            } else {
                "$xpToNextLevel XP to next level"
            },
            style = MaterialTheme.typography.bodySmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("+${streakBonusXp} streak XP") })
            AssistChip(onClick = {}, label = { Text("-${penaltyXp} penalty XP") })
            if (weeklyQuestBonusXp > 0) {
                AssistChip(onClick = {}, label = { Text("+${weeklyQuestBonusXp} weekly chain XP") })
            }
        }

        Text(dailyQuestLabel, style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(
            progress = {
                if (dailyQuestTarget <= 0) 0f
                else (dailyQuestProgress.toFloat() / dailyQuestTarget.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = if (dailyQuestCompleted) {
                "Daily quest complete. +$dailyQuestRewardXp XP bonus"
            } else {
                "$dailyQuestProgress/$dailyQuestTarget progress · +$dailyQuestRewardXp XP reward"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = if (missedHabits == 0) {
                "No missed check-ins recently. Keep this momentum."
            } else {
                "$missedHabits missed check-ins in the recent window. Recover by completing habits today."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuestHistoryCard(
    entries: List<com.example.habittracker.ui.viewmodel.QuestHistoryEntry>,
    completedDaysThisWeek: Int,
    targetDays: Int,
    currentStreakDays: Int,
    weeklyCompleted: Boolean,
    rewardXp: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LinearProgressIndicator(
            progress = {
                if (targetDays <= 0) 0f
                else (completedDaysThisWeek.toFloat() / targetDays.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = if (weeklyCompleted) {
                "Weekly chain complete: $completedDaysThisWeek/$targetDays days · +$rewardXp XP"
            } else {
                "Weekly chain: $completedDaysThisWeek/$targetDays days · $currentStreakDays day streak"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            entries.forEach { item ->
                Surface(
                    color = if (item.completed) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(item.dayLabel, style = MaterialTheme.typography.labelSmall)
                        Text(if (item.completed) "Done" else "Miss", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun WeeklyBarChart(points: List<ProgressPoint>, animationProgress: Float) {
    val maxValue = max(1, points.maxOfOrNull { it.value } ?: 1)
    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val w = size.width
            val h = size.height
            val section = w / points.size
            val barWidth = section * 0.56f
            val baseLine = h - 14f

            drawLine(
                color = axisColor,
                start = Offset(0f, baseLine),
                end = Offset(w, baseLine),
                strokeWidth = 2f
            )

            points.forEachIndexed { index, point ->
                val ratio = point.value.toFloat() / maxValue.toFloat()
                val barHeight = ratio * (h - 36f) * animationProgress
                val left = (index * section) + ((section - barWidth) / 2f)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, baseLine - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(14f, 14f)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun MonthlyLineChart(points: List<ProgressPoint>, animationProgress: Float) {
    val maxValue = max(1, points.maxOfOrNull { it.value } ?: 1)
    val lineColor = MaterialTheme.colorScheme.tertiary
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val w = size.width
            val h = size.height
            val stepX = if (points.size > 1) w / (points.size - 1) else w
            val chartTop = 12f
            val chartBottom = h - 18f
            val chartHeight = chartBottom - chartTop

            drawLine(
                color = axisColor,
                start = Offset(0f, chartBottom),
                end = Offset(w, chartBottom),
                strokeWidth = 2f
            )

            val path = Path()
            points.forEachIndexed { index, point ->
                val ratio = point.value.toFloat() / maxValue.toFloat()
                val x = stepX * index
                val targetY = chartBottom - (ratio * chartHeight)
                val y = chartBottom - ((chartBottom - targetY) * animationProgress)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            points.forEachIndexed { index, point ->
                val ratio = point.value.toFloat() / maxValue.toFloat()
                val x = stepX * index
                val targetY = chartBottom - (ratio * chartHeight)
                val y = chartBottom - ((chartBottom - targetY) * animationProgress)
                drawCircle(
                    color = lineColor,
                    radius = 7f,
                    center = Offset(x, y)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun StreakComparison(currentStreak: Int, longestStreak: Int, animationProgress: Float) {
    val maxValue = max(1, max(currentStreak, longestStreak))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StreakBarRow(
            label = "Current",
            value = currentStreak,
            maxValue = maxValue,
            color = MaterialTheme.colorScheme.primary,
            animationProgress = animationProgress
        )
        StreakBarRow(
            label = "Longest",
            value = longestStreak,
            maxValue = maxValue,
            color = MaterialTheme.colorScheme.tertiary,
            animationProgress = animationProgress
        )
    }
}

@Composable
private fun StreakBarRow(
    label: String,
    value: Int,
    maxValue: Int,
    color: Color,
    animationProgress: Float
) {
    val ratio = if (maxValue == 0) 0f else value.toFloat() / maxValue.toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("$value", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio * animationProgress)
                    .background(color, shape = MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
private fun HabitHeatmap(
    cells: List<HeatmapCell>,
    animationProgress: Float,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val groupedByWeek = cells.groupBy { it.weekIndex }.toSortedMap()
    val maxCount = max(1, cells.maxOfOrNull { it.count } ?: 1)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(6.dp))
            repeat(5) { level ->
                val color = heatColor(level, 4)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(10.dp)
                        .background(color, shape = MaterialTheme.shapes.extraSmall)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("More", style = MaterialTheme.typography.labelSmall)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            groupedByWeek.values.forEach { weekCells ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..6).forEach { dayOfWeekIndex ->
                        val cell = weekCells.firstOrNull { it.dayOfWeekIndex == dayOfWeekIndex }
                        val intensity = if (cell == null || maxCount == 0) 0 else {
                            ((cell.count.toFloat() / maxCount.toFloat()) * 4f).roundToInt().coerceIn(0, 4)
                        }
                        val baseColor = heatColor(intensity, 4)
                        val color = baseColor.copy(alpha = 0.35f + (0.65f * animationProgress))
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, shape = MaterialTheme.shapes.extraSmall)
                        )
                    }
                }
            }
        }
    }
}

private fun heatColor(level: Int, maxLevel: Int): Color {
    if (level <= 0) return Color(0xFFE8EDF3)
    val t = level.toFloat() / maxLevel.toFloat()
    return lerpColor(Color(0xFFB7E4C7), Color(0xFF1B7F4A), t)
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}

private data class ProgressPoint(val label: String, val value: Int)
private data class HeatmapCell(
    val weekIndex: Int,
    val dayOfWeekIndex: Int,
    val count: Int
)

private enum class AnalyticsRange(
    val key: String,
    val label: String,
    val days: Int,
    val heatmapWeeks: Int
) {
    SevenDays("7D", "7D", 7, 8),
    ThirtyDays("30D", "30D", 30, 14),
    NinetyDays("90D", "90D", 90, 20);

    companion object {
        fun fromKey(key: String): AnalyticsRange {
            return entries.firstOrNull { it.key == key } ?: ThirtyDays
        }
    }
}

private data class StatsKpiState(
    val completionRate: Int,
    val totalCompleted: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val habitId: Long?,
    val range: AnalyticsRange
)

private fun weeklySeries(completions: List<HabitCompletion>): List<ProgressPoint> {
    val today = LocalDate.now()
    return (6 downTo 0).map { offset ->
        val day = today.minusDays(offset.toLong())
        val epoch = day.toEpochDay()
        val count = completions.count { it.epochDay == epoch }
        ProgressPoint(label = day.dayOfWeek.name.take(3), value = count)
    }
}

private fun monthlySeries(completions: List<HabitCompletion>): List<ProgressPoint> {
    val current = YearMonth.now()
    return (5 downTo 0).map { offset ->
        val month = current.minusMonths(offset.toLong())
        val count = completions.count { completion ->
            YearMonth.from(LocalDate.ofEpochDay(completion.epochDay)) == month
        }
        ProgressPoint(
            label = month.month.getDisplayName(TextStyle.SHORT, Locale.US),
            value = count
        )
    }
}

private fun heatmapSeries(completions: List<HabitCompletion>, weeks: Int): List<HeatmapCell> {
    val today = LocalDate.now()
    val endEpoch = today.toEpochDay()
    val startEpoch = endEpoch - (weeks * 7L) + 1L
    val completionsByDay = completions.groupingBy { it.epochDay }.eachCount()

    return (0 until weeks).flatMap { weekIndex ->
        (0..6).map { dayIndex ->
            val epoch = startEpoch + (weekIndex * 7L) + dayIndex.toLong()
            HeatmapCell(
                weekIndex = weekIndex,
                dayOfWeekIndex = dayIndex,
                count = completionsByDay[epoch] ?: 0
            )
        }
    }
}

private fun overallCompletionRatePercent(
    completions: List<HabitCompletion>,
    habitsCount: Int,
    daysWindow: Int
): Int {
    if (habitsCount <= 0 || daysWindow <= 0) return 0

    val today = DateUtils.todayEpochDay()
    val start = today - (daysWindow - 1)
    val windowCompletions = completions.count { it.epochDay in start..today }
    val denominator = habitsCount * daysWindow
    val rate = (windowCompletions.toFloat() / denominator.toFloat()) * 100f
    return rate.roundToInt().coerceIn(0, 100)
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
