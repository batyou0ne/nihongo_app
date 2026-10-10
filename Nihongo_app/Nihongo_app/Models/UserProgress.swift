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
    case grammar // N5 gramer modülü (GrammarPoint) tarafından kullanılır

    var displayName: String {
        switch self {
        case .hiraganaCharacter: return "Hiragana"
        case .katakanaCharacter: return "Katakana"
        case .vocabularyWord: return L10n.itemKindName(rawValue)
        case .kanji: return "Kanji"
        case .grammar: return L10n.itemKindName(rawValue)
        }
    }

    /// Modüldeki toplam öğe sayısı (Resources/*.json içerikleriyle eşleşir).
    /// HomeView ve ProgressOverviewView aynı kaynağı kullansın diye burada tutulur.
    var totalCount: Int {
        switch self {
        case .hiraganaCharacter, .katakanaCharacter: return 46
        case .kanji: return 80
        case .vocabularyWord: return 675
        case .grammar: return 85
        }
    }

    /// Ana ekranda ikon yerine gösterilen Japonca karakter.
    var symbol: String {
        switch self {
        case .hiraganaCharacter: return "あ"
        case .katakanaCharacter: return "ア"
        case .kanji: return "漢"
        case .vocabularyWord: return "語"
        case .grammar: return "文"
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

    /// Bu oturumun en son ne zaman açıldığı. Ana ekrandaki "Kaldığın yer" kartı,
    /// en son açılan oturumu bulmak için bu alana göre sıralar.
    /// Varsayılanı `.distantPast` — böylece bu alan eklenmeden önce oluşmuş
    /// kayıtlar (SwiftData migration) "hiç açılmamış" sayılıp kartta çıkmaz.
    var lastOpenedAt: Date = Date.distantPast

    init(
        moduleType: String,
        remainingItemIDs: [String],
        wrongItemIDs: [String] = [],
        isCompleted: Bool = false,
        currentCardID: String? = nil,
        totalAnswerCount: Int = 0,
        wrongAnswerCounts: [String: Int] = [:],
        lastOpenedAt: Date = .distantPast
    ) {
        self.moduleType = moduleType
        self.remainingItemIDs = remainingItemIDs
        self.wrongItemIDs = wrongItemIDs
        self.isCompleted = isCompleted
        self.currentCardID = currentCardID
        self.totalAnswerCount = totalAnswerCount
        self.wrongAnswerCounts = wrongAnswerCounts
        self.lastOpenedAt = lastOpenedAt
    }
}

/// Günlük kazanılan XP ve çalışılan öğe sayısını tutar (Swift Charts için).
/// `dateString` alanı "YYYY-MM-DD" formatında tutulur ki her gün için tek bir eşsiz kayıt oluşsun.
@Model
final class DailyActivity {
    @Attribute(.unique) var dateString: String
    var xpEarned: Int
    var itemsReviewed: Int
    var timeSpentSeconds: Int = 0
    
    init(date: Date = .now, xpEarned: Int = 0, itemsReviewed: Int = 0, timeSpentSeconds: Int = 0) {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        self.dateString = formatter.string(from: date)
        self.xpEarned = xpEarned
        self.itemsReviewed = itemsReviewed
        self.timeSpentSeconds = timeSpentSeconds
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
    var totalXP: Int = 0
    var createdAt: Date

    init(
        currentStreak: Int = 0,
        longestStreak: Int = 0,
        lastStudyDate: Date? = nil,
        totalItemsLearned: Int = 0,
        totalXP: Int = 0,
        createdAt: Date = .now
    ) {
        self.currentStreak = currentStreak
        self.longestStreak = longestStreak
        self.lastStudyDate = lastStudyDate
        self.totalItemsLearned = totalItemsLearned
        self.totalXP = totalXP
        self.createdAt = createdAt
    }

    /// Gösterime uygun seri sayısı. `currentStreak` ancak bir sonraki çalışmada
    /// sıfırlandığı için (bkz. recordStudySession), araya gün girmişse alanda hâlâ
    /// eski değer durur. Seri bugün ya da dün çalışıldıysa canlıdır; daha eskiyse
    /// kopmuştur ve 0 gösterilir.
    var activeStreak: Int {
        guard let last = lastStudyDate, currentStreak > 0 else { return 0 }
        let calendar = Calendar.current
        let days = calendar.dateComponents(
            [.day],
            from: calendar.startOfDay(for: last),
            to: calendar.startOfDay(for: .now)
        ).day ?? 0
        return days <= 1 ? currentStreak : 0
    }

    /// `daysAgo` gün önce çalışılmış mı? (0 = bugün). Ayrı bir "çalışılan günler"
    /// tablosu tutmuyoruz; seri kesintisiz günlerden oluştuğu için son çalışma
    /// tarihi + seri uzunluğu bu bilgiyi vermeye yeter. Ana ekrandaki 7 günlük
    /// seri şeridi bunu kullanır.
    func didStudy(daysAgo: Int, now: Date = .now) -> Bool {
        guard let last = lastStudyDate, currentStreak > 0 else { return false }
        let calendar = Calendar.current
        guard let day = calendar.date(byAdding: .day, value: -daysAgo, to: now) else { return false }

        let dayStart = calendar.startOfDay(for: day)
        let lastStart = calendar.startOfDay(for: last)
        // Serinin bittiği günden sonrası (ör. bugün henüz çalışılmadıysa bugün) dolu değildir.
        guard dayStart <= lastStart else { return false }

        let distance = calendar.dateComponents([.day], from: dayStart, to: lastStart).day ?? 0
        return distance < currentStreak
    }

    /// Bugün çalışıldığında çağrılır. Streak'i güncel tutar; bir gün atlanırsa sıfırlar.
    func recordStudySession(on date: Date = .now) {
        let calendar = Calendar.current
        defer { 
            lastStudyDate = date
            // Streak güncellendikten sonra bildirimi de senkronize et
            NotificationManager.shared.updateDailyReminderStreak(activeStreak: activeStreak)
        }

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

// MARK: - JLPT Seviye Modelleri & İlerleme

/// JLPT (Japanese-Language Proficiency Test) seviyeleri (N5 en temel, N1 en ileri).
enum JLPTLevel: String, CaseIterable, Codable, Comparable, Identifiable {
    case n5 = "N5"
    case n4 = "N4"
    case n3 = "N3"
    case n2 = "N2"
    case n1 = "N1"

    var id: String { rawValue }

    /// Karşılaştırma ve sıralama için sıra numarası (0: N5 -> 4: N1)
    var order: Int {
        switch self {
        case .n5: return 0
        case .n4: return 1
        case .n3: return 2
        case .n2: return 3
        case .n1: return 4
        }
    }

    static func < (lhs: JLPTLevel, rhs: JLPTLevel) -> Bool {
        lhs.order < rhs.order
    }

    /// Bir sonraki üst seviye (örn. N5 -> N4)
    var nextLevel: JLPTLevel? {
        switch self {
        case .n5: return .n4
        case .n4: return .n3
        case .n3: return .n2
        case .n2: return .n1
        case .n1: return nil
        }
    }

    /// Bir önceki alt seviye (örn. N4 -> N5)
    var previousLevel: JLPTLevel? {
        switch self {
        case .n5: return nil
        case .n4: return .n5
        case .n3: return .n4
        case .n2: return .n3
        case .n1: return .n2
        }
    }

    var title: String {
        "\(rawValue)"
    }

    var subtitle: String {
        switch self {
        case .n5: return "Temel Başlangıç (Beginner)"
        case .n4: return "Temel İleri (Elementary)"
        case .n3: return "Orta Seviye (Intermediate)"
        case .n2: return "İleri Seviye (Upper Intermediate)"
        case .n1: return "Ustalık (Advanced / Native)"
        }
    }

    /// Kapı sınavına (Gateway Exam) hak kazanmak için gereken asgari modül tamamlama oranı (%85)
    var completionThreshold: Double {
        0.85
    }

    /// Seviyeye ait toplam yaklaşık içerik sayıları (UI göstergeleri ve hedef takibi için)
    var targetCounts: (kanji: Int, vocab: Int, grammar: Int) {
        switch self {
        case .n5: return (kanji: 80, vocab: 675, grammar: 85)
        case .n4: return (kanji: 180, vocab: 800, grammar: 115)
        case .n3: return (kanji: 350, vocab: 1500, grammar: 140)
        case .n2: return (kanji: 400, vocab: 2500, grammar: 150)
        case .n1: return (kanji: 1000, vocab: 3500, grammar: 120)
        }
    }
}

/// Bir kullanıcının JLPT seviyesi bazındaki genel ilerleme, kilit ve sınav durumu.
/// N5 varsayılan olarak açıktır; N4, N3, N2 ve N1 ise önceki seviye tamamlanana
/// veya Test-Out sınavı geçilene kadar kilitli kalır.
@Model
final class UserLevelProgress {
    /// "N5", "N4", "N3", "N2", "N1" (JLPTLevel.rawValue)
    @Attribute(.unique) var levelRaw: String

    /// Bu seviyenin kilidi açık mı? (N5 için true, diğerleri false başlar)
    var isUnlocked: Bool

    /// Seviyenin tüm zorunlu bölümleri ve kapı sınavı tamamlandı mı?
    var isCompleted: Bool

    /// Seviye sonu kapı sınavı (Gateway Exam) başarıyla geçildi mi?
    var hasPassedGatewayExam: Bool

    /// Kapı sınavı skoru (0.0 - 1.0 arası)
    var gatewayExamScore: Double?

    /// Kapı sınavının geçildiği tarih
    var gatewayExamDate: Date?

    /// Seviye dersleri tek tek yapılmadan doğrudan "Test-Out" sınavı ile mi atlandı?
    var isSkippedViaPlacement: Bool

    /// Modül bazında tamamlanan part/ünite indeksleri (JSON formatında saklanır).
    /// Örn: {"kanji":[0,1,2],"vocab":[0,1,2,3],"grammar":[1,2]}
    var completedPartsJSON: String

    /// Son güncelleme tarihi
    var lastUpdated: Date

    init(
        level: String,
        isUnlocked: Bool = false,
        isCompleted: Bool = false,
        hasPassedGatewayExam: Bool = false,
        gatewayExamScore: Double? = nil,
        gatewayExamDate: Date? = nil,
        isSkippedViaPlacement: Bool = false,
        completedPartsJSON: String = "{}",
        lastUpdated: Date = .now
    ) {
        self.levelRaw = level
        self.isUnlocked = isUnlocked
        self.isCompleted = isCompleted
        self.hasPassedGatewayExam = hasPassedGatewayExam
        self.gatewayExamScore = gatewayExamScore
        self.gatewayExamDate = gatewayExamDate
        self.isSkippedViaPlacement = isSkippedViaPlacement
        self.completedPartsJSON = completedPartsJSON
        self.lastUpdated = lastUpdated
    }

    var level: JLPTLevel? {
        JLPTLevel(rawValue: levelRaw)
    }

    // MARK: - Parça (Part) Tamamlama Yardımcıları

    /// Belirtilen modülde (örn. "kanji", "vocab", "grammar") tamamlanmış parça indeksleri kümesi.
    func completedPartIndices(module: String) -> Set<Int> {
        guard let data = completedPartsJSON.data(using: .utf8),
              let dict = try? JSONDecoder().decode([String: [Int]].self, from: data),
              let list = dict[module] else {
            return []
        }
        return Set(list)
    }

    /// Belirtilen parçanın (0-indexed) tamamlanıp tamamlanmadığını döner.
    func isPartCompleted(module: String, index: Int) -> Bool {
        completedPartIndices(module: module).contains(index)
    }

    /// Belirtilen parçayı tamamlandı olarak işaretler ve JSON'ı günceller.
    func markPartCompleted(module: String, index: Int) {
        var dict: [String: [Int]] = [:]
        if let data = completedPartsJSON.data(using: .utf8),
           let parsed = try? JSONDecoder().decode([String: [Int]].self, from: data) {
            dict = parsed
        }
        var set = Set(dict[module] ?? [])
        set.insert(index)
        dict[module] = Array(set).sorted()

        if let encoded = try? JSONEncoder().encode(dict),
           let jsonStr = String(data: encoded, encoding: .utf8) {
            self.completedPartsJSON = jsonStr
            self.lastUpdated = .now
        }
    }
}
