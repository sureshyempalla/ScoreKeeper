package com.scorekeeper.data

import com.scorekeeper.domain.AuthUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Email/password auth, backed by the GitLive Firebase Kotlin SDK - the one
 * piece of Firebase Auth that behaves identically on Android and iOS from a
 * single commonMain implementation. Phone auth does not go through here; see
 * [PhoneAuthGateway] for why.
 */
class EmailAuthService {
    private val auth = Firebase.auth

    /** Null when signed out. Emits again on every sign-in/sign-out. */
    val currentUser: Flow<AuthUser?> = auth.authStateChanged.map { it?.toAuthUser() }

    suspend fun signIn(email: String, password: String): AuthUser {
        val result = auth.signInWithEmailAndPassword(email, password)
        return result.user?.toAuthUser()
            ?: throw IllegalStateException("Sign-in succeeded but returned no user")
    }

    suspend fun signUp(email: String, password: String): AuthUser {
        val result = auth.createUserWithEmailAndPassword(email, password)
        return result.user?.toAuthUser()
            ?: throw IllegalStateException("Sign-up succeeded but returned no user")
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email)
    }

    suspend fun signOut() {
        auth.signOut()
    }

    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        uid = uid,
        email = email,
        phoneNumber = phoneNumber,
        displayName = displayName
    )
}
