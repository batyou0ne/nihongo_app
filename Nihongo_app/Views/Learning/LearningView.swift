import SwiftUI
import SwiftData

/// Hiragana/Katakana öğrenme ekranı — karakterleri JSON'dan yükler, gerçek flashcard
/// akışını (tek tek gelen kartlar, 4 şık, oturum devamlılığı) FlashcardSessionView'a bırakır.
struct LearningView: View {
    let characterType: CharacterType

    @State private var characters: [JapaneseCharacter] = []

    private var accentColor: Color {
        characterType == .hiragana ? .red : .blue
    }

    var body: some View {
        Group {
            if characters.isEmpty {
                SwiftUI.ProgressView()
            } else {
                FlashcardSessionView(
                    sessionKey: characterType.rawValue,
                    itemKind: characterType == .hiragana ? .hiraganaCharacter : .katakanaCharacter,
                    allItems: characters,
                    distractorPool: characters,
                    accentColor: accentColor,
                    title: characterType.displayName
                )
            }
        }
        .onAppear {
            guard characters.isEmpty else { return }
            let fileName = characterType == .hiragana ? "HiraganaData" : "KatakanaData"
            guard let url = Bundle.main.url(forResource: fileName, withExtension: "json"),
                  let data = try? Data(contentsOf: url),
                  var loaded = try? JSONDecoder().decode([JapaneseCharacter].self, from: data) else {
                return
            }
            for index in loaded.indices { loaded[index].type = characterType }
            characters = loaded
        }
    }
}

#Preview {
    NavigationStack {
        LearningView(characterType: .hiragana)
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
