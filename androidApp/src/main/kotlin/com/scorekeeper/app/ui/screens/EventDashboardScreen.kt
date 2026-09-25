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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.EmojiEvents
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
import com.scorekeeper.app.ui.util.formatEventDateShort
import com.scorekeeper.domain.CommunityEvent
import com.scorekeeper.domain.EventGame
import com.scorekeeper.domain.EventGameStatus

/**
 * Event Dashboard, per the wireframe (EventDashboard.dc.html): the list of
 * games/sports inside one event, each showing its bracket status, plus
 * "Add Another Game" and a footer "View Overall Results" action.
 */
@Composable
fun EventDashboardScreen(
    event: CommunityEvent,
    games: List<EventGame>,
    onBack: () -> Unit,
    onAddGame: () -> Unit,
    onOpenGame: (EventGame) -> Unit,
    onViewResults: () -> Unit
) {
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
                Text("${event.emoji} ${event.name}", style = MaterialTheme.typography.titleLarge)
                Text(
                    buildString {
                        append(formatEventDateShort(event.dateMillis))
                        if (!event.location.isNullOrBlank()) append(" · ${event.location}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
            }
        }

        LazyColumn(
            Modifier.weight(1f).padding(20.dp, 16.dp, 20.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("GAMES", style = MaterialTheme.typography.labelMedium, color = Muted)
            }
            items(games, key = { it.id }) { game ->
                GameRow(game, onClick = { onOpenGame(game) })
            }
            item {
                Surface(
                    onClick = onAddGame,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, Green)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Green, modifier = Modifier.size(16.dp))
                        Text(
                            "Add Another Game",
                            color = Green,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp)) {
            Surface(
                onClick = onViewResults,
                shape = RoundedCornerShape(14.dp),
                color = Amber,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(vertical = 14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFF3A2A05), modifier = Modifier.size(18.dp))
                    Text(
                        "View Overall Results",
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
private fun GameRow(game: EventGame, onClick: () -> Unit) {
    val (badgeBg, badgeColor, badgeText) = when (game.status) {
        EventGameStatus.SETUP -> Triple(Color(0xFFF1EDE3), Muted, "Setup")
        EventGameStatus.DRAW_READY -> Triple(Color(0xFFEAF3EF), Green, "Draw Ready")
        EventGameStatus.IN_PROGRESS -> Triple(Color(0xFFFDF3E1), Color(0xFFB9822B), "In Progress")
        EventGameStatus.COMPLETE -> Triple(Color(0xFFEAF3EF), Green, "✓ Done")
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(42.dp).background(Color(0xFFEAF3EF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(game.emoji, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f)) {
                Text(game.sportName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${game.format.displayName}" + when (game.status) {
                        EventGameStatus.SETUP -> " · needs players"
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted
                )
            }
            Surface(shape = RoundedCornerShape(999.dp), color = badgeBg) {
                Text(
                    badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
