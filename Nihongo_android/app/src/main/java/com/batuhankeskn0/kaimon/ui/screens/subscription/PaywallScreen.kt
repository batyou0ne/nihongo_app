package com.batuhankeskn0.kaimon.ui.screens.subscription

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhankeskn0.kaimon.data.model.ProEntitlement
import com.batuhankeskn0.kaimon.data.model.SubscriptionPlan
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.data.service.PurchaseOutcome
import com.batuhankeskn0.kaimon.data.service.SubscriptionManager
import com.batuhankeskn0.kaimon.ui.components.ProBenefitRow
import com.batuhankeskn0.kaimon.ui.components.ProPlanCard
import com.batuhankeskn0.kaimon.ui.components.findActivity
import com.batuhankeskn0.kaimon.ui.theme.PrimaryButton
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.launch

/**
 * Kaimon Pro Satın Alma Ekranı.
 * iOS `Views/Subscription/PaywallView.swift` bileşeninin birebir Jetpack Compose karşılığıdır.
 *
 * - Pro avantajlarını listeler (tüm N5 içeriği, sınırsız AI sohbeti, reklamsız deneyim, sınırsız sınav).
 * - Yıllık (en avantajlı), Aylık ve Ömür Boyu plan kartlarını Google Play fiyatlarıyla sunar.
 * - Google Play Billing üzerinden satın alma ve geri yükleme akışlarını yönetir.
 * - Pro aktif olduğunda aktif plan detaylarını ve Google Play abonelik yönetme bağlantısını sunar.
 */
