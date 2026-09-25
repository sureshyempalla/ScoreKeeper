package com.scorekeeper.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scorekeeper.AppController
import com.scorekeeper.AuthController
import com.scorekeeper.app.ui.nav.Screen
import com.scorekeeper.app.ui.screens.AddPlayersScreen
import com.scorekeeper.app.ui.screens.GamePickerScreen
import com.scorekeeper.app.ui.screens.HomeScreen
import com.scorekeeper.app.ui.screens.LoginScreen
import com.scorekeeper.app.ui.screens.ScoreEntryScreen
import com.scorekeeper.app.ui.screens.SummaryScreen
import com.scorekeeper.app.ui.theme.ScoreKeeperTheme
import com.scorekeeper.domain.AuthStatuses
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.GameType

@Composable
fun ScoreKeeperApp(controller: AppController, authController: AuthController) {
    ScoreKeeperTheme {
        val authState by authController.uiState.collectAsStateWithLifecycle()
        val activity = LocalContext.current

        val hasAccess = authState.statusId == AuthStatuses.SIGNED_IN || authState.statusId == AuthStatuses.GUEST
        if (!hasAccess) {
            LoginScreen(
                uiState = authState,
                onSignInEmail = { email, password -> authController.signInWithEmail(email, password) },
                onSignUpEmail = { email, password -> authController.signUpWithEmail(email, password) },
                onSendPhoneCode = { phone -> authController.startPhoneVerification(phone, activity) },
                onConfirmPhoneCode = { code -> authController.confirmPhoneCode(code, activity) },
                onClearError = { authController.clearError() },
                onContinueAsGuest = { authController.continueAsGuest() }
            )
        } else {
            ScoreKeeperHome(controller)
        }
    }
}

@Composable
private fun ScoreKeeperHome(controller: AppController) {
    val navController = rememberNavController()
    val sessions by controller.sessions.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
            composable(Screen.Home.route) {
                HomeScreen(
                    sessions = sessions,
                    onNewGame = { navController.navigate(Screen.GamePicker.route) },
                    onOpenSession = { sessionId -> navController.navigate(Screen.ScoreEntry.build(sessionId)) },
                    onDeleteSession = { controller.deleteSession(it) }
                )
            }

            composable(Screen.GamePicker.route) {
                GamePickerScreen(
                    onGameSelected = { game -> navController.navigate(Screen.AddPlayers.build(game.name)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.AddPlayers.route,
                arguments = listOf(navArgument("gameType") { type = NavType.StringType })
            ) { backStackEntry ->
                val gameType = GameType.valueOf(backStackEntry.arguments?.getString("gameType") ?: GameType.CUSTOM.name)
                AddPlayersScreen(
                    gameType = gameType,
                    onBack = { navController.popBackStack() },
                    onStart = { sessionName, playerNames, rules ->
                        controller.startNewGame(gameType, sessionName, playerNames, rules) { sessionId ->
                            navController.navigate(Screen.ScoreEntry.build(sessionId)) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }
                )
            }

            composable(
                Screen.ScoreEntry.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                val session by watchSessionState(controller, sessionId)
                session?.let {
                    ScoreEntryScreen(
                        session = it,
                        onBack = { navController.popBackStack() },
                        onSubmitRound = { entries ->
                            val nextRound = (it.rounds.maxOfOrNull { r -> r.roundNumber } ?: 0) + 1
                            entries.forEach { (playerId, scoreAndOutcome) ->
                                val (score, outcome) = scoreAndOutcome
                                controller.recordRound(sessionId, playerId, nextRound, score, outcome)
                            }
                        },
                        onUndo = { controller.undoLastRound(sessionId) },
                        onFinish = {
                            controller.finishSession(sessionId)
                            navController.navigate(Screen.Summary.build(sessionId)) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )
                }
            }

            composable(
                Screen.Summary.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                val session by watchSessionState(controller, sessionId)
                session?.let {
                    SummaryScreen(
                        session = it,
                        onBackToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }

/** Bridges AppController's callback-based watchSession into Compose state. */
@Composable
private fun watchSessionState(controller: AppController, sessionId: String) =
    produceState<GameSession?>(initialValue = null, sessionId) {
        val cancellable = controller.watchSession(sessionId) { value = it }
        awaitDispose { cancellable.cancel() }
    }
