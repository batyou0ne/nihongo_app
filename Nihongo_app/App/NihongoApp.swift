import SwiftUI
import SwiftData

@main
struct NihongoApp: App {
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
        }
        .modelContainer(modelContainer)
    }
}
