package com.scorekeeper.domain

/**
 * A signed-in user, trimmed to the fields the app actually shows. Deliberately
 * a plain data class (not the GitLive/Firebase SDK's own user type) so neither
 * UI layer depends on a third-party auth library's shape.
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val displayName: String?
)

/**
 * Every state the login flow can be in, as a string id rather than a Kotlin
 * enum - see the note on [com.scorekeeper.GameTypes] in InteropHelpers.kt for
 * why: this crosses the Swift boundary the same way. [AuthStatuses] below is
 * the matching lookup object.
 */
object AuthStatuses {
    /** No user signed in yet; showing the login screen. */
    const val SIGNED_OUT = "SIGNED_OUT"

    /** A sign-in/sign-up/phone-code call is in flight. */
    const val LOADING = "LOADING"

    /** [AuthUiState.verificationId] is set; waiting for the user to enter the SMS code. */
    const val PHONE_CODE_SENT = "PHONE_CODE_SENT"

    /** [AuthUiState.user] is set; the app can proceed past login. */
    const val SIGNED_IN = "SIGNED_IN"

    /**
     * The user tapped "Continue as guest": no Firebase user, but the app
     * proceeds past login the same as SIGNED_IN. [AuthUiState.user] stays
     * null in this state.
     */
    const val GUEST = "GUEST"

    /** [AuthUiState.errorMessage] is set; the last action failed. */
    const val ERROR = "ERROR"
}

/**
 * The one piece of state both the Android and iOS login screens observe.
 * [statusId] is one of [AuthStatuses]; the other fields are only meaningful
 * for the matching status ([user] for SIGNED_IN, [verificationId] for
 * PHONE_CODE_SENT, [errorMessage] for ERROR).
 */
data class AuthUiState(
    val statusId: String = AuthStatuses.SIGNED_OUT,
    val user: AuthUser? = null,
    val verificationId: String? = null,
    val errorMessage: String? = null
)
