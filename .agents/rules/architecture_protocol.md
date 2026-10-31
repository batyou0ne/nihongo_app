---
trigger: always_on
---

# Kaimon (開門) — Dual-Platform (iOS & Android) System Architecture & Agent Protocol

Binding rules for all AI agents operating in the **Kaimon (`nihongo_app`)** monorepo (`Nihongo_app/` for iOS and `Nihongo_android/` for Android).

---

## 1. MONOREPO TECH STACK & DIRECTORY MAP

### 1.1 Shared Assets & Context Protection (Single Source of Truth)
- **`Nihongo_app/Nihongo_app/Resources/`**: Canonical static JLPT JSON files (`N5`–`N1` Kanji, Vocabulary, Grammar, Syllabus, Stories, `HiraganaData.json`, `KatakanaData.json`, `ScenariosData.json`) and `Resources/Audio/` (`audio_manifest.json` + `.mp3` files).
- **Zero-Duplication Asset Rule:** Android (`Nihongo_android/`) MUST reference `Nihongo_app/Nihongo_app/Resources` directly via Gradle `sourceSets["main"].assets.srcDirs` or a pre-build sync task (ignored by Git). Never commit duplicate copies of the 15 MB JSON/Audio files into the repository.
- **Context Protection Rule:** NEVER read full `Resources/*.json` or `Resources/Audio/*` files into context. Limit JSON schema inspection to lines 1–30.

### 1.2 iOS Platform (`Nihongo_app/Nihongo_app/`)
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

