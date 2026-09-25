package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.Border
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted
import com.scorekeeper.domain.GameType

/**
 * Choose a Game, per the wireframe (GamePicker.dc.html): a 2-column grid of
 * game cards (Rummy highlighted as "Popular"), plus a dimmed "More on the
 * way" row of not-yet-supported games.
 */
@Composable
fun GamePickerScreen(
    onGameSelected: (GameType) -> Unit,
    onBack: () -> Unit
) {
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
            Text("Choose a Game", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Pick what you're playing tonight",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            modifier = Modifier.padding(20.dp, 10.dp, 20.dp, 0.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(GameType.entries.toList()) { game ->
                GameCard(
                    game = game,
                    subtitle = subtitleFor(game),
                    popular = game == GameType.RUMMY,
                    onClick = { onGameSelected(game) }
                )
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Column(Modifier.padding(top = 4.dp)) {
                    Text(
                        "MORE ON THE WAY",
                        style = MaterialTheme.typography.labelMedium,
                        color = Muted,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ComingSoonChip("♠️", "Spades", Modifier.weight(1f))
                        ComingSoonChip("♥️", "Hearts", Modifier.weight(1f))
                        ComingSoonChip("🎲", "Yahtzee", Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun subtitleFor(game: GameType): String = when (game) {
    GameType.UNO -> "First to target score"
    GameType.RUMMY -> "Pool scoring, 201 out"
    GameType.PHASE10 -> "Lowest score wins"
    GameType.CUSTOM -> "Any game, your rules"
}

@Composable
private fun GameCard(game: GameType, subtitle: String, popular: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (popular) Color(0xFFEAF3EF) else Color.White,
        border = BorderStroke(if (popular) 1.5.dp else 1.dp, if (popular) Green else Border)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(game.emoji, style = MaterialTheme.typography.displaySmall)
                Text(
                    game.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (popular) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Green,
                    modifier = Modifier.padding(10.dp).align(Alignment.TopEnd)
                ) {
                    Text(
                        "POPULAR",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComingSoonChip(emoji: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF1EDE3),
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
