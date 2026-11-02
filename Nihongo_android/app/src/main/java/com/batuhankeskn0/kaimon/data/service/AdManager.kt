package com.batuhankeskn0.kaimon.data.service

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.batuhankeskn0.kaimon.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Geçiş (interstitial) reklam sıklığı politikası: en az [minCompletionsBetweenAds] tamamlanan oturum
 * ve [minIntervalMillis] bekleme süresi geçmeden reklam göstermez. Saf mantık olduğundan JVM'de test edilir.
 */
class InterstitialPolicy(
    private val minCompletionsBetweenAds: Int = 3,
    private val minIntervalMillis: Long = 3 * 60 * 1000L
) {
    private var completionsSinceLastAd = 0
    private var lastShownAtMillis: Long? = null

    /** Bir ders/sınav oturumu tamamlandığında çağrılır; şimdi reklam gösterilmeli mi? */
    @Synchronized
    fun recordCompletionAndShouldShow(nowMillis: Long): Boolean {
        completionsSinceLastAd += 1
        val last = lastShownAtMillis
        val intervalOk = last == null || nowMillis - last >= minIntervalMillis
        return completionsSinceLastAd >= minCompletionsBetweenAds && intervalOk
    }

    @Synchronized
    fun markShown(nowMillis: Long) {
        completionsSinceLastAd = 0
        lastShownAtMillis = nowMillis
    }
}

/**
 * Google Mobile Ads + UMP (GDPR/KVKK onay) yöneticisi.
 *
 * - **`isPro == true` ise tüm reklamlar devre dışıdır:** onay formu gösterilmez, SDK başlatılmaz,
 *   banner/interstitial/rewarded isteği yapılmaz ve yüklü reklamlar bırakılır.
 * - UMP onayı alınmadan (`canRequestAds() == false`) hiçbir reklam isteği yapılmaz.
 * - Reklam kimlikleri `BuildConfig` üzerinden gelir (varsayılan: Google test kimlikleri).
 */
class AdManager internal constructor(
    private val isProProvider: () -> Boolean = { FeatureGate.shared.isPro }
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val isSdkInitialized = AtomicBoolean(false)
    private val interstitialPolicy = InterstitialPolicy()

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false

    private val _canRequestAds = MutableStateFlow(false)

    /** UMP onayı sonrası reklam istenebilir mi? */
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)

    /** Hesap ekranında "Reklam Gizlilik Tercihleri" girişi gösterilmeli mi? */
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    /** Reklam gösterilebilir mi? Pro kullanıcılarda her zaman `false`. */
    val canShowAds: Boolean
        get() = shouldShowAds(isPro = isProProvider(), canRequestAds = _canRequestAds.value)

    // MARK: - UMP onayı

    /**
     * Uygulama açılışında çağrılır: UMP onay bilgisini günceller, gerekiyorsa formu gösterir ve
     * onay uygunsa Mobile Ads SDK'sını başlatır. Pro kullanıcılarda hiçbir şey yapmaz.
     */
    fun gatherConsent(activity: Activity) {
        if (isProProvider()) return
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        // Önceki oturumda onay alınmışsa reklam altyapısını beklemeden başlat.
        publishConsentState(consentInformation, activity)

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "UMP formu hatası: ${formError.errorCode}")
                    }
                    publishConsentState(consentInformation, activity)
                }
            },
            { requestError ->
                Log.w(TAG, "UMP bilgisi güncellenemedi: ${requestError.errorCode}")
                publishConsentState(consentInformation, activity)
            }
        )
    }

    /** Kullanıcının onay tercihlerini yeniden açabilmesi için gizlilik seçenekleri formunu gösterir. */
    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Gizlilik seçenekleri formu hatası: ${formError.errorCode}")
            }
            publishConsentState(UserMessagingPlatform.getConsentInformation(activity), activity)
        }
    }

    private fun publishConsentState(info: ConsentInformation, context: Context) {
        _isPrivacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        val canRequest = info.canRequestAds()
        _canRequestAds.value = canRequest
        if (canRequest && !isProProvider()) {
            initializeSdk(context.applicationContext)
        }
    }

    private fun initializeSdk(context: Context) {
        if (!isSdkInitialized.compareAndSet(false, true)) return
        scope.launch {
            MobileAds.initialize(context) {
                mainHandler.post {
                    loadInterstitial(context)
                    loadRewarded(context)
                }
            }
        }
    }

    // MARK: - Geçiş (interstitial) reklamı

    private fun loadInterstitial(context: Context) {
        if (!canShowAds || interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    /**
     * Bir ders/sınav oturumu tamamlandığında çağrılır. Politika uygunsa ve Pro değilse
     * yüklü geçiş reklamını gösterir; reklam kapandığında [onFinished] çalışır.
     * Reklam gösterilmediyse [onFinished] hemen çağrılır.
     */
    fun onSessionCompleted(activity: Activity, nowMillis: Long = System.currentTimeMillis(), onFinished: () -> Unit = {}) {
        if (!canShowAds) {
            onFinished()
            return
        }
        val ad = interstitialAd
        if (ad == null || !interstitialPolicy.recordCompletionAndShouldShow(nowMillis)) {
            if (ad == null) loadInterstitial(activity.applicationContext)
            onFinished()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                interstitialPolicy.markShown(System.currentTimeMillis())
                loadInterstitial(activity.applicationContext)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                loadInterstitial(activity.applicationContext)
                onFinished()
            }
        }
        ad.show(activity)
    }

    // MARK: - Ödüllü reklam

    private fun loadRewarded(context: Context) {
        if (!canShowAds || rewardedAd != null || isLoadingRewarded) return
        isLoadingRewarded = true
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                }
            }
        )
    }

    /** Ödüllü reklam hazır mı? */
    val isRewardedAdReady: Boolean
        get() = canShowAds && rewardedAd != null

    /**
     * Ödüllü reklamı gösterir; kullanıcı ödülü kazanırsa [onRewarded] çağrılır
     * (ör. `FeatureGate.grantAIBonus`). @return Reklam gösterime başladı mı?
     */
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit): Boolean {
        val ad = rewardedAd
        if (!canShowAds || ad == null) return false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewarded(activity.applicationContext)
            }
        }
        ad.show(activity) { onRewarded() }
        return true
    }

    /** Pro'ya geçildiğinde yüklü reklam nesnelerini bırakır. */
    fun releaseAds() {
        interstitialAd = null
        rewardedAd = null
    }

    companion object {
        private const val TAG = "AdManager"

        @Volatile
        private var sharedInstance: AdManager = AdManager()

        val shared: AdManager
            get() = sharedInstance

        /** Reklam politikası: Pro kullanıcılarda hiçbir zaman, aksi halde yalnızca UMP onayı varsa. */
        fun shouldShowAds(isPro: Boolean, canRequestAds: Boolean): Boolean = !isPro && canRequestAds
    }
}
