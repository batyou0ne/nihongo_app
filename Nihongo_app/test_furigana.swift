import Foundation
import CoreFoundation

struct Token {
    let kanji: String
    let reading: String?
}

func generateAutoFurigana(for string: String) -> [Token] {
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
            var k = tokenString
            var read = r
            
            var suffix = ""
            while k.count > 1 && read.count > 1 && k.last == read.last {
                suffix.insert(k.removeLast(), at: suffix.startIndex)
                read.removeLast()
            }
            
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

struct VocabularyWord: Codable {
    let kanji: String
    let hiragana: String
    let romaji: String
    let turkishMeaning: String
    let exampleSentence: String?
    let exampleTranslation: String?
}

let url = URL(fileURLWithPath: "Nihongo_app/Resources/N5VocabularyData.json")
do {
    let data = try Data(contentsOf: url)
    let words = try JSONDecoder().decode([VocabularyWord].self, from: data)
    print("Testing \(words.count) words...")
    var c = 0
    for word in words {
        let prompt = word.kanji.isEmpty ? word.hiragana : word.kanji
        _ = generateAutoFurigana(for: prompt)
        c += 1
    }
    print("Success: \(c) words parsed.")
} catch {
    print("Error: \(error)")
}
