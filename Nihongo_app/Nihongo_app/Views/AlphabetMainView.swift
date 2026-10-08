import SwiftUI
import SwiftData

struct AlphabetMainView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var allProgress: [LearningItemProgress]



    enum AlphabetDisplayMode: Int {
        case chart
        case practice
    }

    @State private var displayMode: AlphabetDisplayMode = .chart

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

                // Tab Değiştirici: Alfabe Tablosu (Sesli) / Kartlarla Çalış
                Picker("", selection: $displayMode) {
                    Text(L10n.alphabetChartTitle).tag(AlphabetDisplayMode.chart)
                    Text(L10n.practiceCardsTab).tag(AlphabetDisplayMode.practice)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 20)
                .padding(.bottom, 12)

                if displayMode == .chart {
                    AlphabetChartView()
                } else {
                    practiceCardsView
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
