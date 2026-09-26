package com.scorekeeper

import com.scorekeeper.data.EventSyncRepository
import com.scorekeeper.data.GameRepository
import com.scorekeeper.domain.CommunityEvent
import com.scorekeeper.domain.EntrantStanding
import com.scorekeeper.domain.EventGame
import com.scorekeeper.domain.EventTeamMode
import com.scorekeeper.domain.GameRules
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.GameType
import com.scorekeeper.domain.PlayerStanding
import com.scorekeeper.domain.PointRules
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
import kotlinx.datetime.Clock

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
    private val syncRepository = EventSyncRepository()

    // --- Sync (Phase 1: on-demand, not realtime -- see EventSyncRepository) ---

    /** Set by the UI whenever [AuthController.uiState] changes; null while signed out/guest. Auto-push only runs while this is set. */
    private var currentUserId: String? = null

    /**
     * Called by the UI (Android: LaunchedEffect on authController.uiState;
     * iOS: didSet on the observed uid) whenever sign-in state changes. Kicks
     * off one sync immediately on sign-in, so a second device's events show
     * up here right away rather than waiting for the next score/manual sync.
     */
    fun setCurrentUserId(uid: String?) {
        val changed = currentUserId != uid
        currentUserId = uid
        if (changed && uid != null) syncNow()
    }

    /** Pushes every local event to the server, then pulls down any event this device doesn't have yet (see EventSyncRepository's merge rule). */
    fun syncNow(onResult: (Boolean) -> Unit = {}) {
        val uid = currentUserId
        if (uid == null) {
            onResult(false)
            return
        }
        scope.launch {
            val ok = runCatching {
                repository.getAllFullEvents().forEach { syncRepository.push(uid, it) }
                val known = repository.knownEventIds()
                syncRepository.remoteEventIds(uid).filterNot { it in known }.forEach { eventId ->
                    syncRepository.pull(uid, eventId)?.let { repository.importEventBundle(it) }
                }
                repository.setDeviceState("lastSyncedMillis", Clock.System.now().toEpochMilliseconds().toString())
            }.isSuccess
            onResult(ok)
        }
    }

    fun getLastSyncedMillis(onResult: (Long?) -> Unit) {
        scope.launch { onResult(repository.getDeviceState("lastSyncedMillis")?.toLongOrNull()) }
    }

    /** Fire-and-forget push of one event after a score-affecting action, while signed in. Failures are silent -- the next syncNow() (auto on next sign-in, or manual) catches up. */
    private fun autoPushEvent(eventId: String) {
        val uid = currentUserId ?: return
        scope.launch { runCatching { repository.getFullEvent(eventId)?.let { syncRepository.push(uid, it) } } }
    }

    /** Same as [autoPushEvent] but starting from a gameId, looking up its parent eventId first. */
    private fun autoPushEventForGame(gameId: String) {
        if (currentUserId == null) return
        scope.launch {
            val eventId = repository.getEventGameDetail(gameId)?.eventId ?: return@launch
            autoPushEvent(eventId)
        }
    }

    /** Same as [autoPushEvent] but starting from a matchId (match -> game -> event). */
    private fun autoPushEventForMatch(matchId: String) {
        if (currentUserId == null) return
        scope.launch {
            val eventId = repository.eventIdForMatch(matchId) ?: return@launch
            autoPushEvent(eventId)
        }
    }

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

    fun createEvent(
        name: String,
        emoji: String,
        dateMillis: Long,
        endDateMillis: Long?,
        location: String?,
        onCreated: (String) -> Unit
    ) {
        scope.launch { onCreated(repository.createEvent(name, emoji, dateMillis, endDateMillis, location)) }
    }

    fun watchEvent(eventId: String, onChange: (CommunityEvent?) -> Unit): Cancellable {
        val job = scope.launch { onChange(repository.getEvent(eventId)) }
        return JobCancellable(job)
    }

    fun watchEventGames(eventId: String, onChange: (List<EventGame>) -> Unit): Cancellable {
        val job = scope.launch { repository.observeEventGames(eventId).collect { onChange(it) } }
        return JobCancellable(job)
    }

    fun deleteEvent(eventId: String, onDeleted: () -> Unit = {}) {
        scope.launch { repository.deleteEvent(eventId); onDeleted() }
    }

    fun addEventGame(
        eventId: String,
        sportName: String,
        emoji: String,
        format: TournamentFormat,
        teamMode: EventTeamMode,
        playersPerTeam: Int,
        pointRules: PointRules? = null,
        onCreated: (String) -> Unit
    ) {
        scope.launch {
            onCreated(repository.addEventGame(eventId, sportName, emoji, format, teamMode, playersPerTeam, pointRules))
            autoPushEvent(eventId)
        }
    }

    fun watchEventGameDetail(gameId: String, onChange: (EventGame?) -> Unit): Cancellable {
        val job = scope.launch { repository.observeEventGameDetail(gameId).collect { onChange(it) } }
        return JobCancellable(job)
    }

    fun updateEventGameConfig(
        gameId: String,
        sportName: String,
        emoji: String,
        format: TournamentFormat,
        teamMode: EventTeamMode,
        playersPerTeam: Int,
        pointRules: PointRules? = null,
        onUpdated: () -> Unit = {}
    ) {
        scope.launch {
            repository.updateEventGameConfig(gameId, sportName, emoji, format, teamMode, playersPerTeam, pointRules)
            onUpdated()
        }
    }

    fun deleteEventGame(gameId: String, onDeleted: () -> Unit = {}) {
        scope.launch { repository.deleteEventGame(gameId); onDeleted() }
    }

    fun generateDraw(gameId: String, eventId: String, names: List<String>) {
        scope.launch { repository.generateDraw(gameId, eventId, names); autoPushEvent(eventId) }
    }

    fun saveMatchProgress(matchId: String, scoreA: Int, scoreB: Int) {
        scope.launch { repository.saveMatchProgress(matchId, scoreA, scoreB); autoPushEventForMatch(matchId) }
    }

    fun declareMatchWinner(matchId: String, scoreA: Int, scoreB: Int, winnerEntrantId: String) {
        scope.launch {
            repository.declareMatchWinner(matchId, scoreA, scoreB, winnerEntrantId)
            autoPushEventForMatch(matchId)
        }
    }

    fun markGameCompleteIfAllMatchesDone(gameId: String) {
        scope.launch { repository.markGameCompleteIfAllMatchesDone(gameId) }
    }

    // --- Point-based-sport scoring (Table Tennis etc) ---

    fun recordSetScore(matchId: String, scoreA: Int, scoreB: Int, onResult: (Boolean) -> Unit = {}) {
        scope.launch {
            val recorded = repository.recordSetScore(matchId, scoreA, scoreB)
            if (recorded) autoPushEventForMatch(matchId)
            onResult(recorded)
        }
    }

    fun undoLastSet(matchId: String) {
        scope.launch { repository.undoLastSet(matchId); autoPushEventForMatch(matchId) }
    }

    fun standingsFor(game: EventGame): List<EntrantStanding> = EventStandings.compute(game)

    fun championFor(game: EventGame) = EventStandings.champion(game)

    // --- Team-sport flow (Volleyball etc.) ---

    fun addTeam(gameId: String, teamName: String, roster: List<String>, onAdded: () -> Unit = {}) {
        scope.launch { repository.addTeam(gameId, teamName, roster); onAdded() }
    }

    fun updateTeamRoster(entrantId: String, roster: List<String>) {
        scope.launch { repository.updateTeamRoster(entrantId, roster) }
    }

    fun updateTeam(entrantId: String, teamName: String, roster: List<String>) {
        scope.launch { repository.updateTeam(entrantId, teamName, roster) }
    }

    fun removeTeam(entrantId: String) {
        scope.launch { repository.removeTeam(entrantId) }
    }

    fun autoAssignGroups(gameId: String, groupCount: Int) {
        scope.launch { repository.autoAssignGroups(gameId, groupCount) }
    }

    fun moveEntrantToGroup(entrantId: String, groupLabel: String) {
        scope.launch { repository.moveEntrantToGroup(entrantId, groupLabel) }
    }

    fun resetGroups(gameId: String) {
        scope.launch { repository.resetGroups(gameId) }
    }

    fun startGroupStageDraw(gameId: String, onStarted: () -> Unit = {}) {
        scope.launch { repository.startGroupStageDraw(gameId); autoPushEventForGame(gameId); onStarted() }
    }

    fun advanceToPlayoffs(gameId: String, onAdvanced: () -> Unit = {}) {
        scope.launch { repository.advanceToPlayoffs(gameId); autoPushEventForGame(gameId); onAdvanced() }
    }

    fun groupLabelsFor(game: EventGame): List<String> = EventStandings.groupLabels(game)

    fun groupStandingsFor(game: EventGame, groupLabel: String): List<EntrantStanding> =
        EventStandings.groupStandings(game, groupLabel)
}
