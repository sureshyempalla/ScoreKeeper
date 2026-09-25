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
    /** Bottom-nav destinations not built yet (Stats, Profile). */
    object ComingSoon : Screen("coming_soon/{feature}") {
        fun build(feature: String) = "coming_soon/$feature"
    }

    // --- Community Events ---
    object EventsHome : Screen("events_home")
    object CreateEvent : Screen("create_event")
    object EventDashboard : Screen("event_dashboard/{eventId}") {
        fun build(eventId: String) = "event_dashboard/$eventId"
    }
    object AddSport : Screen("add_sport/{eventId}") {
        fun build(eventId: String) = "add_sport/$eventId"
    }
    /** Sport name/emoji chosen on AddSport are hoisted, not passed as route args. */
    object ConfigureGame : Screen("configure_game/{eventId}") {
        fun build(eventId: String) = "configure_game/$eventId"
    }
    object AddParticipants : Screen("add_participants/{gameId}") {
        fun build(gameId: String) = "add_participants/$gameId"
    }
    /**
     * Team-sport games (Volleyball etc.): one route hosts Teams -> Groups ->
     * Standings -> (knockout) Bracket as a single state-driven flow, switching
     * which screen it renders as the game's entrants/matches change, rather
     * than four separate routes the caller has to know how to sequence.
     */
    object VolleyballFlow : Screen("volleyball_flow/{gameId}") {
        fun build(gameId: String) = "volleyball_flow/$gameId"
    }
    object BracketView : Screen("bracket_view/{gameId}") {
        fun build(gameId: String) = "bracket_view/$gameId"
    }
    /** Needs both ids: the match to score, and its game (for entrant names / re-fetch after save). */
    object MatchScore : Screen("match_score/{gameId}/{matchId}") {
        fun build(gameId: String, matchId: String) = "match_score/$gameId/$matchId"
    }
    object EventResults : Screen("event_results/{eventId}") {
        fun build(eventId: String) = "event_results/$eventId"
    }
}
