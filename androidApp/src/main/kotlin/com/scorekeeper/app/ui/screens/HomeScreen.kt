package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.AvatarColors
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.AuthStatuses
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.SavedPlayer

/**
 * Home, per the wireframe (Main.dc.html): a sign-in nudge banner (shown
 * whenever the person isn't fully signed in -- guest included), the Card
 * Games / Community Events entry tiles, a "Continue" section for the most
 * recent unfinished game, a Saved Players roster, and a bottom nav bar.
 * Unlike the earlier build, Home no longer sits behind a login wall: local
 * scorekeeping works fully signed-out, matching the design.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    sessions: List<GameSession>,
    savedPlayers: List<SavedPlayer>,
    authStatusId: String,
    onNewGame: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenProfile: () -> Unit,
    onSignInBannerClick: () -> Unit,
    onAddSavedPlayer: (String) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    val inProgress = sessions.firstOrNull { !it.isFinished }
    var sessionPendingDelete by remember { mutableStateOf<GameSession?>(null) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            Column(Modifier.padding(20.dp, 24.dp, 20.dp, 0.dp)) {
                Text("Score Keeper", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "${sessions.count { it.isRecent() }} games this week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (authStatusId != AuthStatuses.SIGNED_IN) {
                Row(
                    Modifier
                        .padding(20.dp, 14.dp, 20.dp, 0.dp)
                        .fillMaxWidth()
                        .clip(14.dp)
                        .background(Color(0xFFEAF3EF))
                        .clickable(onClick = onSignInBannerClick)
                        .padding(14.dp, 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Sign in to sync your data across devices",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF164F3C),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF164F3C))
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 16.dp)
            ) {
                Column(Modifier.padding(20.dp, 0.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeTile(
                            title = "Card Games",
                            subtitle = "Score Uno, Rummy, Phase 10 & more",
                            footer = "${sessions.size} games this week →",
                            emojis = listOf("🃏", "🔴", "🔟", "⭐"),
                            containerColor = Color.White,
                            onClick = onNewGame
                        )
                        HomeTile(
                            title = "Community Events",
                            subtitle = "Plan multi-sport tournaments & meetups",
                            footer = "Coming soon →",
                            emojis = listOf("🏐", "🏓", "🎯", "🪔"),
                            containerColor = Green,
                            onDark = true,
                            onClick = onOpenEvents
                        )
                    }

                    if (inProgress != null) {
                        Column {
                            SectionLabel("Continue")
                            ContinueCard(
                                session = inProgress,
                                onClick = { onOpenSession(inProgress.id) },
                                onLongClick = { sessionPendingDelete = inProgress }
                            )
                        }
                    }

                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionLabel("Saved Players")
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(savedPlayers, key = { it.id }) { player ->
                                SavedPlayerAvatar(player)
                            }
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEAF3EF))
                                            .clickable { showAddPlayerDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add saved player", tint = Green)
                                    }
                                    Text("New", style = MaterialTheme.typography.labelMedium, color = Green)
                                }
                            }
                        }
                    }
                }
            }
        }

        HomeBottomNavBar(
            selected = HomeTab.HOME,
            onHome = {},
            onEvents = onOpenEvents,
            onStats = onOpenStats,
            onProfile = onOpenProfile
        )
    }

    if (showAddPlayerDialog) {
        AddSavedPlayerDialog(
            onDismiss = { showAddPlayerDialog = false },
            onConfirm = { name ->
                onAddSavedPlayer(name)
                showAddPlayerDialog = false
            }
        )
    }

    sessionPendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionPendingDelete = null },
            title = { Text("Delete \"${session.name}\"?") },
            text = { Text("This removes the game and all its rounds. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSession(session.id)
                    sessionPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

private fun GameSession.isRecent(): Boolean {
    val weekMillis = 7L * 24 * 60 * 60 * 1000
    return System.currentTimeMillis() - createdAtMillis <= weekMillis
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Muted,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTile(
    title: String,
    subtitle: String,
    footer: String,
    emojis: List<String>,
    containerColor: Color,
    onDark: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (onDark) Color.White.copy(alpha = 0.85f) else Muted
    val footerColor = if (onDark) Color(0xFFFDF3E1) else Green
    val chipBg = if (onDark) Color.White.copy(alpha = 0.15f) else Color(0xFFEAF3EF)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = if (onDark) null else BorderStroke(1.dp, Border)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emojis.forEach { emoji ->
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(chipBg),
                            contentAlignment = Alignment.Center
                        ) { Text(emoji) }
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = if (onDark) Color.White else Muted)
            }
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = textColor)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subColor, modifier = Modifier.padding(top = 2.dp))
            }
            Text(footer, style = MaterialTheme.typography.labelMedium, color = footerColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueCard(session: GameSession, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFEAF3EF)),
                contentAlignment = Alignment.Center
            ) { Text(session.gameType.emoji) }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(session.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Round ${(session.rounds.maxOfOrNull { it.roundNumber } ?: 0) + 1} · ${session.players.size} players",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }
    }
}

@Composable
private fun SavedPlayerAvatar(player: SavedPlayer) {
    val color = AvatarColors[player.colorIndex.mod(AvatarColors.size)]
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                player.name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(player.name, style = MaterialTheme.typography.labelMedium, color = Muted)
    }
}

@Composable
private fun AddSavedPlayerDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
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
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Modifier.clip(radius: androidx.compose.ui.unit.Dp): Modifier =
    this.clip(RoundedCornerShape(radius))
