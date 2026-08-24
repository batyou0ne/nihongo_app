import SwiftUI
import SwiftData

/// İlerleme ekranı. Struct adı bilerek `ProgressOverviewView` — SwiftUI'nin kendi
/// `ProgressView` (spinner) tipiyle isim çakışmasını önlemek için. Dosya adı klasör
/// yapısındaki isimlendirmeyle tutarlı kalsın diye ProgressView.swift olarak bırakıldı.
struct ProgressOverviewView: View {
    @Query private var allProgress: [LearningItemProgress]
    @Query private var userProgressRecords: [UserProgress]

    private var hiraganaLearned: Int {
        allProgress.filter { $0.itemKind == .hiraganaCharacter && $0.isLearned }.count
    }
    private var katakanaLearned: Int {
        allProgress.filter { $0.itemKind == .katakanaCharacter && $0.isLearned }.count
    }
    private var kanjiLearned: Int {
        allProgress.filter { $0.itemKind == .kanji && $0.isLearned }.count
    }

    var body: some View {
        List {
            if let userProgress = userProgressRecords.first {
                Section("Seri") {
                    HStack {
                        Image(systemName: "flame.fill")
                            .foregroundStyle(.orange)
                        Text("\(userProgress.currentStreak) günlük seri")
                        Spacer()
                        Text("En uzun: \(userProgress.longestStreak)")
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Section("Öğrenilenler") {
                progressRow(title: "Hiragana", learned: hiraganaLearned, total: 46, color: .red)
                progressRow(title: "Katakana", learned: katakanaLearned, total: 46, color: .blue)
                progressRow(title: "Kanji (N5)", learned: kanjiLearned, total: 80, color: .green)
            }
        }
        .navigationTitle("İlerleme")
    }

    private func progressRow(title: String, learned: Int, total: Int, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title)
                Spacer()
                Text("\(learned)/\(total)")
                    .foregroundStyle(.secondary)
            }
            SwiftUI.ProgressView(value: Double(learned), total: Double(total))
                .tint(color)
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    NavigationStack {
        ProgressOverviewView()
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
