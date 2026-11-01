package com.batuhankeskn0.kaimon.domain.engine

import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LevelProgressDao
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.local.UserLevelProgressEntity
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Bir seviyedeki Kanji, Kelime, Gramer ve Genel ilerleme oranlarını (`0.0..1.0`) temsil eder.
 */
data class LevelProgressBreakdown(
    val kanji: Double,
    val vocab: Double,
    val grammar: Double,
    val overall: Double
)

/**
 * JLPT seviye ve bölüm kilitleme mantığını yöneten merkezi servis.
 * Tüm modüller (Kanji, Kelime, Gramer, Okuma) kilit durumlarını bu servis üzerinden sorgular.
 */
@Singleton
class LevelProgressionService {

    private var contentStore: ContentStore

    @Inject
    constructor(contentStore: ContentStore) {
        this.contentStore = contentStore
        sharedInstance = this
    }

    constructor() {
        this.contentStore = ContentStore.shared
    }

    // MARK: - Başlangıç Durumu Oluşturma (Room DAO)

    /**
     * Uygulama ilk açıldığında N5-N1 seviye kayıtlarını hazırlar.
     * N5 varsayılan açık; N4, N3, N2, N1 kilitli başlar.
     */
    suspend fun ensureInitialProgress(
        dao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao
    ): List<UserLevelProgressEntity> {
        val existingRecords = dao.getAllLevelProgress()
        val recordDict = existingRecords.associateBy { it.levelRaw }

        val results = mutableListOf<UserLevelProgressEntity>()
        val missingRecords = mutableListOf<UserLevelProgressEntity>()

        for (level in JLPTLevel.allCases) {
            val existing = recordDict[level.rawValue]
            if (existing != null) {
                results.add(existing)
            } else {
                val isN5 = (level == JLPTLevel.N5)
                val newProgress = UserLevelProgressEntity(
                    levelRaw = level.rawValue,
                    isUnlocked = isN5,
                    isCompleted = false
                )
                missingRecords.add(newProgress)
                results.add(newProgress)
            }
        }

        if (missingRecords.isNotEmpty()) {
            dao.upsertAllLevelProgress(missingRecords)
        }

        return results
    }

    /**
     * Belirtilen seviyeye ait ilerleme kaydını döner.
     */
    suspend fun getProgress(
        level: JLPTLevel,
        dao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao
    ): UserLevelProgressEntity {
        dao.getLevelProgress(level.rawValue)?.let { return it }
        val list = ensureInitialProgress(dao)
        return list.firstOrNull { it.levelRaw == level.rawValue }
            ?: UserLevelProgressEntity(levelRaw = level.rawValue, isUnlocked = level == JLPTLevel.N5)
    }

    // MARK: - Seviye Kilidi Sorgulama

    /**
     * Seviyenin kilidinin açık olup olmadığını döner.
     */
    suspend fun isLevelUnlocked(
        level: JLPTLevel,
        dao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao
    ): Boolean {
        if (level == JLPTLevel.N5) return true
        return getProgress(level, dao).isUnlocked
    }

    fun isLevelUnlocked(
        level: JLPTLevel,
        records: Collection<UserLevelProgressEntity>
    ): Boolean {
        if (level == JLPTLevel.N5) return true
        return records.firstOrNull { it.levelRaw == level.rawValue }?.isUnlocked ?: false
    }

    // MARK: - Bölüm İçi (Intra-Level) Parça Kilidi

    /**
     * Bir seviye içindeki belirli bir parçanın (`partIndex`: 0-indexed) açık olup olmadığını döner.
     * Kural:
     * 1. Seviyenin kendisi açık olmalıdır.
     * 2. İlk parça (`partIndex == 0`) seviye açıksa her zaman açıktır.
     * 3. Sonraki parçalar (`partIndex > 0`), bir önceki parça (`partIndex - 1`) tamamlanmışsa açılır.
     */
    suspend fun isPartUnlocked(
        module: String,
        level: JLPTLevel,
        partIndex: Int,
        dao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao
    ): Boolean {
        if (!isLevelUnlocked(level, dao)) return false
        if (partIndex == 0) return true
        val progress = getProgress(level, dao)
        return progress.isPartCompleted(module = module, index = partIndex - 1)
    }

    fun isPartUnlocked(
        module: String,
        level: JLPTLevel,
        partIndex: Int,
        progress: UserLevelProgressEntity?
    ): Boolean {
        val isUnlocked = if (level == JLPTLevel.N5) true else (progress?.isUnlocked == true)
        if (!isUnlocked) return false
        if (partIndex == 0) return true
        return progress?.isPartCompleted(module = module, index = partIndex - 1) == true
    }

    /**
     * Bir parçayı (örn. Kanji Part 1 veya Vocab Part 3) tamamlandı olarak kaydeder.
     */
    suspend fun markPartCompleted(
        module: String,
        level: JLPTLevel,
        partIndex: Int,
        dao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao
    ): UserLevelProgressEntity {
        val progress = getProgress(level, dao)
        progress.markPartCompleted(module = module, index = partIndex)
        dao.upsertLevelProgress(progress)
        return progress
    }

