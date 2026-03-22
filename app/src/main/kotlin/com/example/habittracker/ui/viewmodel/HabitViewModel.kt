package com.example.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.habittracker.R
import com.example.habittracker.data.preferences.HomeLayoutPreferences
import com.example.habittracker.domain.model.AchievementCategory
import com.example.habittracker.domain.model.AchievementProgress
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCompletion
import com.example.habittracker.domain.model.HabitFrequency
import com.example.habittracker.domain.model.HabitStreak
import com.example.habittracker.domain.model.HabitWithStats
import com.example.habittracker.domain.usecase.HabitUseCases
import com.example.habittracker.notifications.ReminderScheduler
import com.example.habittracker.ui.theme.ThemeMode
import com.example.habittracker.util.DateUtils
import java.lang.System.currentTimeMillis
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.max
import kotlin.math.roundToInt
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class HabitViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val habitUseCases: HabitUseCases,
    private val reminderScheduler: ReminderScheduler,
    private val homeLayoutPreferences: HomeLayoutPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitUiState(status = HabitUiStatus.Loading))
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()
    private var loadHabitsJob: Job? = null
    private var clockTickerJob: Job? = null
    private var layoutModeJob: Job? = null
    private var selectedRouteJob: Job? = null
    private var selectedHabitJob: Job? = null
    private var themeModeJob: Job? = null
    private var analyticsRangeJob: Job? = null
    private var analyticsHabitJob: Job? = null
    private var analyticsHeatmapScrollJob: Job? = null
    private var analyticsHabitFilterScrollJob: Job? = null
    private var achievementProgressJob: Job? = null

    init {
        startClockTicker()
        observePersistedLayoutMode()
        observePersistedTopLevelRoute()
        observePersistedSelectedHabit()
        observePersistedThemeMode()
        observePersistedAnalyticsRange()
        observePersistedAnalyticsHabit()
        observePersistedAnalyticsHeatmapScroll()
        observePersistedAnalyticsHabitFilterScroll()
        observeAchievementProgress()
        loadHabits()
    }

    private fun observeAchievementProgress() {
        achievementProgressJob?.cancel()
        achievementProgressJob = viewModelScope.launch {
            habitUseCases.getAchievementProgress().collect { progress ->
                _uiState.update { it.copy(achievementProgress = progress) }
            }
        }
    }

    private fun observePersistedAnalyticsRange() {
        analyticsRangeJob?.cancel()
        analyticsRangeJob = viewModelScope.launch {
            homeLayoutPreferences.analyticsRange.collect { range ->
                _uiState.update { it.copy(analyticsRange = range) }
            }
        }
    }

    private fun observePersistedAnalyticsHabit() {
        analyticsHabitJob?.cancel()
        analyticsHabitJob = viewModelScope.launch {
            homeLayoutPreferences.analyticsHabitId.collect { habitId ->
                _uiState.update {
                    it.copy(
                        analyticsHabitId = habitId,
                        habitInsights = calculateHabitInsights(
                            habits = it.habits,
                            stats = it.stats,
                            completions = it.completions,
                            selectedHabitId = habitId
                        )
                    )
                }
            }
        }
    }

    private fun observePersistedAnalyticsHeatmapScroll() {
        analyticsHeatmapScrollJob?.cancel()
        analyticsHeatmapScrollJob = viewModelScope.launch {
            homeLayoutPreferences.analyticsHeatmapScroll.collect { offset ->
                _uiState.update { it.copy(analyticsHeatmapScroll = offset) }
            }
        }
    }

    private fun observePersistedAnalyticsHabitFilterScroll() {
        analyticsHabitFilterScrollJob?.cancel()
        analyticsHabitFilterScrollJob = viewModelScope.launch {
            homeLayoutPreferences.analyticsHabitFilterScroll.collect { offset ->
                _uiState.update { it.copy(analyticsHabitFilterScroll = offset) }
            }
        }
    }

    private fun observePersistedThemeMode() {
        themeModeJob?.cancel()
        themeModeJob = viewModelScope.launch {
            homeLayoutPreferences.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    private fun observePersistedSelectedHabit() {
        selectedHabitJob?.cancel()
        selectedHabitJob = viewModelScope.launch {
            homeLayoutPreferences.selectedHabitId.collect { habitId ->
                _uiState.update { it.copy(selectedHabitId = habitId) }
            }
        }
    }

    private fun observePersistedTopLevelRoute() {
        selectedRouteJob?.cancel()
        selectedRouteJob = viewModelScope.launch {
            homeLayoutPreferences.selectedTopLevelRoute.collect { route ->
                _uiState.update { it.copy(preferredTopLevelRoute = route) }
            }
        }
    }

    private fun observePersistedLayoutMode() {
        layoutModeJob?.cancel()
        layoutModeJob = viewModelScope.launch {
            homeLayoutPreferences.layoutMode.collect { persistedMode ->
                _uiState.update { it.copy(homeLayoutMode = persistedMode) }
            }
        }
    }

    private fun startClockTicker() {
        clockTickerJob?.cancel()
        clockTickerJob = viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(currentTimeMillis = currentTimeMillis()) }
                delay(60_000)
            }
        }
    }

    fun loadHabits() {
        loadHabitsJob?.cancel()
        _uiState.update {
            val hasExistingContent = it.habits.isNotEmpty()
            it.copy(
                status = if (hasExistingContent) HabitUiStatus.Success else HabitUiStatus.Loading,
                isRefreshing = true,
                errorMessage = null
            )
        }

        loadHabitsJob = viewModelScope.launch {
            combine(
                habitUseCases.getHabits(),
                habitUseCases.getHabitStats(),
                habitUseCases.getAllCompletions()
            ) { habits, stats, completions ->
                Triple(habits, stats, completions)
            }.catch { throwable ->
                _uiState.update {
                    it.copy(
                        status = if (it.habits.isEmpty()) HabitUiStatus.Error else HabitUiStatus.Success,
                        errorMessage = throwable.message ?: "Failed to load habits",
                        isRefreshing = false
                    )
                }
            }.collect { (habits, stats, completions) ->
                val previousLevel = _uiState.value.gamification.levelIndex
                val currentSelected = _uiState.value.selectedHabitId
                val currentAnalyticsHabit = _uiState.value.analyticsHabitId
                val resolvedSelected = when {
                    currentSelected != null && habits.any { it.id == currentSelected } -> currentSelected
                    else -> habits.firstOrNull()?.id
                }
                val resolvedAnalyticsHabit = currentAnalyticsHabit?.takeIf { selectedId ->
                    habits.any { it.id == selectedId }
                }
                val dailyQuest = calculateDailyQuestStatus(habits, completions)
                val questHistory = buildQuestHistory(habits, completions, days = 14)
                val weeklyQuestChain = calculateWeeklyQuestChain(questHistory)
                val gamification = calculateGamificationSummary(
                    habits = habits,
                    stats = stats,
                    completions = completions,
                    dailyQuestBonusXp = if (dailyQuest.completed) dailyQuest.rewardXp else 0,
                    weeklyQuestBonusXp = if (weeklyQuestChain.completed) weeklyQuestChain.rewardXp else 0
                )

                _uiState.update {
                    it.copy(
                        habits = habits,
                        stats = stats,
                        completions = completions,
                        habitInsights = calculateHabitInsights(
                            habits = habits,
                            stats = stats,
                            completions = completions,
                            selectedHabitId = resolvedAnalyticsHabit
                        ),
                        gamification = gamification,
                        dailyQuest = dailyQuest,
                        questHistory = questHistory,
                        weeklyQuestChain = weeklyQuestChain,
                        selectedHabitId = resolvedSelected,
                        status = HabitUiStatus.Success,
                        errorMessage = null,
                        isRefreshing = false,
                        lastRefreshedAtMillis = currentTimeMillis()
                    )
                }

                if (gamification.levelIndex > previousLevel) {
                    triggerLevelUpCelebration(gamification.levelIndex, gamification.levelName)
                }

                if (currentSelected != resolvedSelected) {
                    homeLayoutPreferences.setSelectedHabitId(resolvedSelected)
                }

                if (currentAnalyticsHabit != resolvedAnalyticsHabit) {
                    homeLayoutPreferences.setAnalyticsHabitId(resolvedAnalyticsHabit)
                }

                syncAchievementProgress(habits, stats, completions)

                refreshReminderDiagnostics()
            }
        }
    }

    private suspend fun syncAchievementProgress(
        habits: List<Habit>,
        stats: List<HabitWithStats>,
        completions: List<HabitCompletion>
    ) {
        val now = currentTimeMillis()
        val today = DateUtils.todayEpochDay()
        val currentWeekStart = LocalDate.now()
            .minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
            .toEpochDay()

        val completedOnly = completions.filter { it.completed }
        val completedThisWeek = completedOnly.count { it.epochDay in currentWeekStart..today }
        val activeWeekDays = completedOnly
            .map { it.epochDay }
            .filter { it in currentWeekStart..today }
            .toSet()
            .size

        val bestCurrentStreak = stats.maxOfOrNull { it.currentStreak } ?: 0
        val bestLongestStreak = stats.maxOfOrNull { it.longestStreak } ?: 0
        val totalCompletions = completedOnly.size
        val dailyQuest = calculateDailyQuestStatus(habits, completions)
        val weeklyQuestChain = calculateWeeklyQuestChain(
            buildQuestHistory(habits, completions, days = 14)
        )
        val gamification = calculateGamificationSummary(
            habits = habits,
            stats = stats,
            completions = completions,
            dailyQuestBonusXp = if (dailyQuest.completed) dailyQuest.rewardXp else 0,
            weeklyQuestBonusXp = if (weeklyQuestChain.completed) weeklyQuestChain.rewardXp else 0
        )
        val existingByKey = _uiState.value.achievementProgress.associateBy { it.key }

        val customGoals = existingByKey.values
            .filter { it.category == AchievementCategory.Goal && it.key.startsWith("custom_goal_") }
            .map { existing ->
                val progress = if (existing.key.contains("_streak_")) {
                    bestCurrentStreak
                } else {
                    totalCompletions
                }

                buildAchievement(
                    key = existing.key,
                    category = AchievementCategory.Goal,
                    title = existing.title,
                    description = existing.description,
                    target = existing.target,
                    progress = progress,
                    now = now,
                    existing = existing,
                    allowCustomTarget = true
                )
            }

        val items = listOf(
            buildAchievement(
                key = "goal_streak_7",
                category = AchievementCategory.Goal,
                title = "Goal: 7-Day Streak",
                description = "Reach a 7-day streak on any habit",
                target = 7,
                progress = bestCurrentStreak,
                now = now,
                existing = existingByKey["goal_streak_7"],
                allowCustomTarget = true
            ),
            buildAchievement(
                key = "goal_streak_30",
                category = AchievementCategory.Goal,
                title = "Goal: 30-Day Streak",
                description = "Reach a 30-day streak on any habit",
                target = 30,
                progress = bestCurrentStreak,
                now = now,
                existing = existingByKey["goal_streak_30"],
                allowCustomTarget = true
            ),
            buildAchievement(
                key = "challenge_weekly_12",
                category = AchievementCategory.Challenge,
                title = "Weekly Challenge: 12 Completions",
                description = "Complete habits 12 times this week",
                target = 12,
                progress = completedThisWeek,
                now = now,
                existing = existingByKey["challenge_weekly_12"],
                allowCustomTarget = false
            ),
            buildAchievement(
                key = "challenge_weekly_active_days",
                category = AchievementCategory.Challenge,
                title = "Weekly Challenge: Active Week",
                description = "Complete at least one habit on 5 days this week",
                target = 5,
                progress = activeWeekDays,
                now = now,
                existing = existingByKey["challenge_weekly_active_days"],
                allowCustomTarget = false
            ),
            buildAchievement(
                key = "badge_first_completion",
                category = AchievementCategory.Badge,
                title = "Badge: First Win",
                description = "Complete your first habit",
                target = 1,
                progress = totalCompletions,
                now = now,
                existing = existingByKey["badge_first_completion"],
                allowCustomTarget = false
            ),
            buildAchievement(
                key = "badge_consistency_7",
                category = AchievementCategory.Badge,
                title = "Badge: Consistency Starter",
                description = "Reach 7 total completions",
                target = 7,
                progress = totalCompletions,
                now = now,
                existing = existingByKey["badge_consistency_7"],
                allowCustomTarget = false
            ),
            buildAchievement(
                key = "badge_streak_master",
                category = AchievementCategory.Badge,
                title = "Badge: Streak Master",
                description = "Reach a 30-day longest streak",
                target = 30,
                progress = bestLongestStreak,
                now = now,
                existing = existingByKey["badge_streak_master"],
                allowCustomTarget = false
            ),
            buildAchievement(
                key = "badge_xp_500",
                category = AchievementCategory.Badge,
                title = "Badge: XP Rookie",
                description = "Reach 500 XP",
                target = 500,
                progress = gamification.totalXp,
                now = now,
                existing = existingByKey["badge_xp_500"],
                allowCustomTarget = false
            ),
            buildAchievement(
                key = "badge_xp_1500",
                category = AchievementCategory.Badge,
                title = "Badge: XP Grinder",
                description = "Reach 1500 XP",
                target = 1500,
                progress = gamification.totalXp,
                now = now,
                existing = existingByKey["badge_xp_1500"],
                allowCustomTarget = false
            ),
            buildAchievement(
                key = "badge_level_pro",
                category = AchievementCategory.Badge,
                title = "Badge: Level Pro",
                description = "Reach Pro level",
                target = 1,
                progress = if (gamification.levelName == "Pro") 1 else 0,
                now = now,
                existing = existingByKey["badge_level_pro"],
                allowCustomTarget = false
            )
        ) + customGoals

        habitUseCases.syncAchievementProgress(items)
    }

    private fun buildAchievement(
        key: String,
        category: AchievementCategory,
        title: String,
        description: String,
        target: Int,
        progress: Int,
        now: Long,
        existing: AchievementProgress?,
        allowCustomTarget: Boolean
    ): AchievementProgress {
        val resolvedTarget = if (allowCustomTarget) {
            existing?.target?.coerceAtLeast(1) ?: target
        } else {
            target
        }
        val clamped = progress.coerceAtMost(resolvedTarget)
        val achieved = clamped >= resolvedTarget
        return AchievementProgress(
            key = key,
            category = category,
            title = title,
            description = description,
            target = resolvedTarget,
            progress = clamped,
            achieved = achieved,
            updatedAtMillis = now,
            achievedAtMillis = when {
                !achieved -> null
                existing?.achieved == true -> existing.achievedAtMillis ?: now
                else -> now
            }
        )
    }

    private fun calculateGamificationSummary(
        habits: List<Habit>,
        stats: List<HabitWithStats>,
        completions: List<HabitCompletion>,
        dailyQuestBonusXp: Int = 0,
        weeklyQuestBonusXp: Int = 0
    ): GamificationSummary {
        val completed = completions.count { it.completed }
        val baseXp = completed * 10

        // Streaks provide extra momentum points to reward consistency.
        val streakBonusXp = stats.sumOf { habitStats ->
            val currentBonus = habitStats.currentStreak * 2
            val longestBonus = habitStats.longestStreak
            currentBonus + longestBonus
        }

        val missed = estimateMissedHabits(habits = habits, completions = completions)
        val penaltyXp = missed * 4

        val totalXp = max(0, baseXp + streakBonusXp + dailyQuestBonusXp + weeklyQuestBonusXp - penaltyXp)
        val level = levelForXp(totalXp)
        val nextLevel = LEVELS.getOrNull(level.index + 1)

        val xpIntoCurrentLevel = (totalXp - level.minXp).coerceAtLeast(0)
        val xpToNextLevel = if (nextLevel != null) {
            (nextLevel.minXp - totalXp).coerceAtLeast(0)
        } else {
            0
        }

        val levelProgress = if (nextLevel == null) {
            1f
        } else {
            val span = (nextLevel.minXp - level.minXp).coerceAtLeast(1)
            xpIntoCurrentLevel.toFloat() / span.toFloat()
        }

        return GamificationSummary(
            totalXp = totalXp,
            levelIndex = level.index + 1,
            levelName = level.name,
            xpIntoCurrentLevel = xpIntoCurrentLevel,
            xpToNextLevel = xpToNextLevel,
            levelProgress = levelProgress.coerceIn(0f, 1f),
            streakBonusXp = streakBonusXp,
            penaltyXp = penaltyXp,
            missedHabits = missed,
            dailyQuestBonusXp = dailyQuestBonusXp,
            weeklyQuestBonusXp = weeklyQuestBonusXp
        )
    }

    private fun calculateDailyQuestStatus(
        habits: List<Habit>,
        completions: List<HabitCompletion>
    ): DailyQuestStatus {
        return calculateDailyQuestForDay(
            epochDay = DateUtils.todayEpochDay(),
            habits = habits,
            completions = completions
        )
    }

    private fun calculateDailyQuestForDay(
        epochDay: Long,
        habits: List<Habit>,
        completions: List<HabitCompletion>
    ): DailyQuestStatus {
        val completedOnDay = completions.filter { it.completed && it.epochDay == epochDay }
        val completedByHabitId = completedOnDay.map { it.habitId }.toSet()
        val questType = when ((epochDay % 3L).toInt()) {
            0 -> DailyQuestType.CheckinsToday
            1 -> DailyQuestType.ActiveHabits
            else -> DailyQuestType.DailyPerfect
        }

        return when (questType) {
            DailyQuestType.CheckinsToday -> {
                val target = 3
                val progress = completedOnDay.size
                DailyQuestStatus(
                    type = questType,
                    title = appContext.getString(R.string.quest_daily_type_triple_title),
                    description = appContext.getString(R.string.quest_daily_type_triple_desc),
                    target = target,
                    progress = progress.coerceAtMost(target),
                    rewardXp = 30,
                    completed = progress >= target,
                    epochDay = epochDay
                )
            }

            DailyQuestType.ActiveHabits -> {
                val target = 2
                val progress = completedByHabitId.size
                DailyQuestStatus(
                    type = questType,
                    title = appContext.getString(R.string.quest_daily_type_multi_title),
                    description = appContext.getString(R.string.quest_daily_type_multi_desc),
                    target = target,
                    progress = progress.coerceAtMost(target),
                    rewardXp = 40,
                    completed = progress >= target,
                    epochDay = epochDay
                )
            }

            DailyQuestType.DailyPerfect -> {
                val dailyHabits = habits.filter { it.frequency == HabitFrequency.DAILY }
                val target = if (dailyHabits.isEmpty()) 1 else dailyHabits.size
                val progress = if (dailyHabits.isEmpty()) {
                    if (completedOnDay.isNotEmpty()) 1 else 0
                } else {
                    dailyHabits.count { it.id in completedByHabitId }
                }
                DailyQuestStatus(
                    type = questType,
                    title = appContext.getString(R.string.quest_daily_type_perfect_title),
                    description = appContext.getString(R.string.quest_daily_type_perfect_desc),
                    target = target,
                    progress = progress.coerceAtMost(target),
                    rewardXp = 50,
                    completed = progress >= target,
                    epochDay = epochDay
                )
            }
        }
    }

    private fun calculateHabitInsights(
        habits: List<Habit>,
        stats: List<HabitWithStats>,
        completions: List<HabitCompletion>,
        selectedHabitId: Long?
    ): List<String> {
        if (habits.isEmpty()) return emptyList()

        val today = DateUtils.todayEpochDay()
        val windowDays = 56L
        val start = today - (windowDays - 1)
        val scopedHabits = if (selectedHabitId != null) {
            habits.filter { it.id == selectedHabitId }
        } else {
            habits
        }
        val scopedHabitIds = scopedHabits.map { it.id }.toSet()
        val habitsById = scopedHabits.associateBy { it.id }
        val scopedStats = stats.filter { it.habit.id in scopedHabitIds }

        if (scopedHabits.isEmpty()) {
            return listOf("Select a valid habit to view habit-specific insights.")
        }

        val expectedByDow = IntArray(7)
        val completedByDow = IntArray(7)

        for (day in start..today) {
            val dow = (LocalDate.ofEpochDay(day).dayOfWeek.value - 1).coerceIn(0, 6)
            scopedHabits.forEach { habit ->
                if (habit.frequency == HabitFrequency.DAILY && habit.createdAtEpochDay <= day) {
                    expectedByDow[dow]++
                }
            }
        }

        completions.asSequence()
            .filter { it.completed && it.epochDay in start..today && it.habitId in scopedHabitIds }
            .forEach { completion ->
                val habit = habitsById[completion.habitId] ?: return@forEach
                if (habit.frequency == HabitFrequency.DAILY) {
                    val dow = (LocalDate.ofEpochDay(completion.epochDay).dayOfWeek.value - 1).coerceIn(0, 6)
                    completedByDow[dow]++
                }
            }

        val missedByDow = IntArray(7) { index ->
            (expectedByDow[index] - completedByDow[index]).coerceAtLeast(0)
        }

        val insights = mutableListOf<String>()

        val strongestDay = (0..6)
            .filter { expectedByDow[it] >= 2 }
            .maxByOrNull { index ->
                completedByDow[index].toFloat() / expectedByDow[index].toFloat()
            }

        if (strongestDay != null) {
            val dayName = DayOfWeek.of(strongestDay + 1)
                .getDisplayName(TextStyle.FULL, Locale.getDefault())
            insights += "You are most consistent on $dayName."
        }

        val weekendMiss = missedByDow[5] + missedByDow[6]
        val weekendExpected = expectedByDow[5] + expectedByDow[6]
        val weekdayMiss = missedByDow.copyOfRange(0, 5).sum()
        val weekdayExpected = expectedByDow.copyOfRange(0, 5).sum()

        val weekendMissRate = if (weekendExpected > 0) weekendMiss.toFloat() / weekendExpected.toFloat() else 0f
        val weekdayMissRate = if (weekdayExpected > 0) weekdayMiss.toFloat() / weekdayExpected.toFloat() else 0f

        if (weekendExpected >= 4 && weekendMissRate > weekdayMissRate + 0.08f) {
            insights += "You miss habits mostly on weekends."
        } else if (weekdayExpected >= 10 && weekdayMissRate > weekendMissRate + 0.08f) {
            insights += "Weekdays are your toughest consistency window."
        }

        val thisWeekStart = today - 6
        val prevWeekStart = thisWeekStart - 7
        val thisWeekCount = completions.count {
            it.completed && it.habitId in scopedHabitIds && it.epochDay in thisWeekStart..today
        }
        val prevWeekCount = completions.count {
            it.completed && it.habitId in scopedHabitIds && it.epochDay in prevWeekStart until thisWeekStart
        }
        if (prevWeekCount >= 3) {
            val delta = ((thisWeekCount - prevWeekCount).toFloat() / prevWeekCount.toFloat())
            if (delta >= 0.20f) {
                insights += "You are up ${(delta * 100f).roundToInt()}% in completions this week."
            } else if (delta <= -0.20f) {
                insights += "Completions are down ${(-delta * 100f).roundToInt()}% this week."
            }
        }

        val streakLeader = scopedStats.maxByOrNull { it.currentStreak }
        if (streakLeader != null && streakLeader.currentStreak >= 3) {
            insights += "${streakLeader.habit.name} is your current momentum leader (${streakLeader.currentStreak}-day streak)."
        }

        if (selectedHabitId != null && scopedHabits.size == 1) {
            val habitName = scopedHabits.first().name
            insights.add(0, "Insights for $habitName")
        }

        return insights
            .distinct()
            .take(4)
            .ifEmpty { listOf("Keep logging habits to unlock personalized insights.") }
    }

    private fun buildQuestHistory(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        days: Int
    ): List<QuestHistoryEntry> {
        val today = DateUtils.todayEpochDay()
        return (0 until days)
            .map { offset -> today - offset }
            .map { day ->
                val quest = calculateDailyQuestForDay(day, habits, completions)
                QuestHistoryEntry(
                    epochDay = day,
                    dayLabel = LocalDate.ofEpochDay(day)
                        .dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    title = quest.title,
                    completed = quest.completed,
                    rewardXp = quest.rewardXp
                )
            }
    }

    private fun calculateWeeklyQuestChain(history: List<QuestHistoryEntry>): WeeklyQuestChainStatus {
        if (history.isEmpty()) return WeeklyQuestChainStatus()

        val today = DateUtils.todayEpochDay()
        val weekStart = LocalDate.now()
            .minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
            .toEpochDay()

        val completedThisWeek = history.count { it.epochDay in weekStart..today && it.completed }
        val target = 5
        val currentStreak = history
            .sortedByDescending { it.epochDay }
            .takeWhile { it.completed }
            .count()

        return WeeklyQuestChainStatus(
            completedDaysThisWeek = completedThisWeek,
            targetDays = target,
            currentStreakDays = currentStreak,
            completed = completedThisWeek >= target,
            rewardXp = 80
        )
    }

    private fun triggerLevelUpCelebration(levelIndex: Int, levelName: String) {
        val message = "Level up! You reached Level $levelIndex: $levelName"
        _uiState.update { it.copy(levelUpMessage = message) }
        viewModelScope.launch {
            delay(3500)
            val current = _uiState.value.levelUpMessage
            if (current == message) {
                _uiState.update { it.copy(levelUpMessage = null) }
            }
        }
    }

    fun clearLevelUpMessage() {
        _uiState.update { it.copy(levelUpMessage = null) }
    }

    private fun estimateMissedHabits(habits: List<Habit>, completions: List<HabitCompletion>): Int {
        if (habits.isEmpty()) return 0

        val today = DateUtils.todayEpochDay()
        val windowDays = 30L
        val start = today - (windowDays - 1)
        val completedByHabit = completions
            .asSequence()
            .filter { it.completed && it.epochDay in start..today }
            .groupBy { it.habitId }

        return habits.sumOf { habit ->
            when (habit.frequency) {
                HabitFrequency.DAILY -> {
                    val effectiveStart = max(start, habit.createdAtEpochDay)
                    if (effectiveStart > today) {
                        0
                    } else {
                        val expected = (today - effectiveStart + 1).toInt()
                        val actual = completedByHabit[habit.id]
                            ?.count { it.epochDay >= effectiveStart }
                            ?: 0
                        (expected - actual).coerceAtLeast(0)
                    }
                }

                HabitFrequency.WEEKLY -> {
                    val weeklyExpected = ((windowDays + 6) / 7).toInt()
                    val weeklyActual = completedByHabit[habit.id]?.size ?: 0
                    (weeklyExpected - weeklyActual).coerceAtLeast(0)
                }
            }
        }
    }

    private fun levelForXp(totalXp: Int): LevelDefinition {
        return LEVELS.lastOrNull { totalXp >= it.minXp } ?: LEVELS.first()
    }

    private data class LevelDefinition(
        val index: Int,
        val name: String,
        val minXp: Int
    )

    companion object {
        private val LEVELS = listOf(
            LevelDefinition(index = 0, name = "Beginner", minXp = 0),
            LevelDefinition(index = 1, name = "Rising", minXp = 250),
            LevelDefinition(index = 2, name = "Focused", minXp = 600),
            LevelDefinition(index = 3, name = "Consistent", minXp = 1100),
            LevelDefinition(index = 4, name = "Advanced", minXp = 1800),
            LevelDefinition(index = 5, name = "Pro", minXp = 2800)
        )
    }

    fun addHabit(
        name: String,
        description: String,
        frequency: HabitFrequency,
        colorHex: String,
        reminderEnabled: Boolean,
        reminderHour: Int?,
        reminderMinute: Int?,
        reminderMessage: String?
    ) {
        viewModelScope.launch {
            runCatching {
                val newHabitId = habitUseCases.addHabit(
                    Habit(
                        id = 0,
                        name = name.trim(),
                        description = description.trim(),
                        frequency = frequency,
                        colorHex = colorHex,
                        reminderEnabled = reminderEnabled,
                        reminderHour = reminderHour,
                        reminderMinute = reminderMinute,
                        reminderMessage = reminderMessage?.trim().orEmpty().ifBlank { null },
                        createdAtEpochDay = DateUtils.todayEpochDay(),
                        archived = false
                    )
                )

                if (reminderEnabled && reminderHour != null && reminderMinute != null) {
                    reminderScheduler.scheduleHabitReminder(
                        habitId = newHabitId,
                        habitName = name.trim(),
                        message = reminderMessage,
                        hour = reminderHour,
                        minute = reminderMinute
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        status = HabitUiStatus.Error,
                        errorMessage = throwable.message ?: "Failed to add habit"
                    )
                }
            }
        }
    }

    fun adjustGoalTarget(key: String, delta: Int) {
        viewModelScope.launch {
            runCatching {
                val existing = _uiState.value.achievementProgress.firstOrNull { it.key == key } ?: return@runCatching
                if (existing.category != AchievementCategory.Goal) return@runCatching

                val updatedTarget = (existing.target + delta).coerceAtLeast(1)
                habitUseCases.updateAchievementTarget(key = key, target = updatedTarget)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        status = HabitUiStatus.Error,
                        errorMessage = throwable.message ?: "Failed to update goal target"
                    )
                }
            }
        }
    }

    fun addCustomGoal(title: String, target: Int, metric: String) {
        viewModelScope.launch {
            runCatching {
                val cleanTitle = title.trim().ifBlank { "Custom Goal" }
                val normalizedMetric = metric.trim().uppercase(Locale.US)
                val validMetric = if (normalizedMetric == "STREAK") "STREAK" else "COMPLETION"
                val key = "custom_goal_${validMetric.lowercase(Locale.US)}_${System.currentTimeMillis()}"

                val progress = when (validMetric) {
                    "STREAK" -> _uiState.value.stats.maxOfOrNull { it.currentStreak } ?: 0
                    else -> _uiState.value.completions.count { it.completed }
                }

                val clampedTarget = target.coerceAtLeast(1)
                val achieved = progress >= clampedTarget
                val now = System.currentTimeMillis()

                habitUseCases.syncAchievementProgress(
                    listOf(
                        AchievementProgress(
                            key = key,
                            category = AchievementCategory.Goal,
                            title = cleanTitle,
                            description = if (validMetric == "STREAK") {
                                "Custom streak goal"
                            } else {
                                "Custom completion goal"
                            },
                            target = clampedTarget,
                            progress = progress.coerceAtMost(clampedTarget),
                            achieved = achieved,
                            updatedAtMillis = now,
                            achievedAtMillis = if (achieved) now else null
                        )
                    )
                )
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        status = HabitUiStatus.Error,
                        errorMessage = throwable.message ?: "Failed to add custom goal"
                    )
                }
            }
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            runCatching {
                habitUseCases.updateHabit(habit)
                if (habit.reminderEnabled && habit.reminderHour != null && habit.reminderMinute != null) {
                    reminderScheduler.rescheduleHabitReminder(
                        habitId = habit.id,
                        habitName = habit.name,
                        message = habit.reminderMessage,
                        hour = habit.reminderHour,
                        minute = habit.reminderMinute
                    )
                } else {
                    reminderScheduler.cancelHabitReminder(habit.id)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        status = HabitUiStatus.Error,
                        errorMessage = throwable.message ?: "Failed to update habit"
                    )
                }
            }
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            runCatching {
                reminderScheduler.cancelHabitReminder(habitId)
                habitUseCases.deleteHabit(habitId)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        status = HabitUiStatus.Error,
                        errorMessage = throwable.message ?: "Failed to delete habit"
                    )
                }
            }
        }
    }

    fun toggleHabitCompletion(habitId: Long, epochDay: Long) {
        viewModelScope.launch {
            runCatching {
                val stat = uiState.value.stats.firstOrNull { it.habit.id == habitId }
                val currentlyCompleted = stat?.completedDays?.contains(epochDay) == true
                val updatedCompleted = !currentlyCompleted

                habitUseCases.markHabitComplete(habitId, epochDay, updatedCompleted)
                calculateStreaks(habitId)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        status = HabitUiStatus.Error,
                        errorMessage = throwable.message ?: "Failed to toggle completion"
                    )
                }
            }
        }
    }

    fun setCompletion(habitId: Long, epochDay: Long, completed: Boolean) {
        viewModelScope.launch {
            runCatching {
                habitUseCases.markHabitComplete(habitId, epochDay, completed)
                calculateStreaks(habitId)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        status = HabitUiStatus.Error,
                        errorMessage = throwable.message ?: "Failed to update completion"
                    )
                }
            }
        }
    }

    fun setHabitNote(habitId: Long, epochDay: Long, note: String?) {
        viewModelScope.launch {
            runCatching {
                habitUseCases.setHabitNote(habitId, epochDay, note)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        status = HabitUiStatus.Error,
                        errorMessage = throwable.message ?: "Failed to save note"
                    )
                }
            }
        }
    }

    fun sendTestReminder(habitName: String, reminderMessage: String?) {
        runCatching {
            val resolvedName = habitName.trim().ifBlank { "your habit" }
            reminderScheduler.sendTestReminder(
                habitName = resolvedName,
                message = reminderMessage?.trim().orEmpty().ifBlank { null }
            )
            refreshReminderDiagnostics()
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(
                    status = HabitUiStatus.Error,
                    errorMessage = throwable.message ?: "Failed to send test reminder"
                )
            }
        }
    }

    fun setHabitReminderEnabled(habitId: Long, enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                val existing = _uiState.value.habits.firstOrNull { it.id == habitId } ?: return@runCatching
                val hour = existing.reminderHour ?: 20
                val minute = existing.reminderMinute ?: 0

                val updated = existing.copy(
                    reminderEnabled = enabled,
                    reminderHour = if (enabled) hour else existing.reminderHour,
                    reminderMinute = if (enabled) minute else existing.reminderMinute
                )

                habitUseCases.updateHabit(updated)

                if (enabled) {
                    reminderScheduler.rescheduleHabitReminder(
                        habitId = updated.id,
                        habitName = updated.name,
                        message = updated.reminderMessage,
                        hour = hour,
                        minute = minute
                    )
                } else {
                    reminderScheduler.cancelHabitReminder(updated.id)
                }

                refreshReminderDiagnostics()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        status = HabitUiStatus.Error,
                        errorMessage = throwable.message ?: "Failed to update reminder setting"
                    )
                }
            }
        }
    }

    fun calculateStreaks(habitId: Long) {
        viewModelScope.launch {
            runCatching {
                val streak = habitUseCases.getHabitStreak(habitId, DateUtils.todayEpochDay())
                _uiState.update {
                    it.copy(habitStreaks = it.habitStreaks + (habitId to streak))
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        status = HabitUiStatus.Error,
                        errorMessage = throwable.message ?: "Failed to calculate streaks"
                    )
                }
            }
        }
    }

    fun setSelectedHabit(habitId: Long) {
        _uiState.update { it.copy(selectedHabitId = habitId) }
        viewModelScope.launch {
            homeLayoutPreferences.setSelectedHabitId(habitId)
        }
    }

    fun setHomeLayoutMode(mode: HomeLayoutMode) {
        _uiState.update { it.copy(homeLayoutMode = mode) }
        viewModelScope.launch {
            homeLayoutPreferences.setLayoutMode(mode)
        }
    }

    fun setPreferredTopLevelRoute(route: String) {
        _uiState.update { it.copy(preferredTopLevelRoute = route) }
        viewModelScope.launch {
            homeLayoutPreferences.setSelectedTopLevelRoute(route)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            homeLayoutPreferences.setThemeMode(mode)
        }
    }

    fun setAnalyticsRange(range: String) {
        _uiState.update { it.copy(analyticsRange = range) }
        viewModelScope.launch {
            homeLayoutPreferences.setAnalyticsRange(range)
        }
    }

    fun setAnalyticsHabit(habitId: Long?) {
        _uiState.update {
            it.copy(
                analyticsHabitId = habitId,
                habitInsights = calculateHabitInsights(
                    habits = it.habits,
                    stats = it.stats,
                    completions = it.completions,
                    selectedHabitId = habitId
                )
            )
        }
        viewModelScope.launch {
            homeLayoutPreferences.setAnalyticsHabitId(habitId)
        }
    }

    fun setAnalyticsHeatmapScroll(offset: Int) {
        _uiState.update { it.copy(analyticsHeatmapScroll = offset) }
        viewModelScope.launch {
            homeLayoutPreferences.setAnalyticsHeatmapScroll(offset)
        }
    }

    fun setAnalyticsHabitFilterScroll(offset: Int) {
        _uiState.update { it.copy(analyticsHabitFilterScroll = offset) }
        viewModelScope.launch {
            homeLayoutPreferences.setAnalyticsHabitFilterScroll(offset)
        }
    }

    fun refreshReminderDiagnostics() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(reminderDiagnosticsLoading = true) }

            val habits = _uiState.value.habits
            val workManager = WorkManager.getInstance(appContext)

            val diagnostics = habits.map { habit ->
                val uniqueWorkName = "habit_reminder_work_${habit.id}"
                val workInfos = runCatching {
                    workManager.getWorkInfosForUniqueWork(uniqueWorkName).get()
                }.getOrDefault(emptyList())

                ReminderWorkDebugItem(
                    habitId = habit.id,
                    habitName = habit.name,
                    reminderEnabled = habit.reminderEnabled,
                    reminderTimeLabel = if (habit.reminderHour != null && habit.reminderMinute != null) {
                        String.format(Locale.US, "%02d:%02d", habit.reminderHour, habit.reminderMinute)
                    } else {
                        "--:--"
                    },
                    uniqueWorkName = uniqueWorkName,
                    workStates = workInfos.map { it.state.name },
                    runAttemptCounts = workInfos.map { it.runAttemptCount }
                )
            }

            _uiState.update {
                it.copy(
                    reminderDiagnostics = diagnostics,
                    reminderDiagnosticsLoading = false,
                    reminderDiagnosticsUpdatedAtMillis = currentTimeMillis()
                )
            }
        }
    }
}

