import SwiftUI
import SwiftData

/// Seçilen JLPT seviyesinin kelimelerini 25'erlik partlara böler ("N5 Kelimeler Part 1"
/// gibi). Bir part, o partın kelimeleriyle FlashcardSessionView'ı açar — şık havuzu
/// olarak yine tüm seviyeyi kullanır ki 4 şık her zaman dolu olsun.
struct VocabularyPartSelectionView: View {
    let level: String

    @State private var allWords: [VocabularyWord] = []

    /// Bölümleme ContentStore'da; ana ekrandaki "Kaldığın yer" kartı da aynı
    /// fonksiyonu kullanıyor ki iki taraf aynı kartlara işaret etsin.
    private var parts: [[VocabularyWord]] {
        allWords.isEmpty ? [] : ContentStore.vocabularyParts(level: level)
    }

    var body: some View {
        Group {
            if allWords.isEmpty {
                SwiftUI.ProgressView()
            } else {
                ScrollView {
                    VStack(spacing: 14) {
                        ForEach(Array(parts.enumerated()), id: \.offset) { index, part in
                            NavigationLink {
                                FlashcardSessionView(
                                    sessionKey: "vocab_\(level)_part\(index + 1)",
                                    itemKind: .vocabularyWord,
                                    allItems: part,
                                    distractorPool: allWords,
                                    accentColor: Theme.accent,
                                    title: "\(level) Kelimeler Part \(index + 1)"
                                )
                            } label: {
                                partRow(index: index, count: part.count)
                            }
                        }
                    }
                    .padding(20)
                }
                .scrollIndicators(.hidden)
                .background(Theme.paper)
            }
        }
        .navigationTitle("\(level) Kelimeler")
        .onAppear {
            guard allWords.isEmpty else { return }
            guard let url = Bundle.main.url(forResource: "\(level)VocabularyData", withExtension: "json"),
                  let data = try? Data(contentsOf: url),
                  let loaded = try? JSONDecoder().decode([VocabularyWord].self, from: data) else {
                return
            }
            allWords = loaded
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
                Text("\(level) Kelimeler Part \(index + 1)")
                    .font(Theme.heading(19))
                    .foregroundStyle(Theme.ink)
                Text("\(count) kelime")
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
        VocabularyPartSelectionView(level: "N5")
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
