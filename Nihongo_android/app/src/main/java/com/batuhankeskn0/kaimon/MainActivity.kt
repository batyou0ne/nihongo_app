package com.batuhankeskn0.kaimon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.batuhankeskn0.kaimon.data.service.AdManager
import com.batuhankeskn0.kaimon.data.service.SubscriptionManager
import kotlinx.coroutines.launch
import com.batuhankeskn0.kaimon.ui.navigation.MainTabScreen
import com.batuhankeskn0.kaimon.ui.theme.KaimonTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Kaimon Ana Aktivite.
 * Edge-to-Edge ekran kurulumunu yapar ve `KaimonTheme` içinde `MainTabScreen` ana navigasyon kabuğunu bağlar.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash Screen API: `Theme.Kaimon.Starting` -> `Theme.Kaimon` geçişi super.onCreate'ten önce kurulmalıdır.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // UMP (GDPR/KVKK) onayı ve reklam SDK'sı; Pro kullanıcılarda atlanır.
        AdManager.shared.gatherConsent(this)
        setContent {
            KaimonTheme {
                MainTabScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Play'de (başka cihaz/iade/iptal) değişen abonelikleri uygulamaya dönüşte yeniden doğrula.
        lifecycleScope.launch { SubscriptionManager.shared.refresh() }
    }
}
