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
        .background(Color(uiColor: .systemGroupedBackground))
        .navigationTitle("Kanji")
    }

    private func levelRow(_ level: String, isAvailable: Bool) -> some View {
        HStack(spacing: 16) {
            Image(systemName: "character.book.closed.fill")
                .font(.title2)
                .foregroundStyle(isAvailable ? .green : .secondary)
                .frame(width: 44, height: 44)
                .background((isAvailable ? Color.green : Color.gray).opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(alignment: .leading, spacing: 2) {
                Text("\(level) Kanji's")
                    .font(.system(.headline, design: .serif))
                    .foregroundStyle(isAvailable ? .primary : .secondary)
                Text(isAvailable ? "80 kanji · 4 bölüm" : "Yakında")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Spacer()
            if isAvailable {
                Image(systemName: "chevron.right")
                    .foregroundStyle(.tertiary)
            } else {
                Image(systemName: "lock.fill")
                    .foregroundStyle(.tertiary)
            }
        }
        .padding()
        .background(Color(uiColor: .systemBackground))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder((isAvailable ? Color.green : Color.gray).opacity(0.25), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .opacity(isAvailable ? 1 : 0.6)
    }
}

#Preview {
    NavigationStack {
        KanjiLevelSelectionView()
    }
}
