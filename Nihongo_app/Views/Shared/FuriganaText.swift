import SwiftUI
import CoreFoundation

/// Metin içindeki Kanji'lerin altına okunuşunu (hiragana) yerleştirir.
/// Eğer metin "漢字[かんじ]" gibi manuel etiketler içeriyorsa onları kullanır.
/// İçermiyorsa CFStringTokenizer ile otomatik olarak kanjileri tespit edip okunuşlarını bulur.
struct FuriganaText: View {
    let text: String
    var font: Font = .body
    var readingFont: Font = .caption2
    var color: Color = Theme.ink
    
    struct Token: Identifiable {
        let id = UUID()
        let kanji: String
        let reading: String?
    }
    
    private let tokens: [Token]
    
    init(text: String, font: Font = .body, readingFont: Font = .caption2, color: Color = Theme.ink) {
        self.text = text
        self.font = font
        self.readingFont = readingFont
        self.color = color
        
        if text.contains("[") && text.contains("]") {
            self.tokens = FuriganaText.parseManualTags(text)
        } else {
            self.tokens = FuriganaText.generateAutoFurigana(for: text)
        }
    }
    
    private static func parseManualTags(_ text: String) -> [Token] {
        var result: [Token] = []
        let pattern = "([一-龯]+)\\[([ぁ-んァ-ン]+)\\]"
        guard let regex = try? NSRegularExpression(pattern: pattern, options: []) else {
            return [Token(kanji: text, reading: nil)]
        }
        
        var currentIndex = text.startIndex
        let nsString = text as NSString
        let matches = regex.matches(in: text, options: [], range: NSRange(location: 0, length: nsString.length))
        
        for match in matches {
            let matchStart = text.index(text.startIndex, offsetBy: match.range.location)
            if matchStart > currentIndex {
                let prefix = String(text[currentIndex..<matchStart])
                if !prefix.isEmpty {
                    result.append(Token(kanji: prefix, reading: nil))
                }
            }
            
            let kanji = nsString.substring(with: match.range(at: 1))
            let reading = nsString.substring(with: match.range(at: 2))
            result.append(Token(kanji: kanji, reading: reading))
            
            currentIndex = text.index(text.startIndex, offsetBy: match.range.location + match.range.length)
        }
        
        if currentIndex < text.endIndex {
            let suffix = String(text[currentIndex...])
            if !suffix.isEmpty {
                result.append(Token(kanji: suffix, reading: nil))
            }
        }
        return result.isEmpty ? [Token(kanji: text, reading: nil)] : result
    }
    
    private static func generateAutoFurigana(for string: String) -> [Token] {
        let cfString = string as CFString
        let range = CFRangeMake(0, CFStringGetLength(cfString))
        let tokenizer = CFStringTokenizerCreate(kCFAllocatorDefault, cfString, range, kCFStringTokenizerUnitWordBoundary, CFLocaleCreate(kCFAllocatorDefault, CFLocaleIdentifier("ja_JP" as CFString)))
        
        var tokens: [Token] = []
        var tokenType = CFStringTokenizerAdvanceToNextToken(tokenizer)
        
        while !tokenType.isEmpty {
            let tokenRange = CFStringTokenizerGetCurrentTokenRange(tokenizer)
            let tokenString = CFStringCreateWithSubstring(kCFAllocatorDefault, cfString, tokenRange)! as String
            let latinTypeRef = CFStringTokenizerCopyCurrentTokenAttribute(tokenizer, kCFStringTokenizerAttributeLatinTranscription)
            
            var reading: String? = nil
            let hasKanji = tokenString.range(of: "\\p{Han}", options: .regularExpression) != nil
            
            if hasKanji, let latinString = latinTypeRef as? String {
                if let hiragana = latinString.applyingTransform(.latinToHiragana, reverse: false) {
                    reading = hiragana
                }
            }
            
            if let r = reading {
                // Okurigana (son ek) ve önek ayıklama
                var k = tokenString
                var read = r
                
                // Son ekleri ayıkla (örn. 会う -> あう, son 'う' ortak)
                var suffix = ""
                while k.count > 1 && read.count > 1 && k.last == read.last {
                    suffix.insert(k.removeLast(), at: suffix.startIndex)
                    read.removeLast()
                }
                
                // Ön ekleri ayıkla (örn. お茶 -> おちゃ, ilk 'お' ortak)
                var prefix = ""
                while k.count > 1 && read.count > 1 && k.first == read.first {
                    prefix.append(k.removeFirst())
                    read.removeFirst()
                }
                
                if !prefix.isEmpty {
                    tokens.append(Token(kanji: prefix, reading: nil))
                }
                tokens.append(Token(kanji: String(k), reading: String(read)))
                if !suffix.isEmpty {
                    tokens.append(Token(kanji: suffix, reading: nil))
                }
            } else {
                tokens.append(Token(kanji: tokenString, reading: nil))
            }
            
            tokenType = CFStringTokenizerAdvanceToNextToken(tokenizer)
        }
        
        return tokens
    }
    
