package com.example.habittracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.habittracker.R
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitCompletion
import com.example.habittracker.domain.model.HabitFrequency
import com.example.habittracker.ui.viewmodel.HabitUiState
import com.example.habittracker.ui.viewmodel.HabitUiStatus
import com.example.habittracker.ui.viewmodel.QuestHistoryEntry
import com.example.habittracker.ui.viewmodel.WeeklyQuestChainStatus
import com.example.habittracker.ui.components.adaptiveContentPadding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestScreen(
    state: HabitUiState,
    onRetry: () -> Unit,
    onOpenHabit: (Long) -> Unit
) {
    var selectedQuestDay by remember { mutableStateOf<QuestHistoryEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.quest_hub_title))
                        Text(
                            text = stringResource(R.string.quest_hub_subtitle),
                            style = MaterialTheme.typography.labelSmall
                        )
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
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.quest_loading_data), modifier = Modifier.padding(top = 12.dp))
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
                    Text(stringResource(R.string.quest_error_title), style = MaterialTheme.typography.headlineSmall)
                    Text(state.errorMessage ?: stringResource(R.string.quest_error_unknown))
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.quest_retry))
                    }
                }
            }

            HabitUiStatus.Success -> {
                val contentPadding = adaptiveContentPadding(vertical = 18.dp)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        WeeklyChainHeroCard(
                            chain = state.weeklyQuestChain,
                            weeklyBonusXp = state.gamification.weeklyQuestBonusXp
                        )
                    }

                    item {
                        DailyQuestSpotlightCard(
                            title = state.dailyQuest.title,
                            description = state.dailyQuest.description,
                            progress = state.dailyQuest.progress,
                            target = state.dailyQuest.target,
                            rewardXp = state.dailyQuest.rewardXp,
                            completed = state.dailyQuest.completed
                        )
                    }

                    item {
                        QuestTrendChart(
                            entries = state.questHistory,
                            onSelectEntry = { selectedQuestDay = it }
                        )
                    }

                    item {
                        Text(stringResource(R.string.quest_recent_log), style = MaterialTheme.typography.titleMedium)
                    }

                    items(state.questHistory.take(14), key = { it.epochDay }) { item ->
                        QuestHistoryRow(
                            item = item,
                            onClick = { selectedQuestDay = item }
                        )
                    }
                }

                selectedQuestDay?.let { day ->
                    QuestDayDetailsDialog(
                        entry = day,
                        habits = state.habits,
                        completions = state.completions,
                        onOpenHabit = onOpenHabit,
                        onDismiss = { selectedQuestDay = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyChainHeroCard(
    chain: WeeklyQuestChainStatus,
    weeklyBonusXp: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.quest_weekly_chain_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (chain.completed) {
                    stringResource(R.string.quest_weekly_chain_complete)
                } else {
                    stringResource(
                        R.string.quest_weekly_chain_progress,
                        chain.completedDaysThisWeek,
                        chain.targetDays
                    )
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            LinearProgressIndicator(
                progress = {
                    if (chain.targetDays <= 0) 0f
                    else (chain.completedDaysThisWeek.toFloat() / chain.targetDays.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(
                        text = stringResource(R.string.quest_weekly_chain_streak, chain.currentStreakDays),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(
                        text = stringResource(R.string.quest_weekly_chain_target_reward, chain.rewardXp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (weeklyBonusXp > 0) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                        Text(
                            text = stringResource(R.string.quest_weekly_chain_active_bonus, weeklyBonusXp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyQuestSpotlightCard(
    title: String,
    description: String,
    progress: Int,
    target: Int,
    rewardXp: Int,
    completed: Boolean
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.quest_daily_spotlight_title), style = MaterialTheme.typography.titleMedium)
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(description, style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = {
                    if (target <= 0) 0f else (progress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (completed) {
                    stringResource(R.string.quest_daily_spotlight_completed, rewardXp)
                } else {
                    stringResource(R.string.quest_daily_spotlight_progress, progress, target, rewardXp)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuestTrendChart(
    entries: List<QuestHistoryEntry>,
    onSelectEntry: (QuestHistoryEntry) -> Unit
) {
    val ordered = entries.take(14).sortedBy { it.epochDay }
    val maxValue = max(1, ordered.size)
    val primary = MaterialTheme.colorScheme.primary
    val missed = Color(0xFFB0BEC5)
    val haptic = LocalHapticFeedback.current
    var highlightedEpochDay by remember(ordered) { mutableStateOf<Long?>(null) }
    var tooltipEntry by remember(ordered) { mutableStateOf<QuestHistoryEntry?>(null) }
    var tooltipXFraction by remember(ordered) { mutableStateOf(0.5f) }
    var chartWidthPx by remember { mutableStateOf(0) }
    var chartHeightPx by remember { mutableStateOf(0) }
    var tooltipAbovePoint by remember { mutableStateOf(true) }
    var tooltipBubbleWidthPx by remember { mutableStateOf(136) }
    var tooltipBubbleHeightPx by remember { mutableStateOf(28) }
    var pendingSelection by remember(ordered) { mutableStateOf<QuestHistoryEntry?>(null) }
    val pulseTransition = rememberInfiniteTransition(label = "quest-point-pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "quest-point-pulse-value"
    )

    LaunchedEffect(highlightedEpochDay) {
        if (highlightedEpochDay != null) {
            delay(900)
            highlightedEpochDay = null
        }
    }

    LaunchedEffect(pendingSelection) {
        val selected = pendingSelection ?: return@LaunchedEffect
        delay(340)
        onSelectEntry(selected)
        pendingSelection = null
        tooltipEntry = null
    }

    LaunchedEffect(tooltipEntry?.epochDay) {
        if (tooltipEntry != null) {
            tooltipBubbleWidthPx = 136
            tooltipBubbleHeightPx = 28
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.quest_trend_title), style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .onSizeChanged {
                        chartWidthPx = it.width
                        chartHeightPx = it.height
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .pointerInput(ordered) {
                            detectTapGestures { offset ->
                                if (ordered.isEmpty()) return@detectTapGestures

                                val width = size.width.toFloat().coerceAtLeast(1f)
                                val height = size.height.toFloat().coerceAtLeast(1f)
                                val step = width / maxValue.toFloat()
                                val rawIndex = (offset.x / step - 0.5f).roundToInt()
                                val index = rawIndex.coerceIn(0, ordered.lastIndex)

                                val top = 18f
                                val bottom = height - 18f
                                val targetY = if (ordered[index].completed) top else bottom
                                val closeEnoughToPoint = abs(offset.y - targetY) <= 28f

                                if (closeEnoughToPoint) {
                                    val selected = ordered[index]
                                    highlightedEpochDay = selected.epochDay
                                    tooltipEntry = selected
                                    tooltipXFraction = ((index + 0.5f) / maxValue.toFloat()).coerceIn(0f, 1f)
                                    tooltipAbovePoint = targetY > (height / 2f)
                                    pendingSelection = selected
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                ) {
                    if (ordered.isEmpty()) return@Canvas
                    val step = size.width / maxValue.toFloat()
                    val top = 18f
                    val bottom = size.height - 18f

                    drawLine(
                        color = Color(0x33000000),
                        start = Offset(0f, bottom),
                        end = Offset(size.width, bottom),
                        strokeWidth = 2f
                    )

                    val points = ordered.mapIndexed { index, item ->
                        val x = (index + 0.5f) * step
                        val y = if (item.completed) top else bottom
                        Offset(x, y)
                    }

                    points.zipWithNext { start, end ->
                        drawLine(
                            color = primary,
                            start = start,
                            end = end,
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }

                    points.forEachIndexed { index, point ->
                        val isHighlighted = ordered[index].epochDay == highlightedEpochDay
                        if (isHighlighted) {
                            drawCircle(
                                color = primary.copy(alpha = 0.25f + (0.25f * pulse)),
                                center = point,
                                radius = 11f + (4f * pulse)
                            )
                        }

                        drawCircle(
                            color = if (ordered[index].completed) primary else missed,
                            center = point,
                            radius = if (isHighlighted) 9f else 7f,
                            style = Stroke(width = 3f)
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    modifier = Modifier.align(Alignment.TopStart),
                    visible = tooltipEntry != null,
                    enter = fadeIn(animationSpec = tween(140)) + scaleIn(
                        animationSpec = tween(170),
                        initialScale = 0.92f
                    ),
                    exit = fadeOut(animationSpec = tween(110)) + scaleOut(
                        animationSpec = tween(130),
                        targetScale = 0.95f
                    )
                ) {
                    val item = tooltipEntry
                    if (item != null) {
                        val tooltipBackground = MaterialTheme.colorScheme.inverseSurface
                        val bubbleWidth = tooltipBubbleWidthPx.coerceAtLeast(96)
                        val bubbleHeight = tooltipBubbleHeightPx.coerceAtLeast(24)
                        val pointerHeight = 12

                        val tooltipLeftPx = (tooltipXFraction * chartWidthPx - (bubbleWidth / 2f))
                            .roundToInt()
                            .coerceAtLeast(4)
                            .coerceAtMost((chartWidthPx - bubbleWidth).coerceAtLeast(4))
                        val tooltipTopPx = if (tooltipAbovePoint) {
                            6
                        } else {
                            (chartHeightPx - (bubbleHeight + pointerHeight + 6)).coerceAtLeast(6)
                        }
                        Column(
                            modifier = Modifier.offset { IntOffset(tooltipLeftPx, tooltipTopPx) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!tooltipAbovePoint) {
                                TooltipPointer(
                                    upward = true,
                                    color = tooltipBackground
                                )
                            }

                            Surface(
                                color = tooltipBackground,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.onSizeChanged {
                                    tooltipBubbleWidthPx = it.width
                                    tooltipBubbleHeightPx = it.height
                                }
                            ) {
                                Text(
                                    text = if (item.completed) {
                                        stringResource(R.string.quest_tooltip_done, item.dayLabel, item.rewardXp)
                                    } else {
                                        stringResource(R.string.quest_tooltip_miss, item.dayLabel)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.inverseOnSurface
                                )
                            }

                            if (tooltipAbovePoint) {
                                TooltipPointer(
                                    upward = false,
                                    color = tooltipBackground
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.quest_trend_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.quest_trend_tap_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TooltipPointer(
    upward: Boolean,
    color: Color
) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val pointer = Path().apply {
            if (upward) {
                moveTo(size.width * 0.5f, size.height * 0.2f)
                lineTo(size.width * 0.15f, size.height * 0.85f)
                lineTo(size.width * 0.85f, size.height * 0.85f)
            } else {
                moveTo(size.width * 0.15f, size.height * 0.2f)
                lineTo(size.width * 0.85f, size.height * 0.2f)
                lineTo(size.width * 0.5f, size.height * 0.85f)
            }
            close()
        }
        drawPath(path = pointer, color = color)
    }
}

@Composable
private fun QuestHistoryRow(
    item: QuestHistoryEntry,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.dayLabel, style = MaterialTheme.typography.labelLarge)
                Text(item.title, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = stringResource(R.string.quest_row_tap_details),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = if (item.completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = if (item.completed) {
                        stringResource(R.string.quest_done_with_xp, item.rewardXp)
                    } else {
                        stringResource(R.string.quest_miss)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun QuestDayDetailsDialog(
    entry: QuestHistoryEntry,
    habits: List<Habit>,
    completions: List<HabitCompletion>,
    onOpenHabit: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val date = LocalDate.ofEpochDay(entry.epochDay)
    val dateLabel = date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    val completedHabitIds = completions
        .asSequence()
        .filter { it.epochDay == entry.epochDay && it.completed }
        .map { it.habitId }
        .toSet()
    val completedHabits = habits.filter { it.id in completedHabitIds }
    val missedDailyHabits = habits.filter {
        it.frequency == HabitFrequency.DAILY &&
            it.createdAtEpochDay <= entry.epochDay &&
            it.id !in completedHabitIds
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quest_day_details_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(dateLabel, style = MaterialTheme.typography.titleSmall)
                Text(entry.title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = if (entry.completed) {
                        stringResource(R.string.quest_day_completed, entry.rewardXp)
                    } else {
                        stringResource(R.string.quest_day_missed)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(stringResource(R.string.quest_completed_habits_title), style = MaterialTheme.typography.titleSmall)
                if (completedHabits.isEmpty()) {
                    Text(stringResource(R.string.quest_no_habits_completed), style = MaterialTheme.typography.bodySmall)
                } else {
                    completedHabits.forEach { habit ->
                        Text(
                            text = stringResource(R.string.quest_bulleted_habit, habit.name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onDismiss()
                                onOpenHabit(habit.id)
                            }
                        )
                    }
                }

                Text(stringResource(R.string.quest_missed_daily_habits_title), style = MaterialTheme.typography.titleSmall)
                if (missedDailyHabits.isEmpty()) {
                    Text(stringResource(R.string.quest_no_missed_daily_habits), style = MaterialTheme.typography.bodySmall)
                } else {
                    missedDailyHabits.take(6).forEach { habit ->
                        Text(
                            text = stringResource(R.string.quest_bulleted_habit, habit.name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onDismiss()
                                onOpenHabit(habit.id)
                            }
                        )
                    }
                    if (missedDailyHabits.size > 6) {
                        Text(
                            stringResource(R.string.quest_more_count, missedDailyHabits.size - 6),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.quest_tip_open_edit),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.quest_close))
            }
        }
    )
}
