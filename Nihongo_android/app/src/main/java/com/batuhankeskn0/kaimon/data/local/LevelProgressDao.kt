package com.batuhankeskn0.kaimon.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * `UserLevelProgressEntity` tablosu için reaktif (`Flow`) ve `suspend` erişim sağlayan DAO.
 */
@Dao
interface LevelProgressDao {

    @Query("SELECT * FROM user_level_progress")
    fun observeAllLevelProgress(): Flow<List<UserLevelProgressEntity>>

    @Query("SELECT * FROM user_level_progress")
    suspend fun getAllLevelProgress(): List<UserLevelProgressEntity>

    @Query("SELECT * FROM user_level_progress WHERE levelRaw = :levelRaw LIMIT 1")
    fun observeLevelProgress(levelRaw: String): Flow<UserLevelProgressEntity?>

    @Query("SELECT * FROM user_level_progress WHERE levelRaw = :levelRaw LIMIT 1")
    suspend fun getLevelProgress(levelRaw: String): UserLevelProgressEntity?

    @Upsert
    suspend fun upsertLevelProgress(progress: UserLevelProgressEntity)

    @Upsert
    suspend fun upsertAllLevelProgress(items: List<UserLevelProgressEntity>)

    @Query("DELETE FROM user_level_progress")
    suspend fun clearAllLevelProgress()
}
