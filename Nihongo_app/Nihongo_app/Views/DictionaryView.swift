import SwiftUI

enum DictionaryTab: String, CaseIterable {
    case vocabulary = "Kelime Sözlüğü"
    case grammar = "Dilbilgisi Kütüphanesi"
}

struct DictionaryView: View {
    @State private var allWords: [VocabularyWord] = []
    @State private var searchText = ""
    @State private var selectedTab: DictionaryTab = .vocabulary

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
            VStack(spacing: 0) {
                Picker("", selection: $selectedTab) {
                    ForEach(DictionaryTab.allCases, id: \.self) { tab in
                        Text(tab.rawValue).tag(tab)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(Theme.paper)

                if selectedTab == .vocabulary {
                    vocabularyContent
                } else {
                    GrammarReferenceView()
                }
            }
            .background(Theme.paper)
            .navigationTitle(selectedTab == .vocabulary ? L10n.dictionaryTitle : "Dilbilgisi Kütüphanesi")
            .navigationBarTitleDisplayMode(.large)
        }
        .onAppear {
            if allWords.isEmpty {
                allWords = ContentStore.loadVocabulary(level: "N5")
            }
        }
    }

    private var vocabularyContent: some View {
        VStack(spacing: 0) {
            // Custom Search Bar
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Theme.secondaryInk)
                
                TextField(L10n.dictionarySearch, text: $searchText)
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
            .padding(.bottom, 10)

            List(filteredWords) { word in
                HStack(alignment: .top) {
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
                    
                    Spacer()
                    
                    Button {
                        AudioService.shared.speak(word.speechText)
                    } label: {
                        Image(systemName: "speaker.wave.2.circle.fill")
                            .font(.title2)
                            .foregroundStyle(Theme.accent)
                    }
                }
                .padding(.vertical, 8)
                .listRowBackground(Theme.paper)
                .listRowSeparatorTint(Theme.secondaryInk.opacity(0.3))
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            // Tab bar boşluğu
            .safeAreaInset(edge: .bottom) {
                Color.clear.frame(height: 80)
            }
        }
    }
}

#Preview {
    DictionaryView()
}
