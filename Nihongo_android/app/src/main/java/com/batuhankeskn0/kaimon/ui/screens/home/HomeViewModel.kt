package com.batuhankeskn0.kaimon.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.LearningSessionStateEntity
import com.batuhankeskn0.kaimon.data.local.LevelProgressDao
import com.batuhankeskn0.kaimon.data.local.ProgressDao
import com.batuhankeskn0.kaimon.data.local.SessionDao
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.local.UserProgressEntity
import com.batuhankeskn0.kaimon.data.model.CharacterType
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.SubscriptionManager
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

/**
 * Ana ekrandaki "Kaldığın yer" kartının gösterdiği oturum.
 * iOS `HomeView.swift` içindeki `ResumeTarget` yapısının birebir karşılığıdır.
 * `LearningSessionStateEntity.moduleType` (ör. `"vocab_N5_part3"`) ayrıştırılıp
 * hem okunabilir başlığa hem de açılacak modüle çevrilir.
 */
data class ResumeTarget(
    val sessionKey: String,
    val module: Module,
    val title: String,
    val done: Int,
    val total: Int
) {
    sealed interface Module {
        data object Hiragana : Module
        data object Katakana : Module
        data class Kanji(val level: String, val index: Int) : Module
        data class Vocabulary(val level: String, val index: Int) : Module
    }

    val fraction: Double
        get() = if (total > 0) done.toDouble() / total.toDouble() else 0.0

    companion object {
        /**
         * Verilen oturumlar arasından (en yeniden eskiye sıralı) kartta
         * gösterilebilecek ilk tamamlanmamış oturumu seçer.
         */
        fun fromSessions(
            sessions: List<LearningSessionStateEntity>,
            contentStore: ContentStore = ContentStore.shared
        ): ResumeTarget? {
            val sorted = sessions.sortedByDescending { it.lastOpenedAt }
            for (session in sorted) {
                if (session.lastOpenedAt <= 0L) continue
                val target = fromSession(session, contentStore)
                if (target != null) return target
            }
            return null
        }

        fun fromSession(
            session: LearningSessionStateEntity,
            contentStore: ContentStore = ContentStore.shared
        ): ResumeTarget? {
            if (session.lastOpenedAt <= 0L || session.isCompleted) return null

            val key = session.moduleType
            val module: Module
            val title: String
            val total: Int

            when {
                key == CharacterType.HIRAGANA.rawValue -> {
                    module = Module.Hiragana
                    title = "Hiragana"
                    total = LearnableItemKind.HIRAGANA_CHARACTER.totalCount
                }
                key == CharacterType.KATAKANA.rawValue -> {
                    module = Module.Katakana
                    title = "Katakana"
                    total = LearnableItemKind.KATAKANA_CHARACTER.totalCount
                }
                key.startsWith("kanji_") -> {
                    val (level, index) = parsePart(key, prefix = "kanji_") ?: return null
                    val parts = contentStore.kanjiParts(level = level)
                    if (index !in parts.indices) return null
                    module = Module.Kanji(level = level, index = index)
                    title = "$level Kanji's Part ${index + 1}"
                    total = parts[index].size
                }
                key.startsWith("vocab_") -> {
                    val (level, index) = parsePart(key, prefix = "vocab_") ?: return null
                    val parts = contentStore.vocabularyParts(level = level)
                    if (index !in parts.indices) return null
                    module = Module.Vocabulary(level = level, index = index)
                    title = "$level Words Part ${index + 1}"
                    total = parts[index].size
                }
                else -> return null // review_* ve tanınmayan anahtarlar
            }

            // FlashcardSessionView'daki ölçütle aynı: turda hâlâ dolaşan öğeler dışındakiler bitmiş sayılır.
            val inPlay = session.remainingItemIDs.size + session.wrongItemIDs.size
            val done = max(0, total - inPlay)

            // Oturum bitmişse "kaldığın yer" diye bir şey kalmamıştır.
            if (done >= total) return null

            return ResumeTarget(
                sessionKey = key,
                module = module,
                title = title,
                done = done,
                total = total
            )
        }

        /**
         * `"kanji_N5_part3"` -> `("N5", 2)`. Bölüm numarası 1'den başlar, dizi indeksi 0'dan.
         */
        fun parsePart(key: String, prefix: String): Pair<String, Int>? {
            if (!key.startsWith(prefix)) return null
            val rest = key.removePrefix(prefix) // "N5_part3"
            val pieces = rest.split("_part")
            if (pieces.size != 2) return null
            val level = pieces[0]
            val number = pieces[1].toIntOrNull() ?: return null
            if (level.isEmpty() || number <= 0) return null
            return level to (number - 1)
        }
    }
}

/**
 * `HomeScreen` arayüz durumu.
 */
