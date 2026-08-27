import SwiftUI
import SwiftData

/// Bir veya birden çok gramer konusunun pratiğini art arda çalıştırır.
/// - Ders ekranından tek konuyla açılır (`points: [point]`).
/// - "Tekrar Çalış" ekranından yanlış yapılan konuların listesiyle açılır.
///
/// Her konunun soruları bitince o konu için `SpacedRepetitionService` üzerinden
/// ilerleme kaydedilir (hepsi doğruysa `.good`, en az bir yanlış varsa `.again`),
/// yanlış varsa konu `needsReview` işaretlenir. Diğer modüllerle aynı
/// `LearningItemProgress` tablosu kullanılır (`itemKind == .grammar`).
struct GrammarPracticeView: View {
    let points: [GrammarPoint]
    let title: String

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query private var userProgressRecords: [UserProgress]

    /// wordOrder sorusundaki kelime kartı — aynı metinli iki kart olabildiği için
    /// (ör. iki tane は) kimlik UUID'den gelir.
    private struct Token: Identifiable, Equatable {
        let id = UUID()
        let text: String
    }

    private struct WrongEntry: Identifiable {
        let id = UUID()
        let pointTitle: String
        let prompt: String
        let answer: String
    }

    @State private var progressByID: [String: LearningItemProgress] = [:]
    @State private var didSetup = false

    @State private var pointIndex = 0
    @State private var questionIndex = 0

    // Çoktan seçmeli / boşluk doldurma durumu
    @State private var selectedChoice: String?
    @State private var isChoiceLocked = false

    // Kelime sıralama durumu
    @State private var orderAssembled: [Token] = []
    @State private var orderPool: [Token] = []
    @State private var isOrderChecked = false
    @State private var isOrderCorrect = false

    // Sayaçlar
    @State private var pointWrongCount = 0
    @State private var totalAnswered = 0
    @State private var totalWrong = 0
    @State private var wrongSummary: [WrongEntry] = []

    @State private var isFinished = false
    @State private var hasRecordedStreak = false

    private var currentPoint: GrammarPoint? {
        points.indices.contains(pointIndex) ? points[pointIndex] : nil
    }

    private var currentQuestion: GrammarQuestion? {
        guard let point = currentPoint, point.questions.indices.contains(questionIndex) else { return nil }
        return point.questions[questionIndex]
    }

    private var totalQuestions: Int {
        points.reduce(0) { $0 + $1.questions.count }
    }

    private var answeredFraction: Double {
        totalQuestions == 0 ? 0 : Double(totalAnswered) / Double(totalQuestions)
    }

    private var hasAnswered: Bool {
        isChoiceLocked || isOrderChecked
    }

