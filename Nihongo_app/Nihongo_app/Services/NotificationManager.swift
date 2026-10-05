import Foundation
import UserNotifications
import Observation

@Observable
class NotificationManager: NSObject {
    static let shared = NotificationManager()
    
    var isAuthorized: Bool = false
    
    private override init() {
        super.init()
        Task {
            await checkAuthorizationStatus()
        }
    }
    
    func requestPermission() async -> Bool {
        do {
            let options: UNAuthorizationOptions = [.alert, .badge, .sound]
            let granted = try await UNUserNotificationCenter.current().requestAuthorization(options: options)
            await MainActor.run {
                self.isAuthorized = granted
            }
            return granted
        } catch {
            print("Bildirim izni istenirken hata oluştu: \(error)")
            return false
        }
    }
    
    func checkAuthorizationStatus() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        await MainActor.run {
            self.isAuthorized = (settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional)
        }
    }
    
    func scheduleDailyReminder(hour: Int, minute: Int, activeStreak: Int? = nil) {
        if !isAuthorized {
            Task {
                let granted = await requestPermission()
                if granted {
                    self.setupDailyReminder(hour: hour, minute: minute, activeStreak: activeStreak)
                }
            }
        } else {
            setupDailyReminder(hour: hour, minute: minute, activeStreak: activeStreak)
        }
    }
    
    private func setupDailyReminder(hour: Int, minute: Int, activeStreak: Int?) {
        let center = UNUserNotificationCenter.current()
        // Önceki hatırlatıcıları temizle
        center.removePendingNotificationRequests(withIdentifiers: ["dailyReminder"])
        
        let content = UNMutableNotificationContent()
        if let streak = activeStreak, streak > 0 {
            content.title = "Serini Kaybetme! 🔥"
            content.body = "Tam \(streak) gündür harika gidiyorsun. Bugün Japonca çalışıp serini korumak için uygulamaya gir!"
        } else {
            content.title = "Çalışma Zamanı!"
            content.body = "Japonca hedeflerine bir adım daha yaklaş. Günlük egzersizlerini yapmak için hemen uygulamaya gir! 🎌"
        }
        content.sound = .default
        
        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = minute
        
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        
        let request = UNNotificationRequest(identifier: "dailyReminder", content: content, trigger: trigger)
        
        center.add(request) { error in
            if let error = error {
                print("Bildirim ayarlanırken hata oluştu: \(error)")
            } else {
                print("Günlük hatırlatıcı her gün saat \(hour):\(String(format: "%02d", minute)) (Seri: \(activeStreak ?? 0)) için kuruldu.")
            }
        }
    }
    
    func updateDailyReminderStreak(activeStreak: Int) {
        let isEnabled = UserDefaults.standard.bool(forKey: "isDailyReminderEnabled")
        guard isEnabled else { return }
        
        let savedTime = UserDefaults.standard.double(forKey: "reminderTimeInterval")
        guard savedTime > 0 else { return }
        
        let date = Date(timeIntervalSince1970: savedTime)
        let components = Calendar.current.dateComponents([.hour, .minute], from: date)
        
        scheduleDailyReminder(hour: components.hour ?? 20, minute: components.minute ?? 0, activeStreak: activeStreak)
    }
    
    func cancelDailyReminder() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["dailyReminder"])
    }
}
