package com.scorekeeper.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.scorekeeper.app.ui.screens.ComingSoonScreen
import com.scorekeeper.app.ui.screens.GamePickerScreen
import com.scorekeeper.app.ui.screens.HomeScreen
import com.scorekeeper.app.ui.screens.LoginScreen
import com.scorekeeper.app.ui.screens.PlayerPickerScreen
import com.scorekeeper.app.ui.screens.RoundHistoryScreen
import com.scorekeeper.app.ui.screens.ScoreEntryScreen
import com.scorekeeper.app.ui.screens.SetupScreen
import com.scorekeeper.app.ui.screens.SummaryScreen
import com.scorekeeper.app.ui.theme.ScoreKeeperTheme
import com.scorekeeper.domain.AuthStatuses
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.GameType
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun ScoreKeeperApp(controller: AppController, authController: AuthController) {
    ScoreKeeperTheme {
        ScoreKeeperHome(controller, authController)
    }
}

@Composable
private fun ScoreKeeperHome(controller: AppController, authController: AuthController) {
    val navController = rememberNavController()
    val sessions by controller.sessions.collectAsStateWithLifecycle()
    val savedPlayers by controller.savedPlayers.collectAsStateWithLifecycle()
    val authState by authController.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current
    // Hoisted between PlayerPicker -> Setup, since the picked player names
    // aren't a good fit for a nav-route argument.
    var pendingPlayerNames by remember { mutableStateOf<List<String>>(emptyList()) }

    NavHost(navController = navController, startDestination = Screen.Home.route) {
            composable(Screen.Home.route) {
                HomeScreen(
                    sessions = sessions,
                    savedPlayers = savedPlayers,
                    authStatusId = authState.statusId,
                    onNewGame = { navController.navigate(Screen.GamePicker.route) },
                    onOpenEvents = { navController.navigate(Screen.ComingSoon.build("Events")) },
                    onOpenSession = { sessionId -> navController.navigate(Screen.ScoreEntry.build(sessionId)) },
                    onOpenStats = { navController.navigate(Screen.ComingSoon.build("Stats")) },
                    onOpenProfile = { navController.navigate(Screen.ComingSoon.build("Profile")) },
                    onSignInBannerClick = { navController.navigate(Screen.Login.route) },
                    onAddSavedPlayer = { name -> controller.addSavedPlayer(name) },
                    onDeleteSession = { controller.deleteSession(it) }
                )
            }

            composable(Screen.Login.route) {
                // Once sign-in succeeds, drop the Login screen and return to Home.
                LaunchedEffect(authState.statusId) {
                    if (authState.statusId == AuthStatuses.SIGNED_IN) {
                        navController.popBackStack()
                    }
                }
                LoginScreen(
                    uiState = authState,
                    onSignInEmail = { email, password -> authController.signInWithEmail(email, password) },
                    onSignUpEmail = { email, password -> authController.signUpWithEmail(email, password) },
                    onSendPhoneCode = { phone -> authController.startPhoneVerification(phone, activity) },
                    onConfirmPhoneCode = { code -> authController.confirmPhoneCode(code, activity) },
                    onClearError = { authController.clearError() },
                    onContinueAsGuest = {
                        authController.continueAsGuest()
                        navController.popBackStack()
                    }
                )
            }

            composable(
                Screen.ComingSoon.route,
                arguments = listOf(navArgument("feature") { type = NavType.StringType })
            ) { backStackEntry ->
                val feature = backStackEntry.arguments?.getString("feature") ?: "This"
                ComingSoonScreen(feature = feature, onBack = { navController.popBackStack() })
            }

            composable(Screen.GamePicker.route) {
                GamePickerScreen(
                    onGameSelected = { game -> navController.navigate(Screen.PlayerPicker.build(game.name)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.PlayerPicker.route,
                arguments = listOf(navArgument("gameType") { type = NavType.StringType })
            ) { backStackEntry ->
                val gameType = GameType.valueOf(backStackEntry.arguments?.getString("gameType") ?: GameType.CUSTOM.name)
                PlayerPickerScreen(
                    gameType = gameType,
                    savedPlayers = savedPlayers,
                    onBack = { navController.popBackStack() },
                    onAddSavedPlayer = { name -> controller.addSavedPlayer(name) },
                    onContinue = { playerNames ->
                        pendingPlayerNames = playerNames
                        navController.navigate(Screen.Setup.build(gameType.name))
                    }
                )
            }

            composable(
                Screen.Setup.route,
                arguments = listOf(navArgument("gameType") { type = NavType.StringType })
            ) { backStackEntry ->
                val gameType = GameType.valueOf(backStackEntry.arguments?.getString("gameType") ?: GameType.CUSTOM.name)
                SetupScreen(
                    gameType = gameType,
                    onBack = { navController.popBackStack() },
                    onStart = { sessionName, rules ->
                        controller.startNewGame(gameType, sessionName, pendingPlayerNames, rules) { sessionId ->
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
                        },
                        onViewHistory = { navController.navigate(Screen.RoundHistory.build(sessionId)) }
                    )
                }
            }

            composable(
                Screen.RoundHistory.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                val session by watchSessionState(controller, sessionId)
                session?.let {
                    RoundHistoryScreen(
                        session = it,
                        onBack = { navController.popBackStack() }
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
                        },
                        onRematch = {
                            navController.navigate(Screen.PlayerPicker.build(it.gameType.name)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onViewHistory = { navController.navigate(Screen.RoundHistory.build(sessionId)) }
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
