import SwiftUI

/// Hesap ekranı: bağlı hesabın bilgilerini ve çıkış seçeneğini gösterir.
/// Giriş yapılmışken profil butonuna basıldığında SignInView yerine bu açılır.
/// (Hesap silme, account-management aşamasında eklenecek.)
struct AccountView: View {
    @Environment(\.dismiss) private var dismiss

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

            VStack(alignment: .leading, spacing: 6) {
                Text("HESAP")
                    .font(.caption.weight(.heavy))
                    .foregroundStyle(Theme.secondaryInk)
                Text(AuthService.shared.accountDescription)
                    .font(Theme.heading(17))
                    .foregroundStyle(Theme.ink)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .inkBordered()
            .padding(.bottom, 12)

            VStack(alignment: .leading, spacing: 6) {
                Text("GİRİŞ YÖNTEMLERİ")
                    .font(.caption.weight(.heavy))
                    .foregroundStyle(Theme.secondaryInk)
                Text(AuthService.shared.providerNames.joined(separator: " · "))
                    .font(Theme.heading(17))
                    .foregroundStyle(Theme.ink)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .inkBordered()
            .padding(.bottom, 24)

            Text("İlerlemen bu hesaba bağlı. Başka bir cihazda aynı hesapla giriş yaptığında kaldığın yerden devam edersin.")
                .font(.footnote)
                .foregroundStyle(Theme.secondaryInk)
                .padding(.bottom, 16)

            Button("Çıkış Yap") {
                Task {
                    await AuthService.shared.signOut()
                    dismiss()
                }
            }
            .buttonStyle(PrimaryButtonStyle())

            Spacer()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 24)
        .background(Theme.paper)
    }
}

#Preview {
    AccountView()
}
