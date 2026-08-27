import SwiftUI
import SwiftData

/// Seçilen JLPT seviyesinin gramer konularını kategoriye göre gruplar
/// ("Edatlar", "Fiiller"...). Her satır bir konu; seçilince o konunun ders
/// ekranı (GrammarLessonView) açılır. Öğrenilen konularda küçük bir onay işareti gösterilir.
struct GrammarPartSelectionView: View {
    let level: String

    @Environment(\.modelContext) private var modelContext

    @State private var points: [GrammarPoint] = []
    @State private var learnedIDs: Set<String> = []

    /// Kategori sırası GrammarCategory.order ile; kategori içi difficulty'ye göre.
    private var groups: [(category: GrammarCategory, points: [GrammarPoint])] {
        Dictionary(grouping: points, by: \.category)
            .map { (category: $0.key, points: $0.value.sorted { $0.difficulty < $1.difficulty }) }
            .sorted { $0.category.order < $1.category.order }
    }

    var body: some View {
        Group {
            if points.isEmpty {
                SwiftUI.ProgressView()
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 28) {
                        ForEach(groups, id: \.category) { group in
                            VStack(alignment: .leading, spacing: 12) {
                                Text(group.category.displayName)
                                    .font(Theme.display(24))
                                    .foregroundStyle(Theme.ink)

                                ForEach(group.points) { point in
                                    NavigationLink {
                                        GrammarLessonView(point: point)
                                    } label: {
                                        topicRow(point)
                                    }
                                }
                            }
                        }
                    }
                    .padding(20)
                }
                .scrollIndicators(.hidden)
                .background(Theme.paper)
            }
        }
        .navigationTitle("\(level) Gramer")
        .onAppear {
            if points.isEmpty {
                points = ContentStore.loadGrammar(level: level)
            }
            refreshLearned()
        }
    }

    /// Hangi konuların en az bir kez doğru bilindiğini (repetitionCount >= 1) bellekte hesaplar.
    /// #Predicate ile enum karşılaştırması SwiftData'da desteklenmediği için tüm kayıtlar
    /// çekilip türe göre burada filtrelenir (bkz. FlashcardSessionView.syncProgress).
    private func refreshLearned() {
        let all = (try? modelContext.fetch(FetchDescriptor<LearningItemProgress>())) ?? []
        learnedIDs = Set(all.filter { $0.itemKind == .grammar && $0.repetitionCount >= 1 }.map(\.itemID))
    }

    private func topicRow(_ point: GrammarPoint) -> some View {
        HStack(spacing: 14) {
            Text(point.pattern)
                .font(Theme.heading(20))
                .foregroundStyle(Theme.accent)
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .frame(width: 120, alignment: .leading)

            VStack(alignment: .leading, spacing: 2) {
                Text(point.title)
                    .font(Theme.heading(16))
                    .foregroundStyle(Theme.ink)
                    .lineLimit(1)
                Text(point.romaji)
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryInk)
                    .lineLimit(1)
            }

            Spacer()

            if learnedIDs.contains(point.id) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(Theme.accent)
            } else {
                Image(systemName: "arrow.right")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Theme.ink)
            }
        }
        .padding()
        .inkBordered()
    }
}

#Preview {
    NavigationStack {
        GrammarPartSelectionView(level: "N5")
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
