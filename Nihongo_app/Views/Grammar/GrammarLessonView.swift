import SwiftUI

/// Tek bir gramer konusunun ders ekranı. Kullanıcıya sırasıyla:
/// kısa açıklama → formül → 2-3 örnek cümle → "Pratiğe geç" gösterir.
/// (Bkz. modül tasarımındaki öğrenme akışı; içerik N5GrammarData.json'dan gelir.)
struct GrammarLessonView: View {
    let point: GrammarPoint

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                header
                formulaBox
                examplesSection

                NavigationLink {
                    GrammarPracticeView(points: [point], title: point.title)
                } label: {
                    Text("Pratiğe geç →")
                        .font(.system(size: 17, weight: .bold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(Theme.ink)
                        .foregroundStyle(Theme.paper)
                }
                .padding(.top, 4)
            }
            .padding(20)
        }
        .scrollIndicators(.hidden)
        .background(Theme.paper)
        .navigationTitle(point.title)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(point.pattern)
                .font(Theme.display(34))
                .foregroundStyle(Theme.accent)
                .minimumScaleFactor(0.5)
                .lineLimit(1)
            Text(point.romaji)
                .font(.subheadline)
                .foregroundStyle(Theme.secondaryInk)
            Text(point.explanation)
                .font(.system(size: 16))
                .foregroundStyle(Theme.ink)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var formulaBox: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Formül")
                .font(.caption.weight(.heavy))
                .foregroundStyle(Theme.secondaryInk)
            Text(point.formula)
                .font(Theme.heading(18))
                .foregroundStyle(Theme.ink)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .inkBordered()
    }

    private var examplesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Örnekler")
                .font(Theme.display(20))
                .foregroundStyle(Theme.ink)

            ForEach(point.examples, id: \.japanese) { example in
                VStack(alignment: .leading, spacing: 4) {
                    HStack(alignment: .firstTextBaseline, spacing: 10) {
                        Text(example.japanese)
                            .font(Theme.heading(20))
                            .foregroundStyle(Theme.ink)
                            .fixedSize(horizontal: false, vertical: true)
                        Spacer(minLength: 0)
                        Button {
                            AudioService.shared.speak(example.hiragana ?? example.japanese)
                        } label: {
                            Image(systemName: "speaker.wave.2.fill")
                                .foregroundStyle(Theme.accent)
                        }
                    }
                    if let hiragana = example.hiragana, !hiragana.isEmpty {
                        Text(hiragana)
                            .font(.system(size: 13))
                            .foregroundStyle(Color(uiColor: .tertiaryLabel))
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    Text(example.romaji)
                        .font(.subheadline)
                        .foregroundStyle(Theme.secondaryInk)
                    Text(example.turkish)
                        .font(.system(size: 15))
                        .foregroundStyle(Theme.ink)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .inkBordered()
            }
        }
    }
}

#Preview {
    NavigationStack {
        GrammarLessonView(point: ContentStore.loadGrammar().first ?? GrammarPoint(
            key: "demo", pattern: "～は～です", romaji: "~ wa ~ desu", title: "A wa B desu",
            explanation: "En temel cümle yapısı.", formula: "[A] + は + [B] + です",
            category: .particle, difficulty: 1,
            examples: [GrammarExample(japanese: "私は学生です。", hiragana: "わたしはがくせいです。", romaji: "watashi wa gakusei desu.", turkish: "Ben öğrenciyim.")],
            questions: []
        ))
    }
}
