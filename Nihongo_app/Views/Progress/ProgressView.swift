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
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                if let userProgress = userProgressRecords.first {
                    streakSection(userProgress)
                }

                VStack(alignment: .leading, spacing: 14) {
                    sectionTitle("Öğrenilenler")
                    progressRow(title: "Hiragana", learned: hiraganaLearned, total: 46)
                    progressRow(title: "Katakana", learned: katakanaLearned, total: 46)
                    progressRow(title: "Kanji (N5)", learned: kanjiLearned, total: 80)
                }
            }
            .padding(20)
        }
        .background(Theme.paper)
        .navigationTitle("İlerleme")
    }

    private func sectionTitle(_ title: String) -> some View {
        Text(title)
            .font(Theme.display(24))
            .foregroundStyle(Theme.ink)
    }

    private func streakSection(_ userProgress: UserProgress) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            sectionTitle("Seri")

            HStack(spacing: 10) {
                Image(systemName: "flame.fill")
                    .foregroundStyle(Theme.accent)
                Text("\(userProgress.currentStreak) günlük seri")
                    .font(.system(size: 17, weight: .heavy))
                    .foregroundStyle(Theme.ink)
                Spacer()
                Text("En uzun: \(userProgress.longestStreak)")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
            }
            .padding()
            .inkBordered()
        }
    }

    private func progressRow(title: String, learned: Int, total: Int) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(title)
                    .font(Theme.heading(17))
                    .foregroundStyle(Theme.ink)
                Spacer()
                Text("\(learned)/\(total)")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Theme.secondaryInk)
            }

            progressBar(fraction: total > 0 ? Double(learned) / Double(total) : 0)
        }
        .padding()
        .inkBordered()
    }

    /// Keskin köşeli, siyah kenarlıklı ilerleme çubuğu — dolu kısım vermilyon.
    private func progressBar(fraction: Double) -> some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Rectangle()
                    .fill(Theme.paper)
                Rectangle()
                    .fill(Theme.accent)
                    .frame(width: geometry.size.width * min(max(fraction, 0), 1))
            }
            .overlay(Rectangle().strokeBorder(Theme.ink, lineWidth: 2))
        }
        .frame(height: 16)
    }
}

#Preview {
    NavigationStack {
        ProgressOverviewView()
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
