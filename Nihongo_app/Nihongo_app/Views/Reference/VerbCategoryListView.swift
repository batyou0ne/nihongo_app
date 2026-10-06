import SwiftUI

// MARK: - Verb Entry

struct VerbEntry: Identifiable, Hashable {
    let id: String       // hiragana
    let hiragana: String
    let kanji: String?
    let romaji: String
    let turkishMeaning: String
    let verbType: VerbType
}

// MARK: - Verb Category List View

/// Fiil kategorisi için özel liste ekranı.
/// N5 kelime hazinesinden fiilleri çeker; üstte arama çubuğu,
/// altında tam liste (tüm fiiller varsayılan gösterilir).
/// Tıklamada VerbConjugationView sheet açılır.
struct VerbCategoryListView: View {

    @State private var verbs: [VerbEntry] = []
    @State private var searchText = ""
    @State private var selectedVerb: VerbEntry? = nil

    // MARK: - Filtered

    private var filtered: [VerbEntry] {
        let q = searchText.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return verbs }
        return verbs.filter {
            $0.hiragana.contains(q) ||
            ($0.kanji?.contains(q) ?? false) ||
            $0.romaji.lowercased().contains(q.lowercased()) ||
            $0.turkishMeaning.localizedCaseInsensitiveContains(q)
        }
    }

    // MARK: - Body

    var body: some View {
        VStack(spacing: 0) {
            // Custom Search Bar
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Theme.secondaryInk)
                
                TextField("Fiil, romaji veya Türkçe anlam ara...", text: $searchText)
                    .font(.system(size: 16))
                    .foregroundStyle(Theme.ink)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                
                if !searchText.isEmpty {
                    Button {
                        searchText = ""
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(Theme.secondaryInk)
                    }
                }
            }
            .padding(12)
            .inkBordered(lineWidth: 1.5)
            .padding(.horizontal, 20)
            .padding(.top, 10)
            .padding(.bottom, 10)
            .background(Theme.paper)

            ScrollView {
                LazyVStack(spacing: 10) {
                    if filtered.isEmpty {
                        emptyState
                    } else {
                        // Type legend
                        typeLegend
                            .padding(.bottom, 4)

                        ForEach(filtered) { verb in
                            Button {
                                selectedVerb = verb
                            } label: {
                                verbRow(verb)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 90)
            }
            .scrollIndicators(.hidden)
        }
        .background(Theme.paper)
        .navigationTitle("Fiiller")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { loadVerbs() }
        .sheet(item: $selectedVerb) { verb in
            VerbConjugationView(
                hiragana: verb.hiragana,
                kanji: verb.kanji,
                turkishMeaning: verb.turkishMeaning
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
    }

    // MARK: - Type Legend

    private var typeLegend: some View {
        HStack(spacing: 16) {
            legendChip("Gr.1", color: Theme.accent, subtitle: "Godan")
            legendChip("Gr.2", color: Color(red: 0.13, green: 0.53, blue: 0.90), subtitle: "Ichidan")
            legendChip("する/くる", color: Color(red: 0.60, green: 0.20, blue: 0.80), subtitle: "Düzensiz")
            Spacer()
            Text("\(filtered.count) fiil")
                .font(.caption)
                .foregroundStyle(Theme.secondaryInk)
        }
    }

    private func legendChip(_ label: String, color: Color, subtitle: String) -> some View {
        HStack(spacing: 4) {
            Circle().fill(color).frame(width: 7, height: 7)
            Text("\(label)")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(Theme.ink)
        }
    }

    // MARK: - Verb Row

    private func verbRow(_ verb: VerbEntry) -> some View {
        HStack(spacing: 14) {
            // Japanese
            VStack(alignment: .leading, spacing: 2) {
                if let kanji = verb.kanji {
                    Text(kanji)
                        .font(Theme.heading(20))
                        .foregroundStyle(Theme.ink)
                    Text(verb.hiragana)
                        .font(.caption)
                        .foregroundStyle(Theme.secondaryInk)
                } else {
                    Text(verb.hiragana)
                        .font(Theme.heading(20))
                        .foregroundStyle(Theme.ink)
                }
            }
            .frame(width: 90, alignment: .leading)

            // Meaning
            VStack(alignment: .leading, spacing: 2) {
                Text(verb.turkishMeaning)
                    .font(.system(size: 14))
                    .foregroundStyle(Theme.ink)
                    .lineLimit(2)
                Text(verb.romaji)
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryInk)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Type badge + chevron
            VStack(alignment: .trailing, spacing: 4) {
                typeTag(verb.verbType)
                Image(systemName: "tablecells")
                    .font(.caption2)
                    .foregroundStyle(Theme.secondaryInk)
            }
        }
        .padding(14)
        .inkBordered()
    }

    private func typeTag(_ type: VerbType) -> some View {
        let (label, color): (String, Color) = {
            switch type {
            case .ichidan:           return ("Gr.2", Color(red: 0.13, green: 0.53, blue: 0.90))
            case .suru:              return ("する", Color(red: 0.60, green: 0.20, blue: 0.80))
            case .kuru:              return ("くる", Color(red: 0.60, green: 0.20, blue: 0.80))
            case .aru:               return ("ある", Color(red: 0.60, green: 0.20, blue: 0.80))
            default:                 return ("Gr.1", Theme.accent)
            }
        }()

        return Text(label)
            .font(.caption2.weight(.bold))
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(color.opacity(0.13))
            .foregroundStyle(color)
            .clipShape(RoundedRectangle(cornerRadius: 6))
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

    // MARK: - Data Loading

    private func loadVerbs() {
        guard verbs.isEmpty else { return }

        DispatchQueue.global(qos: .userInitiated).async {
            guard
                let url = Bundle.main.url(forResource: "N5VocabularyData", withExtension: "json"),
                let data = try? Data(contentsOf: url),
                let raw = try? JSONDecoder().decode([[String: String]].self, from: data)
            else { return }

            let verbEndings: Set<Character> = ["う","く","ぐ","す","つ","ぬ","ぶ","む","る"]
            let nonVerbHints = ["(ünlem)", "(bağlaç)", "(edat)", "(sıfat)", "(zamir)"]

            var seen = Set<String>()
            let entries: [VerbEntry] = raw.compactMap { dict -> VerbEntry? in
                guard
                    let hira = dict["hiragana"], !hira.isEmpty,
                    let last = hira.last, verbEndings.contains(last),
                    let meaning = dict["turkishMeaning"], !meaning.isEmpty,
                    let romaji = dict["romaji"]
                else { return nil }

                if nonVerbHints.contains(where: { meaning.contains($0) }) { return nil }
                guard seen.insert(hira).inserted else { return nil }

                let kanji = dict["kanji"].flatMap { $0.isEmpty ? nil : $0 }
                let type = VerbConjugationEngine.classify(hira)

                return VerbEntry(
                    id: hira,
                    hiragana: hira,
                    kanji: kanji,
                    romaji: romaji,
                    turkishMeaning: meaning,
                    verbType: type
                )
            }
            .sorted { $0.hiragana < $1.hiragana }

            DispatchQueue.main.async {
                self.verbs = entries
            }
        }
    }
}

#Preview {
    NavigationStack {
        VerbCategoryListView()
    }
}
