import SwiftUI
import SwiftData

/// JLPT Seviye Kapı Sınavı (Gateway Exam) ve Test-Out Sınavı ekranı.
/// Test-Out modunda 3 hata hakkı (can barı) uygulanır; Kapı Sınavı modunda %80 başarı aranır.
struct GatewayExamView: View {
    let level: JLPTLevel
    let mode: ExamMode
    var onSuccess: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @Query private var userProgressRecords: [UserProgress]

    @State private var questions: [ExamQuestion] = []
    @State private var currentIndex: Int = 0
    @State private var hearts: Int = 3
    @State private var correctAnswersCount: Int = 0
    @State private var selectedOption: String?
    @State private var isAnswerCorrect: Bool?
    @State private var isFailed: Bool = false
    @State private var isPassed: Bool = false
    @State private var showLessonComplete: Bool = false
    @State private var autoAdvanceTask: Task<Void, Never>?

    private var currentQuestion: ExamQuestion? {
        questions.indices.contains(currentIndex) ? questions[currentIndex] : nil
    }

    private var progressFraction: Double {
        questions.isEmpty ? 0 : Double(currentIndex) / Double(questions.count)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Theme.paper.ignoresSafeArea()

                if questions.isEmpty {
                    SwiftUI.ProgressView()
                } else if isFailed {
                    failedView
                } else if isPassed {
                    passedSummaryView
                } else if let question = currentQuestion {
                    examContent(question)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(Theme.ink)
                    }
                }

