package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.GameType
import com.scorekeeper.domain.SavedPlayer

/**
 * "Who's Playing?" per the wireframe (PlayerPicker.dc.html): pick from the
 * device's Saved Players roster, or add someone new on the fly (which also
 * saves them to the roster for next time). Continues once >= 2 are picked.
 */
@Composable
fun PlayerPickerScreen(
    gameType: GameType,
    savedPlayers: List<SavedPlayer>,
    onBack: () -> Unit,
    onAddSavedPlayer: (String) -> Unit,
    onContinue: (List<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val selectedNames = remember { mutableStateListOf<String>() }
    var showAddDialog by remember { mutableStateOf(false) }

    val visiblePlayers = remember(savedPlayers, query) {
        if (query.isBlank()) savedPlayers else savedPlayers.filter { it.name.contains(query, ignoreCase = true) }
    }

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
            Text(
                "${gameType.emoji} Who's Playing?",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Border),
            modifier = Modifier.padding(20.dp, 12.dp, 20.dp, 0.dp).fillMaxWidth()
        ) {
            Row(Modifier.padding(4.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Muted, modifier = Modifier.padding(start = 10.dp).size(18.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search or add a player") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    )
                )
            }
        }

        Column(Modifier.weight(1f).padding(20.dp)) {
            Text(
                "SAVED PLAYERS",
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(visiblePlayers, key = { it.id }) { player ->
                    val selected = selectedNames.contains(player.name)
                    PlayerAvatarTile(
                        player = player,
                        selected = selected,
                        onClick = {
                            if (selected) selectedNames.remove(player.name) else selectedNames.add(player.name)
                        }
                    )
                }
                item {
                    AddPlayerTile(onClick = { showAddDialog = true })
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Color.White).padding(20.dp, 14.dp, 20.dp, 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (selectedNames.size < 2) "${selectedNames.size} player${if (selectedNames.size == 1) "" else "s"} selected · need at least 2"
                else "${selectedNames.size} players selected",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Button(
                onClick = { onContinue(selectedNames.toList()) },
                enabled = selectedNames.size >= 2,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add a player") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotBlank()) {
                            onAddSavedPlayer(trimmed)
                            selectedNames.add(trimmed)
                        }
                        showAddDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PlayerAvatarTile(player: SavedPlayer, selected: Boolean, onClick: () -> Unit) {
    val color = AvatarColors[player.colorIndex.mod(AvatarColors.size)]
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .then(if (selected) Modifier.border(3.dp, color, CircleShape) else Modifier.border(2.dp, Border, CircleShape))
                    .background(if (selected) color else Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.name.take(1).uppercase(),
                    color = if (selected) Color.White else Muted,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (selected) {
                Box(
                    Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Green),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Text(
            player.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface else Muted
        )
    }
}

@Composable
private fun AddPlayerTile(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAF3EF))
                .border(1.5.dp, Green, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add player", tint = Green)
        }
        Text("New", style = MaterialTheme.typography.labelMedium, color = Green, fontWeight = FontWeight.SemiBold)
    }
}
