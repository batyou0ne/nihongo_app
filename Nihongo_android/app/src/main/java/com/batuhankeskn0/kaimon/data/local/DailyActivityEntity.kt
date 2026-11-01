package com.batuhankeskn0.kaimon.data.local

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Günlük kazanılan XP, çalışılan öğe sayısı ve aktif çalışma süresini tutar.
 * `dateString` alanı `"yyyy-MM-dd"` formatında tutulur ki her gün için tek bir eşsiz kayıt oluşsun.
 */
@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey
    val dateString: String = formatDateString(System.currentTimeMillis()),
    var xpEarned: Int = 0,
    var itemsReviewed: Int = 0,
    var timeSpentSeconds: Int = 0
) {
    @Ignore
    constructor(
        dateMillis: Long,
        xpEarned: Int = 0,
        itemsReviewed: Int = 0,
        timeSpentSeconds: Int = 0
    ) : this(
        dateString = formatDateString(dateMillis),
        xpEarned = xpEarned,
        itemsReviewed = itemsReviewed,
        timeSpentSeconds = timeSpentSeconds
    )

    @Ignore
    constructor(
        date: Date,
        xpEarned: Int = 0,
        itemsReviewed: Int = 0,
        timeSpentSeconds: Int = 0
    ) : this(
        dateString = formatDateString(date.time),
        xpEarned = xpEarned,
        itemsReviewed = itemsReviewed,
        timeSpentSeconds = timeSpentSeconds
    )

    companion object {
        private val formatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

        fun formatDateString(
            epochMillis: Long = System.currentTimeMillis(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): String = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().format(formatter)
    }
}
