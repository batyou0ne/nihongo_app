package com.batuhankeskn0.kaimon.data.service

import com.batuhankeskn0.kaimon.BuildConfig

/**
 * Uygulama ortam değişkenleri ve API anahtarları (`local.properties` -> `BuildConfig`).
 *
 * iOS `Config/Secrets.swift` yapısıyla birebir uyumludur.
 * Güvenlik kuralı gereği API anahtarları hiçbir koşulda loglara yazılmaz ve `toString()` çıktısında maskelenir.
 */
object Secrets {
    const val PLACEHOLDER_API_KEY = "API_KEY_BURAYA_GELECEK"

    @Volatile
    private var geminiKeyOverride: String? = null

    @Volatile
    private var openAiKeyOverride: String? = null

    @Volatile
    private var providerOverride: String? = null

    val geminiApiKey: String
        get() = (geminiKeyOverride ?: BuildConfig.GEMINI_API_KEY).trim()

    val openAiApiKey: String
        get() = (openAiKeyOverride ?: BuildConfig.OPENAI_API_KEY).trim()

    val aiProvider: String
        get() = (providerOverride ?: BuildConfig.AI_PROVIDER).trim().ifBlank { "gemini" }

    /**
     * iOS `Secrets.aiAPIKey` karşılığı. Seçili sağlayıcıya (`gemini` veya `openai`) ait aktif anahtarı döner.
     */
    val aiAPIKey: String
        get() = when (aiProvider.lowercase()) {
            "openai" -> openAiApiKey.ifBlank { geminiApiKey }
            else -> geminiApiKey.ifBlank { openAiApiKey }
        }

    fun isKeyConfigured(key: String = aiAPIKey): Boolean {
        val trimmed = key.trim()
        return trimmed.isNotEmpty() && trimmed != PLACEHOLDER_API_KEY
    }

    internal fun overrideForTesting(
        geminiKey: String? = null,
        openAiKey: String? = null,
        provider: String? = null
    ) {
        geminiKeyOverride = geminiKey
        openAiKeyOverride = openAiKey
        providerOverride = provider
    }

    internal fun resetOverrides() {
        geminiKeyOverride = null
        openAiKeyOverride = null
        providerOverride = null
    }

    override fun toString(): String =
        "Secrets(aiProvider=$aiProvider, isConfigured=${isKeyConfigured()})"
}
