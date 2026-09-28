import Foundation
import SwiftData

/// Uygulamanın aktif kullanıldığı süreyi hesaplayıp günceller.
@MainActor
class TimeTrackingService {
    static let shared = TimeTrackingService()
    private init() {}
    
    private var sessionStartTime: Date?
    
    func appDidBecomeActive() {
        sessionStartTime = .now
    }
    
    func appWillResignActive(context: ModelContext) {
        guard let start = sessionStartTime else { return }
        let duration = Int(Date.now.timeIntervalSince(start))
        
        if duration > 0 {
            addTime(seconds: duration, context: context)
        }
        
        // Yeniden active olana kadar süreyi sıfırla
        sessionStartTime = nil
    }
    
    /// Uygulama hala açıkken süreyi veritabanına yansıtmak için kullanılır (ör. grafik açıldığında).
    func commitCurrentSession(context: ModelContext) {
        guard let start = sessionStartTime else { return }
        let duration = Int(Date.now.timeIntervalSince(start))
        
        if duration > 0 {
            addTime(seconds: duration, context: context)
            // Çift saymayı önlemek için başlangıç zamanını şu an olarak güncelle
            sessionStartTime = .now
        }
    }
    
    private func addTime(seconds: Int, context: ModelContext) {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let todayString = formatter.string(from: .now)
        
        let dailyDescriptor = FetchDescriptor<DailyActivity>(predicate: #Predicate { $0.dateString == todayString })
        let dailyActivity: DailyActivity
        if let existingDaily = (try? context.fetch(dailyDescriptor))?.first {
            dailyActivity = existingDaily
        } else {
            dailyActivity = DailyActivity(date: .now)
            context.insert(dailyActivity)
        }
        
        dailyActivity.timeSpentSeconds += seconds
        try? context.save()
    }
}
