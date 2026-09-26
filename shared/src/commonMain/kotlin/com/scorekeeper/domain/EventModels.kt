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

/**
 * One completed set within a point-based-sport match (e.g. Table Tennis).
 * [scoreA]/[scoreB] are the final points each side reached in that set.
 */
@Serializable
data class EventSet(
    val setNumber: Int,
    val scoreA: Int,
    val scoreB: Int
)

@Serializable
data class EventMatch(
    val id: String,
    val roundLabel: String,
    val matchIndex: Int,
    val entrantAId: String?,
    val entrantBId: String?,
    /** For a point-based-sport match ([EventGame.pointRules] != null) this is sets won, not raw points. */
    val scoreA: Int,
    val scoreB: Int,
    val status: EventMatchStatus,
    val winnerEntrantId: String?,
    val nextMatchId: String?,
    val nextMatchSlot: Int?,
    /** Non-empty only for point-based-sport matches; each entry is one completed set's final score. */
    val sets: List<EventSet> = emptyList()
)

/**
 * Configurable match rules for a point-based racket sport (Table Tennis today;
 * Badminton/Tennis could reuse this later). A match is won by the first side
 * to reach [setsToWin] sets (a majority of [bestOfSets]); each set is won by
 * the first side to reach [pointsPerSet] points, with a required 2-point
 * margin at deuce unless [winByTwo] is false, or capped hard at [deuceCap]
 * points regardless of margin once either side reaches it (null = no cap).
 */
@Serializable
data class PointRules(
    val pointsPerSet: Int = 11,
    val bestOfSets: Int = 3,
    val winByTwo: Boolean = true,
    val deuceCap: Int? = null
) {
    /** Sets needed to win the match -- a majority of [bestOfSets] (e.g. best-of-3 -> 2). */
    val setsToWin: Int get() = (bestOfSets / 2) + 1
}

/**
 * Point-based racket sports whose matches use [PointRules] (configurable
 * sets/points/win-by-2) instead of a single raw score. Keyed by sport name,
 * case-insensitively, so adding Badminton later is a one-line change here.
 */
object PointBasedSports {
    private val names = setOf("table tennis", "ping pong")

    fun usesPointRules(sportName: String): Boolean = sportName.trim().lowercase() in names
}

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
    val matches: List<EventMatch> = emptyList(),
    /** Non-null only for [PointBasedSports] like Table Tennis; drives set-by-set score entry. */
    val pointRules: PointRules? = null
)

/** A game's entrants ranked by result, used both for round-robin standings and for the results screen. */
data class EntrantStanding(
    val entrant: EventEntrant,
    val wins: Int,
    val losses: Int,
    val rank: Int
)
