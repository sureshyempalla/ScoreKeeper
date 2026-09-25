package com.scorekeeper.app.ui.nav

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Login : Screen("login")
    object GamePicker : Screen("game_picker")
    object AddPlayers : Screen("add_players/{gameType}") {
        fun build(gameType: String) = "add_players/$gameType"
    }
    object ScoreEntry : Screen("score_entry/{sessionId}") {
        fun build(sessionId: String) = "score_entry/$sessionId"
    }
    object Summary : Screen("summary/{sessionId}") {
        fun build(sessionId: String) = "summary/$sessionId"
    }
    /** Bottom-nav destinations not built yet (Events, Stats, Profile). */
    object ComingSoon : Screen("coming_soon/{feature}") {
        fun build(feature: String) = "coming_soon/$feature"
    }
}
