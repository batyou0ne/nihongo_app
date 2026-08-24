import Foundation

/// Kullanıcının bir tekrarda verdiği cevabın kalitesi. QuizView bunu quiz sonucundan üretir.
enum ReviewQuality {
    case again  // Yanlış cevap: sıfırdan başla.
    case hard   // Doğru ama tereddütlü: interval küçük büyür.
    case good   // Doğru: normal SM-2 ilerlemesi.
    case easy   // Doğru ve hızlı: interval daha büyük atlar.
}

/// SM-2 algoritmasının basitleştirilmiş bir uygulaması. Anki ve benzeri sistemlerin
/// temelini oluşturan bu algoritma, her doğru cevapta tekrar aralığını (interval)
/// ease factor ile çarparak büyütür; yanlış cevapta sıfırlar.
/// Stateless bırakıldı — tüm durum LearningItemProgress'te (SwiftData) tutuluyor,
/// bu servis sadece "bir sonraki durum ne olmalı" hesabını yapıyor.
enum SpacedRepetitionService {

    private static let minimumEaseFactor = 1.3

    /// Bir tekrar sonucuna göre `progress`'i günceller ve yeni tekrar tarihini belirler.
    static func schedule(_ progress: LearningItemProgress, quality: ReviewQuality, now: Date = .now) {
        switch quality {
        case .again:
            progress.repetitionCount = 0
            progress.intervalDays = 1
            progress.easeFactor = max(minimumEaseFactor, progress.easeFactor - 0.2)

        case .hard:
            progress.repetitionCount += 1
            progress.intervalDays = nextInterval(after: progress, multiplier: 1.2)
            progress.easeFactor = max(minimumEaseFactor, progress.easeFactor - 0.15)

        case .good:
            progress.repetitionCount += 1
            progress.intervalDays = nextInterval(after: progress, multiplier: progress.easeFactor)

        case .easy:
            progress.repetitionCount += 1
            progress.intervalDays = nextInterval(after: progress, multiplier: progress.easeFactor * 1.3)
            progress.easeFactor += 0.15
        }

        progress.lastReviewedDate = now
        progress.dueDate = Calendar.current.date(byAdding: .day, value: progress.intervalDays, to: now) ?? now

        // Ard arda 4+ başarılı tekrardan sonra "öğrenildi" say (ProgressView bunu gösterir).
        progress.isLearned = progress.repetitionCount >= 4
    }

    private static func nextInterval(after progress: LearningItemProgress, multiplier: Double) -> Int {
        switch progress.repetitionCount {
        case 0: return 1
        case 1: return 3
        default:
            let raw = Double(progress.intervalDays) * multiplier
            return max(1, Int(raw.rounded()))
        }
    }

    /// Bugün tekrar edilmesi gereken öğeler (dueDate geçmiş veya bugün olanlar).
    static func dueItems(from allProgress: [LearningItemProgress], now: Date = .now) -> [LearningItemProgress] {
        allProgress.filter { $0.dueDate <= now }
    }
}
