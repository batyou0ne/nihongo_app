import SwiftUI
import SwiftData

struct AlphabetMainView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var allProgress: [LearningItemProgress]



    enum AlphabetDisplayMode: Int {
        case practice
        case chart
    }

    @State private var displayMode: AlphabetDisplayMode = .practice

    private func learnedCount(_ kind: LearnableItemKind) -> Int {
        allProgress.filter { $0.itemKind == kind && $0.repetitionCount >= 1 }.count
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Başlık
                HStack {
                    Text(L10n.alphabetTitle)
                        .font(Theme.display(36))
                        .foregroundStyle(Theme.ink)
                    Spacer()
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 8)

                // Tab Değiştirici: Kartlarla Çalış (Varsayılan) / Alfabe Tablosu (Sesli)
                Picker("", selection: $displayMode) {
                    Text(L10n.practiceCardsTab).tag(AlphabetDisplayMode.practice)
                    Text(L10n.alphabetChartTitle).tag(AlphabetDisplayMode.chart)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 20)
                .padding(.bottom, 12)

                if displayMode == .practice {
                    practiceCardsView
                } else {
                    AlphabetChartView()
                }
            }
            .background(Theme.paper)
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(Theme.accent)
    }

    private var practiceCardsView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                SectionLabel(L10n.sectionLearn)

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
            .padding(.bottom, 80)
        }
    }
}

#Preview {
    AlphabetMainView()
        .modelContainer(for: LearningItemProgress.self, inMemory: true)
}
