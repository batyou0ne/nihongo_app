import SwiftUI

struct DictionaryView: View {
    @State private var allWords: [VocabularyWord] = []
    @State private var searchText = ""

    var filteredWords: [VocabularyWord] {
        if searchText.isEmpty {
            return allWords
        } else {
            return allWords.filter { word in
                word.turkishMeaning.localizedCaseInsensitiveContains(searchText) ||
                word.kanji.contains(searchText) ||
                word.hiragana.contains(searchText) ||
                word.romaji.localizedCaseInsensitiveContains(searchText)
            }
        }
    }

    var body: some View {
        NavigationStack {
            List(filteredWords) { word in
                VStack(alignment: .leading, spacing: 6) {
                    HStack(alignment: .firstTextBaseline) {
                        Text(word.displayText)
                            .font(Theme.heading(20))
                            .foregroundStyle(Theme.ink)
                        if !word.kanji.isEmpty {
                            Text(word.hiragana)
                                .font(.subheadline)
                                .foregroundStyle(Theme.secondaryInk)
                        }
                    }
                    Text(word.turkishMeaning)
                        .font(.body)
                        .foregroundStyle(Theme.ink)
                }
                .padding(.vertical, 8)
                .listRowBackground(Theme.paper)
                .listRowSeparatorTint(Theme.secondaryInk.opacity(0.3))
            }
            .listStyle(.plain)
            .background(Theme.paper)
            .scrollContentBackground(.hidden)
            .navigationTitle("Sözlük")
            .searchable(text: $searchText, prompt: "Kelime veya anlam ara...")
            // Tab bar boşluğu
            .safeAreaInset(edge: .bottom) {
                Color.clear.frame(height: 80)
            }
        }
        .onAppear {
            if allWords.isEmpty {
                allWords = ContentStore.loadVocabulary(level: "N5")
            }
        }
    }
}

#Preview {
    DictionaryView()
}
