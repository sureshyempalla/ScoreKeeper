package com.scorekeeper

import com.scorekeeper.data.GameRepository
import com.scorekeeper.domain.CommunityEvent
import com.scorekeeper.domain.EntrantStanding
import com.scorekeeper.domain.EventGame
import com.scorekeeper.domain.EventTeamMode
import com.scorekeeper.domain.GameRules
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.GameType
import com.scorekeeper.domain.PlayerStanding
import com.scorekeeper.domain.RoundOutcome
import com.scorekeeper.domain.SavedPlayer
import com.scorekeeper.domain.TournamentFormat
import com.scorekeeper.events.EventStandings
import com.scorekeeper.scoring.ScoringEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A trivial Closeable so Swift can cancel a subscription without pulling in kotlinx-coroutines types. */
interface Cancellable {
    fun cancel()
}

internal class JobCancellable(private val job: Job) : Cancellable {
    override fun cancel() {
        job.cancel()
    }
}

/**
 * Platform-agnostic app-level controller. This replaces the old Compose-specific
 * AppState: it owns no UI framework types, so both the native Android (Jetpack
 * Compose) and native iOS (SwiftUI) apps can share this one implementation.
 *
 * - Android/Compose consumes [sessions] and [currentSession] directly as StateFlow
 *   (collectAsStateWithLifecycle()).
 * - Swift/iOS has no first-class StateFlow support without extra tooling, so it
 *   uses the callback-based [watchSessions]/[watchSession] instead - a Kotlin
 *   lambda parameter compiles to a plain Objective-C block, callable as an
 *   ordinary trailing closure from Swift.
 */
