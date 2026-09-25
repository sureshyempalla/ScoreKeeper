package com.scorekeeper

import com.scorekeeper.data.DatabaseDriverFactory
import com.scorekeeper.data.GameRepository
import com.scorekeeper.domain.GameRules
import com.scorekeeper.domain.GameType
import com.scorekeeper.domain.RoundOutcome
import com.scorekeeper.scoring.RummyScoringEngine

/**
 * Swift-friendly helpers that sidestep Kotlin/Native's Objective-C interop for enums.
 *
 * Kotlin/Native *does* expose enum entries to Swift (as static members, name-mangled
 * from SCREAMING_SNAKE_CASE to camelCase - e.g. `RoundOutcome.FIRST_DROP` becomes
 * `.firstDrop`), but that mangling isn't something this environment can compile and
 * verify against a real Xcode toolchain. Rather than have the iOS app depend on a
 * naming convention nobody has confirmed here, every enum crossing the Swift boundary
 * goes through a plain string id instead: [GameType.name] / [RoundOutcome.name],
 * round-tripped with [GameTypes]/[RoundOutcomes] below. Plain Kotlin `object`s with
 * ordinary functions (no default arguments) are the one interop shape that is
 * well-documented to bridge predictably.
 */
object GameTypes {
    /** All playable game types, in declaration order - for the game-picker grid. */
    val all: List<GameType> = GameType.entries.toList()

    fun idOf(type: GameType): String = type.name

    fun byId(id: String): GameType = GameType.entries.firstOrNull { it.name == id } ?: GameType.CUSTOM
}

object RoundOutcomes {
    val all: List<RoundOutcome> = RoundOutcome.entries.toList()

    fun idOf(outcome: RoundOutcome): String = outcome.name

    fun byId(id: String): RoundOutcome = RoundOutcome.entries.firstOrNull { it.name == id } ?: RoundOutcome.NORMAL
}

/**
 * `GameRules` is a data class with default parameter values, which Kotlin/Native does
 * NOT carry over to the generated Objective-C/Swift API - every constructor or
 * `copy()` call from Swift would have to pass all nine fields explicitly. These two
 * factories are the only two shapes the iOS app actually needs, each with every
 * parameter spelled out so there's nothing implicit for Swift to get wrong.
 */
fun defaultGameRules(): GameRules = GameRules()

fun rummyRules(
    poolLimit: Int,
    firstDropPenalty: Int,
    middleDropPenalty: Int,
    fullCountPenalty: Int
): GameRules = GameRules(
    rummyPoolLimit = poolLimit,
    rummyFirstDropPenalty = firstDropPenalty,
    rummyMiddleDropPenalty = middleDropPenalty,
    rummyFullCountPenalty = fullCountPenalty
)

/** The Rummy penalty for a given outcome id, looked up through [RoundOutcomes]. */
fun rummyPenaltyForOutcomeId(outcomeId: String, rules: GameRules, enteredDeadwood: Int): Int =
    RummyScoringEngine.penaltyFor(RoundOutcomes.byId(outcomeId), rules, enteredDeadwood)

/**
 * `GameRepository`'s `ioDispatcher` parameter has a default value, which - like
 * `GameRules` above - Kotlin/Native does not expose to Swift. This factory takes
 * only what the iOS app actually supplies (a [DatabaseDriverFactory]) and lets the
 * default `Dispatchers.Default` apply on the Kotlin side, where defaults still work.
 */
fun createGameRepository(driverFactory: DatabaseDriverFactory): GameRepository =
    GameRepository(driverFactory)
