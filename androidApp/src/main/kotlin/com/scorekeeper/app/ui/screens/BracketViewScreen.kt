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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.scorekeeper.domain.EventGame
import com.scorekeeper.domain.EventMatch
import com.scorekeeper.domain.EventMatchStatus
import com.scorekeeper.domain.TournamentFormat

/**
 * Bracket / standings, per the wireframe (BracketView.dc.html): elimination
 * formats show rounds grouped smallest-round-last with a Champion card once
 * the final is decided; round robin shows a ranked standings table instead
 * (per the wireframe's own footnote).
 */
@Composable
fun BracketViewScreen(
    game: EventGame,
    standings: List<EntrantStanding>,
    champion: EventEntrant?,
    onBack: () -> Unit,
    onReshuffle: () -> Unit,
    onOpenMatch: (EventMatch) -> Unit
) {
    fun name(id: String?): String = game.entrants.firstOrNull { it.id == id }?.name ?: "TBD"

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
                Text("${game.emoji} ${game.sportName}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${game.format.displayName} · ${game.entrants.size} entrants",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
            }
            Surface(onClick = onReshuffle, shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, Border)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Re-shuffle draw")
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (game.format == TournamentFormat.ROUND_ROBIN) {
                // Round robin still needs its matches listed and tappable -- the standings
                // table alone (the wireframe's headline view for this format) has no way to
                // actually score a match. Caught while build-verifying: with only the table
                // shown, there was no path from "draw generated" to "any match played."
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("MATCHES", style = MaterialTheme.typography.labelMedium, color = Muted)
                        game.matches.sortedBy { it.matchIndex }.forEach { match ->
                            MatchCard(match, ::name, onClick = { onOpenMatch(match) })
                        }
                    }
                }
                item { StandingsTable(standings) }
            } else {
                val roundsInOrder = game.matches.groupBy { it.roundLabel }
                    .entries.sortedByDescending { it.value.size }
                items(roundsInOrder.toList()) { (label, matches) ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Muted)
                        matches.sortedBy { it.matchIndex }.forEach { match ->
                            MatchCard(match, ::name, onClick = { onOpenMatch(match) })
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🏆 CHAMPION", style = MaterialTheme.typography.labelMedium, color = Muted)
                        if (champion != null) {
                            Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.5.dp, Amber)) {
                                Row(
                                    Modifier.fillMaxWidth().padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("🥇", style = MaterialTheme.typography.titleLarge)
                                    Text(champion.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        } else {
                            Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.5.dp, Border)) {
                                Text(
                                    "Awaiting final result",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
                    Text(
                        if (game.format == TournamentFormat.ROUND_ROBIN)
                            "Round Robin: everyone plays everyone once, ranked by wins."
                        else
                            "Tap a match once both sides are set to enter its score.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchCard(match: EventMatch, name: (String?) -> String, onClick: () -> Unit) {
    val playable = match.entrantAId != null && match.entrantBId != null && match.status != EventMatchStatus.COMPLETE
    Surface(
        onClick = onClick,
        enabled = playable,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(if (match.status == EventMatchStatus.LIVE) 1.5.dp else 1.dp, if (match.status == EventMatchStatus.LIVE) Amber else Border)
    ) {
        Column {
            MatchSideRow(name(match.entrantAId), match.scoreA, isWinner = match.winnerEntrantId == match.entrantAId, match.status)
            HorizontalDivider(color = Color(0xFFF1EDE3))
            MatchSideRow(name(match.entrantBId), match.scoreB, isWinner = match.winnerEntrantId == match.entrantBId, match.status)
        }
    }
}

@Composable
private fun MatchSideRow(name: String, score: Int, isWinner: Boolean, status: EventMatchStatus) {
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

@Composable
private fun StandingsTable(standings: List<EntrantStanding>) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
        Column {
            Row(Modifier.background(Color(0xFFF1EDE3)).padding(14.dp, 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rank", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.weight(2f))
                Text("Wins", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Muted)
            }
            standings.forEachIndexed { index, standing ->
                if (index > 0) HorizontalDivider(color = Color(0xFFF1EDE3))
                Row(
                    Modifier.fillMaxWidth().padding(14.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("#${standing.rank}", color = Muted, style = MaterialTheme.typography.labelMedium)
                        Text(standing.entrant.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("${standing.wins}-${standing.losses}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
