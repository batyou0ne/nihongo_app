package com.batuhankeskn0.kaimon.data.service

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Pro'ya özel özellikler. UI katmanı yalnızca bu enum üzerinden kilit sorar.
 * iOS `FeatureGate.swift` -> `ProFeature` karşılığıdır.
 */
enum class ProFeature {
    UNLIMITED_AI_CHAT,
    LOCKED_CONTENT, // Ücretsiz sınırın ötesindeki ünite/bölümler
    UNLIMITED_EXAMS,
    AD_FREE;

    companion object {
        val unlimitedAIChat: ProFeature = UNLIMITED_AI_CHAT
        val lockedContent: ProFeature = LOCKED_CONTENT
        val unlimitedExams: ProFeature = UNLIMITED_EXAMS
        val adFree: ProFeature = AD_FREE
    }
}

/**
 * Ücretsiz kullanıcı sınırları — tek yerden ayarlanır.
 * iOS `FeatureGate.swift` -> `FreeTierLimits` karşılığıdır.
 */
object FreeTierLimits {
    const val dailyAIMessages: Int = 3
    /** Kelime/Kanji/Dilbilgisi'nde ücretsiz açık bölüm sayısı. */
    const val freeUnitsPerModule: Int = 2
    const val dailyExams: Int = 1

    const val DAILY_AI_MESSAGES: Int = dailyAIMessages
    const val FREE_UNITS_PER_MODULE: Int = freeUnitsPerModule
    const val DAILY_EXAMS: Int = dailyExams
}

/**
 * `DailyQuota` için anahtar-değer saklama soyutlaması.
 * Cihaz üzerinde `SharedPreferences`, JVM birim testlerinde ise `InMemoryQuotaStorage` kullanılır.
 */
interface QuotaStorage {
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun clear()
}

/**
 * Bellek içi (in-memory) kota depolaması — Android `Context` olmayan JVM testlerinde varsayılan olarak çalışır.
 */
class InMemoryQuotaStorage : QuotaStorage {
    private val longs = mutableMapOf<String, Long>()
    private val ints = mutableMapOf<String, Int>()

    override fun getLong(key: String, defaultValue: Long): Long = longs[key] ?: defaultValue