data class ReminderWorkDebugItem(
    val habitId: Long,
    val habitName: String,
    val reminderEnabled: Boolean,
    val reminderTimeLabel: String,
    val uniqueWorkName: String,
    val workStates: List<String>,
    val runAttemptCounts: List<Int>
)

data class HabitUiState(
    val habits: List<Habit> = emptyList(),
    val stats: List<HabitWithStats> = emptyList(),
    val completions: List<HabitCompletion> = emptyList(),
    val selectedHabitId: Long? = null,
    val status: HabitUiStatus = HabitUiStatus.Loading,
    val errorMessage: String? = null,
    val habitStreaks: Map<Long, HabitStreak> = emptyMap(),
    val isRefreshing: Boolean = false,
    val lastRefreshedAtMillis: Long? = null,
    val currentTimeMillis: Long = currentTimeMillis(),
    val homeLayoutMode: HomeLayoutMode = HomeLayoutMode.Auto,
    val preferredTopLevelRoute: String = "habits",
    val themeMode: ThemeMode = ThemeMode.System,
    val analyticsRange: String = "30D",
    val analyticsHabitId: Long? = null,
    val analyticsHeatmapScroll: Int = 0,
    val analyticsHabitFilterScroll: Int = 0,
    val habitInsights: List<String> = emptyList(),
    val gamification: GamificationSummary = GamificationSummary(),
    val dailyQuest: DailyQuestStatus = DailyQuestStatus(),
    val questHistory: List<QuestHistoryEntry> = emptyList(),
    val weeklyQuestChain: WeeklyQuestChainStatus = WeeklyQuestChainStatus(),
    val levelUpMessage: String? = null,
    val achievementProgress: List<AchievementProgress> = emptyList(),
    val reminderDiagnostics: List<ReminderWorkDebugItem> = emptyList(),
    val reminderDiagnosticsLoading: Boolean = false,
    val reminderDiagnosticsUpdatedAtMillis: Long? = null
)

