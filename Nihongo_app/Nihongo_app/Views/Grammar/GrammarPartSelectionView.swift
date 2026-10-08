import SwiftUI
import SwiftData

/// Seçilen JLPT seviyesinin gramer konularını bir "Learning Path" (öğrenme yolu) olarak
/// ünitelere böler. Kullanıcı bu ekranı yukarıdan aşağıya takip eder.
/// Bölüm İçi Kilit (Intra-Level Gating): Ünite 1 açıktır; sonraki üniteler önceki ünite bitince açılır.
struct GrammarPartSelectionView: View {
    let level: String

    @Environment(\.modelContext) private var modelContext

    @State private var syllabus: [GrammarUnit] = []
    @State private var pointsDict: [String: GrammarPoint] = [:]
    @State private var learnedIDs: Set<String> = []
    @State private var showLockedAlert = false
    @State private var lockedUnitId: Int = 1

    private var jlptLevel: JLPTLevel {
        JLPTLevel(rawValue: level) ?? .n5
    }

    var body: some View {
        Group {
            if syllabus.isEmpty {
                SwiftUI.ProgressView()
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 36) {
                        ForEach(Array(syllabus.enumerated()), id: \.offset) { index, unit in
                            let isUnlocked = LevelProgressionService.shared.isPartUnlocked(
                                module: "grammar",
                                level: jlptLevel,
                                partIndex: index,
                                context: modelContext
                            )
                            let isUnitComplete = isUnitFinished(unit)

                            VStack(alignment: .leading, spacing: 12) {
                                // Ünite Başlığı
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        HStack(spacing: 8) {
                                            Text("Ünite \(unit.id): \(unit.title)")
                                                .font(Theme.display(24))
                                                .foregroundStyle(isUnlocked ? Theme.ink : Theme.secondaryInk)

                                            if isUnitComplete {
                                                Image(systemName: "checkmark.seal.fill")
                                                    .foregroundStyle(.green)
                                            } else if !isUnlocked {
                                                Image(systemName: "lock.fill")
                                                    .foregroundStyle(Theme.secondaryInk)
                                            }
                                        }

                                        Text(unit.description)
                                            .font(.subheadline)
                                            .foregroundStyle(Theme.secondaryInk)
                                            .lineSpacing(4)
                                            .fixedSize(horizontal: false, vertical: true)
                                    }
                                }
                                .padding(.bottom, 8)

                                // Ünite Konuları
                                ForEach(unit.grammarKeys, id: \.self) { key in
                                    if let point = pointsDict[key] {
                                        if isUnlocked {
                                            NavigationLink {
                                                GrammarLessonView(point: point)
                                            } label: {
                                                topicRow(point, isUnlocked: true)
                                            }
                                        } else {
                                            Button {
                                                lockedUnitId = unit.id
                                                showLockedAlert = true
                                            } label: {
                                                topicRow(point, isUnlocked: false)
                                            }
                                            .buttonStyle(.plain)
                                        }
                                    }
                                }
                            }
                            .opacity(isUnlocked ? 1 : 0.6)
                        }
                    }
                    .padding(20)
                }
                .scrollIndicators(.hidden)
                .background(Theme.paper)
            }
        }
        .navigationTitle("\(level) Gramer")
        .alert("Ünite Kilitli", isPresented: $showLockedAlert) {
            Button("Tamam", role: .cancel) {}
        } message: {
            Text("Ünite \(lockedUnitId) kilidini açmak için lütfen önceki ünitedeki konuları tamamlayın.")
        }
        .onAppear {
            LevelProgressionService.shared.ensureInitialProgress(context: modelContext)
            if syllabus.isEmpty {
                syllabus = ContentStore.loadGrammarSyllabus(level: level)
                let points = ContentStore.loadGrammar(level: level)
                pointsDict = Dictionary(uniqueKeysWithValues: points.map { ($0.key, $0) })
            }
            refreshLearned()
        }
    }

    private func isUnitFinished(_ unit: GrammarUnit) -> Bool {
        guard !unit.grammarKeys.isEmpty else { return false }
        for key in unit.grammarKeys {
            if let point = pointsDict[key] {
                if !learnedIDs.contains(point.id) {
                    return false
                }
            } else {
                return false
            }
        }
        return true
    }

    private func refreshLearned() {
        let all = (try? modelContext.fetch(FetchDescriptor<LearningItemProgress>())) ?? []
        learnedIDs = Set(all.filter { $0.itemKind == .grammar && $0.repetitionCount >= 1 }.map(\.itemID))

        // Ünite tamamlanmalarını LevelProgressionService'e senkronize et
        for (index, unit) in syllabus.enumerated() {
            if isUnitFinished(unit) {
                LevelProgressionService.shared.markPartCompleted(
                    module: "grammar",
                    level: jlptLevel,
                    partIndex: index,
                    context: modelContext
                )
            }
        }
    }

    private func topicRow(_ point: GrammarPoint, isUnlocked: Bool) -> some View {
        let done = learnedIDs.contains(point.id)

        return HStack(spacing: 14) {
            Text(point.pattern)
                .font(Theme.heading(20))
                .foregroundStyle(isUnlocked ? Theme.accent : Theme.secondaryInk)
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .frame(width: 120, alignment: .leading)

            VStack(alignment: .leading, spacing: 2) {
                Text(point.title)
                    .font(Theme.heading(16))
                    .foregroundStyle(isUnlocked ? Theme.ink : Theme.secondaryInk)
                    .lineLimit(1)
                Text(point.romaji)
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryInk)
                    .lineLimit(1)
            }

            Spacer()

            if done {
                Image(systemName: "checkmark.circle.fill")
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
        .background(done ? Color.green.opacity(0.12) : Theme.paper)
        .overlay(Rectangle().strokeBorder(Theme.ink, lineWidth: 2))
    }
}

#Preview {
    NavigationStack {
        GrammarPartSelectionView(level: "N5")
    }
    .modelContainer(for: [UserLevelProgress.self, LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
