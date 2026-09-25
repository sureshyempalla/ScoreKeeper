package com.scorekeeper.events

import com.scorekeeper.data.newId
import com.scorekeeper.domain.EventEntrant
import com.scorekeeper.domain.EventMatch
import com.scorekeeper.domain.EventMatchStatus
import com.scorekeeper.domain.TournamentFormat

/**
 * Builds a full set of matches for a game's entrants, ready to insert as-is.
 *
 * Single/double elimination: entrants are shuffled (a "random draw"), padded
 * with byes up to the next power of two, then paired sequentially. Byes are
 * resolved immediately -- the present entrant is auto-advanced into the next
 * round -- so what the player sees after generating a draw is already clean.
 *
 * Round robin: every entrant plays every other entrant exactly once, all in
 * one flat "Round Robin" list; there's no bracket to advance through, just a
 * standings table computed from completed matches (see [BracketGenerator]'s
 * companion in the UI layer / [EventRepository]).
 */
object BracketGenerator {

    fun generate(format: TournamentFormat, entrants: List<EventEntrant>): List<EventMatch> = when (format) {
        TournamentFormat.ROUND_ROBIN -> generateRoundRobin(entrants, "Round Robin", 0)
        TournamentFormat.SINGLE_ELIMINATION, TournamentFormat.DOUBLE_ELIMINATION -> generateElimination(entrants, 0)
        TournamentFormat.GROUP_STAGE_THEN_KNOCKOUT -> emptyList() // built by generateGroupStage + the knockout call below instead
    }

    /**
     * One round-robin schedule per group (e.g. Group A's teams play each other,
     * Group B's teams play each other, nobody crosses groups) -- the first half
     * of [TournamentFormat.GROUP_STAGE_THEN_KNOCKOUT]. [groups] maps each group's
     * label ("A", "B", ...) to its entrants; matchIndex runs continuously across
     * every group's matches so they sort predictably once stored together.
     */
    fun generateGroupStage(groups: Map<String, List<EventEntrant>>): List<EventMatch> {
        var nextIndex = 0
        return groups.toSortedMap().flatMap { (label, entrants) ->
            val matches = generateRoundRobin(entrants, "Group $label", nextIndex)
            nextIndex += matches.size
            matches
        }
    }

    /**
     * The knockout half of [TournamentFormat.GROUP_STAGE_THEN_KNOCKOUT]: a normal
     * single-elimination bracket over the qualifiers, with matchIndex continuing
     * on from [startIndex] so its matches sort after the group stage's.
     */
    fun generateKnockout(qualifiers: List<EventEntrant>, startIndex: Int): List<EventMatch> =
        generateElimination(qualifiers, startIndex)

    private fun nextPowerOfTwo(n: Int): Int {
        var size = 1
        while (size < n) size *= 2
        return size
    }

    private fun roundLabel(roundNumber: Int, numRounds: Int): String = when (numRounds - roundNumber) {
        0 -> "Final"
        1 -> "Semifinals"
        2 -> "Quarterfinals"
        else -> "Round $roundNumber"
    }

