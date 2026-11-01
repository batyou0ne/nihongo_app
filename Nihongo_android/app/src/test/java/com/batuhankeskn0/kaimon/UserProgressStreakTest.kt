package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.data.local.ActivityDao
import com.batuhankeskn0.kaimon.data.local.DailyActivityEntity
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.ProgressDao
import com.batuhankeskn0.kaimon.data.local.RoomConverters
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.local.UserProgressEntity
import com.batuhankeskn0.kaimon.domain.engine.TimeTrackingService
import com.batuhankeskn0.kaimon.domain.engine.XPManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId

/**
 * Faz 3 — `UserProgressEntity` seri (streak) hesaplamaları, `XPManager`,
 * `TimeTrackingService`, `RoomConverters` ve `UserDatabaseManager` dosya taşıma (migration) testleri.
 */
class UserProgressStreakTest {

    private val testZone: ZoneId = ZoneId.of("UTC")

    private fun epochForDayOffset(daysOffset: Long, hour: Int = 12): Long =
        LocalDate.of(2026, 5, 10)
            .plusDays(daysOffset)
            .atTime(hour, 0)
            .atZone(testZone)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `initial UserProgress has zero streak and didStudy returns false`() {
        val progress = UserProgressEntity()
        val now = epochForDayOffset(0)

        assertEquals(0, progress.currentStreak)
        assertEquals(0, progress.longestStreak)
        assertEquals(0, progress.activeStreak(now, testZone))
        assertFalse(progress.didStudy(daysAgo = 0, now = now, zoneId = testZone))
        assertFalse(progress.didStudy(daysAgo = 1, now = now, zoneId = testZone))
    }

    @Test
    fun `recordStudySession increments consecutive days and ignores duplicate same-day sessions`() {
        val progress = UserProgressEntity()

        // Gün 0: İlk çalışma
        val day0Morning = epochForDayOffset(0, hour = 9)
        progress.recordStudySession(on = day0Morning, zoneId = testZone)
        assertEquals(1, progress.currentStreak)
        assertEquals(1, progress.longestStreak)
        assertEquals(1, progress.activeStreak(day0Morning, testZone))
        assertTrue(progress.didStudy(daysAgo = 0, now = day0Morning, zoneId = testZone))
        assertFalse(progress.didStudy(daysAgo = 1, now = day0Morning, zoneId = testZone))

        // Gün 0 akşam: Aynı gün tekrar çalışıldığında streak artmamalı
        val day0Evening = epochForDayOffset(0, hour = 21)
        progress.recordStudySession(on = day0Evening, zoneId = testZone)
        assertEquals(1, progress.currentStreak)
        assertEquals(1, progress.longestStreak)

        // Gün 1: Ardışık 2. gün
        val day1 = epochForDayOffset(1)
        progress.recordStudySession(on = day1, zoneId = testZone)
        assertEquals(2, progress.currentStreak)
        assertEquals(2, progress.longestStreak)

        // Gün 2: Ardışık 3. gün
        val day2 = epochForDayOffset(2)
        progress.recordStudySession(on = day2, zoneId = testZone)
        assertEquals(3, progress.currentStreak)
        assertEquals(3, progress.longestStreak)
        assertTrue(progress.didStudy(daysAgo = 0, now = day2, zoneId = testZone))
        assertTrue(progress.didStudy(daysAgo = 1, now = day2, zoneId = testZone))
        assertTrue(progress.didStudy(daysAgo = 2, now = day2, zoneId = testZone))
        assertFalse(progress.didStudy(daysAgo = 3, now = day2, zoneId = testZone))

        // Gün 3 sabahı (henüz çalışılmadı): dün çalışıldığı için activeStreak hâlâ 3 olmalı
        val day3BeforeStudy = epochForDayOffset(3, hour = 8)
        assertEquals(3, progress.activeStreak(day3BeforeStudy, testZone))
        assertFalse(progress.didStudy(daysAgo = 0, now = day3BeforeStudy, zoneId = testZone))
        assertTrue(progress.didStudy(daysAgo = 1, now = day3BeforeStudy, zoneId = testZone))
        assertTrue(progress.didStudy(daysAgo = 2, now = day3BeforeStudy, zoneId = testZone))
        assertTrue(progress.didStudy(daysAgo = 3, now = day3BeforeStudy, zoneId = testZone))
    }

