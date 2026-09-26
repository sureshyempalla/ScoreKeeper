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
import androidx.compose.material.icons.filled.Edit
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
 *
 * Also doubles as the generic entrant-entry step for any other sport routed
 * into the same Group Stage + Playoffs flow (see App.kt's VolleyballFlow):
 * when [requiresRoster] is false -- e.g. Table Tennis, which is played
 * 1-vs-1 or 2-vs-2 rather than as rostered teams -- each "team" is just a
 * single name (a player, or an already-paired doubles team typed as one
 * entry) with no roster field or minimum-size check at all.
 */
@Composable
fun VolleyballTeamsScreen(
    sportName: String,
    emoji: String,
    minTeamSize: Int,
    requiresRoster: Boolean = true,
    teams: List<EventEntrant>,
    onBack: () -> Unit,
    onAddTeam: (name: String, roster: List<String>) -> Unit,
    onUpdateTeam: (entrantId: String, name: String, roster: List<String>) -> Unit,
    onRemoveTeam: (entrantId: String) -> Unit,
    onContinue: () -> Unit
) {
    val noun = if (requiresRoster) "team" else "player"
    var showAddDialog by remember { mutableStateOf(false) }
    // Non-null while editing an existing team -- tapping a card opens the same
    // dialog as "Add Another Team" but pre-filled, saving through onUpdateTeam
    // instead of onAddTeam.
    var editingTeam by remember { mutableStateOf<EventEntrant?>(null) }
    val allTeamsValid = teams.isNotEmpty() && (!requiresRoster || teams.all { it.roster.size >= minTeamSize })
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
            Text("$emoji $sportName ${if (requiresRoster) "Teams" else "Players"}", style = MaterialTheme.typography.headlineSmall)
        }

        if (requiresRoster) {
            Text(
                "Each team needs at least $minTeamSize players on its roster.",
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                modifier = Modifier.padding(20.dp, 4.dp, 20.dp, 0.dp)
            )
        }

        LazyColumn(
            Modifier.weight(1f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(teams, key = { it.id }) { team ->
                TeamCard(team, minTeamSize, requiresRoster, onEdit = { editingTeam = team }, onRemove = { onRemoveTeam(team.id) })
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
                        Text("Add Another ${noun.replaceFirstChar { it.uppercase() }}", color = Green, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${teams.size} $noun${if (teams.size == 1) "" else "s"} added" +
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

    if (showAddDialog || editingTeam != null) {
        val isEditing = editingTeam != null
        var name by remember { mutableStateOf(editingTeam?.name ?: "") }
        var rosterText by remember { mutableStateOf(editingTeam?.roster?.joinToString(", ") ?: "") }
        fun close() { showAddDialog = false; editingTeam = null }
        AlertDialog(
            onDismissRequest = ::close,
            title = { Text(if (isEditing) "Edit $noun" else "Add a $noun") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (requiresRoster) "Team name" else "Player name") },
                        singleLine = true
                    )
                    if (requiresRoster) {
                        OutlinedTextField(
                            value = rosterText,
                            onValueChange = { rosterText = it },
                            label = { Text("Players (comma separated)") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val roster = if (requiresRoster) {
                            rosterText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        } else {
                            emptyList()
                        }
                        if (trimmedName.isNotBlank()) {
                            val editing = editingTeam
                            if (editing != null) onUpdateTeam(editing.id, trimmedName, roster) else onAddTeam(trimmedName, roster)
                        }
                        close()
                    },
                    enabled = name.isNotBlank()
                ) { Text(if (isEditing) "Save" else "Add") }
            },
            dismissButton = { TextButton(onClick = ::close) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TeamCard(team: EventEntrant, minTeamSize: Int, requiresRoster: Boolean, onEdit: () -> Unit, onRemove: () -> Unit) {
    val valid = !requiresRoster || team.roster.size >= minTeamSize
    Surface(
        onClick = onEdit,
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
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Muted)
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Muted)
                    }
                }
            }
            if (requiresRoster) {
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
}
