import SwiftUI
import FirebaseAuth

/// Hesap ekranı: kullanıcının kişisel bilgilerini ve çıkış seçeneğini gösterir.
/// Giriş yapılmışken profil butonuna basıldığında SignInView yerine bu açılır.
/// (Hesap silme, account-management aşamasında eklenecek.)
struct AccountView: View {
    @Environment(\.dismiss) private var dismiss

    private var profile: UserProfile? {
        UserProfileService.shared.profile
    }

    /// Firestore profili varsa ad-soyad, yoksa (Google/Apple girişi gibi) Auth'taki isim/email.
    private var displayName: String {
        profile?.fullName ?? AuthService.shared.accountDescription
    }

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
            .padding(.bottom, 28)

            Image(systemName: "person.crop.circle.fill.badge.checkmark")
                .font(.system(size: 44))
                .foregroundStyle(Theme.accent)
                .padding(.bottom, 12)

            Text("Hesabım")
                .font(Theme.display(32))
                .foregroundStyle(Theme.ink)
                .padding(.bottom, 24)

            VStack(alignment: .leading, spacing: 10) {
                Text(displayName)
                    .font(Theme.heading(22))
                    .foregroundStyle(Theme.ink)

                if let email = AuthService.shared.currentUser?.email {
                    Text(email)
                        .font(.system(size: 16))
                        .foregroundStyle(Theme.secondaryInk)
                }

                if let age = profile?.age {
                    Text("Yaş: \(age)")
                        .font(.system(size: 16))
                        .foregroundStyle(Theme.secondaryInk)
                }
            }
            .padding(.bottom, 24)

            Text("İlerlemen bu hesaba bağlı. Başka bir cihazda aynı hesapla giriş yaptığında kaldığın yerden devam edersin.")
                .font(.footnote)
                .foregroundStyle(Theme.secondaryInk)
                .padding(.bottom, 16)

            Button("Çıkış Yap") {
                Task {
                    await AuthService.shared.signOut()
                    UserProfileService.shared.clear()
                    dismiss()
                }
            }
            .buttonStyle(PrimaryButtonStyle())

            Spacer()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 24)
        .background(Theme.paper)
        .task {
            await UserProfileService.shared.loadIfNeeded()
        }
    }
}

#Preview {
    AccountView()
}
