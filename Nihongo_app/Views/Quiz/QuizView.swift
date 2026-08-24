import SwiftUI
import SwiftData

/// Çoktan seçmeli quiz ekranı. `Item` hem JapaneseCharacter hem Kanji olabilir
/// (bkz. QuizViewModel.swift'teki QuizItem protokolü) — modül fark etmeksizin aynı UI.
struct QuizView<Item: QuizItem>: View {
    let itemKind: LearnableItemKind
    let accentColor: Color

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query private var userProgressRecords: [UserProgress]
    @State private var viewModel: QuizViewModel<Item>?
    @State private var hasRecordedStreak = false

    private let questions: [Item]
    private let progressLookup: (String) -> LearningItemProgress?

    init(
        questions: [Item],
        itemKind: LearnableItemKind,
        accentColor: Color,
        progressLookup: @escaping (String) -> LearningItemProgress?
    ) {
        self.questions = questions
        self.itemKind = itemKind
        self.accentColor = accentColor
        self.progressLookup = progressLookup
    }

    var body: some View {
        Group {
            if let viewModel {
                if viewModel.isFinished {
                    resultView(score: viewModel.score, total: viewModel.questions.count)
                        .onAppear { recordStreakIfNeeded() }
                } else {
                    quizContent(viewModel)
                }
            } else {
                SwiftUI.ProgressView()
            }
        }
        .navigationTitle("Quiz")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Kapat") { dismiss() }
            }
        }
        .onAppear {
            if viewModel == nil {
                viewModel = QuizViewModel(
                    questions: questions,
                    itemKind: itemKind,
                    modelContext: modelContext,
                    progressLookup: progressLookup
                )
            }
        }
    }

    @ViewBuilder
    private func quizContent(_ viewModel: QuizViewModel<Item>) -> some View {
        VStack(spacing: 24) {
            SwiftUI.ProgressView(value: viewModel.progressFraction)
                .tint(accentColor)
                .padding(.horizontal)

            if let question = viewModel.currentQuestion {
                Text(question.prompt)
                    .font(.system(size: 88, weight: .regular, design: .serif))
                    .padding(.top, 24)

                VStack(spacing: 12) {
                    ForEach(viewModel.options, id: \.self) { option in
                        optionButton(option, viewModel: viewModel)
                    }
                }
                .padding(.horizontal)

                if viewModel.selectedAnswer != nil {
                    Button("Devam Et") {
                        withAnimation { viewModel.moveToNext() }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(accentColor)
                    .padding(.top, 8)
                }
            }

            Spacer()
        }
        .padding(.top)
    }

    private func optionButton(_ option: String, viewModel: QuizViewModel<Item>) -> some View {
        let isSelected = viewModel.selectedAnswer == option
        let isCorrectAnswer = option == viewModel.currentQuestion?.correctAnswer
        let showResult = viewModel.selectedAnswer != nil

        return Button {
            withAnimation(.easeInOut(duration: 0.2)) {
                viewModel.submitAnswer(option)
            }
        } label: {
            HStack {
                Text(option)
                Spacer()
                if showResult && isCorrectAnswer {
                    Image(systemName: "checkmark.circle.fill")
                } else if showResult && isSelected {
                    Image(systemName: "xmark.circle.fill")
                }
            }
            .padding()
            .background(optionBackground(isSelected: isSelected, isCorrectAnswer: isCorrectAnswer, showResult: showResult))
            .foregroundStyle(.primary)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .disabled(showResult)
    }

    private func optionBackground(isSelected: Bool, isCorrectAnswer: Bool, showResult: Bool) -> Color {
        guard showResult else { return Color(uiColor: .secondarySystemGroupedBackground) }
        if isCorrectAnswer { return .green.opacity(0.25) }
        if isSelected { return .red.opacity(0.25) }
        return Color(uiColor: .secondarySystemGroupedBackground)
    }

    private func recordStreakIfNeeded() {
        guard !hasRecordedStreak else { return }
        hasRecordedStreak = true

        let userProgress = userProgressRecords.first ?? {
            let newProgress = UserProgress()
            modelContext.insert(newProgress)
            return newProgress
        }()
        userProgress.recordStudySession()
        try? modelContext.save()
    }

    private func resultView(score: Int, total: Int) -> some View {
        VStack(spacing: 16) {
            Image(systemName: score == total ? "star.fill" : "checkmark.seal.fill")
                .font(.system(size: 56))
                .foregroundStyle(accentColor)
            Text("\(score) / \(total) doğru")
                .font(.title2.bold())
            Button("Bitir") { dismiss() }
                .buttonStyle(.borderedProminent)
                .tint(accentColor)
        }
    }
}
