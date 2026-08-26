import SwiftUI
import SwiftData

/// Ortak flashcard motoru: karakterler/kanjiler tek tek, tam ekran bir kart olarak gelir.
/// Her kartın altında 4 şık vardır — seçilen şık doğruysa yeşile, yanlışsa kırmızıya döner.
/// Cevaplandıktan sonra kart otomatik çevrilir ve arkasında bir özet + örnek kelime gösterilir.
/// Hem LearningView (Hiragana/Katakana) hem Kanji akışı bu bileşeni sarmalayarak kullanır.
///
/// Oturum devamlılığı: kullanıcı ekrandan çıkıp geri döndüğünde kaldığı karta devam eder;
/// yanlış yapılan kartlar hafızada (SwiftData'da) tutulur ve tüm kartlar bitince otomatik
/// olarak sadece onlarla yeni bir tur başlar — bu, art arda bir turda hepsi doğru bilinene
/// kadar sürer.
struct FlashcardSessionView<Item: FlashcardItem>: View {
    let sessionKey: String
    let itemKind: LearnableItemKind
    let allItems: [Item]
    let distractorPool: [Item]
    let accentColor: Color
    let title: String

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query private var userProgressRecords: [UserProgress]

    @State private var progressByID: [String: LearningItemProgress] = [:]
    @State private var quizViewModel: QuizViewModel<Item>?
    @State private var sessionState: LearningSessionState?
    @State private var isFlipped = false
    @State private var isPresentingQuiz = false
    @State private var hasRecordedStreak = false
    @State private var autoAdvanceTask: Task<Void, Never>?
    @State private var didSetup = false

    /// Toplam öğe sayısı üzerinden "kalıcı olarak ustalaşılan" oran. `remainingItemIDs` +
    /// `wrongItemIDs` her zaman "henüz bitmemiş" öğeleri temsil eder (tur değişse bile bu
    /// toplam sabit kalır), bu yüzden bu ikisinin dışındakiler gerçekten ustalaşılmış demektir.
    private var moduleProgress: Double {
        guard let session = sessionState, !allItems.isEmpty else { return 0 }
        let inPlay = session.remainingItemIDs.count + session.wrongItemIDs.count
        return Double(max(0, allItems.count - inPlay)) / Double(allItems.count)
    }

    private var moduleProgressLabel: String {
        guard let session = sessionState else { return "" }
        let inPlay = session.remainingItemIDs.count + session.wrongItemIDs.count
        let mastered = max(0, allItems.count - inPlay)
        return "\(mastered)/\(allItems.count) öğrenildi"
    }

