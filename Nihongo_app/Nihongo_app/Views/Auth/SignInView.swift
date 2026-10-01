import SwiftUI
import AuthenticationServices

/// Hesap oluşturma / giriş ekranı.
///
/// Misafir kullanımı engellemez; kullanıcı isterse email/şifre veya Apple ile
/// hesap bağlayıp ilerlemesini buluta taşır. Anonim hesap yeni kimliğe "link"
/// edildiği için o ana kadarki ilerleme korunur (bkz. AuthService).
struct SignInView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme

    private enum Mode {
        case signUp, signIn
    }

    /// Sign in with Apple, ücretsiz (Personal Team) hesapla cihazda çalışmadığı
    /// için geçici olarak kapalı. Ücretli Apple Developer üyeliği alınınca bu
    /// bayrak açılmalı ve entitlement geri eklenmeli (bkz. Nihongo_app.entitlements).
    private let isSignInWithAppleEnabled = false

    @State private var mode: Mode = .signIn
    @State private var email = ""
    @State private var password = ""
    @State private var firstName = ""
    @State private var lastName = ""
    @State private var ageText = ""
    @State private var errorMessage: String?
    @State private var infoMessage: String?
    @State private var isWorking = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(Theme.ink)
                }
                .padding(.top, 24)
                .padding(.bottom, 28)

                Text("開門")
                    .font(Theme.display(48))
                    .foregroundStyle(Theme.accent)
                    .padding(.bottom, 10)

                Text(mode == .signUp ? L10n.signUpTitle : L10n.signInTitle)
                    .font(Theme.display(30))
                    .foregroundStyle(Theme.ink)
                    .padding(.bottom, 8)

                Text(L10n.signInDescription)
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
                    .padding(.bottom, 24)

                emailPasswordForm

                divider
                    .padding(.vertical, 20)

                if isSignInWithAppleEnabled {
                    SignInWithAppleButton(.signIn) { request in
                        AuthService.shared.prepareAppleRequest(request)
                    } onCompletion: { result in
                        Task { await handleAppleCompletion(result) }
                    }
                    .signInWithAppleButtonStyle(colorScheme == .dark ? .white : .black)
                    .frame(height: 52)
                }

                Button {
                    signInWithGoogle()
                } label: {
                    HStack(spacing: 8) {
                        Text("G")
                            .font(.system(size: 20, weight: .black))
                            .foregroundStyle(Theme.accent)
                        Text(L10n.googleSignIn)
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(Theme.ink)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .inkBordered()
                }
                .padding(.top, 12)

                Button(L10n.continueAsGuest) {
                    dismiss()
                }
                .font(.system(size: 16, weight: .heavy))
                .foregroundStyle(Theme.accent)
                .padding(.top, 20)
                .padding(.bottom, 32)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 24)
        }
        .scrollIndicators(.hidden)
        .background(Theme.paper)
    }

    // MARK: - Email/şifre formu

    private var emailPasswordForm: some View {
        VStack(alignment: .leading, spacing: 12) {
            if mode == .signUp {
                HStack(spacing: 12) {
                    TextField(L10n.firstNamePlaceholder, text: $firstName)
                        .textContentType(.givenName)
                        .padding(14)
                        .inkBordered()

                    TextField(L10n.lastNamePlaceholder, text: $lastName)
                        .textContentType(.familyName)
                        .padding(14)
                        .inkBordered()
                }

                TextField(L10n.agePlaceholder, text: $ageText)
                    .keyboardType(.numberPad)
                    .padding(14)
                    .inkBordered()
            }

            TextField(L10n.emailPlaceholder, text: $email)
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .padding(14)
                .inkBordered()

            SecureField(L10n.passwordPlaceholder, text: $password)
                .textContentType(mode == .signUp ? .newPassword : .password)
                .padding(14)
                .inkBordered()

            if let errorMessage {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundStyle(Theme.accent)
            }
            if let infoMessage {
                Text(infoMessage)
                    .font(.footnote)
                    .foregroundStyle(Theme.ink)
            }

            Button(mode == .signUp ? L10n.signUpButton : L10n.signInButton) {
                submitEmailPassword()
            }
            .buttonStyle(PrimaryButtonStyle())
            .disabled(isWorking)
            .opacity(isWorking ? 0.6 : 1)

            HStack {
                Button(mode == .signUp ? L10n.alreadyHaveAccount : L10n.noAccount) {
                    mode = mode == .signUp ? .signIn : .signUp
                    errorMessage = nil
                    infoMessage = nil
                }
                .font(.footnote.weight(.bold))
                .foregroundStyle(Theme.ink)

                Spacer()

                if mode == .signIn {
                    Button(L10n.forgotPassword) {
                        sendPasswordReset()
                    }
                    .font(.footnote.weight(.bold))
                    .foregroundStyle(Theme.secondaryInk)
                }
            }
            .padding(.top, 4)
        }
    }

    private var divider: some View {
        HStack(spacing: 12) {
            Rectangle().fill(Theme.ink).frame(height: 2)
            Text(L10n.orDivider)
                .font(.footnote.weight(.bold))
                .foregroundStyle(Theme.secondaryInk)
            Rectangle().fill(Theme.ink).frame(height: 2)
        }
    }

    // MARK: - Aksiyonlar

    private func submitEmailPassword() {
        errorMessage = nil
        infoMessage = nil
        let trimmedEmail = email.trimmingCharacters(in: .whitespaces)
        guard !trimmedEmail.isEmpty, !password.isEmpty else {
            errorMessage = L10n.emptyEmailPassword
            return
        }

        let trimmedFirstName = firstName.trimmingCharacters(in: .whitespaces)
        let trimmedLastName = lastName.trimmingCharacters(in: .whitespaces)
        if mode == .signUp {
            guard !trimmedFirstName.isEmpty, !trimmedLastName.isEmpty else {
                errorMessage = L10n.emptyName
                return
            }
        }

        isWorking = true
        Task {
            do {
                if mode == .signUp {
                    try await AuthService.shared.signUp(
                        email: trimmedEmail,
                        password: password,
                        displayName: "\(trimmedFirstName) \(trimmedLastName)"
                    )
                    // Kişisel bilgiler Firestore'a yazılır; hesap zaten oluştuğu
                    // için profil kaydı başarısız olsa bile akış kesilmez.
                    let profile = UserProfile(
                        firstName: trimmedFirstName,
                        lastName: trimmedLastName,
                        age: Int(ageText.trimmingCharacters(in: .whitespaces))
                    )
                    try? UserProfileService.shared.save(profile)
                } else {
                    try await AuthService.shared.signIn(email: trimmedEmail, password: password)
                }
                isWorking = false
                dismiss()
            } catch {
                isWorking = false
                errorMessage = AuthService.friendlyMessage(for: error)
            }
        }
    }

    private func sendPasswordReset() {
        errorMessage = nil
        infoMessage = nil
        let trimmedEmail = email.trimmingCharacters(in: .whitespaces)
        guard !trimmedEmail.isEmpty else {
            errorMessage = L10n.passwordResetPrompt
            return
        }

        Task {
            do {
                try await AuthService.shared.sendPasswordReset(email: trimmedEmail)
                infoMessage = L10n.passwordResetSent(trimmedEmail)
            } catch {
                errorMessage = AuthService.friendlyMessage(for: error)
            }
        }
    }

    private func signInWithGoogle() {
        errorMessage = nil
        infoMessage = nil
        Task {
            do {
                let didSignIn = try await AuthService.shared.signInWithGoogle()
                if didSignIn {
                    dismiss()
                }
            } catch {
                errorMessage = AuthService.friendlyMessage(for: error)
            }
        }
    }

    private func handleAppleCompletion(_ result: Result<ASAuthorization, Error>) async {
        do {
            let didSignIn = try await AuthService.shared.handleAppleCompletion(result)
            if didSignIn {
                dismiss()
            }
        } catch {
            errorMessage = AuthService.friendlyMessage(for: error)
        }
    }
}

#Preview {
    SignInView()
}
