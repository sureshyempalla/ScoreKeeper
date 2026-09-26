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
    case comingSoon(feature: String)
}

struct ContentView: View {
    @StateObject private var appVM: AppViewModel
    @StateObject private var authVM: AuthViewModel
    @State private var path: [Route] = []
    @State private var showLogin = false

    init() {
        // Top-level Kotlin functions in InteropHelpers.kt are exported under the
        // file-facade class `InteropHelpersKt` (Kotlin/Native's standard convention
        // for top-level declarations, same as JVM's `Kt`-suffixed facade classes).
        let repository = InteropHelpersKt.createGameRepository(driverFactory: DatabaseDriverFactory())
        let controller = AppController(repository: repository)
        _appVM = StateObject(wrappedValue: AppViewModel(controller: controller))
        _authVM = StateObject(wrappedValue: AuthViewModel(controller: AuthControllerKt.createAuthController(repository: repository)))
    }

    var body: some View {
        // Home is always reachable -- no hard login gate. The wireframe shows a
        // dismissible "sign in to sync" banner on Home instead; tapping it (or the
        // Profile tab) presents Login as a sheet rather than blocking the app.
        NavigationStack(path: $path) {
            HomeView(appVM: appVM, authVM: authVM, path: $path, showLogin: $showLogin)
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
                    case .comingSoon(let feature):
                        ComingSoonView(feature: feature)
                    }
                }
        }
        .sheet(isPresented: $showLogin) {
            LoginView(authVM: authVM)
        }
        .onChange(of: authVM.isSignedIn) { _, signedIn in
            if signedIn { showLogin = false }
        }
        .tint(Color.skGreen)
    }
}
