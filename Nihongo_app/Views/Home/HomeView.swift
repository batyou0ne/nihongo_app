import SwiftUI
import SwiftData

/// Uygulamanın giriş ekranı. Bir menü değil, bir gösterge paneli olacak şekilde
/// düzenlendi: üstte 7 günlük seri şeridi, altında (varsa) "Kaldığın yer" kartı,
/// sonra ilerleme çubuklu öğrenme modülleri ve en altta tekrar/özet bölümü.
struct HomeView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var userProgressRecords: [UserProgress]
    @Query private var allProgress: [LearningItemProgress]
    @Query(filter: #Predicate<LearningItemProgress> { $0.needsReview == true })
    private var reviewItems: [LearningItemProgress]
    /// En son açılan oturum başta; "Kaldığın yer" kartı bunun ilkini kullanır.
    @Query(sort: \LearningSessionState.lastOpenedAt, order: .reverse)
    private var sessions: [LearningSessionState]

    @State private var showSignIn = false
    /// İçerik JSON'larından üretilir; body içinde dosya okumamak için onAppear'da hesaplanır.
    @State private var resume: ResumeTarget?

    private var userProgress: UserProgress {
        if let existing = userProgressRecords.first {
            return existing
        }
        let newProgress = UserProgress()
        modelContext.insert(newProgress)
        return newProgress
    }

    /// Bir modülde en az bir kez doğru bilinen öğe sayısı (ProgressOverviewView ile aynı ölçüt).
    private func learnedCount(_ kind: LearnableItemKind) -> Int {
        allProgress.filter { $0.itemKind == kind && $0.repetitionCount >= 1 }.count
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 28) {
                    header
                    streakStrip

                    if let resume {
                        resumeCard(resume)
                    }

                    reviewSection
                }
                .padding(20)
            }
            .scrollIndicators(.hidden)
            .background(Theme.paper)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showSignIn = true
                    } label: {
                        Image(systemName: AuthService.shared.hasAccount
                              ? "person.crop.circle.fill.badge.checkmark"
                              : "person.crop.circle")
                            .foregroundStyle(Theme.ink)
                    }
                }
            }
            .navigationDestination(for: ResumeTarget.self) { target in
                resumeDestination(target)
            }
            .sheet(isPresented: $showSignIn) {
                if AuthService.shared.hasAccount {
                    AccountView()
                } else {
                    SignInView()
                }
            }
            .onAppear {
                _ = userProgress
                resume = ResumeTarget(sessions: sessions)
            }
        }
        .tint(Theme.accent)
    }

    // MARK: - Başlık

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("開門")
                .font(Theme.display(56))
                .foregroundStyle(Theme.accent)
            Text("Kaimon")
                .font(Theme.heading(22))
                .foregroundStyle(Theme.ink)
        }
    }

    // MARK: - Seri şeridi (son 7 gün)

    /// Sağdaki kareler soldan sağa 6 gün önce → bugün sırasında; çalışılan günler dolu.
    private var streakStrip: some View {
        let streak = userProgress.activeStreak

        return HStack(spacing: 10) {
            Image(systemName: "flame.fill")
                .foregroundStyle(streak > 0 ? Theme.accent : Theme.secondaryInk)
            Text(streak > 0 ? "\(streak) günlük seri" : "Seri yok — bugün başla")
                .font(.system(size: 17, weight: .heavy))
                .foregroundStyle(streak > 0 ? Theme.ink : Theme.secondaryInk)
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            Spacer(minLength: 8)

            HStack(spacing: 5) {
                ForEach((0..<7).reversed(), id: \.self) { daysAgo in
                    let studied = userProgress.didStudy(daysAgo: daysAgo)
                    RoundedRectangle(cornerRadius: 4)
                        .fill(studied ? Theme.accent : Theme.paper)
                        .frame(width: 13, height: 13)
                        .overlay(RoundedRectangle(cornerRadius: 4).strokeBorder(studied ? Theme.accent : Theme.secondaryInk, lineWidth: 1.5))
                }
            }
        }
        .padding()
        .inkBordered()
    }

    // MARK: - Kaldığın yer

    private func resumeCard(_ target: ResumeTarget) -> some View {
        NavigationLink(value: target) {
            VStack(alignment: .leading, spacing: 10) {
                Text("KALDIĞIN YER")
                    .font(.caption.weight(.heavy))
                    .foregroundStyle(Theme.secondaryInk)

                HStack(alignment: .firstTextBaseline) {
                    Text(target.title)
                        .font(Theme.heading(20))
                        .foregroundStyle(Theme.ink)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    Spacer(minLength: 8)
                    Text("\(target.done)/\(target.total)")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(Theme.secondaryInk)
                }

                ProgressBar(fraction: target.fraction)

                Text("DEVAM ET  →")
                    .font(.system(size: 17, weight: .bold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Theme.ink)
                    .foregroundStyle(Theme.paper)
                    .padding(.top, 2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .inkBordered(lineWidth: 3)
        }
    }

    @ViewBuilder
    private func resumeDestination(_ target: ResumeTarget) -> some View {
        switch target.module {
        case .hiragana:
            LearningView(characterType: .hiragana)
        case .katakana:
            LearningView(characterType: .katakana)
        case .kanji(let level, let index):
            let parts = ContentStore.kanjiParts(level: level)
            if parts.indices.contains(index) {
                FlashcardSessionView(
                    sessionKey: target.sessionKey,
                    itemKind: .kanji,
                    allItems: parts[index],
                    distractorPool: ContentStore.loadKanji(level: level),
                    accentColor: Theme.accent,
                    title: target.title
                )
            }
        case .vocabulary(let level, let index):
            let parts = ContentStore.vocabularyParts(level: level)
            if parts.indices.contains(index) {
                FlashcardSessionView(
                    sessionKey: target.sessionKey,
                    itemKind: .vocabularyWord,
                    allItems: parts[index],
                    distractorPool: ContentStore.loadVocabulary(level: level),
                    accentColor: Theme.accent,
                    title: target.title
                )
            }
        }
    }



    // MARK: - Tekrar & ilerleme

    private var reviewSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionLabel("TEKRAR & İLERLEME")

            VStack(spacing: 12) {
                NavigationLink {
                    ReviewListView()
                } label: {
                    UtilityCard(
                        systemImage: "exclamationmark.arrow.circlepath",
                        title: "Tekrar Çalış",
                        subtitle: reviewItems.isEmpty
                            ? "Bekleyen öğe yok"
                            : "\(reviewItems.count) öğe seni bekliyor",
                        // Bekleyen öğe varsa kart öne çıksın, yoksa geri çekilsin.
                        isHighlighted: !reviewItems.isEmpty
                    )
                }

                NavigationLink {
                    ProgressOverviewView()
                } label: {
                    UtilityCard(
                        systemImage: "chart.bar.fill",
                        title: "İlerleme",
                        subtitle: "Öğrenilenleri görüntüle",
                        isHighlighted: false
                    )
                }
            }
        }
    }
}