### 1.3 Android Platform (`Nihongo_android/app/src/main/java/com/batuhankeskn0/kaimon/`)
- **Stack:** Kotlin 2.0+, Jetpack Compose (Material 3 + Custom Brutalist Theme), Kotlin Coroutines + `StateFlow`/`SharedFlow`, `Room` Database (`progress_<uid>.db` per user), `Hilt` (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`), `Kotlinx Serialization`, `Navigation Compose`, `Kuromoji` (`kuromoji-ipadic` for auto-furigana), `Firebase Auth` (Credential Manager) + `Firestore`, `Google Play Billing v7`, `Google Mobile Ads` + `UMP`.
- **`data/model/`**: `@Serializable` domain models (`JapaneseCharacter`, `Kanji`, `VocabularyWord`, `GrammarPoint`, `GrammarUnit`, `Story`, `ExamQuestion`, `ConversationScenario`, `ChatMessage`) and `QuizItem`/`FlashcardItem` interfaces.
- **`data/local/`**: Room `@Entity` classes (`LearningItemProgressEntity`, `LearningSessionStateEntity`, `DailyActivityEntity`, `UserProgressEntity`, `UserLevelProgressEntity`), DAOs, `@TypeConverter`s, and `UserDatabaseManager` (per-UID database switching & `progress_anonymous.db` -> `progress_<uid>.db` migration).
- **`data/repository/` & `data/service/`**: `ContentStore`, `LanguageManager`, `L10n`, `AudioService`, `AlphabetAudioService`, `FeedbackManager`, `NotificationManager`, `AuthService`, `UserProfileService`, `AIService`, `SubscriptionManager`, `FeatureGate`, `AdManager`.
- **`domain/engine/`**: Pure Kotlin business engines (`SpacedRepetitionService`, `LevelProgressionService`, `XPManager`, `TimeTrackingService`, `VerbConjugationEngine`, `ExamGenerator`).
- **`ui/theme/` & `ui/components/`**: `Theme.kt` (`KaimonTheme`, `Modifier.inkBordered()`, `PrimaryButton`), `FuriganaText.kt`, `DashboardComponents.kt`, `LockedLevelInfoSheet.kt`, `LessonCompleteView.kt`.
- **`ui/navigation/` & `ui/screens/`**: `MainTabScreen.kt`, `TabBarManager.kt`, and feature screen packages (`home/`, `alphabet/`, `learning/`, `quiz/`, `kanji/`, `vocabulary/`, `grammar/`, `reference/`, `reading/`, `dictionary/`, `review/`, `progress/`, `scenarios/`, `chat/`, `auth/`, `subscription/`).

---

## 2. 4-PHASE AGENT EXECUTION PROTOCOL

Agents MUST NOT jump directly into writing unverified implementation code.
- **For Full-Stack / UI Feature Tasks:** Execute all 4 sequential phases below, stopping for approval at the end of each phase.
- **For Dedicated Infrastructure, Data-Only, or Engine-Only Roadmap Phases (e.g., Android Roadmap Phases 1, 2, 3):** Execute the targeted phase scope (e.g., Gradle/Scaffold or Models/Room/Engines + Unit Tests + `./gradlew` verification) cleanly and present the final summary for approval before merging to `main`.

### PHASE 1 — Data & Contract Architect
- **Scope:**
  - *iOS:* `Models/`, `ViewModels/` (protocols only), `Resources/` (schema contract).
  - *Android:* `data/model/`, `data/local/` (Room entities/DAOs), `domain/engine/` contracts, `build.gradle.kts` dependencies.
- **Tasks:** Define serializable models, database entities (with safe defaults for migrations), `QuizItem`/`FlashcardItem` contracts, and a complete **File Roadmap**.
- **FORBIDDEN:** Writing UI view code (`SwiftUI` `View` or `@Composable` screens), styling, or network/billing calls.

### PHASE 2 — UI Component Specialist
- **Scope:**
  - *iOS:* `Views/`, `Views/Shared/`, `Theme/Theme.swift`, `Services/L10n.swift`.
  - *Android:* `ui/screens/`, `ui/components/`, `ui/theme/Theme.kt`, `data/service/L10n.kt`.
- **Tasks:** Build stateless/presentational UI views (`SwiftUI` Views or `@Composable` functions) using Phase 1 contracts (`val`/props, state callbacks/closures).
- **Design & L10n Rules:**
  - Use `Theme.paper`, `Theme.ink`, `Theme.secondaryInk`, `Theme.accent` (vermilion), `Theme.display(_:)`, `Theme.heading(_:)`, `.inkBordered()`, and `PrimaryButtonStyle`/`PrimaryButton`. Arbitrary colors (`Color.blue`, `Color.Red`, etc.) are prohibited.
  - Hardcoded UI strings are prohibited; define all user-facing strings (TR/EN) in `L10n.swift` (iOS) or `L10n.kt` (Android).
- **FORBIDDEN:** Embedding direct HTTP calls, `Firestore`, database writes (`ModelContext.save()` / DAO mutations), or SRS calculations directly inside UI composables/views.

### PHASE 3 — Integration & State Engineer
- **Scope:**
  - *iOS:* `ViewModels/`, `Services/`, `Views/` (container wiring), `App/`.
  - *Android:* `ViewModel`s, `data/repository/`, `data/service/`, `domain/engine/`, `ui/navigation/`.
- **Tasks:** Wire Phase 2 Views to ViewModels, Services, and local persistence (`SwiftData` on iOS, `Room` on Android). Use `ContentStore` for all JSON loading/chunking, `LevelProgressionService` for level/part locks, `FeatureGate` for Pro quotas, and `TabBarManager` for tab bar visibility.
- **FORBIDDEN:** Breaking or altering the approved UI hierarchy and `Theme` styling from Phase 2.

### PHASE 4 — Security & QA Auditor
- **Scope:** All modified files, secrets configuration, build & unit tests.
- **Tasks:**
  1. **Memory Leaks & Lifecycle:** Verify `Task` cancellation (`deinit`/`.onDisappear`) and `[weak self]` on iOS; verify `viewModelScope`, `DisposableEffect` cleanup (especially for `MediaPlayer`/`TextToSpeech`), and `collectAsStateWithLifecycle` on Android.
  2. **Performance & Concurrency:** Forbid disk I/O, JSON parsing, or heavy Kuromoji tokenization on the main thread during UI rendering (`View.body` or `@Composable` recomposition); dispatch heavy work to background threads (`Dispatchers.IO` / `Dispatchers.Default`).
  3. **Edge-Cases & Security:** Ensure API keys (`Secrets.swift` / `local.properties` / `BuildConfig`) are never logged or committed to Git; verify offline/429/503 handling in `AIService`, purchase restore in `SubscriptionManager`, and anonymous-to-linked UID database migration (`progress_<uid>`).
  4. **Verification:**
     - *iOS:* Run `xcodebuild` to confirm zero build errors and pass verification tests.
     - *Android:* Run `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` inside `Nihongo_android/` to confirm zero build errors and green unit tests.

---

## 3. HUMAN-IN-THE-LOOP CHECKPOINT RULE

1. **No Unapproved Phase Jumps:** When executing the 4-Phase protocol on a feature, stop at the end of each phase. When executing a dedicated phase from the 10-Phase Android Roadmap, complete that roadmap phase's tasks and verification, then stop before merging to `main`.
2. **Diff & Decision Summary:** At each checkpoint, present:
   - **Completed Phase** & **Modified/Created File Paths**
   - **Key Architectural Decisions & Verification Results**
   - **Next Step / Merge Plan**
3. **Explicit Approval Gate:** Wait for explicit Tech Lead approval (**"Approved" / "Proceed" / "Onaylandı" / "Devam et"**) before advancing or merging into `main`.

---

## 4. CODING, NAMING & IMPORT STANDARDS

- **Naming:** `PascalCase` for types/files (`...View`/`...Screen`, `...ViewModel`, `...Service`, `...Manager`). `camelCase` for members; prefix booleans with `is`, `has`, `can`, `needs`.
- **State Pattern:**
  - *iOS:* Do NOT use `ObservableObject`/`@Published`/`@StateObject`. Use `@Observable final class` and `@Observable @MainActor final class <Service> { static let shared = ...; private init() {} }`.
  - *Android:* Use `@HiltViewModel` (or factory-backed `ViewModel`) exposing immutable `StateFlow<UiState>`, and `@Singleton` services injected via Hilt or clean singleton `object`/classes.
- **Architecture Isolation:** Never import `SwiftUI`/`UIKit` in iOS `Models/`, and never import `androidx.compose.*` or `android.view.*` in Android `data/model/` or `domain/engine/`.
- **Comments & Git:** Preserve Turkish `///` / `/** ... */` doc-comments and section dividers. Follow `.agents/rules/git_workflow.md` (feature branches off `main`, incremental commits, merge back to `main` after verification).
