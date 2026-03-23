package com.example.habittracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.PaddingValues
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitWithStats
import com.example.habittracker.ui.components.GlassCard

@Composable
fun HabitCard(
    habit: Habit,
    stats: HabitWithStats?,
    isCompletedToday: Boolean,
    onToggleToday: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = runCatching { Color(android.graphics.Color.parseColor(habit.colorHex)) }
        .getOrElse { MaterialTheme.colorScheme.primary }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = habit.name, style = MaterialTheme.typography.titleMedium)
                    if (habit.description.isNotBlank()) {
                        Text(text = habit.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = {}, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("Current: ${stats?.currentStreak ?: 0} 🔥")
                }
                TextButton(onClick = {}, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("Longest: ${stats?.longestStreak ?: 0}")
                }
            }

            Row(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.26f), MaterialTheme.colorScheme.surface)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isCompletedToday, onCheckedChange = onToggleToday)
                Text(
                    text = if (isCompletedToday) "Completed today" else "Mark as done today",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
