package com.scorekeeper.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.scorekeeper.db.EventEntity
import com.scorekeeper.db.EventEntrantEntity
import com.scorekeeper.db.EventGameEntity
import com.scorekeeper.db.EventMatchEntity
import com.scorekeeper.db.GameSessionEntity
import com.scorekeeper.db.PlayerEntity
import com.scorekeeper.db.RoundScoreEntity
import com.scorekeeper.db.SavedPlayerEntity
import com.scorekeeper.db.ScoreKeeperDatabase
import com.scorekeeper.domain.CommunityEvent
import com.scorekeeper.domain.EventEntrant
import com.scorekeeper.domain.EventGame
import com.scorekeeper.domain.EventGameStatus
import com.scorekeeper.domain.EventMatch
import com.scorekeeper.domain.EventMatchStatus
import com.scorekeeper.domain.EventTeamMode
import com.scorekeeper.domain.GameRules
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.GameType
import com.scorekeeper.domain.Player
import com.scorekeeper.domain.RoundOutcome
import com.scorekeeper.domain.RoundScore
import com.scorekeeper.domain.SavedPlayer
import com.scorekeeper.domain.TournamentFormat
import com.scorekeeper.events.BracketGenerator
import com.scorekeeper.events.EventStandings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

private val json = Json { ignoreUnknownKeys = true }

/** Separator for EventEntrantEntity.roster -- \u0001 so a player's own name can never contain it. */
private const val ROSTER_SEPARATOR = "\u0001"

/** Simple, dependency-free unique id — good enough for local-only rows. */
fun newId(): String {
    val time = Clock.System.now().toEpochMilliseconds()
    val rand = Random.nextInt(0, Int.MAX_VALUE)
    return "$time-$rand"
}

