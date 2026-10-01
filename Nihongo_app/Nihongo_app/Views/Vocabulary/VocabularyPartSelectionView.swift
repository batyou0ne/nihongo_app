import SwiftUI
import SwiftData

/// Seçilen JLPT seviyesinin kelimelerini yükler ve kullanıcının henüz tamamlamadığı
/// ilk "part"ı otomatik bularak FlashcardSessionView'ı başlatır.
struct VocabularyPartSelectionView: View {
    let level: String

    @State private var allWords: [VocabularyWord] = []
    @Query private var allProgress: [LearningItemProgress]
    @Environment(TabBarManager.self) private var tabBarManager

    private var parts: [[VocabularyWord]] {
        allWords.isEmpty ? [] : ContentStore.vocabularyParts(level: level)
    }

    private var currentPartIndex: Int {
        guard !parts.isEmpty else { return 0 }
        let learnedIDs = Set(allProgress.filter { $0.itemKind == .vocabularyWord && $0.isLearned }.map(\.itemID))
        for (index, part) in parts.enumerated() {
            let partIDs = Set(part.map(\.id))
            if !partIDs.isSubset(of: learnedIDs) {
                return index
            }
        }
        return parts.count - 1
    }

    var body: some View {
        Group {
            if allWords.isEmpty {
                SwiftUI.ProgressView()
                    .background(Theme.paper)
                    .navigationTitle(L10n.vocabularyLevel(level))
            } else {
                let index = currentPartIndex
                FlashcardSessionView(
                    sessionKey: "vocab_\(level)_part\(index + 1)",
                    itemKind: .vocabularyWord,
                    allItems: parts[index],
                    distractorPool: allWords,
                    accentColor: Theme.accent,
                    title: "\(level) Part \(index + 1)"
                )
            }
        }
        .onAppear {
            tabBarManager.isHidden = true
            guard allWords.isEmpty else { return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                guard let url = Bundle.main.url(forResource: "\(level)VocabularyData", withExtension: "json"),
                      let data = try? Data(contentsOf: url),
                      let loaded = try? JSONDecoder().decode([VocabularyWord].self, from: data) else {
                    return
                }
                allWords = loaded
            }
        }
        .onDisappear {
            tabBarManager.isHidden = false
        }
    }
}

#Preview {
    NavigationStack {
        VocabularyPartSelectionView(level: "N5")
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