@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hostActivity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    val isPro by SubscriptionManager.shared.isProFlow.collectAsStateWithLifecycle()
    val entitlement by SubscriptionManager.shared.entitlement.collectAsStateWithLifecycle()
    val offers by SubscriptionManager.shared.offers.collectAsStateWithLifecycle()
    val isLoadingProducts by SubscriptionManager.shared.isLoadingProducts.collectAsStateWithLifecycle()
    val isPurchasing by SubscriptionManager.shared.isPurchasing.collectAsStateWithLifecycle()
    val lastError by SubscriptionManager.shared.lastError.collectAsStateWithLifecycle()

    var selectedPlan by rememberSaveable { mutableStateOf(SubscriptionPlan.YEARLY) }
    var infoMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        SubscriptionManager.shared.loadProducts()
    }

    val selectedOffer = offers.firstOrNull { it.plan == selectedPlan }
    val selectedHasTrial = selectedPlan == SubscriptionPlan.YEARLY && (selectedOffer?.hasFreeTrial == true)

    val buttonTitle = when {
        selectedHasTrial -> L10n.startTrial
        selectedPlan == SubscriptionPlan.LIFETIME -> L10n.buyLifetimeButton
        else -> L10n.subscribeButton
    }

    fun handlePurchase() {
        val activity = hostActivity ?: return
        infoMessage = null
        scope.launch {
            val outcome = SubscriptionManager.shared.purchase(activity, selectedPlan)
            when (outcome) {
                PurchaseOutcome.SUCCESS -> {
                    FeedbackManager.shared.playSuccess()
                    onDismiss()
                }
                PurchaseOutcome.PENDING -> {
                    infoMessage = L10n.purchasePending
                }
                PurchaseOutcome.CANCELLED -> Unit
            }
        }
    }

    fun handleRestore() {
        infoMessage = null
        scope.launch {
            val hasPro = SubscriptionManager.shared.restorePurchases()
            if (hasPro) {
                FeedbackManager.shared.playSuccess()
                infoMessage = L10n.restoreSuccess
            } else {
                infoMessage = L10n.restoreNothingFound
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Üst Bar: Kapat Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Theme.paper)
                        .border(width = 2.dp, color = Theme.ink, shape = CircleShape)
                        .clickable {
                            FeedbackManager.shared.playSelection()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = L10n.closeButton,
                        tint = Theme.ink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isPro) {
                // MARK: - Pro Aktif Görünümü (iOS activeContent)
                ActiveProContent(
                    entitlement = entitlement,
                    packageName = context.packageName
                )
            } else {
                // MARK: - Satın Alma Görünümü (iOS purchaseContent)
                // Başlık
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "開門",
                        style = Theme.display(48.sp),
                        color = Theme.accent
                    )
                    Text(
                        text = L10n.proName.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Theme.secondaryInk
                    )
                    Text(
                        text = L10n.paywallHeadline,
                        style = Theme.display(32.sp),
                        color = Theme.ink,
                        lineHeight = 38.sp
                    )
                }

                // Avantajlar Kutusu
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .inkBordered(lineWidth = 2.dp)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ProBenefitRow(
                        icon = Icons.Filled.AutoStories,
                        text = L10n.paywallBenefitContent
                    )
                    ProBenefitRow(
                        icon = Icons.Filled.ChatBubble,
                        text = L10n.paywallBenefitAI
                    )
                    ProBenefitRow(
                        icon = Icons.Filled.Block,
                        text = L10n.paywallBenefitNoAds
                    )
                    ProBenefitRow(
                        icon = Icons.Filled.School,
                        text = L10n.paywallBenefitExams
                    )
                }

                // Plan Kartları
                if (offers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingProducts) {
                            CircularProgressIndicator(color = Theme.accent)
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = L10n.productsUnavailable,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Theme.secondaryInk
                                )
                                PrimaryButton(
                                    text = L10n.retry,
                                    onClick = {
                                        scope.launch { SubscriptionManager.shared.loadProducts() }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SubscriptionPlan.sortedForPaywall.forEach { plan ->
                            val offer = offers.firstOrNull { it.plan == plan }
                            val price = offer?.displayPrice ?: "—"
                            val subtitle = when (plan) {
                                SubscriptionPlan.YEARLY -> {
                                    if (offer != null) {
                                        val monthly = SubscriptionManager.monthlyEquivalentPrice(
                                            offer.priceAmountMicros,
                                            offer.currencyCode
                                        )
                                        L10n.planPerMonthEquivalent(monthly)
                                    } else ""
                                }
                                SubscriptionPlan.MONTHLY -> ""
                                SubscriptionPlan.LIFETIME -> L10n.planOneTime
                            }
                            val suffix = when (plan) {
                                SubscriptionPlan.YEARLY -> L10n.planPerYear
                                SubscriptionPlan.MONTHLY -> L10n.planPerMonth
                                SubscriptionPlan.LIFETIME -> ""
                            }
                            val title = when (plan) {
                                SubscriptionPlan.YEARLY -> L10n.planYearly
                                SubscriptionPlan.MONTHLY -> L10n.planMonthly
                                SubscriptionPlan.LIFETIME -> L10n.planLifetime
                            }

                            ProPlanCard(
                                title = title,
                                subtitle = subtitle,
                                price = price,
                                priceSuffix = suffix,
                                isSelected = selectedPlan == plan,
                                showBestValueBadge = plan == SubscriptionPlan.YEARLY,
                                onClick = {
                                    FeedbackManager.shared.playSelection()
                                    selectedPlan = plan
                                }
                            )
                        }
                    }

                    // Satın Alma Butonu
                    PrimaryButton(
                        text = buttonTitle,
                        onClick = { handlePurchase() },
                        enabled = !isPurchasing && selectedOffer != null,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Hata veya Bilgilendirme
                val displayMessage = infoMessage ?: lastError
                displayMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Theme.accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Alt Bilgi (Footer): Geri Yükle & İnce Metin
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = L10n.restorePurchases,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.ink,
                        modifier = Modifier.clickable {
                            FeedbackManager.shared.playSelection()
                            handleRestore()
                        }
                    )

                    Text(
                        text = L10n.paywallFinePrint(trial = selectedHasTrial),
                        fontSize = 11.sp,
                        color = Theme.secondaryInk,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(
                            text = L10n.termsOfUse,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Theme.secondaryInk,
                            modifier = Modifier.clickable {
                                val url = "https://play.google.com/intl/tr_ALL/about/play-terms/"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                        Text(
                            text = L10n.privacyPolicy,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Theme.secondaryInk,
                            modifier = Modifier.clickable {
                                val url = "https://policies.google.com/privacy"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pro aktif kullanıcı için gösterilen kutlama ve abonelik yönetimi ekranı.
 */
@Composable
private fun ActiveProContent(
    entitlement: ProEntitlement,
    packageName: String
) {
    val context = LocalContext.current
    val plan = (entitlement as? ProEntitlement.Pro)?.plan ?: SubscriptionPlan.YEARLY

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "開門",
            style = Theme.display(64.sp),
            color = Theme.accent
        )

        Text(
            text = L10n.proActiveTitle,
            style = Theme.display(30.sp),
            color = Theme.ink
        )

        Text(
            text = L10n.proActiveSubtitle,
            fontSize = 15.sp,
            color = Theme.secondaryInk,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered(lineWidth = 2.dp)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val title = when (plan) {
                SubscriptionPlan.YEARLY -> L10n.planYearly
                SubscriptionPlan.MONTHLY -> L10n.planMonthly
                SubscriptionPlan.LIFETIME -> L10n.planLifetime
            }
            Text(
                text = title,
                style = Theme.heading(18.sp),
                color = Theme.ink
            )
            if (plan == SubscriptionPlan.LIFETIME) {
                Text(
                    text = L10n.lifetimeActive,
                    fontSize = 13.sp,
                    color = Theme.secondaryInk
                )
            }
        }

        if (plan.isAutoRenewing) {
            PrimaryButton(
                text = L10n.manageSubscription,
                onClick = {
                    val url = SubscriptionManager.manageSubscriptionUrl(plan, packageName)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            )
        }
    }
}
