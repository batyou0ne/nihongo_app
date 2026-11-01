package com.batuhankeskn0.kaimon.data.model

import kotlinx.serialization.Serializable

/**
 * ScenariosData.json içindeki interaktif konuşma senaryosu.
 */
@Serializable
data class ConversationScenario(
    val id: String,
    val title: String,
    val description: String,
    val level: String,
    val steps: List<ScenarioStep> = emptyList()
)

/**
 * Senaryodaki tek bir diyalog adımı: bot mesajı ve kullanıcının seçebileceği yanıt şıkları.
 */
@Serializable
data class ScenarioStep(
    val id: String,
    val botMessage: ChatMessageData,
    val options: List<ScenarioOption> = emptyList()
)

/**
 * Bot tarafından gönderilen çok dilli mesaj verisi.
 */
@Serializable
data class ChatMessageData(
    val japanese: String,
    val romaji: String,
    val turkish: String
)

/**
 * Kullanıcının senaryo adımında seçebileceği yanıt seçeneği.
 */
@Serializable
data class ScenarioOption(
    val id: String,
    val japanese: String,
    val romaji: String,
    val turkish: String,
    val isCorrect: Boolean
)
