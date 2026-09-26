package com.scorekeeper.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.random.Random

private const val DEVICE_SESSION_KEY = "activeFirebaseSessionId"

@Serializable
private data class SessionDoc(val activeSessionId: String, val claimedAtMillis: Long)

/**
 * Enforces "one device signed in at a time" for a Firebase account (the
 * project's chosen simplification for Phase 1 sync, so two devices never
 * score the same match at once -- see the session's design discussion).
 *
 * On sign-in, this device claims a fresh session id in Firestore
 * (`users/{uid}`), overwriting whatever a previous device had claimed there.
 * [watchForTakeover] then reports when a *different* device's later sign-in
 * has overwritten this device's own claim, so [com.scorekeeper.AuthController]
 * can react by forcing a local sign-out.
 */
class SessionGuard(private val repository: GameRepository) {
    private fun userDoc(uid: String) = Firebase.firestore.collection("users").document(uid)

    /** Call right after a successful sign-in (or on app start for an already-signed-in user). Returns the claimed session id. */
    suspend fun claim(uid: String): String {
        val sessionId = "${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(0, Int.MAX_VALUE)}"
        userDoc(uid).set(SessionDoc(activeSessionId = sessionId, claimedAtMillis = Clock.System.now().toEpochMilliseconds()))
        repository.setDeviceState(DEVICE_SESSION_KEY, sessionId)
        return sessionId
    }

    /** The session id this device last claimed, if any (survives app restart). */
    suspend fun localSessionId(): String? = repository.getDeviceState(DEVICE_SESSION_KEY)

    /**
     * Emits once every time the server's active session id no longer matches
     * [localSessionId] -- i.e. this same account signed in on another device
     * and took over. Never emits for this device's own claim.
     */
    fun watchForTakeover(uid: String, localSessionId: String): Flow<Unit> =
        userDoc(uid).snapshots
            .mapNotNull { snap -> snap.takeIf { it.exists }?.data<SessionDoc>()?.activeSessionId }
            .filter { remoteSessionId -> remoteSessionId != localSessionId }
            .map { }
}