    @Test
    fun `skipping a day resets activeStreak to 0 and next session starts at 1 while preserving longestStreak`() {
        val progress = UserProgressEntity()
        progress.recordStudySession(on = epochForDayOffset(0), zoneId = testZone)
        progress.recordStudySession(on = epochForDayOffset(1), zoneId = testZone)
        progress.recordStudySession(on = epochForDayOffset(2), zoneId = testZone)
        assertEquals(3, progress.currentStreak)
        assertEquals(3, progress.longestStreak)

        // Gün 3 atlandı -> Gün 4'te henüz çalışmadan kontrol edildiğinde activeStreak = 0
        val day4 = epochForDayOffset(4)
        assertEquals(0, progress.activeStreak(day4, testZone))

        // Gün 4'te yeni çalışma kaydedildiğinde currentStreak = 1 olur, longestStreak = 3 korunur
        progress.recordStudySession(on = day4, zoneId = testZone)
        assertEquals(1, progress.currentStreak)
        assertEquals(3, progress.longestStreak)
        assertEquals(1, progress.activeStreak(day4, testZone))
        assertTrue(progress.didStudy(daysAgo = 0, now = day4, zoneId = testZone))
        assertFalse(progress.didStudy(daysAgo = 1, now = day4, zoneId = testZone))
    }

    @Test
    fun `XPManager and TimeTrackingService update UserProgress and DailyActivity accurately`() = runTest {
        val fakeProgressDao = FakeProgressDao()
        val fakeActivityDao = FakeActivityDao()
        val now = epochForDayOffset(0)
        val todayStr = DailyActivityEntity.formatDateString(now)

        // XP ekleme testleri (5 + 2 + 20 + 20 = 47 XP, 2 öğe tekrar)
        XPManager.shared.addXP(XPManager.XPAction.newCardLearned, fakeProgressDao, fakeActivityDao, now)
        XPManager.shared.addXP(XPManager.XPAction.cardReviewed, fakeProgressDao, fakeActivityDao, now)
        XPManager.shared.addXP(XPManager.XPAction.grammarCompleted, fakeProgressDao, fakeActivityDao, now)
        XPManager.shared.addXP(XPManager.XPAction.storyCompleted, fakeProgressDao, fakeActivityDao, now)

        val userProgress = fakeProgressDao.getOrCreateUserProgress()
        val dailyActivity = fakeActivityDao.getOrCreateActivity(todayStr)

        assertEquals(47, userProgress.totalXP)
        assertEquals(47, dailyActivity.xpEarned)
        assertEquals(2, dailyActivity.itemsReviewed)

        // TimeTrackingService süre kaydı testi
        val tracker = TimeTrackingService()
        tracker.appDidBecomeActive(nowMillis = now)
        tracker.commitCurrentSession(activityDao = fakeActivityDao, nowMillis = now + 45_000L)
        assertEquals(45, fakeActivityDao.getOrCreateActivity(todayStr).timeSpentSeconds)

        tracker.appWillResignActive(activityDao = fakeActivityDao, nowMillis = now + 120_000L)
        assertEquals(120, fakeActivityDao.getOrCreateActivity(todayStr).timeSpentSeconds)
    }

    @Test
    fun `RoomConverters serialize and deserialize List and Map types accurately`() {
        val converters = RoomConverters()

        val list = listOf("あ", "い", "う")
        val encodedList = converters.fromStringList(list)
        assertEquals(list, converters.toStringList(encodedList))

        val map = mapOf("あ" to 2, "か" to 5)
        val encodedMap = converters.fromStringIntMap(map)
        assertEquals(map, converters.toStringIntMap(encodedMap))

        assertEquals(
            LearnableItemKind.VOCABULARY_WORD,
            converters.toLearnableItemKind(converters.fromLearnableItemKind(LearnableItemKind.VOCABULARY_WORD))
        )
    }

