package com.batuhankeskn0.kaimon.data.service

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.batuhankeskn0.kaimon.BuildConfig
import com.batuhankeskn0.kaimon.data.model.ProEntitlement
import com.batuhankeskn0.kaimon.data.model.SubscriptionPlan
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.coroutines.resume

/** Satın alma akışının sonucu. iOS `PurchaseOutcome` karşılığıdır. */
enum class PurchaseOutcome {
    SUCCESS,
    PENDING,
    CANCELLED
}

/**
 * Paywall'da gösterilen, `ProductDetails`'ten bağımsız (UI-güvenli) plan fiyat bilgisi.
 * @property priceAmountMicros Yenilenen/tek seferlik ana fiyat (mikro birim); deneme fiyatı DEĞİLDİR.
 * @property hasFreeTrial Kullanıcının uygun olduğu ücretsiz deneme teklifi var mı?
 */
data class PlanOffer(
    val plan: SubscriptionPlan,
    val displayPrice: String,
    val priceAmountMicros: Long,
    val currencyCode: String,
    val hasFreeTrial: Boolean = false
)

/** `Purchase` nesnesinin test edilebilir sade özeti. */
data class PurchaseSnapshot(
    val productIds: List<String>,
    val isPurchased: Boolean
)

/**
 * Google Play Billing Library v7 tabanlı abonelik yöneticisi. Tek doğruluk kaynağı: [isPro].
 * iOS `SubscriptionManager.swift` karşılığıdır.
 *
 * - Ürünleri yükler (`queryProductDetails`), satın alma (`launchBillingFlow`) ve geri yükleme
 *   (`queryPurchasesAsync`) işlemlerini yürütür.
 * - `PurchasesUpdatedListener` ile yenileme/onay akışını dinler; bekleyen olmayan alımları onaylar (`acknowledgePurchase`).
 * - Hak durumu [FeatureGate]'e yansıtılır; böylece tüm ekranlardaki kilitler otomatik güncellenir.
 * - Yalnızca DEBUG derlemelerinde [debugForcePro] ile Pro, Play Billing olmadan açılıp kapatılabilir.
 */
