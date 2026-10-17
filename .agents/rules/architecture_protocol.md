---
trigger: always_on
---

# Kaimon (開門) — System Architecture & 4-Phase Agent Protocol

Binding rules for all AI agents operating in the **Kaimon (`Nihongo_app`)** iOS repository.

## 1. TECH STACK & DIRECTORY MAP (`Nihongo_app/Nihongo_app/`)

- **Stack:** Swift 5.0+, SwiftUI, Swift Concurrency (`async/await`), `Observation` (`@Observable`, `@ObservationIgnored`, `@MainActor`), `SwiftData` (`@Model`, `@Query`, `ModelContext`, per-user `progress_<uid>.store`), `FirebaseAuth` + `FirebaseFirestore` + `GoogleSignIn`, `StoreKit 2`, `GoogleMobileAds` + `UserMessagingPlatform`.
- **`App/`**: Root lifecycle & container setup (`NihongoApp.swift`, `TabBarManager.swift`, `GoogleService-Info.plist`).
- **`Config/`**: Environment & ad constants (`Secrets.swift`, `AdConfig.swift`).
- **`Models/`**:
  - SwiftData `@Model` entities in `UserProgress.swift`: `LearningItemProgress`, `UserProgress`, `LearningSessionState`, `DailyActivity`, `UserLevelProgress` + `JLPTLevel`, `LearnableItemKind`.
  - Domain `Codable` structs: `Character.swift`, `Kanji.swift`, `VocabularyWord.swift`, `Grammar.swift`, `Story.swift`, `ExamModels.swift`, `ChatMessage.swift`, `Models/Scenarios/ConversationScenario.swift`.
- **`ViewModels/`**: `@Observable` screen state & shared protocols (`QuizViewModel.swift` -> `QuizItem`, `FlashcardItem`, `QuizViewModel<Item>`; `ChatViewModel.swift`).
- **`Services/`**:
  - Core & SRS: `ContentStore.swift`, `SpacedRepetitionService.swift`, `LevelProgressionService.swift`, `ExamGenerator.swift`, `VerbConjugationEngine.swift`, `XPManager.swift`, `TimeTrackingService.swift`.
  - Localization: `L10n.swift`, `LanguageManager.swift`.
  - Audio/Haptics/Notifs: `AudioService.swift`, `AlphabetAudioService.swift`, `NotificationManager.swift`, `FeedbackManager.swift`.
  - Cloud/Auth/AI: `AuthService.swift`, `UserProfileService.swift`, `AIService.swift`.
  - Subscription (`Services/Subscription/`): `SubscriptionManager.swift`, `SubscriptionPlan.swift`, `FeatureGate.swift`.
  - Ads (`Services/Ads/`): `AdManager.swift`.
- **`Theme/`**: Brutalist design system (`Theme.swift` -> `Theme`, `PrimaryButtonStyle`, `InkBorder`, `.inkBordered()`).
- **`Views/`**: Root views (`MainTabView.swift`, `AlphabetMainView.swift`, `AlphabetChartView.swift`, `GrammarMainView.swift`, `VocabularyMainView.swift`, `ReadingMainView.swift`, `StoryReaderView.swift`, `DictionaryView.swift`), feature folders (`Home/`, `Learning/`, `Quiz/`, `Grammar/`, `Kanji/`, `Vocabulary/`, `Review/`, `Progress/`, `Reference/`, `Scenarios/`, `Chat/`, `Auth/`, `Subscription/`), and `Views/Shared/` (`DashboardComponents.swift`, `FuriganaText.swift`, `LockedLevelInfoSheet.swift`).
- **`Resources/`**: Static JLPT JSON files (`N5`–`N1`) and `Resources/Audio/`.
- **Context Protection Rule:** NEVER read full `Resources/*.json` or `Resources/Audio/*` files into context. Limit JSON schema inspection to lines 1–30.

---

## 2. 4-PHASE AGENT EXECUTION PROTOCOL

Agents MUST NOT jump directly into writing implementation code. Every task is divided into 4 sequential phases:

