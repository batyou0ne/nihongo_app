import SwiftUI

/// Kanji modülünün ilk ekranı: JLPT seviyesi seçimi (N5-N1). Şu an sadece N5 için veri
/// var (bkz. N5KanjiData.json, https://jlptsensei.com/jlpt-n5-kanji-list/ kaynaklı);
/// diğer seviyeler veri eklenene kadar kilitli gösteriliyor.
struct KanjiLevelSelectionView: View {
    private let levels: [(level: String, isAvailable: Bool)] = [
        ("N5", true),
        ("N4", false),
        ("N3", false),
        ("N2", false),
        ("N1", false)
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                ForEach(levels, id: \.level) { entry in
                    if entry.isAvailable {
                        NavigationLink {
                            KanjiPartSelectionView(level: entry.level)
                        } label: {
                            levelRow(entry.level, isAvailable: true)
                        }
                    } else {
                        levelRow(entry.level, isAvailable: false)
                    }
                }
            }
            .padding()
        }
        .scrollIndicators(.hidden)
        .background(Theme.paper)
        .navigationTitle("Kanji")
    }

    private func levelRow(_ level: String, isAvailable: Bool) -> some View {
        HStack(spacing: 16) {
            Image(systemName: "character.book.closed.fill")
                .font(.title2)
                .foregroundStyle(isAvailable ? Theme.accent : Theme.secondaryInk)
                .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 2) {
                Text("\(level) Kanji's")
                    .font(Theme.heading(19))
                    .foregroundStyle(isAvailable ? Theme.ink : Theme.secondaryInk)
                Text(isAvailable ? "80 kanji · 4 bölüm" : "Yakında")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
            }

            Spacer()
            if isAvailable {
                Image(systemName: "arrow.right")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Theme.ink)
            } else {
                Image(systemName: "lock.fill")
                    .foregroundStyle(Theme.secondaryInk)
            }
        }
        .padding()
        .inkBordered()
        .opacity(isAvailable ? 1 : 0.5)
    }
}

#Preview {
    NavigationStack {
        KanjiLevelSelectionView()
    }
}
