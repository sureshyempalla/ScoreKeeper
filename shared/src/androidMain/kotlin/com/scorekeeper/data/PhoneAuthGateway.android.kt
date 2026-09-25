package com.scorekeeper.data

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.scorekeeper.domain.AuthUser
import java.util.concurrent.TimeUnit

actual class PhoneAuthGateway actual constructor(private val context: Any?) {

    private val activity: Activity
        get() = context as? Activity
            ?: error("PhoneAuthGateway on Android needs the current Activity; got ${context?.let { it::class }}")

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    actual fun sendVerificationCode(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onAutoVerified: (AuthUser) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Android auto-retrieved or instantly-verified the SMS code itself.
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        if (user == null) {
                            onError("Sign-in succeeded but returned no user")
                        } else {
                            onAutoVerified(user.toAuthUser())
                        }
                    }
                    .addOnFailureListener { e -> onError(e.message ?: "Sign-in failed") }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onError(e.message ?: "Phone verification failed")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    actual fun confirmCode(
        verificationId: String,
        smsCode: String,
        onVerified: (AuthUser) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onError("Sign-in succeeded but returned no user")
                } else {
                    onVerified(user.toAuthUser())
                }
            }
            .addOnFailureListener { e -> onError(e.message ?: "Invalid or expired code") }
    }

    private fun com.google.firebase.auth.FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        phoneNumber = phoneNumber,
        displayName = displayName
    )
}
