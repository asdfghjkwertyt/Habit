package com.example.habittracker.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.habittracker.domain.model.Habit
import com.example.habittracker.domain.model.HabitFrequency
import java.util.Locale
import kotlinx.coroutines.launch

private data class HabitColorOption(
    val name: String,
    val hex: String,
    val swatchSize: Dp = 22.dp
)

private val presetColors = listOf(
    HabitColorOption(name = "Forest Green", hex = "#2E7D32"),
    HabitColorOption(name = "Ocean Blue", hex = "#1565C0"),
    HabitColorOption(name = "Royal Purple", hex = "#6A1B9A"),
    HabitColorOption(name = "Crimson Red", hex = "#C62828"),
    HabitColorOption(name = "Sunset Orange", hex = "#EF6C00"),
    HabitColorOption(name = "Slate Gray", hex = "#37474F"),
    HabitColorOption(name = "Rose Pink", hex = "#D81B60"),
    HabitColorOption(name = "Teal", hex = "#00897B"),
    HabitColorOption(name = "Sky Blue", hex = "#039BE5"),
    HabitColorOption(name = "Amber", hex = "#FFB300"),
    HabitColorOption(name = "Lime", hex = "#7CB342"),
    HabitColorOption(name = "Deep Indigo", hex = "#3949AB")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitFormScreen(
    existing: Habit?,
    onSave: (
        name: String,
        description: String,
        frequency: HabitFrequency,
        colorHex: String,
        reminderEnabled: Boolean,
        reminderHour: Int?,
        reminderMinute: Int?,
        reminderMessage: String?
    ) -> Unit,
    onUpdate: (Habit) -> Unit,
    onTestReminder: (habitName: String, reminderMessage: String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var selectedFrequency by remember { mutableStateOf(existing?.frequency ?: HabitFrequency.DAILY) }
    var selectedColorHex by remember { mutableStateOf(existing?.colorHex ?: "#2E7D32") }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: false) }
    var reminderHour by remember { mutableStateOf(existing?.reminderHour ?: 20) }
    var reminderMinute by remember { mutableStateOf(existing?.reminderMinute ?: 0) }
    var reminderMessage by remember { mutableStateOf(existing?.reminderMessage.orEmpty()) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    val selectedColorName = presetColors
        .firstOrNull { it.hex.equals(selectedColorHex, ignoreCase = true) }
        ?.name ?: selectedColorHex

    val isNameValid = name.trim().isNotEmpty()
    val saveEnabled = isNameValid

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (existing == null) "Add Habit" else "Edit Habit") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Habit name") },
                isError = !isNameValid && name.isNotEmpty(),
                supportingText = {
                    if (!isNameValid && name.isNotEmpty()) {
                        Text("Name cannot be empty")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = !frequencyExpanded }
            ) {
                OutlinedTextField(
                    value = selectedFrequency.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors()
                )

                DropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false }
                ) {
                    HabitFrequency.entries.forEach { frequency ->
                        DropdownMenuItem(
                            text = { Text(frequency.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                selectedFrequency = frequency
                                frequencyExpanded = false
                            }
                        )
                    }
                }
            }

            Text("Color", style = MaterialTheme.typography.labelLarge)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 420.dp) 3 else 2
                val chipRows = presetColors.chunked(columns)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    chipRows.forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowOptions.forEach { option ->
                                val selected = selectedColorHex.equals(option.hex, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedColorHex = option.hex },
                                    modifier = Modifier
                                        .weight(1f)
                                        .widthIn(min = 0.dp),
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(option.swatchSize)
                                                    .background(
                                                        Color(android.graphics.Color.parseColor(option.hex)),
                                                        shape = MaterialTheme.shapes.small
                                                    )
                                            )
                                            Text(option.name, style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    colors = FilterChipDefaults.filterChipColors()
                                )
                            }

                            repeat(columns - rowOptions.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(
                        color = Color(android.graphics.Color.parseColor(selectedColorHex)).copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Selected color: $selectedColorName", style = MaterialTheme.typography.labelMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily reminder", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (reminderEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it }
                )
            }

            if (reminderEnabled) {
                Button(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minuteOfHour ->
                                reminderHour = hourOfDay
                                reminderMinute = minuteOfHour
                            },
                            reminderHour,
                            reminderMinute,
                            true
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reminder time: ${formatTime(reminderHour, reminderMinute)}")
                }

                OutlinedTextField(
                    value = reminderMessage,
                    onValueChange = { reminderMessage = it },
                    label = { Text("Custom reminder message") },
                    placeholder = { Text("Time to complete this habit") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onTestReminder(
                            name.trim(),
                            reminderMessage.trim().ifBlank { null }
                        )
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Test reminder sent")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Test Reminder Now")
                }
            }

            Button(
                onClick = {
                    if (existing == null) {
                        onSave(
                            name.trim(),
                            description.trim(),
                            selectedFrequency,
                            selectedColorHex,
                            reminderEnabled,
                            if (reminderEnabled) reminderHour else null,
                            if (reminderEnabled) reminderMinute else null,
                            reminderMessage.trim().ifBlank { null }
                        )
                    } else {
                        onUpdate(
                            existing.copy(
                                name = name.trim(),
                                description = description.trim(),
                                frequency = selectedFrequency,
                                colorHex = selectedColorHex,
                                reminderEnabled = reminderEnabled,
                                reminderHour = if (reminderEnabled) reminderHour else null,
                                reminderMinute = if (reminderEnabled) reminderMinute else null,
                                reminderMessage = reminderMessage.trim().ifBlank { null }
                            )
                        )
                    }
                    onBack()
                },
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (existing == null) "Create Habit" else "Save Changes")
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    return String.format(Locale.US, "%02d:%02d", hour, minute)
}
