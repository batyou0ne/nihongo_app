import Foundation
import CoreFoundation

func furiganaTokens(for string: String) -> [(String, String?)] {
    let cfString = string as CFString
    let range = CFRangeMake(0, CFStringGetLength(cfString))
    
    // Create tokenizer
    let tokenizer = CFStringTokenizerCreate(kCFAllocatorDefault, cfString, range, kCFStringTokenizerUnitWordBoundary, CFLocaleCreate(kCFAllocatorDefault, CFLocaleIdentifier("ja_JP" as CFString)))
    
    var tokens: [(String, String?)] = []
    
    var tokenType = CFStringTokenizerAdvanceToNextToken(tokenizer)
    while !tokenType.isEmpty {
        let tokenRange = CFStringTokenizerGetCurrentTokenRange(tokenizer)
        let tokenString = CFStringCreateWithSubstring(kCFAllocatorDefault, cfString, tokenRange)! as String
        
        let latinTypeRef = CFStringTokenizerCopyCurrentTokenAttribute(tokenizer, kCFStringTokenizerAttributeLatinTranscription)
        
        var reading: String? = nil
        if let latinString = latinTypeRef as? String {
            // Convert Romaji to Hiragana
            if let hiragana = latinString.applyingTransform(.latinToHiragana, reverse: false) {
                // Sadece Kanji içeren tokenlara reading ekle
                let hasKanji = tokenString.range(of: "\\p{Han}", options: .regularExpression) != nil
                if hasKanji {
                    reading = hiragana
                }
            }
        }
        
        tokens.append((tokenString, reading))
        
        tokenType = CFStringTokenizerAdvanceToNextToken(tokenizer)
    }
    
    return tokens
}

let result = furiganaTokens(for: "私は学生です。")
print(result)
let result2 = furiganaTokens(for: "明日、友達と会う予定です。")
print(result2)
