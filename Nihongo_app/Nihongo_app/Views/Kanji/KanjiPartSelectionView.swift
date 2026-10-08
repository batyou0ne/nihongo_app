import SwiftUI
import SwiftData

/// Seçilen JLPT seviyesinin kanjilerini eşit parçalara böler ("N5 Kanji's Part 1" gibi).
/// Bölüm İçi Kilit (Intra-Level Gating): Part 1 her zaman açıktır; sonraki parçalar
/// ancak bir önceki parça tamamlandığında açılır.
struct KanjiPartSelectionView: View {
    let level: String

    @Environment(\.modelContext) private var modelContext
    @Query private var levelRecords: [UserLevelProgress]

    @State private var allKanji: [Kanji] = []
    @State private var showLockedPartAlert = false
    @State private var lockedPartNumber: Int = 1

    private var jlptLevel: JLPTLevel {
        JLPTLevel(rawValue: level) ?? .n5
    }

    private var currentLevelProgress: UserLevelProgress {
        LevelProgressionService.shared.getProgress(for: jlptLevel, context: modelContext)
    }

    private var parts: [[Kanji]] {
        allKanji.isEmpty ? [] : ContentStore.kanjiParts(level: level)
    }

    var body: some View {
        Group {
            if allKanji.isEmpty {
                SwiftUI.ProgressView()
            } else {
                ScrollView {
                    VStack(spacing: 16) {
                        ForEach(Array(parts.enumerated()), id: \.offset) { index, part in
                            let isUnlocked = LevelProgressionService.shared.isPartUnlocked(
                                module: "kanji",
                                level: jlptLevel,
                                partIndex: index,
                                context: modelContext
                            )
                            let isCompleted = currentLevelProgress.isPartCompleted(module: "kanji", index: index)

                            if isUnlocked {
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
                                    partRow(index: index, count: part.count, isUnlocked: true, isCompleted: isCompleted)
                                }
                            } else {
                                Button {
                                    lockedPartNumber = index + 1
                                    showLockedPartAlert = true
                                } label: {
                                    partRow(index: index, count: part.count, isUnlocked: false, isCompleted: false)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding()
                }
                .scrollIndicators(.hidden)
                .background(Theme.paper)
            }
        }
        .navigationTitle("\(level) Kanji's")
        .alert("Bölüm Kilitli", isPresented: $showLockedPartAlert) {
            Button("Tamam", role: .cancel) {}
        } message: {
            Text("Part \(lockedPartNumber) kilidini açmak için lütfen önceki bölümü tamamlayın.")
        }
        .onAppear {
            LevelProgressionService.shared.ensureInitialProgress(context: modelContext)
            guard allKanji.isEmpty else { return }
            guard let url = Bundle.main.url(forResource: "\(level)KanjiData", withExtension: "json"),
                  let data = try? Data(contentsOf: url),
                  let loaded = try? JSONDecoder().decode([Kanji].self, from: data) else {
                return
            }
            allKanji = loaded
        }
    }

    private func partRow(index: Int, count: Int, isUnlocked: Bool, isCompleted: Bool) -> some View {
        HStack(spacing: 16) {
            Text("\(index + 1)")
                .font(Theme.heading(19))
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)
                .background(isUnlocked ? (isCompleted ? Color.green : Theme.accent) : Theme.secondaryInk)

            VStack(alignment: .leading, spacing: 2) {
                Text("\(level) Kanji's Part \(index + 1)")
                    .font(Theme.heading(19))
                    .foregroundStyle(isUnlocked ? Theme.ink : Theme.secondaryInk)
                Text(isUnlocked ? (isCompleted ? "Tamamlandı • \(count) kanji" : "\(count) kanji") : "Kilitli • Önceki bölümü tamamla")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
            }

            Spacer()

            if isCompleted {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 20))
                    .foregroundStyle(.green)
            } else if isUnlocked {
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
        KanjiPartSelectionView(level: "N5")
    }
    .modelContainer(for: [UserLevelProgress.self, LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
