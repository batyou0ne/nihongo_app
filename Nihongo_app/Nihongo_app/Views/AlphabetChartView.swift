import SwiftUI

/// Tek bir alfabe karakterinin model yapısı
struct AlphabetChartItem: Identifiable, Hashable {
    var id: String { character }
    let character: String
    let romaji: String
    let pronunciation: String
}

/// 5 sütunlu (a, i, u, e, o) satır yapısı
struct AlphabetChartRow: Identifiable {
    var id: String { header }
    let header: String
    let cells: [AlphabetChartItem?] // 5 eleman (boşluklar nil)
}

/// Hiragana ve Katakana için etkileşimli, sesli alfabe tablosu görünümü.
/// Her karaktere dokunulduğunda `AlphabetAudioService` aracılığıyla sesli telaffuz çalar.
struct AlphabetChartView: View {
    @State private var selectedAlphabet: CharacterType = .hiragana
    @State private var selectedCategory: AlphabetCategory = .basic
    @State private var lastTappedItem: AlphabetChartItem? = nil
    
    enum AlphabetCategory: String, CaseIterable {
        case basic
        case dakuten
        
        var title: String {
            switch self {
            case .basic: return L10n.basicAlphabetTab
            case .dakuten: return L10n.dakutenAlphabetTab
            }
        }
    }
    
    private let columns = Array(repeating: GridItem(.flexible(), spacing: 8), count: 5)
    private let columnHeaders = ["a", "i", "u", "e", "o"]
    
