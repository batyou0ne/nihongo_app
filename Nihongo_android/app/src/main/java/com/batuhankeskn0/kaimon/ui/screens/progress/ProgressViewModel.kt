package com.batuhankeskn0.kaimon.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhankeskn0.kaimon.data.local.DailyActivityEntity
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.local.UserLevelProgressEntity
import com.batuhankeskn0.kaimon.data.local.UserProgressEntity
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AppLanguage
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.data.service.LanguageManager
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressBreakdown
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.domain.engine.TimeTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Son 7 günlük grafikte tek bir günün verisi (dakika ve kazanılan XP).
 */
data class DailyChartPoint(
    val dateString: String,
    val dayLabel: String,
    val minutes: Int,
    val xpEarned: Int,
    val itemsReviewed: Int
)

/**
 * SRS 5 ana kademe dağılım satırı (Çırak, Kalfa, Usta, Aydınlanmış, Kalıcı).
 */
enum class SrsStageGroup {
    APPRENTICE,
    GURU,
    MASTER,
    ENLIGHTENED,
    BURNED;

    val localizedTitle: String
        get() = when (this) {
            APPRENTICE -> L10n.srsStageApprentice
            GURU -> L10n.srsStageGuru
            MASTER -> L10n.srsStageMaster
            ENLIGHTENED -> L10n.srsStageEnlightened
            BURNED -> L10n.srsStageBurned
        }
}

data class SrsStageCount(
    val stage: SrsStageGroup,
    val count: Int,
    val fraction: Double
)

/**
 * JLPT Seviye Durumu satırı (N5–N1).
 */
data class JlptLevelStatus(
    val level: JLPTLevel,
    val isUnlocked: Boolean,
    val breakdown: LevelProgressBreakdown
)

/**
 * Modül bazlı öğrenilen öğe satırı (Hiragana, Katakana, Kanji, Kelime, Gramer).
 */
data class ModuleProgressRow(
    val kind: LearnableItemKind,
    val learned: Int,
    val mastered: Int,
    val total: Int
) {
    val fraction: Double
        get() = if (total > 0) (learned.toDouble() / total.toDouble()).coerceIn(0.0, 1.0) else 0.0
}

/**
 * `ProgressScreen` arayüz durumu.
 */
data class ProgressUiState(
    val activeStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalXP: Int = 0,
    val userLevel: Int = 1,
    val currentLevelXP: Int = 0,
    val nextLevelXP: Int = 500,
    val totalLearnedItemsCount: Int = 0,
    val totalMasteredItemsCount: Int = 0,
    val last7Days: List<DailyChartPoint> = emptyList(),
    val totalMinutesThisWeek: Int = 0,
    val totalXpThisWeek: Int = 0,
    val srsDistribution: List<SrsStageCount> = SrsStageGroup.entries.map { SrsStageCount(it, 0, 0.0) },
    val jlptLevels: List<JlptLevelStatus> = emptyList(),
    val moduleRows: List<ModuleProgressRow> = emptyList()
) {
    val xpFraction: Double
        get() = if (nextLevelXP > 0) (currentLevelXP.toDouble() / nextLevelXP.toDouble()).coerceIn(0.0, 1.0) else 0.0
}

