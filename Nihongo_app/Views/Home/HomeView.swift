import SwiftUI
import SwiftData

/// Uygulamanın giriş ekranı. Büyük vermilyon "日本語" başlığı, altında keskin
/// köşeli siyah kenarlıklı modül kartları ve günlük seri gösterir.
struct HomeView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var userProgressRecords: [UserProgress]
    @Query(filter: #Predicate<LearningItemProgress> { $0.needsReview == true })
    private var reviewItems: [LearningItemProgress]
    @State private var showSignIn = false

    private var userProgress: UserProgress {
        if let existing = userProgressRecords.first {
            return existing
        }
        let newProgress = UserProgress()
        modelContext.insert(newProgress)
        return newProgress
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 28) {
                    header

                    streakHeader

                    VStack(spacing: 14) {
                        NavigationLink {
                            LearningView(characterType: .hiragana)
                        } label: {
                            ModuleCard(
                                title: "Hiragana",
                                subtitle: "46 karakter · あいうえお",
                                systemImage: "character.book.closed.fill"
                            )
                        }

                        NavigationLink {
                            LearningView(characterType: .katakana)
                        } label: {
                            ModuleCard(
                                title: "Katakana",
                                subtitle: "46 karakter · アイウエオ",
                                systemImage: "character.book.closed.fill"
                            )
                        }

                        NavigationLink {
                            KanjiLevelSelectionView()
                        } label: {
                            ModuleCard(
                                title: "Kanji",
                                subtitle: "N5 · 80 kanji",
                                systemImage: "text.book.closed.fill"
                            )
                        }

                        NavigationLink {
                            VocabularyLevelSelectionView()
                        } label: {
                            ModuleCard(
                                title: "Kelimeler",
                                subtitle: "N5 · 675 kelime",
                                systemImage: "character.bubble.fill"
                            )
                        }

                        NavigationLink {
                            GrammarLevelSelectionView()
                        } label: {
                            ModuleCard(
                                title: "Gramer",
                                subtitle: "N5 · 16 konu",
                                systemImage: "text.alignleft"
                            )
                        }

                        NavigationLink {
                            ReviewListView()
                        } label: {
                            ModuleCard(
                                title: "Tekrar Çalış",
                                subtitle: reviewItems.isEmpty
                                    ? "Bekleyen öğe yok"
                                    : "\(reviewItems.count) öğe seni bekliyor",
                                systemImage: "exclamationmark.arrow.circlepath"
                            )
                        }

                        NavigationLink {
                            ProgressOverviewView()
                        } label: {
                            ModuleCard(
                                title: "İlerleme",
                                subtitle: "Öğrenilenleri görüntüle",
                                systemImage: "chart.bar.fill"
                            )
                        }
                    }
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
            .sheet(isPresented: $showSignIn) {
                if AuthService.shared.hasAccount {
                    AccountView()
                } else {
                    SignInView()
                }
            }
            .onAppear {
                _ = userProgress
            }
        }
        .tint(Theme.accent)
    }

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

    private var streakHeader: some View {
        HStack(spacing: 10) {
            Image(systemName: "flame.fill")
                .foregroundStyle(Theme.accent)
            Text("\(userProgress.currentStreak) günlük seri")
                .font(.system(size: 17, weight: .heavy))
                .foregroundStyle(Theme.ink)
            Spacer()
        }
        .padding()
        .inkBordered()
    }
}

/// Ana ekrandaki modül kartı: keskin köşeli, kalın siyah kenarlıklı,
/// vermilyon ikonlu.
private struct ModuleCard: View {
    let title: String
    let subtitle: String
    let systemImage: String

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: systemImage)
                .font(.title2)
                .foregroundStyle(Theme.accent)
                .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(Theme.heading(19))
                    .foregroundStyle(Theme.ink)
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
            }

            Spacer()
            Image(systemName: "arrow.right")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(Theme.ink)
        }
        .padding()
        .inkBordered()
    }
}

#Preview {
    HomeView()
        .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
