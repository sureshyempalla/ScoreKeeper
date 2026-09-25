import Foundation
import Shared

/// Bridges the shared KMP `AppController` into SwiftUI.
///
/// `AppController` exposes a callback + `Cancellable` API for Swift (rather than
/// `StateFlow`, which has no first-class Swift support without extra tooling such as
/// SKIE - deliberately skipped here to keep the shared module dependency-free). This
/// wraps that callback API in an `ObservableObject` so SwiftUI views can just read
/// `@Published` properties.
@MainActor
final class AppViewModel: ObservableObject {

    let controller: AppController

    @Published var sessions: [GameSession] = []

    private var sessionsWatch: Cancellable?

    init(controller: AppController) {
        self.controller = controller
        sessionsWatch = controller.watchSessions { [weak self] sessions in
            DispatchQueue.main.async {
                self?.sessions = sessions
            }
        }
    }

    deinit {
        sessionsWatch?.cancel()
    }

    func newGame(gameTypeId: String, sessionName: String, playerNames: [String], rules: GameRules, onCreated: @escaping (String) -> Void) {
        controller.startNewGame(gameTypeId: gameTypeId, sessionName: sessionName, playerNames: playerNames, rules: rules) { sessionId in
            DispatchQueue.main.async { onCreated(sessionId) }
        }
    }

    func recordRound(sessionId: String, playerId: String, roundNumber: Int32, rawScore: Int32, outcomeId: String) {
        controller.recordRound(sessionId: sessionId, playerId: playerId, roundNumber: roundNumber, rawScore: rawScore, outcomeId: outcomeId)
    }

    func undoLastRound(sessionId: String) {
        controller.undoLastRound(sessionId: sessionId)
    }

    func finishSession(sessionId: String) {
        controller.finishSession(sessionId: sessionId)
    }

    func deleteSession(sessionId: String) {
        controller.deleteSession(sessionId: sessionId)
    }
}

/// Bridges `AppController.watchSession(sessionId:onChange:)` into a `@Published`
/// property scoped to one session's lifetime (the Score Entry / Summary screens).
@MainActor
final class SessionViewModel: ObservableObject {

    private let controller: AppController
    @Published var session: GameSession?

    private var watch: Cancellable?

    init(controller: AppController, sessionId: String) {
        self.controller = controller
        watch = controller.watchSession(sessionId: sessionId) { [weak self] session in
            DispatchQueue.main.async {
                self?.session = session
            }
        }
    }

    deinit {
        watch?.cancel()
    }
}
