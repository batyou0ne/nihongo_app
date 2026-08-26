import SwiftUI
import AuthenticationServices

/// Hesap oluşturma / giriş ekranı.
///
/// Misafir kullanımı engellemez; kullanıcı isterse Apple ile hesap bağlayıp
/// ilerlemesini buluta taşır (çoklu cihaz senkronizasyonu için).
struct SignInView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme

    @State private var errorMessage: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button {
                dismiss()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(Theme.ink)
            }
            .padding(.top, 24)

            Spacer()

            Text("日本語")
                .font(Theme.display(64))
                .foregroundStyle(Theme.accent)
                .padding(.bottom, 16)

            Text("Hesabını bağla")
                .font(Theme.display(32))
                .foregroundStyle(Theme.ink)
                .padding(.bottom, 12)

            Text("İlerlemen hesabına kaydedilir; başka bir cihazdan giriş yaptığında kaldığın yerden devam edersin.")
                .font(.system(size: 17))
                .foregroundStyle(Theme.secondaryInk)
                .padding(.bottom, 28)

            SignInWithAppleButton(.signIn) { request in
                AuthService.shared.prepareAppleRequest(request)
            } onCompletion: { result in
                Task { await handleCompletion(result) }
            }
            .signInWithAppleButtonStyle(colorScheme == .dark ? .white : .black)
            .frame(height: 56)

            if let errorMessage {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundStyle(Theme.accent)
                    .padding(.top, 12)
            }

            Button("Şimdilik misafir olarak devam et") {
                dismiss()
            }
            .font(.system(size: 17, weight: .heavy))
            .foregroundStyle(Theme.accent)
            .padding(.top, 20)

            Spacer()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 24)
        .background(Theme.paper)
    }

    private func handleCompletion(_ result: Result<ASAuthorization, Error>) async {
        do {
            let didSignIn = try await AuthService.shared.handleAppleCompletion(result)
            if didSignIn {
                dismiss()
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

#Preview {
    SignInView()
}
