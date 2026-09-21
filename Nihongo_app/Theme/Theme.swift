import SwiftUI

/// Uygulamanın tasarım sistemi: beyaz zemin, belirgin tipografi ve tek vurgu
/// rengi olarak vermilyon kırmızısı. Köşeler yuvarlatılmış, kartlar
/// siyah kenarlıklı — daha yumuşak, modern bir stil.
enum Theme {
    /// Vermilyon: markanın tek vurgu rengi (açık/koyu modda sabit).
    static let accent = Color(red: 0.92, green: 0.23, blue: 0.05)

    /// Ana metin/kenarlık rengi (açık modda siyah, koyu modda beyaz).
    static let ink = Color(uiColor: .label)

    /// Zemin (açık modda beyaz).
    static let paper = Color(uiColor: .systemBackground)

    /// İkincil metinler.
    static let secondaryInk = Color(uiColor: .secondaryLabel)

    /// Büyük başlıklar için kalın (bold) ve yuvarlak kesim.
    static func display(_ size: CGFloat) -> Font {
        .system(size: size, weight: .bold, design: .rounded)
    }

    /// Ara başlıklar için yarı kalın (semibold) ve yuvarlak kesim.
    static func heading(_ size: CGFloat) -> Font {
        .system(size: size, weight: .semibold, design: .rounded)
    }
}

/// Siyah zeminli, beyaz yazılı, yuvarlak köşeli birincil buton.
struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 17, weight: .bold, design: .rounded))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(Theme.ink)
            .foregroundStyle(Theme.paper)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .opacity(configuration.isPressed ? 0.75 : 1)
    }
}

/// Yuvarlak köşeli, siyah kenarlıklı kart/kutu görünümü.
struct InkBorder: ViewModifier {
    var lineWidth: CGFloat = 2

    func body(content: Content) -> some View {
        content
            .background(RoundedRectangle(cornerRadius: 16).fill(Theme.paper))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Theme.ink, lineWidth: lineWidth))
    }
}

extension View {
    func inkBordered(lineWidth: CGFloat = 2) -> some View {
        modifier(InkBorder(lineWidth: lineWidth))
    }
}
