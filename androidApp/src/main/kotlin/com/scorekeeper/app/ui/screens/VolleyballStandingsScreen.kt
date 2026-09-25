package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.scorekeeper.app.ui.theme.Amber
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.EntrantStanding
import com.scorekeeper.domain.EventEntrant
import com.scorekeeper.domain.EventMatch
import com.scorekeeper.domain.EventMatchStatus

/**
 * Volleyball Standings, per the wireframe (VolleyballStandings.dc.html): a
 * per-group points table (win count, head-to-head tiebreak already applied
 * by EventStandings.groupStandings) plus that group's matches, with an
 * "Advance to Playoffs" action gated until every group-stage match is done.
 */
@Composable
fun VolleyballStandingsScreen(
    sportName: String,
    emoji: String,
    groupLabels: List<String>,
    entrants: List<EventEntrant>,
    matches: List<EventMatch>,
    standingsForGroup: (String) -> List<EntrantStanding>,
    onBack: () -> Unit,
    onOpenMatch: (EventMatch) -> Unit,
    onAdvance: () -> Unit
) {
    if (groupLabels.isEmpty()) return
    var selectedGroup by remember { mutableStateOf(groupLabels.first()) }
    fun name(id: String?): String = entrants.firstOrNull { it.id == id }?.name ?: "TBD"

    val allGroupMatchesComplete = matches.isNotEmpty() && matches.all { it.status == EventMatchStatus.COMPLETE }

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
            Text("$emoji $sportName Standings", style = MaterialTheme.typography.headlineSmall)
        }

        Row(
            Modifier.padding(20.dp, 14.dp, 20.dp, 0.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupLabels.forEach { label ->
                val selected = label == selectedGroup
                Surface(
                    onClick = { selectedGroup = label },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) Green else Color(0xFFF1EDE3)
                ) {
                    Text(
                        "Group $label",
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(16.dp, 8.dp)
                    )
                }
            }
        }

        val groupMatches = matches.filter { it.roundLabel == "Group $selectedGroup" }.sortedBy { it.matchIndex }
        LazyColumn(
            Modifier.weight(1f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { GroupStandingsTable(standingsForGroup(selectedGroup)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MATCHES", style = MaterialTheme.typography.labelMedium, color = Muted)
                    groupMatches.forEach { match ->
                        GroupMatchCard(match, ::name, onClick = { onOpenMatch(match) })
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!allGroupMatchesComplete) {
                Text(
                    "Finish every group match before advancing to playoffs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            Button(
                onClick = onAdvance,
                enabled = allGroupMatchesComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Advance to Playoffs →", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GroupStandingsTable(standings: List<EntrantStanding>) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
        Column {
            Row(Modifier.background(Color(0xFFF1EDE3)).padding(14.dp, 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rank", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.weight(2f))
                Text("Team", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.weight(3f))
                Text("W-L", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Muted)
            }
            standings.forEachIndexed { index, standing ->
                if (index > 0) HorizontalDivider(color = Color(0xFFF1EDE3))
                val isFirst = standing.rank == 1
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (isFirst) Color(0xFFFDF3E1) else Color.Transparent)
                        .padding(14.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("#${standing.rank}", color = if (isFirst) Color(0xFFB9822B) else Muted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2f))
                    Text(standing.entrant.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(3f))
                    Text("${standing.wins}-${standing.losses}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun GroupMatchCard(match: EventMatch, name: (String?) -> String, onClick: () -> Unit) {
    val playable = match.entrantAId != null && match.entrantBId != null && match.status != EventMatchStatus.COMPLETE
    Surface(
        onClick = onClick,
        enabled = playable,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(if (match.status == EventMatchStatus.LIVE) 1.5.dp else 1.dp, if (match.status == EventMatchStatus.LIVE) Amber else Border)
    ) {
        Column {
            GroupMatchSideRow(name(match.entrantAId), match.scoreA, isWinner = match.winnerEntrantId == match.entrantAId, match.status)
            HorizontalDivider(color = Color(0xFFF1EDE3))
            GroupMatchSideRow(name(match.entrantBId), match.scoreB, isWinner = match.winnerEntrantId == match.entrantBId, match.status)
        }
    }
}

@Composable
private fun GroupMatchSideRow(name: String, score: Int, isWinner: Boolean, status: EventMatchStatus) {
    Row(
        Modifier.fillMaxWidth().padding(14.dp, 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            color = if (name == "TBD") Muted else MaterialTheme.colorScheme.onSurface
        )
        when {
            status == EventMatchStatus.COMPLETE -> Text(
                score.toString(),
                fontWeight = FontWeight.Bold,
                color = if (isWinner) Green else Muted
            )
            status == EventMatchStatus.LIVE -> Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFFDF3E1)) {
                Text(
                    "LIVE",
                    color = Color(0xFFB9822B),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            else -> Text("vs", color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