data class HomeUiState(
    val userProgress: UserProgressEntity = UserProgressEntity(),
    val activeStreak: Int = 0,
    val studiedDaysAgoMap: Map<Int, Boolean> = (0..6).associateWith { false },
    val resumeTarget: ResumeTarget? = null,
    val reviewWaitingCount: Int = 0,
    val learnedCounts: Map<LearnableItemKind, Int> = LearnableItemKind.allCases.associateWith { 0 },
    val totalStoriesCount: Int = 0,
    val isPro: Boolean = false,
    val hasAccount: Boolean = false
) {
    fun didStudy(daysAgo: Int): Boolean = studiedDaysAgoMap[daysAgo] ?: false

    fun learnedCount(kind: LearnableItemKind): Int = learnedCounts[kind] ?: 0
}

/**
 * Ana Ekran (`HomeScreen`) durum yöneticisi.
 * iOS `HomeView.swift` içindeki `@Query` sorgularını (`userProgressRecords`, `allProgress`, `sessions`)
 * Room `Flow` akışlarıyla reaktif olarak birleştirir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userDatabaseManager: UserDatabaseManager,
    private val contentStore: ContentStore
) : ViewModel() {

    constructor() : this(
        userDatabaseManager = UserDatabaseManager.shared,
        contentStore = ContentStore.shared
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        ensureInitialProgressAndObserve()
    }

    private fun ensureInitialProgressAndObserve() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val db = userDatabaseManager.getDatabase()
                db.progressDao().getOrCreateUserProgress()
                LevelProgressionService.shared.ensureInitialProgress(db.levelProgressDao())
            }
        }

        viewModelScope.launch {
            userDatabaseManager.currentUidFlow
                .flatMapLatest { uid ->
                    val db = userDatabaseManager.getDatabase(uid)
                    combine(
                        db.progressDao().observeUserProgress(),
                        db.progressDao().observeAllItemProgress(),
                        db.sessionDao().observeAllSessions()
                    ) { userProg, allItems, sessions ->
                        buildHomeUiState(
                            userProgress = userProg ?: UserProgressEntity(),
                            allProgress = allItems,
                            sessions = sessions,
                            contentStore = contentStore,
                            hasAccount = uid != UserDatabaseManager.ANONYMOUS_UID
                        )
                    }
                }
                .flowOn(Dispatchers.IO)
                .collect { state ->
                    _uiState.value = state.copy(isPro = SubscriptionManager.shared.isPro)
                }
        }

        // Pro durumu (satın alma / geri yükleme / debug anahtarı) değiştiğinde Home arayüzünü günceller.
        viewModelScope.launch {
            SubscriptionManager.shared.isProFlow.collect { isPro ->
                _uiState.update { it.copy(isPro = isPro) }
            }
        }
    }

    fun refresh(nowMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val db = userDatabaseManager.getDatabase()
                val userProg = db.progressDao().getOrCreateUserProgress()
                LevelProgressionService.shared.ensureInitialProgress(db.levelProgressDao())
                val allItems = db.progressDao().getAllItemProgress()
                val sessions = db.sessionDao().getAllSessions()
                _uiState.value = buildHomeUiState(
                    userProgress = userProg,
                    allProgress = allItems,
                    sessions = sessions,
                    contentStore = contentStore,
                    hasAccount = userDatabaseManager.currentUid != UserDatabaseManager.ANONYMOUS_UID,
                    isPro = SubscriptionManager.shared.isPro,
                    nowMillis = nowMillis
                )
            }
        }
    }

    companion object {
        fun buildHomeUiState(
            userProgress: UserProgressEntity,
            allProgress: List<LearningItemProgressEntity>,
            sessions: List<LearningSessionStateEntity>,
            contentStore: ContentStore = ContentStore.shared,
            hasAccount: Boolean = false,
            isPro: Boolean = false,
            nowMillis: Long = System.currentTimeMillis()
        ): HomeUiState {
            val activeStreak = userProgress.activeStreak(nowMillis)
            val studiedMap = (0..6).associateWith { daysAgo ->
                userProgress.didStudy(daysAgo = daysAgo, now = nowMillis)
            }

            val reviewCount = allProgress.count { item ->
                item.needsReview || (item.isLearned && item.dueDate <= nowMillis)
            }

            val learnedCounts = LearnableItemKind.allCases.associateWith { kind ->
                allProgress.count { it.itemKind == kind && it.repetitionCount >= 1 }
            }

            val resumeTarget = ResumeTarget.fromSessions(
                sessions = sessions,
                contentStore = contentStore
            )

            val storiesCount = runCatching {
                contentStore.loadStories(level = "N5").size
            }.getOrDefault(0)

            return HomeUiState(
                userProgress = userProgress,
                activeStreak = activeStreak,
                studiedDaysAgoMap = studiedMap,
                resumeTarget = resumeTarget,
                reviewWaitingCount = reviewCount,
                learnedCounts = learnedCounts,
                totalStoriesCount = storiesCount,
                isPro = isPro,
                hasAccount = hasAccount
            )
        }
    }
}
