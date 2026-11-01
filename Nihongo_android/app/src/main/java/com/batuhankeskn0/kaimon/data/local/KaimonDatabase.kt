package com.batuhankeskn0.kaimon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Kaimon kullanıcı ilerleme Room veritabanı.
 * Her kullanıcı (Firebase `uid` veya `"anonymous"`) için `UserDatabaseManager` tarafından
 * ayrı bir `progress_<uid>.db` dosyasında açılır.
 */
@Database(
    entities = [
        LearningItemProgressEntity::class,
        LearningSessionStateEntity::class,
        DailyActivityEntity::class,
        UserProgressEntity::class,
        UserLevelProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class KaimonDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun sessionDao(): SessionDao
    abstract fun activityDao(): ActivityDao
    abstract fun levelProgressDao(): LevelProgressDao
}
