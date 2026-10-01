import SwiftUI

struct ScenarioListView: View {
    @State private var scenarios: [ConversationScenario] = []
    @State private var selectedScenario: ConversationScenario?
    
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if scenarios.isEmpty {
                    SwiftUI.ProgressView()
                } else {
                    ForEach(scenarios) { scenario in
                        Button {
                            selectedScenario = scenario
                        } label: {
                            scenarioCard(scenario)
                        }
                    }
                }
            }
            .padding(20)
            .padding(.bottom, 80) // Tab bar
        }
        .background(Theme.paper)
        .navigationTitle(L10n.scenariosTitle)
        .navigationBarTitleDisplayMode(.inline)
        .fullScreenCover(item: $selectedScenario) { scenario in
            NavigationStack {
                ScenarioChatView(scenario: scenario)
            }
        }
        .onAppear {
            loadScenarios()
        }
    }
    
    private func scenarioCard(_ scenario: ConversationScenario) -> some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(Theme.accent.opacity(0.15))
                    .frame(width: 50, height: 50)
                Text(scenario.level)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(Theme.accent)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(scenario.title)
                    .font(Theme.heading(20))
                    .foregroundStyle(Theme.ink)
                    .lineLimit(1)
                
                Text(scenario.description)
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
                    .lineLimit(2)
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .foregroundStyle(Theme.secondaryInk)
        }
        .padding(16)
        .inkBordered()
    }
    
    private func loadScenarios() {
        guard scenarios.isEmpty else { return }
        guard let url = Bundle.main.url(forResource: "ScenariosData", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let loaded = try? JSONDecoder().decode([ConversationScenario].self, from: data) else {
            return
        }
        self.scenarios = loaded
    }
}
