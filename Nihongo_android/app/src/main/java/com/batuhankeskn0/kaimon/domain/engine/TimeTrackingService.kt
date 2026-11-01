package com.batuhankeskn0.kaimon.domain.engine

import com.batuhankeskn0.kaimon.data.local.ActivityDao
import com.batuhankeskn0.kaimon.data.local.DailyActivityEntity
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uygulamanın aktif kullanıldığı süreyi saniye bazında hesaplayıp `DailyActivityEntity`
 * üzerinde güncelleyen servis.
 */
@Singleton
class TimeTrackingService @Inject constructor() {

    @Volatile
    private var sessionStartTimeMillis: Long? = null

    fun appDidBecomeActive(nowMillis: Long = System.currentTimeMillis()) {
        sessionStartTimeMillis = nowMillis
    }

    suspend fun appWillResignActive(
        activityDao: ActivityDao = UserDatabaseManager.shared.activityDao,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val start = sessionStartTimeMillis ?: return
        val duration = ((nowMillis - start) / 1000L).toInt()

        if (duration > 0) {
            addTime(seconds = duration, activityDao = activityDao, nowMillis = nowMillis)
        }

        // Yeniden active olana kadar süreyi sıfırla
        sessionStartTimeMillis = null
    }

    /**
     * Uygulama hala açıkken süreyi veritabanına yansıtmak için kullanılır (ör. grafik açıldığında).
     */
    suspend fun commitCurrentSession(
        activityDao: ActivityDao = UserDatabaseManager.shared.activityDao,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val start = sessionStartTimeMillis ?: return
        val duration = ((nowMillis - start) / 1000L).toInt()

        if (duration > 0) {
            addTime(seconds = duration, activityDao = activityDao, nowMillis = nowMillis)
            // Çift saymayı önlemek için başlangıç zamanını şu an olarak güncelle
            sessionStartTimeMillis = nowMillis
        }
    }

    private suspend fun addTime(
        seconds: Int,
        activityDao: ActivityDao,
        nowMillis: Long
    ) {
        val todayString = DailyActivityEntity.formatDateString(nowMillis)
        val dailyActivity = activityDao.getOrCreateActivity(todayString)
        dailyActivity.timeSpentSeconds += seconds
        activityDao.upsertActivity(dailyActivity)
    }

    companion object {
        val shared: TimeTrackingService = TimeTrackingService()
    }
}
