package com.scorekeeper.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.scorekeeper.domain.AuthStatuses
import com.scorekeeper.domain.AuthUiState

private enum class LoginTab { EMAIL, PHONE }

/**
 * Login screen per the wireframes: email-or-phone sign-in, tabbed. All the
 * actual auth work happens in [com.scorekeeper.AuthController]; this screen
 * only reads [uiState] and forwards user intent through the callbacks -
 * MainActivity/App.kt wires those to the controller (see ScoreKeeperApp).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onSignInEmail: (email: String, password: String) -> Unit,
    onSignUpEmail: (email: String, password: String) -> Unit,
    onSendPhoneCode: (phoneNumber: String) -> Unit,
    onConfirmPhoneCode: (code: String) -> Unit,
    onClearError: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(LoginTab.EMAIL) }
    val isLoading = uiState.statusId == AuthStatuses.LOADING
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onClearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Text("🃏", style = MaterialTheme.typography.displayMedium)
            Text(
                "Score Keeper",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Sign in to sync your games everywhere",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            TabRow(selectedTabIndex = tab.ordinal, modifier = Modifier.fillMaxWidth()) {
                Tab(
                    selected = tab == LoginTab.EMAIL,
                    onClick = { tab = LoginTab.EMAIL },
                    text = { Text("Email") },
                    icon = { Icon(Icons.Filled.Email, contentDescription = null) }
                )
                Tab(
                    selected = tab == LoginTab.PHONE,
                    onClick = { tab = LoginTab.PHONE },
                    text = { Text("Phone") },
                    icon = { Icon(Icons.Filled.Phone, contentDescription = null) }
                )
            }

            Spacer(Modifier.height(24.dp))

            when (tab) {
                LoginTab.EMAIL -> EmailLoginForm(
                    isLoading = isLoading,
                    onSignIn = onSignInEmail,
                    onSignUp = onSignUpEmail
                )
                LoginTab.PHONE -> PhoneLoginForm(
                    uiState = uiState,
                    isLoading = isLoading,
                    onSendCode = onSendPhoneCode,
                    onConfirmCode = onConfirmPhoneCode
                )
            }

            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onContinueAsGuest, enabled = !isLoading) {
                Text("Continue as guest")
            }
        }
    }
}

@Composable
private fun EmailLoginForm(
    isLoading: Boolean,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        val canSubmit = email.isNotBlank() && password.length >= 6 && !isLoading
        Button(
            onClick = { onSignIn(email.trim(), password) },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Sign In")
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { onSignUp(email.trim(), password) }, enabled = canSubmit) {
            Text("New here? Create an account")
        }
    }
}

@Composable
private fun PhoneLoginForm(
    uiState: AuthUiState,
    isLoading: Boolean,
    onSendCode: (String) -> Unit,
    onConfirmCode: (String) -> Unit
) {
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    val codeSent = uiState.statusId == AuthStatuses.PHONE_CODE_SENT

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone number") },
            placeholder = { Text("+1 415 555 1234") },
            singleLine = true,
            enabled = !isLoading && !codeSent,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        if (codeSent) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("6-digit code") },
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))

        if (!codeSent) {
            Button(
                onClick = { onSendCode(phoneNumber.trim()) },
                enabled = phoneNumber.trim().length >= 8 && !isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Send code")
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConfirmCode(code.trim()) },
                    enabled = code.trim().length >= 4 && !isLoading,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Verify")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onSendCode(phoneNumber.trim()) }, enabled = !isLoading) {
                Text("Resend code")
            }
        }
    }
}
