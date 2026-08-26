import Foundation
import SwiftData

/// Bir `LearningItemProgress` kaydının hangi içerik türüne ait olduğunu belirtir.
/// Hem karakterler hem kelimeler aynı spaced-repetition modelini paylaşır,
/// bu yüzden tek bir tabloda tutulup türe göre ayrıştırılır.
enum LearnableItemKind: String, Codable {
    case hiraganaCharacter
    case katakanaCharacter
    case vocabularyWord // N5 kelime modülü (VocabularyWord) tarafından kullanılır
    case kanji

    var displayName: String {
        switch self {
        case .hiraganaCharacter: return "Hiragana"
        case .katakanaCharacter: return "Katakana"
        case .vocabularyWord: return "Kelimeler"
        case .kanji: return "Kanji"
        }
    }
}

/// Tek bir öğrenilebilir öğenin (karakter/kanji) spaced-repetition durumu.
/// `itemID` alanı JapaneseCharacter.id ya da Kanji.id ile eşleşir.
/// Zamanlama mantığı SpacedRepetitionService içinde hesaplanır, bu model sadece
/// hesaplanan sonucu (yeni interval, yeni tekrar tarihi) saklar.
@Model
final class LearningItemProgress {
    @Attribute(.unique) var itemID: String
    var itemKind: LearnableItemKind

    /// SM-2 algoritmasının temel değişkenleri.
    var easeFactor: Double
    var intervalDays: Int
    var repetitionCount: Int

    var dueDate: Date
    var lastReviewedDate: Date?
    var isLearned: Bool

    /// Yanlış cevap verildiğinde işaretlenir ve kullanıcı "Tekrar Çalış" ekranında
    /// öğrendiğini onaylayana kadar işaretli kalır (doğru bilmek otomatik temizlemez).
    var needsReview: Bool = false

    init(
        itemID: String,
        itemKind: LearnableItemKind,
        easeFactor: Double = 2.5,
        intervalDays: Int = 0,
        repetitionCount: Int = 0,
        dueDate: Date = .now,
        lastReviewedDate: Date? = nil,
        isLearned: Bool = false,
        needsReview: Bool = false
    ) {
        self.itemID = itemID
        self.itemKind = itemKind
        self.easeFactor = easeFactor
        self.intervalDays = intervalDays
        self.repetitionCount = repetitionCount
        self.dueDate = dueDate
        self.lastReviewedDate = lastReviewedDate
        self.isLearned = isLearned
        self.needsReview = needsReview
    }
}

/// Bir modülün (Hiragana/Katakana) o anki çalışma turunun durumu. Kullanıcı ortadan
/// çıkıp geri döndüğünde kaldığı yerden devam edebilmesi için her cevaptan sonra kaydedilir.
/// `remainingItemIDs` bu turda henüz cevaplanmamış karakterleri, `wrongItemIDs` bu turda
/// yanlış yapılıp bir sonraki tekrar turunu bekleyen karakterleri tutar. `remainingItemIDs`
/// boşalınca `wrongItemIDs` doluysa yeni bir tur bu listeyle başlar — tüm kartlar art arda
/// bir turda doğru yapılana kadar bu döngü sürer.
@Model
final class LearningSessionState {
    @Attribute(.unique) var moduleType: String
    var remainingItemIDs: [String]
    var wrongItemIDs: [String]
    var isCompleted: Bool

    /// O an ekranda gösterilmekte olan kartın id'si. Kullanıcı uygulamadan çıkıp
    /// geri döndüğünde turun ortasından rastgele bir kartla değil, tam olarak
    /// bıraktığı kartla karşılaşsın diye ayrıca saklanır.
    var currentCardID: String?

    /// Oturum boyunca (tekrar turları dahil) verilen toplam cevap sayısı.
    /// Oturum sonundaki özet ekranında gösterilir; yeni oturumda sıfırlanır.
    var totalAnswerCount: Int = 0

    /// Oturum boyunca yanlış yapılan öğeler ve kaçar kez yanlış yapıldıkları
    /// (itemID → yanlış sayısı). Özet ekranındaki liste bundan üretilir.
    var wrongAnswerCounts: [String: Int] = [:]

    init(
        moduleType: String,
        remainingItemIDs: [String],
        wrongItemIDs: [String] = [],
        isCompleted: Bool = false,
        currentCardID: String? = nil,
        totalAnswerCount: Int = 0,
        wrongAnswerCounts: [String: Int] = [:]
    ) {
        self.moduleType = moduleType
        self.remainingItemIDs = remainingItemIDs
        self.wrongItemIDs = wrongItemIDs
        self.isCompleted = isCompleted
        self.currentCardID = currentCardID
        self.totalAnswerCount = totalAnswerCount
        self.wrongAnswerCounts = wrongAnswerCounts
    }
}

/// Kullanıcının genel ilerlemesi: günlük streak takibi için tek bir kayıt yeterli,
/// bu yüzden uygulama boyunca tek bir `UserProgress` nesnesi tutulur (bkz. HomeView/LearningViewModel
/// içindeki "fetch veya oluştur" deseni).
@Model
final class UserProgress {
    var currentStreak: Int
    var longestStreak: Int
    var lastStudyDate: Date?
    var totalItemsLearned: Int
    var createdAt: Date

    init(
        currentStreak: Int = 0,
        longestStreak: Int = 0,
        lastStudyDate: Date? = nil,
        totalItemsLearned: Int = 0,
        createdAt: Date = .now
    ) {
        self.currentStreak = currentStreak
        self.longestStreak = longestStreak
        self.lastStudyDate = lastStudyDate
        self.totalItemsLearned = totalItemsLearned
        self.createdAt = createdAt
    }

    /// Bugün çalışıldığında çağrılır. Streak'i güncel tutar; bir gün atlanırsa sıfırlar.
    func recordStudySession(on date: Date = .now) {
        let calendar = Calendar.current
        defer { lastStudyDate = date }

        guard let last = lastStudyDate else {
            currentStreak = 1
            longestStreak = max(longestStreak, currentStreak)
            return
        }

        if calendar.isDate(last, inSameDayAs: date) {
            return // Bugün zaten kaydedildi.
        }

        if let dayAfterLast = calendar.date(byAdding: .day, value: 1, to: last),
           calendar.isDate(dayAfterLast, inSameDayAs: date) {
            currentStreak += 1
        } else {
            currentStreak = 1
        }
        longestStreak = max(longestStreak, currentStreak)
    }
}
