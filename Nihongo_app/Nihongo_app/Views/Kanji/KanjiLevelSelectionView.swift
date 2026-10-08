import SwiftUI
import SwiftData

/// Kanji modülünün ilk ekranı: JLPT seviyesi seçimi (N5-N1).
/// N5 varsayılan açıktır; üst seviyeler (N4-N1) önceki seviye tamamlanana kadar
/// kilitlidir ve tıklandığında kilit bilgilendirme sheet'i açılır.
struct KanjiLevelSelectionView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var levelRecords: [UserLevelProgress]

    @State private var selectedLockedLevel: JLPTLevel?

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                ForEach(JLPTLevel.allCases) { level in
                    let isUnlocked = LevelProgressionService.shared.isLevelUnlocked(level, context: modelContext)

                    if isUnlocked {
                        NavigationLink {
                            KanjiPartSelectionView(level: level.rawValue)
                        } label: {
                            levelRow(level, isUnlocked: true)
                        }
                    } else {
                        Button {
                            selectedLockedLevel = level
                        } label: {
                            levelRow(level, isUnlocked: false)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding()
        }
        .scrollIndicators(.hidden)
        .background(Theme.paper)
        .navigationTitle("Kanji")
        .sheet(item: $selectedLockedLevel) { level in
            LockedLevelInfoSheet(level: level)
        }
        .onAppear {
            LevelProgressionService.shared.ensureInitialProgress(context: modelContext)
        }
    }

    private func levelRow(_ level: JLPTLevel, isUnlocked: Bool) -> some View {
        let counts = level.targetCounts

        return HStack(spacing: 16) {
            Image(systemName: "character.book.closed.fill")
                .font(.title2)
                .foregroundStyle(isUnlocked ? Theme.accent : Theme.secondaryInk)
                .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 2) {
                Text("\(level.rawValue) Kanji's")
                    .font(Theme.heading(19))
                    .foregroundStyle(isUnlocked ? Theme.ink : Theme.secondaryInk)

                Text(isUnlocked ? "\(counts.kanji) kanji" : "Kilitli • \(level.subtitle)")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
            }

            Spacer()
            if isUnlocked {
                Image(systemName: "arrow.right")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Theme.ink)
            } else {
                Image(systemName: "lock.fill")
                    .foregroundStyle(Theme.secondaryInk)
            }
        }
        .padding()
        .inkBordered()
        .opacity(isUnlocked ? 1 : 0.6)
    }
}

#Preview {
    NavigationStack {
        KanjiLevelSelectionView()
    }
    .modelContainer(for: [UserLevelProgress.self, LearningItemProgress.self, UserProgress.self], inMemory: true)
}