class AppController(private val repository: GameRepository) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val sessions: StateFlow<List<GameSession>> =
        repository.observeSessions().stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Home's "Saved Players" row -- a device-wide roster, independent of any one session. */
    val savedPlayers: StateFlow<List<SavedPlayer>> =
        repository.observeSavedPlayers().stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun watchSavedPlayers(onChange: (List<SavedPlayer>) -> Unit): Cancellable {
        val job = scope.launch { repository.observeSavedPlayers().collect { onChange(it) } }
        return JobCancellable(job)
    }

    fun addSavedPlayer(name: String) {
        scope.launch { repository.addSavedPlayer(name) }
    }

    fun deleteSavedPlayer(id: String) {
        scope.launch { repository.deleteSavedPlayer(id) }
    }

    fun watchSessions(onChange: (List<GameSession>) -> Unit): Cancellable {
        val job = scope.launch { repository.observeSessions().collect { onChange(it) } }
        return JobCancellable(job)
    }

    fun watchSession(sessionId: String, onChange: (GameSession?) -> Unit): Cancellable {
        val job = scope.launch { repository.observeSession(sessionId).collect { onChange(it) } }
        return JobCancellable(job)
    }

    fun startNewGame(
        gameType: GameType,
        sessionName: String,
        playerNames: List<String>,
        rules: GameRules,
        onCreated: (String) -> Unit
    ) {
        scope.launch {
            val sessionId = repository.createSession(sessionName, gameType, rules)
            playerNames.forEachIndexed { index, name ->
                repository.addPlayer(sessionId, name, index)
            }
            onCreated(sessionId)
        }
    }

    fun recordRound(sessionId: String, playerId: String, roundNumber: Int, rawScore: Int, outcome: RoundOutcome) {
        scope.launch {
            repository.recordRound(sessionId, playerId, roundNumber, rawScore, outcome)
        }
    }

    fun undoLastRound(sessionId: String) {
        scope.launch { repository.undoLastRound(sessionId) }
    }

    fun finishSession(sessionId: String) {
        scope.launch { repository.finishSession(sessionId) }
    }

    fun deleteSession(sessionId: String) {
        scope.launch { repository.deleteSession(sessionId) }
    }

    // --- String-id-based overloads, safe to call from Swift without touching -----
    // Kotlin enum interop (see InteropHelpers.kt for why). ------------------------

    /** Same as [startNewGame] but takes a [GameType.name] id instead of the enum. */
    fun startNewGame(
        gameTypeId: String,
        sessionName: String,
        playerNames: List<String>,
        rules: GameRules,
        onCreated: (String) -> Unit
    ) = startNewGame(GameTypes.byId(gameTypeId), sessionName, playerNames, rules, onCreated)

    /** Same as [recordRound] but takes a [RoundOutcome.name] id instead of the enum. */
    fun recordRound(sessionId: String, playerId: String, roundNumber: Int, rawScore: Int, outcomeId: String) =
        recordRound(sessionId, playerId, roundNumber, rawScore, RoundOutcomes.byId(outcomeId))

    /** Computed standings for a session - centralizes the ScoringEngine lookup so Swift never needs it. */
    fun standings(session: GameSession): List<PlayerStanding> {
        val engine = ScoringEngine.forGameType(session.gameType)
        return engine.standings(session.players, session.rounds, session.rules)
    }

    /** Whether [session] should be considered finished (target hit, one player left, etc). */
    fun isGameOver(session: GameSession): Boolean {
        val engine = ScoringEngine.forGameType(session.gameType)
        return engine.isGameOver(standings(session), session.rules)
    }

    /** The next round number to record for [session] (1-based). */
    fun nextRoundNumber(session: GameSession): Int =
        (session.rounds.maxOfOrNull { it.roundNumber } ?: 0) + 1

    // --- Community Events --------------------------------------------------

    val events: StateFlow<List<CommunityEvent>> =
        repository.observeEvents().stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun createEvent(name: String, emoji: String, dateMillis: Long, location: String?, onCreated: (String) -> Unit) {
        scope.launch { onCreated(repository.createEvent(name, emoji, dateMillis, location)) }
    }

    fun watchEvent(eventId: String, onChange: (CommunityEvent?) -> Unit): Cancellable {
        val job = scope.launch { onChange(repository.getEvent(eventId)) }
        return JobCancellable(job)
    }

    fun watchEventGames(eventId: String, onChange: (List<EventGame>) -> Unit): Cancellable {
        val job = scope.launch { repository.observeEventGames(eventId).collect { onChange(it) } }
        return JobCancellable(job)
    }

    fun deleteEvent(eventId: String) {
        scope.launch { repository.deleteEvent(eventId) }
    }

    fun addEventGame(
        eventId: String,
        sportName: String,
        emoji: String,
        format: TournamentFormat,
        teamMode: EventTeamMode,
        playersPerTeam: Int,
        onCreated: (String) -> Unit
    ) {
        scope.launch {
            onCreated(repository.addEventGame(eventId, sportName, emoji, format, teamMode, playersPerTeam))
        }
    }

    fun watchEventGameDetail(gameId: String, onChange: (EventGame?) -> Unit): Cancellable {
        val job = scope.launch { repository.observeEventGameDetail(gameId).collect { onChange(it) } }
        return JobCancellable(job)
    }

    fun generateDraw(gameId: String, eventId: String, names: List<String>) {
        scope.launch { repository.generateDraw(gameId, eventId, names) }
    }

    fun saveMatchProgress(matchId: String, scoreA: Int, scoreB: Int) {
        scope.launch { repository.saveMatchProgress(matchId, scoreA, scoreB) }
    }

    fun declareMatchWinner(matchId: String, scoreA: Int, scoreB: Int, winnerEntrantId: String) {
        scope.launch { repository.declareMatchWinner(matchId, scoreA, scoreB, winnerEntrantId) }
    }

    fun markGameCompleteIfAllMatchesDone(gameId: String) {
        scope.launch { repository.markGameCompleteIfAllMatchesDone(gameId) }
    }

    fun standingsFor(game: EventGame): List<EntrantStanding> = EventStandings.compute(game)

    fun championFor(game: EventGame) = EventStandings.champion(game)
}
