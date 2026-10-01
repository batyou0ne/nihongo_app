import Foundation
import SwiftData

/// Kullanıcının kazandığı XP'leri ve günlük çalışma istatistiklerini yönetir.
class XPManager {
    static let shared = XPManager()
    private init() {}
    
    enum XPAction {
        case newCardLearned
        case cardReviewed
        case grammarCompleted
        case storyCompleted
        
        var points: Int {
            switch self {
            case .newCardLearned: return 5
            case .cardReviewed: return 2
            case .grammarCompleted: return 20
            case .storyCompleted: return 20
            }
        }
    }
    
    /// Belirtilen eylem için XP ekler ve günlük aktiviteyi günceller.
    @MainActor
    func addXP(action: XPAction, context: ModelContext) {
        let points = action.points
        
        // 1. Toplam XP'yi güncelle
        let userDescriptor = FetchDescriptor<UserProgress>()
        let userProgress: UserProgress
        if let existing = (try? context.fetch(userDescriptor))?.first {
            userProgress = existing
        } else {
            userProgress = UserProgress()
            context.insert(userProgress)
        }
        userProgress.totalXP += points
        
        // 2. Günlük Aktiviteyi güncelle
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let todayString = formatter.string(from: .now)
        
        // Predicate içinde string eşleştirmesi
        let dailyDescriptor = FetchDescriptor<DailyActivity>(predicate: #Predicate { $0.dateString == todayString })
        let dailyActivity: DailyActivity
        if let existingDaily = (try? context.fetch(dailyDescriptor))?.first {
            dailyActivity = existingDaily
        } else {
            dailyActivity = DailyActivity(date: .now)
            context.insert(dailyActivity)
        }
        
        dailyActivity.xpEarned += points
        
        if action == .newCardLearned || action == .cardReviewed {
            dailyActivity.itemsReviewed += 1
        }
        
        try? context.save()
    }
}
