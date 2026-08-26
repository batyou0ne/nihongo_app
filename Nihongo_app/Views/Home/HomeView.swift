import SwiftUI
import SwiftData

/// Uygulamanın giriş ekranı. Her öğrenme modülü için renk kodlu bir kart gösterir
/// (Hiragana: kırmızı, Katakana: mavi, Kanji: yeşil) ve üstte günlük seriyi gösterir.
struct HomeView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var userProgressRecords: [UserProgress]
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
                VStack(alignment: .leading, spacing: 24) {
                    streakHeader

                    VStack(spacing: 16) {
                        NavigationLink {
                            LearningView(characterType: .hiragana)
                        } label: {
                            ModuleCard(
                                title: "Hiragana",
                                subtitle: "46 karakter · あいうえお",
                                systemImage: "character.book.closed.fill",
                                color: .red
                            )
                        }

                        NavigationLink {
                            LearningView(characterType: .katakana)
                        } label: {
                            ModuleCard(
                                title: "Katakana",
                                subtitle: "46 karakter · アイウエオ",
                                systemImage: "character.book.closed.fill",
                                color: .blue
                            )
                        }

                        NavigationLink {
                            KanjiLevelSelectionView()
                        } label: {
                            ModuleCard(
                                title: "Kanji",
                                subtitle: "N5 · 80 kanji",
                                systemImage: "text.book.closed.fill",
                                color: .green
                            )
                        }

                        NavigationLink {
                            ProgressOverviewView()
                        } label: {
                            ModuleCard(
                                title: "İlerleme",
                                subtitle: "Öğrenilenleri görüntüle",
                                systemImage: "chart.bar.fill",
                                color: .gray
                            )
                        }
                    }
                }
                .padding()
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .navigationTitle("Nihongo")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showSignIn = true
                    } label: {
                        Image(systemName: AuthService.shared.isLinkedToApple
                              ? "person.crop.circle.fill.badge.checkmark"
                              : "person.crop.circle")
                    }
                }
            }
            .sheet(isPresented: $showSignIn) {
                SignInView()
            }
            .onAppear {
                _ = userProgress
            }
        }
    }

    private var streakHeader: some View {
        HStack {
            Image(systemName: "flame.fill")
                .foregroundStyle(.orange)
            Text("\(userProgress.currentStreak) günlük seri")
                .font(.headline)
            Spacer()
        }
        .padding()
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

/// Ana ekrandaki modül kartı. Minimalist, ince kenarlıklı, tek bir vurgu rengiyle.
private struct ModuleCard: View {
    let title: String
    let subtitle: String
    let systemImage: String
    let color: Color

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: systemImage)
                .font(.title2)
                .foregroundStyle(color)
                .frame(width: 44, height: 44)
                .background(color.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(.headline, design: .serif))
                    .foregroundStyle(.primary)
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(.tertiary)
        }
        .padding()
        .background(Color(uiColor: .systemBackground))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(color.opacity(0.25), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

#Preview {
    HomeView()
        .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
