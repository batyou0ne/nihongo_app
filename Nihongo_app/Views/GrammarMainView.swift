import SwiftUI
import SwiftData

struct GrammarMainView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var allProgress: [LearningItemProgress]

    private func learnedCount(_ kind: LearnableItemKind) -> Int {
        allProgress.filter { $0.itemKind == kind && $0.repetitionCount >= 1 }.count
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("Gramer")
                        .font(Theme.display(36))
                        .foregroundStyle(Theme.ink)
                        .padding(.top, 10)
                        
                    SectionLabel("ÖĞREN")

                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                        NavigationLink {
                            GrammarLevelSelectionView()
                        } label: {
                            ModuleCard(kind: .grammar, subtitle: "N5 · 5 kategori", learned: learnedCount(.grammar))
                        }
                    }
                }
                .padding(20)
                .padding(.bottom, 80)
            }
            .background(Theme.paper)
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(Theme.accent)
    }
}

#Preview {
    GrammarMainView()
        .modelContainer(for: LearningItemProgress.self, inMemory: true)
}
