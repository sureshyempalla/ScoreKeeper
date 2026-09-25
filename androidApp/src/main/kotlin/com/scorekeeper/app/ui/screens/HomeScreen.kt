package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.Danger
import com.scorekeeper.domain.GameSession

@Composable
fun HomeScreen(
    sessions: List<GameSession>,
    onNewGame: () -> Unit,
    onOpenSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewGame) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  New Game")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Score Keeper",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(20.dp)
            )
            if (sessions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No games yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap \"New Game\" to pick a game and start keeping score",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        SessionRow(session, onClick = { onOpenSession(session.id) }, onDelete = { onDeleteSession(session.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: GameSession, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(session.gameType.emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.size(36.dp))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(session.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${session.gameType.displayName} · ${session.players.size} players" +
                        if (session.isFinished) " · Finished" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Danger)
            }
        }
    }
}
