package com.batuhankeskn0.kaimon.data.model

/**
 * Kaimon Pro'nun satın alınabilir planları. Ürün kimlikleri Google Play Console'da
 * (abonelikler: `monthly`, `yearly`; tek seferlik ürün: `lifetime`) birebir aynı olmalıdır.
 * iOS `SubscriptionPlan.swift` karşılığıdır.
 */
enum class SubscriptionPlan(val productId: String) {
    MONTHLY("com.batuhankeskn0.kaimon.pro.monthly"),
    YEARLY("com.batuhankeskn0.kaimon.pro.yearly"),
    LIFETIME("com.batuhankeskn0.kaimon.pro.lifetime");

    /** Otomatik yenilenen bir abonelik mi (Play `SUBS`)? `LIFETIME` tek seferlik (`INAPP`) üründür. */
    val isAutoRenewing: Boolean
        get() = this != LIFETIME

    /** Paywall'da ürünlerin sıralanması için (küçük olan önce). */
    val sortOrder: Int
        get() = when (this) {
            YEARLY -> 0
            MONTHLY -> 1
            LIFETIME -> 2
        }

    companion object {
        /** Play Console'daki abonelik grubu/temel plan kimliği (yıllık denemenin tanımlandığı grup). */
        const val GROUP_ID = "kaimon_pro"

        val allProductIds: List<String>
            get() = entries.map { it.productId }

        val subscriptionProductIds: List<String>
            get() = entries.filter { it.isAutoRenewing }.map { it.productId }

        val inAppProductIds: List<String>
            get() = entries.filterNot { it.isAutoRenewing }.map { it.productId }

        fun fromProductId(productId: String): SubscriptionPlan? =
            entries.firstOrNull { it.productId == productId }

        /** Paywall'da gösterim sırasına göre planlar. */
        val sortedForPaywall: List<SubscriptionPlan>
            get() = entries.sortedBy { it.sortOrder }
    }
}

/**
 * Kullanıcının mevcut erişim durumu. Google Play istemci tarafında bitiş tarihi vermediği için
 * `expiresAtMillis` yalnızca bilindiğinde dolu olur (genellikle `null`).
 */
sealed interface ProEntitlement {
    data object Free : ProEntitlement

    data class Pro(
        val plan: SubscriptionPlan,
        val expiresAtMillis: Long? = null
    ) : ProEntitlement

    val isPro: Boolean
        get() = this is Pro
}
