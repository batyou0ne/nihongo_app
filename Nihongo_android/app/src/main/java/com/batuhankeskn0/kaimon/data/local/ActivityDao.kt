package com.batuhankeskn0.kaimon.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * `DailyActivityEntity` tablosu için reaktif (`Flow`) ve `suspend` erişim sağlayan DAO.
 */
@Dao
interface ActivityDao {

    @Query("SELECT * FROM daily_activity ORDER BY dateString DESC")
    fun observeAllActivities(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity ORDER BY dateString DESC")
    suspend fun getAllActivities(): List<DailyActivityEntity>

    @Query("SELECT * FROM daily_activity WHERE dateString = :dateString LIMIT 1")
    fun observeActivityByDate(dateString: String): Flow<DailyActivityEntity?>

    @Query("SELECT * FROM daily_activity WHERE dateString = :dateString LIMIT 1")
    suspend fun getActivityByDate(dateString: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity ORDER BY dateString DESC LIMIT :limit")
    fun observeRecentActivities(limit: Int = 7): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity ORDER BY dateString DESC LIMIT :limit")
    suspend fun getRecentActivities(limit: Int = 7): List<DailyActivityEntity>

    @Upsert
    suspend fun upsertActivity(activity: DailyActivityEntity)

    @Upsert
    suspend fun upsertAllActivities(activities: List<DailyActivityEntity>)

    @Query("DELETE FROM daily_activity")
    suspend fun clearAllActivities()

    suspend fun getOrCreateActivity(
        dateString: String = DailyActivityEntity.formatDateString()
    ): DailyActivityEntity {
        val existing = getActivityByDate(dateString)
        if (existing != null) return existing
        val created = DailyActivityEntity(dateString = dateString)
        upsertActivity(created)
        return created
    }
}
