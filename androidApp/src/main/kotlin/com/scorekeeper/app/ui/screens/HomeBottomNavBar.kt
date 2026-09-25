package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.Green
import com.scorekeeper.app.ui.theme.Muted

/** Which tab is active, per the wireframe's persistent bottom nav (Main/Leaderboard/Profile). */
enum class HomeTab { HOME, EVENTS, STATS, PROFILE }

@Composable
fun HomeBottomNavBar(
    selected: HomeTab,
    onHome: () -> Unit,
    onEvents: () -> Unit,
    onStats: () -> Unit,
    onProfile: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(androidx.compose.ui.graphics.Color.White)
    ) {
        NavTab(Icons.Filled.Home, "Home", selected == HomeTab.HOME, onHome, Modifier.weight(1f))
        NavTab(Icons.Filled.EmojiEvents, "Events", selected == HomeTab.EVENTS, onEvents, Modifier.weight(1f))
        NavTab(Icons.Filled.Leaderboard, "Stats", selected == HomeTab.STATS, onStats, Modifier.weight(1f))
        NavTab(Icons.Filled.Person, "Profile", selected == HomeTab.PROFILE, onProfile, Modifier.weight(1f))
    }
}

@Composable
private fun NavTab(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val color = if (active) Green else Muted
    Column(
        modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
