import SwiftUI
import SwiftData

/// Otomatik Kelime Oturumu.
/// Seçilen seviyedeki öğrenilmemiş kelimelerden belli bir miktar (örn. 20) seçer
/// ve FlashcardSessionView'a aktarır. Part seçimine gerek kalmadan sürekli
/// akış sağlar.
struct VocabularySessionView: View {
    let level: String
    
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    @State private var sessionWords: [VocabularyWord] = []
    @State private var allWords: [VocabularyWord] = []
    @State private var isLoading = true
    @State private var isAllLearned = false
    
    var body: some View {
        Group {
            if isLoading {
                ProgressView()
            } else if isAllLearned {
                allLearnedView
            } else if !sessionWords.isEmpty {
                FlashcardSessionView(
                    sessionKey: "vocab_\(level)_auto",
                    itemKind: .vocabularyWord,
                    allItems: sessionWords,
                    distractorPool: allWords,
                    accentColor: Theme.accent,
                    title: "\(level) Kelimeler"
                )
            }
        }
        .onAppear(perform: loadSession)
        .navigationBarTitleDisplayMode(.inline)
    }
    
    private func loadSession() {
        guard isLoading else { return }
        
        // 1. Tüm kelimeleri yükle
        guard let url = Bundle.main.url(forResource: "\(level)VocabularyData", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let loaded = try? JSONDecoder().decode([VocabularyWord].self, from: data) else {
            isLoading = false
            return
        }
        allWords = loaded
        
        // 2. Öğrenilmiş olanları bul
        let descriptor = FetchDescriptor<LearningItemProgress>()
        let allProgress = (try? modelContext.fetch(descriptor)) ?? []
        let learnedIDs = Set(allProgress.filter { $0.itemKind == .vocabularyWord && $0.repetitionCount >= 1 }.map(\.itemID))
        
        // 3. Öğrenilmemişleri filtrele
        let unlearnedWords = allWords.filter { !learnedIDs.contains($0.id) }
        
        if unlearnedWords.isEmpty {
            isAllLearned = true
        } else {
            // En fazla 20 kelime al
            sessionWords = Array(unlearnedWords.prefix(20))
        }
        isLoading = false
    }
    
    private var allLearnedView: some View {
        VStack(spacing: 20) {
            Image(systemName: "party.popper.fill")
                .font(.system(size: 60))
                .foregroundStyle(Theme.accent)
            Text("Harika!")
                .font(Theme.display(32))
                .foregroundStyle(Theme.ink)
            Text("\(level) seviyesindeki tüm kelimeleri bitirdin.")
                .font(Theme.heading(18))
                .foregroundStyle(Theme.secondaryInk)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button("Geri Dön") {
                dismiss()
            }
            .buttonStyle(PrimaryButtonStyle())
            .padding(.top, 20)
            .padding(.horizontal)
        }
    }
}

#Preview {
    NavigationStack {
        VocabularySessionView(level: "N5")
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
