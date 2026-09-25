package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.Danger
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.EventEntrant
import androidx.compose.ui.graphics.Color

/**
 * Volleyball Teams, per the wireframe (VolleyballTeams.dc.html): each team is
 * its own card with a named roster (not a flat player list auto-split), a
 * live minimum-roster-size check, and a "+ Add Another Team" affordance so
 * the organizer can add as many teams as the event actually has.
 */
@Composable
fun VolleyballTeamsScreen(
    sportName: String,
    emoji: String,
    minTeamSize: Int,
    teams: List<EventEntrant>,
    onBack: () -> Unit,
    onAddTeam: (name: String, roster: List<String>) -> Unit,
    onRemoveTeam: (entrantId: String) -> Unit,
    onContinue: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val allTeamsValid = teams.isNotEmpty() && teams.all { it.roster.size >= minTeamSize }
    val canContinue = teams.size >= 2 && allTeamsValid

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
            Text("$emoji $sportName Teams", style = MaterialTheme.typography.headlineSmall)
        }

        Text(
            "Each team needs at least $minTeamSize players on its roster.",
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
            modifier = Modifier.padding(20.dp, 4.dp, 20.dp, 0.dp)
        )

        LazyColumn(
            Modifier.weight(1f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(teams, key = { it.id }) { team ->
                TeamCard(team, minTeamSize, onRemove = { onRemoveTeam(team.id) })
            }
            item {
                Surface(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, Green)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Green)
                        Text("Add Another Team", color = Green, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${teams.size} team${if (teams.size == 1) "" else "s"} added" +
                    if (teams.size < 2) " · need at least 2" else if (!allTeamsValid) " · fix rosters below $minTeamSize" else "",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Button(
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Continue to Groups", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var rosterText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add a team") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Team name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rosterText,
                        onValueChange = { rosterText = it },
                        label = { Text("Players (comma separated)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val roster = rosterText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        if (trimmedName.isNotBlank()) {
                            onAddTeam(trimmedName, roster)
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

@Composable
private fun TeamCard(team: EventEntrant, minTeamSize: Int, onRemove: () -> Unit) {
    val valid = team.roster.size >= minTeamSize
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(if (valid) 1.dp else 1.5.dp, if (valid) Border else Danger)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(team.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove team", tint = Muted)
                }
            }
            Text(
                if (team.roster.isEmpty()) "No players yet" else team.roster.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = if (valid) Muted else Danger,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "${team.roster.size} / $minTeamSize players" + if (!valid) " · needs ${minTeamSize - team.roster.size} more" else "",
                style = MaterialTheme.typography.labelSmall,
                color = if (valid) Green else Danger,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
