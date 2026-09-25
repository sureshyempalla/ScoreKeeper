package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.Danger
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.GameType
import com.scorekeeper.domain.Player
import com.scorekeeper.domain.RoundOutcome
import com.scorekeeper.scoring.RummyScoringEngine
import com.scorekeeper.scoring.ScoringEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreEntryScreen(
    session: GameSession,
    onBack: () -> Unit,
    onSubmitRound: (Map<String, Pair<Int, RoundOutcome>>) -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit
) {
    val engine = remember(session.gameType) { ScoringEngine.forGameType(session.gameType) }
    val standings = remember(session) { engine.standings(session.players, session.rounds, session.rules) }
    val nextRoundNumber = (session.rounds.maxOfOrNull { it.roundNumber } ?: 0) + 1
    val gameOver = engine.isGameOver(standings, session.rules)

    val entries = remember(session.id, nextRoundNumber) {
        session.players.filterNot { p -> standings.first { it.player.id == p.id }.isEliminated }
            .associate { it.id to mutableStateOf(RoundOutcome.NORMAL to "") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onUndo, enabled = session.rounds.isNotEmpty()) {
                        Icon(Icons.Filled.Undo, contentDescription = "Undo last round")
                    }
                }
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                if (gameOver) {
                    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                        Text("Finish Game")
                    }
                } else {
                    Button(
                        onClick = {
                            val result = entries.mapValues { (_, state) ->
                                val (outcome, text) = state.value
                                val raw = if (session.gameType == GameType.RUMMY) {
                                    RummyScoringEngine.penaltyFor(outcome, session.rules, text.toIntOrNull() ?: 0)
                                } else {
                                    text.toIntOrNull() ?: 0
                                }
                                raw to outcome
                            }
                            onSubmitRound(result)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Round $nextRoundNumber")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Text("Standings", style = MaterialTheme.typography.titleMedium)
            }
            items(standings, key = { it.player.id }) { standing ->
                StandingRow(standing.player, standing.total, standing.rank, standing.isEliminated)
            }

            if (!gameOver) {
                item {
                    Text(
                        "Round $nextRoundNumber",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                    )
                }
                items(entries.entries.toList(), key = { it.key }) { (playerId, state) ->
                    val player = session.players.first { it.id == playerId }
                    RoundEntryRow(
                        player = player,
                        isRummy = session.gameType == GameType.RUMMY,
                        outcome = state.value.first,
                        text = state.value.second,
                        onOutcomeChange = { state.value = it to state.value.second },
                        onTextChange = { state.value = state.value.first to it }
                    )
                }
            }
        }
    }
}

@Composable
private fun StandingRow(player: Player, total: Int, rank: Int, eliminated: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#$rank", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
        Text(
            player.name + if (eliminated) " (out)" else "",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (eliminated) Danger else MaterialTheme.colorScheme.onSurface
        )
        Text("$total pts", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RoundEntryRow(
    player: Player,
    isRummy: Boolean,
    outcome: RoundOutcome,
    text: String,
    onOutcomeChange: (RoundOutcome) -> Unit,
    onTextChange: (String) -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(player.name, style = MaterialTheme.typography.titleSmall)
            if (isRummy) {
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RummyChip("Win", outcome == RoundOutcome.WIN) { onOutcomeChange(RoundOutcome.WIN) }
                    RummyChip("1st Drop", outcome == RoundOutcome.FIRST_DROP) { onOutcomeChange(RoundOutcome.FIRST_DROP) }
                    RummyChip("Mid Drop", outcome == RoundOutcome.MIDDLE_DROP) { onOutcomeChange(RoundOutcome.MIDDLE_DROP) }
                    RummyChip("Full Count", outcome == RoundOutcome.FULL_COUNT) { onOutcomeChange(RoundOutcome.FULL_COUNT) }
                }
                if (outcome == RoundOutcome.NORMAL || outcome == RoundOutcome.WIN) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChange,
                        label = { Text(if (outcome == RoundOutcome.WIN) "Points (usually 0)" else "Deadwood points") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            } else {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text("Points this round") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun RummyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = if (selected) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            AssistChipDefaults.assistChipColors()
        }
    )
}
