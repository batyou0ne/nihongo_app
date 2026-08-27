import SwiftUI
import SwiftData

/// "Tekrar Çalışılması Gerekenler" ekranı. Herhangi bir modülde yanlış yapılan öğeler
/// (needsReview işaretli) burada modüle göre gruplu listelenir. Bir öğe, kullanıcı
/// "Öğrendim" ile onaylayana kadar listede kalır — doğru bilmek otomatik çıkarmaz.
/// "Bunlarla çalış" ile sadece o gruptaki öğelerle mini bir flashcard oturumu açılır.
struct ReviewListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(filter: #Predicate<LearningItemProgress> { $0.needsReview == true })
    private var reviewProgress: [LearningItemProgress]

    @State private var hiragana: [JapaneseCharacter] = []
    @State private var katakana: [JapaneseCharacter] = []
    @State private var kanji: [Kanji] = []
    @State private var vocabulary: [VocabularyWord] = []
    @State private var grammar: [GrammarPoint] = []

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                if reviewProgress.isEmpty {
                    emptyState
                } else {
                    moduleSection(kind: .hiraganaCharacter, allModuleItems: hiragana)
                    moduleSection(kind: .katakanaCharacter, allModuleItems: katakana)
                    moduleSection(kind: .kanji, allModuleItems: kanji)
                    moduleSection(kind: .vocabularyWord, allModuleItems: vocabulary)
                    grammarSection()
                }
            }
            .padding(20)
        }
        .scrollIndicators(.hidden)
        .background(Theme.paper)
        .navigationTitle("Tekrar Çalış")
        .onAppear {
            guard hiragana.isEmpty else { return }
            hiragana = ContentStore.loadCharacters(.hiragana)
            katakana = ContentStore.loadCharacters(.katakana)
            kanji = ContentStore.loadKanji()
            vocabulary = ContentStore.loadVocabulary()
            grammar = ContentStore.loadGrammar()
        }
    }

    private var emptyState: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("🎉")
                .font(.system(size: 44))
            Text("Harika!")
                .font(Theme.display(28))
                .foregroundStyle(Theme.ink)
            Text("Tekrar çalışman gereken bir şey yok. Yanlış yaptığın kartlar burada birikir.")
                .font(.subheadline)
                .foregroundStyle(Theme.secondaryInk)
        }
        .padding(.top, 8)
    }

    private func reviewIDs(for kind: LearnableItemKind) -> Set<String> {
        Set(reviewProgress.filter { $0.itemKind == kind }.map(\.itemID))
    }

    @ViewBuilder
    private func moduleSection<Item: FlashcardItem>(kind: LearnableItemKind, allModuleItems: [Item]) -> some View {
        let ids = reviewIDs(for: kind)
        let items = allModuleItems.filter { ids.contains($0.id) }

        if !items.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .firstTextBaseline) {
                    Text("\(kind.displayName) (\(items.count))")
                        .font(Theme.display(24))
                        .foregroundStyle(Theme.ink)
                    Spacer()
                    NavigationLink {
                        FlashcardSessionView(
                            sessionKey: "review_\(kind.rawValue)",
                            itemKind: kind,
                            allItems: items,
                            distractorPool: allModuleItems,
                            accentColor: Theme.accent,
                            title: "Tekrar: \(kind.displayName)"
                        )
                    } label: {
                        Text("Bunlarla çalış →")
                            .font(.system(size: 15, weight: .heavy))
                            .foregroundStyle(Theme.accent)
                    }
                }

                ForEach(items, id: \.id) { item in
                    reviewRow(item, kind: kind)
                }
            }
        }
    }

    private func reviewRow<Item: FlashcardItem>(_ item: Item, kind: LearnableItemKind) -> some View {
        HStack(spacing: 14) {
            Text(item.prompt)
                .font(Theme.heading(26))
                .foregroundStyle(Theme.ink)
                .lineLimit(1)
                .minimumScaleFactor(0.5)

            Text(item.flipRecap)
                .font(.subheadline)
                .foregroundStyle(Theme.secondaryInk)
                .lineLimit(2)

            Spacer()

            Button {
                markAsLearned(kind: kind, itemID: item.id)
            } label: {
                Text("Öğrendim ✓")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(Theme.paper)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Theme.ink)
            }
        }
        .padding(12)
        .inkBordered()
    }

    /// Gramer konuları FlashcardItem değil (ders + pratik akışı farklı), bu yüzden
    /// kendi bölümü var. "Bunlarla çalış" işaretli konuların pratiğini art arda açar.
    @ViewBuilder
    private func grammarSection() -> some View {
        let ids = reviewIDs(for: .grammar)
        let points = grammar.filter { ids.contains($0.id) }

        if !points.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .firstTextBaseline) {
                    Text("Gramer (\(points.count))")
                        .font(Theme.display(24))
                        .foregroundStyle(Theme.ink)
                    Spacer()
                    NavigationLink {
                        GrammarPracticeView(points: points, title: "Tekrar: Gramer")
                    } label: {
                        Text("Bunlarla çalış →")
                            .font(.system(size: 15, weight: .heavy))
                            .foregroundStyle(Theme.accent)
                    }
                }

                ForEach(points) { point in
                    HStack(spacing: 14) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(point.pattern)
                                .font(Theme.heading(20))
                                .foregroundStyle(Theme.accent)
                                .lineLimit(1)
                                .minimumScaleFactor(0.5)
                            Text(point.title)
                                .font(.subheadline)
                                .foregroundStyle(Theme.secondaryInk)
                                .lineLimit(1)
                        }

                        Spacer()

                        Button {
                            markAsLearned(kind: .grammar, itemID: point.id)
                        } label: {
                            Text("Öğrendim ✓")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundStyle(Theme.paper)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(Theme.ink)
                        }
                    }
                    .padding(12)
                    .inkBordered()
                }
            }
        }
    }

    private func markAsLearned(kind: LearnableItemKind, itemID: String) {
        guard let progress = reviewProgress.first(where: { $0.itemKind == kind && $0.itemID == itemID }) else {
            return
        }
        progress.needsReview = false
        try? modelContext.save()
    }
}

#Preview {
    NavigationStack {
        ReviewListView()
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
