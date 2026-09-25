package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Share
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
import com.scorekeeper.app.ui.theme.GreenDark
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.app.ui.util.formatEventDateShort
import com.scorekeeper.domain.CommunityEvent
import com.scorekeeper.domain.EventEntrant
import com.scorekeeper.domain.EventGame

/**
 * Event Results, per the wireframe (EventResults.dc.html): a green header
 * banner, one champion card per game, and a "most overall wins" summary that
 * tallies champion names across games (entrant ids aren't shared across
 * separate games, so the tally is done by name).
 */
@Composable
fun EventResultsScreen(
    event: CommunityEvent,
    games: List<EventGame>,
    championFor: (EventGame) -> EventEntrant?,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    val champions = games.map { it to championFor(it) }
    val overallWinner = champions
        .mapNotNull { it.second?.name }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }

    Column(Modifier.fillMaxSize().background(Cream)) {
        Column(
            Modifier.fillMaxWidth().background(GreenDark).padding(20.dp, 24.dp, 20.dp, 26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(onClick = onBack, shape = CircleShape, color = Color(0x33FFFFFF)) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Back", tint = Color.White)
                    }
                }
                Text(
                    "${formatEventDateShort(event.dateMillis)} · ${games.size} games",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFCFE6DB)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(event.emoji, style = MaterialTheme.typography.displaySmall)
            Text(
                "${event.name} Results",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            Modifier.weight(1f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("CHAMPIONS", style = MaterialTheme.typography.labelMedium, color = Muted)
            }
            items(champions) { (game, champion) ->
                ChampionCard(game, champion)
            }
            if (overallWinner != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFDF3E1),
                        border = BorderStroke(1.5.dp, Amber)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text(
                                "MOST OVERALL WINS",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF8A6420)
                            )
                            Row(
                                Modifier.padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🏆", style = MaterialTheme.typography.headlineSmall)
                                Column {
                                    Text(
                                        overallWinner.key,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "${overallWinner.value} of ${games.size} games won",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8A6420)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp)) {
            Surface(
                onClick = onShare,
                shape = RoundedCornerShape(14.dp),
                color = Amber,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(vertical = 14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = Color(0xFF3A2A05), modifier = Modifier.size(18.dp))
                    Text(
                        "Share Event Results",
                        color = Color(0xFF3A2A05),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChampionCard(game: EventGame, champion: EventEntrant?) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(46.dp).background(Color(0xFFEAF3EF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(game.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${game.sportName.uppercase()} CHAMPION" +
                        if (game.teamMode == com.scorekeeper.domain.EventTeamMode.SINGLES) "" else "S",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted
                )
                Text(
                    champion?.name ?: "Not decided yet",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (champion != null) MaterialTheme.colorScheme.onSurface else Muted
                )
            }
            if (champion != null) {
                Text("🥇", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
