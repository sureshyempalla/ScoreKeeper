package com.scorekeeper.events

import com.scorekeeper.domain.PointRules

/**
 * Validates a completed set's final score against a game's [PointRules], and
 * computes the resulting match state from a list of completed sets. Used by
 * the Table Tennis (and future point-based-sport) score entry screen so an
 * organizer can't submit an impossible score, e.g. winning by only 1 point at
 * deuce, or a set that stopped short of the target.
 */
object PointRulesEngine {

    /**
     * True iff [scoreA]/[scoreB] is a legal *final* score for one set under
     * [rules] -- i.e. the set is actually over, not just in progress.
     */
    fun isValidCompletedSet(scoreA: Int, scoreB: Int, rules: PointRules): Boolean {
        if (scoreA < 0 || scoreB < 0 || scoreA == scoreB) return false
        val hi = maxOf(scoreA, scoreB)
        val lo = minOf(scoreA, scoreB)
        val target = rules.pointsPerSet
        if (hi < target) return false

        if (!rules.winByTwo) {
            // Straight race to the target -- the set ends the instant it's reached.
            return hi == target
        }

        val cap = rules.deuceCap
        if (cap != null && hi >= cap) {
            // Sudden-death cap: reaching it wins outright, margin no longer matters.
            return hi == cap
        }

        // Normal deuce rule: win by 2, so a completed score is either exactly the
        // target with the loser at least 2 back, or past it with an exact 2-point margin.
        return if (hi == target) lo <= target - 2 else hi - lo == 2
    }

    /** Which side (A/B) has already clinched the match given [setsWonA]/[setsWonB] sets won, or null if undecided. */
    fun matchWinnerSide(setsWonA: Int, setsWonB: Int, rules: PointRules): Char? = when {
        setsWonA >= rules.setsToWin -> 'A'
        setsWonB >= rules.setsToWin -> 'B'
        else -> null
    }

    /** How many sets each side has won, from a list of (scoreA, scoreB) completed sets. */
    fun tally(sets: List<Pair<Int, Int>>): Pair<Int, Int> {
        val a = sets.count { it.first > it.second }
        val b = sets.count { it.second > it.first }
        return a to b
    }
}
