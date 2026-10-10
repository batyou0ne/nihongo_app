import SwiftUI
import SwiftData

/// Hiragana/Katakana öğrenme ekranı — karakterleri JSON'dan yükler, gerçek flashcard
/// akışını (tek tek gelen kartlar, 4 şık, oturum devamlılığı) FlashcardSessionView'a bırakır.
struct LearningView: View {
    let characterType: CharacterType
    @Environment(TabBarManager.self) private var tabBarManager

    @State private var characters: [JapaneseCharacter] = []

    init(characterType: CharacterType) {
        self.characterType = characterType
        _characters = State(initialValue: ContentStore.loadCharacters(characterType))
    }

    private var accentColor: Color {
        Theme.accent
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
            tabBarManager.isHidden = true
            if characters.isEmpty {
                characters = ContentStore.loadCharacters(characterType)
            }
        }
        .onDisappear {
            tabBarManager.isHidden = false
        }
    }
}

#Preview {
    NavigationStack {
        LearningView(characterType: .hiragana)
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
