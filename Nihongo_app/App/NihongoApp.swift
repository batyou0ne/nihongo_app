import SwiftUI
import SwiftData
import FirebaseCore
import GoogleSignIn

@main
struct NihongoApp: App {
    init() {
        FirebaseApp.configure()
    }

    let modelContainer: ModelContainer = {
        let schema = Schema([
            LearningItemProgress.self,
            UserProgress.self,
            LearningSessionState.self
        ])
        let configuration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        do {
            return try ModelContainer(for: schema, configurations: [configuration])
        } catch {
            fatalError("SwiftData ModelContainer oluşturulamadı: \(error)")
        }
    }()

    var body: some Scene {
        WindowGroup {
            MainTabView()
                .task {
                    // Oturum yoksa sessizce anonim oturum aç; kullanıcı daha
                    // sonra hesap bağladığında uid korunur.
                    await AuthService.shared.signInAnonymouslyIfNeeded()
                }
                .onOpenURL { url in
                    // Google giriş akışının OAuth geri dönüşü (URL şeması).
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
        .modelContainer(modelContainer)
    }
}
