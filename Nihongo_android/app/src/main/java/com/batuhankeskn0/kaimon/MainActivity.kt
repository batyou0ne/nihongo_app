package com.batuhankeskn0.kaimon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KaimonTheme {
                MainTabScreen()
            }
        }
    }
}
