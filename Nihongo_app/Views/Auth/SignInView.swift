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

    @State private var mode: Mode = .signUp
    @State private var email = ""
    @State private var password = ""
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

                Text("日本語")
                    .font(Theme.display(48))
                    .foregroundStyle(Theme.accent)
                    .padding(.bottom, 10)

                Text(mode == .signUp ? "Hesap oluştur" : "Giriş yap")
                    .font(Theme.display(30))
                    .foregroundStyle(Theme.ink)
                    .padding(.bottom, 8)

                Text("İlerlemen hesabına kaydedilir; başka bir cihazdan giriş yaptığında kaldığın yerden devam edersin.")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
                    .padding(.bottom, 24)

                emailPasswordForm

                divider
                    .padding(.vertical, 20)

                SignInWithAppleButton(.signIn) { request in
                    AuthService.shared.prepareAppleRequest(request)
                } onCompletion: { result in
                    Task { await handleAppleCompletion(result) }
                }
                .signInWithAppleButtonStyle(colorScheme == .dark ? .white : .black)
                .frame(height: 52)

                Button("Şimdilik misafir olarak devam et") {
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
            TextField("Email", text: $email)
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .padding(14)
                .inkBordered()

            SecureField("Şifre (en az 6 karakter)", text: $password)
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

            Button(mode == .signUp ? "Kayıt Ol" : "Giriş Yap") {
                submitEmailPassword()
            }
            .buttonStyle(PrimaryButtonStyle())
            .disabled(isWorking)
            .opacity(isWorking ? 0.6 : 1)

            HStack {
                Button(mode == .signUp ? "Zaten hesabın var mı? Giriş yap" : "Hesabın yok mu? Kayıt ol") {
                    mode = mode == .signUp ? .signIn : .signUp
                    errorMessage = nil
                    infoMessage = nil
                }
                .font(.footnote.weight(.bold))
                .foregroundStyle(Theme.ink)

                Spacer()

                if mode == .signIn {
                    Button("Şifremi unuttum") {
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
            Text("veya")
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
            errorMessage = "Email ve şifre boş bırakılamaz."
            return
        }

        isWorking = true
        Task {
            do {
                if mode == .signUp {
                    try await AuthService.shared.signUp(email: trimmedEmail, password: password)
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
            errorMessage = "Şifre sıfırlama için üstteki alana email adresini yaz."
            return
        }

        Task {
            do {
                try await AuthService.shared.sendPasswordReset(email: trimmedEmail)
                infoMessage = "Şifre sıfırlama bağlantısı \(trimmedEmail) adresine gönderildi."
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
