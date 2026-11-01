package com.batuhankeskn0.kaimon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Bir kullanıcının JLPT seviyesi bazındaki genel ilerleme, kilit ve sınav durumu.
 * N5 varsayılan olarak açıktır; N4, N3, N2 ve N1 ise önceki seviye tamamlanana
 * veya Test-Out sınavı geçilene kadar kilitli kalır.
 */
@Entity(tableName = "user_level_progress")
data class UserLevelProgressEntity(
    /** `"N5"`, `"N4"`, `"N3"`, `"N2"`, `"N1"` (`JLPTLevel.rawValue`) */
    @PrimaryKey
    val levelRaw: String,

    /** Bu seviyenin kilidi açık mı? (N5 için true, diğerleri false başlar) */
    var isUnlocked: Boolean = false,

    /** Seviyenin tüm zorunlu bölümleri ve kapı sınavı tamamlandı mı? */
    var isCompleted: Boolean = false,

    /** Seviye sonu kapı sınavı (Gateway Exam) başarıyla geçildi mi? */
    var hasPassedGatewayExam: Boolean = false,

    /** Kapı sınavı skoru (`0.0 - 1.0` arası) */
    var gatewayExamScore: Double? = null,

    /** Kapı sınavının geçildiği tarih (epoch ms) */
    var gatewayExamDate: Long? = null,

    /** Seviye dersleri tek tek yapılmadan doğrudan "Test-Out" sınavı ile mi atlandı? */
    var isSkippedViaPlacement: Boolean = false,

    /**
     * Modül bazında tamamlanan part/ünite indeksleri (JSON formatında saklanır).
     * Örn: `{"kanji":[0,1,2],"vocab":[0,1,2,3],"grammar":[1,2]}`
     */
    var completedPartsJSON: String = "{}",

    /** Son güncelleme tarihi (epoch ms) */
    var lastUpdated: Long = System.currentTimeMillis()
) {
    val level: JLPTLevel?
        get() = JLPTLevel.fromRawValue(levelRaw)

    // MARK: - Parça (Part) Tamamlama Yardımcıları

    /**
     * Belirtilen modülde (örn. `"kanji"`, `"vocab"`, `"grammar"`) tamamlanmış parça indeksleri kümesi.
     */
    fun completedPartIndices(module: String): Set<Int> {
        val dict = parseCompletedPartsMap()
        return dict[module]?.toSet() ?: emptySet()
    }

    /**
     * Belirtilen parçanın (`0-indexed`) tamamlanıp tamamlanmadığını döner.
     */
    fun isPartCompleted(module: String, index: Int): Boolean =
        completedPartIndices(module).contains(index)

    /**
     * Belirtilen parçayı tamamlandı olarak işaretler ve JSON'ı günceller.
     */
    fun markPartCompleted(module: String, index: Int): UserLevelProgressEntity {
        val dict = parseCompletedPartsMap().toMutableMap()
        val updatedSet = (dict[module]?.toMutableSet() ?: mutableSetOf()).apply {
            add(index)
        }
        dict[module] = updatedSet.sorted()
        completedPartsJSON = runCatching { json.encodeToString(dict) }.getOrDefault(completedPartsJSON)
        lastUpdated = System.currentTimeMillis()
        return this
    }

    private fun parseCompletedPartsMap(): Map<String, List<Int>> {
        if (completedPartsJSON.isBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString<Map<String, List<Int>>>(completedPartsJSON)
        }.getOrDefault(emptyMap())
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}