    var body: some View {
        VStack(spacing: 16) {
            // MARK: - Üst Seçici Kontrolleri
            VStack(spacing: 12) {
                // Hiragana / Katakana Değiştirici
                Picker("Alfabe", selection: $selectedAlphabet) {
                    Text("Hiragana (あ)").tag(CharacterType.hiragana)
                    Text("Katakana (ア)").tag(CharacterType.katakana)
                }
                .pickerStyle(.segmented)
                
                // Temel / Dakuten Değiştirici
                HStack(spacing: 8) {
                    ForEach(AlphabetCategory.allCases, id: \.self) { category in
                        Button {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                selectedCategory = category
                            }
                        } label: {
                            Text(category.title)
                                .font(Theme.heading(13))
                                .foregroundStyle(selectedCategory == category ? Theme.paper : Theme.ink)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(
                                    Capsule()
                                        .fill(selectedCategory == category ? Theme.accent : Theme.paper)
                                )
                                .overlay(
                                    Capsule()
                                        .strokeBorder(selectedCategory == category ? Color.clear : Theme.ink.opacity(0.15), lineWidth: 1)
                                )
                        }
                    }
                    Spacer()
                }
            }
            .padding(.horizontal)
            
            // MARK: - Son Dokunulan Karakter Bilgi Çubuğu
            if let item = lastTappedItem {
                HStack(spacing: 12) {
                    Text(item.character)
                        .font(Theme.display(28))
                        .foregroundStyle(Theme.accent)
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.romaji)
                            .font(Theme.heading(15))
                            .foregroundStyle(Theme.ink)
                        Text(item.pronunciation)
                            .font(.caption)
                            .foregroundStyle(Theme.secondaryInk)
                    }
                    
                    Spacer()
                    
                    Button {
                        AlphabetAudioService.shared.play(character: item.character)
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "speaker.wave.2.fill")
                            Text("Dinle")
                        }
                        .font(Theme.heading(13))
                        .foregroundStyle(Theme.paper)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Capsule().fill(Theme.accent))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Theme.paper)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .strokeBorder(Theme.accent.opacity(0.3), lineWidth: 1.5)
                        )
                )
                .padding(.horizontal)
                .transition(.opacity.combined(with: .scale(scale: 0.95)))
            } else {
                HStack(spacing: 8) {
                    Image(systemName: "hand.tap.fill")
                        .foregroundStyle(Theme.accent)
                    Text(L10n.alphabetChartSubtitle)
                        .font(.footnote)
                        .foregroundStyle(Theme.secondaryInk)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal)
            }
            
            // MARK: - Sütun Başlıkları (a, i, u, e, o)
            HStack(spacing: 8) {
                // Satır başlığı boşluğu (K, S, T hizası için)
                Text("")
                    .frame(width: 24)
                
                ForEach(columnHeaders, id: \.self) { header in
                    Text(header.uppercased())
                        .font(Theme.heading(13))
                        .foregroundStyle(Theme.secondaryInk)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal)
            
            // MARK: - Alfabe Tablosu Izgarası
            ScrollView {
                VStack(spacing: 10) {
                    ForEach(currentRows) { row in
                        HStack(spacing: 8) {
                            // Satır etiketi (ör. K, S, T)
                            Text(row.header)
                                .font(Theme.heading(12))
                                .foregroundStyle(Theme.secondaryInk)
                                .frame(width: 24, alignment: .center)
                            
                            // 5 Sütun
                            ForEach(0..<5, id: \.self) { colIndex in
                                if let item = row.cells[colIndex] {
                                    AlphabetCellView(item: item) {
                                        lastTappedItem = item
                                        AlphabetAudioService.shared.play(character: item.character)
                                    }
                                } else {
                                    // Boşluk hücresi (ör. Ya/Wa satırlarındaki boşluklar)
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(Theme.paper.opacity(0.3))
                                        .frame(height: 66)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 12)
                                                .strokeBorder(Theme.ink.opacity(0.05), style: StrokeStyle(lineWidth: 1, dash: [4]))
                                        )
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 90)
            }
        }
        .background(Theme.paper)
    }
    
    // MARK: - Satır Verileri
    
    private var currentRows: [AlphabetChartRow] {
        switch (selectedAlphabet, selectedCategory) {
        case (.hiragana, .basic):
            return hiraganaBasicRows
        case (.hiragana, .dakuten):
            return hiraganaDakutenRows
        case (.katakana, .basic):
            return katakanaBasicRows
        case (.katakana, .dakuten):
            return katakanaDakutenRows
        }
    }
    
    // MARK: - Hiragana Temel (46)
    private var hiraganaBasicRows: [AlphabetChartRow] {
        [
            AlphabetChartRow(header: "A", cells: [
                AlphabetChartItem(character: "あ", romaji: "a", pronunciation: "a"),
                AlphabetChartItem(character: "い", romaji: "i", pronunciation: "i"),
                AlphabetChartItem(character: "う", romaji: "u", pronunciation: "u"),
                AlphabetChartItem(character: "え", romaji: "e", pronunciation: "e"),
                AlphabetChartItem(character: "お", romaji: "o", pronunciation: "o")
            ]),
            AlphabetChartRow(header: "K", cells: [
                AlphabetChartItem(character: "か", romaji: "ka", pronunciation: "ka"),
                AlphabetChartItem(character: "き", romaji: "ki", pronunciation: "ki"),
                AlphabetChartItem(character: "く", romaji: "ku", pronunciation: "ku"),
                AlphabetChartItem(character: "け", romaji: "ke", pronunciation: "ke"),
                AlphabetChartItem(character: "こ", romaji: "ko", pronunciation: "ko")
            ]),
            AlphabetChartRow(header: "S", cells: [
                AlphabetChartItem(character: "さ", romaji: "sa", pronunciation: "sa"),
                AlphabetChartItem(character: "し", romaji: "shi", pronunciation: "şi"),
                AlphabetChartItem(character: "す", romaji: "su", pronunciation: "su"),
                AlphabetChartItem(character: "せ", romaji: "se", pronunciation: "se"),
                AlphabetChartItem(character: "そ", romaji: "so", pronunciation: "so")
            ]),
            AlphabetChartRow(header: "T", cells: [
                AlphabetChartItem(character: "た", romaji: "ta", pronunciation: "ta"),
                AlphabetChartItem(character: "ち", romaji: "chi", pronunciation: "çi"),
                AlphabetChartItem(character: "つ", romaji: "tsu", pronunciation: "tsu"),
                AlphabetChartItem(character: "て", romaji: "te", pronunciation: "te"),
                AlphabetChartItem(character: "と", romaji: "to", pronunciation: "to")
            ]),
            AlphabetChartRow(header: "N", cells: [
                AlphabetChartItem(character: "な", romaji: "na", pronunciation: "na"),
                AlphabetChartItem(character: "に", romaji: "ni", pronunciation: "ni"),
                AlphabetChartItem(character: "ぬ", romaji: "nu", pronunciation: "nu"),
                AlphabetChartItem(character: "ね", romaji: "ne", pronunciation: "ne"),
                AlphabetChartItem(character: "の", romaji: "no", pronunciation: "no")
            ]),
            AlphabetChartRow(header: "H", cells: [
                AlphabetChartItem(character: "は", romaji: "ha", pronunciation: "ha"),
                AlphabetChartItem(character: "ひ", romaji: "hi", pronunciation: "hi"),
                AlphabetChartItem(character: "ふ", romaji: "fu", pronunciation: "fu"),
                AlphabetChartItem(character: "へ", romaji: "he", pronunciation: "he"),
                AlphabetChartItem(character: "ほ", romaji: "ho", pronunciation: "ho")
            ]),
            AlphabetChartRow(header: "M", cells: [
                AlphabetChartItem(character: "ま", romaji: "ma", pronunciation: "ma"),
                AlphabetChartItem(character: "み", romaji: "mi", pronunciation: "mi"),
                AlphabetChartItem(character: "む", romaji: "mu", pronunciation: "mu"),
                AlphabetChartItem(character: "め", romaji: "me", pronunciation: "me"),
                AlphabetChartItem(character: "も", romaji: "mo", pronunciation: "mo")
            ]),
            AlphabetChartRow(header: "Y", cells: [
                AlphabetChartItem(character: "や", romaji: "ya", pronunciation: "ya"),
                nil,
                AlphabetChartItem(character: "ゆ", romaji: "yu", pronunciation: "yu"),
                nil,
                AlphabetChartItem(character: "よ", romaji: "yo", pronunciation: "yo")
            ]),
            AlphabetChartRow(header: "R", cells: [
                AlphabetChartItem(character: "ら", romaji: "ra", pronunciation: "ra"),
                AlphabetChartItem(character: "り", romaji: "ri", pronunciation: "ri"),
                AlphabetChartItem(character: "る", romaji: "ru", pronunciation: "ru"),
                AlphabetChartItem(character: "れ", romaji: "re", pronunciation: "re"),
                AlphabetChartItem(character: "ろ", romaji: "ro", pronunciation: "ro")
            ]),
            AlphabetChartRow(header: "W", cells: [
                AlphabetChartItem(character: "わ", romaji: "wa", pronunciation: "va"),
                nil,
                nil,
                nil,
                AlphabetChartItem(character: "を", romaji: "wo", pronunciation: "o")
            ]),
            AlphabetChartRow(header: "N*", cells: [
                AlphabetChartItem(character: "ん", romaji: "n", pronunciation: "n"),
                nil,
                nil,
                nil,
                nil
            ])
        ]
    }
    
    // MARK: - Hiragana Dakuten & Handakuten (25)
    private var hiraganaDakutenRows: [AlphabetChartRow] {
        [
            AlphabetChartRow(header: "G", cells: [
                AlphabetChartItem(character: "が", romaji: "ga", pronunciation: "ga"),
                AlphabetChartItem(character: "ぎ", romaji: "gi", pronunciation: "gi"),
                AlphabetChartItem(character: "ぐ", romaji: "gu", pronunciation: "gu"),
                AlphabetChartItem(character: "げ", romaji: "ge", pronunciation: "ge"),
                AlphabetChartItem(character: "ご", romaji: "go", pronunciation: "go")
            ]),
            AlphabetChartRow(header: "Z", cells: [
                AlphabetChartItem(character: "ざ", romaji: "za", pronunciation: "za"),
                AlphabetChartItem(character: "じ", romaji: "ji", pronunciation: "ci"),
                AlphabetChartItem(character: "ず", romaji: "zu", pronunciation: "zu"),
                AlphabetChartItem(character: "ぜ", romaji: "ze", pronunciation: "ze"),
                AlphabetChartItem(character: "ぞ", romaji: "zo", pronunciation: "zo")
            ]),
            AlphabetChartRow(header: "D", cells: [
                AlphabetChartItem(character: "だ", romaji: "da", pronunciation: "da"),
                AlphabetChartItem(character: "ぢ", romaji: "ji", pronunciation: "ci"),
                AlphabetChartItem(character: "づ", romaji: "zu", pronunciation: "zu"),
                AlphabetChartItem(character: "で", romaji: "de", pronunciation: "de"),
                AlphabetChartItem(character: "ど", romaji: "do", pronunciation: "do")
            ]),
            AlphabetChartRow(header: "B", cells: [
                AlphabetChartItem(character: "ば", romaji: "ba", pronunciation: "ba"),
                AlphabetChartItem(character: "び", romaji: "bi", pronunciation: "bi"),
                AlphabetChartItem(character: "ぶ", romaji: "bu", pronunciation: "bu"),
                AlphabetChartItem(character: "べ", romaji: "be", pronunciation: "be"),
                AlphabetChartItem(character: "ぼ", romaji: "bo", pronunciation: "bo")
            ]),
            AlphabetChartRow(header: "P", cells: [
                AlphabetChartItem(character: "ぱ", romaji: "pa", pronunciation: "pa"),
                AlphabetChartItem(character: "ぴ", romaji: "pi", pronunciation: "pi"),
                AlphabetChartItem(character: "ぷ", romaji: "pu", pronunciation: "pu"),
                AlphabetChartItem(character: "ぺ", romaji: "pe", pronunciation: "pe"),
                AlphabetChartItem(character: "ぽ", romaji: "po", pronunciation: "po")
            ])
        ]
    }
    
    // MARK: - Katakana Temel (46)
    private var katakanaBasicRows: [AlphabetChartRow] {
        [
            AlphabetChartRow(header: "A", cells: [
                AlphabetChartItem(character: "ア", romaji: "a", pronunciation: "a"),
                AlphabetChartItem(character: "イ", romaji: "i", pronunciation: "i"),
                AlphabetChartItem(character: "ウ", romaji: "u", pronunciation: "u"),
                AlphabetChartItem(character: "エ", romaji: "e", pronunciation: "e"),
                AlphabetChartItem(character: "オ", romaji: "o", pronunciation: "o")
            ]),
            AlphabetChartRow(header: "K", cells: [
                AlphabetChartItem(character: "カ", romaji: "ka", pronunciation: "ka"),
                AlphabetChartItem(character: "キ", romaji: "ki", pronunciation: "ki"),
                AlphabetChartItem(character: "ク", romaji: "ku", pronunciation: "ku"),
                AlphabetChartItem(character: "ケ", romaji: "ke", pronunciation: "ke"),
                AlphabetChartItem(character: "コ", romaji: "ko", pronunciation: "ko")
            ]),
            AlphabetChartRow(header: "S", cells: [
                AlphabetChartItem(character: "サ", romaji: "sa", pronunciation: "sa"),
                AlphabetChartItem(character: "シ", romaji: "shi", pronunciation: "şi"),
                AlphabetChartItem(character: "ス", romaji: "su", pronunciation: "su"),
                AlphabetChartItem(character: "セ", romaji: "se", pronunciation: "se"),
                AlphabetChartItem(character: "ソ", romaji: "so", pronunciation: "so")
            ]),
            AlphabetChartRow(header: "T", cells: [
                AlphabetChartItem(character: "タ", romaji: "ta", pronunciation: "ta"),
                AlphabetChartItem(character: "チ", romaji: "chi", pronunciation: "çi"),
                AlphabetChartItem(character: "ツ", romaji: "tsu", pronunciation: "tsu"),
                AlphabetChartItem(character: "テ", romaji: "te", pronunciation: "te"),
                AlphabetChartItem(character: "ト", romaji: "to", pronunciation: "to")
            ]),
            AlphabetChartRow(header: "N", cells: [
                AlphabetChartItem(character: "ナ", romaji: "na", pronunciation: "na"),
                AlphabetChartItem(character: "ニ", romaji: "ni", pronunciation: "ni"),
                AlphabetChartItem(character: "ヌ", romaji: "nu", pronunciation: "nu"),
                AlphabetChartItem(character: "ネ", romaji: "ne", pronunciation: "ne"),
                AlphabetChartItem(character: "ノ", romaji: "no", pronunciation: "no")
            ]),
            AlphabetChartRow(header: "H", cells: [
                AlphabetChartItem(character: "ハ", romaji: "ha", pronunciation: "ha"),
                AlphabetChartItem(character: "ヒ", romaji: "hi", pronunciation: "hi"),
                AlphabetChartItem(character: "フ", romaji: "fu", pronunciation: "fu"),
                AlphabetChartItem(character: "ヘ", romaji: "he", pronunciation: "he"),
                AlphabetChartItem(character: "ホ", romaji: "ho", pronunciation: "ho")
            ]),
            AlphabetChartRow(header: "M", cells: [
                AlphabetChartItem(character: "マ", romaji: "ma", pronunciation: "ma"),
                AlphabetChartItem(character: "ミ", romaji: "mi", pronunciation: "mi"),
                AlphabetChartItem(character: "ム", romaji: "mu", pronunciation: "mu"),
                AlphabetChartItem(character: "メ", romaji: "me", pronunciation: "me"),
                AlphabetChartItem(character: "モ", romaji: "mo", pronunciation: "mo")
            ]),
            AlphabetChartRow(header: "Y", cells: [
                AlphabetChartItem(character: "ヤ", romaji: "ya", pronunciation: "ya"),
                nil,
                AlphabetChartItem(character: "ユ", romaji: "yu", pronunciation: "yu"),
                nil,
                AlphabetChartItem(character: "ヨ", romaji: "yo", pronunciation: "yo")
            ]),
            AlphabetChartRow(header: "R", cells: [
                AlphabetChartItem(character: "ラ", romaji: "ra", pronunciation: "ra"),
                AlphabetChartItem(character: "リ", romaji: "ri", pronunciation: "ri"),
                AlphabetChartItem(character: "ル", romaji: "ru", pronunciation: "ru"),
                AlphabetChartItem(character: "レ", romaji: "re", pronunciation: "re"),
                AlphabetChartItem(character: "ロ", romaji: "ro", pronunciation: "ro")
            ]),
            AlphabetChartRow(header: "W", cells: [
                AlphabetChartItem(character: "ワ", romaji: "wa", pronunciation: "va"),
                nil,
                nil,
                nil,
                AlphabetChartItem(character: "ヲ", romaji: "wo", pronunciation: "o")
            ]),
            AlphabetChartRow(header: "N*", cells: [
                AlphabetChartItem(character: "ン", romaji: "n", pronunciation: "n"),
                nil,
                nil,
                nil,
                nil
            ])
        ]
    }
    
    // MARK: - Katakana Dakuten & Handakuten (25)
    private var katakanaDakutenRows: [AlphabetChartRow] {
        [
            AlphabetChartRow(header: "G", cells: [
                AlphabetChartItem(character: "ガ", romaji: "ga", pronunciation: "ga"),
                AlphabetChartItem(character: "ギ", romaji: "gi", pronunciation: "gi"),
                AlphabetChartItem(character: "グ", romaji: "gu", pronunciation: "gu"),
                AlphabetChartItem(character: "ゲ", romaji: "ge", pronunciation: "ge"),
                AlphabetChartItem(character: "ゴ", romaji: "go", pronunciation: "go")
            ]),
            AlphabetChartRow(header: "Z", cells: [
                AlphabetChartItem(character: "ザ", romaji: "za", pronunciation: "za"),
                AlphabetChartItem(character: "ジ", romaji: "ji", pronunciation: "ci"),
                AlphabetChartItem(character: "ズ", romaji: "zu", pronunciation: "zu"),
                AlphabetChartItem(character: "ゼ", romaji: "ze", pronunciation: "ze"),
                AlphabetChartItem(character: "ゾ", romaji: "zo", pronunciation: "zo")
            ]),
            AlphabetChartRow(header: "D", cells: [
                AlphabetChartItem(character: "ダ", romaji: "da", pronunciation: "da"),
                AlphabetChartItem(character: "ヂ", romaji: "ji", pronunciation: "ci"),
                AlphabetChartItem(character: "ヅ", romaji: "zu", pronunciation: "zu"),
                AlphabetChartItem(character: "デ", romaji: "de", pronunciation: "de"),
                AlphabetChartItem(character: "ド", romaji: "do", pronunciation: "do")
            ]),
            AlphabetChartRow(header: "B", cells: [
                AlphabetChartItem(character: "バ", romaji: "ba", pronunciation: "ba"),
                AlphabetChartItem(character: "ビ", romaji: "bi", pronunciation: "bi"),
                AlphabetChartItem(character: "ブ", romaji: "bu", pronunciation: "bu"),
                AlphabetChartItem(character: "ベ", romaji: "be", pronunciation: "be"),
                AlphabetChartItem(character: "ボ", romaji: "bo", pronunciation: "bo")
            ]),
            AlphabetChartRow(header: "P", cells: [
                AlphabetChartItem(character: "パ", romaji: "pa", pronunciation: "pa"),
                AlphabetChartItem(character: "ピ", romaji: "pi", pronunciation: "pi"),
                AlphabetChartItem(character: "プ", romaji: "pu", pronunciation: "pu"),
                AlphabetChartItem(character: "ペ", romaji: "pe", pronunciation: "pe"),
                AlphabetChartItem(character: "ポ", romaji: "po", pronunciation: "po")
            ])
        ]
    }
}

/// Tek bir karakter kutucuğu (hücresi)
private struct AlphabetCellView: View {
    let item: AlphabetChartItem
    let onTap: () -> Void
    
    private var isCurrentlyPlaying: Bool {
        AlphabetAudioService.shared.currentCharacter == item.character
    }
    
    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                Text(item.character)
                    .font(Theme.display(24))
                    .foregroundStyle(isCurrentlyPlaying ? Theme.accent : Theme.ink)
                
                Text(item.romaji)
                    .font(Theme.heading(12))
                    .foregroundStyle(isCurrentlyPlaying ? Theme.accent : Theme.secondaryInk)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 66)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isCurrentlyPlaying ? Theme.accent.opacity(0.12) : Theme.paper)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(
                        isCurrentlyPlaying ? Theme.accent : Theme.ink.opacity(0.12),
                        lineWidth: isCurrentlyPlaying ? 2 : 1
                    )
            )
            .scaleEffect(isCurrentlyPlaying ? 1.06 : 1.0)
            .animation(.spring(response: 0.25, dampingFraction: 0.6), value: isCurrentlyPlaying)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    AlphabetChartView()
}
