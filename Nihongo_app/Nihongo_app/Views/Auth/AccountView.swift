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

            Text(L10n.accountTitle)
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
                    Text(L10n.ageLabel(age))
                        .font(.system(size: 16))
                        .foregroundStyle(Theme.secondaryInk)
                }
            }
            .padding(.bottom, 24)

            Text(L10n.accountSyncInfo)
                .font(.footnote)
                .foregroundStyle(Theme.secondaryInk)
                .padding(.bottom, 16)

            VStack(alignment: .leading, spacing: 8) {
                Text(L10n.languageSettingsTitle)
                    .font(Theme.heading(20))
                    .foregroundStyle(Theme.ink)

                Text(L10n.languageSettingsSubtitle)
                    .font(.footnote)
                    .foregroundStyle(Theme.secondaryInk)

                Picker("", selection: Bindable(LanguageManager.shared).current) {
                    ForEach(LanguageManager.AppLanguage.allCases) { lang in
                        Text("\(lang.flag) \(lang.displayName)").tag(lang)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.top, 8)
            }
            .padding(.bottom, 32)

            Button(L10n.signOutButton) {
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
