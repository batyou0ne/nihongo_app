package com.batuhankeskn0.kaimon.data.local

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Room veritabanı ve DAO bağımlılıklarını Hilt bileşen ağacına sağlayan modül.
 * Aktif kullanıcı değiştiğinde (`UserDatabaseManager.switchUser`), DAO sağlayıcıları
 * her zaman aktif `progress_<uid>.db` örneğine yönlendirilir.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideKaimonDatabase(userDatabaseManager: UserDatabaseManager): KaimonDatabase =
        userDatabaseManager.getDatabase()

    @Provides
    fun provideProgressDao(userDatabaseManager: UserDatabaseManager): ProgressDao =
        userDatabaseManager.progressDao

    @Provides
    fun provideSessionDao(userDatabaseManager: UserDatabaseManager): SessionDao =
        userDatabaseManager.sessionDao

    @Provides
    fun provideActivityDao(userDatabaseManager: UserDatabaseManager): ActivityDao =
        userDatabaseManager.activityDao

    @Provides
    fun provideLevelProgressDao(userDatabaseManager: UserDatabaseManager): LevelProgressDao =
        userDatabaseManager.levelProgressDao
}
