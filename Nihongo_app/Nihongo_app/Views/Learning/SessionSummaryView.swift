import SwiftUI

/// Özet ekranındaki tek bir "yanlış yapılan öğe" satırının verisi.
struct SessionSummaryItem: Identifiable {
    let id: String
    let prompt: String
    let detail: String
    let wrongCount: Int
}

/// Bir modül oturumu (tüm tekrar turları dahil) bittiğinde gösterilen özet.
/// Hiragana/Katakana/Kanji/Kelimeler — hepsi FlashcardSessionView üzerinden
/// bu ortak bileşeni kullanır.
struct SessionSummaryView: View {
    let totalAnswers: Int
    let wrongItems: [SessionSummaryItem]
    let onFinish: () -> Void

    private var totalWrong: Int {
        wrongItems.reduce(0) { $0 + $1.wrongCount }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: 44))
                    .foregroundStyle(Theme.accent)
                Text(L10n.sessionSummary)
                    .font(Theme.display(32))
                    .foregroundStyle(Theme.ink)
            }

            HStack(spacing: 14) {
                statBox(value: totalAnswers, label: L10n.cardsSeenLabel)
                statBox(value: totalWrong, label: L10n.wrongAnswersLabel)
            }

            if wrongItems.isEmpty {
                Text(L10n.perfectScore)
                    .font(Theme.heading(17))
                    .foregroundStyle(Theme.ink)
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    Text(L10n.wrongItemsTitle)
                        .font(Theme.heading(19))
                        .foregroundStyle(Theme.ink)

                    ScrollView {
                        VStack(spacing: 10) {
                            ForEach(wrongItems) { item in
                                wrongItemRow(item)
                            }
                        }
                    }
                    .scrollIndicators(.hidden)

                    Text(L10n.addedToReview)
                        .font(.footnote)
                        .foregroundStyle(Theme.secondaryInk)
                }
            }

            Spacer(minLength: 0)

            Button(L10n.finishButton) { onFinish() }
                .buttonStyle(PrimaryButtonStyle())
        }
        .padding(20)
        .background(Theme.paper)
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

    private func wrongItemRow(_ item: SessionSummaryItem) -> some View {
        HStack(spacing: 14) {
            Text(item.prompt)
                .font(Theme.heading(26))
                .foregroundStyle(Theme.ink)
                .lineLimit(1)
                .minimumScaleFactor(0.5)

            VStack(alignment: .leading, spacing: 2) {
                Text(item.detail)
                    .font(.subheadline)
                    .foregroundStyle(Theme.secondaryInk)
                    .lineLimit(2)
            }

            Spacer()

            Text("\(item.wrongCount)×")
                .font(Theme.heading(17))
                .foregroundStyle(Theme.accent)
        }
        .padding(12)
        .inkBordered()
    }
}

#Preview {
    SessionSummaryView(
        totalAnswers: 52,
        wrongItems: [
            SessionSummaryItem(id: "あ", prompt: "あ", detail: "あ = a · a", wrongCount: 3),
            SessionSummaryItem(id: "ぬ", prompt: "ぬ", detail: "ぬ = nu · nu", wrongCount: 2)
        ],
        onFinish: {}
    )
}