    // MARK: - İlerleme Hesaplama & Kapı Sınavı Uygunluğu

    /**
     * Seviyedeki toplam ve tamamlanan parça sayılarına göre modül bazlı ve genel ilerleme yüzdesi döner.
     */
    suspend fun calculateLevelProgress(
        level: JLPTLevel,
        dao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao,
        store: ContentStore = contentStore
    ): LevelProgressBreakdown {
        val progress = getProgress(level, dao)
        return calculateLevelProgress(level = level, progress = progress, store = store)
    }

    fun calculateLevelProgress(
        level: JLPTLevel,
        progress: UserLevelProgressEntity,
        store: ContentStore = contentStore
    ): LevelProgressBreakdown {
        val kanjiTotalParts = max(1, store.kanjiParts(level = level.rawValue).size)
        val vocabTotalParts = max(1, store.vocabularyParts(level = level.rawValue).size)
        val grammarTotalUnits = max(1, store.loadGrammarSyllabus(level = level.rawValue).size)

        return calculateLevelProgressWithTotals(
            progress = progress,
            kanjiTotalParts = kanjiTotalParts,
            vocabTotalParts = vocabTotalParts,
            grammarTotalUnits = grammarTotalUnits
        )
    }

    fun calculateLevelProgressWithTotals(
        progress: UserLevelProgressEntity,
        kanjiTotalParts: Int,
        vocabTotalParts: Int,
        grammarTotalUnits: Int
    ): LevelProgressBreakdown {
        val kanjiCompleted = progress.completedPartIndices(module = "kanji").size
        val vocabCompleted = progress.completedPartIndices(module = "vocab").size
        val grammarCompleted = progress.completedPartIndices(module = "grammar").size

        val kanjiRatio = min(1.0, kanjiCompleted.toDouble() / max(1, kanjiTotalParts).toDouble())
        val vocabRatio = min(1.0, vocabCompleted.toDouble() / max(1, vocabTotalParts).toDouble())
        val grammarRatio = min(1.0, grammarCompleted.toDouble() / max(1, grammarTotalUnits).toDouble())

        val overallRatio = (kanjiRatio + vocabRatio + grammarRatio) / 3.0

        return LevelProgressBreakdown(
            kanji = kanjiRatio,
            vocab = vocabRatio,
            grammar = grammarRatio,
            overall = overallRatio
        )
    }

    /**
     * Kullanıcının Seviye Kapı Sınavına (Gateway Exam) girip giremeyeceğini denetler.
     * Kural: Seviye açık olmalı ve modüllerin genel ortalaması en az %85 olmalıdır.
     */
    suspend fun canTakeGatewayExam(
        level: JLPTLevel,
        dao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao,
        store: ContentStore = contentStore
    ): Boolean {
        if (!isLevelUnlocked(level, dao)) return false
        val progress = calculateLevelProgress(level = level, dao = dao, store = store)
        return progress.overall >= level.completionThreshold
    }

    fun canTakeGatewayExam(
        level: JLPTLevel,
        progress: UserLevelProgressEntity,
        store: ContentStore = contentStore
    ): Boolean {
        val isUnlocked = if (level == JLPTLevel.N5) true else progress.isUnlocked
        if (!isUnlocked) return false
        val breakdown = calculateLevelProgress(level = level, progress = progress, store = store)
        return breakdown.overall >= level.completionThreshold
    }

    // MARK: - Seviye Atlama & Üst Seviyenin Kilidini Çözme

    /**
     * Kapı Sınavı veya Test-Out sınavı başarıyla geçildiğinde çağrılır.
     * Mevcut seviyeyi tamamlandı yapar ve bir sonraki seviyenin kilidini açar.
     */
    suspend fun unlockNextLevel(
        afterCompleting: JLPTLevel,
        score: Double,
        isTestOut: Boolean,
        dao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val currentProgress = getProgress(afterCompleting, dao)
        currentProgress.isCompleted = true
        currentProgress.hasPassedGatewayExam = true
        currentProgress.gatewayExamScore = score
        currentProgress.gatewayExamDate = nowMillis
        if (isTestOut) {
            currentProgress.isSkippedViaPlacement = true
        }
        currentProgress.lastUpdated = nowMillis
        dao.upsertLevelProgress(currentProgress)

        val next = afterCompleting.nextLevel
        if (next != null) {
            val nextProgress = getProgress(next, dao)
            nextProgress.isUnlocked = true
            nextProgress.lastUpdated = nowMillis
            dao.upsertLevelProgress(nextProgress)
        }
    }

    companion object {
        @Volatile
        private var sharedInstance: LevelProgressionService = LevelProgressionService()

        val shared: LevelProgressionService
            get() = sharedInstance
    }
}
