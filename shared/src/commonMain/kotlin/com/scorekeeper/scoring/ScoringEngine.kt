package com.scorekeeper.scoring

import com.scorekeeper.domain.GameRules
import com.scorekeeper.domain.GameType
import com.scorekeeper.domain.Player
import com.scorekeeper.domain.PlayerStanding
import com.scorekeeper.domain.RoundOutcome
import com.scorekeeper.domain.RoundScore

/**
 * Turns raw round entries into standings, and knows when a game is over.
 * One engine per GameType; ScoringEngine.forGameType() picks the right one.
 */
interface ScoringEngine {

    /** Running totals + rank, lowest-total-first or highest-first depending on the game. */
    fun standings(players: List<Player>, rounds: List<RoundScore>, rules: GameRules): List<PlayerStanding>

    /** True once the session should be considered over (target hit, one player left, etc). */
    fun isGameOver(standings: List<PlayerStanding>, rules: GameRules): Boolean

    /** Whether a lower cumulative total is better in this game (Rummy, Custom-lowest-wins). */
    fun lowerIsBetter(rules: GameRules): Boolean

    companion object {
        fun forGameType(type: GameType): ScoringEngine = when (type) {
            GameType.RUMMY -> RummyScoringEngine
            GameType.UNO -> UnoScoringEngine
            GameType.PHASE10 -> Phase10ScoringEngine
            GameType.CUSTOM -> CustomScoringEngine
        }
    }
}

private fun rank(totals: Map<Player, Int>, players: List<Player>, eliminated: Set<String>, lowerIsBetter: Boolean): List<PlayerStanding> {
    val ordered = players.sortedWith(
        compareBy<Player> { it.id in eliminated } // active players first
            .then(if (lowerIsBetter) compareBy { totals[it] ?: 0 } else compareByDescending { totals[it] ?: 0 })
    )
    return ordered.mapIndexed { index, player ->
        PlayerStanding(
            player = player,
            total = totals[player] ?: 0,
            isEliminated = player.id in eliminated,
            rank = index + 1
        )
    }
}

/**
 * Pool Rummy: cumulative score across hands. Drop / middle-drop / full-count
 * are just penalty amounts entered per round; a player whose cumulative total
 * exceeds the pool limit (default 200, i.e. 201+ is out) is eliminated and
 * takes no further rounds.
 */
object RummyScoringEngine : ScoringEngine {
    override fun standings(players: List<Player>, rounds: List<RoundScore>, rules: GameRules): List<PlayerStanding> {
        val totals = players.associateWith { p -> rounds.filter { it.playerId == p.id }.sumOf { it.rawScore } }
        val eliminated = players.filter { (totals[it] ?: 0) > rules.rummyPoolLimit }.map { it.id }.toSet()
        return rank(totals, players, eliminated, lowerIsBetter = true)
    }

    override fun isGameOver(standings: List<PlayerStanding>, rules: GameRules): Boolean =
        standings.count { !it.isEliminated } <= 1

    override fun lowerIsBetter(rules: GameRules) = true

    /** Penalty amount for a given round outcome, per this session's configured rules. */
    fun penaltyFor(outcome: RoundOutcome, rules: GameRules, enteredDeadwood: Int = 0): Int = when (outcome) {
        RoundOutcome.WIN -> 0
        RoundOutcome.FIRST_DROP -> rules.rummyFirstDropPenalty
        RoundOutcome.MIDDLE_DROP -> rules.rummyMiddleDropPenalty
        RoundOutcome.FULL_COUNT -> rules.rummyFullCountPenalty
        RoundOutcome.NORMAL -> enteredDeadwood.coerceAtMost(rules.rummyFullCountPenalty)
    }
}

/** Uno: points accumulate from each hand's loser tally; first to the target score "wins" (or loses, if configured). */
object UnoScoringEngine : ScoringEngine {
    override fun standings(players: List<Player>, rounds: List<RoundScore>, rules: GameRules): List<PlayerStanding> {
        val totals = players.associateWith { p -> rounds.filter { it.playerId == p.id }.sumOf { it.rawScore } }
        return rank(totals, players, emptySet(), lowerIsBetter = rules.unoLowestScoreWins)
    }

    override fun isGameOver(standings: List<PlayerStanding>, rules: GameRules): Boolean {
        if (rules.unoTargetScore <= 0) return false
        return standings.any {
            if (rules.unoLowestScoreWins) false else it.total >= rules.unoTargetScore
        }
    }

    override fun lowerIsBetter(rules: GameRules) = rules.unoLowestScoreWins
}

/** Phase 10: lowest cumulative score wins; game ends when someone completes phase 10 (tracked by round count/target). */
object Phase10ScoringEngine : ScoringEngine {
    override fun standings(players: List<Player>, rounds: List<RoundScore>, rules: GameRules): List<PlayerStanding> {
        val totals = players.associateWith { p -> rounds.filter { it.playerId == p.id }.sumOf { it.rawScore } }
        return rank(totals, players, emptySet(), lowerIsBetter = true)
    }

    override fun isGameOver(standings: List<PlayerStanding>, rules: GameRules): Boolean {
        if (rules.phase10TargetScore <= 0) return false
        return standings.any { it.total >= rules.phase10TargetScore }
    }

    override fun lowerIsBetter(rules: GameRules) = true
}

/** Generic scorepad for any game not explicitly modeled: just running totals, optional target, optional lowest-wins. */
object CustomScoringEngine : ScoringEngine {
    override fun standings(players: List<Player>, rounds: List<RoundScore>, rules: GameRules): List<PlayerStanding> {
        val totals = players.associateWith { p -> rounds.filter { it.playerId == p.id }.sumOf { it.rawScore } }
        return rank(totals, players, emptySet(), lowerIsBetter = rules.customLowestScoreWins)
    }

    override fun isGameOver(standings: List<PlayerStanding>, rules: GameRules): Boolean {
        if (rules.customTargetScore <= 0) return false
        return standings.any {
            if (rules.customLowestScoreWins) it.total <= rules.customTargetScore else it.total >= rules.customTargetScore
        }
    }

    override fun lowerIsBetter(rules: GameRules) = rules.customLowestScoreWins
}
