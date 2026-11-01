package com.batuhankeskn0.kaimon

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

// Kaimon Brutalist Renk Paleti (Faz 4'te ui/theme/Theme.kt içine taşınacaktır)
private val KaimonPaper = Color(0xFFF5F0E8)
private val KaimonCard = Color(0xFFEBE4D8)
private val KaimonInk = Color(0xFF1A1A1A)
private val KaimonSecondaryInk = Color(0xFF5C5852)
private val KaimonAccent = Color(0xFFD9381E)

/**
 * Kaimon Ana Aktivite (Faz 1 İskeleti).
 * Edge-to-Edge ekran kurulumunu yapar ve ortak iOS/Android asset'lerinin
 * (`HiraganaData.json` ve `Audio/audio_manifest.json`) okunabilirliğini doğrular.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                KaimonBootstrapScreen()
            }
        }
    }
}

private data class AssetVerificationStatus(
    val isLoaded: Boolean = false,
    val hiraganaCount: Int = 0,
    val audioClipCount: Int = 0,
    val errorMessage: String? = null
)

@Composable
private fun KaimonBootstrapScreen() {
    val context = LocalContext.current
    var status by remember { mutableStateOf(AssetVerificationStatus()) }

    LaunchedEffect(Unit) {
        status = verifySharedAssets(context)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = KaimonPaper
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KaimonPaper)
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KaimonCard)
                    .border(width = 2.dp, color = KaimonInk)
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "開門 KAIMON",
                    color = KaimonAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Kaimon 開門 — Android Hazır",
                    color = KaimonInk,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )

                Spacer(modifier = Modifier.height(4.dp))

                val statusText = when {
                    status.errorMessage != null ->
                        "Asset Hatası: ${status.errorMessage}"
                    !status.isLoaded ->
                        "Ortak Asset'ler doğrulanıyor..."
                    else ->
                        "✓ HiraganaData.json okundu (${status.hiraganaCount} karakter)\n" +
                            "✓ Audio/audio_manifest.json okundu (${status.audioClipCount} ses kaydı)"
                }

                Text(
                    text = statusText,
                    color = if (status.errorMessage != null) KaimonAccent else KaimonSecondaryInk,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private suspend fun verifySharedAssets(context: Context): AssetVerificationStatus =
    withContext(Dispatchers.IO) {
        runCatching {
            val hiraganaRaw = context.assets.open("HiraganaData.json")
                .bufferedReader()
                .use { it.readText() }
            val hiraganaCount = Json.parseToJsonElement(hiraganaRaw).jsonArray.size

            val manifestRaw = context.assets.open("Audio/audio_manifest.json")
                .bufferedReader()
                .use { it.readText() }
            val audioClipCount = Json.parseToJsonElement(manifestRaw).jsonObject.size

            AssetVerificationStatus(
                isLoaded = true,
                hiraganaCount = hiraganaCount,
                audioClipCount = audioClipCount
            )
        }.getOrElse { error ->
            AssetVerificationStatus(
                isLoaded = true,
                errorMessage = error.localizedMessage ?: "Bilinmeyen okuma hatası"
            )
        }
    }
