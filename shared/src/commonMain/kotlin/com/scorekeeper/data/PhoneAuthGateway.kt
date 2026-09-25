package com.scorekeeper.data

import com.scorekeeper.domain.AuthUser

/**
 * Phone-number sign-in, deliberately kept OUT of [EmailAuthService] / the
 * GitLive SDK. Firebase's phone flow needs a platform-specific app-verifier
 * (Play Integrity/SafetyNet + a visible Activity on Android; a silent APNs
 * push on iOS) that a single commonMain implementation can't provide, so
 * this is an expect/actual with one real native-SDK implementation per
 * platform instead.
 *
 * [context] is `Any?` rather than a typed platform parameter so this class
 * can appear in a common `expect` declaration at all: the Android actual
 * casts it to the `Activity` the native Firebase SDK requires; the iOS
 * actual ignores it.
 */
expect class PhoneAuthGateway(context: Any?) {
    /**
     * Sends an SMS code to [phoneNumber] (E.164 format, e.g. "+14155551234").
     * Exactly one of [onCodeSent] / [onAutoVerified] / [onError] fires.
     * [onAutoVerified] covers Android's instant auto-retrieval; there is no
     * iOS equivalent, so the iOS actual never calls it.
     */
    fun sendVerificationCode(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onAutoVerified: (AuthUser) -> Unit,
        onError: (message: String) -> Unit
    )

    /** Confirms the code the user typed against the id from [onCodeSent] above. */
    fun confirmCode(
        verificationId: String,
        smsCode: String,
        onVerified: (AuthUser) -> Unit,
        onError: (message: String) -> Unit
    )
}