// MARK: - Kaldığın yer hedefi

/// Ana ekrandaki "Kaldığın yer" kartının gösterdiği oturum. `LearningSessionState.moduleType`
/// (ör. "vocab_N5_part3") ayrıştırılıp hem okunabilir başlığa hem de açılacak ekrana çevrilir.
/// Gramer modülü LearningSessionState kullanmadığı için burada yer almaz; "Tekrar Çalış"
/// oturumları (review_*) da kasten dışarıda bırakıldı — onların kendi girişi var.
struct ResumeTarget: Hashable {
    enum Module: Hashable {
        case hiragana
        case katakana
        case kanji(level: String, index: Int)
        case vocabulary(level: String, index: Int)
    }

    let sessionKey: String
    let module: Module
    let title: String
    let done: Int
    let total: Int

    var fraction: Double {
        total > 0 ? Double(done) / Double(total) : 0
    }

    /// Verilen oturumlar arasından (en yeniden eskiye sıralı gelir) kartta
    /// gösterilebilecek ilkini seçer.
    init?(sessions: [LearningSessionState]) {
        for session in sessions where session.lastOpenedAt > .distantPast {
            if let target = ResumeTarget(session: session) {
                self = target
                return
            }
        }
        return nil
    }

    private init?(session: LearningSessionState) {
        let key = session.moduleType
        let module: Module
        let title: String
        let total: Int

        if key == CharacterType.hiragana.rawValue {
            module = .hiragana
            title = "Hiragana"
            total = LearnableItemKind.hiraganaCharacter.totalCount
        } else if key == CharacterType.katakana.rawValue {
            module = .katakana
            title = "Katakana"
            total = LearnableItemKind.katakanaCharacter.totalCount
        } else if let (level, index) = Self.parsePart(key, prefix: "kanji_") {
            let parts = ContentStore.kanjiParts(level: level)
            guard parts.indices.contains(index) else { return nil }
            module = .kanji(level: level, index: index)
            title = "\(level) Kanji's Part \(index + 1)"
            total = parts[index].count
        } else if let (level, index) = Self.parsePart(key, prefix: "vocab_") {
            let parts = ContentStore.vocabularyParts(level: level)
            guard parts.indices.contains(index) else { return nil }
            module = .vocabulary(level: level, index: index)
            title = "\(level) Kelimeler Part \(index + 1)"
            total = parts[index].count
        } else {
            return nil // review_* ve tanınmayan anahtarlar
        }

        // FlashcardSessionView'daki ölçütle aynı: turda hâlâ dolaşan öğeler dışındakiler bitmiş sayılır.
        let inPlay = session.remainingItemIDs.count + session.wrongItemIDs.count
        let done = max(0, total - inPlay)

        // Oturum bitmişse "kaldığın yer" diye bir şey kalmamıştır.
        guard !session.isCompleted, done < total else { return nil }

        self.sessionKey = key
        self.module = module
        self.title = title
        self.total = total
        self.done = done
    }

