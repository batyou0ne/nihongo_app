import SwiftUI
import SwiftData

/// Seçilen JLPT seviyesinin gramer konularını bir "Learning Path" (öğrenme yolu) olarak
/// ünitelere böler. Kullanıcı bu ekranı yukarıdan aşağıya takip eder.
struct GrammarPartSelectionView: View {
    let level: String

    @Environment(\.modelContext) private var modelContext

    @State private var syllabus: [GrammarUnit] = []
    @State private var pointsDict: [String: GrammarPoint] = [:]
    @State private var learnedIDs: Set<String> = []

    var body: some View {
        Group {
            if syllabus.isEmpty {
                SwiftUI.ProgressView()
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 36) {
                        ForEach(syllabus) { unit in
                            VStack(alignment: .leading, spacing: 12) {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text("Ünite \(unit.id): \(unit.title)")
                                        .font(Theme.display(24))
                                        .foregroundStyle(Theme.ink)
                                    Text(unit.description)
                                        .font(.subheadline)
                                        .foregroundStyle(Theme.secondaryInk)
                                        .lineSpacing(4)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                                .padding(.bottom, 8)

                                ForEach(unit.grammarKeys, id: \.self) { key in
                                    if let point = pointsDict[key] {
                                        NavigationLink {
                                            GrammarLessonView(point: point)
                                        } label: {
                                            topicRow(point)
                                        }
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
            if syllabus.isEmpty {
                syllabus = ContentStore.loadGrammarSyllabus(level: level)
                let points = ContentStore.loadGrammar(level: level)
                pointsDict = Dictionary(uniqueKeysWithValues: points.map { ($0.key, $0) })
            }
            refreshLearned()
        }
    }

    private func refreshLearned() {
        let all = (try? modelContext.fetch(FetchDescriptor<LearningItemProgress>())) ?? []
        learnedIDs = Set(all.filter { $0.itemKind == .grammar && $0.repetitionCount >= 1 }.map(\.itemID))
    }

    private func topicRow(_ point: GrammarPoint) -> some View {
        let done = learnedIDs.contains(point.id)

        return HStack(spacing: 14) {
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

            if done {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
            } else {
                Image(systemName: "arrow.right")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Theme.ink)
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
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
