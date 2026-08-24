import SwiftUI
import SwiftData

/// Seçilen JLPT seviyesinin kanjilerini 4 eşit parçaya böler ("N5 Kanji's Part 1" gibi).
/// Bir parça, o parçadaki kanjilerle FlashcardSessionView'ı açar — şık havuzu olarak
/// yine tüm seviyeyi (80 kanji) kullanır ki 4 şık her zaman dolu olsun.
struct KanjiPartSelectionView: View {
    let level: String

    @State private var allKanji: [Kanji] = []

    private var parts: [[Kanji]] {
        guard !allKanji.isEmpty else { return [] }
        let partCount = 4
        let size = Int(ceil(Double(allKanji.count) / Double(partCount)))
        return stride(from: 0, to: allKanji.count, by: size).map {
            Array(allKanji[$0..<min($0 + size, allKanji.count)])
        }
    }

    var body: some View {
        Group {
            if allKanji.isEmpty {
                SwiftUI.ProgressView()
            } else {
                ScrollView {
                    VStack(spacing: 16) {
                        ForEach(Array(parts.enumerated()), id: \.offset) { index, part in
                            NavigationLink {
                                FlashcardSessionView(
                                    sessionKey: "kanji_\(level)_part\(index + 1)",
                                    itemKind: .kanji,
                                    allItems: part,
                                    distractorPool: allKanji,
                                    accentColor: .green,
                                    title: "\(level) Kanji's Part \(index + 1)"
                                )
                            } label: {
                                partRow(index: index, count: part.count)
                            }
                        }
                    }
                    .padding()
                }
                .background(Color(uiColor: .systemGroupedBackground))
            }
        }
        .navigationTitle("\(level) Kanji's")
        .onAppear {
            guard allKanji.isEmpty else { return }
            guard let url = Bundle.main.url(forResource: "\(level)KanjiData", withExtension: "json"),
                  let data = try? Data(contentsOf: url),
                  let loaded = try? JSONDecoder().decode([Kanji].self, from: data) else {
                return
            }
            allKanji = loaded
        }
    }

    private func partRow(index: Int, count: Int) -> some View {
        HStack(spacing: 16) {
            Text("\(index + 1)")
                .font(.headline)
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)
                .background(Color.green)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(alignment: .leading, spacing: 2) {
                Text("\(level) Kanji's Part \(index + 1)")
                    .font(.system(.headline, design: .serif))
                Text("\(count) kanji")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(.tertiary)
        }
        .padding()
        .background(Color(uiColor: .systemBackground))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(Color.green.opacity(0.25), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

#Preview {
    NavigationStack {
        KanjiPartSelectionView(level: "N5")
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
