import SwiftUI
import Shared

private enum LoginTab: String, CaseIterable {
    case email = "Email"
    case phone = "Phone"
}

/// Login screen per the wireframes: email-or-phone sign-in, tabbed. Mirrors
/// the Android `LoginScreen.kt` composable - all the actual auth work
/// happens in `AuthController` via `AuthViewModel`; this view only reads
/// state and forwards user intent.
struct LoginView: View {
    @ObservedObject var authVM: AuthViewModel
    @State private var tab: LoginTab = .email
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                Spacer().frame(height: 32)
                Text("🃏").font(.system(size: 56))
                Text("Score Keeper")
                    .font(.title.bold())
                Text("Sign in to sync your games everywhere")
                    .font(.subheadline)
                    .foregroundStyle(Color.skMuted)
                    .padding(.bottom, 24)

                Picker("", selection: $tab) {
                    ForEach(LoginTab.allCases, id: \.self) { Text($0.rawValue) }
                }
                .pickerStyle(.segmented)
                .padding(.bottom, 16)

                switch tab {
                case .email:
                    EmailLoginForm(authVM: authVM)
                case .phone:
                    PhoneLoginForm(authVM: authVM)
                }

                Button("Continue as guest") {
                    authVM.continueAsGuest()
                }
                .disabled(authVM.state?.statusId == AuthStatuses.shared.LOADING)
                .font(.subheadline)
                .padding(.top, 20)
            }
            .padding(.horizontal, 24)
        }
        .background(Color.skCream)
        .onChange(of: authVM.state?.errorMessage) { _, message in
            errorMessage = message
        }
        .alert("Something went wrong", isPresented: .constant(errorMessage != nil), actions: {
            Button("OK") {
                errorMessage = nil
                authVM.clearError()
            }
        }, message: {
            Text(errorMessage ?? "")
        })
    }
}

private struct EmailLoginForm: View {
    @ObservedObject var authVM: AuthViewModel
    @State private var email = ""
    @State private var password = ""

    private var isLoading: Bool { authVM.state?.statusId == AuthStatuses.shared.LOADING }
    private var canSubmit: Bool { !email.isEmpty && password.count >= 6 && !isLoading }

    var body: some View {
        VStack(spacing: 12) {
            TextField("Email", text: $email)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .disabled(isLoading)

            SecureField("Password", text: $password)
                .textFieldStyle(.roundedBorder)
                .disabled(isLoading)

            Button {
                authVM.signInWithEmail(email: email.trimmingCharacters(in: .whitespaces), password: password)
            } label: {
                HStack {
                    if isLoading { ProgressView().tint(.white) } else { Text("Sign In").bold() }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 44)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.skGreen)
            .disabled(!canSubmit)

            Button("New here? Create an account") {
                authVM.signUpWithEmail(email: email.trimmingCharacters(in: .whitespaces), password: password)
            }
            .disabled(!canSubmit)
            .font(.subheadline)
        }
    }
}

private struct PhoneLoginForm: View {
    @ObservedObject var authVM: AuthViewModel
    @State private var phoneNumber = ""
    @State private var code = ""

    private var isLoading: Bool { authVM.state?.statusId == AuthStatuses.shared.LOADING }
    private var codeSent: Bool { authVM.state?.statusId == AuthStatuses.shared.PHONE_CODE_SENT }

    var body: some View {
        VStack(spacing: 12) {
            TextField("Phone number (+1 415 555 1234)", text: $phoneNumber)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.phonePad)
                .disabled(isLoading || codeSent)

            if codeSent {
                TextField("6-digit code", text: $code)
                    .textFieldStyle(.roundedBorder)
                    .keyboardType(.numberPad)
                    .disabled(isLoading)
            }

            if !codeSent {
                Button {
                    authVM.sendPhoneCode(phoneNumber: phoneNumber.trimmingCharacters(in: .whitespaces))
                } label: {
                    HStack {
                        if isLoading { ProgressView().tint(.white) } else { Text("Send code").bold() }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.skGreen)
                .disabled(phoneNumber.trimmingCharacters(in: .whitespaces).count < 8 || isLoading)
            } else {
                Button {
                    authVM.confirmPhoneCode(code: code.trimmingCharacters(in: .whitespaces))
                } label: {
                    HStack {
                        if isLoading { ProgressView().tint(.white) } else { Text("Verify").bold() }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.skGreen)
                .disabled(code.trimmingCharacters(in: .whitespaces).count < 4 || isLoading)

                Button("Resend code") {
                    authVM.sendPhoneCode(phoneNumber: phoneNumber.trimmingCharacters(in: .whitespaces))
                }
                .disabled(isLoading)
                .font(.subheadline)
            }
        }
    }
}
