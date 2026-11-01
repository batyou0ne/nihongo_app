package com.batuhankeskn0.kaimon.data.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Seviyeye ait toplam yaklaşık içerik sayıları (UI göstergeleri ve hedef takibi için).
 */
@Serializable
data class JLPTLevelTargetCounts(
    val kanji: Int,
    val vocab: Int,
    val grammar: Int
)

/**
 * JLPT (Japanese-Language Proficiency Test) seviyeleri (N5 en temel, N1 en ileri).
 */
@Serializable
enum class JLPTLevel(val rawValue: String) {
    @SerialName("N5")
    N5("N5"),

    @SerialName("N4")
    N4("N4"),

    @SerialName("N3")
    N3("N3"),

    @SerialName("N2")
    N2("N2"),

    @SerialName("N1")
    N1("N1");

    val id: String
        get() = rawValue

    /** Karşılaştırma ve sıralama için sıra numarası (0: N5 -> 4: N1). */
    val order: Int
        get() = when (this) {
            N5 -> 0
            N4 -> 1
            N3 -> 2
            N2 -> 3
            N1 -> 4
        }

    /** Bir sonraki üst seviye (örn. N5 -> N4). */
    val nextLevel: JLPTLevel?
        get() = when (this) {
            N5 -> N4
            N4 -> N3
            N3 -> N2
            N2 -> N1
            N1 -> null
        }

    /** Bir önceki alt seviye (örn. N4 -> N5). */
    val previousLevel: JLPTLevel?
        get() = when (this) {
            N5 -> null
            N4 -> N5
            N3 -> N4
            N2 -> N3
            N1 -> N2
        }

    val title: String
        get() = rawValue

    val subtitle: String
        get() = when (this) {
            N5 -> "Temel Başlangıç (Beginner)"
            N4 -> "Temel İleri (Elementary)"
            N3 -> "Orta Seviye (Intermediate)"
            N2 -> "İleri Seviye (Upper Intermediate)"
            N1 -> "Ustalık (Advanced / Native)"
        }

    /** Kapı sınavına (Gateway Exam) hak kazanmak için gereken asgari modül tamamlama oranı (%85). */
    val completionThreshold: Double
        get() = 0.85

    /** Seviyeye ait toplam yaklaşık içerik sayıları (UI göstergeleri ve hedef takibi için). */
    val targetCounts: JLPTLevelTargetCounts
        get() = when (this) {
            N5 -> JLPTLevelTargetCounts(kanji = 80, vocab = 675, grammar = 85)
            N4 -> JLPTLevelTargetCounts(kanji = 180, vocab = 800, grammar = 115)
            N3 -> JLPTLevelTargetCounts(kanji = 350, vocab = 1500, grammar = 140)
            N2 -> JLPTLevelTargetCounts(kanji = 400, vocab = 2500, grammar = 150)
            N1 -> JLPTLevelTargetCounts(kanji = 1000, vocab = 3500, grammar = 120)
        }

    companion object {
        val n5: JLPTLevel = N5
        val n4: JLPTLevel = N4
        val n3: JLPTLevel = N3
        val n2: JLPTLevel = N2
        val n1: JLPTLevel = N1

        val allCases: List<JLPTLevel>
            get() = entries

        fun fromRawValue(rawValue: String): JLPTLevel? =
            entries.firstOrNull { it.rawValue.equals(rawValue, ignoreCase = true) }
    }
}
