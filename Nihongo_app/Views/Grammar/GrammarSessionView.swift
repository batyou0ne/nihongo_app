import SwiftUI
import SwiftData

/// Otomatik Gramer Oturumu.
/// Seçilen seviyedeki öğrenilmemiş gramer konularından 3 tanesini seçer.
/// Sırayla konuların derslerini gösterir, ardından üçü için toplu pratik yaptırır.
struct GrammarSessionView: View {
    let level: String

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Environment(TabBarManager.self) private var tabBarManager

    @State private var sessionPoints: [GrammarPoint] = []
    @State private var currentLessonIndex = 0
    @State private var isPracticing = false
    @State private var isLoading = true
    @State private var isAllLearned = false

    var body: some View {
        Group {
            if isLoading {
                ProgressView()
            } else if isAllLearned {
                allLearnedView
            } else if isPracticing {
                GrammarPracticeView(points: sessionPoints, title: "Pratik", onFinish: {
                    dismiss()
                })
            } else if sessionPoints.indices.contains(currentLessonIndex) {
                GrammarLessonView(
                    point: sessionPoints[currentLessonIndex],
                    isSessionMode: true,
                    onNext: advanceLesson
                )
            }
        }
        .onAppear {
            tabBarManager.isHidden = true
            loadSession()
        }
        .onDisappear {
            tabBarManager.isHidden = false
        }
        .navigationBarTitleDisplayMode(.inline)
    }

    private func loadSession() {
        guard isLoading else { return }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            // 1. Tüm gramer noktalarını yükle
            let allPoints = ContentStore.loadGrammar(level: level)
            
            // 2. Öğrenilmiş olanları bul
            let allProgress = (try? modelContext.fetch(FetchDescriptor<LearningItemProgress>())) ?? []
            let learnedIDs = Set(allProgress.filter { $0.itemKind == .grammar && $0.repetitionCount >= 1 }.map(\.itemID))
            
            // 3. Öğrenilmemişleri filtrele
            let unlearnedPoints = allPoints.filter { !learnedIDs.contains($0.id) }
            
            if unlearnedPoints.isEmpty {
                isAllLearned = true
            } else {
                // En fazla 3 konu al
                sessionPoints = Array(unlearnedPoints.prefix(3))
            }
            isLoading = false
        }
    }

    private func advanceLesson() {
        if currentLessonIndex + 1 < sessionPoints.count {
            withAnimation {
                currentLessonIndex += 1
            }
        } else {
            withAnimation {
                isPracticing = true
            }
        }
    }

    private var allLearnedView: some View {
        VStack(spacing: 20) {
            Image(systemName: "party.popper.fill")
                .font(.system(size: 60))
                .foregroundStyle(Theme.accent)
            Text("Harika!")
                .font(Theme.display(32))
                .foregroundStyle(Theme.ink)
            Text("\(level) seviyesindeki tüm gramer konularını bitirdin.")
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