/**
 * İlerleme Ekranı (`ProgressScreen`) ViewModel'i.
 * iOS `Views/Progress/ProgressView.swift` içindeki tüm hesaplamaları ve istatistikleri yönetir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val userDatabaseManager: UserDatabaseManager,
    private val contentStore: ContentStore,
    private val levelProgressionService: LevelProgressionService
) : ViewModel() {

    constructor() : this(
        userDatabaseManager = UserDatabaseManager.shared,
        contentStore = ContentStore.shared,
        levelProgressionService = LevelProgressionService.shared
    )

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        commitSessionAndObserve()
    }

    fun onScreenAppear() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val db = userDatabaseManager.getDatabase()
                TimeTrackingService.shared.commitCurrentSession(activityDao = db.activityDao())
            }
        }
    }

    private fun commitSessionAndObserve() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val db = userDatabaseManager.getDatabase()
                db.progressDao().getOrCreateUserProgress()
                levelProgressionService.ensureInitialProgress(db.levelProgressDao())
                TimeTrackingService.shared.commitCurrentSession(activityDao = db.activityDao())
            }
        }

        viewModelScope.launch {
            userDatabaseManager.currentUidFlow
                .flatMapLatest { uid ->
                    val db = userDatabaseManager.getDatabase(uid)
                    combine(
                        db.progressDao().observeUserProgress(),
                        db.progressDao().observeAllItemProgress(),
                        db.activityDao().observeAllActivities(),
                        db.levelProgressDao().observeAllLevelProgress()
                    ) { userProg, allItems, activities, levelRecords ->
                        buildProgressUiState(
                            userProgress = userProg ?: UserProgressEntity(),
                            allProgress = allItems,
                            dailyActivities = activities,
                            levelProgressRecords = levelRecords,
                            contentStore = contentStore,
                            levelProgressionService = levelProgressionService
                        )
                    }
                }
                .flowOn(Dispatchers.IO)
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    companion object {
        private val dateKeyFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

        fun buildProgressUiState(
            userProgress: UserProgressEntity,
            allProgress: List<LearningItemProgressEntity>,
            dailyActivities: List<DailyActivityEntity>,
            levelProgressRecords: List<UserLevelProgressEntity>,
            contentStore: ContentStore = ContentStore.shared,
            levelProgressionService: LevelProgressionService = LevelProgressionService.shared,
            nowMillis: Long = System.currentTimeMillis(),
            zoneId: ZoneId = ZoneId.systemDefault(),
            language: AppLanguage = LanguageManager.shared.current
        ): ProgressUiState {
            val activeStreak = userProgress.activeStreak(nowMillis, zoneId)
            val longestStreak = maxOf(userProgress.longestStreak, activeStreak)
            val totalXP = userProgress.totalXP
            val userLevel = (totalXP / 500) + 1
            val currentLevelXP = totalXP % 500
            val nextLevelXP = 500

            // "Öğrenildi" = repetitionCount >= 1; "Kalıcı" = isLearned (veya SRS >= 4/7)
            val learnedItems = allProgress.filter { it.repetitionCount >= 1 }
            val totalLearnedItemsCount = maxOf(learnedItems.size, userProgress.totalItemsLearned)
            val totalMasteredItemsCount = allProgress.count { it.isLearned }

            // Son 7 günün verisi (6 gün önce -> bugün)
            val today: LocalDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
            val activityMap = dailyActivities.associateBy { it.dateString }
            val locale = if (language == AppLanguage.TURKISH) Locale("tr", "TR") else Locale.ENGLISH

            val last7Days = (6 downTo 0).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val dateStr = date.format(dateKeyFormatter)
                val activity = activityMap[dateStr]
                val minutes = (activity?.timeSpentSeconds ?: 0) / 60
                val xp = activity?.xpEarned ?: 0
                val reviewed = activity?.itemsReviewed ?: 0
                val rawDayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                val dayLabel = rawDayLabel.replace(".", "").take(3)
                DailyChartPoint(
                    dateString = dateStr,
                    dayLabel = dayLabel,
                    minutes = minutes,
                    xpEarned = xp,
                    itemsReviewed = reviewed
                )
            }

            val totalMinutesThisWeek = last7Days.sumOf { it.minutes }
            val totalXpThisWeek = last7Days.sumOf { it.xpEarned }

            // SRS Kademelerine Göre Dağılım (Çırak 1..3, Kalfa 4, Usta 5, Aydınlanmış 6, Kalıcı >= 7)
            val apprenticeCount = allProgress.count { it.repetitionCount in 1..3 }
            val guruCount = allProgress.count { it.repetitionCount == 4 }
            val masterCount = allProgress.count { it.repetitionCount == 5 }
            val enlightenedCount = allProgress.count { it.repetitionCount == 6 }
            val burnedCount = allProgress.count { it.repetitionCount >= 7 }
            val totalSrsActive = (apprenticeCount + guruCount + masterCount + enlightenedCount + burnedCount)
                .coerceAtLeast(1)

            val srsDistribution = listOf(
                SrsStageCount(SrsStageGroup.APPRENTICE, apprenticeCount, apprenticeCount.toDouble() / totalSrsActive),
                SrsStageCount(SrsStageGroup.GURU, guruCount, guruCount.toDouble() / totalSrsActive),
                SrsStageCount(SrsStageGroup.MASTER, masterCount, masterCount.toDouble() / totalSrsActive),
                SrsStageCount(SrsStageGroup.ENLIGHTENED, enlightenedCount, enlightenedCount.toDouble() / totalSrsActive),
                SrsStageCount(SrsStageGroup.BURNED, burnedCount, burnedCount.toDouble() / totalSrsActive)
            )

            // JLPT Seviye Durumu (N5–N1)
            val recordMap = levelProgressRecords.associateBy { it.levelRaw }
            val jlptLevels = JLPTLevel.allCases.map { level ->
                val record = recordMap[level.rawValue]
                    ?: UserLevelProgressEntity(levelRaw = level.rawValue, isUnlocked = level == JLPTLevel.N5)
                val isUnlocked = if (level == JLPTLevel.N5) true else record.isUnlocked
                val breakdown = levelProgressionService.calculateLevelProgress(
                    level = level,
                    progress = record,
                    store = contentStore
                )
                JlptLevelStatus(
                    level = level,
                    isUnlocked = isUnlocked,
                    breakdown = breakdown
                )
            }

            // Modül Bazlı Öğrenilenler
            val moduleOrder = listOf(
                LearnableItemKind.HIRAGANA_CHARACTER,
                LearnableItemKind.KATAKANA_CHARACTER,
                LearnableItemKind.KANJI,
                LearnableItemKind.VOCABULARY_WORD,
                LearnableItemKind.GRAMMAR
            )
            val moduleRows = moduleOrder.map { kind ->
                val learned = allProgress.count { it.itemKind == kind && it.repetitionCount >= 1 }
                val mastered = allProgress.count { it.itemKind == kind && it.isLearned }
                ModuleProgressRow(
                    kind = kind,
                    learned = learned,
                    mastered = mastered,
                    total = kind.totalCount
                )
            }

            return ProgressUiState(
                activeStreak = activeStreak,
                longestStreak = longestStreak,
                totalXP = totalXP,
                userLevel = userLevel,
                currentLevelXP = currentLevelXP,
                nextLevelXP = nextLevelXP,
                totalLearnedItemsCount = totalLearnedItemsCount,
                totalMasteredItemsCount = totalMasteredItemsCount,
                last7Days = last7Days,
                totalMinutesThisWeek = totalMinutesThisWeek,
                totalXpThisWeek = totalXpThisWeek,
                srsDistribution = srsDistribution,
                jlptLevels = jlptLevels,
                moduleRows = moduleRows
            )
        }
    }
}
