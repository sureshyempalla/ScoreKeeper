package com.scorekeeper.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
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
import com.scorekeeper.app.ui.screens.AddParticipantsScreen
import com.scorekeeper.app.ui.screens.AddSportScreen
import com.scorekeeper.app.ui.screens.BracketViewScreen
import com.scorekeeper.app.ui.screens.ComingSoonScreen
import com.scorekeeper.app.ui.screens.ConfigureGameScreen
import com.scorekeeper.app.ui.screens.CreateEventScreen
import com.scorekeeper.app.ui.screens.EventDashboardScreen
import com.scorekeeper.app.ui.screens.EventResultsScreen
import com.scorekeeper.app.ui.screens.EventsHomeScreen
import com.scorekeeper.app.ui.screens.GamePickerScreen
import com.scorekeeper.app.ui.screens.HomeScreen
import com.scorekeeper.app.ui.screens.LoginScreen
import com.scorekeeper.app.ui.screens.MatchScoreScreen
import com.scorekeeper.app.ui.screens.PlayerPickerScreen
import com.scorekeeper.app.ui.screens.RoundHistoryScreen
import com.scorekeeper.app.ui.screens.ScoreEntryScreen
import com.scorekeeper.app.ui.screens.SetScoreEntryScreen
import com.scorekeeper.app.ui.screens.SetupScreen
import com.scorekeeper.app.ui.screens.SummaryScreen
import com.scorekeeper.app.ui.screens.VolleyballGroupsScreen
import com.scorekeeper.app.ui.screens.VolleyballStandingsScreen
import com.scorekeeper.app.ui.screens.VolleyballTeamsScreen
import com.scorekeeper.app.ui.theme.Cream
import com.scorekeeper.app.ui.theme.ScoreKeeperTheme
import com.scorekeeper.domain.AuthStatuses
import com.scorekeeper.domain.CommunityEvent
import com.scorekeeper.domain.EventGame
import com.scorekeeper.domain.EventTeamMode
import com.scorekeeper.domain.GameSession
import com.scorekeeper.domain.GameType
import com.scorekeeper.domain.SportRules
import com.scorekeeper.domain.TournamentFormat
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
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

    // Events: hoisted between AddSport -> ConfigureGame, since the chosen
    // sport name/emoji aren't yet backed by a created EventGame row.
    var pendingSportName by remember { mutableStateOf("") }
    var pendingSportEmoji by remember { mutableStateOf("") }
    val events by controller.events.collectAsStateWithLifecycle()

    // Android 15 (targetSdk 35) draws content edge-to-edge under the status bar and behind
    // the gesture nav bar regardless of what MainActivity does, so every screen needs this
    // safe-area padding -- applied once here rather than per-screen. The Surface behind it
    // means the sliver under the system bars shows Cream instead of the window's default
    // background color.
    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxSize().systemBarsPadding()
    ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    sessions = sessions,
                    savedPlayers = savedPlayers,
                    authStatusId = authState.statusId,
                    onNewGame = { navController.navigate(Screen.GamePicker.route) },
                    onOpenEvents = { navController.navigate(Screen.EventsHome.route) },
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

            // --- Community Events ---------------------------------------------

            composable(Screen.EventsHome.route) {
                val gamesByEvent by watchAllEventGameCounts(controller, events)
                EventsHomeScreen(
                    events = events,
                    gameCounts = gamesByEvent.mapValues { it.value.size },
                    gameCompletionCounts = gamesByEvent.mapValues { (_, games) ->
                        games.count { it.status == com.scorekeeper.domain.EventGameStatus.COMPLETE } to games.size
                    },
                    onOpenEvent = { eventId -> navController.navigate(Screen.EventDashboard.build(eventId)) },
                    onCreateEvent = { navController.navigate(Screen.CreateEvent.route) },
                    onHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onStats = { navController.navigate(Screen.ComingSoon.build("Stats")) },
                    onProfile = { navController.navigate(Screen.ComingSoon.build("Profile")) }
                )
            }

            composable(Screen.CreateEvent.route) {
                CreateEventScreen(
                    onBack = { navController.popBackStack() },
                    onCreate = { name, emoji, dateMillis, endDateMillis, location ->
                        controller.createEvent(name, emoji, dateMillis, endDateMillis, location) { eventId ->
                            navController.navigate(Screen.EventDashboard.build(eventId)) {
                                popUpTo(Screen.EventsHome.route)
                            }
                        }
                    }
                )
            }

            composable(
                Screen.EventDashboard.route,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                val event = events.firstOrNull { it.id == eventId }
                val games by watchEventGamesState(controller, eventId)
                event?.let {
                    EventDashboardScreen(
                        event = it,
                        games = games,
                        onBack = { navController.popBackStack() },
                        onAddGame = { navController.navigate(Screen.AddSport.build(eventId)) },
                        onOpenGame = { game ->
                        if (game.format == TournamentFormat.GROUP_STAGE_THEN_KNOCKOUT) {
                            navController.navigate(Screen.VolleyballFlow.build(game.id))
                        } else {
                            navController.navigate(Screen.BracketView.build(game.id))
                        }
                    },
                        onEditGame = { game -> navController.navigate(Screen.EditGame.build(game.id)) },
                        onDeleteEvent = {
                            controller.deleteEvent(eventId) { navController.popBackStack() }
                        },
                        onViewResults = { navController.navigate(Screen.EventResults.build(eventId)) }
                    )
                }
            }

            composable(
                Screen.AddSport.route,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                AddSportScreen(
                    onBack = { navController.popBackStack() },
                    onSportChosen = { name, emoji ->
                        pendingSportName = name
                        pendingSportEmoji = emoji
                        navController.navigate(Screen.ConfigureGame.build(eventId))
                    }
                )
            }

            composable(
                Screen.ConfigureGame.route,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                ConfigureGameScreen(
                    sportName = pendingSportName,
                    emoji = pendingSportEmoji,
                    onBack = { navController.popBackStack() },
                    onContinue = { _, _, format, teamMode, playersPerTeam, pointRules ->
                        controller.addEventGame(eventId, pendingSportName, pendingSportEmoji, format, teamMode, playersPerTeam, pointRules) { gameId ->
                            if (format == TournamentFormat.GROUP_STAGE_THEN_KNOCKOUT) {
                                navController.navigate(Screen.VolleyballFlow.build(gameId)) {
                                    popUpTo(Screen.EventDashboard.build(eventId))
                                }
                            } else {
                                navController.navigate(Screen.AddParticipants.build(gameId))
                            }
                        }
                    }
                )
            }

            composable(
                Screen.EditGame.route,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
                val game by watchEventGameDetailState(controller, gameId)
                game?.let { g ->
                    ConfigureGameScreen(
                        sportName = g.sportName,
                        emoji = g.emoji,
                        isEditing = true,
                        locked = g.entrants.isNotEmpty(),
                        initialFormat = g.format,
                        initialTeamMode = g.teamMode,
                        initialPlayersPerTeam = g.playersPerTeam,
                        initialPointRules = g.pointRules,
                        onBack = { navController.popBackStack() },
                        onDelete = {
                            controller.deleteEventGame(gameId) { navController.popBackStack() }
                        },
                        onContinue = { newSportName, newEmoji, format, teamMode, playersPerTeam, pointRules ->
                            controller.updateEventGameConfig(gameId, newSportName, newEmoji, format, teamMode, playersPerTeam, pointRules) {
                                navController.popBackStack()
                            }
                        }
                    )
                }
            }

            composable(
                Screen.AddParticipants.route,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
                val game by watchEventGameDetailState(controller, gameId)
                AddParticipantsScreen(
                    sportName = game?.sportName ?: pendingSportName,
                    emoji = game?.emoji ?: pendingSportEmoji,
                    teamMode = game?.teamMode ?: EventTeamMode.SINGLES,
                    savedPlayers = savedPlayers,
                    onBack = { navController.popBackStack() },
                    onAddSavedPlayer = { name -> controller.addSavedPlayer(name) },
                    onGenerateDraw = { names ->
                        val eventId = game?.eventId
                        if (eventId != null) {
                            controller.generateDraw(gameId, eventId, names)
                            navController.navigate(Screen.BracketView.build(gameId)) {
                                popUpTo(Screen.EventDashboard.build(eventId))
                            }
                        }
                    }
                )
            }

            composable(
                Screen.VolleyballFlow.route,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
                val game by watchEventGameDetailState(controller, gameId)
                game?.let { g ->
                    val minTeamSize = SportRules.minTeamSize(g.sportName) ?: 6
                    // Groups aren't assigned until the organizer taps "Continue to Groups" on the
                    // Teams screen (autoAssignGroups labels every entrant at once), so "no entrant
                    // has a groupLabel yet" is what actually distinguishes "still adding teams"
                    // from "teams done, ready to arrange groups" -- entrants.isEmpty() alone would
                    // jump straight to the Groups screen the moment the very first team is added.
                    val anyGroupAssigned = g.entrants.any { it.groupLabel != null }
                    val groupStageMatches = g.matches.filter { it.roundLabel.startsWith("Group ") }
                    val knockoutMatches = g.matches.filterNot { it.roundLabel.startsWith("Group ") }

                    // State-driven flow: which stage to show falls out of the game's actual
                    // entrants/matches, not a separately tracked "step" -- so re-entering this
                    // route (from EventDashboard's onOpenGame, or after the app is killed and
                    // restarted) always lands on the right stage automatically.
                    when {
                        g.entrants.isEmpty() || !anyGroupAssigned -> VolleyballTeamsScreen(
                            sportName = g.sportName,
                            emoji = g.emoji,
                            minTeamSize = minTeamSize,
                            teams = g.entrants,
                            onBack = { navController.popBackStack() },
                            onAddTeam = { name, roster -> controller.addTeam(gameId, name, roster) },
                            onUpdateTeam = { entrantId, name, roster -> controller.updateTeam(entrantId, name, roster) },
                            onRemoveTeam = { entrantId -> controller.removeTeam(entrantId) },
                            onContinue = {
                                // Same bounds as the Groups screen's own stepper (see there for why):
                                // at least 1 group, at most one group per 2 teams.
                                val maxGroups = maxOf(1, g.entrants.size / 2)
                                val suggestedGroups = if (g.entrants.size <= 4) 1 else (g.entrants.size + 3) / 4
                                controller.autoAssignGroups(gameId, suggestedGroups.coerceIn(1, maxGroups))
                            }
                        )

                        g.matches.isEmpty() -> {
                            val currentGroupCount = g.entrants.mapNotNull { it.groupLabel }.distinct().size
                                .coerceAtLeast(1)
                            VolleyballGroupsScreen(
                                sportName = g.sportName,
                                emoji = g.emoji,
                                teams = g.entrants,
                                groupCount = currentGroupCount,
                                onGroupCountChange = { newCount -> controller.autoAssignGroups(gameId, newCount) },
                                onMoveTeam = { entrantId, groupLabel -> controller.moveEntrantToGroup(entrantId, groupLabel) },
                                onBack = { navController.popBackStack() },
                                onEditTeams = { controller.resetGroups(gameId) },
                                onConfirm = { controller.startGroupStageDraw(gameId) }
                            )
                        }

                        knockoutMatches.isEmpty() -> VolleyballStandingsScreen(
                            sportName = g.sportName,
                            emoji = g.emoji,
                            groupLabels = controller.groupLabelsFor(g),
                            entrants = g.entrants,
                            matches = groupStageMatches,
                            standingsForGroup = { label -> controller.groupStandingsFor(g, label) },
                            onBack = { navController.popBackStack() },
                            onOpenMatch = { match -> navController.navigate(Screen.MatchScore.build(gameId, match.id)) },
                            onAdvance = { controller.advanceToPlayoffs(gameId) }
                        )

                        else -> BracketViewScreen(
                            game = g,
                            standings = controller.standingsFor(g),
                            champion = controller.championFor(g),
                            onBack = { navController.popBackStack() },
                            onReshuffle = { /* no-op: re-shuffling a group-stage bracket isn't supported */ },
                            onOpenMatch = { match -> navController.navigate(Screen.MatchScore.build(gameId, match.id)) }
                        )
                    }
                }
            }

            composable(
                Screen.BracketView.route,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
                val game by watchEventGameDetailState(controller, gameId)
                game?.let {
                    BracketViewScreen(
                        game = it,
                        standings = controller.standingsFor(it),
                        champion = controller.championFor(it),
                        onBack = { navController.popBackStack() },
                        onReshuffle = {
                            controller.generateDraw(gameId, it.eventId, it.entrants.sortedBy { e -> e.seed }.map { e -> e.name })
                        },
                        onOpenMatch = { match -> navController.navigate(Screen.MatchScore.build(gameId, match.id)) }
                    )
                }
            }

            composable(
                Screen.MatchScore.route,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("matchId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
                val matchId = backStackEntry.arguments?.getString("matchId") ?: return@composable
                val game by watchEventGameDetailState(controller, gameId)
                game?.let { g ->
                    val match = g.matches.firstOrNull { it.id == matchId }
                    match?.let { m ->
                        val pointRules = g.pointRules
                        if (pointRules != null) {
                            SetScoreEntryScreen(
                                match = m,
                                rules = pointRules,
                                sportLabel = g.sportName,
                                entrantAName = g.entrants.firstOrNull { e -> e.id == m.entrantAId }?.name ?: "TBD",
                                entrantBName = g.entrants.firstOrNull { e -> e.id == m.entrantBId }?.name ?: "TBD",
                                onBack = { navController.popBackStack() },
                                onSubmitSet = { scoreA, scoreB ->
                                    controller.recordSetScore(matchId, scoreA, scoreB) {
                                        controller.markGameCompleteIfAllMatchesDone(gameId)
                                    }
                                },
                                onUndoLastSet = { controller.undoLastSet(matchId) }
                            )
                        } else {
                            MatchScoreScreen(
                                match = m,
                                sportLabel = g.sportName,
                                entrantAName = g.entrants.firstOrNull { e -> e.id == m.entrantAId }?.name ?: "TBD",
                                entrantBName = g.entrants.firstOrNull { e -> e.id == m.entrantBId }?.name ?: "TBD",
                                onBack = { navController.popBackStack() },
                                onSaveProgress = { scoreA, scoreB ->
                                    controller.saveMatchProgress(matchId, scoreA, scoreB)
                                    navController.popBackStack()
                                },
                                onDeclareWinner = { winnerId, scoreA, scoreB ->
                                    controller.declareMatchWinner(matchId, scoreA, scoreB, winnerId)
                                    controller.markGameCompleteIfAllMatchesDone(gameId)
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }

            composable(
                Screen.EventResults.route,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                val event = events.firstOrNull { it.id == eventId }
                val shallowGames by watchEventGamesState(controller, eventId)
                val detailedGames by watchEventGamesDetailedState(controller, shallowGames)
                event?.let {
                    EventResultsScreen(
                        event = it,
                        games = detailedGames,
                        championFor = { game -> controller.championFor(game) },
                        onBack = { navController.popBackStack() },
                        onShare = { /* placeholder, matches SummaryScreen's Share Results action */ }
                    )
                }
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

/** Bridges AppController's callback-based watchEventGames (shallow, no entrants/matches) into Compose state. */
@Composable
private fun watchEventGamesState(controller: AppController, eventId: String) =
    produceState<List<EventGame>>(initialValue = emptyList(), eventId) {
        val cancellable = controller.watchEventGames(eventId) { value = it }
        awaitDispose { cancellable.cancel() }
    }

/** Bridges AppController's callback-based watchEventGameDetail (full, with entrants/matches) into Compose state. */
@Composable
private fun watchEventGameDetailState(controller: AppController, gameId: String) =
    produceState<EventGame?>(initialValue = null, gameId) {
        val cancellable = controller.watchEventGameDetail(gameId) { value = it }
        awaitDispose { cancellable.cancel() }
    }

/**
 * For EventsHome: watches every event's games (shallow) at once, keyed by event id, so the
 * event list can show a live game count / completion badge per event.
 */
@Composable
private fun watchAllEventGameCounts(controller: AppController, events: List<CommunityEvent>): State<Map<String, List<EventGame>>> {
    val eventIds = events.map { it.id }
    return produceState<Map<String, List<EventGame>>>(initialValue = emptyMap(), eventIds) {
        val map = mutableStateMapOf<String, List<EventGame>>()
        val cancellables = eventIds.map { eventId ->
            controller.watchEventGames(eventId) { games ->
                map[eventId] = games
                value = map.toMap()
            }
        }
        awaitDispose { cancellables.forEach { it.cancel() } }
    }
}

/**
 * For EventResults: the dashboard's shallow game list has no entrants/matches (needed to
 * compute each game's champion), so this re-watches every game's full detail in parallel.
 */
@Composable
private fun watchEventGamesDetailedState(controller: AppController, shallowGames: List<EventGame>): State<List<EventGame>> {
    val gameIds = shallowGames.map { it.id }
    return produceState<List<EventGame>>(initialValue = shallowGames, gameIds) {
        val map = mutableStateMapOf<String, EventGame>()
        val cancellables = gameIds.map { gameId ->
            controller.watchEventGameDetail(gameId) { detail ->
                if (detail != null) {
                    map[gameId] = detail
                    value = shallowGames.map { map[it.id] ?: it }
                }
            }
        }
        awaitDispose { cancellables.forEach { it.cancel() } }
    }
}