data class GamificationSummary(
    val totalXp: Int = 0,
    val levelIndex: Int = 1,
    val levelName: String = "Beginner",
    val xpIntoCurrentLevel: Int = 0,
    val xpToNextLevel: Int = 0,
    val levelProgress: Float = 0f,
    val streakBonusXp: Int = 0,
    val penaltyXp: Int = 0,
    val missedHabits: Int = 0,
    val dailyQuestBonusXp: Int = 0,
    val weeklyQuestBonusXp: Int = 0
)

enum class DailyQuestType {
    CheckinsToday,
    ActiveHabits,
    DailyPerfect
}

data class DailyQuestStatus(
    val type: DailyQuestType = DailyQuestType.CheckinsToday,
    val title: String = "Daily Quest",
    val description: String = "Complete habits today to earn bonus XP",
    val target: Int = 1,
    val progress: Int = 0,
    val rewardXp: Int = 20,
    val completed: Boolean = false,
    val epochDay: Long = 0
)

data class QuestHistoryEntry(
    val epochDay: Long,
    val dayLabel: String,
    val title: String,
    val completed: Boolean,
    val rewardXp: Int
)

data class WeeklyQuestChainStatus(
    val completedDaysThisWeek: Int = 0,
    val targetDays: Int = 5,
    val currentStreakDays: Int = 0,
    val completed: Boolean = false,
    val rewardXp: Int = 80
)

enum class HabitUiStatus {
    Loading,
    Success,
    Error
}

enum class HomeLayoutMode {
    Auto,
    List,
    Grid
}
