import SwiftUI
import SwiftData

/// Seçilen JLPT seviyesinin kelimelerini 25'erlik parçalara böler.
/// Kullanıcının kaldığı parçayı öne çıkarır ve tüm parçaların kilit/tamamlanma durumlarını listeler.
/// Bölüm İçi Kilit (Intra-Level Gating): Bir önceki parça tamamlanmadan sonraki kilitlidir.
struct VocabularyPartSelectionView: View {
    let level: String

    @Environment(\.modelContext) private var modelContext
    @Environment(TabBarManager.self) private var tabBarManager

    @State private var allWords: [VocabularyWord] = []
    @State private var showLockedPartAlert = false
    @State private var lockedPartNumber: Int = 1

    private var jlptLevel: JLPTLevel {
        JLPTLevel(rawValue: level) ?? .n5
    }

    private var currentLevelProgress: UserLevelProgress {
        LevelProgressionService.shared.getProgress(for: jlptLevel, context: modelContext)
    }

    private var parts: [[VocabularyWord]] {
        allWords.isEmpty ? [] : ContentStore.vocabularyParts(level: level)
    }

    /// Kullanıcının henüz tamamlamadığı ilk parçanın indeksi
    private var currentPartIndex: Int {
        guard !parts.isEmpty else { return 0 }
        for index in 0..<parts.count {
            if !currentLevelProgress.isPartCompleted(module: "vocab", index: index) {
                return index
            }
        }
        return max(0, parts.count - 1)
    }

    var body: some View {
        Group {
            if allWords.isEmpty {
                SwiftUI.ProgressView()
                    .background(Theme.paper)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        // Kaldığın Yerden Devam Et Başlığı / Hızlı Giriş Kartı
                        let activeIdx = currentPartIndex
                        if parts.indices.contains(activeIdx) {
                            continueCard(index: activeIdx)
                        }

                        SectionLabel("TÜM BÖLÜMLER (\(parts.count) PARÇA)")

                        // Parçalar Listesi
                        VStack(spacing: 12) {
                            ForEach(Array(parts.enumerated()), id: \.offset) { index, part in
                                let isUnlocked = LevelProgressionService.shared.isPartUnlocked(
                                    module: "vocab",
                                    level: jlptLevel,
                                    partIndex: index,
                                    context: modelContext
                                )
                                let isCompleted = currentLevelProgress.isPartCompleted(module: "vocab", index: index)

                                if isUnlocked {
                                    NavigationLink {
                                        FlashcardSessionView(
                                            sessionKey: "vocab_\(level)_part\(index + 1)",
                                            itemKind: .vocabularyWord,
                                            allItems: part,
                                            distractorPool: allWords,
                                            accentColor: Theme.accent,
                                            title: "\(level) Kelimeler Part \(index + 1)"
                                        )
                                    } label: {
                                        partRow(index: index, count: part.count, isUnlocked: true, isCompleted: isCompleted)
                                    }
                                } else {
                                    Button {
                                        lockedPartNumber = index + 1
                                        showLockedPartAlert = true
                                    } label: {
                                        partRow(index: index, count: part.count, isUnlocked: false, isCompleted: false)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }
                    .padding(20)
                    .padding(.bottom, 80)
                }
                .scrollIndicators(.hidden)
                .background(Theme.paper)
            }
        }
        .navigationTitle(L10n.vocabularyLevel(level))
        .alert("Bölüm Kilitli", isPresented: $showLockedPartAlert) {
            Button("Tamam", role: .cancel) {}
        } message: {
            Text("Part \(lockedPartNumber) kilidini açmak için lütfen önceki bölümü tamamlayın.")
        }
        .onAppear {
            LevelProgressionService.shared.ensureInitialProgress(context: modelContext)
            guard allWords.isEmpty else { return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                guard let url = Bundle.main.url(forResource: "\(level)VocabularyData", withExtension: "json"),
                      let data = try? Data(contentsOf: url),
                      let loaded = try? JSONDecoder().decode([VocabularyWord].self, from: data) else {
                    return
                }
                allWords = loaded
            }
        }
    }

    // MARK: - Hızlı Devam Kartı

    private func continueCard(index: Int) -> some View {
        NavigationLink {
            FlashcardSessionView(
                sessionKey: "vocab_\(level)_part\(index + 1)",
                itemKind: .vocabularyWord,
                allItems: parts[index],
                distractorPool: allWords,
                accentColor: Theme.accent,
                title: "\(level) Kelimeler Part \(index + 1)"
            )
        } label: {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("KALDIĞIN YER")
                        .font(.caption.weight(.heavy))
                        .foregroundStyle(Theme.accent)
                    Spacer()
                    Text("Part \(index + 1) / \(parts.count)")
                        .font(.caption.bold())
                        .foregroundStyle(Theme.secondaryInk)
                }

                Text("\(level) Kelimeler: Part \(index + 1)")
                    .font(Theme.heading(20))
                    .foregroundStyle(Theme.ink)

                Text("Hemen devam et ve bu bölümdeki 25 kelimeyi tamamla.")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)

                HStack {
                    Spacer()
                    HStack(spacing: 6) {
                        Text("Çalışmaya Başla")
                            .font(.system(size: 15, weight: .bold))
                        Image(systemName: "arrow.right")
                    }
                    .foregroundStyle(Theme.paper)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(Theme.accent)
                    .clipShape(Capsule())
                }
            }
            .padding()
            .inkBordered(lineWidth: 2.5)
        }
    }

    // MARK: - Satır

    private func partRow(index: Int, count: Int, isUnlocked: Bool, isCompleted: Bool) -> some View {
        HStack(spacing: 16) {
            Text("\(index + 1)")
                .font(Theme.heading(18))
                .foregroundStyle(.white)
                .frame(width: 40, height: 40)
                .background(isUnlocked ? (isCompleted ? Color.green : Theme.accent) : Theme.secondaryInk)

            VStack(alignment: .leading, spacing: 2) {
                Text("\(level) Part \(index + 1)")
                    .font(Theme.heading(18))
                    .foregroundStyle(isUnlocked ? Theme.ink : Theme.secondaryInk)
                Text(isUnlocked ? (isCompleted ? "Tamamlandı • \(count) kelime" : "\(count) kelime") : "Kilitli • Önceki bölümü tamamla")
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryInk)
            }

            Spacer()

            if isCompleted {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 20))
                    .foregroundStyle(.green)
            } else if isUnlocked {
                Image(systemName: "arrow.right")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(Theme.ink)
            } else {
                Image(systemName: "lock.fill")
                    .foregroundStyle(Theme.secondaryInk)
            }
        }
        .padding(14)
        .inkBordered()
        .opacity(isUnlocked ? 1 : 0.6)
    }
}

#Preview {
    NavigationStack {
        VocabularyPartSelectionView(level: "N5")
    }
    .modelContainer(for: [UserLevelProgress.self, LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
