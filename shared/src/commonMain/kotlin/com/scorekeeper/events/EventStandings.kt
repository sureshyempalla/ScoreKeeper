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
}
