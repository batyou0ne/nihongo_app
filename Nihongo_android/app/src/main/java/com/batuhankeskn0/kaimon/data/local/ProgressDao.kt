package com.batuhankeskn0.kaimon.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * `LearningItemProgressEntity` ve `UserProgressEntity` tabloları için reaktif (`Flow`)
 * ve `suspend` erişim sağlayan Room DAO arayüzü.
 */
@Dao
interface ProgressDao {

    // MARK: - LearningItemProgress

    @Query("SELECT * FROM learning_item_progress")
    fun observeAllItemProgress(): Flow<List<LearningItemProgressEntity>>

    @Query("SELECT * FROM learning_item_progress")
    suspend fun getAllItemProgress(): List<LearningItemProgressEntity>

    @Query("SELECT * FROM learning_item_progress WHERE itemID = :itemID LIMIT 1")
    fun observeItemProgress(itemID: String): Flow<LearningItemProgressEntity?>

    @Query("SELECT * FROM learning_item_progress WHERE itemID = :itemID LIMIT 1")
    suspend fun getItemProgress(itemID: String): LearningItemProgressEntity?

    @Query("SELECT * FROM learning_item_progress WHERE itemKind = :kind")
    fun observeProgressByKind(kind: LearnableItemKind): Flow<List<LearningItemProgressEntity>>

    @Query("SELECT * FROM learning_item_progress WHERE itemKind = :kind")
    suspend fun getProgressByKind(kind: LearnableItemKind): List<LearningItemProgressEntity>

    @Query(
        "SELECT * FROM learning_item_progress " +
            "WHERE needsReview = 1 OR (isLearned = 1 AND dueDate <= :nowMillis)"
    )
    fun observeReviewItems(nowMillis: Long = System.currentTimeMillis()): Flow<List<LearningItemProgressEntity>>

    @Query(
        "SELECT * FROM learning_item_progress " +
            "WHERE needsReview = 1 OR (isLearned = 1 AND dueDate <= :nowMillis)"
    )
    suspend fun getReviewItems(nowMillis: Long = System.currentTimeMillis()): List<LearningItemProgressEntity>

    @Query(
        "SELECT COUNT(*) FROM learning_item_progress " +
            "WHERE itemKind = :kind AND repetitionCount >= 1"
    )
    fun observeLearnedCount(kind: LearnableItemKind): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM learning_item_progress " +
            "WHERE itemKind = :kind AND repetitionCount >= 1"
    )
    suspend fun getLearnedCount(kind: LearnableItemKind): Int

    @Upsert
    suspend fun upsertItemProgress(progress: LearningItemProgressEntity)

    @Upsert
    suspend fun upsertAllItemProgress(items: List<LearningItemProgressEntity>)

    @Delete
    suspend fun deleteItemProgress(progress: LearningItemProgressEntity)

    @Query("DELETE FROM learning_item_progress")
    suspend fun clearAllItemProgress()

    // MARK: - UserProgress (id = 1)

    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    fun observeUserProgress(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    suspend fun getUserProgress(): UserProgressEntity?

    @Upsert
    suspend fun upsertUserProgress(userProgress: UserProgressEntity)

    @Query("DELETE FROM user_progress")
    suspend fun clearUserProgress()

    suspend fun getOrCreateUserProgress(): UserProgressEntity {
        val existing = getUserProgress()
        if (existing != null) return existing
        val created = UserProgressEntity()
        upsertUserProgress(created)
        return created
    }
}
