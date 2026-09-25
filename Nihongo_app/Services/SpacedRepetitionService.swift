import Foundation
import SwiftData

/// Anki tarzı Spaced Repetition (Aralıklı Tekrar) algoritmasını yürüten servis.
/// SuperMemo-2 (SM-2) algoritması temel alınmıştır.
class SpacedRepetitionService {
    static let shared = SpacedRepetitionService()
    
    private init() {}
    
    /// Verilen öğrenme kaydını SM-2 algoritmasına göre günceller.
    /// - Parameters:
    ///   - progress: Güncellenecek kayıt
    ///   - correct: Cevabın doğruluğu. (Doğruysa kalite=4, yanlışsa kalite=1 varsayılır)
    func updateProgress(for progress: LearningItemProgress, correct: Bool) {
        let quality = correct ? 4.0 : 1.0
        
        if quality >= 3 {
            // Doğru cevap
            if progress.repetitionCount == 0 {
                progress.intervalDays = 1
            } else if progress.repetitionCount == 1 {
                progress.intervalDays = 6
            } else {
                progress.intervalDays = Int(round(Double(progress.intervalDays) * progress.easeFactor))
            }
            progress.repetitionCount += 1
            progress.isLearned = true
            progress.needsReview = false
        } else {
            // Yanlış cevap: tekrar sıfırlanır, kısa aralığa (1 gün) alınır.
            progress.repetitionCount = 0
            progress.intervalDays = 1
            progress.needsReview = true
        }
        
        // Ease Factor güncellenir: EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        let newEase = progress.easeFactor + (0.1 - (5.0 - quality) * (0.08 + (5.0 - quality) * 0.02))
        progress.easeFactor = max(1.3, newEase) // En düşük 1.3 olabilir
        
        progress.lastReviewedDate = .now
        
        // Gelecek çalışma tarihi belirlenir (Gece yarısına hizalanması önerilir, basitlik için doğrudan gün ekliyoruz)
        if let newDueDate = Calendar.current.date(byAdding: .day, value: progress.intervalDays, to: .now) {
            progress.dueDate = newDueDate
        }
    }
}