    var body: some View {
        Group {
            if isFinished {
                summaryView
            } else if let point = currentPoint, let question = currentQuestion {
                practiceContent(point, question)
            } else {
                SwiftUI.ProgressView()
            }
        }
        .background(Theme.paper)
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: setup)
    }

    // MARK: - Kurulum

    private func setup() {
        guard !didSetup else { return }
        didSetup = true
        syncProgress()
        resetQuestionState()
    }

    /// Konuların `LearningItemProgress` kayıtlarını çeker, yoksa oluşturur.
    /// #Predicate ile enum karşılaştırması SwiftData'da desteklenmediğinden tür filtresi
    /// bellekte yapılır (bkz. FlashcardSessionView.syncProgress).
    private func syncProgress() {
        let all = (try? modelContext.fetch(FetchDescriptor<LearningItemProgress>())) ?? []
        var byID: [String: LearningItemProgress] = [:]
        for progress in all where progress.itemKind == .grammar && byID[progress.itemID] == nil {
            byID[progress.itemID] = progress
        }
        for point in points where byID[point.id] == nil {
            let newProgress = LearningItemProgress(itemID: point.id, itemKind: .grammar)
            modelContext.insert(newProgress)
            byID[point.id] = newProgress
        }
        try? modelContext.save()
        progressByID = byID
    }

    private func resetQuestionState() {
        selectedChoice = nil
        isChoiceLocked = false
        isOrderChecked = false
        isOrderCorrect = false
        orderAssembled = []
        orderPool = []
        if let question = currentQuestion, question.kind == .wordOrder {
            orderPool = question.choices.shuffled().map { Token(text: $0) }
        }
    }

    // MARK: - Cevap akışı

    private func registerAnswer(correct: Bool) {
        totalAnswered += 1
        guard !correct, let point = currentPoint, let question = currentQuestion else { return }
        pointWrongCount += 1
        totalWrong += 1
        wrongSummary.append(WrongEntry(pointTitle: point.title, prompt: question.prompt, answer: question.answer))
    }

    private func advance() {
        guard let point = currentPoint else { return }

        if questionIndex + 1 < point.questions.count {
            questionIndex += 1
            withAnimation(.easeInOut(duration: 0.2)) { resetQuestionState() }
            return
        }

        // Konu bitti → SR kaydı
        finalizePoint(point)

        if pointIndex + 1 < points.count {
            pointIndex += 1
            questionIndex = 0
            pointWrongCount = 0
            withAnimation(.easeInOut(duration: 0.2)) { resetQuestionState() }
        } else {
            isFinished = true
            recordStreakIfNeeded()
        }
    }

    private func finalizePoint(_ point: GrammarPoint) {
        guard let progress = progressByID[point.id] else { return }
        SpacedRepetitionService.schedule(progress, quality: pointWrongCount == 0 ? .good : .again)
        if pointWrongCount > 0 {
            progress.needsReview = true
        }
        try? modelContext.save()
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

    // MARK: - Pratik ekranı

    @ViewBuilder
    private func practiceContent(_ point: GrammarPoint, _ question: GrammarQuestion) -> some View {
        VStack(spacing: 20) {
            VStack(spacing: 4) {
                SwiftUI.ProgressView(value: answeredFraction)
                    .tint(Theme.accent)
                if points.count > 1 {
                    Text("Konu \(pointIndex + 1)/\(points.count)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal)

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Text(point.pattern)
                        .font(Theme.heading(18))
                        .foregroundStyle(Theme.accent)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)

                    switch question.kind {
                    case .fillBlank, .multipleChoice:
                        choiceQuestion(question)
                    case .wordOrder:
                        wordOrderQuestion(question)
                    }
                }
                .padding(.horizontal)
                .padding(.top, 4)
            }
            .scrollIndicators(.hidden)

            Spacer(minLength: 0)

            actionButton(question)
                .padding(.horizontal)
                .padding(.bottom, 8)
        }
        .padding(.top)
    }

    /// Kelime sıralamada "Kontrol Et" ancak tüm kartlar yerleştirilince aktifleşir.
    private var canCheckOrder: Bool {
        orderPool.isEmpty && !orderAssembled.isEmpty
    }

    @ViewBuilder
    private func actionButton(_ question: GrammarQuestion) -> some View {
        if question.kind == .wordOrder && !isOrderChecked {
            Button("Kontrol Et") { checkOrder() }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(!canCheckOrder)
                .opacity(canCheckOrder ? 1 : 0.5)
        } else if hasAnswered {
            Button("Devam Et") { advance() }
                .buttonStyle(PrimaryButtonStyle())
        }
    }

    // MARK: - Çoktan seçmeli / boşluk doldurma

    private func choiceQuestion(_ question: GrammarQuestion) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(question.kind == .fillBlank ? "Boşluğu doldur" : "Soru")
                .font(.caption.weight(.heavy))
                .foregroundStyle(Theme.secondaryInk)

            Text(question.prompt)
                .font(Theme.heading(22))
                .foregroundStyle(Theme.ink)
                .fixedSize(horizontal: false, vertical: true)

            if let hint = question.hint {
                Text(hint)
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
            }

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(question.choices, id: \.self) { option in
                    choiceButton(option, question: question)
                }
            }
            .padding(.top, 4)
        }
    }

    private func choiceButton(_ option: String, question: GrammarQuestion) -> some View {
        let isSelected = selectedChoice == option
        let isCorrect = option == question.answer

        return Button {
            guard !isChoiceLocked else { return }
            withAnimation(.easeInOut(duration: 0.15)) {
                selectedChoice = option
                isChoiceLocked = true
            }
            registerAnswer(correct: isCorrect)
        } label: {
            Text(option)
                .font(.system(size: 17, weight: .bold))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .minimumScaleFactor(0.6)
                .padding(.horizontal, 10)
                .frame(maxWidth: .infinity)
                .frame(height: 60)
                .background(choiceBackground(isSelected: isSelected, isCorrect: isCorrect))
                .foregroundStyle(choiceForeground(isSelected: isSelected, isCorrect: isCorrect))
                .overlay(Rectangle().strokeBorder(Theme.ink, lineWidth: 2))
        }
        .disabled(isChoiceLocked)
    }

    private func choiceBackground(isSelected: Bool, isCorrect: Bool) -> Color {
        guard isChoiceLocked else { return Theme.paper }
        if isCorrect { return .green }
        if isSelected { return Theme.accent }
        return Theme.paper
    }

    private func choiceForeground(isSelected: Bool, isCorrect: Bool) -> Color {
        (isChoiceLocked && (isCorrect || isSelected)) ? .white : Theme.ink
    }

    // MARK: - Kelime sıralama

    private func wordOrderQuestion(_ question: GrammarQuestion) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Kelimeleri sıraya diz")
                .font(.caption.weight(.heavy))
                .foregroundStyle(Theme.secondaryInk)

            Text(question.prompt)
                .font(Theme.heading(20))
                .foregroundStyle(Theme.ink)
                .fixedSize(horizontal: false, vertical: true)

            // Kurulan cevap
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 56), spacing: 8)], alignment: .leading, spacing: 8) {
                ForEach(orderAssembled) { token in
                    tokenChip(token.text) {
                        guard !isOrderChecked else { return }
                        orderAssembled.removeAll { $0.id == token.id }
                        orderPool.append(token)
                    }
                }
            }
            .frame(maxWidth: .infinity, minHeight: 52, alignment: .leading)
            .padding(10)
            .background(orderAnswerTint)
            .overlay(Rectangle().strokeBorder(Theme.ink, lineWidth: 2))

            if orderAssembled.isEmpty {
                Text("Aşağıdaki kelimelere dokunarak diz.")
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryInk)
            }

            // Havuz
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 56), spacing: 8)], alignment: .leading, spacing: 8) {
                ForEach(orderPool) { token in
                    tokenChip(token.text) {
                        guard !isOrderChecked else { return }
                        orderPool.removeAll { $0.id == token.id }
                        orderAssembled.append(token)
                    }
                }
            }
            .padding(.top, 4)

            if isOrderChecked && !isOrderCorrect {
                Text("Doğrusu: \(question.answer)")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Theme.accent)
            }
        }
    }

    private var orderAnswerTint: Color {
        guard isOrderChecked else { return Theme.paper }
        return (isOrderCorrect ? Color.green : Theme.accent).opacity(0.18)
    }

    private func tokenChip(_ text: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(Theme.ink)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Theme.paper)
                .overlay(Rectangle().strokeBorder(Theme.ink, lineWidth: 2))
        }
        .disabled(isOrderChecked)
    }

    private func checkOrder() {
        guard let question = currentQuestion, !isOrderChecked else { return }
        isOrderChecked = true
        isOrderCorrect = orderAssembled.map(\.text).joined() == question.answer
        registerAnswer(correct: isOrderCorrect)
    }

    // MARK: - Özet

    private var summaryView: some View {
        VStack(alignment: .leading, spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: 44))
                    .foregroundStyle(Theme.accent)
                Text("Pratik Bitti")
                    .font(Theme.display(32))
                    .foregroundStyle(Theme.ink)
            }

            HStack(spacing: 14) {
                statBox(value: max(0, totalAnswered - totalWrong), label: "Doğru")
                statBox(value: totalWrong, label: "Yanlış")
            }

            if wrongSummary.isEmpty {
                Text("Hiç yanlışın yok — mükemmel! 🎉")
                    .font(Theme.heading(17))
                    .foregroundStyle(Theme.ink)
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Tekrar bakılacaklar")
                        .font(Theme.heading(19))
                        .foregroundStyle(Theme.ink)

                    ScrollView {
                        VStack(spacing: 10) {
                            ForEach(wrongSummary) { entry in
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(entry.pointTitle)
                                        .font(Theme.heading(15))
                                        .foregroundStyle(Theme.accent)
                                    Text(entry.prompt)
                                        .font(.subheadline)
                                        .foregroundStyle(Theme.ink)
                                        .lineLimit(2)
                                    Text("Doğru: \(entry.answer)")
                                        .font(.caption)
                                        .foregroundStyle(Theme.secondaryInk)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(12)
                                .inkBordered()
                            }
                        }
                    }
                    .scrollIndicators(.hidden)

                    Text("Yanlış yaptığın konular \"Tekrar Çalış\" listesine eklendi.")
                        .font(.footnote)
                        .foregroundStyle(Theme.secondaryInk)
                }
            }

            Spacer(minLength: 0)

            Button("Bitir") { dismiss() }
                .buttonStyle(PrimaryButtonStyle())
        }
        .padding(20)
    }

    private func statBox(value: Int, label: String) -> some View {
        VStack(spacing: 4) {
            Text("\(value)")
                .font(Theme.display(32))
                .foregroundStyle(Theme.accent)
            Text(label)
                .font(.subheadline)
                .foregroundStyle(Theme.secondaryInk)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .inkBordered()
    }
}

#Preview {
    NavigationStack {
        GrammarPracticeView(points: Array(ContentStore.loadGrammar().prefix(2)), title: "Pratik")
    }
    .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
