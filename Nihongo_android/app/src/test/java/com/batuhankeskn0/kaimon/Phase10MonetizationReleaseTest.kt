package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.model.ProEntitlement
import com.batuhankeskn0.kaimon.data.model.SubscriptionPlan
import com.batuhankeskn0.kaimon.data.service.AdManager
import com.batuhankeskn0.kaimon.data.service.AuthService
import com.batuhankeskn0.kaimon.data.service.AuthServiceException
import com.batuhankeskn0.kaimon.data.service.AuthUserInfo
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.InMemoryQuotaStorage
import com.batuhankeskn0.kaimon.data.service.InterstitialPolicy
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.data.service.LanguageManager
import com.batuhankeskn0.kaimon.data.service.ProFeature
import com.batuhankeskn0.kaimon.data.service.PurchaseSnapshot
import com.batuhankeskn0.kaimon.data.service.SubscriptionManager
import com.batuhankeskn0.kaimon.data.service.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Faz 10 (Firebase Auth, Firestore, Google Play Billing, AdMob ve Release Hazırlığı) Kapsamlı Birim Testleri.
 */
class Phase10MonetizationReleaseTest {

    @Before
    fun setUp() {
        LanguageManager.shared.setLanguage(LanguageManager.AppLanguage.TURKISH)
    }

    // MARK: - 1. SubscriptionPlan Testleri

    @Test
    fun `subscription plan product ids and types are configured correctly`() {
        assertEquals(3, SubscriptionPlan.entries.size)
        assertEquals(
            "com.batuhankeskn0.kaimon.pro.monthly",
            SubscriptionPlan.MONTHLY.productId
        )
        assertEquals(
            "com.batuhankeskn0.kaimon.pro.yearly",
            SubscriptionPlan.YEARLY.productId
        )
        assertEquals(
            "com.batuhankeskn0.kaimon.pro.lifetime",
            SubscriptionPlan.LIFETIME.productId
        )

        assertTrue(SubscriptionPlan.MONTHLY.isAutoRenewing)
        assertTrue(SubscriptionPlan.YEARLY.isAutoRenewing)
        assertFalse(SubscriptionPlan.LIFETIME.isAutoRenewing)

        val subIds = SubscriptionPlan.subscriptionProductIds
        assertEquals(2, subIds.size)
        assertTrue(subIds.contains(SubscriptionPlan.MONTHLY.productId))
        assertTrue(subIds.contains(SubscriptionPlan.YEARLY.productId))

        val inAppIds = SubscriptionPlan.inAppProductIds
        assertEquals(1, inAppIds.size)
        assertTrue(inAppIds.contains(SubscriptionPlan.LIFETIME.productId))

        assertEquals(SubscriptionPlan.YEARLY, SubscriptionPlan.fromProductId("com.batuhankeskn0.kaimon.pro.yearly"))
        assertNull(SubscriptionPlan.fromProductId("unknown.product"))

        val sorted = SubscriptionPlan.sortedForPaywall
        assertEquals(SubscriptionPlan.YEARLY, sorted[0])
        assertEquals(SubscriptionPlan.MONTHLY, sorted[1])
        assertEquals(SubscriptionPlan.LIFETIME, sorted[2])
    }

    // MARK: - 2. SubscriptionManager Entitlement Resolution Testleri

    @Test
    fun `resolveEntitlement returns Free when purchases are empty or not purchased`() {
        val emptyResult = SubscriptionManager.resolveEntitlement(emptyList())
        assertEquals(ProEntitlement.Free, emptyResult)
        assertFalse(emptyResult.isPro)

        val pendingPurchases = listOf(
            PurchaseSnapshot(
                productIds = listOf(SubscriptionPlan.YEARLY.productId),
                isPurchased = false
            )
        )
        val pendingResult = SubscriptionManager.resolveEntitlement(pendingPurchases)
        assertEquals(ProEntitlement.Free, pendingResult)
    }

