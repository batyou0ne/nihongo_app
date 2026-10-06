import SwiftUI

// MARK: - Grammar Reference Main View

/// Dilbilgisi Başvuru Kütüphanesi — beş gramer kategorisini kart olarak listeler.
/// Fiil kategorisi → VerbCategoryListView (N5 sözcük listesinden fiiller)
/// Diğer kategoriler → GrammarCategoryListView (N5 gramer kuralları)
struct GrammarReferenceView: View {

    private let categories: [(category: GrammarCategory, icon: String, color: Color)] = [
        (.verb,        "arrowshape.turn.up.right.fill", Color(red: 0.92, green: 0.23, blue: 0.05)),
        (.particle,    "link",                          Color(red: 0.13, green: 0.53, blue: 0.90)),
        (.adjective,   "paintpalette.fill",             Color(red: 0.20, green: 0.70, blue: 0.40)),
        (.conjunction, "arrow.triangle.merge",          Color(red: 0.80, green: 0.45, blue: 0.05)),
        (.expression,  "quote.bubble.fill",             Color(red: 0.55, green: 0.20, blue: 0.80)),
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {

                Text("Bir kategoriye tıkla, kuralları ve formları incele.")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
                    .padding(.top, 4)

                VStack(spacing: 14) {
                    ForEach(categories, id: \.category) { entry in
                        navigationLinkForCategory(entry)
                    }
                }
            }
            .padding(20)
            .padding(.bottom, 90)
        }
        .scrollIndicators(.hidden)
        .background(Theme.paper)
        .tint(Theme.accent)
    }

    // MARK: - Navigation Routing

    @ViewBuilder
    private func navigationLinkForCategory(
        _ entry: (category: GrammarCategory, icon: String, color: Color)
    ) -> some View {
        if entry.category == .verb {
            NavigationLink {
                VerbCategoryListView()
            } label: {
                categoryCard(entry, subtitle: verbSubtitle())
            }
        } else {
            NavigationLink {
                GrammarCategoryListView(
                    category: entry.category,
                    accentColor: entry.color
                )
            } label: {
                categoryCard(entry, subtitle: grammarSubtitle(entry.category))
            }
        }
    }

    // MARK: - Category Card

    private func categoryCard(
        _ entry: (category: GrammarCategory, icon: String, color: Color),
        subtitle: String
    ) -> some View {
        HStack(spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(entry.color.opacity(0.12))
                    .frame(width: 52, height: 52)
                Image(systemName: entry.icon)
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(entry.color)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text(entry.category.displayName)
                    .font(Theme.heading(19))
                    .foregroundStyle(Theme.ink)
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
            }

            Spacer()

            Image(systemName: "arrow.right")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(Theme.secondaryInk)
        }
        .padding(16)
        .inkBordered()
    }

    // MARK: - Subtitles

    private func grammarSubtitle(_ category: GrammarCategory) -> String {
        let count = ContentStore.loadGrammar(level: "N5").filter { $0.category == category }.count
        return "\(count) kural · N5"
    }

    private func verbSubtitle() -> String {
        "Çekim tabloları · N5 fiilleri"
    }
}

#Preview {
    GrammarReferenceView()
}