class SubscriptionManager internal constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val connectionMutex = Mutex()

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private var billingClient: BillingClient? = null

    private val productDetailsByPlan = mutableMapOf<SubscriptionPlan, ProductDetails>()
    private val offerTokenByPlan = mutableMapOf<SubscriptionPlan, String>()

    @Volatile
    private var pendingPurchase: CompletableDeferred<PurchaseOutcome>? = null

    private val _offers = MutableStateFlow<List<PlanOffer>>(emptyList())
    val offers: StateFlow<List<PlanOffer>> = _offers.asStateFlow()

    private val _entitlement = MutableStateFlow<ProEntitlement>(ProEntitlement.Free)
    val entitlement: StateFlow<ProEntitlement> = _entitlement.asStateFlow()

    private val _isLoadingProducts = MutableStateFlow(false)
    val isLoadingProducts: StateFlow<Boolean> = _isLoadingProducts.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _debugForcePro = MutableStateFlow(false)

    /** Geliştirme sırasında Pro'yu Play Billing olmadan açıp kapatmak için (yalnızca DEBUG derlemesinde etkindir). */
    val debugForcePro: StateFlow<Boolean> = _debugForcePro.asStateFlow()

    private val _isPro = MutableStateFlow(false)

    /** Pro erişimi var mı (`debugForcePro` veya geçerli satın alma)? */
    val isProFlow: StateFlow<Boolean> = _isPro.asStateFlow()

    val isPro: Boolean
        get() = _isPro.value

    /** `KaimonApplication.onCreate` içinde çağrılır; BillingClient'ı kurar ve mevcut hakları arka planda yeniler. */
    internal fun attachContext(context: Context) {
        val app = context.applicationContext
        appContext = app
        val sharedPrefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sharedPrefs

        _debugForcePro.value = BuildConfig.DEBUG && sharedPrefs.getBoolean(KEY_DEBUG_FORCE_PRO, false)
        val cachedPlan = sharedPrefs.getString(KEY_CACHED_PLAN, null)?.let { SubscriptionPlan.fromProductId(it) }
        _entitlement.value = cachedPlan?.let { ProEntitlement.Pro(it) } ?: ProEntitlement.Free
        publishProStatus()

        billingClient = BillingClient.newBuilder(app)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()

        scope.launch { refresh() }
    }

    // MARK: - Debug anahtarı

    /** Yalnızca DEBUG derlemelerinde etkili olur; release'te çağrılsa bile Pro açılmaz. */
    fun setDebugForcePro(enabled: Boolean) {
        val effective = BuildConfig.DEBUG && enabled
        _debugForcePro.value = effective
        prefs?.edit()?.putBoolean(KEY_DEBUG_FORCE_PRO, effective)?.apply()
        publishProStatus()
    }

    // MARK: - Ürünler

    suspend fun loadProducts() {
        if (_offers.value.isNotEmpty()) return
        _isLoadingProducts.value = true
        try {
            if (!ensureReady()) {
                _lastError.value = L10n.billingUnavailable
                return
            }
            val client = billingClient ?: return
            val loaded = mutableListOf<ProductDetails>()
            for ((type, ids) in listOf(
                BillingClient.ProductType.SUBS to SubscriptionPlan.subscriptionProductIds,
                BillingClient.ProductType.INAPP to SubscriptionPlan.inAppProductIds
            )) {
                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        ids.map {
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(it)
                                .setProductType(type)
                                .build()
                        }
                    )
                    .build()
                val result = client.queryProductDetails(params)
                if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    loaded += result.productDetailsList.orEmpty()
                }
            }

            val offers = mutableListOf<PlanOffer>()
            for (details in loaded) {
                val plan = SubscriptionPlan.fromProductId(details.productId) ?: continue
                buildOffer(plan, details)?.let { offer ->
                    productDetailsByPlan[plan] = details
                    offers += offer
                }
            }
            _offers.value = offers.sortedBy { it.plan.sortOrder }
            if (offers.isEmpty()) {
                _lastError.value = L10n.productsUnavailable
            }
        } catch (error: Exception) {
            Log.w(TAG, "Ürünler yüklenemedi: ${error.javaClass.simpleName}")
            _lastError.value = L10n.productsUnavailable
        } finally {
            _isLoadingProducts.value = false
        }
    }

    private fun buildOffer(plan: SubscriptionPlan, details: ProductDetails): PlanOffer? {
        if (plan.isAutoRenewing) {
            val offerDetails = details.subscriptionOfferDetails.orEmpty()
            // Play yalnızca kullanıcının uygun olduğu teklifleri döner; deneme içeren teklif varsa onu seç.
            val chosen = offerDetails.firstOrNull { offer ->
                offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
            } ?: offerDetails.firstOrNull { it.offerId == null } ?: offerDetails.firstOrNull() ?: return null

            val phases = chosen.pricingPhases.pricingPhaseList
            val recurring = phases.lastOrNull() ?: return null
            offerTokenByPlan[plan] = chosen.offerToken
            return PlanOffer(
                plan = plan,
                displayPrice = recurring.formattedPrice,
                priceAmountMicros = recurring.priceAmountMicros,
                currencyCode = recurring.priceCurrencyCode,
                hasFreeTrial = phases.any { it.priceAmountMicros == 0L }
            )
        }
        val oneTime = details.oneTimePurchaseOfferDetails ?: return null
        return PlanOffer(
            plan = plan,
            displayPrice = oneTime.formattedPrice,
            priceAmountMicros = oneTime.priceAmountMicros,
            currencyCode = oneTime.priceCurrencyCode
        )
    }

    fun offerFor(plan: SubscriptionPlan): PlanOffer? = _offers.value.firstOrNull { it.plan == plan }

    // MARK: - Satın alma

    /**
     * Seçilen planın Google Play satın alma akışını başlatır. Sonuç `PurchasesUpdatedListener` üzerinden gelir.
     * `Activity` bağlamı gerektirdiğinden UI katmanı mevcut `Activity`'yi geçirir (burada saklanmaz).
     */
    suspend fun purchase(activity: Activity, plan: SubscriptionPlan): PurchaseOutcome {
        _isPurchasing.value = true
        _lastError.value = null
        try {
            if (!ensureReady()) {
                _lastError.value = L10n.billingUnavailable
                return PurchaseOutcome.CANCELLED
            }
            val client = billingClient ?: return PurchaseOutcome.CANCELLED
            if (productDetailsByPlan[plan] == null) loadProducts()
            val details = productDetailsByPlan[plan]
            if (details == null) {
                _lastError.value = L10n.productsUnavailable
                return PurchaseOutcome.CANCELLED
            }

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .apply { offerTokenByPlan[plan]?.let { setOfferToken(it) } }
                .build()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()

            val deferred = CompletableDeferred<PurchaseOutcome>()
            pendingPurchase = deferred
            val launchResult = withContext(Dispatchers.Main) { client.launchBillingFlow(activity, flowParams) }
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                pendingPurchase = null
                return handleBillingFailure(launchResult)
            }
            return deferred.await()
        } finally {
            _isPurchasing.value = false
        }
    }

    private fun handleBillingFailure(result: BillingResult): PurchaseOutcome {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                scope.launch { refresh() }
                return PurchaseOutcome.SUCCESS
            }
            else -> _lastError.value = L10n.purchaseFailed
        }
        return PurchaseOutcome.CANCELLED
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        scope.launch {
            val deferred = pendingPurchase
            pendingPurchase = null
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val list = purchases.orEmpty()
                    list.forEach { acknowledgeIfNeeded(it) }
                    refresh()
                    val outcome = when {
                        list.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } -> PurchaseOutcome.SUCCESS
                        list.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> PurchaseOutcome.PENDING
                        else -> PurchaseOutcome.CANCELLED
                    }
                    deferred?.complete(outcome)
                }
                else -> deferred?.complete(handleBillingFailure(result))
            }
        }
    }

    /** "Satın Alımları Geri Yükle" (Play politikası zorunluluğu). @return Geri yükleme sonunda Pro aktif mi? */
    suspend fun restorePurchases(): Boolean {
        _lastError.value = null
        val ok = refresh()
        if (!ok) _lastError.value = L10n.billingUnavailable
        return _entitlement.value.isPro
    }

    // MARK: - Hak durumu

    /**
     * Mevcut geçerli satın alımlardan hak durumunu yeniden hesaplar (`queryPurchasesAsync`).
     * Play erişilemezse önbellekteki hak korunur. @return Sorgu başarılı mı?
     */
    suspend fun refresh(): Boolean {
        if (!ensureReady()) return false
        val client = billingClient ?: return false
        val snapshots = mutableListOf<PurchaseSnapshot>()
        var allSucceeded = true

        for (type in listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)) {
            val result = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(type).build()
            )
            if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                allSucceeded = false
                continue
            }
            result.purchasesList.forEach { purchase ->
                acknowledgeIfNeeded(purchase)
                snapshots += PurchaseSnapshot(
                    productIds = purchase.products,
                    isPurchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                )
            }
        }

        if (allSucceeded) {
            setEntitlement(resolveEntitlement(snapshots))
        }
        return allSucceeded
    }

    private fun setEntitlement(value: ProEntitlement) {
        _entitlement.value = value
        val planId = (value as? ProEntitlement.Pro)?.plan?.productId
        prefs?.edit()?.apply {
            if (planId != null) putString(KEY_CACHED_PLAN, planId) else remove(KEY_CACHED_PLAN)
        }?.apply()
        publishProStatus()
    }

    private fun publishProStatus() {
        val pro = _debugForcePro.value || _entitlement.value.isPro
        _isPro.value = pro
        FeatureGate.shared.isPro = pro
    }

    // MARK: - Dahili

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isAcknowledged) return
        val client = billingClient ?: return
        runCatching {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            )
        }.onFailure { Log.w(TAG, "Satın alma onaylanamadı: ${it.javaClass.simpleName}") }
    }

    /** BillingClient bağlı değilse bağlanır; bağlantı kurulabildi mi döner. */
    private suspend fun ensureReady(): Boolean = connectionMutex.withLock {
        val client = billingClient ?: return@withLock false
        if (client.isReady) return@withLock true
        suspendCancellableCoroutine { continuation: CancellableContinuation<Boolean> ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) {
                        continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Bir sonraki işlemde `ensureReady` yeniden bağlanır.
                }
            })
        }
    }

    companion object {
        private const val TAG = "SubscriptionManager"
        private const val PREFS_NAME = "kaimon_subscription_prefs"
        private const val KEY_DEBUG_FORCE_PRO = "debugForcePro"
        private const val KEY_CACHED_PLAN = "cachedProPlan"

        @Volatile
        private var sharedInstance: SubscriptionManager = SubscriptionManager()

        val shared: SubscriptionManager
            get() = sharedInstance

        fun init(context: Context) {
            sharedInstance.attachContext(context)
        }

        /**
         * Satın alım özetlerinden en güçlü hakkı seçer: `LIFETIME` > `YEARLY` > `MONTHLY` > ücretsiz.
         * Yalnızca `PURCHASED` durumundaki alımlar sayılır (bekleyen alımlar hak vermez).
         */
        fun resolveEntitlement(purchases: List<PurchaseSnapshot>): ProEntitlement {
            val owned = purchases
                .filter { it.isPurchased }
                .flatMap { it.productIds }
                .mapNotNull { SubscriptionPlan.fromProductId(it) }
                .toSet()
            val best = listOf(SubscriptionPlan.LIFETIME, SubscriptionPlan.YEARLY, SubscriptionPlan.MONTHLY)
                .firstOrNull { it in owned }
            return best?.let { ProEntitlement.Pro(it) } ?: ProEntitlement.Free
        }

        /** Yıllık planın aylık eşdeğer fiyatını (yerel para biçimiyle) üretir. */
        fun monthlyEquivalentPrice(
            yearlyPriceMicros: Long,
            currencyCode: String,
            locale: Locale = Locale.getDefault()
        ): String {
            val format = NumberFormat.getCurrencyInstance(locale)
            runCatching { format.currency = Currency.getInstance(currencyCode) }
            return format.format(yearlyPriceMicros / 12.0 / 1_000_000.0)
        }

        /** Play Store abonelik yönetimi sayfasına derin bağlantı. */
        fun manageSubscriptionUrl(plan: SubscriptionPlan, packageName: String): String =
            "https://play.google.com/store/account/subscriptions?sku=${plan.productId}&package=$packageName"
    }
}
