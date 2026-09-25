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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.scorekeeper.domain.EventTeamMode
import com.scorekeeper.domain.SportRules
import com.scorekeeper.domain.TournamentFormat

/**
 * Configure Game, per the wireframe (ConfigureGame.dc.html): tournament
 * format (radio-style cards), team mode (segmented control), and a players-
 * per-team stepper shown only for Doubles/Teams.
 *
 * Sports with real team-sport rules ([SportRules.usesGroupStage], e.g.
 * Volleyball) skip this generic picker entirely: the format is forced to
 * [TournamentFormat.GROUP_STAGE_THEN_KNOCKOUT] and team mode to
 * [EventTeamMode.TEAMS], and the flow continues into the dedicated
 * Teams/Groups/Standings screens instead of the generic Add Participants one.
 */
@Composable
fun ConfigureGameScreen(
    sportName: String,
    emoji: String,
    onBack: () -> Unit,
    onContinue: (format: TournamentFormat, teamMode: EventTeamMode, playersPerTeam: Int) -> Unit
) {
    val minTeamSize = SportRules.minTeamSize(sportName)
    val isTeamSport = minTeamSize != null
    var format by remember { mutableStateOf(if (isTeamSport) TournamentFormat.GROUP_STAGE_THEN_KNOCKOUT else TournamentFormat.SINGLE_ELIMINATION) }
    var teamMode by remember { mutableStateOf(if (isTeamSport) EventTeamMode.TEAMS else EventTeamMode.SINGLES) }
    var playersPerTeam by remember { mutableStateOf(minTeamSize ?: 2) }

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
            Text("$emoji $sportName Setup", style = MaterialTheme.typography.headlineSmall)
        }

        if (isTeamSport) {
            LazyColumn(
                Modifier.weight(1f).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Team Sport", style = MaterialTheme.typography.labelMedium, color = Muted)
                            Text(
                                "$sportName is played in teams of at least $minTeamSize. Next you'll add teams and their rosters, split them into groups, and play a round-robin group stage before knockout playoffs decide the champion.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        } else {
        LazyColumn(
            Modifier.weight(1f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "TOURNAMENT FORMAT",
                        style = MaterialTheme.typography.labelMedium,
                        color = Muted,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    TournamentFormat.entries.forEach { option ->
                        FormatCard(option, selected = format == option, onClick = { format = option })
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TEAM MODE", style = MaterialTheme.typography.labelMedium, color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        EventTeamMode.entries.forEach { mode ->
                            val selected = mode == teamMode
                            Surface(
                                onClick = { teamMode = mode; if (mode == EventTeamMode.SINGLES) playersPerTeam = 1 else if (playersPerTeam <= 1) playersPerTeam = 2 },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) Green else Color(0xFFF1EDE3),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        mode.displayName,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                    if (teamMode != EventTeamMode.SINGLES) {
                        Row(
                            Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp))
                                .then(Modifier).padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Players per team", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StepperDot("−") { if (playersPerTeam > 2) playersPerTeam-- }
                                Text(playersPerTeam.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                StepperDot("+") { playersPerTeam++ }
                            }
                        }
                    }
                    Text(
                        "Add players next — they'll be entered as the entrants for this game's draw.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        }

        Column(Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp)) {
            Button(
                onClick = { onContinue(format, teamMode, playersPerTeam) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FormatCard(format: TournamentFormat, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Green else Border),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(20.dp)
                    .background(if (selected) Green else Color.Transparent, CircleShape)
                    .then(
                        if (selected) Modifier
                        else Modifier.border(2.dp, Border, CircleShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            Column {
                Text(format.displayName, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(format.blurb, style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.padding(top = 2.dp))
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
