package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Danger
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.EventMatch
import com.scorekeeper.domain.EventMatchStatus
import com.scorekeeper.domain.PointRules
import com.scorekeeper.events.PointRulesEngine

/**
 * Set-by-set score entry for a point-based-sport match (Table Tennis etc, see
 * [PointRules]/[com.scorekeeper.domain.PointBasedSports]) -- quick set-score
 * entry rather than live point-by-point, validated against the game's active
 * rules so an impossible score (e.g. winning by only 1 point at deuce) can't
 * be submitted. Once one side reaches a majority of sets the match is
 * declared complete automatically.
 */
@Composable
fun SetScoreEntryScreen(
    match: EventMatch,
    rules: PointRules,
    sportLabel: String,
    entrantAName: String,
    entrantBName: String,
    onBack: () -> Unit,
    onSubmitSet: (scoreA: Int, scoreB: Int) -> Unit,
    onUndoLastSet: () -> Unit
) {
    var scoreA by remember { mutableStateOf(rules.pointsPerSet) }
    var scoreB by remember { mutableStateOf(0) }

    val setsWonA = match.sets.count { it.scoreA > it.scoreB }
    val setsWonB = match.sets.count { it.scoreB > it.scoreA }
    val isComplete = match.status == EventMatchStatus.COMPLETE
    val isValid = PointRulesEngine.isValidCompletedSet(scoreA, scoreB, rules)

    Box(Modifier.fillMaxSize().background(Color(0x66000000))) {
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp, 22.dp, 24.dp, 30.dp)
        ) {
            Box(
                Modifier.size(width = 40.dp, height = 4.dp).align(Alignment.CenterHorizontally)
                    .background(Color(0xFFE8E3D9), RoundedCornerShape(999.dp))
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "${match.roundLabel.uppercase()} · $sportLabel",
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Best of ${rules.bestOfSets} · ${rules.pointsPerSet} points" +
                    (if (rules.winByTwo) ", win by 2" else "") +
                    (rules.deuceCap?.let { " (capped at $it)" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp)
            )
            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entrantAName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    "$setsWonA — $setsWonB",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    entrantBName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }

            if (isComplete) {
                Spacer(Modifier.height(14.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFEAF3EF)) {
                    Text(
                        "Match complete — ${if (setsWonA > setsWonB) entrantAName else entrantBName} won $setsWonA-$setsWonB.",
                        color = com.scorekeeper.app.ui.theme.GreenDark,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else if (match.sets.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("SETS SO FAR", style = MaterialTheme.typography.labelSmall, color = Muted)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    match.sets.sortedBy { it.setNumber }.forEach { set ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Set ${set.setNumber}", style = MaterialTheme.typography.labelSmall, color = Muted)
                            Text("${set.scoreA}-${set.scoreB}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (!isComplete) {
                Spacer(Modifier.height(16.dp))
                Text("NEXT SET", style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.height(6.dp))
                SetScoreStepper(entrantAName, scoreA, onDecrement = { if (scoreA > 0) scoreA-- }, onIncrement = { scoreA++ })
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
                    Text("VS", style = MaterialTheme.typography.labelSmall, color = Muted, fontWeight = FontWeight.Bold)
                }
                SetScoreStepper(entrantBName, scoreB, onDecrement = { if (scoreB > 0) scoreB-- }, onIncrement = { scoreB++ })

                if (!isValid && (scoreA > 0 || scoreB > 0)) {
                    Text(
                        "Not a valid final score for this set yet -- check the target and win-by-2 rule above.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Danger,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        onSubmitSet(scoreA, scoreB)
                        scoreA = rules.pointsPerSet
                        scoreB = 0
                    },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text("Confirm Set", fontWeight = FontWeight.Bold)
                }
            }

            if (match.sets.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Undo Last Set",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isComplete) Muted else Danger,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp)
                        .clickable(enabled = !isComplete, onClick = onUndoLastSet)
                )
            }
            Text(
                "Back",
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .clickable(onClick = onBack)
            )
        }
    }
}

@Composable
private fun SetScoreStepper(name: String, score: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(onClick = onDecrement, shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, Border)) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease score")
                }
            }
            Text(score.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
            Surface(onClick = onIncrement, shape = CircleShape, color = Green) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase score", tint = Color.White)
                }
            }
        }
    }
}