    private fun generateElimination(entrants: List<EventEntrant>, startIndex: Int): List<EventMatch> {
        if (entrants.isEmpty()) return emptyList()
        val shuffled = entrants.shuffled()
        val size = nextPowerOfTwo(maxOf(2, shuffled.size))
        val padded: List<EventEntrant?> = shuffled + List(size - shuffled.size) { null }
        val numRounds = countRounds(size)

        // Build every round's matches first (ids assigned up front so we can link
        // each match to the one it feeds into), then wire up nextMatchId/Slot.
        val roundsOfIds = mutableListOf<List<String>>()
        val draftsByRound = mutableListOf<MutableList<EventMatch>>()

        for (round in 1..numRounds) {
            val countInRound = size / (1 shl round)
            val ids = List(countInRound) { newId() }
            roundsOfIds += ids
            val label = roundLabel(round, numRounds)
            val drafts = ids.mapIndexed { index, id ->
                if (round == 1) {
                    val a = padded.getOrNull(index * 2)
                    val b = padded.getOrNull(index * 2 + 1)
                    EventMatch(
                        id = id,
                        roundLabel = label,
                        matchIndex = startIndex + index,
                        entrantAId = a?.id,
                        entrantBId = b?.id,
                        scoreA = 0,
                        scoreB = 0,
                        status = EventMatchStatus.PENDING,
                        winnerEntrantId = null,
                        nextMatchId = null,
                        nextMatchSlot = null
                    )
                } else {
                    EventMatch(
                        id = id,
                        roundLabel = label,
                        matchIndex = index,
                        entrantAId = null,
                        entrantBId = null,
                        scoreA = 0,
                        scoreB = 0,
                        status = EventMatchStatus.PENDING,
                        winnerEntrantId = null,
                        nextMatchId = null,
                        nextMatchSlot = null
                    )
                }
            }.toMutableList()
            draftsByRound += drafts
        }

        // Link each round's matches to the match they feed into in the next round.
        for (round in 1 until numRounds) {
            val thisRound = draftsByRound[round - 1]
            val nextRoundIds = roundsOfIds[round]
            for ((index, match) in thisRound.withIndex()) {
                val nextMatchId = nextRoundIds[index / 2]
                val nextSlot = index % 2
                thisRound[index] = match.copy(nextMatchId = nextMatchId, nextMatchSlot = nextSlot)
            }
        }

        // Resolve round-1 byes immediately, cascading the auto-win into round 2+.
        for ((index, match) in draftsByRound[0].withIndex()) {
            val hasA = match.entrantAId != null
            val hasB = match.entrantBId != null
            if (hasA != hasB) {
                val winnerId = match.entrantAId ?: match.entrantBId
                draftsByRound[0][index] = match.copy(status = EventMatchStatus.COMPLETE, winnerEntrantId = winnerId)
                if (numRounds > 1) {
                    placeWinnerInNextRound(draftsByRound, 1, match.nextMatchId, match.nextMatchSlot, winnerId)
                }
            }
        }

        return draftsByRound.flatten()
    }

    /** Fills [winnerId] into the slot of the match identified by [nextMatchId] within draftsByRound[roundIndex]. */
    private fun placeWinnerInNextRound(
        draftsByRound: MutableList<MutableList<EventMatch>>,
        roundIndex: Int,
        nextMatchId: String?,
        nextSlot: Int?,
        winnerId: String?
    ) {
        if (nextMatchId == null || nextSlot == null || roundIndex >= draftsByRound.size) return
        val round = draftsByRound[roundIndex]
        val pos = round.indexOfFirst { it.id == nextMatchId }
        if (pos == -1) return
        val target = round[pos]
        round[pos] = if (nextSlot == 0) target.copy(entrantAId = winnerId) else target.copy(entrantBId = winnerId)
    }

    private fun countRounds(size: Int): Int {
        var rounds = 0
        var n = size
        while (n > 1) {
            n /= 2
            rounds++
        }
        return rounds
    }

    private fun generateRoundRobin(entrants: List<EventEntrant>, label: String, startIndex: Int): List<EventMatch> {
        val list = entrants
        val matches = mutableListOf<EventMatch>()
        var index = startIndex
        for (i in list.indices) {
            for (j in i + 1 until list.size) {
                matches += EventMatch(
                    id = newId(),
                    roundLabel = label,
                    matchIndex = index++,
                    entrantAId = list[i].id,
                    entrantBId = list[j].id,
                    scoreA = 0,
                    scoreB = 0,
                    status = EventMatchStatus.PENDING,
                    winnerEntrantId = null,
                    nextMatchId = null,
                    nextMatchSlot = null
                )
            }
        }
        return matches
    }
}
