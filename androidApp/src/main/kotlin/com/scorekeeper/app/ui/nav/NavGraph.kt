package com.scorekeeper.app.ui.nav

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Login : Screen("login")
    object GamePicker : Screen("game_picker")
    object PlayerPicker : Screen("player_picker/{gameType}") {
        fun build(gameType: String) = "player_picker/$gameType"
    }
    object Setup : Screen("setup/{gameType}") {
        fun build(gameType: String) = "setup/$gameType"
    }
    object ScoreEntry : Screen("score_entry/{sessionId}") {
        fun build(sessionId: String) = "score_entry/$sessionId"
    }
    object RoundHistory : Screen("round_history/{sessionId}") {
        fun build(sessionId: String) = "round_history/$sessionId"
    }
    object Summary : Screen("summary/{sessionId}") {
        fun build(sessionId: String) = "summary/$sessionId"
    }
    /** Bottom-nav destinations not built yet (Events, Stats, Profile). */
    object ComingSoon : Screen("coming_soon/{feature}") {
        fun build(feature: String) = "coming_soon/$feature"
    }
}