### PHASE 1 — Data & Contract Architect
- **Scope:** `Models/`, `ViewModels/` (protocols only), `Resources/` (schema contract).
- **Tasks:** Define `Codable`/`Identifiable`/`Hashable` structs, SwiftData `@Model` classes (with default values for migration safety in `NihongoApp.swift`), Swift `protocol` contracts (`QuizItem`, `FlashcardItem`), and a complete **File Roadmap**.
- **FORBIDDEN:** Writing `SwiftUI` `View` code, styling, or service/network logic.

### PHASE 2 — UI Component Specialist
- **Scope:** `Views/`, `Views/Shared/`, `Theme/Theme.swift`, `Services/L10n.swift`.
- **Tasks:** Build pure presentational SwiftUI Views using Phase 1 contracts (`let` props, `@Binding`, action closures).
- **Design & L10n Rules:**
  - Use `Theme.paper`, `Theme.ink`, `Theme.secondaryInk`, `Theme.accent` (vermilion), `Theme.display(_:)`, `Theme.heading(_:)`, `.inkBordered()`, and `PrimaryButtonStyle`. Arbitrary colors (`Color.blue`, etc.) are prohibited.
  - Hardcoded UI strings are prohibited; define all user-facing strings (TR/EN) in `Services/L10n.swift`.
- **FORBIDDEN:** Embedding `URLSession`, `Firestore`, `ModelContext.save()`, SRS calculations, or business logic inside Views.

### PHASE 3 — Integration & State Engineer
- **Scope:** `ViewModels/`, `Services/`, `Views/` (container wiring), `App/`.
- **Tasks:** Wire Phase 2 Views to `@Observable` ViewModels, singleton Services, and SwiftData (`@Environment(\.modelContext)`, `@Query`). Use `ContentStore` for all JSON loading/chunking, `LevelProgressionService.shared` for level/part locks, `FeatureGate.shared` for Pro quotas, and `TabBarManager.shared` for tab bar visibility.
- **FORBIDDEN:** Breaking or altering the approved UI hierarchy and `Theme` styling from Phase 2.

### PHASE 4 — Security & QA Auditor
- **Scope:** All modified files, `Config/Secrets.swift`, build & tests.
- **Tasks:**
  1. **Memory Leaks:** Verify `Task` cancellation (`deinit`/`.onDisappear`) and `[weak self]` in closures/listeners.
  2. **Performance & Concurrency:** Mark non-UI state with `@ObservationIgnored`, forbid disk I/O or heavy filtering inside `View.body`, and enforce `@MainActor` on UI mutations.
  3. **Edge-Cases & Security:** Ensure `Secrets.swift` keys are never logged; verify offline/429/503 handling in `AIService`, StoreKit restore in `SubscriptionManager`, and anonymous-to-linked UID store migration in `NihongoApp.swift`.
  4. **Verification:** Run `xcodebuild` to confirm zero build errors and pass verification tests.

---

## 3. HUMAN-IN-THE-LOOP CHECKPOINT RULE

1. **No Auto-Advance:** Agents MUST STOP at the end of each phase and NEVER proceed to the next phase automatically.
2. **Diff & Decision Summary:** At the end of each phase, present:
   - **Completed Phase** & **Modified/Created File Paths**
   - **Key Architectural Decisions**
   - **Next Phase Plan**
3. **Explicit Approval Gate:** Wait for explicit Tech Lead approval (**"Approved" / "Proceed" / "Onaylandı" / "Devam et"**) before starting the next phase.

---

## 4. CODING, NAMING & IMPORT STANDARDS

- **Naming:** `PascalCase` for types/files (`...View`, `...ViewModel`, `...Service`, `...Manager`). `camelCase` for members; prefix booleans with `is`, `has`, `can`, `needs`. Use caseless `enum` for stateless namespaces (`Theme`, `L10n`, `ContentStore`, `AdConfig`).
- **State Pattern:** Do NOT use `ObservableObject`/`@Published`/`@StateObject`. Use `@Observable final class` and `@Observable @MainActor final class <Service> { static let shared = ...; private init() {} }`.
- **Imports:** Import only required modules (Apple frameworks first, 3rd-party SPM packages second). Never import `SwiftUI`/`UIKit` in `Models/`.
- **Comments & Git:** Preserve Turkish `///` doc-comments and `// MARK: -` dividers. Follow `.agents/rules/git_workflow.md` (feature branches off `main`, incremental commits per approved phase).