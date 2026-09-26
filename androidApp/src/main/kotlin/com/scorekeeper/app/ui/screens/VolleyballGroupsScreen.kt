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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.EventEntrant

/**
 * Volleyball Groups, per the wireframe (VolleyballGroups.dc.html): the app's
 * auto-split suggestion (evenly dealt across N groups, e.g. 9 teams -> 5/4)
 * shown as editable group lists -- the organizer can move any team to a
 * different group before confirming and generating the group-stage draw.
 */
@Composable
fun VolleyballGroupsScreen(
    sportName: String,
    emoji: String,
    teams: List<EventEntrant>,
    groupCount: Int,
    onGroupCountChange: (Int) -> Unit,
    onMoveTeam: (entrantId: String, groupLabel: String) -> Unit,
    onBack: () -> Unit,
    onEditTeams: () -> Unit,
    onConfirm: () -> Unit
) {
    val allLabels = ('A' until 'A' + groupCount).map { it.toString() }
    val byGroup = allLabels.associateWith { label -> teams.filter { it.groupLabel == label } }

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
            Column(Modifier.weight(1f)) {
                Text("$emoji $sportName Groups", style = MaterialTheme.typography.headlineSmall)
            }
            TextButton(onClick = onEditTeams) {
                Text("Edit Teams", color = Green, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(20.dp, 10.dp, 20.dp, 0.dp)
                .background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Number of groups", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Bounds: at least 1 group, at most one group per 2 teams (keeps every
                // group at 2+ teams). With few teams this previously pinned min==max at
                // a hardcoded 2, so neither button could ever do anything -- the reported
                // "+/- doesn't visibly change the number" bug.
                val maxGroups = maxOf(1, teams.size / 2)
                StepperDot("−") { if (groupCount > 1) onGroupCountChange(groupCount - 1) }
                Text(groupCount.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                StepperDot("+") { if (groupCount < maxGroups) onGroupCountChange(groupCount + 1) }
            }
        }

        LazyColumn(
            Modifier.weight(1f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(allLabels) { label ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GROUP $label", style = MaterialTheme.typography.labelMedium, color = Muted)
                    val groupTeams = byGroup[label].orEmpty()
                    if (groupTeams.isEmpty()) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
                            Text(
                                "No teams yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        groupTeams.forEach { team ->
                            GroupTeamRow(team, allLabels, currentLabel = label, onMoveTeam = onMoveTeam)
                        }
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Uneven counts are normal -- e.g. 9 teams split 5/4. Move any team into a different group before confirming.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted
                        )
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp)) {
            Button(
                onClick = onConfirm,
                enabled = teams.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Confirm Groups & Start Draw", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GroupTeamRow(
    team: EventEntrant,
    allLabels: List<String>,
    currentLabel: String,
    onMoveTeam: (entrantId: String, groupLabel: String) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp, 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(team.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                if (team.roster.isNotEmpty()) {
                    Text("${team.roster.size} players", style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
            Box {
                TextButton(onClick = { menuOpen = true }) {
                    Text("Move")
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    allLabels.filter { it != currentLabel }.forEach { targetLabel ->
                        DropdownMenuItem(
                            text = { Text("Move to Group $targetLabel") },
                            onClick = {
                                onMoveTeam(team.id, targetLabel)
                                menuOpen = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperDot(symbol: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Cream, border = BorderStroke(1.dp, Border)) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            Text(symbol, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
