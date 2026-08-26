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
                                    accentColor: Theme.accent,
                                    title: "\(level) Kanji's Part \(index + 1)"
                                )
                            } label: {
                                partRow(index: index, count: part.count)
                            }
                        }
                    }
                    .padding()
                }
                .background(Theme.paper)
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
                .font(Theme.heading(19))
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)
                .background(Theme.accent)

            VStack(alignment: .leading, spacing: 2) {
                Text("\(level) Kanji's Part \(index + 1)")
                    .font(Theme.heading(19))
                    .foregroundStyle(Theme.ink)
                Text("\(count) kanji")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
            }

            Spacer()
            Image(systemName: "arrow.right")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(Theme.ink)
        }
        .padding()
        .inkBordered()
    }
}

#Preview {
    NavigationStack {
        KanjiPartSelectionView(level: "N5")
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
