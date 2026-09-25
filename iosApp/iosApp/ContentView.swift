import SwiftUI
import Shared

/// Navigation destinations for the app's single `NavigationStack`.
/// Game types and round outcomes travel as plain string ids (see
/// `InteropHelpers.kt`), never as raw Kotlin enum instances.
enum Route: Hashable {
    case gamePicker
    case addPlayers(gameTypeId: String)
    case scoreEntry(sessionId: String)
    case summary(sessionId: String)
}

struct ContentView: View {
    @StateObject private var appVM: AppViewModel
    @StateObject private var authVM: AuthViewModel
    @State private var path: [Route] = []

    init() {
        // Top-level Kotlin functions in InteropHelpers.kt are exported under the
        // file-facade class `InteropHelpersKt` (Kotlin/Native's standard convention
        // for top-level declarations, same as JVM's `Kt`-suffixed facade classes).
        let repository = InteropHelpersKt.createGameRepository(driverFactory: DatabaseDriverFactory())
        let controller = AppController(repository: repository)
        _appVM = StateObject(wrappedValue: AppViewModel(controller: controller))
        _authVM = StateObject(wrappedValue: AuthViewModel(controller: AuthControllerKt.createAuthController()))
    }

    var body: some View {
        Group {
            if authVM.isSignedIn {
                NavigationStack(path: $path) {
                    HomeView(appVM: appVM, path: $path)
                        .navigationDestination(for: Route.self) { route in
                            switch route {
                            case .gamePicker:
                                GamePickerView(path: $path)
                            case .addPlayers(let gameTypeId):
                                AddPlayersView(gameTypeId: gameTypeId, appVM: appVM, path: $path)
                            case .scoreEntry(let sessionId):
                                ScoreEntryView(sessionId: sessionId, appVM: appVM, path: $path)
                            case .summary(let sessionId):
                                SummaryView(sessionId: sessionId, appVM: appVM, path: $path)
                            }
                        }
                }
            } else {
                LoginView(authVM: authVM)
            }
        }
        .tint(Color.skGreen)
    }
}
