import Foundation
import SwiftData

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
