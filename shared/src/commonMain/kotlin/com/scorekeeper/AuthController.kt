package com.scorekeeper

import com.scorekeeper.data.EmailAuthService
import com.scorekeeper.data.GameRepository
import com.scorekeeper.data.PhoneAuthGateway
import com.scorekeeper.data.SessionGuard
import com.scorekeeper.domain.AuthStatuses
import com.scorekeeper.domain.AuthUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Platform-agnostic login/logout controller - the [AppController] equivalent
 * for auth. Same two observation shapes as [AppController]: Android/Compose
 * collects [uiState] as a StateFlow directly; iOS/SwiftUI uses the callback-
 * based [watchUiState] instead (see [AppController]'s doc comment for why).
 *
 * No `suspend fun` is exposed here either, for the same reason as
 * [AppController]: every action is a plain `fun` that launches on [scope]
 * internally, and the result (success or failure) is reported back through
 * [uiState] rather than a return value or a thrown exception across the
 * Swift boundary.
 */
class AuthController(repository: GameRepository) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val emailAuth = EmailAuthService()
    private val sessionGuard = SessionGuard(repository)

    /** Cancelled and replaced every time sign-in state changes; watches for another device taking over this session. */
    private var takeoverWatchJob: Job? = null

    private val _uiState = MutableStateFlow(AuthUiState())

    /** Android/Compose: collectAsStateWithLifecycle(). */
    val uiState: StateFlow<AuthUiState> = _uiState
        .stateIn(scope, SharingStarted.Eagerly, AuthUiState())

    init {
        emailAuth.currentUser
            .onEach { user ->
                takeoverWatchJob?.cancel()
                takeoverWatchJob = null
                _uiState.update { current ->
                    if (user != null) {
                        current.copy(statusId = AuthStatuses.SIGNED_IN, user = user, errorMessage = null)
                    } else if (current.statusId == AuthStatuses.SIGNED_IN) {
                        // Only reset on an actual sign-out; don't clobber an
                        // in-progress LOADING/PHONE_CODE_SENT/ERROR state.
                        AuthUiState()
                    } else current
                }
                if (user != null) claimSessionAndWatch(user.uid)
            }
            .launchIn(scope)
    }

    /**
     * Claims this device as the account's one active session (see
     * [SessionGuard]), then watches for a later sign-in on another device
     * taking that claim over -- if that happens, this device is signed out
     * locally with an explanatory message.
     */
    private fun claimSessionAndWatch(uid: String) {
        scope.launch {
            val sessionId = sessionGuard.claim(uid)
            // Assigned here (not the wrapping scope.launch above) -- this is the
            // actual long-lived listener job takeoverWatchJob?.cancel() needs to
            // stop; the wrapping launch finishes as soon as this line runs.
            takeoverWatchJob = sessionGuard.watchForTakeover(uid, sessionId).onEach {
                runCatching { emailAuth.signOut() }
                _uiState.update {
                    AuthUiState(
                        statusId = AuthStatuses.ERROR,
                        errorMessage = "Signed out because this account was used on another device."
                    )
                }
            }.launchIn(scope)
        }
    }

    /** iOS/SwiftUI: wraps this in an ObservableObject (see AppViewModel.swift). */
    fun watchUiState(onChange: (AuthUiState) -> Unit): Cancellable {
        val job = scope.launch {
            uiState.collect { onChange(it) }
        }
        return JobCancellable(job)
    }

    fun signInWithEmail(email: String, password: String) {
        _uiState.update { it.copy(statusId = AuthStatuses.LOADING, errorMessage = null) }
        scope.launch {
            runCatching { emailAuth.signIn(email, password) }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(statusId = AuthStatuses.ERROR, errorMessage = e.message ?: "Sign-in failed")
                    }
                }
            // onSuccess is handled by the authStateChanged collector above.
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        _uiState.update { it.copy(statusId = AuthStatuses.LOADING, errorMessage = null) }
        scope.launch {
            runCatching { emailAuth.signUp(email, password) }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(statusId = AuthStatuses.ERROR, errorMessage = e.message ?: "Sign-up failed")
                    }
                }
        }
    }

    /**
     * [platformContext] must be the current `Activity` on Android (SwiftUI
     * callers pass `null`; see [PhoneAuthGateway]).
     */
    fun startPhoneVerification(phoneNumber: String, platformContext: Any?) {
        _uiState.update { it.copy(statusId = AuthStatuses.LOADING, errorMessage = null) }
        PhoneAuthGateway(platformContext).sendVerificationCode(
            phoneNumber = phoneNumber,
            onCodeSent = { verificationId ->
                _uiState.update {
                    it.copy(statusId = AuthStatuses.PHONE_CODE_SENT, verificationId = verificationId)
                }
            },
            onAutoVerified = { user ->
                _uiState.update {
                    it.copy(statusId = AuthStatuses.SIGNED_IN, user = user, errorMessage = null)
                }
            },
            onError = { message ->
                _uiState.update { it.copy(statusId = AuthStatuses.ERROR, errorMessage = message) }
            }
        )
    }

    fun confirmPhoneCode(smsCode: String, platformContext: Any?) {
        val verificationId = _uiState.value.verificationId ?: run {
            _uiState.update {
                it.copy(statusId = AuthStatuses.ERROR, errorMessage = "No verification in progress")
            }
            return
        }
        _uiState.update { it.copy(statusId = AuthStatuses.LOADING) }
        PhoneAuthGateway(platformContext).confirmCode(
            verificationId = verificationId,
            smsCode = smsCode,
            onVerified = { user ->
                _uiState.update {
                    it.copy(statusId = AuthStatuses.SIGNED_IN, user = user, errorMessage = null)
                }
            },
            onError = { message ->
                _uiState.update { it.copy(statusId = AuthStatuses.ERROR, errorMessage = message) }
            }
        )
    }

    /**
     * Skips auth entirely: no Firebase user is created, but the app proceeds
     * past login exactly as if signed in. Cheaper than a real anonymous
     * Firebase sign-in and doesn't need network access to work offline.
     */
    fun continueAsGuest() {
        _uiState.update { AuthUiState(statusId = AuthStatuses.GUEST) }
    }

    fun signOut() {
        scope.launch {
            runCatching { emailAuth.signOut() }
            _uiState.update { AuthUiState() }
        }
    }

    /** Clears an ERROR state back to SIGNED_OUT so the login form is usable again. */
    fun clearError() {
        _uiState.update {
            if (it.statusId == AuthStatuses.ERROR) AuthUiState() else it
        }
    }
}

/** No-default-args factory - see the note on similar factories in InteropHelpers.kt. */
fun createAuthController(repository: GameRepository): AuthController = AuthController(repository)
