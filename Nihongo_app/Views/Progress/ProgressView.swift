import SwiftUI
import SwiftData
import Charts

/// İlerleme ekranı. Struct adı bilerek `ProgressOverviewView` — SwiftUI'nin kendi
/// `ProgressView` (spinner) tipiyle isim çakışmasını önlemek için. Dosya adı klasör
/// yapısındaki isimlendirmeyle tutarlı kalsın diye ProgressView.swift olarak bırakıldı.
struct ProgressOverviewView: View {
    @Query private var allProgress: [LearningItemProgress]
    @Query private var userProgressRecords: [UserProgress]
    @Query(sort: \DailyActivity.dateString, order: .reverse)
    private var dailyActivities: [DailyActivity]

    /// "Öğrenildi" = son cevabı doğru olan öğe (yanlış cevap sayacı sıfırladığı için
    /// repetitionCount >= 1 bunu garanti eder). SM-2'nin uzun vadeli `isLearned`
    /// bayrağı (4+ doğru tekrar) çubuğun altında ayrıca gösterilir.
    private func learnedCount(_ kind: LearnableItemKind) -> Int {
        allProgress.filter { $0.itemKind == kind && $0.repetitionCount >= 1 }.count
    }

    private func masteredCount(_ kind: LearnableItemKind) -> Int {
        allProgress.filter { $0.itemKind == kind && $0.isLearned }.count
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                if let userProgress = userProgressRecords.first {
                    streakSection(userProgress)
                    xpBarSection(userProgress)
                    timeChartSection()
                }

                VStack(alignment: .leading, spacing: 14) {
                    sectionTitle("Öğrenilenler")
                    progressRow(title: "Hiragana", kind: .hiraganaCharacter)
                    progressRow(title: "Katakana", kind: .katakanaCharacter)
                    progressRow(title: "Kanji (N5)", kind: .kanji)
                    progressRow(title: "Kelimeler (N5)", kind: .vocabularyWord)
                    progressRow(title: "Gramer (N5)", kind: .grammar)
                }
            }
            .padding(20)
        }
        .scrollIndicators(.hidden)
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
                Text("\(userProgress.activeStreak) günlük seri")
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
    
    private struct DailyTimeData: Identifiable {
        let id = UUID()
        let date: Date
        let minutes: Int
    }
    
    private func xpBarSection(_ userProgress: UserProgress) -> some View {
        let total = userProgress.totalXP
        let level = (total / 500) + 1
        let currentXP = total % 500
        let nextXP = 500
        
        return VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .bottom) {
                Text("Seviye \(level)")
                    .font(Theme.display(28))
                    .foregroundStyle(Theme.ink)
                Spacer()
                Text("\(currentXP) / \(nextXP) XP")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Theme.secondaryInk)
            }
            
            progressBar(fraction: Double(currentXP) / Double(nextXP))
        }
        .padding()
        .inkBordered()
    }
    
    private func timeChartSection() -> some View {
        let calendar = Calendar.current
        var last7Days: [Date] = []
        let today = calendar.startOfDay(for: .now)
        for i in (0..<7).reversed() {
            if let d = calendar.date(byAdding: .day, value: -i, to: today) {
                last7Days.append(d)
            }
        }
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        
        let data: [DailyTimeData] = last7Days.map { date in
            let dateStr = formatter.string(from: date)
            let activity = dailyActivities.first(where: { $0.dateString == dateStr })
            let minutes = (activity?.timeSpentSeconds ?? 0) / 60
            return DailyTimeData(date: date, minutes: minutes)
        }
        
        let totalMinutesThisWeek = data.reduce(0) { $0 + $1.minutes }
        let hours = totalMinutesThisWeek / 60
        let mins = totalMinutesThisWeek % 60
        let timeStr = hours > 0 ? "\(hours) sa \(mins) dk" : "\(mins) dk"
        
        return VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline) {
                sectionTitle("Çalışma Süresi")
                Spacer()
                Text(timeStr)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Theme.accent)
            }
            
            Chart(data) { item in
                BarMark(
                    x: .value("Gün", item.date, unit: .day),
                    y: .value("Dakika", item.minutes)
                )
                .foregroundStyle(Theme.accent.gradient)
            }
            .chartXAxis {
                AxisMarks(values: .stride(by: .day)) { _ in
                    AxisValueLabel(format: .dateTime.weekday(.abbreviated))
                }
            }
            .frame(height: 180)
            .padding()
            .inkBordered()
        }
    }

    private func progressRow(title: String, kind: LearnableItemKind) -> some View {
        let total = kind.totalCount
        let learned = learnedCount(kind)
        let mastered = masteredCount(kind)

        return VStack(alignment: .leading, spacing: 10) {
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

            if mastered > 0 {
                Text("🏆 \(mastered) tanesi kalıcı öğrenildi (4+ doğru tekrar)")
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryInk)
            }
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
