package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.scorekeeper.domain.AuthStatuses
import com.scorekeeper.domain.AuthUiState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Profile: shows who's signed in and a manual sync control (Phase 1 sync is
 * on-demand, not realtime -- see EventSyncRepository/AppController.syncNow).
 * Signed-out/guest just prompts to sign in; nothing here is reachable without
 * going through Home's optional "sign in to sync" banner first.
 */
@Composable
fun ProfileScreen(
    uiState: AuthUiState,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: (onResult: (Boolean) -> Unit) -> Unit,
    onGetLastSynced: (onResult: (Long?) -> Unit) -> Unit
) {
    var lastSyncedMillis by remember { mutableStateOf<Long?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.statusId) {
        onGetLastSynced { lastSyncedMillis = it }
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
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
        }

        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (uiState.statusId) {
                AuthStatuses.SIGNED_IN -> {
                    val user = uiState.user
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Signed in as", style = MaterialTheme.typography.labelMedium, color = Muted)
                            Text(
                                user?.email ?: user?.phoneNumber ?: "Unknown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Sync", style = MaterialTheme.typography.labelMedium, color = Muted)
                            Text(
                                lastSyncedMillis?.let { "Last synced " + it.toReadableTime() } ?: "Never synced yet",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )
                            Button(
                                onClick = {
                                    syncing = true
                                    syncMessage = null
                                    onSyncNow { ok ->
                                        syncing = false
                                        syncMessage = if (ok) "Synced." else "Sync failed -- check your connection and try again."
                                        if (ok) onGetLastSynced { lastSyncedMillis = it }
                                    }
                                },
                                enabled = !syncing,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Green)
                            ) {
                                if (syncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("  Sync now", fontWeight = FontWeight.Bold)
                                }
                            }
                            syncMessage?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign out")
                    }
                }
                else -> {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Sign in to sync your events and scores across devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) {
                        Text("Sign in", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun Long.toReadableTime(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 = if (dt.hour % 12 == 0) 12 else dt.hour % 12
    val ampm = if (dt.hour < 12) "AM" else "PM"
    val minute = dt.minute.toString().padStart(2, '0')
    return "$hour12:$minute $ampm"
}
