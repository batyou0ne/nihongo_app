import SwiftUI

/// Pro'ya özel içerikte kilit ikonunun yerine gösterilen küçük rozet.
struct ProBadge: View {
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "crown.fill")
            Text("PRO")
        }
        .font(.system(size: 11, weight: .heavy, design: .rounded))
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Capsule().fill(Theme.accent))
        .foregroundStyle(.white)
    }
}

/// Günlük AI mesaj hakkı bitince sohbet ekranında gösterilen davet kartı.
struct AIQuotaBanner: View {
    let remaining: Int
    let onUpgrade: () -> Void
    var onWatchAd: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 8) {
            HStack(spacing: 10) {
                Image(systemName: remaining > 0 ? "bubble.left.fill" : "lock.fill")
                    .foregroundStyle(Theme.accent)
                Text(remaining > 0 ? L10n.aiQuotaRemaining(remaining) : L10n.aiQuotaExhausted)
                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                    .foregroundStyle(Theme.ink)
                    .lineLimit(2)
                Spacer(minLength: 8)
                Button(action: onUpgrade) {
                    ProBadge()
                }
            }

            if remaining == 0, let onWatchAd {
                Button(action: onWatchAd) {
                    Label(L10n.watchAdForMessages(AdConfig.rewardedAIMessages), systemImage: "play.rectangle.fill")
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Theme.ink, lineWidth: 1.5))
                        .foregroundStyle(Theme.ink)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Theme.paper)
    }
}
