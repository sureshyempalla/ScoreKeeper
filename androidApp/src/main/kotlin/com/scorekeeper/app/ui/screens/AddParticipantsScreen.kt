package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.AvatarColors
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.EventTeamMode
import com.scorekeeper.domain.SavedPlayer

/**
 * Add Players/Teams, per the wireframe (AddParticipants.dc.html): pick from
 * the saved-player roster or type new entrant names, then generate a random
 * draw. In Teams/Doubles mode each name entered is already one bracket
 * entrant (e.g. "Team Falcons") rather than an individual to auto-split.
 */
@Composable
fun AddParticipantsScreen(
    sportName: String,
    emoji: String,
    teamMode: EventTeamMode,
    savedPlayers: List<SavedPlayer>,
    onBack: () -> Unit,
    onAddSavedPlayer: (String) -> Unit,
    onGenerateDraw: (List<String>) -> Unit
) {
    val selectedNames = remember { mutableStateListOf<String>() }
    var showAddDialog by remember { mutableStateOf(false) }

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
            Text(
                "$emoji Add ${if (teamMode == EventTeamMode.SINGLES) "Players" else "Teams"}",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        if (selectedNames.isNotEmpty()) {
            Row(
                Modifier.padding(20.dp, 14.dp, 20.dp, 0.dp)
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedNames.forEachIndexed { index, name ->
                    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFEAF3EF)) {
                        Row(
                            Modifier.padding(6.dp, 6.dp, 12.dp, 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val color = AvatarColors[index.mod(AvatarColors.size)]
                            Box(Modifier.size(20.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                                Text(name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(name, color = com.scorekeeper.app.ui.theme.GreenDark, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        Column(Modifier.weight(1f).padding(20.dp)) {
            Text(
                if (teamMode == EventTeamMode.SINGLES) "SAVED PLAYERS" else "SAVED PLAYERS / ADD A TEAM NAME",
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(savedPlayers, key = { it.id }) { player ->
                    val selected = selectedNames.contains(player.name)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable {
                            if (selected) selectedNames.remove(player.name) else selectedNames.add(player.name)
                        }
                    ) {
                        val color = AvatarColors[player.colorIndex.mod(AvatarColors.size)]
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(if (selected) color else Color.White)
                                .border(2.dp, if (selected) color else Border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(player.name.take(1).uppercase(), color = if (selected) Color.White else Muted, fontWeight = FontWeight.SemiBold)
                        }
                        Text(player.name, style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                }
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { showAddDialog = true }
                    ) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFEAF3EF))
                                .border(1.5.dp, Green, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = Green)
                        }
                        Text("New", style = MaterialTheme.typography.labelSmall, color = Green, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${selectedNames.size} ${if (teamMode == EventTeamMode.SINGLES) "players" else "teams"} selected" +
                    if (selectedNames.size < 2) " · need at least 2" else "",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Button(
                onClick = { onGenerateDraw(selectedNames.toList()) },
                enabled = selectedNames.size >= 2,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Generate Random Draw", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (teamMode == EventTeamMode.SINGLES) "Add a player" else "Add a team") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (teamMode == EventTeamMode.SINGLES) "Name" else "Team name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotBlank()) {
                            if (teamMode == EventTeamMode.SINGLES) onAddSavedPlayer(trimmed)
                            selectedNames.add(trimmed)
                        }
                        showAddDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}
