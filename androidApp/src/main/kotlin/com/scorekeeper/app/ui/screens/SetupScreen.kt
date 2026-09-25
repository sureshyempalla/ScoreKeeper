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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.GameRules
import com.scorekeeper.domain.GameType

/**
 * Setup, per the wireframe (Setup.dc.html): name the game, and for Rummy
 * tune the pool rules with steppers. Other game types skip straight to a
 * name field since they have no house rules to configure yet.
 */
@Composable
fun SetupScreen(
    gameType: GameType,
    onBack: () -> Unit,
    onStart: (sessionName: String, rules: GameRules) -> Unit
) {
    var sessionName by remember { mutableStateOf(defaultSessionName(gameType)) }
    var rules by remember { mutableStateOf(GameRules()) }

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
            Text("${gameType.emoji} ${gameType.displayName} Setup", style = MaterialTheme.typography.headlineSmall)
        }

        LazyColumn(
            Modifier.weight(1f).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("GAME NAME", style = MaterialTheme.typography.labelMedium, color = Muted)
                    OutlinedTextField(
                        value = sessionName,
                        onValueChange = { sessionName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (gameType == GameType.RUMMY) {
                item {
                    Text(
                        "POOL RULES",
                        style = MaterialTheme.typography.labelMedium,
                        color = Muted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
                        Column {
                            RuleStepperRow(
                                title = "Pool Limit",
                                subtitle = "Eliminated above this total",
                                value = rules.rummyPoolLimit,
                                step = 5,
                                onChange = { rules = rules.copy(rummyPoolLimit = it) }
                            )
                            RuleDivider()
                            RuleStepperRow(
                                title = "First Drop",
                                subtitle = "Penalty for dropping early",
                                value = rules.rummyFirstDropPenalty,
                                step = 5,
                                onChange = { rules = rules.copy(rummyFirstDropPenalty = it) }
                            )
                            RuleDivider()
                            RuleStepperRow(
                                title = "Middle Drop",
                                subtitle = "Penalty for dropping mid-hand",
                                value = rules.rummyMiddleDropPenalty,
                                step = 5,
                                onChange = { rules = rules.copy(rummyMiddleDropPenalty = it) }
                            )
                            RuleDivider()
                            RuleStepperRow(
                                title = "Full Count",
                                subtitle = "Loses with hand unmelded",
                                value = rules.rummyFullCountPenalty,
                                step = 5,
                                onChange = { rules = rules.copy(rummyFullCountPenalty = it) }
                            )
                        }
                    }
                }
            }

            if (gameType == GameType.UNO) {
                item {
                    Text("TARGET SCORE", style = MaterialTheme.typography.labelMedium, color = Muted)
                }
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
                        RuleStepperRow(
                            title = "Target Score",
                            subtitle = "First player to reach this loses",
                            value = rules.unoTargetScore,
                            step = 50,
                            onChange = { rules = rules.copy(unoTargetScore = it) }
                        )
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp)) {
            Button(
                onClick = { onStart(sessionName.ifBlank { defaultSessionName(gameType) }, rules) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Start Game", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun defaultSessionName(gameType: GameType): String = "${gameType.displayName} Night"

@Composable
private fun RuleDivider() {
    androidx.compose.material3.HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFFF1EDE3))
}

@Composable
private fun RuleStepperRow(title: String, subtitle: String, value: Int, step: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepperButton("−") { if (value - step >= 0) onChange(value - step) }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.size(32.dp).padding(top = 2.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            StepperButton("+") { onChange(value + step) }
        }
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Cream, border = BorderStroke(1.dp, Border)) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Text(symbol, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
