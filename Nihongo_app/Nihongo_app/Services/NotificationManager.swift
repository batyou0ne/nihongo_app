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
    
    func scheduleDailyReminder(hour: Int, minute: Int) {
        if !isAuthorized {
            Task {
                let granted = await requestPermission()
                if granted {
                    self.setupDailyReminder(hour: hour, minute: minute)
                }
            }
        } else {
            setupDailyReminder(hour: hour, minute: minute)
        }
    }
    
    private func setupDailyReminder(hour: Int, minute: Int) {
        let center = UNUserNotificationCenter.current()
        // Önceki hatırlatıcıları temizle
        center.removePendingNotificationRequests(withIdentifiers: ["dailyReminder"])
        
        let content = UNMutableNotificationContent()
        content.title = "Çalışma Zamanı!"
        content.body = "Japonca hedeflerine bir adım daha yaklaş. Günlük egzersizlerini yapmak için hemen uygulamaya gir! 🎌"
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
                print("Günlük hatırlatıcı her gün saat \(hour):\(String(format: "%02d", minute)) için kuruldu.")
            }
        }
    }
    
    func cancelDailyReminder() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["dailyReminder"])
    }
}
