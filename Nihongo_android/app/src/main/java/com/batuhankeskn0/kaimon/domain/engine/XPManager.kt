package com.batuhankeskn0.kaimon.domain.engine

import com.batuhankeskn0.kaimon.data.local.ActivityDao
import com.batuhankeskn0.kaimon.data.local.DailyActivityEntity
import com.batuhankeskn0.kaimon.data.local.ProgressDao
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kullanıcının kazandığı XP'leri ve günlük çalışma istatistiklerini (`DailyActivityEntity`) yönetir.
 */
@Singleton
class XPManager @Inject constructor() {

    enum class XPAction(val points: Int) {
        NEW_CARD_LEARNED(5),
        CARD_REVIEWED(2),
        GRAMMAR_COMPLETED(20),
        STORY_COMPLETED(20);

        companion object {
            val newCardLearned: XPAction = NEW_CARD_LEARNED
            val cardReviewed: XPAction = CARD_REVIEWED
            val grammarCompleted: XPAction = GRAMMAR_COMPLETED
            val storyCompleted: XPAction = STORY_COMPLETED
        }
    }

    /**
     * Belirtilen eylem için XP ekler ve günlük aktiviteyi günceller.
     */
    suspend fun addXP(
        action: XPAction,
        progressDao: ProgressDao = UserDatabaseManager.shared.progressDao,
        activityDao: ActivityDao = UserDatabaseManager.shared.activityDao,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val points = action.points

        // 1. Toplam XP'yi güncelle
        val userProgress = progressDao.getOrCreateUserProgress()
        userProgress.totalXP += points
        progressDao.upsertUserProgress(userProgress)

        // 2. Günlük Aktiviteyi güncelle
        val todayString = DailyActivityEntity.formatDateString(nowMillis)
        val dailyActivity = activityDao.getOrCreateActivity(todayString)
        dailyActivity.xpEarned += points

        if (action == XPAction.NEW_CARD_LEARNED || action == XPAction.CARD_REVIEWED) {
            dailyActivity.itemsReviewed += 1
        }

        activityDao.upsertActivity(dailyActivity)
    }

    companion object {
        val shared: XPManager = XPManager()
    }
}
