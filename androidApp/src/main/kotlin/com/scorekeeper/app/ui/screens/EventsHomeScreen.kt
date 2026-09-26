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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.app.ui.util.formatEventDateRangeShort
import com.scorekeeper.domain.CommunityEvent

/**
 * Events home, per the wireframe (EventsHome.dc.html): a list of events with
 * a highlighted "next up" card, a floating "New Event" button, and the
 * shared bottom nav with Events active.
 */
@Composable
fun EventsHomeScreen(
    events: List<CommunityEvent>,
    gameCounts: Map<String, Int>,
    gameCompletionCounts: Map<String, Pair<Int, Int>>, // eventId -> (completed, total)
    onOpenEvent: (String) -> Unit,
    onCreateEvent: () -> Unit,
    onHome: () -> Unit,
    onStats: () -> Unit,
    onProfile: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Cream)) {
            Column(Modifier.padding(20.dp, 28.dp, 20.dp, 8.dp)) {
                Text("Events", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Tournaments & multi-game meetups",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (events.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No events yet. Tap \"New Event\" to bundle a few games/sports under one occasion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).padding(20.dp, 6.dp, 20.dp, 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        val total = gameCounts[event.id] ?: 0
                        val (completed, _) = gameCompletionCounts[event.id] ?: (0 to 0)
                        val isDone = total > 0 && completed == total
                        EventCard(event, total, isDone, onClick = { onOpenEvent(event.id) })
                    }
                }
            }

            HomeBottomNavBar(
                selected = HomeTab.EVENTS,
                onHome = onHome,
                onEvents = {},
                onStats = onStats,
                onProfile = onProfile
            )
        }

        Surface(
            onClick = onCreateEvent,
            shape = RoundedCornerShape(999.dp),
            color = Amber,
            modifier = Modifier.padding(20.dp, 0.dp, 20.dp, 88.dp).align(Alignment.BottomEnd)
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF3A2A05))
                Text("New Event", color = Color(0xFF3A2A05), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EventCard(event: CommunityEvent, gameCount: Int, isDone: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(if (isDone) 1.dp else 1.5.dp, if (isDone) Border else Amber),
        modifier = Modifier.fillMaxWidth().then(if (isDone) Modifier else Modifier)
    ) {
        Row(
            Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(event.emoji, style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f)) {
                Text(event.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append(formatEventDateRangeShort(event.dateMillis, event.endDateMillis))
                        if (gameCount > 0) append(" · $gameCount game${if (gameCount == 1) "" else "s"}")
                        if (!event.location.isNullOrBlank()) append(" · ${event.location}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isDone) Color(0xFFEAF3EF) else Color(0xFFFDF3E1)
            ) {
                Text(
                    if (isDone) "Completed" else if (gameCount == 0) "Setup" else "Upcoming",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) com.scorekeeper.app.ui.theme.Green else Color(0xFFB9822B),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
