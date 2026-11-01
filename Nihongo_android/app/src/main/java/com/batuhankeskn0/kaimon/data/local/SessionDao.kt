package com.batuhankeskn0.kaimon.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * `LearningSessionStateEntity` tablosu için reaktif (`Flow`) ve `suspend` erişim sağlayan DAO.
 */
@Dao
interface SessionDao {

    @Query("SELECT * FROM learning_session_state ORDER BY lastOpenedAt DESC")
    fun observeAllSessions(): Flow<List<LearningSessionStateEntity>>

    @Query("SELECT * FROM learning_session_state ORDER BY lastOpenedAt DESC")
    suspend fun getAllSessions(): List<LearningSessionStateEntity>

    @Query("SELECT * FROM learning_session_state WHERE moduleType = :moduleType LIMIT 1")
    fun observeSession(moduleType: String): Flow<LearningSessionStateEntity?>

    @Query("SELECT * FROM learning_session_state WHERE moduleType = :moduleType LIMIT 1")
    suspend fun getSession(moduleType: String): LearningSessionStateEntity?

    @Upsert
    suspend fun upsertSession(session: LearningSessionStateEntity)

    @Upsert
    suspend fun upsertAllSessions(sessions: List<LearningSessionStateEntity>)

    @Query("DELETE FROM learning_session_state WHERE moduleType = :moduleType")
    suspend fun deleteSession(moduleType: String)

    @Query("DELETE FROM learning_session_state")
    suspend fun clearAllSessions()
}
