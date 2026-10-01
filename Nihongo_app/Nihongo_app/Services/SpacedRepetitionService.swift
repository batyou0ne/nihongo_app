import Foundation
import SwiftData

/// Anki tarzı Spaced Repetition (Aralıklı Tekrar) algoritmasını yürüten servis.
/// SuperMemo-2 (SM-2) algoritması temel alınmıştır.
class SpacedRepetitionService {
    enum SRSLevel: Int, CaseIterable {
        case apprentice1 = 1 // 4 saat
        case apprentice2 = 2 // 1 gün
        case apprentice3 = 3 // 3 gün
        case guru = 4        // 1 hafta
        case master = 5      // 2 hafta
        case enlightened = 6 // 1 ay
        case burned = 7      // Kalıcı öğrenilmiş

        var title: String {
            switch self {
            case .apprentice1, .apprentice2, .apprentice3: return "Çırak"
            case .guru: return "Kalfa"
            case .master: return "Usta"
            case .enlightened: return "Aydınlanmış"
            case .burned: return "Kalıcı (Burned)"
            }
        }
        
        var nextIntervalHours: Int {
            switch self {
            case .apprentice1: return 4
            case .apprentice2: return 24 // 1 gün
            case .apprentice3: return 72 // 3 gün
            case .guru: return 168       // 1 hafta
            case .master: return 336     // 2 hafta
            case .enlightened: return 720 // ~1 ay
            case .burned: return 0
            }
        }
    }

    static let shared = SpacedRepetitionService()
    
    private init() {}
    
    /// Verilen öğrenme kaydını SRS (WaniKani stili) algoritmasına göre günceller.
    /// `repetitionCount` alanı artık SRS Level olarak görev yapar.
    func updateProgress(for progress: LearningItemProgress, correct: Bool) {
        var currentLevel = progress.repetitionCount
        
        if correct {
            currentLevel += 1
            if currentLevel > 7 { currentLevel = 7 }
            
            progress.needsReview = false
            progress.isLearned = true // Bir kere bile doğru bilindiyse öğrenildi sayılır.
        } else {
            // Yanlış cevap verildiğinde ceza olarak 1 veya daha fazla seviye düşer.
            // Kalfa (4) ve üstüyse daha sert, çıraksa 1 seviye düşürebiliriz.
            if currentLevel > 4 {
                currentLevel -= 2
            } else {
                currentLevel -= 1
            }
            
            if currentLevel < 1 { currentLevel = 1 } // En düşük seviye 1 (Apprentice 1)
            progress.needsReview = true
        }
        
        progress.repetitionCount = currentLevel
        progress.lastReviewedDate = .now
        
        // Yeni çalışma tarihini belirle
        if let level = SRSLevel(rawValue: currentLevel) {
            progress.intervalDays = level.nextIntervalHours / 24 // Geriye dönük uyumluluk veya istatistik için
            
            if level == .burned {
                // Kalıcı öğrenilenler için bir daha review'a düşmemesi adına 10 yıl sonraya atıyoruz
                progress.dueDate = Calendar.current.date(byAdding: .year, value: 10, to: .now) ?? .now
            } else {
                progress.dueDate = Calendar.current.date(byAdding: .hour, value: level.nextIntervalHours, to: .now) ?? .now
            }
        } else {
            // Fallback
            progress.repetitionCount = 1
            progress.intervalDays = 0
            progress.dueDate = Calendar.current.date(byAdding: .hour, value: 4, to: .now) ?? .now
        }
    }
}
