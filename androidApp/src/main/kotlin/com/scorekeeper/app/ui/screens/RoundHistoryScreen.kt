package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
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
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.Danger
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.RoundOutcome

/**
 * Score Log, per the wireframe (RoundHistory.dc.html): a round-by-round
 * grid, one column per player, with running totals and a badge for
 * mid-hand/full-count outcomes. Read-only for now -- "tap to correct" from
 * the wireframe is a follow-up.
 */
@Composable
fun RoundHistoryScreen(session: GameSession, onBack: () -> Unit) {
    val players = session.players
    val rounds = (1..(session.rounds.maxOfOrNull { it.roundNumber } ?: 0))

    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(
            Modifier.padding(20.dp, 24.dp, 20.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(onClick = onBack, shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, Border)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Back")
                }
            }
            Text("Score Log", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Round-by-round breakdown",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            modifier = Modifier.padding(20.dp, 10.dp, 20.dp, 0.dp)
        )

        Column(Modifier.weight(1f).padding(20.dp)) {
            val scroll = rememberScrollState()
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.horizontalScroll(scroll)
            ) {
                Column {
                    Row(Modifier.background(Color(0xFFF1EDE3)).padding(vertical = 10.dp)) {
                        HeaderCell("Rnd", 56.dp)
                        players.forEach { player ->
                            HeaderCell(player.name, 84.dp)
                        }
                    }
                    rounds.forEach { roundNumber ->
                        val isEven = roundNumber % 2 == 0
                        Row(
                            Modifier.fillMaxWidth().background(if (isEven) Color(0xFFFAF9F5) else Color.White).padding(vertical = 10.dp)
                        ) {
                            Box(Modifier.width(56.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                                Text(roundNumber.toString(), fontWeight = FontWeight.SemiBold, color = Muted)
                            }
                            players.forEach { player ->
                                val round = session.rounds.firstOrNull { it.playerId == player.id && it.roundNumber == roundNumber }
                                RoundCell(round?.rawScore, round?.outcome)
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFFF1EDE3)).padding(vertical = 12.dp)
                    ) {
                        Box(Modifier.width(56.dp).padding(horizontal = 8.dp)) {
                            Text("Total", fontWeight = FontWeight.Bold)
                        }
                        players.forEach { player ->
                            val total = session.rounds.filter { it.playerId == player.id }.sumOf { it.rawScore }
                            Box(Modifier.width(84.dp), contentAlignment = Alignment.Center) {
                                Text(total.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LegendDot(Color(0xFFFBEAE8), Danger, "Full count")
                LegendDot(Color(0xFFFDF3E1), Color(0xFFB9822B), "Middle drop")
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Box(Modifier.width(width).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Muted)
    }
}

@Composable
private fun RoundCell(rawScore: Int?, outcome: RoundOutcome?) {
    val (bg, labelColor, tag) = when (outcome) {
        RoundOutcome.FULL_COUNT -> Triple(Color(0xFFFBEAE8), Danger, "FULL")
        RoundOutcome.MIDDLE_DROP -> Triple(Color(0xFFFDF3E1), Color(0xFFB9822B), "MID")
        else -> Triple(Color.Transparent, MaterialTheme.colorScheme.onSurface, null)
    }
    Box(Modifier.width(84.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    rawScore?.toString() ?: "—",
                    fontWeight = FontWeight.SemiBold,
                    color = if (tag != null) labelColor else MaterialTheme.colorScheme.onSurface
                )
                if (tag != null) {
                    Text(tag, style = MaterialTheme.typography.labelSmall, color = labelColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(bg: Color, borderColor: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(8.dp).background(bg, CircleShape).border(1.dp, borderColor, CircleShape)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}
