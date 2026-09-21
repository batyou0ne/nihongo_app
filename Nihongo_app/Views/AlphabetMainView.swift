import SwiftUI
import SwiftData

struct AlphabetMainView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var allProgress: [LearningItemProgress]

    private func learnedCount(_ kind: LearnableItemKind) -> Int {
        allProgress.filter { $0.itemKind == kind && $0.repetitionCount >= 1 }.count
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("Alfabe")
                        .font(Theme.display(36))
                        .foregroundStyle(Theme.ink)
                        .padding(.top, 10)
                        
                    SectionLabel("ÖĞREN")

                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                        NavigationLink {
                            LearningView(characterType: .hiragana)
                        } label: {
                            ModuleCard(kind: .hiraganaCharacter, subtitle: "あいうえお", learned: learnedCount(.hiraganaCharacter))
                        }

                        NavigationLink {
                            LearningView(characterType: .katakana)
                        } label: {
                            ModuleCard(kind: .katakanaCharacter, subtitle: "アイウエオ", learned: learnedCount(.katakanaCharacter))
                        }

                        NavigationLink {
                            KanjiLevelSelectionView()
                        } label: {
                            ModuleCard(kind: .kanji, subtitle: "N5 · 4 bölüm", learned: learnedCount(.kanji))
                        }
                    }
                }
                .padding(20)
                // Yüzen ada (Floating Tab Bar) için altta boşluk bırakalım
                .padding(.bottom, 80)
            }
            .background(Theme.paper)
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(Theme.accent)
    }
}

#Preview {
    AlphabetMainView()
        .modelContainer(for: LearningItemProgress.self, inMemory: true)
}
