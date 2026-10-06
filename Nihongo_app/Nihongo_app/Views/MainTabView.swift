import SwiftUI
import SwiftData

struct MainTabView: View {
    @State private var selectedTab: Int = 0
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.modelContext) private var modelContext
    @State private var tabBarManager = TabBarManager.shared

    var body: some View {
        ZStack(alignment: .bottom) {
            ZStack {
                HomeView()
                    .opacity(selectedTab == 0 ? 1 : 0)
                    .allowsHitTesting(selectedTab == 0)

                AlphabetMainView()
                    .opacity(selectedTab == 1 ? 1 : 0)
                    .allowsHitTesting(selectedTab == 1)

                GrammarMainView()
                    .opacity(selectedTab == 2 ? 1 : 0)
                    .allowsHitTesting(selectedTab == 2)

                VocabularyMainView()
                    .opacity(selectedTab == 3 ? 1 : 0)
                    .allowsHitTesting(selectedTab == 3)

                DictionaryView()
                    .opacity(selectedTab == 4 ? 1 : 0)
                    .allowsHitTesting(selectedTab == 4)

                ReadingMainView()
                    .opacity(selectedTab == 5 ? 1 : 0)
                    .allowsHitTesting(selectedTab == 5)
                    
                GrammarReferenceView()
                    .opacity(selectedTab == 6 ? 1 : 0)
                    .allowsHitTesting(selectedTab == 6)
            }
            .environment(tabBarManager)
            
            if !tabBarManager.isHidden {
                floatingTabBar
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.4, dampingFraction: 0.8), value: tabBarManager.isHidden)
        .onChange(of: scenePhase) { oldPhase, newPhase in
            if newPhase == .active {
                TimeTrackingService.shared.appDidBecomeActive()
            } else if oldPhase == .active && (newPhase == .inactive || newPhase == .background) {
                TimeTrackingService.shared.appWillResignActive(context: modelContext)
            }
        }
    }

    private var floatingTabBar: some View {
        HStack(spacing: 0) {
            tabButton(icon: "house.fill", title: L10n.tabHome, tag: 0)
            Spacer(minLength: 5)
            tabButton(textIcon: "あ", title: L10n.tabAlphabet, tag: 1)
            Spacer(minLength: 5)
            tabButton(icon: "doc.text.fill", title: L10n.tabGrammar, tag: 2)
            Spacer(minLength: 5)
            tabButton(icon: "character.book.closed.fill", title: L10n.tabVocabulary, tag: 3)
            Spacer(minLength: 5)
            tabButton(icon: "book.fill", title: L10n.tabReading, tag: 5)
            Spacer(minLength: 5)
            tabButton(icon: "magnifyingglass", title: L10n.tabDictionary, tag: 4)
            Spacer(minLength: 5)
            tabButton(icon: "books.vertical.fill", title: L10n.tabReference, tag: 6)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 14)
        .background(
            RoundedRectangle(cornerRadius: 30)
                .fill(Theme.paper)
                .shadow(color: Theme.ink.opacity(0.15), radius: 10, x: 0, y: 5)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 30)
                .strokeBorder(Theme.ink, lineWidth: 1.5)
        )
        .padding(.horizontal, 24)
        .padding(.bottom, 10)
    }

    private func tabButton(icon: String? = nil, textIcon: String? = nil, title: String, tag: Int) -> some View {
        Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                selectedTab = tag
            }
        } label: {
            if let textIcon {
                Text(textIcon)
                    .font(.system(size: 22, weight: selectedTab == tag ? .bold : .medium))
                    .foregroundStyle(selectedTab == tag ? Theme.accent : Theme.secondaryInk)
                    .frame(height: 30)
            } else if let icon {
                Image(systemName: icon)
                    .font(.system(size: 22, weight: selectedTab == tag ? .bold : .regular))
                    .foregroundStyle(selectedTab == tag ? Theme.accent : Theme.secondaryInk)
                    .frame(height: 30)
            }
        }
    }
}

#Preview {
    MainTabView()
        .modelContainer(for: [LearningItemProgress.self, UserProgress.self, LearningSessionState.self], inMemory: true)
}