    @Test
    fun `resolveEntitlement prioritizes Lifetime over Yearly and Monthly`() {
        val monthlyPurchase = listOf(
            PurchaseSnapshot(
                productIds = listOf(SubscriptionPlan.MONTHLY.productId),
                isPurchased = true
            )
        )
        val monthlyResult = SubscriptionManager.resolveEntitlement(monthlyPurchase)
        assertEquals(ProEntitlement.Pro(SubscriptionPlan.MONTHLY), monthlyResult)
        assertTrue(monthlyResult.isPro)

        val multiPurchases = listOf(
            PurchaseSnapshot(
                productIds = listOf(SubscriptionPlan.MONTHLY.productId),
                isPurchased = true
            ),
            PurchaseSnapshot(
                productIds = listOf(SubscriptionPlan.LIFETIME.productId),
                isPurchased = true
            )
        )
        val multiResult = SubscriptionManager.resolveEntitlement(multiPurchases)
        assertEquals(ProEntitlement.Pro(SubscriptionPlan.LIFETIME), multiResult)
    }

    @Test
    fun `monthlyEquivalentPrice calculates correct formatted value`() {
        val equivalent = SubscriptionManager.monthlyEquivalentPrice(
            yearlyPriceMicros = 120_000_000L, // 120 TRY -> aylık 10 TRY
            currencyCode = "TRY",
            locale = Locale("tr", "TR")
        )
        assertTrue(equivalent.contains("10") || equivalent.contains("10,00"))
    }

    // MARK: - 3. AdManager & InterstitialPolicy Testleri

    @Test
    fun `shouldShowAds is strictly false for Pro users regardless of consent`() {
        assertFalse(AdManager.shouldShowAds(isPro = true, canRequestAds = true))
        assertFalse(AdManager.shouldShowAds(isPro = true, canRequestAds = false))
        assertFalse(AdManager.shouldShowAds(isPro = false, canRequestAds = false))
        assertTrue(AdManager.shouldShowAds(isPro = false, canRequestAds = true))
    }

    @Test
    fun `interstitial policy respects completion count and interval`() {
        val policy = InterstitialPolicy(minCompletionsBetweenAds = 3, minIntervalMillis = 60_000L)
        val baseTime = 1_000_000L

        // 1. tamamlanma: gösterilmemeli
        assertFalse(policy.recordCompletionAndShouldShow(baseTime))
        // 2. tamamlanma: gösterilmemeli
        assertFalse(policy.recordCompletionAndShouldShow(baseTime + 5_000L))
        // 3. tamamlanma: gösterilmeli
        assertTrue(policy.recordCompletionAndShouldShow(baseTime + 10_000L))

        // Gösterildi olarak işaretlenir
        policy.markShown(baseTime + 10_000L)

        // Gösterildikten sonra hemen 3 tamamlanma gelse bile süre kısıtı (60 saniye) engel olmalı
        assertFalse(policy.recordCompletionAndShouldShow(baseTime + 20_000L))
        assertFalse(policy.recordCompletionAndShouldShow(baseTime + 30_000L))
        // 3. tamamlanma ama henüz 60 sn geçmedi:
        assertFalse(policy.recordCompletionAndShouldShow(baseTime + 40_000L))

        // 60 saniyeden sonra:
        assertTrue(policy.recordCompletionAndShouldShow(baseTime + 80_000L))
    }

    // MARK: - 4. UserProfile Firestore Mapping Testleri

    @Test
    fun `user profile mapping handles serialization and missing fields gracefully`() {
        val original = UserProfile(
            firstName = "Batu",
            lastName = "Keskin",
            age = 25
        )
        assertEquals("Batu Keskin", original.fullName)

        val map = original.toFirestoreMap()
        assertEquals("Batu", map[UserProfile.FIELD_FIRST_NAME])
        assertEquals("Keskin", map[UserProfile.FIELD_LAST_NAME])
        assertEquals(25, map[UserProfile.FIELD_AGE])

        val restored = UserProfile.fromFirestoreMap(map)
        assertNotNull(restored)
        assertEquals("Batu", restored?.firstName)
        assertEquals("Keskin", restored?.lastName)
        assertEquals(25, restored?.age)

        // Boş veya eksik harita
        val fromEmpty = UserProfile.fromFirestoreMap(emptyMap())
        assertNotNull(fromEmpty)
        assertEquals("", fromEmpty?.firstName)
        assertNull(fromEmpty?.age)

        assertNull(UserProfile.fromFirestoreMap(null))
    }

