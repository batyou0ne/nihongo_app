import SwiftUI

// MARK: - Vocabulary Word (local struct for internal use)

private struct VerbEntry: Identifiable {
    let id: String      // hiragana (unique key)
    let hiragana: String
    let kanji: String?
    let romaji: String
    let turkishMeaning: String
}

// MARK: - Verb Library View

/// Tüm N5 fiillerini listeleyen kütüphane ekranı.
/// Bir fiile tıklandığında VerbConjugationView sheet olarak açılır.
struct VerbLibraryView: View {

    @State private var verbs: [VerbEntry] = []
    @State private var searchText = ""
    @State private var selectedVerb: VerbEntry? = nil
    @State private var showConjugation = false

    // MARK: - Filtered

    private var filtered: [VerbEntry] {
        if searchText.trimmingCharacters(in: .whitespaces).isEmpty { return verbs }
        let q = searchText.lowercased()
        return verbs.filter {
            $0.hiragana.contains(q) ||
            ($0.kanji?.contains(q) ?? false) ||
            $0.romaji.lowercased().contains(q) ||
            $0.turkishMeaning.lowercased().contains(q)
        }
    }

    // MARK: - Body

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 10) {
                if filtered.isEmpty {
                    emptyState
                } else {
                    ForEach(filtered) { verb in
                        verbRow(verb)
                            .onTapGesture {
                                selectedVerb = verb
                                showConjugation = true
                            }
                    }
                }
            }
            .padding(20)
            .padding(.bottom, 80)
        }
        .searchable(text: $searchText, prompt: "Fiil, romaji veya anlam ara...")
        .scrollIndicators(.hidden)
        .background(Theme.paper)
        .navigationTitle("Fiil Kütüphanesi")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { loadVerbs() }
        .sheet(isPresented: $showConjugation) {
            if let verb = selectedVerb {
                VerbConjugationView(
                    hiragana: verb.hiragana,
                    kanji: verb.kanji,
                    turkishMeaning: verb.turkishMeaning
                )
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
            }
        }
    }

    // MARK: - Verb Row

    private func verbRow(_ verb: VerbEntry) -> some View {
        HStack(spacing: 14) {
            // Japanese column
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
            .frame(width: 100, alignment: .leading)

            // Meaning column
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

            // Verb type badge
            let vType = VerbConjugationEngine.classify(verb.hiragana)
            verbTypeTag(vType)

            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundStyle(Theme.secondaryInk)
        }
        .padding(14)
        .background(Theme.paper)
        .inkBordered()
    }

    private func verbTypeTag(_ type: VerbType) -> some View {
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
            .padding(.vertical, 4)
            .background(color.opacity(0.13))
            .foregroundStyle(color)
            .clipShape(RoundedRectangle(cornerRadius: 6))
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 40))
                .foregroundStyle(Theme.secondaryInk)
            Text("Sonuç bulunamadı")
                .font(Theme.heading(18))
                .foregroundStyle(Theme.ink)
            Text("'\(searchText)' ile eşleşen fiil yok.")
                .font(.subheadline)
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

            // Verb detection: ends in う/く/ぐ/す/つ/ぬ/ぶ/む/る
            let verbEndings: Set<Character> = ["う","く","ぐ","す","つ","ぬ","ぶ","む","る"]
            let entries: [VerbEntry] = raw.compactMap { dict -> VerbEntry? in
                guard
                    let hira = dict["hiragana"], !hira.isEmpty,
                    let last = hira.last, verbEndings.contains(last),
                    let meaning = dict["turkishMeaning"],
                    let romaji = dict["romaji"]
                else { return nil }

                // Filter out obvious non-verbs by meaning clues
                let nonVerbHints = ["(ünlem)", "(bağlaç)", "(edat)", "(sıfat)", "(zamir)", "(önek)", "(sonek)"]
                if nonVerbHints.contains(where: { meaning.contains($0) }) { return nil }

                let kanji = dict["kanji"].flatMap { $0.isEmpty ? nil : $0 }
                return VerbEntry(
                    id: hira,
                    hiragana: hira,
                    kanji: kanji,
                    romaji: romaji,
                    turkishMeaning: meaning
                )
            }
            .sorted { $0.hiragana < $1.hiragana }
            // Deduplicate
            var seen = Set<String>()
            let deduped = entries.filter { seen.insert($0.hiragana).inserted }

            DispatchQueue.main.async {
                self.verbs = deduped
            }
        }
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        VerbLibraryView()
    }
}
