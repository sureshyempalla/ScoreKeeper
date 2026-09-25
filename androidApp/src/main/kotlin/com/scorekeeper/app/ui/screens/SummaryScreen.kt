package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.Amber
import com.scorekeeper.app.ui.theme.AvatarColors
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.Danger
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.PlayerStanding
import com.scorekeeper.scoring.ScoringEngine

private val medals = listOf("🥇", "🥈", "🥉")

/**
 * Game Summary, per the wireframe (Summary.dc.html): a felt-green header
 * banner announcing the winner, a ranked standings list with medals for
 * the top three, and a footer with Share / Rematch / Home actions.
 */
@Composable
fun SummaryScreen(
    session: GameSession,
    onBackToHome: () -> Unit,
    onRematch: () -> Unit,
    onViewHistory: () -> Unit
) {
    val engine = ScoringEngine.forGameType(session.gameType)
    val standings = engine.standings(session.players, session.rounds, session.rules).sortedBy { it.rank }
    val winner = standings.firstOrNull { !it.isEliminated } ?: standings.minByOrNull { it.rank }

    Column(Modifier.fillMaxSize().background(Cream)) {
        Column(
            Modifier.fillMaxWidth().background(Green).padding(20.dp, 36.dp, 20.dp, 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Amber, modifier = Modifier.size(34.dp))
            Text(
                "${session.name} · ${session.rounds.maxOfOrNull { it.roundNumber } ?: 0} rounds",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "${winner?.player?.name ?: "—"} Wins! 🎉",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        LazyColumn(
            Modifier.weight(1f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(standings, key = { it.player.id }) { standing ->
                StandingCard(standing, session.rounds.filter { it.playerId == standing.player.id }.maxOfOrNull { it.roundNumber })
            }
            item {
                Text(
                    "View full score log →",
                    style = MaterialTheme.typography.bodySmall,
                    color = Green,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp).clickable(onClick = onViewHistory)
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = Amber, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = Color(0xFF3A2A05), modifier = Modifier.size(16.dp))
                    Text(
                        "Share Results",
                        color = Color(0xFF3A2A05),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(onClick = onRematch, shape = RoundedCornerShape(14.dp), color = Color(0xFFF1EDE3), modifier = Modifier.weight(1f)) {
                    Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("Rematch", fontWeight = FontWeight.SemiBold)
                    }
                }
                Surface(onClick = onBackToHome, shape = RoundedCornerShape(14.dp), color = Color(0xFFF1EDE3), modifier = Modifier.weight(1f)) {
                    Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("Home", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingCard(standing: PlayerStanding, lastRound: Int?) {
    val isTop3 = standing.rank <= 3 && !standing.isEliminated
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(if (standing.rank == 1 && !standing.isEliminated) 1.5.dp else 1.dp, if (standing.rank == 1 && !standing.isEliminated) Amber else Border)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp).then(if (standing.isEliminated) Modifier else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTop3) {
                Text(medals[standing.rank - 1], style = MaterialTheme.typography.titleLarge)
            } else {
                Text(
                    standing.rank.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (standing.isEliminated) Danger else Muted,
                    modifier = Modifier.size(20.dp)
                )
            }
            val color = AvatarColors[standing.player.orderIndex.mod(AvatarColors.size)]
            Box(
                Modifier.size(36.dp).padding(start = 12.dp).clip(CircleShape).background(if (standing.isEliminated) Danger else color),
                contentAlignment = Alignment.Center
            ) {
                Text(standing.player.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.weight(1f).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    standing.player.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (standing.isEliminated && lastRound != null) {
                    Text(
                        "  OUT R$lastRound",
                        style = MaterialTheme.typography.labelSmall,
                        color = Danger,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text("${standing.total} pts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
