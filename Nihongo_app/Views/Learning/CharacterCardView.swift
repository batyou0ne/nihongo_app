import SwiftUI

/// Tek bir hiragana/katakana kartı. Öne Japonca karakteri büyük gösterir,
/// dokununca 3B flip animasyonuyla arkaya döner ve romaji + Türkçe okunuşu gösterir.
struct CharacterCardView: View {
    let character: JapaneseCharacter
    var accentColor: Color = Theme.accent

    @State private var isFlipped = false

    var body: some View {
        ZStack {
            front
                .opacity(isFlipped ? 0 : 1)
            back
                .opacity(isFlipped ? 1 : 0)
                .rotation3DEffect(.degrees(180), axis: (x: 0, y: 1, z: 0))
        }
        .rotation3DEffect(.degrees(isFlipped ? 180 : 0), axis: (x: 0, y: 1, z: 0))
        .onTapGesture {
            withAnimation(.spring(response: 0.45, dampingFraction: 0.75)) {
                isFlipped.toggle()
            }
        }
        .frame(width: 160, height: 200)
    }

    private var front: some View {
        cardBackground {
            VStack(spacing: 8) {
                Text(character.character)
                    .font(Theme.display(72))
                    .foregroundStyle(Theme.ink)
                Button {
                    AudioService.shared.speak(character.character)
                } label: {
                    Image(systemName: "speaker.wave.2.fill")
                        .foregroundStyle(accentColor)
                }
            }
        }
    }

    private var back: some View {
        cardBackground {
            VStack(spacing: 6) {
                Text(character.romaji)
                    .font(Theme.heading(28))
                Text(character.turkishPronunciation)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func cardBackground<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        Rectangle()
            .fill(Theme.paper)
            .overlay(Rectangle().strokeBorder(Theme.accent, lineWidth: 2))
            .overlay(content())
    }
}

#Preview {
    CharacterCardView(
        character: JapaneseCharacter(character: "あ", romaji: "a", turkishPronunciation: "a", row: "a", strokeCount: 3),
        accentColor: .red
    )
    .padding()
}