    override fun putLong(key: String, value: Long) {
        longs[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue

    override fun putInt(key: String, value: Int) {
        ints[key] = value
    }

    override fun clear() {
        longs.clear()
        ints.clear()
    }
}

/**
 * Android `SharedPreferences` tabanlı kalıcı kota depolaması.
 */
class SharedPrefsQuotaStorage(
    private val prefs: SharedPreferences
) : QuotaStorage {
    override fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}

/**
 * Günlük kota sayacı (cihaz yerel, gün değişince otomatik sıfırlanır).
 * iOS `FeatureGate.swift` -> `DailyQuota` yapısının birebir karşılığıdır.
 */
class DailyQuota(
    val key: String,
    val limit: Int,
    private var storage: QuotaStorage = InMemoryQuotaStorage(),
    private var nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private var timeZoneProvider: () -> TimeZone = { TimeZone.getDefault() }
) {
    private val dayKey: String
        get() = "quota.$key.day"
    private val countKey: String
        get() = "quota.$key.count"
    private val bonusKey: String
        get() = "quota.$key.bonus"

    internal fun setStorage(newStorage: QuotaStorage) {
        this.storage = newStorage
    }

    internal fun setClock(
        nowProvider: () -> Long,
        tzProvider: () -> TimeZone = { TimeZone.getDefault() }
    ) {
        this.nowMillisProvider = nowProvider
        this.timeZoneProvider = tzProvider
    }

    private fun startOfDaySeconds(nowMillis: Long): Long {
        val calendar = Calendar.getInstance(timeZoneProvider()).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis / 1000L
    }

    private fun rolloverIfNeeded() {
        val today = startOfDaySeconds(nowMillisProvider())
        if (storage.getLong(dayKey, -1L) != today) {
            storage.putLong(dayKey, today)
            storage.putInt(countKey, 0)
            storage.putInt(bonusKey, 0)
        }
    }

    val used: Int
        get() {
            rolloverIfNeeded()
            return storage.getInt(countKey, 0)
        }

    val bonus: Int
        get() {
            rolloverIfNeeded()
            return storage.getInt(bonusKey, 0)
        }

    val remaining: Int
        get() = max(0, limit + bonus - used)

    fun consume() {
        rolloverIfNeeded()
        val currentUsed = storage.getInt(countKey, 0)
        storage.putInt(countKey, currentUsed + 1)
    }

    /**
     * Ödüllü reklam vb. ile ek hak ekler (reklam aşamasında kullanılacak).
     */
    fun grantBonus(amount: Int) {
        if (amount <= 0) return
        rolloverIfNeeded()
        val currentBonus = storage.getInt(bonusKey, 0)
        storage.putInt(bonusKey, currentBonus + amount)
    }

    fun reset() {
        storage.putLong(dayKey, -1L)
        storage.putInt(countKey, 0)
        storage.putInt(bonusKey, 0)
    }
}

/**
 * Özellik erişim kararlarını veren merkezi servis.
 * iOS `Services/Subscription/FeatureGate.swift` sınıfının birebir karşılığıdır.
 */
@Singleton
class FeatureGate {

    private var storage: QuotaStorage
    private var nowMillisProvider: () -> Long

    val aiQuota: DailyQuota
    val examQuota: DailyQuota

    /** Kota değişimlerinde Compose UI'nin yenilenmesi için tetikleyici. */
    var revision: Int by mutableIntStateOf(0)
        private set

    private var _isProState by mutableStateOf(false)
    private var isProProvider: (() -> Boolean)? = null

    /**
     * Kullanıcının aktif Pro aboneliği olup olmadığını döner.
     */
    var isPro: Boolean
        get() = isProProvider?.invoke() ?: _isProState
        set(value) {
            _isProState = value
            revision += 1
        }

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        storage = SharedPrefsQuotaStorage(
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        )
    ) {
        sharedInstance = this
    }

    constructor(
        storage: QuotaStorage = InMemoryQuotaStorage(),
        nowMillisProvider: () -> Long = { System.currentTimeMillis() },
        isProProvider: (() -> Boolean)? = null
    ) {
        this.storage = storage
        this.nowMillisProvider = nowMillisProvider
        this.isProProvider = isProProvider
        this.aiQuota = DailyQuota(
            key = "ai",
            limit = FreeTierLimits.dailyAIMessages,
            storage = storage,
            nowMillisProvider = nowMillisProvider
        )
        this.examQuota = DailyQuota(
            key = "exam",
            limit = FreeTierLimits.dailyExams,
            storage = storage,
            nowMillisProvider = nowMillisProvider
        )
    }

    internal fun attachContext(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sharedStorage = SharedPrefsQuotaStorage(prefs)
        this.storage = sharedStorage
        this.aiQuota.setStorage(sharedStorage)
        this.examQuota.setStorage(sharedStorage)
        revision += 1
    }

    fun setProStatusProvider(provider: (() -> Boolean)?) {
        this.isProProvider = provider
        revision += 1
    }

    fun isUnlocked(feature: ProFeature): Boolean {
        @Suppress("UNUSED_VARIABLE")
        val rev = revision
        return isPro
    }

    /**
     * Bölüm indeksi (`0`'dan başlayarak) ücretsiz kullanıcıya açık mı?
     */
    fun isUnitAvailable(index: Int): Boolean {
        @Suppress("UNUSED_VARIABLE")
        val rev = revision
        return isPro || index < FreeTierLimits.freeUnitsPerModule
    }

    // MARK: - AI Sohbet Kotası

    val remainingAIMessages: Int?
        get() {
            @Suppress("UNUSED_VARIABLE")
            val rev = revision
            return if (isPro) null else aiQuota.remaining
        }

    val canSendAIMessage: Boolean
        get() {
            @Suppress("UNUSED_VARIABLE")
            val rev = revision
            return isPro || aiQuota.remaining > 0
        }

    fun recordAIMessage() {
        if (isPro) return
        aiQuota.consume()
        revision += 1
    }

    fun grantAIBonus(amount: Int) {
        aiQuota.grantBonus(amount)
        revision += 1
    }

    // MARK: - Sınav Kotası

    val remainingExams: Int?
        get() {
            @Suppress("UNUSED_VARIABLE")
            val rev = revision
            return if (isPro) null else examQuota.remaining
        }

    val canStartExam: Boolean
        get() {
            @Suppress("UNUSED_VARIABLE")
            val rev = revision
            return isPro || examQuota.remaining > 0
        }

    fun recordExam() {
        if (isPro) return
        examQuota.consume()
        revision += 1
    }

    fun grantExamBonus(amount: Int) {
        examQuota.grantBonus(amount)
        revision += 1
    }

    fun resetQuotasForTesting() {
        aiQuota.reset()
        examQuota.reset()
        revision += 1
    }

    companion object {
        private const val PREFS_NAME = "kaimon_feature_gate_prefs"

        @Volatile
        private var sharedInstance: FeatureGate = FeatureGate()

        val shared: FeatureGate
            get() = sharedInstance

        fun init(context: Context) {
            sharedInstance.attachContext(context)
        }
    }
}
