import Foundation

/// Sınav türü: Müfredatı bitirenler için "Kapı Sınavı" veya önceden bilenler için "Test-Out Challenge"
enum ExamMode: String, Identifiable {
    case gateway = "gateway"       // Standart Seviye Kapı Sınavı (%80 başarı)
    case testOut = "testOut"       // Seviye Atlama Sınavı (3 can hakkı)

    var id: String { rawValue }

    var title: String {
        switch self {
        case .gateway: return "Seviye Kapı Sınavı"
        case .testOut: return "Seviye Atlama (Test-Out) Sınavı"
        }
    }
}

/// Sınav sorusunun ait olduğu dil alanı
enum ExamQuestionType: String, CaseIterable {
    case kanji = "Kanji"
    case vocabulary = "Kelime"
    case grammar = "Gramer"

    var badgeIcon: String {
        switch self {
        case .kanji: return "character.book.closed.fill"
        case .vocabulary: return "character.bubble.fill"
        case .grammar: return "text.alignleft"
        }
    }
}

/// Sınavda gösterilen tek bir soru
struct ExamQuestion: Identifiable {
    let id: String
    let type: ExamQuestionType
    let prompt: String              // Japonca ana metin veya cümle
    let subPrompt: String?          // Okunuş, ipucu veya soru açıklaması
    let options: [String]           // 4 seçenek
    let correctAnswer: String       // Doğru şık
    let explanation: String?        // Çözüm / çeviri açıklaması
}
