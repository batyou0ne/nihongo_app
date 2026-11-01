package com.batuhankeskn0.kaimon.data.service

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Top-level typealias: hem `AppLanguage.TURKISH` hem `LanguageManager.AppLanguage.TURKISH` erişimini destekler. */
typealias AppLanguage = LanguageManager.AppLanguage

/**
 * Uygulamanın arayüz dilini yöneten servis. Kullanıcı profil ekranından
 * Türkçe veya İngilizce seçebilir; tercih SharedPreferences'ta saklanır.
 * Hem Jetpack Compose anlık recomposition (`mutableStateOf`) hem de
 * Coroutines / ViewModel akışı (`StateFlow<AppLanguage>`) sağlar.
 */
@Singleton
class LanguageManager private constructor() {

    enum class AppLanguage(val rawValue: String) {
        TURKISH("tr"),
        ENGLISH("en");

        val id: String
            get() = rawValue

        val displayName: String
            get() = when (this) {
                TURKISH -> "Türkçe"
                ENGLISH -> "English"
            }

        val flag: String
            get() = when (this) {
                TURKISH -> "🇹🇷"
                ENGLISH -> "🇬🇧"
            }

        companion object {
            val turkish: AppLanguage = TURKISH
            val english: AppLanguage = ENGLISH
            val allCases: List<AppLanguage>
                get() = entries

            fun fromRawValue(rawValue: String?): AppLanguage =
                entries.firstOrNull { it.rawValue == rawValue } ?: TURKISH
        }
    }

    @Inject
    constructor(@ApplicationContext context: Context) : this() {
        attachContext(context)
        sharedInstance = this
    }

    private var prefs: SharedPreferences? = null

    private var _currentComposeState by mutableStateOf(AppLanguage.TURKISH)
    private val _currentLanguageFlow = MutableStateFlow(AppLanguage.TURKISH)

    /** ViewModel ve Coroutine katmanları için reaktif dil akışı. */
    val currentLanguageFlow: StateFlow<AppLanguage> = _currentLanguageFlow.asStateFlow()

    /** `currentLanguageFlow` için kısa erişim takma adı. */
    val languageFlow: StateFlow<AppLanguage>
        get() = currentLanguageFlow

    /**
     * Aktif uygulama dili.
     * Compose `@Composable` fonksiyonları içinden (`L10n.*` üzerinden) okunduğunda
     * `_currentComposeState` sayesinde dil değiştiği anda otomatik recomposition tetikler.
     */
    var current: AppLanguage
        get() = _currentComposeState
        set(value) {
            _currentComposeState = value
            _currentLanguageFlow.value = value
            prefs?.edit()?.putString(STORAGE_KEY, value.rawValue)?.apply()
            if (this !== sharedInstance) {
                sharedInstance._currentComposeState = value
                sharedInstance._currentLanguageFlow.value = value
            }
        }

    fun setLanguage(language: AppLanguage) {
        current = language
    }

    internal fun attachContext(context: Context) {
        val sharedPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sharedPrefs
        val savedRaw = sharedPrefs.getString(STORAGE_KEY, null)
        val resolved = AppLanguage.fromRawValue(savedRaw)
        _currentComposeState = resolved
        _currentLanguageFlow.value = resolved
    }

    companion object {
        private const val PREFS_NAME = "kaimon_language_prefs"
        private const val STORAGE_KEY = "app_language"

        @Volatile
        private var sharedInstance: LanguageManager = LanguageManager()

        val shared: LanguageManager
            get() = sharedInstance

        var current: AppLanguage
            get() = sharedInstance.current
            set(value) {
                sharedInstance.current = value
            }

        val currentLanguageFlow: StateFlow<AppLanguage>
            get() = sharedInstance.currentLanguageFlow

        val languageFlow: StateFlow<AppLanguage>
            get() = sharedInstance.languageFlow

        fun init(context: Context) {
            sharedInstance.attachContext(context)
        }

        fun setLanguage(language: AppLanguage) {
            sharedInstance.current = language
        }
    }
}