class GameRepository(
    driverFactory: DatabaseDriverFactory,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val db = ScoreKeeperDatabase(driverFactory.createDriver())
    private val q = db.scoreKeeperQueries

    suspend fun createSession(name: String, gameType: GameType, rules: GameRules): String = withContext(ioDispatcher) {
        val id = newId()
        q.insertSession(
            id = id,
            gameType = gameType.name,
            name = name,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            isFinished = 0,
            rulesJson = json.encodeToString(rules)
        )
        id
    }

    suspend fun addPlayer(sessionId: String, name: String, orderIndex: Int): String = withContext(ioDispatcher) {
        val id = newId()
        q.insertPlayer(id = id, sessionId = sessionId, name = name, orderIndex = orderIndex.toLong())
        id
    }

    suspend fun recordRound(
        sessionId: String,
        playerId: String,
        roundNumber: Int,
        rawScore: Int,
        outcome: RoundOutcome
    ) = withContext(ioDispatcher) {
        q.insertRoundScore(
            id = newId(),
            sessionId = sessionId,
            playerId = playerId,
            roundNumber = roundNumber.toLong(),
            rawScore = rawScore.toLong(),
            outcomeTag = outcome.name,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    suspend fun undoLastRound(sessionId: String) = withContext(ioDispatcher) {
        q.deleteLastRoundForSession(sessionId, sessionId)
    }

    suspend fun setPlayerEliminated(playerId: String, eliminated: Boolean) = withContext(ioDispatcher) {
        q.setPlayerEliminated(if (eliminated) 1 else 0, playerId)
    }

    suspend fun finishSession(sessionId: String) = withContext(ioDispatcher) {
        q.markSessionFinished(sessionId)
    }

    suspend fun deleteSession(sessionId: String) = withContext(ioDispatcher) {
        q.deleteSession(sessionId)
    }

    fun observeSessions(): Flow<List<GameSession>> =
        q.selectAllSessions().asFlow().mapToList(ioDispatcher).map { rows -> rows.map { it.toDomainShallow() } }

    suspend fun getSession(sessionId: String): GameSession? = withContext(ioDispatcher) {
        val session = q.selectSessionById(sessionId).executeAsOneOrNull() ?: return@withContext null
        val players = q.selectPlayersBySession(sessionId).executeAsList().map { it.toDomain() }
        val rounds = q.selectRoundScoresBySession(sessionId).executeAsList().map { it.toDomain() }
        session.toDomain(players, rounds)
    }

    /**
     * Emits a fresh GameSession whenever the session row, its players, OR its round
     * scores change. SQLDelight's asFlow() only re-emits when the tables that specific
     * query reads from change, so we combine all three queries' flows rather than just
     * the session query -- otherwise recording a round would never refresh the UI.
     */
    fun observeSession(sessionId: String): Flow<GameSession?> {
        val sessionFlow = q.selectSessionById(sessionId).asFlow().mapToOneOrNull(ioDispatcher)
        val playersFlow = q.selectPlayersBySession(sessionId).asFlow().mapToList(ioDispatcher)
        val roundsFlow = q.selectRoundScoresBySession(sessionId).asFlow().mapToList(ioDispatcher)

        return combine(sessionFlow, playersFlow, roundsFlow) { session, players, rounds ->
            session?.toDomain(players.map { it.toDomain() }, rounds.map { it.toDomain() })
        }
    }

    /** Convenience one-shot fetch used right after writes when a Flow re-emit isn't guaranteed synchronously. */
    suspend fun refreshedSession(sessionId: String): GameSession? = getSession(sessionId)

    // Saved players (device-wide roster, independent of any one session) ------

    suspend fun addSavedPlayer(name: String): String = withContext(ioDispatcher) {
        val id = newId()
        val existingCount = q.selectAllSavedPlayers().executeAsList().size
        q.insertSavedPlayer(
            id = id,
            name = name,
            colorIndex = existingCount.toLong(),
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        id
    }

    suspend fun deleteSavedPlayer(id: String) = withContext(ioDispatcher) {
        q.deleteSavedPlayer(id)
    }

    fun observeSavedPlayers(): Flow<List<SavedPlayer>> =
        q.selectAllSavedPlayers().asFlow().mapToList(ioDispatcher).map { rows -> rows.map { it.toDomain() } }

    // Events -----------------------------------------------------------------

    suspend fun createEvent(
        name: String,
        emoji: String,
        dateMillis: Long,
        endDateMillis: Long?,
        location: String?
    ): String =
        withContext(ioDispatcher) {
            val id = newId()
            q.insertEvent(
                id = id,
                name = name,
                emoji = emoji,
                eventDate = dateMillis,
                endDate = endDateMillis,
                location = location,
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
            id
        }

    fun observeEvents(): Flow<List<CommunityEvent>> =
        q.selectAllEvents().asFlow().mapToList(ioDispatcher).map { rows -> rows.map { it.toDomain() } }

    suspend fun getEvent(eventId: String): CommunityEvent? = withContext(ioDispatcher) {
        q.selectEventById(eventId).executeAsOneOrNull()?.toDomain()
    }

    suspend fun deleteEvent(eventId: String) = withContext(ioDispatcher) {
        q.deleteEvent(eventId)
    }

    suspend fun addEventGame(
        eventId: String,
        sportName: String,
        emoji: String,
        format: TournamentFormat,
        teamMode: EventTeamMode,
        playersPerTeam: Int
    ): String = withContext(ioDispatcher) {
        val id = newId()
        val orderIndex = q.selectGamesByEvent(eventId).executeAsList().size
        q.insertEventGame(
            id = id,
            eventId = eventId,
            sportName = sportName,
            emoji = emoji,
            format = format.name,
            teamMode = teamMode.name,
            playersPerTeam = playersPerTeam.toLong(),
            status = EventGameStatus.SETUP.name,
            orderIndex = orderIndex.toLong()
        )
        id
    }

    /**
     * Edits a game's own settings. Name/emoji can be renamed any time; the UI only
     * lets format/teamMode/playersPerTeam change while the game still has no
     * entrants (ConfigureGameScreen's "locked" mode), so this never needs to touch
     * any existing draw/entrants itself.
     */
    suspend fun updateEventGameConfig(
        gameId: String,
        sportName: String,
        emoji: String,
        format: TournamentFormat,
        teamMode: EventTeamMode,
        playersPerTeam: Int
    ) = withContext(ioDispatcher) {
        q.updateEventGameConfig(sportName, emoji, format.name, teamMode.name, playersPerTeam.toLong(), gameId)
    }

    /** Removes a game entirely; entrants/matches cascade-delete with it. */
    suspend fun deleteEventGame(gameId: String) = withContext(ioDispatcher) {
        q.deleteEventGame(gameId)
    }

    suspend fun getEventGames(eventId: String): List<EventGame> = withContext(ioDispatcher) {
        q.selectGamesByEvent(eventId).executeAsList().map { it.toDomainShallow() }
    }

    fun observeEventGames(eventId: String): Flow<List<EventGame>> =
        q.selectGamesByEvent(eventId).asFlow().mapToList(ioDispatcher).map { rows -> rows.map { it.toDomainShallow() } }

    /** Full detail for one event game -- entrants and matches included. */
    suspend fun getEventGameDetail(gameId: String): EventGame? = withContext(ioDispatcher) {
        val game = q.selectGameById(gameId).executeAsOneOrNull() ?: return@withContext null
        val entrants = q.selectEntrantsByGame(gameId).executeAsList().map { it.toDomain() }
        val matches = q.selectMatchesByGame(gameId).executeAsList().map { it.toDomain() }
        game.toDomainShallow().copy(entrants = entrants, matches = matches)
    }

    fun observeEventGameDetail(gameId: String): Flow<EventGame?> {
        val gameFlow = q.selectGameById(gameId).asFlow().mapToOneOrNull(ioDispatcher)
        val entrantsFlow = q.selectEntrantsByGame(gameId).asFlow().mapToList(ioDispatcher)
        val matchesFlow = q.selectMatchesByGame(gameId).asFlow().mapToList(ioDispatcher)
        return combine(gameFlow, entrantsFlow, matchesFlow) { game, entrants, matches ->
            game?.toDomainShallow()?.copy(
                entrants = entrants.map { it.toDomain() },
                matches = matches.map { it.toDomain() }
            )
        }
    }

    /**
     * Replaces this game's entrants and generates a fresh draw for them --
     * used both for the first draw and for "re-shuffle". [names] is the raw
     * roster; each becomes one seeded entrant.
     */
    suspend fun generateDraw(gameId: String, eventId: String, names: List<String>) = withContext(ioDispatcher) {
        q.deleteMatchesByGame(gameId)
        q.deleteEntrantsByGame(gameId)
        val entrants = names.mapIndexed { index, name ->
            val id = newId()
            q.insertEntrant(id = id, eventGameId = gameId, name = name, colorIndex = index.toLong(), seed = index.toLong(), roster = "", groupLabel = null)
            EventEntrant(id = id, name = name, colorIndex = index, seed = index)
        }
        val game = q.selectGameById(gameId).executeAsOne()
        val format = TournamentFormat.valueOf(game.format)
        val matches = BracketGenerator.generate(format, entrants)
        insertMatches(gameId, matches)
        q.updateGameStatus(EventGameStatus.DRAW_READY.name, gameId)
    }

    private fun insertMatches(gameId: String, matches: List<EventMatch>) {
        matches.forEach { m ->
            q.insertMatch(
                id = m.id,
                eventGameId = gameId,
                roundLabel = m.roundLabel,
                matchIndex = m.matchIndex.toLong(),
                entrantAId = m.entrantAId,
                entrantBId = m.entrantBId,
                scoreA = m.scoreA.toLong(),
                scoreB = m.scoreB.toLong(),
                status = m.status.name,
                winnerEntrantId = m.winnerEntrantId,
                nextMatchId = m.nextMatchId,
                nextMatchSlot = m.nextMatchSlot?.toLong()
            )
        }
    }

    // --- Team-sport flow (Volleyball etc.): teams with rosters -> groups -> group-stage round
    // robin -> knockout playoffs. Kept separate from generateDraw's individual-entrant path
    // above since teams are added one at a time with a roster, not all at once from a name list.

    /** Adds one team (an entrant with a roster) to a team-sport game; returns its new entrant id. */
    suspend fun addTeam(gameId: String, teamName: String, roster: List<String>): String = withContext(ioDispatcher) {
        val id = newId()
        val nextSeed = q.selectEntrantsByGame(gameId).executeAsList().size
        q.insertEntrant(
            id = id,
            eventGameId = gameId,
            name = teamName,
            colorIndex = nextSeed.toLong(),
            seed = nextSeed.toLong(),
            roster = roster.joinToString(ROSTER_SEPARATOR),
            groupLabel = null
        )
        id
    }

    suspend fun updateTeamRoster(entrantId: String, roster: List<String>) = withContext(ioDispatcher) {
        q.updateEntrantRoster(roster.joinToString(ROSTER_SEPARATOR), entrantId)
    }

    /** Renames a team and replaces its roster in one go -- what the Teams screen's edit dialog calls. */
    suspend fun updateTeam(entrantId: String, teamName: String, roster: List<String>) = withContext(ioDispatcher) {
        q.updateEntrantName(teamName, entrantId)
        q.updateEntrantRoster(roster.joinToString(ROSTER_SEPARATOR), entrantId)
    }

    suspend fun removeTeam(entrantId: String) = withContext(ioDispatcher) {
        q.deleteEntrant(entrantId)
    }

    /**
     * Splits a game's current entrants into [groupCount] groups as evenly as
     * possible (e.g. 9 teams into 2 groups -> 5/4), persisting the suggested
     * groupLabel on every entrant. Callers (the Groups screen) can move any
     * entrant to a different group afterward with [moveEntrantToGroup] before
     * confirming; calling this again re-suggests from scratch.
     */
    suspend fun autoAssignGroups(gameId: String, groupCount: Int) = withContext(ioDispatcher) {
        val entrants = q.selectEntrantsByGame(gameId).executeAsList()
        val labels = ('A' until 'A' + groupCount).map { it.toString() }
        // Deal entrants round-robin-style across the groups so an uneven split
        // spreads the extra teams across different groups rather than dumping
        // them all into the first one.
        entrants.forEachIndexed { index, entrant ->
            q.updateEntrantGroup(labels[index % labels.size], entrant.id)
        }
    }

    suspend fun moveEntrantToGroup(entrantId: String, groupLabel: String) = withContext(ioDispatcher) {
        q.updateEntrantGroup(groupLabel, entrantId)
    }

    /**
     * Clears every entrant's groupLabel, sending a GROUP_STAGE_THEN_KNOCKOUT game
     * back to its Teams stage (VolleyballFlow's routing is state-driven: once no
     * entrant has a group, it renders VolleyballTeamsScreen again). This is what
     * the Groups screen's "Edit Teams" link calls -- there was previously no way
     * back to Teams once groups were assigned, so a team's roster/name became
     * uneditable the moment the organizer moved past that screen.
     */
    suspend fun resetGroups(gameId: String) = withContext(ioDispatcher) {
        q.selectEntrantsByGame(gameId).executeAsList().forEach { q.updateEntrantGroup(null, it.id) }
    }

    /** Generates each group's round-robin schedule and moves the game into its group stage. */
    suspend fun startGroupStageDraw(gameId: String) = withContext(ioDispatcher) {
        q.deleteMatchesByGame(gameId)
        val entrants = q.selectEntrantsByGame(gameId).executeAsList().map { it.toDomain() }
        val groups = entrants.filter { it.groupLabel != null }.groupBy { it.groupLabel!! }
        val matches = BracketGenerator.generateGroupStage(groups)
        insertMatches(gameId, matches)
        q.updateGameStatus(EventGameStatus.DRAW_READY.name, gameId)
    }

    /**
     * Takes the top [perGroup] entrants (by [com.scorekeeper.events.EventStandings.groupStandings])
     * from every group, seeds them into a fresh single-elimination bracket appended after the
     * group-stage matches, and moves the game into its knockout phase.
     */
    suspend fun advanceToPlayoffs(gameId: String, perGroup: Int = 2) = withContext(ioDispatcher) {
        val gameRow = q.selectGameById(gameId).executeAsOne()
        val entrants = q.selectEntrantsByGame(gameId).executeAsList().map { it.toDomain() }
        val existingMatches = q.selectMatchesByGame(gameId).executeAsList().map { it.toDomain() }
        val game = gameRow.toDomainShallow().copy(entrants = entrants, matches = existingMatches)
        val groups = EventStandings.groupLabels(game)
        val qualifiers = groups.flatMap { label ->
            EventStandings.groupStandings(game, label).take(perGroup).map { it.entrant }
        }
        val startIndex = (existingMatches.maxOfOrNull { it.matchIndex } ?: -1) + 1
        val knockoutMatches = BracketGenerator.generateKnockout(qualifiers, startIndex)
        insertMatches(gameId, knockoutMatches)
        q.updateGameStatus(EventGameStatus.IN_PROGRESS.name, gameId)
    }

    suspend fun saveMatchProgress(matchId: String, scoreA: Int, scoreB: Int) = withContext(ioDispatcher) {
        q.updateMatchScore(scoreA.toLong(), scoreB.toLong(), EventMatchStatus.LIVE.name, matchId)
        val match = q.selectMatchById(matchId).executeAsOneOrNull() ?: return@withContext
        q.updateGameStatus(EventGameStatus.IN_PROGRESS.name, match.eventGameId)
    }

    /** Declares a winner, advances them into the next round (if any), and marks the game complete once the final is decided. */
    suspend fun declareMatchWinner(matchId: String, scoreA: Int, scoreB: Int, winnerEntrantId: String) =
        withContext(ioDispatcher) {
            val match = q.selectMatchById(matchId).executeAsOneOrNull() ?: return@withContext
            q.updateMatchResult(scoreA.toLong(), scoreB.toLong(), EventMatchStatus.COMPLETE.name, winnerEntrantId, matchId)
            val nextId = match.nextMatchId
            if (nextId != null) {
                if (match.nextMatchSlot == 0L) q.updateMatchEntrantA(winnerEntrantId, nextId)
                else q.updateMatchEntrantB(winnerEntrantId, nextId)
                q.updateGameStatus(EventGameStatus.IN_PROGRESS.name, match.eventGameId)
            } else {
                // No next match -- this was the final (or the only round-robin match
                // isn't tracked this way). For elimination formats, a completed match
                // with no next match means the bracket is done.
                val remaining = q.selectMatchesByGame(match.eventGameId).executeAsList()
                    .count { it.status != EventMatchStatus.COMPLETE.name }
                q.updateGameStatus(
                    if (remaining == 0) EventGameStatus.COMPLETE.name else EventGameStatus.IN_PROGRESS.name,
                    match.eventGameId
                )
            }
        }

    /** Marks a round-robin game (which has no single "final" match) as complete once every match is played. */
    suspend fun markGameCompleteIfAllMatchesDone(gameId: String) = withContext(ioDispatcher) {
        val matches = q.selectMatchesByGame(gameId).executeAsList()
        if (matches.isNotEmpty() && matches.all { it.status == EventMatchStatus.COMPLETE.name }) {
            q.updateGameStatus(EventGameStatus.COMPLETE.name, gameId)
        }
    }

    private fun EventEntity.toDomain(): CommunityEvent = CommunityEvent(
        id = id,
        name = name,
        emoji = emoji,
        dateMillis = eventDate,
        endDateMillis = endDate,
        location = location,
        createdAtMillis = createdAt
    )

    private fun EventGameEntity.toDomainShallow(): EventGame = EventGame(
        id = id,
        eventId = eventId,
        sportName = sportName,
        emoji = emoji,
        format = runCatching { TournamentFormat.valueOf(format) }.getOrDefault(TournamentFormat.SINGLE_ELIMINATION),
        teamMode = runCatching { EventTeamMode.valueOf(teamMode) }.getOrDefault(EventTeamMode.SINGLES),
        playersPerTeam = playersPerTeam.toInt(),
        status = runCatching { EventGameStatus.valueOf(status) }.getOrDefault(EventGameStatus.SETUP),
        orderIndex = orderIndex.toInt()
    )

    private fun EventEntrantEntity.toDomain(): EventEntrant = EventEntrant(
        id = id,
        name = name,
        colorIndex = colorIndex.toInt(),
        seed = seed.toInt(),
        roster = if (roster.isBlank()) emptyList() else roster.split(ROSTER_SEPARATOR).filter { it.isNotBlank() },
        groupLabel = groupLabel
    )

    private fun EventMatchEntity.toDomain(): EventMatch = EventMatch(
        id = id,
        roundLabel = roundLabel,
        matchIndex = matchIndex.toInt(),
        entrantAId = entrantAId,
        entrantBId = entrantBId,
        scoreA = scoreA.toInt(),
        scoreB = scoreB.toInt(),
        status = runCatching { EventMatchStatus.valueOf(status) }.getOrDefault(EventMatchStatus.PENDING),
        winnerEntrantId = winnerEntrantId,
        nextMatchId = nextMatchId,
        nextMatchSlot = nextMatchSlot?.toInt()
    )

    private fun SavedPlayerEntity.toDomain(): SavedPlayer = SavedPlayer(
        id = id,
        name = name,
        colorIndex = colorIndex.toInt()
    )

    private fun GameSessionEntity.toDomainShallow(): GameSession = GameSession(
        id = id,
        gameType = GameType.valueOf(gameType),
        name = name,
        createdAtMillis = createdAt,
        isFinished = isFinished == 1L,
        rules = runCatching { json.decodeFromString<GameRules>(rulesJson) }.getOrDefault(GameRules())
    )

    private fun GameSessionEntity.toDomain(players: List<Player>, rounds: List<RoundScore>): GameSession =
        toDomainShallow().copy(players = players, rounds = rounds)

    private fun PlayerEntity.toDomain(): Player = Player(
        id = id,
        name = name,
        orderIndex = orderIndex.toInt(),
        isEliminated = isEliminated == 1L
    )

    private fun RoundScoreEntity.toDomain(): RoundScore = RoundScore(
        id = id,
        playerId = playerId,
        roundNumber = roundNumber.toInt(),
        rawScore = rawScore.toInt(),
        outcome = outcomeTag?.let { runCatching { RoundOutcome.valueOf(it) }.getOrNull() } ?: RoundOutcome.NORMAL
    )
}
