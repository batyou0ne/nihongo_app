import SwiftUI

struct GrammarMainView: View {
    private let levels: [(level: String, isAvailable: Bool)] = [
        ("N5", true),
        ("N4", false),
        ("N3", false),
        ("N2", false),
        ("N1", false)
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("Gramer")
                        .font(Theme.display(36))
                        .foregroundStyle(Theme.ink)
                        .padding(.top, 10)
                        
                    VStack(spacing: 14) {
                        ForEach(levels, id: \.level) { entry in
                            if entry.isAvailable {
                                NavigationLink {
                                    GrammarPartSelectionView(level: entry.level)
                                } label: {
                                    levelRow(entry.level, isAvailable: true)
                                }
                            } else {
                                levelRow(entry.level, isAvailable: false)
                            }
                        }
                    }
                }
                .padding(20)
                .padding(.bottom, 80)
            }
            .scrollIndicators(.hidden)
            .background(Theme.paper)
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(Theme.accent)
    }

    private func levelRow(_ level: String, isAvailable: Bool) -> some View {
        HStack(spacing: 16) {
            Image(systemName: "text.alignleft")
                .font(.title2)
                .foregroundStyle(isAvailable ? Theme.accent : Theme.secondaryInk)
                .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 2) {
                Text("\(level) Gramer")
                    .font(Theme.heading(19))
                    .foregroundStyle(isAvailable ? Theme.ink : Theme.secondaryInk)
                Text(isAvailable ? "85 konu · 5 kategori" : "Yakında")
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
            }

            Spacer()
            if isAvailable {
                Image(systemName: "arrow.right")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Theme.ink)
            } else {
                Image(systemName: "lock.fill")
                    .foregroundStyle(Theme.secondaryInk)
            }
        }
        .padding()
        .inkBordered()
        .opacity(isAvailable ? 1 : 0.5)
    }
}

#Preview {
    GrammarMainView()
}