    @Test
    fun `UserDatabaseManager migrates anonymous database and wal-shm files to linked uid`() {
        val tempDir = Files.createTempDirectory("kaimon_db_migration_test").toFile()
        try {
            val anonDb = tempDir.resolve("progress_anonymous.db").apply { writeText("SQLITE_DATA") }
            val anonShm = tempDir.resolve("progress_anonymous.db-shm").apply { writeText("SHM_DATA") }
            val anonWal = tempDir.resolve("progress_anonymous.db-wal").apply { writeText("WAL_DATA") }

            val migrated = UserDatabaseManager.migrateDatabaseFilesIfNeeded(
                databaseDir = tempDir,
                targetUid = "firebase_user_123"
            )

            assertTrue("Anonim veritabanı yeni kullanıcıya taşınmalı", migrated)
            assertFalse("Eski anonim .db dosyası taşınmış olmalı", anonDb.exists())
            assertFalse("Eski anonim .db-shm dosyası taşınmış olmalı", anonShm.exists())
            assertFalse("Eski anonim .db-wal dosyası taşınmış olmalı", anonWal.exists())

            val userDb = tempDir.resolve("progress_firebase_user_123.db")
            val userShm = tempDir.resolve("progress_firebase_user_123.db-shm")
            val userWal = tempDir.resolve("progress_firebase_user_123.db-wal")

            assertTrue(userDb.exists())
            assertTrue(userShm.exists())
            assertTrue(userWal.exists())
            assertEquals("SQLITE_DATA", userDb.readText())
            assertEquals("SHM_DATA", userShm.readText())
            assertEquals("WAL_DATA", userWal.readText())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

private class FakeProgressDao : ProgressDao {
    private var userProgress: UserProgressEntity? = null
    private val items = mutableMapOf<String, LearningItemProgressEntity>()

    override fun observeAllItemProgress(): Flow<List<LearningItemProgressEntity>> =
        MutableStateFlow(items.values.toList())

    override suspend fun getAllItemProgress(): List<LearningItemProgressEntity> =
        items.values.toList()

    override fun observeItemProgress(itemID: String): Flow<LearningItemProgressEntity?> =
        MutableStateFlow(items[itemID])

    override suspend fun getItemProgress(itemID: String): LearningItemProgressEntity? =
        items[itemID]

    override fun observeProgressByKind(kind: LearnableItemKind): Flow<List<LearningItemProgressEntity>> =
        MutableStateFlow(items.values.filter { it.itemKind == kind })

    override suspend fun getProgressByKind(kind: LearnableItemKind): List<LearningItemProgressEntity> =
        items.values.filter { it.itemKind == kind }

    override fun observeReviewItems(nowMillis: Long): Flow<List<LearningItemProgressEntity>> =
        MutableStateFlow(getReviewItemsSync(nowMillis))

    override suspend fun getReviewItems(nowMillis: Long): List<LearningItemProgressEntity> =
        getReviewItemsSync(nowMillis)

    private fun getReviewItemsSync(nowMillis: Long): List<LearningItemProgressEntity> =
        items.values.filter { it.needsReview || (it.isLearned && it.dueDate <= nowMillis) }

    override fun observeLearnedCount(kind: LearnableItemKind): Flow<Int> =
        MutableStateFlow(items.values.count { it.itemKind == kind && it.repetitionCount >= 1 })

    override suspend fun getLearnedCount(kind: LearnableItemKind): Int =
        items.values.count { it.itemKind == kind && it.repetitionCount >= 1 }

    override suspend fun upsertItemProgress(progress: LearningItemProgressEntity) {
        items[progress.itemID] = progress
    }

    override suspend fun upsertAllItemProgress(items: List<LearningItemProgressEntity>) {
        items.forEach { this.items[it.itemID] = it }
    }

    override suspend fun deleteItemProgress(progress: LearningItemProgressEntity) {
        items.remove(progress.itemID)
    }

    override suspend fun clearAllItemProgress() {
        items.clear()
    }

    override fun observeUserProgress(): Flow<UserProgressEntity?> =
        MutableStateFlow(userProgress)

    override suspend fun getUserProgress(): UserProgressEntity? = userProgress

    override suspend fun upsertUserProgress(userProgress: UserProgressEntity) {
        this.userProgress = userProgress
    }

    override suspend fun clearUserProgress() {
        userProgress = null
    }
}

private class FakeActivityDao : ActivityDao {
    private val activities = mutableMapOf<String, DailyActivityEntity>()

    override fun observeAllActivities(): Flow<List<DailyActivityEntity>> =
        MutableStateFlow(activities.values.sortedByDescending { it.dateString })

    override suspend fun getAllActivities(): List<DailyActivityEntity> =
        activities.values.sortedByDescending { it.dateString }

    override fun observeActivityByDate(dateString: String): Flow<DailyActivityEntity?> =
        MutableStateFlow(activities[dateString])

    override suspend fun getActivityByDate(dateString: String): DailyActivityEntity? =
        activities[dateString]

    override fun observeRecentActivities(limit: Int): Flow<List<DailyActivityEntity>> =
        MutableStateFlow(activities.values.sortedByDescending { it.dateString }.take(limit))

    override suspend fun getRecentActivities(limit: Int): List<DailyActivityEntity> =
        activities.values.sortedByDescending { it.dateString }.take(limit)

    override suspend fun upsertActivity(activity: DailyActivityEntity) {
        activities[activity.dateString] = activity
    }

    override suspend fun upsertAllActivities(activities: List<DailyActivityEntity>) {
        activities.forEach { this.activities[it.dateString] = it }
    }

    override suspend fun clearAllActivities() {
        activities.clear()
    }
}
