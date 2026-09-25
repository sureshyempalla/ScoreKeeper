package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scorekeeper.domain.GameRules
import com.scorekeeper.domain.GameType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayersScreen(
    gameType: GameType,
    onBack: () -> Unit,
    onStart: (sessionName: String, playerNames: List<String>, rules: GameRules) -> Unit
) {
    val playerNames = remember { mutableStateListOf("", "") }
    var sessionName by remember { mutableStateOf("${gameType.displayName} Game") }
    var rules by remember { mutableStateOf(GameRules()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${gameType.emoji} ${gameType.displayName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        val names = playerNames.map { it.trim() }.filter { it.isNotEmpty() }
                        if (names.size >= 2) onStart(sessionName.ifBlank { "${gameType.displayName} Game" }, names, rules)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = playerNames.count { it.isNotBlank() } >= 2
                ) {
                    Text("Start Game")
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item {
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Game name") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                Text(
                    "Players",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
            }
            items(playerNames.size) { index ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = playerNames[index],
                        onValueChange = { playerNames[index] = it },
                        label = { Text("Player ${index + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                    if (playerNames.size > 2) {
                        IconButton(onClick = { playerNames.removeAt(index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }
            item {
                TextButton(onClick = { playerNames.add("") }) {
                    Text("+ Add player")
                }
                if (gameType == GameType.RUMMY) {
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Text("Rummy pool rules", style = MaterialTheme.typography.titleMedium)
                    RuleNumberField("Pool limit (eliminated above this)", rules.rummyPoolLimit) {
                        rules = rules.copy(rummyPoolLimit = it)
                    }
                    RuleNumberField("First drop penalty", rules.rummyFirstDropPenalty) {
                        rules = rules.copy(rummyFirstDropPenalty = it)
                    }
                    RuleNumberField("Middle drop penalty", rules.rummyMiddleDropPenalty) {
                        rules = rules.copy(rummyMiddleDropPenalty = it)
                    }
                    RuleNumberField("Full count penalty", rules.rummyFullCountPenalty) {
                        rules = rules.copy(rummyFullCountPenalty = it)
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleNumberField(label: String, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text -> text.toIntOrNull()?.let(onChange) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
