import SwiftUI
import SwiftData

struct AlphabetMainView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var allProgress: [LearningItemProgress]

    @State private var selectedCharacterType: CharacterType?
    @State private var isShowingLearning = false

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
                        Button {
                            selectedCharacterType = .hiragana
                            isShowingLearning = true
                        } label: {
                            ModuleCard(kind: .hiraganaCharacter, subtitle: "あいうえお", learned: learnedCount(.hiraganaCharacter))
                        }

                        Button {
                            selectedCharacterType = .katakana
                            isShowingLearning = true
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
            .fullScreenCover(isPresented: $isShowingLearning) {
                if let type = selectedCharacterType {
                    NavigationStack {
                        LearningView(characterType: type)
                    }
                }
            }
        }
        .tint(Theme.accent)
    }
}

#Preview {
    AlphabetMainView()
        .modelContainer(for: LearningItemProgress.self, inMemory: true)
}
