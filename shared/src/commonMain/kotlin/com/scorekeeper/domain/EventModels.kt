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
    ROUND_ROBIN("Round Robin", "Everyone plays everyone, ranked by wins. Best for small groups."),

    /**
     * Teams are split into groups, play round robin within their own group,
     * then the top entrants from each group advance into a single-elimination
     * knockout bracket. This is the real format used by multi-team sports
     * like volleyball, not something the player picks from the generic format
     * list -- [SportRules] decides when a sport uses it, and the app routes
     * to the dedicated Teams/Groups/Standings screens
     * instead of the generic AddParticipants/BracketView ones while it's set.
     */
    GROUP_STAGE_THEN_KNOCKOUT(
        "Group Stage + Playoffs",
        "Teams play round robin within their group, then the top teams advance to a knockout bracket."
    )
}

/**
 * Per-sport rules that shape the Teams/Groups flow (currently just the
 * minimum roster size a team needs). Keyed by sport name, case-insensitively,
 * so a new team sport is a one-line addition here rather than a new screen.
 */
object SportRules {
    private val minTeamSizeBySport = mapOf(
        "volleyball" to 6
    )

    /** Null means this sport has no team-sport rules (e.g. it isn't played in teams+groups). */
    fun minTeamSize(sportName: String): Int? = minTeamSizeBySport[sportName.trim().lowercase()]

    fun usesGroupStage(sportName: String): Boolean = minTeamSize(sportName) != null
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
    /** Null for a single-day event; when set, marks the last day of a multi-day event. */
    val endDateMillis: Long?,
    val location: String?,
    val createdAtMillis: Long
)

@Serializable
data class EventEntrant(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val seed: Int,
    /** Non-empty only for team-sport entrants (e.g. Volleyball's team rosters). */
    val roster: List<String> = emptyList(),
    /** Which group ("A", "B", ...) this entrant was placed in for a group-stage game; null otherwise. */
    val groupLabel: String? = null
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
