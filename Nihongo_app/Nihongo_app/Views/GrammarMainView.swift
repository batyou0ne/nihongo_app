import SwiftUI
import SwiftData

struct GrammarMainView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var levelRecords: [UserLevelProgress]

    @State private var selectedLockedLevel: JLPTLevel?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text(L10n.grammarTitle)
                        .font(Theme.display(36))
                        .foregroundStyle(Theme.ink)
                        .padding(.top, 10)

                    VStack(spacing: 14) {
                        ForEach(JLPTLevel.allCases) { level in
                            let isUnlocked = LevelProgressionService.shared.isLevelUnlocked(level, context: modelContext)

                            if isUnlocked {
                                NavigationLink {
                                    GrammarSessionView(level: level.rawValue)
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
                }
                .padding(20)
                .padding(.bottom, 80)
            }
            .scrollIndicators(.hidden)
            .background(Theme.paper)
            .navigationBarTitleDisplayMode(.inline)
            .sheet(item: $selectedLockedLevel) { level in
                LockedLevelInfoSheet(level: level)
            }
            .onAppear {
                LevelProgressionService.shared.ensureInitialProgress(context: modelContext)
            }
        }
        .tint(Theme.accent)
    }

    private func levelRow(_ level: JLPTLevel, isUnlocked: Bool) -> some View {
        let counts = level.targetCounts

        return HStack(spacing: 16) {
            Image(systemName: "text.alignleft")
                .font(.title2)
                .foregroundStyle(isUnlocked ? Theme.accent : Theme.secondaryInk)
                .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 2) {
                Text(L10n.grammarLevel(level.rawValue))
                    .font(Theme.heading(19))
                    .foregroundStyle(isUnlocked ? Theme.ink : Theme.secondaryInk)

                Text(isUnlocked ? "\(counts.grammar) konu" : "Kilitli • \(level.subtitle)")
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
    GrammarMainView()
        .modelContainer(for: [UserLevelProgress.self, LearningItemProgress.self, UserProgress.self], inMemory: true)
}
