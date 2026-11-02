package com.batuhankeskn0.kaimon.ui.screens.auth

import android.app.TimePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhankeskn0.kaimon.BuildConfig
import com.batuhankeskn0.kaimon.data.service.AdManager
import com.batuhankeskn0.kaimon.data.service.AuthService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.data.service.LanguageManager
import com.batuhankeskn0.kaimon.data.service.NotificationManager
import com.batuhankeskn0.kaimon.data.service.SubscriptionManager
import com.batuhankeskn0.kaimon.data.service.UserProfileService
import com.batuhankeskn0.kaimon.ui.components.findActivity
import com.batuhankeskn0.kaimon.ui.theme.PrimaryButton
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Hesap ve Ayarlar Ekranı.
 * iOS `Views/Auth/AccountView.swift` bileşeninin birebir Jetpack Compose karşılığıdır.
 *
 * - Kullanıcının profil bilgilerini (ad, soyad, yaş, e-posta) gösterir.
 * - Dil seçimi (TR / EN), günlük çalışma hatırlatıcısı saati.
 * - Kaimon Pro abonelik durumu, satın alma/detay butonu ve debug anahtarı.
 * - Oturumu kapatma ve hesabı kalıcı silme akışlarını yürütür.
 */
@Composable
fun AccountScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSignIn: () -> Unit = {},
    onOpenPaywall: () -> Unit = {}
) {
    val context = LocalContext.current
    val hostActivity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    val currentUser by AuthService.shared.currentUser.collectAsStateWithLifecycle()
    val profile by UserProfileService.shared.profile.collectAsStateWithLifecycle()
    val isPro by SubscriptionManager.shared.isProFlow.collectAsStateWithLifecycle()
    val debugForcePro by SubscriptionManager.shared.debugForcePro.collectAsStateWithLifecycle()
    val isPrivacyOptionsRequired by AdManager.shared.isPrivacyOptionsRequired.collectAsStateWithLifecycle()

    val hasAccount = currentUser?.hasAccount == true
    val displayName = profile?.fullName?.takeIf { it.isNotBlank() }
        ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.takeIf { it.isNotBlank() }
        ?: L10n.guestAccountTitle

    // Bildirim Durumu
    var isDailyReminderEnabled by rememberSaveable {
        mutableStateOf(NotificationManager.shared.isDailyReminderEnabled)
    }
    var reminderHour by rememberSaveable {
        mutableIntStateOf(NotificationManager.shared.savedHour)
    }
    var reminderMinute by rememberSaveable {
        mutableIntStateOf(NotificationManager.shared.savedMinute)
    }

    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var isWorking by rememberSaveable { mutableStateOf(false) }
    var actionErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        UserProfileService.shared.loadIfNeeded()
    }

    fun openTimePicker() {
        val picker = TimePickerDialog(
            context,
            { _, hour, minute ->
                reminderHour = hour
                reminderMinute = minute
                NotificationManager.shared.scheduleDailyReminder(hour, minute)
                FeedbackManager.shared.playSelection()
            },
            reminderHour,
            reminderMinute,
            true
        )
        picker.show()
    }

    fun submitSignOut() {
        isWorking = true
        scope.launch {
            AuthService.shared.signOut()
            isWorking = false
            FeedbackManager.shared.playSelection()
            onDismiss()
        }
    }

    fun submitDeleteAccount() {
        showDeleteConfirmDialog = false
        isWorking = true
        actionErrorMessage = null
        scope.launch {
            try {
                AuthService.shared.deleteAccount()
                isWorking = false
                FeedbackManager.shared.playSuccess()
                onDismiss()
            } catch (e: Throwable) {
                isWorking = false
                actionErrorMessage = AuthService.friendlyMessage(e)
                FeedbackManager.shared.playError()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 40.dp)
    ) {
        // Kapat Butonu
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
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
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Profil İkonu & Başlık
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (hasAccount) Icons.Filled.VerifiedUser else Icons.Filled.Person,
                contentDescription = null,
                tint = Theme.accent,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = L10n.accountTitle,
                style = Theme.display(32.sp),
                color = Theme.ink
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Kullanıcı Bilgileri Kartı
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered(lineWidth = 2.dp)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = displayName,
                style = Theme.heading(20.sp),
                color = Theme.ink
            )

            currentUser?.email?.takeIf { it.isNotBlank() }?.let { email ->
                Text(
                    text = email,
                    fontSize = 14.sp,
                    color = Theme.secondaryInk
                )
            }

            profile?.age?.let { age ->
                Text(
                    text = L10n.ageLabel(age),
                    fontSize = 14.sp,
                    color = Theme.secondaryInk
                )
            }

            val providerNames = currentUser?.providerNames.orEmpty()
            if (providerNames.isNotEmpty()) {
                Text(
                    text = "${L10n.signInMethodsLabel}: ${providerNames.joinToString(", ")}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Theme.secondaryInk
                )
            }

            if (!hasAccount) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = L10n.guestAccountInfo,
                    fontSize = 13.sp,
                    color = Theme.secondaryInk,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(
                    text = L10n.createAccountButton,
                    onClick = {
                        FeedbackManager.shared.playSelection()
                        onOpenSignIn()
                    }
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = L10n.accountSyncInfo,
                    fontSize = 12.sp,
                    color = Theme.secondaryInk
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Abonelik Bölümü (Kaimon Pro)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered(lineWidth = 2.dp)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = Theme.accent,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = L10n.proStatusSectionTitle,
                    style = Theme.heading(18.sp),
                    color = Theme.ink
                )
            }

            Text(
                text = if (isPro) L10n.proStatusActive else L10n.proStatusFree,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPro) Theme.accent else Theme.secondaryInk
            )

            PrimaryButton(
                text = if (isPro) L10n.viewProButton else L10n.upgradeToProButton,
                onClick = {
                    FeedbackManager.shared.playSelection()
                    onOpenPaywall()
                }
            )

            // DEBUG Modu: Force Pro Toggle
            if (BuildConfig.DEBUG) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.debugForceProLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.ink
                    )
                    Switch(
                        checked = debugForcePro,
                        onCheckedChange = { checked ->
                            SubscriptionManager.shared.setDebugForcePro(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Theme.paper,
                            checkedTrackColor = Theme.accent,
                            uncheckedThumbColor = Theme.paper,
                            uncheckedTrackColor = Theme.ink.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Uygulama Dili Ayarı (Segmented)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = L10n.languageSettingsTitle,
                style = Theme.heading(18.sp),
                color = Theme.ink
            )
            Text(
                text = L10n.languageSettingsSubtitle,
                fontSize = 13.sp,
                color = Theme.secondaryInk
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LanguageManager.AppLanguage.entries.forEach { lang ->
                    val isSelected = LanguageManager.shared.current == lang
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Theme.ink else Theme.paper)
                            .border(
                                width = 2.dp,
                                color = Theme.ink,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                FeedbackManager.shared.playSelection()
                                LanguageManager.shared.setLanguage(lang)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${lang.flag} ${lang.displayName}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Theme.paper else Theme.ink
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Günlük Hatırlatıcı Bildirimleri
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = Theme.ink,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = L10n.notificationsTitle,
                    style = Theme.heading(18.sp),
                    color = Theme.ink
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L10n.dailyReminderToggle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Theme.ink
                )
                Switch(
                    checked = isDailyReminderEnabled,
                    onCheckedChange = { checked ->
                        isDailyReminderEnabled = checked
                        NotificationManager.shared.isDailyReminderEnabled = checked
                        if (checked) {
                            NotificationManager.shared.scheduleDailyReminder(reminderHour, reminderMinute)
                        } else {
                            NotificationManager.shared.cancelDailyReminder()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Theme.paper,
                        checkedTrackColor = Theme.accent,
                        uncheckedThumbColor = Theme.paper,
                        uncheckedTrackColor = Theme.ink.copy(alpha = 0.3f)
                    )
                )
            }

            if (isDailyReminderEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Theme.paper)
                        .border(
                            width = 1.5.dp,
                            color = Theme.ink.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { openTimePicker() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.reminderTimeLabel,
                        fontSize = 14.sp,
                        color = Theme.ink
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.accent
                    )
                }
            }
        }

        // Reklam Gizlilik Seçenekleri (UMP)
        if (isPrivacyOptionsRequired && hostActivity != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Theme.paper)
                    .border(
                        width = 1.5.dp,
                        color = Theme.ink.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        FeedbackManager.shared.playSelection()
                        AdManager.shared.showPrivacyOptionsForm(hostActivity)
                    }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = L10n.adPrivacyOptions,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.ink
                )
            }
        }

        actionErrorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Theme.accent
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Çıkış Yap Butonu
        PrimaryButton(
            text = L10n.signOutButton,
            onClick = { submitSignOut() },
            enabled = !isWorking
        )

        // Hesap Sil Butonu (yalnızca hesaba bağlıysa)
        if (hasAccount) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isWorking) {
                        FeedbackManager.shared.playSelection()
                        showDeleteConfirmDialog = true
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = L10n.deleteAccountButton,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.accent
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = L10n.deleteAccountConfirmTitle,
                    style = Theme.heading(18.sp),
                    color = Theme.ink
                )
            },
            text = {
                Text(
                    text = L10n.deleteAccountConfirmMessage,
                    fontSize = 14.sp,
                    color = Theme.secondaryInk
                )
            },
            confirmButton = {
                TextButton(onClick = { submitDeleteAccount() }) {
                    Text(
                        text = L10n.deleteAccountButton,
                        fontWeight = FontWeight.Bold,
                        color = Theme.accent
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(
                        text = L10n.cancelButton,
                        fontWeight = FontWeight.Bold,
                        color = Theme.ink
                    )
                }
            },
            containerColor = Theme.paper,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
