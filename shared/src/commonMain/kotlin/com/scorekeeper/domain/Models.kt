package com.scorekeeper.domain

import kotlinx.serialization.Serializable

/**
 * Every game the app knows how to score. CUSTOM is the generic fallback
 * that works for any game not explicitly modeled (Spades, Hearts, Yahtzee
 * scorepads, a house game, etc).
 */
@Serializable
enum class GameType(val displayName: String, val emoji: String) {
    UNO("Uno", "🔴"),
    RUMMY("Rummy (Pool)", "🃏"),
    PHASE10("Phase 10", "🔟"),
    CUSTOM("Custom Game", "⭐")
}

/**
 * How a single round ended for a player. Used by Rummy-style games where
 * the way you're out changes your penalty. Plain point games (Uno, Phase 10,
 * Custom) just use NORMAL.
 */
@Serializable
enum class RoundOutcome {
    NORMAL,
    WIN,
    FIRST_DROP,
    MIDDLE_DROP,
    FULL_COUNT
}

@Serializable
data class Player(
    val id: String,
    val name: String,
    val orderIndex: Int,
    val isEliminated: Boolean = false
)

@Serializable
data class RoundScore(
    val id: String,
    val playerId: String,
    val roundNumber: Int,
    val rawScore: Int,
    val outcome: RoundOutcome = RoundOutcome.NORMAL
)

/**
 * Per-game-type configurable rules. Every field has a sensible default so a
 * session can be created with zero setup, but each game screen lets the
 * player adjust the numbers that vary by house rules.
 */
@Serializable
data class GameRules(
    // Rummy (pool) -----------------------------------------------------
    val rummyPoolLimit: Int = 200,        // cumulative >= limit + 1 eliminates
    val rummyFirstDropPenalty: Int = 25,
    val rummyMiddleDropPenalty: Int = 40,
    val rummyFullCountPenalty: Int = 80,

    // Uno ----------------------------------------------------------------
    val unoTargetScore: Int = 500,        // first to reach (or first to empty hand, mode below)
    val unoLowestScoreWins: Boolean = false,

    // Phase 10 -------------------------------------------------------
    val phase10TargetScore: Int = 0,      // 0 = play until all 10 phases done, else lowest score at target

    // Custom ---------------------------------------------------------
    val customLowestScoreWins: Boolean = false,
    val customTargetScore: Int = 0        // 0 = no target, just track running totals
)

data class GameSession(
    val id: String,
    val gameType: GameType,
    val name: String,
    val createdAtMillis: Long,
    val isFinished: Boolean,
    val rules: GameRules,
    val players: List<Player> = emptyList(),
    val rounds: List<RoundScore> = emptyList()
)

/**
 * A player saved to the device's reusable roster (Home's "Saved Players" row,
 * and the picker on PlayerPicker/AddParticipants) -- independent of any one
 * session's [Player] rows. [colorIndex] picks a stable avatar color, cycled
 * by the UI rather than stored as a literal color so the palette can change
 * without a migration.
 */
@Serializable
data class SavedPlayer(
    val id: String,
    val name: String,
    val colorIndex: Int
)

/** A player's computed standing within a session, ready for the UI to render. */
data class PlayerStanding(
    val player: Player,
    val total: Int,
    val isEliminated: Boolean,
    val rank: Int
)
