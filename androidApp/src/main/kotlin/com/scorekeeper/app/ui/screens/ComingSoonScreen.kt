package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scorekeeper.app.ui.theme.Muted

/**
 * Placeholder destination for bottom-nav tabs whose flows aren't built yet
 * (Events, Stats, Profile). Keeps Home's tab bar fully wired while those
 * screens are developed.
 */
@Composable
fun ComingSoonScreen(feature: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.HourglassEmpty, contentDescription = null, tint = Muted)
        Text(
            "$feature is coming soon",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            "We're still building this part of Score Keeper.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            modifier = Modifier.padding(top = 4.dp)
        )
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text("Back to Home")
        }
    }
}
