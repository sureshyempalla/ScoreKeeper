package com.scorekeeper.data

import com.scorekeeper.domain.AuthUser

/**
 * Not implemented yet. iOS phone auth needs the Firebase iOS SDK linked into
 * the Kotlin/Native framework (a cinterop/CocoaPods step that needs an actual
 * Xcode/macOS toolchain to set up and verify - unavailable in the sandbox
 * this was built in, see README "Known gaps"). Email/password sign-in
 * ([EmailAuthService], via GitLive) works on iOS today; this only blocks the
 * phone tab of the login screen until that native dependency is wired in.
 */
actual class PhoneAuthGateway actual constructor(private val context: Any?) {

    actual fun sendVerificationCode(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onAutoVerified: (AuthUser) -> Unit,
        onError: (message: String) -> Unit
    ) {
        onError("Phone sign-in isn't wired up on iOS yet - use email instead.")
    }

    actual fun confirmCode(
        verificationId: String,
        smsCode: String,
        onVerified: (AuthUser) -> Unit,
        onError: (message: String) -> Unit
    ) {
        onError("Phone sign-in isn't wired up on iOS yet - use email instead.")
    }
}
