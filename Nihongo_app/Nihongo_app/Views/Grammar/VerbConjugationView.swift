import SwiftUI

// MARK: - Verb Conjugation Sheet

/// Fiil çekim tablosunu gösterin sayfa (sheet). GrammarLessonView ve
/// VerbLibraryView'dan tetiklenebilir. Sözlük formu verilen bir fiil için
/// tüm N5 düzeyindeki çekim formlarını kart tablosunda sunar.
struct VerbConjugationView: View {
    let hiragana: String
    let kanji: String?
    let turkishMeaning: String?

    @State private var result: VerbConjugationResult? = nil
    @State private var selectedGroup: FormGroup = .polite
    @Environment(\.dismiss) private var dismiss

    enum FormGroup: String, CaseIterable {
        case polite  = "Kibar"
        case plain   = "Plain"
        case special = "Özel"

        var formIDs: [String] {
            switch self {
            case .polite:  return ["masu", "masen", "mashita", "masen_deshita", "mashou"]
            case .plain:   return ["dict", "ta", "nai", "potential"]
            case .special: return ["te"]
            }
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Theme.paper.ignoresSafeArea()

                if let result {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 24) {
                            headerCard(result)
                            groupPicker
                            formGrid(result)
                            allFormsSection(result)
                        }
                        .padding(20)
                        .padding(.bottom, 32)
                    }
                    .scrollIndicators(.hidden)
                } else {
                    ProgressView()
                }
            }
            .navigationTitle("Fiil Çekim Tablosu")
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
        .onAppear {
            result = VerbConjugationEngine.conjugate(hiragana: hiragana, kanji: kanji)
        }
    }

    // MARK: - Header Card

    private func headerCard(_ result: VerbConjugationResult) -> some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 6) {
                if let kanji = result.kanji {
                    Text(kanji)
                        .font(Theme.display(36))
                        .foregroundStyle(Theme.ink)
                    Text(result.dictionaryForm)
                        .font(Theme.heading(18))
                        .foregroundStyle(Theme.secondaryInk)
                } else {
                    Text(result.dictionaryForm)
                        .font(Theme.display(36))
                        .foregroundStyle(Theme.ink)
                }
                if let meaning = turkishMeaning {
                    Text(meaning)
                        .font(.system(size: 15))
                        .foregroundStyle(Theme.secondaryInk)
                        .padding(.top, 2)
                }
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                verbTypeBadge(result.verbType)
                if result.verbType.isGodan {
                    Text("Grup 1")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(Theme.secondaryInk)
                } else if result.verbType == .ichidan {
                    Text("Grup 2")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(Theme.secondaryInk)
                }
            }
        }
        .padding(20)
        .inkBordered()
    }

    private func verbTypeBadge(_ type: VerbType) -> some View {
        Text(type.displayName)
            .font(.caption.weight(.bold))
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(accentForType(type).opacity(0.15))
            .foregroundStyle(accentForType(type))
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private func accentForType(_ type: VerbType) -> Color {
        switch type {
        case .ichidan:          return Color(red: 0.13, green: 0.53, blue: 0.90)
        case .suru, .kuru, .aru: return Color(red: 0.60, green: 0.20, blue: 0.80)
        default:                return Theme.accent
        }
    }

    // MARK: - Group Picker

    private var groupPicker: some View {
        HStack(spacing: 0) {
            ForEach(FormGroup.allCases, id: \.self) { group in
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        selectedGroup = group
                    }
                } label: {
                    Text(group.rawValue)
                        .font(.system(size: 14, weight: .semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(selectedGroup == group ? Theme.ink : Color.clear)
                        .foregroundStyle(selectedGroup == group ? Theme.paper : Theme.secondaryInk)
                }
            }
        }
        .inkBordered()
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Form Grid (selected group)

    private func formGrid(_ result: VerbConjugationResult) -> some View {
        let ids = selectedGroup.formIDs
        let forms = result.forms.filter { ids.contains($0.id) }

        return VStack(spacing: 12) {
            ForEach(forms) { form in
                formRow(form)
                    .transition(.opacity.combined(with: .move(edge: .trailing)))
            }
        }
    }

    private func formRow(_ form: ConjugationForm) -> some View {
        HStack(alignment: .top, spacing: 16) {
            // Left: label column
            VStack(alignment: .leading, spacing: 3) {
                Text(form.label)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Theme.ink)
                Text(form.explanation)
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryInk)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Divider()
                .frame(width: 1)
                .background(Theme.ink.opacity(0.15))

            // Right: Japanese form
            VStack(alignment: .trailing, spacing: 3) {
                Text(form.japanese)
                    .font(Theme.heading(20))
                    .foregroundStyle(Theme.accent)
                    .multilineTextAlignment(.trailing)
                Text(form.romaji)
                    .font(.caption)
                    .foregroundStyle(Theme.secondaryInk)
            }
            .frame(width: 130, alignment: .trailing)
        }
        .padding(14)
        .inkBordered()
    }

    // MARK: - Full Reference Table

    private func allFormsSection(_ result: VerbConjugationResult) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Tam Çekim Tablosu")
                    .font(Theme.display(18))
                    .foregroundStyle(Theme.ink)
                Spacer()
                Image(systemName: "tablecells")
                    .foregroundStyle(Theme.secondaryInk)
            }

            VStack(spacing: 0) {
                // Header row
                HStack {
                    Text("Form")
                        .font(.caption.weight(.heavy))
                        .foregroundStyle(Theme.secondaryInk)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text("Japonca")
                        .font(.caption.weight(.heavy))
                        .foregroundStyle(Theme.secondaryInk)
                        .frame(width: 140, alignment: .trailing)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Theme.ink.opacity(0.06))

                Divider()

                ForEach(Array(result.forms.enumerated()), id: \.element.id) { idx, form in
                    VStack(spacing: 0) {
                        HStack(alignment: .center, spacing: 8) {
                            Text(form.label)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(Theme.ink)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Text(form.japanese)
                                .font(Theme.heading(17))
                                .foregroundStyle(Theme.accent)
                                .frame(width: 140, alignment: .trailing)
                                .multilineTextAlignment(.trailing)
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(idx % 2 == 0 ? Color.clear : Theme.ink.opacity(0.03))

                        if idx < result.forms.count - 1 {
                            Divider()
                                .padding(.leading, 14)
                        }
                    }
                }
            }
            .inkBordered()
        }
    }
}

// MARK: - Preview

#Preview {
    VerbConjugationView(
        hiragana: "たべる",
        kanji: "食べる",
        turkishMeaning: "yemek"
    )
}
