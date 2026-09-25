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
import androidx.compose.material3.OutlinedButton
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
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.EventMatch

/**
 * Match Score entry, per the wireframe (MatchScore.dc.html): a bottom-sheet-
 * style card over a dark scrim with two entrant rows and +/- score steppers,
 * a "Declare Winner" action once the scores differ, and a lighter "Save &
 * Continue Later" action for scores taken mid-match.
 */
@Composable
fun MatchScoreScreen(
    match: EventMatch,
    sportLabel: String,
    entrantAName: String,
    entrantBName: String,
    onBack: () -> Unit,
    onSaveProgress: (scoreA: Int, scoreB: Int) -> Unit,
    onDeclareWinner: (winnerEntrantId: String, scoreA: Int, scoreB: Int) -> Unit
) {
    var scoreA by remember { mutableStateOf(match.scoreA) }
    var scoreB by remember { mutableStateOf(match.scoreB) }

    Box(Modifier.fillMaxSize().background(Color(0x66000000))) {
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp, 22.dp, 24.dp, 30.dp)
        ) {
            Box(
                Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Color(0xFFE8E3D9), RoundedCornerShape(999.dp))
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "${match.roundLabel.uppercase()} · $sportLabel",
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(18.dp))

            ScoreRow(
                name = entrantAName,
                score = scoreA,
                highlighted = scoreA > scoreB,
                onDecrement = { if (scoreA > 0) scoreA-- },
                onIncrement = { scoreA++ }
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("VS", style = MaterialTheme.typography.labelSmall, color = Muted, fontWeight = FontWeight.Bold)
            }
            ScoreRow(
                name = entrantBName,
                score = scoreB,
                highlighted = scoreB > scoreA,
                onDecrement = { if (scoreB > 0) scoreB-- },
                onIncrement = { scoreB++ }
            )

            Spacer(Modifier.height(6.dp))
            Text(
                "Scores save automatically as you go — declare a winner once the match is decided.",
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)
            )

            Button(
                onClick = {
                    val winnerId = when {
                        scoreA > scoreB -> match.entrantAId
                        scoreB > scoreA -> match.entrantBId
                        else -> null
                    }
                    if (winnerId != null) onDeclareWinner(winnerId, scoreA, scoreB)
                },
                enabled = scoreA != scoreB,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text(
                    if (scoreA == scoreB) "Declare Winner"
                    else "Declare ${if (scoreA > scoreB) entrantAName else entrantBName} Winner",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onSaveProgress(scoreA, scoreB) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                border = BorderStroke(1.dp, Border)
            ) {
                Text("Save & Continue Later", fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Back",
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
                    .clickable(onClick = onBack)
            )
        }
    }
}

@Composable
private fun ScoreRow(
    name: String,
    score: Int,
    highlighted: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) Color(0xFFEAF3EF) else Cream,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, if (highlighted) Green else Border)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                name,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(onClick = onDecrement, shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, Border)) {
                    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease score")
                    }
                }
                Text(score.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Surface(onClick = onIncrement, shape = CircleShape, color = Green) {
                    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase score", tint = Color.White)
                    }
                }
            }
        }
    }
}
