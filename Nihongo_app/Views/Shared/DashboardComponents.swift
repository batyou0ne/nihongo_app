import SwiftUI

/// Bölüm başlığı: küçük, harf aralıklı, ikincil renkte.
struct SectionLabel: View {
    let text: String

    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.system(size: 12, weight: .heavy))
            .tracking(1.2)
            .foregroundStyle(Theme.secondaryInk)
    }
}

/// Yuvarlak köşeli, siyah kenarlıklı ilerleme çubuğu — dolu kısım vermilyon.
struct ProgressBar: View {
    let fraction: Double

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 5).fill(Theme.paper)
                RoundedRectangle(cornerRadius: 5)
                    .fill(Theme.accent)
                    .frame(width: geometry.size.width * min(max(fraction, 0), 1))
            }
            .clipShape(RoundedRectangle(cornerRadius: 5))
            .overlay(RoundedRectangle(cornerRadius: 5).strokeBorder(Theme.ink, lineWidth: 2))
        }
        .frame(height: 10)
    }
}

/// Öğrenme modülü kartı: solda büyük vermilyon Japonca karakter, sağda ad,
/// alt satırda öğrenilen oranı ve ilerleme çubuğu.
struct ModuleCard: View {
    let kind: LearnableItemKind
    let subtitle: String
    let learned: Int

    private var total: Int { kind.totalCount }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                Text(kind.symbol)
                    .font(Theme.display(34))
                    .foregroundStyle(Theme.accent)
                Spacer()
                Image(systemName: "arrow.up.right")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Theme.ink)
            }
            
            Spacer(minLength: 0)

            VStack(alignment: .leading, spacing: 4) {
                Text(kind.displayName)
                    .font(Theme.heading(17))
                    .foregroundStyle(Theme.ink)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)

                Text(subtitle)
                    .font(.caption2)
                    .foregroundStyle(Theme.secondaryInk)
                    .lineLimit(1)
            }
            
            HStack {
                Text("\(learned)/\(total)")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(Theme.secondaryInk)
                Spacer()
            }
            
            ProgressBar(fraction: total > 0 ? Double(learned) / Double(total) : 0)
        }
        .padding(14)
        .aspectRatio(1.0, contentMode: .fit)
        .inkBordered()
    }
}
