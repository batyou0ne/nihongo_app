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
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()

                header

                Spacer()

                VStack(spacing: 12) {
                    SignInWithAppleButton(.signIn) { request in
                        AuthService.shared.prepareAppleRequest(request)
                    } onCompletion: { result in
                        Task { await handleCompletion(result) }
                    }
                    .signInWithAppleButtonStyle(colorScheme == .dark ? .white : .black)
                    .frame(height: 50)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                    if let errorMessage {
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                            .multilineTextAlignment(.center)
                    }

                    Button("Şimdilik misafir olarak devam et") {
                        dismiss()
                    }
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.top, 4)
                }
            }
            .padding(24)
            .background(Color(uiColor: .systemGroupedBackground))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
    }

    private var header: some View {
        VStack(spacing: 12) {
            Text("日本語")
                .font(.system(size: 56, design: .serif))

            Text("Hesabını bağla")
                .font(.title2.bold())

            Text("İlerlemen hesabına kaydedilir; başka bir cihazdan giriş yaptığında kaldığın yerden devam edersin.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
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