    var body: some View {
        FlowLayout(alignment: .leading, spacing: 0) {
            ForEach(tokens) { token in
                VStack(spacing: 0) {
                    Text(token.kanji)
                        .font(font)
                        .foregroundStyle(color)
                    
                    if let reading = token.reading {
                        Text(reading)
                            .font(readingFont)
                            .foregroundStyle(color.opacity(0.7))
                    } else {
                        Text("あ")
                            .font(readingFont)
                            .opacity(0)
                    }
                }
            }
        }
    }
}

// FlowLayout for wrapping items
struct FlowLayout: Layout {
    var alignment: HorizontalAlignment = .leading
    var spacing: CGFloat = 0
    var lineSpacing: CGFloat = 4

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        // Clamp infinite width to screen width to prevent CoreAnimation texture allocation crashes
        let maxWidth = UIScreen.main.bounds.width
        let width = proposal.width ?? maxWidth
        let safeWidth = width == .infinity ? maxWidth : width
        let result = FlowResult(in: safeWidth, subviews: subviews, alignment: alignment, spacing: spacing, lineSpacing: lineSpacing)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let maxWidth = UIScreen.main.bounds.width
        let safeWidth = bounds.width == .infinity ? maxWidth : bounds.width
        let result = FlowResult(in: safeWidth, subviews: subviews, alignment: alignment, spacing: spacing, lineSpacing: lineSpacing)
        for row in result.rows {
            for element in row.elements {
                let x = bounds.minX + element.rect.minX
                let y = bounds.minY + element.rect.minY
                subviews[element.index].place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(element.rect.size))
            }
        }
    }

    struct FlowResult {
        var size: CGSize = .zero
        var rows: [Row] = []

        struct Row {
            var elements: [(index: Int, rect: CGRect)] = []
            var width: CGFloat = 0
            var height: CGFloat = 0
        }

        init(in maxWidth: CGFloat, subviews: Subviews, alignment: HorizontalAlignment, spacing: CGFloat, lineSpacing: CGFloat) {
            var currentRow = Row()
            var y: CGFloat = 0

            for (index, subview) in subviews.enumerated() {
                let size = subview.sizeThatFits(.unspecified)
                if currentRow.width + size.width > maxWidth && !currentRow.elements.isEmpty {
                    rows.append(currentRow)
                    y += currentRow.height + lineSpacing
                    currentRow = Row()
                }
                
                let rect = CGRect(x: currentRow.width, y: y, width: size.width, height: size.height)
                currentRow.elements.append((index, rect))
                currentRow.width += size.width + spacing
                currentRow.height = max(currentRow.height, size.height)
            }
            if !currentRow.elements.isEmpty {
                rows.append(currentRow)
            }
            
            for rIndex in rows.indices {
                let rowHeight = rows[rIndex].height
                for eIndex in rows[rIndex].elements.indices {
                    let h = rows[rIndex].elements[eIndex].rect.height
                    rows[rIndex].elements[eIndex].rect.origin.y += (rowHeight - h)
                }
            }
            
            size.width = rows.map(\.width).max() ?? 0
            size.height = (rows.last?.elements.first?.rect.maxY ?? 0) - (rows.first?.elements.first?.rect.minY ?? 0)
        }
    }
}
