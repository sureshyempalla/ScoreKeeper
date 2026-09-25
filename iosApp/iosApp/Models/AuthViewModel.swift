import Foundation
import Shared

/// Bridges the shared KMP `AuthController` into SwiftUI, the same pattern
/// `AppViewModel` uses for `AppController` (see that file's doc comment).
///
/// `state` starts `nil` and gets its first value from the initial callback
/// `AuthController.watchUiState` fires on subscribe - LoginView shows a
/// spinner for that brief window rather than this constructing a default
/// `AuthUiState` itself (`AuthUiState()` isn't callable from Swift: Kotlin
/// default parameter values don't cross the interop boundary, see
/// InteropHelpers.kt).
@MainActor
final class AuthViewModel: ObservableObject {

    let controller: AuthController

    @Published var state: AuthUiState?

    private var watch: Cancellable?

    init(controller: AuthController) {
        self.controller = controller
        watch = controller.watchUiState { [weak self] state in
            DispatchQueue.main.async {
                self?.state = state
            }
        }
    }

    deinit {
        watch?.cancel()
    }

    var isSignedIn: Bool {
        state?.statusId == AuthStatuses.shared.SIGNED_IN || state?.statusId == AuthStatuses.shared.GUEST
    }

    func signInWithEmail(email: String, password: String) {
        controller.signInWithEmail(email: email, password: password)
    }

    func signUpWithEmail(email: String, password: String) {
        controller.signUpWithEmail(email: email, password: password)
    }

    /// `platformContext` is always `nil` on iOS - phone auth needs the native
    /// Firebase iOS SDK linked in, which isn't wired up yet (see
    /// `PhoneAuthGateway.ios.kt`); this always resolves to an ERROR state
    /// telling the person to use email instead.
    func sendPhoneCode(phoneNumber: String) {
        controller.startPhoneVerification(phoneNumber: phoneNumber, platformContext: nil)
    }

    func confirmPhoneCode(code: String) {
        controller.confirmPhoneCode(smsCode: code, platformContext: nil)
    }

    func clearError() {
        controller.clearError()
    }

    func continueAsGuest() {
        controller.continueAsGuest()
    }
}
