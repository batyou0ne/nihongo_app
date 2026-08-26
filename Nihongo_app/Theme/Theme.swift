import SwiftUI

/// Uygulamanın tasarım sistemi: beyaz zemin, siyah/ağır tipografi ve tek vurgu
/// rengi olarak vermilyon kırmızısı. Köşeler keskin (yuvarlama yok), kartlar
/// kalın siyah kenarlıklı — brutalist/editoryal stil.
enum Theme {
    /// Vermilyon: markanın tek vurgu rengi (açık/koyu modda sabit).
    static let accent = Color(red: 0.92, green: 0.23, blue: 0.05)

    /// Ana metin/kenarlık rengi (açık modda siyah, koyu modda beyaz).
    static let ink = Color(uiColor: .label)

    /// Zemin (açık modda beyaz).
    static let paper = Color(uiColor: .systemBackground)

    /// İkincil metinler.
    static let secondaryInk = Color(uiColor: .secondaryLabel)

    /// Büyük başlıklar için en ağır (black) kesim.
    static func display(_ size: CGFloat) -> Font {
        .system(size: size, weight: .black)
    }

    /// Ara başlıklar için ağır (heavy) kesim.
    static func heading(_ size: CGFloat) -> Font {
        .system(size: size, weight: .heavy)
    }
}

/// Siyah zeminli, beyaz yazılı, keskin köşeli birincil buton.
struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 17, weight: .bold))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(Theme.ink)
            .foregroundStyle(Theme.paper)
            .opacity(configuration.isPressed ? 0.75 : 1)
    }
}

/// Keskin köşeli, kalın siyah kenarlıklı kart/kutu görünümü.
struct InkBorder: ViewModifier {
    var lineWidth: CGFloat = 2

    func body(content: Content) -> some View {
        content
            .background(Theme.paper)
            .overlay(Rectangle().strokeBorder(Theme.ink, lineWidth: lineWidth))
    }
}

extension View {
    func inkBordered(lineWidth: CGFloat = 2) -> some View {
        modifier(InkBorder(lineWidth: lineWidth))
    }
}
