package com.batuhankeskn0.kaimon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bir modülün (Hiragana/Katakana/Kanji/Kelime) o anki çalışma turunun durumu. Kullanıcı ortadan
 * çıkıp geri döndüğünde kaldığı yerden devam edebilmesi için her cevaptan sonra kaydedilir.
 * `remainingItemIDs` bu turda henüz cevaplanmamış karakterleri, `wrongItemIDs` bu turda
 * yanlış yapılıp bir sonraki tekrar turunu bekleyen karakterleri tutar. `remainingItemIDs`
 * boşalınca `wrongItemIDs` doluysa yeni bir tur bu listeyle başlar — tüm kartlar art arda
 * bir turda doğru yapılana kadar bu döngü sürer.
 */
@Entity(tableName = "learning_session_state")
data class LearningSessionStateEntity(
    @PrimaryKey
    val moduleType: String,
    var remainingItemIDs: List<String>,
    var wrongItemIDs: List<String> = emptyList(),
    var isCompleted: Boolean = false,

    /**
     * O an ekranda gösterilmekte olan kartın id'si. Kullanıcı uygulamadan çıkıp
     * geri döndüğünde turun ortasından rastgele bir kartla değil, tam olarak
     * bıraktığı kartla karşılaşsın diye ayrıca saklanır.
     */
    var currentCardID: String? = null,

    /**
     * Oturum boyunca (tekrar turları dahil) verilen toplam cevap sayısı.
     * Oturum sonundaki özet ekranında gösterilir; yeni oturumda sıfırlanır.
     */
    var totalAnswerCount: Int = 0,

    /**
     * Oturum boyunca yanlış yapılan öğeler ve kaçar kez yanlış yapıldıkları
     * (`itemID -> yanlış sayısı`). Özet ekranındaki liste bundan üretilir.
     */
    var wrongAnswerCounts: Map<String, Int> = emptyMap(),

    /**
     * Bu oturumun en son ne zaman açıldığı (epoch ms). Ana ekrandaki "Kaldığın yer" kartı,
     * en son açılan oturumu bulmak için bu alana göre sıralar.
     * Varsayılanı `0L` (SwiftData `Date.distantPast` karşılığı).
     */
    var lastOpenedAt: Long = 0L
)
