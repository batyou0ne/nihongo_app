import SwiftUI
import SwiftData
import FirebaseCore

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
            HomeView()
                .task {
                    // Oturum yoksa sessizce anonim oturum aç; kullanıcı daha
                    // sonra Apple ile hesap bağladığında uid korunur.
                    await AuthService.shared.signInAnonymouslyIfNeeded()
                }
        }
        .modelContainer(modelContainer)
    }
}