    // MARK: - 5. AuthService & AuthUserInfo Testleri

    @Test
    fun `auth user info properties and descriptions are accurate`() {
        val guest = AuthUserInfo(
            uid = "anon_123",
            isAnonymous = true,
            email = null
        )
        assertFalse(guest.hasAccount)
        assertEquals("", guest.accountDescription)
        assertTrue(guest.providerNames.isEmpty())

        val linkedGoogleUser = AuthUserInfo(
            uid = "user_456",
            isAnonymous = false,
            email = "kaimon@example.com",
            displayName = "Kaimon Learner",
            providerIds = listOf(AuthUserInfo.PROVIDER_GOOGLE, AuthUserInfo.PROVIDER_FIREBASE)
        )
        assertTrue(linkedGoogleUser.hasAccount)
        assertTrue(linkedGoogleUser.isLinkedToGoogle)
        assertEquals("kaimon@example.com", linkedGoogleUser.accountDescription)
        assertEquals(listOf("Google"), linkedGoogleUser.providerNames)
    }

    @Test
    fun `auth service friendly message maps known errors to localized strings`() {
        LanguageManager.shared.setLanguage(LanguageManager.AppLanguage.TURKISH)

        val invalidEmailMsg = AuthService.friendlyMessageForCode("ERROR_INVALID_EMAIL")
        assertEquals(L10n.authInvalidEmail, invalidEmailMsg)

        val unavailableMsg = AuthService.friendlyMessageForKind(AuthServiceException.Kind.FIREBASE_UNAVAILABLE)
        assertEquals(L10n.authFirebaseUnavailable, unavailableMsg)

        val googleMissingMsg = AuthService.friendlyMessageForKind(AuthServiceException.Kind.MISSING_GOOGLE_CONFIG)
        assertEquals(L10n.authMissingGoogleConfig, googleMissingMsg)
    }

    // MARK: - 6. UserDatabaseManager UID Resolution Testleri

    @Test
    fun `resolveDatabaseUid correctly isolates anonymous and registered users`() {
        assertEquals("anonymous", UserDatabaseManager.resolveDatabaseUid(null, isAnonymous = true))
        assertEquals("anonymous", UserDatabaseManager.resolveDatabaseUid("", isAnonymous = true))
        assertEquals("anonymous", UserDatabaseManager.resolveDatabaseUid("user_abc", isAnonymous = true))
        assertEquals("user_abc", UserDatabaseManager.resolveDatabaseUid("user_abc", isAnonymous = false))
        assertEquals("user_123_xyz", UserDatabaseManager.resolveDatabaseUid("user!123?xyz", isAnonymous = false))
    }

    // MARK: - 7. FeatureGate Pro Entitlement Testleri

    @Test
    fun `feature gate unlocks all limits when user is pro`() {
        val storage = InMemoryQuotaStorage()
        val featureGate = FeatureGate(storage = storage)

        featureGate.isPro = false
        assertFalse(featureGate.isUnlocked(ProFeature.UNLIMITED_AI_CHAT))
        assertFalse(featureGate.isUnlocked(ProFeature.AD_FREE))
        assertTrue(featureGate.isUnitAvailable(0))
        assertTrue(featureGate.isUnitAvailable(1))
        assertFalse(featureGate.isUnitAvailable(2)) // 3. ünite ücretsiz kullanıcıya kilitli

        featureGate.isPro = true
        assertTrue(featureGate.isUnlocked(ProFeature.UNLIMITED_AI_CHAT))
        assertTrue(featureGate.isUnlocked(ProFeature.AD_FREE))
        assertTrue(featureGate.isUnlocked(ProFeature.LOCKED_CONTENT))
        assertTrue(featureGate.isUnitAvailable(2))
        assertTrue(featureGate.isUnitAvailable(10))
        assertNull(featureGate.remainingAIMessages) // Pro kullanıcıda sınır yok (null)
        assertNull(featureGate.remainingExams)
    }
}