    var body: some View {
        Group {
            if let quizViewModel {
                if quizViewModel.isFinished {
                    completionView(quizViewModel)
                } else {
                    flashcardContent(quizViewModel)
                }
            } else {
                SwiftUI.ProgressView()
            }
        }
        .background(Theme.paper)
        .navigationTitle(title)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Hızlı Quiz") {
                    isPresentingQuiz = true
                }
                .tint(accentColor)
            }
        }
        .sheet(isPresented: $isPresentingQuiz) {
            NavigationStack {
                QuizView(
                    questions: allItems,
                    itemKind: itemKind,
                    accentColor: accentColor,
                    progressLookup: { progressByID[$0] }
                )
            }
        }
        .onAppear {
            guard !didSetup else { return }
            didSetup = true
            syncProgress()
            setupSession()
        }
    }

    // MARK: - İlerleme kaydı (SpacedRepetitionService için)

    private func syncProgress() {
        let kind = itemKind
        // Dikkat: #Predicate ile enum karşılaştırması ($0.itemKind == kind) SwiftData'da
        // desteklenmiyor ve çalışma zamanında hata fırlatıyor. Bu, mevcut kayıtların hiç
        // bulunamamasına, her açılışta aynı itemID'lerle sıfır kayıtların upsert edilmesine
        // ve oturumdaki ilerleme güncellemelerinin kaybolmasına yol açıyordu. Bu yüzden
        // tüm kayıtlar çekilip tür filtresi bellekte yapılıyor.
        let descriptor = FetchDescriptor<LearningItemProgress>()
        let existing = ((try? modelContext.fetch(descriptor)) ?? []).filter { $0.itemKind == kind }

        var byID: [String: LearningItemProgress] = [:]
        for progress in existing where byID[progress.itemID] == nil {
            byID[progress.itemID] = progress
        }

        for item in allItems where byID[item.id] == nil {
            let newProgress = LearningItemProgress(itemID: item.id, itemKind: kind)
            modelContext.insert(newProgress)
            byID[item.id] = newProgress
        }
        try? modelContext.save()
        progressByID = byID
    }

    // MARK: - Oturum (kaldığı yerden devam + yanlışları tekrar turu)

    private func setupSession() {
        let key = sessionKey
        let descriptor = FetchDescriptor<LearningSessionState>(predicate: #Predicate { $0.moduleType == key })
        let existing = (try? modelContext.fetch(descriptor))?.first

        let session: LearningSessionState
        if let existing {
            session = existing
        } else {
            session = LearningSessionState(moduleType: key, remainingItemIDs: allItems.map(\.id).shuffled())
            modelContext.insert(session)
        }

        // Öğe seti değişmiş olabilir (ör. "Tekrar Çalış" oturumlarında liste küçülür);
        // artık geçerli olmayan id'ler temizlenir ki tur boş kartlarla açılmasın.
        let validIDs = Set(allItems.map(\.id))
        session.remainingItemIDs.removeAll { !validIDs.contains($0) }
        session.wrongItemIDs.removeAll { !validIDs.contains($0) }

        if session.isCompleted {
            // Daha önce tüm kartlar art arda doğru bilinerek tamamlanmıştı;
            // tekrar açıldığında sıfırdan yeni bir pratik turu başlatıyoruz.
            session.isCompleted = false
            session.wrongItemIDs = []
            session.remainingItemIDs = allItems.map(\.id).shuffled()
            resetSessionStats(session)
        } else if session.remainingItemIDs.isEmpty && !session.wrongItemIDs.isEmpty {
            // Kaldığımız yer "ana tur bitti, tekrar turu bekleniyor" noktasıydı.
            session.remainingItemIDs = session.wrongItemIDs.shuffled()
            session.wrongItemIDs = []
        } else if session.remainingItemIDs.isEmpty {
            session.remainingItemIDs = allItems.map(\.id).shuffled()
            resetSessionStats(session)
        }

        try? modelContext.save()
        sessionState = session
        startRound(with: session.remainingItemIDs)
    }

    /// `ids`'i sıraya koyar (daha önce ekranda olan kart varsa en başa alır, geri kalanı
    /// karıştırır), sonra bu sabit sırayla bir QuizViewModel oluşturur — kullanıcı ekrandan
    /// çıkıp geri döndüğünde tam olarak bıraktığı kartla karşılaşır. Şık havuzu olarak
    /// `distractorPool`'u (genelde tüm modül) veriyoruz ki küçük tekrar turlarında bile 4 şık çıksın.
    private func startRound(with ids: [String]) {
        var orderedIDs = ids
        if let currentID = sessionState?.currentCardID, let idx = orderedIDs.firstIndex(of: currentID) {
            orderedIDs.remove(at: idx)
            orderedIDs.shuffle()
            orderedIDs.insert(currentID, at: 0)
        } else {
            orderedIDs.shuffle()
        }

        let itemsByID = Dictionary(uniqueKeysWithValues: allItems.map { ($0.id, $0) })
        let items = orderedIDs.compactMap { itemsByID[$0] }

        let vm = QuizViewModel(
            questions: items,
            itemKind: itemKind,
            modelContext: modelContext,
            progressLookup: { progressByID[$0] },
            shuffled: false,
            distractorPool: distractorPool
        )
        quizViewModel = vm
        isFlipped = false
        syncCurrentCard(vm)
    }

    /// O an ekranda olan kartın id'sini oturuma yazar (bkz. LearningSessionState.currentCardID).
    private func syncCurrentCard(_ vm: QuizViewModel<Item>?) {
        sessionState?.currentCardID = vm?.currentQuestion?.id
        try? modelContext.save()
    }

    /// Yeni bir oturum başlarken özet istatistiklerini sıfırlar.
    private func resetSessionStats(_ session: LearningSessionState) {
        session.totalAnswerCount = 0
        session.wrongAnswerCounts = [:]
    }

    /// Her cevaptan hemen sonra çağrılır: öğe artık "kalan" değildir; yanlışsa tekrar turu
    /// listesine eklenir, özet sayaçları güncellenir ve öğe "Tekrar Çalış" listesine
    /// işaretlenir. Kullanıcı ekrandan çıksa bile bunlar kayıtlı kalır.
    private func recordAnswer(for item: Item, correct: Bool) {
        guard let session = sessionState else { return }
        session.totalAnswerCount += 1
        session.remainingItemIDs.removeAll { $0 == item.id }
        if correct {
            session.wrongItemIDs.removeAll { $0 == item.id }
        } else {
            if !session.wrongItemIDs.contains(item.id) {
                session.wrongItemIDs.append(item.id)
            }
            session.wrongAnswerCounts[item.id, default: 0] += 1
            progressByID[item.id]?.needsReview = true
        }
        try? modelContext.save()
    }

    /// Bir tur bittiğinde (son karta cevap verilip ileri geçildiğinde) çağrılır. Yanlış
    /// yapılan öğe varsa modülü bitirmek yerine sadece onlarla yeni bir tur açar — kullanıcı
    /// tüm kartları art arda doğru yapana kadar bu döngü sürer.
    private func handleRoundCompletionIfNeeded(_ vm: QuizViewModel<Item>) {
        guard vm.isFinished, let session = sessionState else { return }

        if session.wrongItemIDs.isEmpty {
            session.isCompleted = true
            try? modelContext.save()
        } else {
            let retryIDs = session.wrongItemIDs.shuffled()
            session.remainingItemIDs = retryIDs
            session.wrongItemIDs = []
            try? modelContext.save()
            withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                startRound(with: retryIDs)
            }
        }
    }

    // MARK: - Akış (tek tek gelen kartlar)

    @ViewBuilder
    private func flashcardContent(_ vm: QuizViewModel<Item>) -> some View {
        VStack(spacing: 24) {
            VStack(spacing: 4) {
                SwiftUI.ProgressView(value: moduleProgress)
                    .tint(accentColor)
                Text(moduleProgressLabel)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal)

            if let question = vm.currentQuestion {
                flipCard(question)
                    .id(question.id)
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing).combined(with: .opacity),
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))
                    .padding(.horizontal)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        guard isFlipped else { return }
                        autoAdvanceTask?.cancel()
                        advanceToNext(vm)
                    }

                optionsGrid(vm)

                // Doğru cevapta otomatik geçilir; yanlışta kullanıcı örnek kelimeyi
                // inceleyip karta dokunarak kendi geçer (bkz. flipCard'daki tap gesture).
                if isFlipped && vm.isAnswerCorrect == false {
                    Text("Devam etmek için karta dokun")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()
        }
        .padding(.top)
        .animation(.spring(response: 0.4, dampingFraction: 0.8), value: vm.currentIndex)
    }

    @ViewBuilder
    private func flipCard(_ item: Item) -> some View {
        ZStack {
            cardFace {
                VStack(spacing: 10) {
                    if let reading = item.promptReading {
                        Text(reading)
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundStyle(Theme.secondaryInk)
                            .minimumScaleFactor(0.5)
                            .lineLimit(1)
                    }
                    Text(item.prompt)
                        .font(Theme.display(80))
                        .foregroundStyle(Theme.ink)
                        .minimumScaleFactor(0.3)
                        .lineLimit(1)
                    Button {
                        AudioService.shared.speak(item.speechText)
                    } label: {
                        Image(systemName: "speaker.wave.2.fill")
                            .foregroundStyle(accentColor)
                    }
                }
            }
            .opacity(isFlipped ? 0 : 1)

            cardFace {
                exampleWordsView(item)
            }
            .opacity(isFlipped ? 1 : 0)
            .rotation3DEffect(.degrees(180), axis: (x: 0, y: 1, z: 0))
        }
        .rotation3DEffect(.degrees(isFlipped ? 180 : 0), axis: (x: 0, y: 1, z: 0))
        .frame(height: 220)
    }

    private func cardFace<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        Rectangle()
            .fill(Theme.paper)
            .overlay(Rectangle().strokeBorder(Theme.accent, lineWidth: 2))
            .overlay(content().padding())
    }

    private func exampleWordsView(_ item: Item) -> some View {
        VStack(spacing: 10) {
            Text(item.flipRecap)
                .font(Theme.heading(17))
                .foregroundStyle(Theme.accent)
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.7)

            if item.exampleWords.isEmpty {
                // Örnek kelimesi olmayan öğelerde (ör. kelime kartları) kartın
                // arkasında doğru cevap (anlam) gösterilir.
                Text(item.correctAnswer)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(Theme.ink)
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.6)
            } else {
                ForEach(item.exampleWords, id: \.hiragana) { word in
                    VStack(spacing: 2) {
                        Text(word.hiragana)
                            .font(.system(size: 20, weight: .bold))
                        Text("\(word.romaji) · \(word.turkishMeaning)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                }
            }
        }
    }

    // MARK: - 4 şıklı seçim

    @ViewBuilder
    private func optionsGrid(_ vm: QuizViewModel<Item>) -> some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
            ForEach(vm.options, id: \.self) { option in
                optionButton(option, vm: vm)
            }
        }
        .padding(.horizontal)
    }

    private func optionButton(_ option: String, vm: QuizViewModel<Item>) -> some View {
        let isSelected = vm.selectedAnswer == option

        return Button {
            guard vm.selectedAnswer == nil, let item = vm.currentQuestion else { return }
            withAnimation(.easeInOut(duration: 0.2)) {
                vm.submitAnswer(option)
            }
            recordAnswer(for: item, correct: vm.isAnswerCorrect == true)
            autoAdvanceTask = Task {
                try? await Task.sleep(for: .seconds(0.7))
                guard !Task.isCancelled else { return }
                withAnimation(.spring(response: 0.45, dampingFraction: 0.75)) {
                    isFlipped = true
                }
                if vm.isAnswerCorrect == true {
                    try? await Task.sleep(for: .seconds(2.8))
                    guard !Task.isCancelled else { return }
                    advanceToNext(vm)
                }
            }
        } label: {
            Text(option)
                .font(.system(size: 17, weight: .bold))
                .frame(maxWidth: .infinity)
                .padding()
                .background(optionColor(isSelected: isSelected, vm: vm))
                .foregroundStyle(isSelected ? .white : Theme.ink)
                .overlay(Rectangle().strokeBorder(Theme.ink, lineWidth: 2))
        }
        .disabled(vm.selectedAnswer != nil)
    }

    private func optionColor(isSelected: Bool, vm: QuizViewModel<Item>) -> Color {
        guard isSelected else { return Theme.paper }
        return vm.isAnswerCorrect == true ? .green : Theme.accent
    }

    private func advanceToNext(_ vm: QuizViewModel<Item>) {
        withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
            isFlipped = false
            vm.moveToNext()
        }
        syncCurrentCard(vm)
        handleRoundCompletionIfNeeded(vm)
    }

    // MARK: - Bitiş

    private func completionView(_ vm: QuizViewModel<Item>) -> some View {
        let itemsByID = Dictionary(uniqueKeysWithValues: allItems.map { ($0.id, $0) })
        let wrongItems = (sessionState?.wrongAnswerCounts ?? [:])
            .compactMap { id, count -> SessionSummaryItem? in
                guard let item = itemsByID[id] else { return nil }
                return SessionSummaryItem(id: id, prompt: item.prompt, detail: item.flipRecap, wrongCount: count)
            }
            .sorted { $0.wrongCount > $1.wrongCount }

        return SessionSummaryView(
            totalAnswers: sessionState?.totalAnswerCount ?? vm.questions.count,
            wrongItems: wrongItems,
            onFinish: { dismiss() }
        )
        .onAppear { recordStreakIfNeeded() }
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
}
