package com.scorekeeper.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.scorekeeper.db.GameSessionEntity
import com.scorekeeper.db.PlayerEntity
import com.scorekeeper.db.RoundScoreEntity
import com.scorekeeper.db.ScoreKeeperDatabase
import com.scorekeeper.domain.GameRules
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.GameType
import com.scorekeeper.domain.Player
import com.scorekeeper.domain.RoundOutcome
import com.scorekeeper.domain.RoundScore
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
