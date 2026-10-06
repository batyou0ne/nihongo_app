import SwiftUI

// MARK: - Grammar Reference Detail View

/// Edat, Sıfat, Bağlaç veya İfade kategorisindeki bir gramer noktasının
/// tam başvuru sayfası. Formül, açıklama ve örnek cümleler gösterilir.
struct GrammarReferenceDetailView: View {
    let point: GrammarPoint
    let accentColor: Color

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    headerSection
                    formulaSection
                    if !point.examples.isEmpty {
                        examplesSection
                    }
                    lessonLinkSection
                }
                .padding(20)
                .padding(.bottom, 32)
            }
            .scrollIndicators(.hidden)
            .background(Theme.paper)
            .navigationTitle("Gramer Referansı")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title3)
                            .foregroundStyle(Theme.secondaryInk)
                    }
                }
            }
        }
    }

    // MARK: - Header

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(point.pattern)
                        .font(Theme.display(34))
                        .foregroundStyle(accentColor)
                        .minimumScaleFactor(0.5)
                        .lineLimit(1)
                    Text(point.romaji)
                        .font(.subheadline)
                        .foregroundStyle(Theme.secondaryInk)
                }
                Spacer()
                difficultyBadge
            }

            Text(point.title)
                .font(Theme.heading(20))
                .foregroundStyle(Theme.ink)

            Text(point.explanation)
                .font(.system(size: 15))
                .foregroundStyle(Theme.ink)
                .fixedSize(horizontal: false, vertical: true)
                .lineSpacing(4)
        }
    }

    private var difficultyBadge: some View {
        VStack(spacing: 4) {
            Text("Zorluk")
                .font(.caption2.weight(.heavy))
                .foregroundStyle(Theme.secondaryInk)
            HStack(spacing: 4) {
                ForEach(1...3, id: \.self) { i in
                    RoundedRectangle(cornerRadius: 3)
                        .fill(i <= point.difficulty ? accentColor : accentColor.opacity(0.18))
                        .frame(width: 18, height: 7)
                }
            }
        }
    }

    // MARK: - Formula

    private var formulaSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("Formül", systemImage: "function")
                .font(.caption.weight(.heavy))
                .foregroundStyle(Theme.secondaryInk)

            Text(point.formula)
                .font(Theme.heading(17))
                .foregroundStyle(Theme.ink)
                .fixedSize(horizontal: false, vertical: true)
                .lineSpacing(3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .inkBordered()
    }

    // MARK: - Examples

    private var examplesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Örnek Cümleler", systemImage: "quote.opening")
                .font(Theme.display(18))
                .foregroundStyle(Theme.ink)

            ForEach(point.examples, id: \.japanese) { example in
                VStack(alignment: .leading, spacing: 4) {
                    HStack(alignment: .firstTextBaseline) {
                        Text(example.japanese)
                            .font(Theme.heading(18))
                            .foregroundStyle(Theme.ink)
                            .fixedSize(horizontal: false, vertical: true)
                        Spacer(minLength: 0)
                        Button {
                            AudioService.shared.speak(example.hiragana ?? example.japanese)
                        } label: {
                            Image(systemName: "speaker.wave.2.fill")
                                .foregroundStyle(accentColor)
                                .font(.subheadline)
                        }
                    }
                    if let hira = example.hiragana {
                        Text(hira)
                            .font(.caption)
                            .foregroundStyle(Theme.secondaryInk)
                    }
                    Text(example.romaji)
                        .font(.caption)
                        .foregroundStyle(Theme.secondaryInk)
                    Text(example.turkish)
                        .font(.system(size: 14))
                        .foregroundStyle(Theme.ink)
                        .padding(.top, 2)
                }
                .padding(14)
                .inkBordered()
            }
        }
    }

    // MARK: - Lesson Link

    private var lessonLinkSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Divider()
            HStack {
                Image(systemName: "lightbulb.fill")
                    .foregroundStyle(accentColor)
                Text("Bu konuyu öğrenmek için Gramer bölümündeki \"\(point.title)\" dersine git.")
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryInk)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

#Preview {
    GrammarReferenceDetailView(
        point: GrammarPoint(
            key: "wa_particle",
            pattern: "～は～",
            romaji: "~ wa ~",
            title: "Konu Edatı は",
            explanation: "Cümlenin konusunu işaretler.",
            formula: "[Konu] + は + [Yüklem]",
            category: .particle,
            difficulty: 1,
            examples: [
                GrammarExample(japanese: "私は学生です。", hiragana: "わたしはがくせいです。",
                               romaji: "Watashi wa gakusei desu.", turkish: "Ben öğrenciyim.")
            ],
            questions: []
        ),
        accentColor: .blue
    )
}
