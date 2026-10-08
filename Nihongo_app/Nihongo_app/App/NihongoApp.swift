import SwiftUI
import SwiftData
import FirebaseCore
import FirebaseAuth
import GoogleSignIn

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        FirebaseApp.configure()
        
        UNUserNotificationCenter.current().delegate = self
        
        return true
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .list, .sound])
    }
}

@main
struct NihongoApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    @State private var modelContainer: ModelContainer?
    @State private var currentUid: String? = nil

    var body: some Scene {
        WindowGroup {
            Group {
                if let container = modelContainer {
                    MainTabView()
                        .modelContainer(container)
                        .id(currentUid ?? "anonymous")
                } else {
                    ProgressView()
                }
            }
            .task {
                // Oturum yoksa sessizce anonim oturum aç; kullanıcı daha
                // sonra hesap bağladığında uid korunur.
                await AuthService.shared.signInAnonymouslyIfNeeded()
            }
            .onAppear {
                let uid = AuthService.shared.currentUser?.uid
                self.currentUid = uid
                self.modelContainer = createModelContainer(for: uid)
            }
            .onChange(of: AuthService.shared.currentUser?.uid) { oldValue, newValue in
                self.currentUid = newValue
                self.modelContainer = createModelContainer(for: newValue)
            }
            .onOpenURL { url in
                // Google giriş akışının OAuth geri dönüşü (URL şeması).
                GIDSignIn.sharedInstance.handle(url)
            }
        }
    }
    
    private func createModelContainer(for uid: String?) -> ModelContainer {
        let userId = uid ?? "anonymous"
        let schema = Schema([
            LearningItemProgress.self,
            UserProgress.self,
            LearningSessionState.self,
            DailyActivity.self,
            UserLevelProgress.self
        ])
        
        let fileManager = FileManager.default
        let appSupportURL = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let storeURL = appSupportURL.appendingPathComponent("progress_\(userId).store")
        
        let defaultStoreURL = appSupportURL.appendingPathComponent("default.store")
        
        // Eğer daha önce kaydedilmiş bir genel ilerleme varsa ve bu kullanıcı için yeni bir veritabanı oluşuyorsa,
        // eski veritabanını yeni kullanıcının dosyasına taşıyarak ilerleme kaybını önle (Migration).
        if fileManager.fileExists(atPath: defaultStoreURL.path) && !fileManager.fileExists(atPath: storeURL.path) {
            do {
                try fileManager.moveItem(at: defaultStoreURL, to: storeURL)
                
                let defaultShmURL = appSupportURL.appendingPathComponent("default.store-shm")
                let storeShmURL = appSupportURL.appendingPathComponent("progress_\(userId).store-shm")
                if fileManager.fileExists(atPath: defaultShmURL.path) {
                    try fileManager.moveItem(at: defaultShmURL, to: storeShmURL)
                }
                
                let defaultWalURL = appSupportURL.appendingPathComponent("default.store-wal")
                let storeWalURL = appSupportURL.appendingPathComponent("progress_\(userId).store-wal")
                if fileManager.fileExists(atPath: defaultWalURL.path) {
                    try fileManager.moveItem(at: defaultWalURL, to: storeWalURL)
                }
            } catch {
                print("Eski veritabanı taşınamadı: \(error)")
            }
        }
        
        let configuration = ModelConfiguration(schema: schema, url: storeURL)
        do {
            return try ModelContainer(for: schema, configurations: [configuration])
        } catch {
            fatalError("SwiftData ModelContainer oluşturulamadı: \(error)")
        }
    }
}
