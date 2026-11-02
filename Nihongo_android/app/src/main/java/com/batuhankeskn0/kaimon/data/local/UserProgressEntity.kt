package com.batuhankeskn0.kaimon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.batuhankeskn0.kaimon.data.service.NotificationManager
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * Kullanıcının genel ilerlemesi: günlük streak takibi için tek bir kayıt (`id = 1`) yeterlidir.
 */
@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey
    val id: Int = 1,
    var currentStreak: Int = 0,
    var longestStreak: Int = 0,
    var lastStudyDate: Long? = null,
    var totalItemsLearned: Int = 0,
    var totalXP: Int = 0,
    var createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Gösterime uygun seri sayısı. `currentStreak` ancak bir sonraki çalışmada
     * sıfırlandığı için (bkz. `recordStudySession`), araya gün girmişse alanda hâlâ
     * eski değer durur. Seri bugün ya da dün çalışıldıysa canlıdır; daha eskiyse
     * kopmuştur ve 0 gösterilir.
     */
    val activeStreak: Int
        get() = activeStreak(System.currentTimeMillis())

    fun activeStreak(nowMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val last = lastStudyDate ?: return 0
        if (currentStreak <= 0) return 0
        val lastDay = Instant.ofEpochMilli(last).atZone(zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val days = ChronoUnit.DAYS.between(lastDay, today)
        return if (days <= 1L) currentStreak else 0
    }

    fun activeStreak(now: Date): Int = activeStreak(now.time)

    /**
     * `daysAgo` gün önce çalışılmış mı? (`0` = bugün). Ayrı bir "çalışılan günler"
     * tablosu tutmuyoruz; seri kesintisiz günlerden oluştuğu için son çalışma
     * tarihi + seri uzunluğu bu bilgiyi vermeye yeter. Ana ekrandaki 7 günlük
     * seri şeridi bunu kullanır.
     */
    fun didStudy(
        daysAgo: Int,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        val last = lastStudyDate ?: return false
        if (currentStreak <= 0) return false
        val dayStart = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate().minusDays(daysAgo.toLong())
        val lastStart = Instant.ofEpochMilli(last).atZone(zoneId).toLocalDate()
        // Serinin bittiği günden sonrası (ör. bugün henüz çalışılmadıysa bugün) dolu değildir.
        if (dayStart.isAfter(lastStart)) return false

        val distance = ChronoUnit.DAYS.between(dayStart, lastStart)
        return distance < currentStreak
    }

    fun didStudy(daysAgo: Int, now: Date): Boolean = didStudy(daysAgo = daysAgo, now = now.time)

    /**
     * Bugün çalışıldığında çağrılır. Streak'i güncel tutar; bir gün atlanırsa sıfırlar.
     * iOS `UserProgress.recordStudySession` ile birebir aynı şekilde güncellenen aktif seriyi
     * `NotificationManager.shared.updateDailyReminderStreak` üzerinden bildirim metnine senkronize eder.
     */
    fun recordStudySession(
        on: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): UserProgressEntity {
        val last = lastStudyDate
        if (last == null) {
            currentStreak = 1
            longestStreak = maxOf(longestStreak, currentStreak)
            lastStudyDate = on
            NotificationManager.shared.updateDailyReminderStreak(activeStreak(nowMillis = on, zoneId = zoneId))
            return this
        }

        val lastDay = Instant.ofEpochMilli(last).atZone(zoneId).toLocalDate()
        val sessionDay = Instant.ofEpochMilli(on).atZone(zoneId).toLocalDate()

        if (lastDay.isEqual(sessionDay)) {
            // Bugün zaten kaydedildi; son çalışma zamanını güncelle.
            lastStudyDate = on
            NotificationManager.shared.updateDailyReminderStreak(activeStreak(nowMillis = on, zoneId = zoneId))
            return this
        }

        if (lastDay.plusDays(1).isEqual(sessionDay)) {
            currentStreak += 1
        } else {
            currentStreak = 1
        }
        longestStreak = maxOf(longestStreak, currentStreak)
        lastStudyDate = on
        NotificationManager.shared.updateDailyReminderStreak(activeStreak(nowMillis = on, zoneId = zoneId))
        return this
    }

    fun recordStudySession(on: Date): UserProgressEntity = recordStudySession(on = on.time)
}
