package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.app.ui.util.formatEventDateLong
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private val coverEmojis = listOf("🪔", "🎆", "🏆", "🎉", "☀️")

/** A [Long] as returned by Compose's [DatePicker] (UTC millis) -> the local-timezone start-of-day millis this app stores dates as. */
private fun pickerMillisToLocalDayMillis(pickerMillis: Long): Long {
    val date = kotlinx.datetime.Instant.fromEpochMilliseconds(pickerMillis).toLocalDateTime(TimeZone.UTC).date
    return date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

private fun localDayMillisToDate(millis: Long): LocalDate =
    kotlinx.datetime.Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date

/**
 * New Event, per the wireframe (CreateEvent.dc.html): pick a cover emoji,
 * name it, and set a start date (defaulting to today) with an optional end
 * date for multi-day events, via a real Material3 date picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    onCreate: (name: String, emoji: String, dateMillis: Long, endDateMillis: Long?, location: String?) -> Unit
) {
    var selectedEmoji by remember { mutableStateOf(coverEmojis.first()) }
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        dateMillis = pickerMillisToLocalDayMillis(picked)
                        // Keep the end date valid: it can't be before the new start date.
                        endDateMillis?.let { end ->
                            if (localDayMillisToDate(end) < localDayMillisToDate(dateMillis)) endDateMillis = null
                        }
                    }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDateMillis ?: dateMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        val candidate = pickerMillisToLocalDayMillis(picked)
                        endDateMillis = if (localDayMillisToDate(candidate) < localDayMillisToDate(dateMillis)) {
                            dateMillis
                        } else {
                            candidate
                        }
                    }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }

    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(
            Modifier.padding(20.dp, 24.dp, 20.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(onClick = onBack, shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, Border)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Back")
                }
            }
            Text("New Event", style = MaterialTheme.typography.headlineSmall)
        }

        LazyColumn(
            Modifier.weight(1f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("COVER", style = MaterialTheme.typography.labelMedium, color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        coverEmojis.forEach { emoji ->
                            val selected = emoji == selectedEmoji
                            Surface(
                                onClick = { selectedEmoji = emoji },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) Color(0xFFEAF3EF) else Color.White,
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Green else Border)
                            ) {
                                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                                    Text(emoji, style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("EVENT NAME", style = MaterialTheme.typography.labelMedium, color = Muted)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Diwali 2026") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("START DATE", style = MaterialTheme.typography.labelMedium, color = Muted)
                    Surface(
                        onClick = { showStartPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Text(
                            formatEventDateLong(dateMillis),
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("END DATE (OPTIONAL)", style = MaterialTheme.typography.labelMedium, color = Muted)
                    Surface(
                        onClick = { showEndPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                endDateMillis?.let { formatEventDateLong(it) } ?: "Same as start date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (endDateMillis == null) Muted else Color.Unspecified
                            )
                            if (endDateMillis != null) {
                                TextButton(onClick = { endDateMillis = null }) {
                                    Text("Clear", color = Green)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LOCATION (OPTIONAL)", style = MaterialTheme.typography.labelMedium, color = Muted)
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        placeholder = { Text("e.g. Community Hall") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFEAF3EF)) {
                    Text(
                        "Next, you'll add the games/sports for this event — Volleyball, Table Tennis, Carrom, or anything else — one at a time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.scorekeeper.app.ui.theme.GreenDark,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp)) {
            Button(
                onClick = {
                    onCreate(
                        name.ifBlank { "New Event" },
                        selectedEmoji,
                        dateMillis,
                        endDateMillis,
                        if (location.isBlank()) null else location
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Create Event", fontWeight = FontWeight.Bold)
            }
        }
    }
}
