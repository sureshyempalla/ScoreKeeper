package com.scorekeeper.domain

import kotlinx.serialization.Serializable

/**
 * How a single game/sport within an event is bracketed.
 *
 * NOTE: DOUBLE_ELIMINATION is selectable in Setup to match the wireframe, but
 * this first pass generates the same single-elimination bracket under the hood
 * (lose once, you're out) — a full winners/losers-bracket implementation is a
 * larger follow-up. It's tracked here as its own value so that follow-up is a
 * bracket-generation change only, not a schema or UI change.
 */
@Serializable
enum class TournamentFormat(val displayName: String, val blurb: String) {
    SINGLE_ELIMINATION("Single Elimination", "Lose once, you're out. Fastest format, works well with limited time."),
    DOUBLE_ELIMINATION("Double Elimination", "Need to lose twice to be out. Fairer, takes longer to play out."),
    ROUND_ROBIN("Round Robin", "Everyone plays everyone, ranked by wins. Best for small groups.")
}

@Serializable
enum class EventTeamMode(val displayName: String) {
    SINGLES("Singles"),
    DOUBLES("Doubles"),
    TEAMS("Teams")
}

@Serializable
enum class EventGameStatus {
    SETUP,       // no entrants yet
    DRAW_READY,  // entrants added, bracket/schedule generated, nothing played yet
    IN_PROGRESS, // at least one match completed or live
    COMPLETE     // champion decided (or, for round robin, explicitly marked done)
}

@Serializable
enum class EventMatchStatus { PENDING, LIVE, COMPLETE }

@Serializable
data class CommunityEvent(
    val id: String,
    val name: String,
    val emoji: String,
    val dateMillis: Long,
    val location: String?,
    val createdAtMillis: Long
)

@Serializable
data class EventEntrant(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val seed: Int
)

@Serializable
data class EventMatch(
    val id: String,
    val roundLabel: String,
    val matchIndex: Int,
    val entrantAId: String?,
    val entrantBId: String?,
    val scoreA: Int,
    val scoreB: Int,
    val status: EventMatchStatus,
    val winnerEntrantId: String?,
    val nextMatchId: String?,
    val nextMatchSlot: Int?
)

data class EventGame(
    val id: String,
    val eventId: String,
    val sportName: String,
    val emoji: String,
    val format: TournamentFormat,
    val teamMode: EventTeamMode,
    val playersPerTeam: Int,
    val status: EventGameStatus,
    val orderIndex: Int,
    val entrants: List<EventEntrant> = emptyList(),
    val matches: List<EventMatch> = emptyList()
)

/** A game's entrants ranked by result, used both for round-robin standings and for the results screen. */
data class EntrantStanding(
    val entrant: EventEntrant,
    val wins: Int,
    val losses: Int,
    val rank: Int
)
