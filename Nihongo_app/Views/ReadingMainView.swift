import SwiftUI

struct ReadingMainView: View {
    @State private var allStories: [Story] = []

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
                    Text("Okuma")
                        .font(Theme.display(36))
                        .foregroundStyle(Theme.ink)
                        .padding(.top, 10)
                        
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
                                                
                                                Text("\(story.sentences.count) cümle")
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
                .padding(20)
                .padding(.bottom, 80)
            }
            .background(Theme.paper)
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                if allStories.isEmpty {
                    allStories = ContentStore.loadStories(level: "N5")
                }
            }
        }
        .tint(Theme.accent)
    }
}

#Preview {
    ReadingMainView()
}
