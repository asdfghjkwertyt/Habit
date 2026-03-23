package com.example.habittracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import com.example.habittracker.ui.components.adaptiveContentPadding
import com.example.habittracker.ui.viewmodel.HabitUiStatus
import com.example.habittracker.ui.viewmodel.HabitUiState
import com.example.habittracker.util.DateUtils
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun CalendarScreen(
    state: HabitUiState,
    onSelectHabit: (Long) -> Unit,
    onToggle: (Long, Long, Boolean) -> Unit,
    onSaveNote: (Long, Long, String?) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit
) {
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val visibleMonth = remember(monthOffset) { YearMonth.now().plusMonths(monthOffset.toLong()) }
    var quickActionDay by rememberSaveable { mutableLongStateOf(Long.MIN_VALUE) }
    var quickActionIsCompleted by rememberSaveable { mutableStateOf(false) }
    var showQuickActions by rememberSaveable { mutableStateOf(false) }
    var showNoteDialog by rememberSaveable { mutableStateOf(false) }
    var noteDraft by rememberSaveable { mutableStateOf("") }

    val selectedStat = state.stats.firstOrNull { it.habit.id == state.selectedHabitId }
    val completedDays = selectedStat?.completedDays ?: emptySet()
    val monthCells = remember(visibleMonth) { buildMonthCells(visibleMonth) }
    val monthTitle = remember(visibleMonth) { visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")) }

    val monthStart = remember(visibleMonth) { visibleMonth.atDay(1).toEpochDay() }
    val monthEnd = remember(visibleMonth) { visibleMonth.atEndOfMonth().toEpochDay() }
    val completedInMonth = remember(completedDays, monthStart, monthEnd) {
        completedDays.filter { it in monthStart..monthEnd }.sortedDescending()
    }
    val selectedHabitId = selectedStat?.habit?.id
    val notesByDay = remember(state.completions, selectedHabitId) {
        state.completions
            .asSequence()
            .filter { completion ->
                completion.habitId == selectedHabitId && !completion.note.isNullOrBlank()
            }
            .associate { completion -> completion.epochDay to completion.note.orEmpty() }
    }

    val historyDays = remember(completedInMonth, notesByDay, monthStart, monthEnd) {
        val noteDays = notesByDay.keys.filter { it in monthStart..monthEnd }.toSet()

        (completedInMonth.toSet() + noteDays).toList().sortedDescending()
    }
    val weeklyProgress = remember(monthCells, completedDays) {
        buildWeekProgress(monthCells, completedDays)
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Calendar")
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
                    Text("Loading calendar...", modifier = Modifier.padding(top = 16.dp))
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
                    Text("Unable to load calendar", style = MaterialTheme.typography.headlineSmall)
                    Text(state.errorMessage ?: "Unknown error")
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Retry")
                    }
                }
            }

            HabitUiStatus.Success -> {
                val contentPadding = adaptiveContentPadding(vertical = 18.dp)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .pullRefresh(pullRefreshState)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("Choose habit", style = MaterialTheme.typography.titleMedium)
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.habits, key = { it.id }) { habit ->
                                    AssistChip(
                                        onClick = { onSelectHabit(habit.id) },
                                        label = { Text(habit.name) }
                                    )
                                }
                            }
                        }

                        item {
                            MonthHeader(
                                title = monthTitle,
                                onPrev = { monthOffset -= 1 },
                                onNext = { monthOffset += 1 },
                                onToday = { monthOffset = 0 }
                            )
                        }

                        item {
                            CalendarLegend()
                        }

                        if (selectedStat == null) {
                            item { Text("Add and select a habit to view calendar history.") }
                        } else {
                            item {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        WeekdayHeader()

                                        for (week in monthCells.chunked(7)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                week.forEach { cellDate ->
                                                    val dayEpoch = cellDate?.toEpochDay()
                                                    val isDone = dayEpoch != null && completedDays.contains(dayEpoch)
                                                    val isToday = cellDate == LocalDate.now()
                                                    val isInVisibleMonth = cellDate?.month == visibleMonth.month

                                                    DayCell(
                                                        modifier = Modifier.weight(1f),
                                                        date = cellDate,
                                                        isCompleted = isDone,
                                                        isToday = isToday,
                                                        enabled = isInVisibleMonth,
                                                        hasNote = selectedHabitId?.let { habitId ->
                                                            dayEpoch?.let { day ->
                                                                notesByDay.containsKey(day)
                                                            } ?: false
                                                        } == true,
                                                        onClick = {
                                                            if (cellDate != null && isInVisibleMonth) {
                                                                onToggle(selectedStat.habit.id, cellDate.toEpochDay(), !isDone)
                                                            }
                                                        },
                                                        onLongClick = {
                                                            if (cellDate != null && isInVisibleMonth) {
                                                                quickActionDay = cellDate.toEpochDay()
                                                                quickActionIsCompleted = isDone
                                                                showQuickActions = true
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (weeklyProgress.isNotEmpty()) {
                                item {
                                    Text("Weekly momentum", style = MaterialTheme.typography.titleMedium)
                                }
                                item {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(weeklyProgress, key = { it.weekNumber }) { summary ->
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Week ${summary.weekNumber}",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    Text(
                                                        text = "${summary.completed}/${summary.trackableDays} days",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text("History", style = MaterialTheme.typography.titleMedium)
                            }

                            if (historyDays.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        )
                                    ) {
                                        Text(
                                            text = "No completions in $monthTitle yet.",
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            } else {
                                items(historyDays, key = { it }) { day ->
                                    val isCompleted = completedDays.contains(day)
                                    val note = notesByDay[day]
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = DateUtils.formatEpochDay(day),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = if (isCompleted) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                                Text(
                                                    text = if (isCompleted) "Completed" else "Missed",
                                                    color = if (isCompleted) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }

                                        if (!note.isNullOrBlank()) {
                                            Text(
                                                text = "Note: $note",
                                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

    if (showQuickActions && selectedHabitId != null && quickActionDay != Long.MIN_VALUE) {
        AlertDialog(
            onDismissRequest = { showQuickActions = false },
            title = { Text("Quick actions") },
            text = {
                Text("${DateUtils.formatEpochDay(quickActionDay)} for ${selectedStat?.habit?.name.orEmpty()}")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        onToggle(selectedHabitId, quickActionDay, true)
                        showQuickActions = false
                    }) {
                        Text("Mark complete")
                    }
                    TextButton(onClick = {
                        onToggle(selectedHabitId, quickActionDay, false)
                        showQuickActions = false
                    }) {
                        Text("Mark missed")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        onToggle(selectedHabitId, quickActionDay, !quickActionIsCompleted)
                        showQuickActions = false
                    }) {
                        Text("Undo")
                    }
                    TextButton(onClick = {
                        noteDraft = notesByDay[quickActionDay].orEmpty()
                        showQuickActions = false
                        showNoteDialog = true
                    }) {
                        Text("Add note")
                    }
                }
            }
        )
    }

    if (showNoteDialog && selectedHabitId != null && quickActionDay != Long.MIN_VALUE) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Day note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(DateUtils.formatEpochDay(quickActionDay))
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        label = { Text("Add note") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveNote(selectedHabitId, quickActionDay, noteDraft)
                    showNoteDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MonthHeader(
    title: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous month")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next month")
            }
        }
        OutlinedButton(onClick = onToday) {
            Text("Today")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekdays.forEach { dayLabel ->
            Text(
                text = dayLabel,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarLegend() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Legend", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendPill(label = "Completed", color = MaterialTheme.colorScheme.primary)
                LegendPill(label = "Today", color = MaterialTheme.colorScheme.primaryContainer)
                LegendPill(label = "Note", color = MaterialTheme.colorScheme.tertiaryContainer)
            }
        }
    }
}

@Composable
private fun LegendPill(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .height(12.dp)
                .fillMaxWidth(0.06f)
                .clip(MaterialTheme.shapes.small)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RowScope.DayCell(
    modifier: Modifier = Modifier,
    date: LocalDate?,
    isCompleted: Boolean,
    isToday: Boolean,
    enabled: Boolean,
    hasNote: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val baseModifier = modifier
        .height(44.dp)
        .clip(MaterialTheme.shapes.medium)

    if (date == null) {
        Box(modifier = baseModifier)
        return
    }

    val colors = MaterialTheme.colorScheme
    val targetTextColor = when {
        !enabled -> colors.onSurface.copy(alpha = 0.3f)
        isCompleted -> colors.onPrimary
        isToday -> colors.primary
        else -> colors.onSurface
    }
    val textColor by animateColorAsState(targetValue = targetTextColor, label = "dayCellText")

    val targetBackground = when {
        isCompleted -> colors.primary
        hasNote -> colors.tertiaryContainer
        isToday -> colors.primaryContainer
        else -> Color.Transparent
    }
    val animatedBackground by animateColorAsState(targetValue = targetBackground, label = "dayCellBg")
    val animatedScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.04f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "dayCellScale"
    )

    Box(
        modifier = baseModifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(animatedBackground)
            .then(
                if (isToday && !isCompleted) {
                    Modifier.border(width = 1.dp, color = colors.primary, shape = MaterialTheme.shapes.medium)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || isCompleted) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

private val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun buildMonthCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingBlanks = max(0, firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value)

    val cells = mutableListOf<LocalDate?>()
    repeat(leadingBlanks) { cells += null }
    for (day in 1..month.lengthOfMonth()) {
        cells += month.atDay(day)
    }

    val trailingBlanks = (7 - (cells.size % 7)) % 7
    repeat(trailingBlanks) { cells += null }
    return cells
}

private data class WeekProgress(
    val weekNumber: Int,
    val completed: Int,
    val trackableDays: Int
)

private fun buildWeekProgress(monthCells: List<LocalDate?>, completedDays: Set<Long>): List<WeekProgress> {
    return monthCells
        .chunked(7)
        .mapIndexedNotNull { index, week ->
            val trackable = week.filterNotNull()
            if (trackable.isEmpty()) {
                null
            } else {
                val completed = trackable.count { completedDays.contains(it.toEpochDay()) }
                WeekProgress(
                    weekNumber = index + 1,
                    completed = completed,
                    trackableDays = trackable.size
                )
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
