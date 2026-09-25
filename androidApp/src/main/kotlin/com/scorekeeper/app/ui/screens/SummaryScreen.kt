package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scorekeeper.domain.GameSession
import com.scorekeeper.scoring.ScoringEngine

@Composable
fun SummaryScreen(
    session: GameSession,
    onBackToHome: () -> Unit
) {
    val engine = ScoringEngine.forGameType(session.gameType)
    val standings = engine.standings(session.players, session.rounds, session.rules)
    val winner = standings.minByOrNull { it.rank }

    Scaffold(
        bottomBar = {
            Button(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Back to Home")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("${session.gameType.emoji} ${session.name}", style = MaterialTheme.typography.headlineSmall)
            Text(
                "🏆 ${winner?.player?.name ?: "—"} wins!",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )
            LazyColumn {
                items(standings, key = { it.player.id }) { standing ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (standing.rank == 1)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${standing.rank}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                standing.player.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp).weight(1f)
                            )
                            Text("${standing.total} pts", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
