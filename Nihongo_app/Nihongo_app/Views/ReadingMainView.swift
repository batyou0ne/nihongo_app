import SwiftUI
import SwiftData

struct ReadingMainView: View {
    @Environment(\.modelContext) private var modelContext

    @State private var selectedLevel: JLPTLevel = .n5
    @State private var allStories: [Story] = []
    @State private var selectedLockedLevel: JLPTLevel?

    var groupedStories: [(key: StoryType, value: [Story])] {
        let grouped = Dictionary(grouping: allStories, by: { $0.type })
        return StoryType.allCases.compactMap { type in
            if let stories = grouped[type], !stories.isEmpty {
                return (key: type, value: stories)
            }
            return nil
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text(L10n.readingTitle)
                        .font(Theme.display(36))
                        .foregroundStyle(Theme.ink)
                        .padding(.top, 10)

                    // Seviye Seçim Barı (N5 - N1)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(JLPTLevel.allCases) { level in
                                let isUnlocked = LevelProgressionService.shared.isLevelUnlocked(level, context: modelContext)
                                let isSelected = (selectedLevel == level)

                                Button {
                                    if isUnlocked {
                                        selectedLevel = level
                                        loadStories(for: level)
                                    } else {
                                        selectedLockedLevel = level
                                    }
                                } label: {
                                    HStack(spacing: 6) {
                                        Text(level.rawValue)
                                            .font(.system(size: 15, weight: .bold))

                                        if !isUnlocked {
                                            Image(systemName: "lock.fill")
                                                .font(.caption2)
                                        }
                                    }
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                                    .background(isSelected ? Theme.accent : Theme.paper)
                                    .foregroundStyle(isSelected ? Theme.paper : (isUnlocked ? Theme.ink : Theme.secondaryInk))
                                    .clipShape(Capsule())
                                    .overlay(
                                        Capsule().strokeBorder(Theme.ink, lineWidth: isSelected ? 0 : 1.5)
                                    )
                                    .opacity(isUnlocked ? 1.0 : 0.6)
                                }
                            }
                        }
                    }

                    // Hikaye Grupları
                    if allStories.isEmpty {
                        VStack(spacing: 12) {
                            Text("Bu seviye için henüz hikaye eklenmedi.")
                                .font(.subheadline)
                                .foregroundStyle(Theme.secondaryInk)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.top, 40)
                    } else {
                        ForEach(groupedStories, id: \.key) { group in
                            VStack(alignment: .leading, spacing: 12) {
                                SectionLabel(group.key.displayName.uppercased())

                                VStack(spacing: 12) {
                                    ForEach(group.value) { story in
                                        NavigationLink {
                                            StoryReaderView(story: story)
                                        } label: {
                                            HStack(spacing: 16) {
                                                Image(systemName: "book.fill")
                                                    .font(.title)
                                                    .foregroundStyle(Theme.accent)
                                                    .frame(width: 40)

                                                VStack(alignment: .leading, spacing: 4) {
                                                    Text(story.title)
                                                        .font(Theme.heading(18))
                                                        .foregroundStyle(Theme.ink)
                                                        .multilineTextAlignment(.leading)

                                                    Text(L10n.sentenceCount(story.sentences.count))
                                                        .font(.caption)
                                                        .foregroundStyle(Theme.secondaryInk)
                                                }

                                                Spacer()

                                                Image(systemName: "arrow.right")
                                                    .font(.system(size: 15, weight: .bold))
                                                    .foregroundStyle(Theme.ink)
                                            }
                                            .padding(16)
                                            .inkBordered()
                                        }
                                    }
                                }
                            }
                            .padding(.top, 10)
                        }
                    }
                }
                .padding(20)
                .padding(.bottom, 80)
            }
            .background(Theme.paper)
            .navigationBarTitleDisplayMode(.inline)
            .sheet(item: $selectedLockedLevel) { level in
                LockedLevelInfoSheet(level: level)
            }
            .onAppear {
                LevelProgressionService.shared.ensureInitialProgress(context: modelContext)
                loadStories(for: selectedLevel)
            }
        }
        .tint(Theme.accent)
    }

    private func loadStories(for level: JLPTLevel) {
        allStories = ContentStore.loadStories(level: level.rawValue)
    }
}

#Preview {
    ReadingMainView()
        .modelContainer(for: [UserLevelProgress.self, LearningItemProgress.self, UserProgress.self], inMemory: true)
}