                ToolbarItem(placement: .principal) {
                    if let question = currentQuestion {
                        HStack(spacing: 6) {
                            Image(systemName: question.type.badgeIcon)
                                .font(.caption)
                            Text(question.type.rawValue)
                                .font(.system(size: 13, weight: .heavy))
                        }
                        .foregroundStyle(Theme.accent)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Theme.accent.opacity(0.12))
                        .clipShape(Capsule())
                    }
                }

                ToolbarItem(placement: .primaryAction) {
                    if mode == .testOut {
                        HStack(spacing: 4) {
                            ForEach(0..<3) { i in
                                Image(systemName: i < hearts ? "heart.fill" : "heart")
                                    .foregroundStyle(i < hearts ? .red : Theme.secondaryInk)
                                    .font(.system(size: 16))
                            }
                        }
                    } else {
                        Text("\(currentIndex + 1) / \(questions.count)")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(Theme.secondaryInk)
                    }
                }
            }
            .fullScreenCover(isPresented: $showLessonComplete) {
                LessonCompleteView(
                    xpGained: mode == .testOut ? 350 : 250,
                    streak: userProgressRecords.first?.activeStreak ?? 1,
                    streakIncreased: false,
                    title: "\(level.rawValue) Seviyesi Açıldı!",
                    ctaTitle: "Devam Et",
                    ringColor: Theme.accent
                ) {
                    showLessonComplete = false
                    onSuccess?()
                    dismiss()
                }
            }
            .onAppear {
                if questions.isEmpty {
                    questions = ExamGenerator.generateExam(for: level, mode: mode)
                }
            }
        }
    }

    // MARK: - Soru Ekranı

    private func examContent(_ question: ExamQuestion) -> some View {
        VStack(spacing: 24) {
            // İlerleme Çubuğu
            ProgressBar(fraction: progressFraction)
                .padding(.horizontal, 20)

            Spacer()

            // Soru Kartı
            VStack(spacing: 12) {
                if let sub = question.subPrompt {
                    Text(sub)
                        .font(.subheadline)
                        .foregroundStyle(Theme.secondaryInk)
                        .multilineTextAlignment(.center)
                }

                Text(question.prompt)
                    .font(Theme.display(question.prompt.count > 15 ? 24 : 44))
                    .foregroundStyle(Theme.ink)
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.6)
                    .lineLimit(3)
                    .padding(.horizontal)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28)
            .background(Theme.paper)
            .inkBordered(lineWidth: 2)
            .padding(.horizontal, 20)

            Spacer()

            // Şıklar
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(question.options, id: \.self) { option in
                    optionButton(option, question: question)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 24)
        }
    }

    private func optionButton(_ option: String, question: ExamQuestion) -> some View {
        let isSelected = selectedOption == option
        let isCorrectChoice = option == question.correctAnswer
        let showResult = selectedOption != nil

        var bgColor = Theme.paper
        var fgColor = Theme.ink

        if showResult {
            if isCorrectChoice {
                bgColor = .green
                fgColor = .white
            } else if isSelected {
                bgColor = Theme.accent
                fgColor = .white
            }
        }

        return Button {
            submitAnswer(option, question: question)
        } label: {
            Text(option)
                .font(.system(size: 16, weight: .bold))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .minimumScaleFactor(0.7)
                .padding(.horizontal, 8)
                .frame(maxWidth: .infinity)
                .frame(height: 64)
                .background(bgColor)
                .foregroundStyle(fgColor)
                .overlay(Rectangle().strokeBorder(Theme.ink, lineWidth: 2))
        }
        .disabled(selectedOption != nil)
    }

    private func submitAnswer(_ option: String, question: ExamQuestion) {
        selectedOption = option
        let isCorrect = (option == question.correctAnswer)
        isAnswerCorrect = isCorrect

        if isCorrect {
            correctAnswersCount += 1
            FeedbackManager.shared.playSuccess()
            AudioService.shared.playCorrectSound()
        } else {
            FeedbackManager.shared.playError()
            AudioService.shared.playWrongSound()

            if mode == .testOut {
                withAnimation {
                    hearts = max(0, hearts - 1)
                }
            }
        }

        autoAdvanceTask = Task {
            try? await Task.sleep(for: .seconds(1.2))
            guard !Task.isCancelled else { return }

            if mode == .testOut && hearts == 0 {
                withAnimation { isFailed = true }
                return
            }

            if currentIndex + 1 >= questions.count {
                handleExamFinish()
            } else {
                withAnimation {
                    currentIndex += 1
                    selectedOption = nil
                    isAnswerCorrect = nil
                }
            }
        }
    }

    private func handleExamFinish() {
        let total = max(1, questions.count)
        let ratio = Double(correctAnswersCount) / Double(total)

        let passed: Bool
        if mode == .testOut {
            passed = hearts > 0
        } else {
            passed = ratio >= 0.80
        }

        if passed {
            // Sonraki seviyenin kilidini aç
            LevelProgressionService.shared.unlockNextLevel(
                afterCompleting: level,
                score: ratio,
                isTestOut: (mode == .testOut),
                context: modelContext
            )

            // XP Ödülü
            let xp = (mode == .testOut ? 350 : 250)
            userProgressRecords.first?.totalXP += xp
            try? modelContext.save()

            showLessonComplete = true
        } else {
            withAnimation { isFailed = true }
        }
    }

    // MARK: - Başarısız Ekranı

    private var failedView: some View {
        VStack(spacing: 24) {
            Spacer()

            ZStack {
                Circle()
                    .fill(Color.red.opacity(0.12))
                    .frame(width: 90, height: 90)

                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(.red)
            }

            Text("Sınav Tamamlanamadı")
                .font(Theme.display(28))
                .foregroundStyle(Theme.ink)

            if mode == .testOut {
                Text("3 hata sınırına ulaştın. Eksik olduğun konuları çalışıp tekrar deneyebilirsin.")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            } else {
                Text("%80 başarı barajına ulaşılamadı. (Doğru: \(correctAnswersCount) / \(questions.count))\nBiraz daha pratik yaparak tekrar deneyebilirsin.")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }

            Spacer()

            Button {
                dismiss()
            } label: {
                Text("Derslere Geri Dön")
                    .font(Theme.heading(16))
                    .foregroundStyle(Theme.paper)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Theme.ink)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
    }

    private var passedSummaryView: some View {
        VStack(spacing: 20) {
            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 60))
                .foregroundStyle(.green)

            Text("Tebrikler!")
                .font(Theme.display(32))
                .foregroundStyle(Theme.ink)

            Text("\(level.rawValue) sınavını başarıyla tamamladın.")
                .font(Theme.heading(18))
                .foregroundStyle(Theme.secondaryInk)

            Button("Devam Et") {
                dismiss()
            }
            .buttonStyle(PrimaryButtonStyle())
            .padding(.top, 20)
        }
    }
}

#Preview {
    GatewayExamView(level: .n5, mode: .testOut)
        .modelContainer(for: [UserLevelProgress.self, LearningItemProgress.self, UserProgress.self], inMemory: true)
}
