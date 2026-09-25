package com.scorekeeper.events

import com.scorekeeper.domain.EntrantStanding
import com.scorekeeper.domain.EventEntrant
import com.scorekeeper.domain.EventGame
import com.scorekeeper.domain.EventMatchStatus
import com.scorekeeper.domain.TournamentFormat

/** Wins/losses ranking for a game's entrants, from whatever matches have completed so far. */
object EventStandings {

    fun compute(game: EventGame): List<EntrantStanding> {
        val wins = mutableMapOf<String, Int>()
        val losses = mutableMapOf<String, Int>()
        for (entrant in game.entrants) {
            wins[entrant.id] = 0
            losses[entrant.id] = 0
        }
        for (match in game.matches) {
            if (match.status != EventMatchStatus.COMPLETE) continue
            val winnerId = match.winnerEntrantId ?: continue
            val loserId = listOfNotNull(match.entrantAId, match.entrantBId).firstOrNull { it != winnerId }
            wins[winnerId] = (wins[winnerId] ?: 0) + 1
            if (loserId != null) losses[loserId] = (losses[loserId] ?: 0) + 1
        }
        return game.entrants
            .map { entrant ->
                Triple(entrant, wins[entrant.id] ?: 0, losses[entrant.id] ?: 0)
            }
            .sortedByDescending { it.second }
            .mapIndexed { index, (entrant, w, l) -> EntrantStanding(entrant, w, l, index + 1) }
    }

    /**
     * The champion once a game is actually decided: elimination formats use the final
     * match's winner (null until it's played); round robin uses most wins, but only once
     * every match has been completed -- otherwise this must return null rather than
     * "whoever happens to be first with 0 wins," which was a real bug caught while
     * build-verifying the Events flow (a fresh, unplayed 2-entrant bracket showed a
     * "champion" immediately).
     */
    fun champion(game: EventGame): EventEntrant? {
        if (game.format == TournamentFormat.ROUND_ROBIN) {
            if (game.matches.isEmpty() || game.matches.any { it.status != EventMatchStatus.COMPLETE }) return null
            return compute(game).firstOrNull()?.entrant
        }
        val final = game.matches.firstOrNull { it.roundLabel == "Final" && it.status == EventMatchStatus.COMPLETE }
        return final?.let { m -> game.entrants.firstOrNull { it.id == m.winnerEntrantId } }
    }

    /** The label a group-stage match's roundLabel uses for group [label] (e.g. "A" -> "Group A"). */
    fun groupRoundLabel(label: String): String = "Group $label"

    /** Every distinct group label assigned among a game's entrants, sorted (A, B, C, ...). */
    fun groupLabels(game: EventGame): List<String> = game.entrants.mapNotNull { it.groupLabel }.distinct().sorted()

    /**
     * Wins/losses ranking scoped to one group, counting only matches between two
     * members of that group. Ties are broken by head-to-head result (per the
     * simple, easy-to-explain rule chosen for this app over a points/set-ratio
     * system) rather than left in whatever order entrants happened to load in.
     */
    fun groupStandings(game: EventGame, groupLabel: String): List<EntrantStanding> {
        val groupEntrants = game.entrants.filter { it.groupLabel == groupLabel }
        val groupEntrantIds = groupEntrants.map { it.id }.toSet()
        val groupMatches = game.matches.filter {
            it.entrantAId in groupEntrantIds && it.entrantBId in groupEntrantIds
        }
        val wins = mutableMapOf<String, Int>()
        val losses = mutableMapOf<String, Int>()
        val headToHeadWinner = mutableSetOf<Pair<String, String>>() // (winnerId, loserId)
        for (entrant in groupEntrants) {
            wins[entrant.id] = 0
            losses[entrant.id] = 0
        }
        for (match in groupMatches) {
            if (match.status != EventMatchStatus.COMPLETE) continue
            val winnerId = match.winnerEntrantId ?: continue
            val loserId = listOfNotNull(match.entrantAId, match.entrantBId).firstOrNull { it != winnerId }
            wins[winnerId] = (wins[winnerId] ?: 0) + 1
            if (loserId != null) {
                losses[loserId] = (losses[loserId] ?: 0) + 1
                headToHeadWinner += winnerId to loserId
            }
        }
        val ranked = groupEntrants.sortedWith(Comparator { x, y ->
            val byWins = (wins[y.id] ?: 0) - (wins[x.id] ?: 0)
            if (byWins != 0) return@Comparator byWins
            if ((x.id to y.id) in headToHeadWinner) return@Comparator -1
            if ((y.id to x.id) in headToHeadWinner) return@Comparator 1
            0
        })
        return ranked.mapIndexed { index, entrant -> EntrantStanding(entrant, wins[entrant.id] ?: 0, losses[entrant.id] ?: 0, index + 1) }
    }
}
