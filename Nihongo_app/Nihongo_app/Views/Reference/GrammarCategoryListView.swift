import SwiftUI

// MARK: - Category List View

/// Seçilen gramer kategorisindeki tüm öğeleri listeler.
/// Üstte arama çubuğu var, tüm öğeler varsayılan olarak görünür.
/// Fiil kategorisi → VerbConjugationView sheet
/// Diğer kategoriler → GrammarReferenceDetailView sheet
struct GrammarCategoryListView: View {
    let category: GrammarCategory
    let accentColor: Color

    @State private var allPoints: [GrammarPoint] = []
    @State private var searchText = ""
    @State private var selectedPoint: GrammarPoint? = nil

    private var filtered: [GrammarPoint] {
        let q = searchText.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return allPoints }
        return allPoints.filter {
            $0.pattern.contains(q) ||
            $0.title.localizedCaseInsensitiveContains(q) ||
            $0.romaji.localizedCaseInsensitiveContains(q) ||
            $0.explanation.localizedCaseInsensitiveContains(q)
        }
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 10) {
                if filtered.isEmpty {
                    emptyState
                } else {
                    ForEach(filtered) { point in
                        Button {
                            selectedPoint = point
                        } label: {
                            pointRow(point)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(20)
            .padding(.bottom, 90)
        }
        .scrollIndicators(.hidden)
        .background(Theme.paper)
        .searchable(text: $searchText, prompt: "Kural, romaji veya açıklama ara...")
        .navigationTitle(category.displayName)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if allPoints.isEmpty {
                allPoints = ContentStore.loadGrammar(level: "N5")
                    .filter { $0.category == category }
                    .sorted { $0.difficulty < $1.difficulty }
            }
        }
        .sheet(item: $selectedPoint) { point in
            if category == .verb {
                // Fiil için özel çekim sheet'i
                VerbConjugationView(
                    hiragana: "たべる",
                    kanji: "食べる",
                    turkishMeaning: "yemek (örnek fiil)"
                )
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
            } else {
                GrammarReferenceDetailView(point: point, accentColor: accentColor)
                    .presentationDetents([.large])
                    .presentationDragIndicator(.visible)
            }
        }
    }

    // MARK: - Row

    private func pointRow(_ point: GrammarPoint) -> some View {
        HStack(spacing: 14) {
            // Pattern badge
            Text(point.pattern)
                .font(Theme.heading(18))
                .foregroundStyle(accentColor)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .frame(width: 110, alignment: .leading)

            VStack(alignment: .leading, spacing: 3) {
                Text(point.title)
                    .font(Theme.heading(15))
                    .foregroundStyle(Theme.ink)
                    .lineLimit(1)
                Text(point.romaji)
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryInk)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Difficulty dots
            HStack(spacing: 3) {
                ForEach(1...3, id: \.self) { i in
                    Circle()
                        .fill(i <= point.difficulty ? accentColor : accentColor.opacity(0.2))
                        .frame(width: 6, height: 6)
                }
            }

            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundStyle(Theme.secondaryInk)
        }
        .padding(14)
        .inkBordered()
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 36))
                .foregroundStyle(Theme.secondaryInk)
            Text("'\(searchText)' için sonuç yok")
                .font(Theme.heading(16))
                .foregroundStyle(Theme.secondaryInk)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 60)
    }
}

#Preview {
    NavigationStack {
        GrammarCategoryListView(category: .particle, accentColor: .blue)
    }
}