    /// "kanji_N5_part3" → ("N5", 2). Bölüm numarası 1'den başlar, dizi indeksi 0'dan.
    private static func parsePart(_ key: String, prefix: String) -> (level: String, index: Int)? {
        guard key.hasPrefix(prefix) else { return nil }
        let rest = key.dropFirst(prefix.count)          // "N5_part3"
        let pieces = rest.components(separatedBy: "_part")
        guard pieces.count == 2,
              let number = Int(pieces[1]), number > 0,
              !pieces[0].isEmpty else { return nil }
        return (pieces[0], number - 1)
    }
}

// MARK: - Ortak parçalar

/// Tekrar Çalış / İlerleme kartı: içerik modüllerinden ayrışsın diye ilerleme
/// çubuğu yok ve daha alçak. `isHighlighted` ise vermilyon zeminle öne çıkar.
private struct UtilityCard: View {
    let systemImage: String
    let title: String
    let subtitle: String
    let isHighlighted: Bool

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: systemImage)
                .font(.title3)
                .foregroundStyle(isHighlighted ? Theme.paper : Theme.accent)
                .frame(width: 30)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(Theme.heading(17))
                    .foregroundStyle(isHighlighted ? Theme.paper : Theme.ink)
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(isHighlighted ? Theme.paper.opacity(0.85) : Theme.secondaryInk)
            }

            Spacer()
            Image(systemName: "arrow.right")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(isHighlighted ? Theme.paper : Theme.ink)
        }
        .padding(.horizontal)
        .padding(.vertical, 14)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(isHighlighted ? Theme.accent : Theme.paper)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(Theme.ink, lineWidth: 2))
    }
}

#Preview {
    HomeView()
        .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
