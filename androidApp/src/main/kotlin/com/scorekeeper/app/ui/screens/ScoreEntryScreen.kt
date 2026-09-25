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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.AvatarColors
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.Danger
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.GameType
import com.scorekeeper.domain.Player
import com.scorekeeper.domain.RoundOutcome
import com.scorekeeper.scoring.RummyScoringEngine
import com.scorekeeper.scoring.ScoringEngine

/**
 * Score Entry, per the wireframe (ScoreEntry.dc.html): a standings card,
 * one round-entry card per still-in player with Rummy outcome chips or a
 * plain points field, and a two-button footer (Finish Game / Submit Round).
 */
@Composable
fun ScoreEntryScreen(
    session: GameSession,
    onBack: () -> Unit,
    onSubmitRound: (Map<String, Pair<Int, RoundOutcome>>) -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit,
    onViewHistory: () -> Unit
) {
    val engine = remember(session.gameType) { ScoringEngine.forGameType(session.gameType) }
    val standings = remember(session) { engine.standings(session.players, session.rounds, session.rules) }
    val nextRoundNumber = (session.rounds.maxOfOrNull { it.roundNumber } ?: 0) + 1
    val gameOver = engine.isGameOver(standings, session.rules)

    val entries = remember(session.id, nextRoundNumber) {
        session.players.filterNot { p -> standings.first { it.player.id == p.id }.isEliminated }
            .associate { it.id to mutableStateOf(RoundOutcome.NORMAL to "") }
    }
    // Tracks whether the player has tried to submit this round with something still
    // invalid, so errors only appear after a real attempt rather than nagging on load.
    var showErrors by remember(session.id, nextRoundNumber) { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(
            Modifier.padding(20.dp, 24.dp, 20.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoundIconButton(onClick = onBack) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Back") }
            Text(
                session.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            RoundIconButton(onClick = onUndo, enabled = session.rounds.isNotEmpty()) {
                Icon(Icons.Filled.Undo, contentDescription = "Undo last round")
            }
            RoundIconButton(onClick = onViewHistory) { Icon(Icons.Filled.List, contentDescription = "Score log") }
        }

        LazyColumn(
            Modifier.weight(1f).padding(20.dp, 14.dp, 20.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        standings.sortedBy { it.rank }.forEachIndexed { index, standing ->
                            if (index > 0) androidx.compose.material3.HorizontalDivider(color = Color(0xFFF1EDE3))
                            StandingRow(standing.player, standing.total, standing.rank, standing.isEliminated)
                        }
                    }
                }
            }

            if (!gameOver) {
                item {
                    Text(
                        "ROUND $nextRoundNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = Muted
                    )
                }
                items(entries.entries.toList(), key = { it.key }) { (playerId, state) ->
                    val player = session.players.first { it.id == playerId }
                    val isRummy = session.gameType == GameType.RUMMY
                    val (outcome, text) = state.value
                    RoundEntryCard(
                        player = player,
                        isRummy = isRummy,
                        outcome = outcome,
                        text = text,
                        rules = session.rules,
                        showError = showErrors && !isRoundEntryValid(isRummy, outcome, text),
                        onOutcomeChange = { state.value = it to state.value.second },
                        onTextChange = { state.value = state.value.first to it }
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onFinish,
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF1EDE3),
                modifier = Modifier.weight(1f)
            ) {
                Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text("Finish Game", fontWeight = FontWeight.SemiBold)
                }
            }
            if (!gameOver) {
                Button(
                    onClick = {
                        val isRummy = session.gameType == GameType.RUMMY
                        val allValid = entries.values.all { state ->
                            val (outcome, text) = state.value
                            isRoundEntryValid(isRummy, outcome, text)
                        }
                        if (!allValid) {
                            showErrors = true
                        } else {
                            val result = entries.mapValues { (_, state) ->
                                val (outcome, text) = state.value
                                val raw = if (isRummy) {
                                    RummyScoringEngine.penaltyFor(outcome, session.rules, text.toIntOrNull() ?: 0)
                                } else {
                                    text.toIntOrNull() ?: 0
                                }
                                raw to outcome
                            }
                            onSubmitRound(result)
                        }
                    },
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text("Submit Round $nextRoundNumber", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Whether one player's round-entry state is complete enough to submit.
 *
 * Plain-point games (Uno, Phase 10, Custom) always need a non-blank, non-negative
 * whole number -- previously `text.toIntOrNull() ?: 0` let a blank or garbled field
 * through as a silent 0, which is the bug this fixes.
 *
 * Rummy: a drop/full-count chip fully determines the penalty on its own, so no text
 * is required there. WIN is usually 0, so a blank field is accepted as 0 there too.
 * But the *default*, untouched state (NORMAL, no chip pressed) means the player
 * hasn't said anything yet -- treated as invalid unless they've typed a real number
 * (which reads as "manual deadwood entry" for that round).
 */
private fun isRoundEntryValid(isRummy: Boolean, outcome: RoundOutcome, text: String): Boolean {
    fun isValidNonNegativeInt(s: String) = s.toIntOrNull()?.let { it >= 0 } == true
    if (!isRummy) return isValidNonNegativeInt(text)
    return when (outcome) {
        RoundOutcome.FIRST_DROP, RoundOutcome.MIDDLE_DROP, RoundOutcome.FULL_COUNT -> true
        RoundOutcome.WIN -> text.isBlank() || isValidNonNegativeInt(text)
        RoundOutcome.NORMAL -> isValidNonNegativeInt(text)
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, enabled: Boolean = true, icon: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Border)
    ) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { icon() }
    }
}

@Composable
private fun StandingRow(player: Player, total: Int, rank: Int, eliminated: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (eliminated) "OUT" else "#$rank",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (eliminated) Danger else Muted,
            modifier = Modifier.padding(end = 10.dp)
        )
        val color = AvatarColors[player.orderIndex.mod(AvatarColors.size)]
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(if (eliminated) Danger else color),
            contentAlignment = Alignment.Center
        ) {
            Text(player.name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
        Text(
            player.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
            textDecoration = if (eliminated) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
        )
        Text("$total", style = MaterialTheme.typography.titleSmall, color = if (rank == 1 && !eliminated) Green else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RoundEntryCard(
    player: Player,
    isRummy: Boolean,
    outcome: RoundOutcome,
    text: String,
    rules: com.scorekeeper.domain.GameRules,
    showError: Boolean,
    onOutcomeChange: (RoundOutcome) -> Unit,
    onTextChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(if (showError) 1.5.dp else 1.dp, if (showError) Danger else Border)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(player.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (isRummy) {
                Row(
                    Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RummyChip("Win", outcome == RoundOutcome.WIN) { onOutcomeChange(RoundOutcome.WIN) }
                    RummyChip("1st Drop", outcome == RoundOutcome.FIRST_DROP) { onOutcomeChange(RoundOutcome.FIRST_DROP) }
                    RummyChip("Mid Drop", outcome == RoundOutcome.MIDDLE_DROP) { onOutcomeChange(RoundOutcome.MIDDLE_DROP) }
                    RummyChip("Full Count", outcome == RoundOutcome.FULL_COUNT) { onOutcomeChange(RoundOutcome.FULL_COUNT) }
                }
                when (outcome) {
                    RoundOutcome.FIRST_DROP -> PenaltyNote("Penalty auto-applied: ${rules.rummyFirstDropPenalty} pts")
                    RoundOutcome.MIDDLE_DROP -> PenaltyNote("Penalty auto-applied: ${rules.rummyMiddleDropPenalty} pts")
                    RoundOutcome.FULL_COUNT -> PenaltyNote("Penalty auto-applied: ${rules.rummyFullCountPenalty} pts")
                    else -> {
                        OutlinedTextField(
                            value = text,
                            onValueChange = onTextChange,
                            label = { Text(if (outcome == RoundOutcome.WIN) "Points (usually 0)" else "Deadwood points") },
                            isError = showError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        )
                        if (showError && outcome == RoundOutcome.NORMAL) {
                            ErrorNote("Pick an outcome above, or enter this player's deadwood points.")
                        } else if (showError) {
                            ErrorNote("Enter a valid points value, or leave it blank for 0.")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text("Points this round") },
                    isError = showError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                if (showError) {
                    ErrorNote("Enter this player's points for the round.")
                }
            }
        }
    }
}

@Composable
private fun ErrorNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Danger,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun PenaltyNote(text: String) {
    Box(
        Modifier.fillMaxWidth().padding(top = 10.dp).background(Color(0xFFF1EDE3), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Muted)
    }
}

@Composable
private fun RummyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) Green else Color(0xFFF1EDE3)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}
