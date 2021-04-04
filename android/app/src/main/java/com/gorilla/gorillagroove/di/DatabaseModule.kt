package com.gorilla.gorillagroove.di

import android.content.Context
import androidx.room.Room
import com.gorilla.gorillagroove.database.DatabaseDao
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.migrations.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object DatabaseModule {

    @Singleton
    @Provides
    fun provideTrackDb(@ApplicationContext context: Context): GorillaDatabase {
        return Room.databaseBuilder(
            context,
            GorillaDatabase::class.java,
            GorillaDatabase.DATABASE_NAME
        ).addMigrations(
            MIGRATION_1_2
        ).build()
    }

    @Singleton
    @Provides
    fun provideDatabaseDAO(gorillaDatabase: GorillaDatabase): DatabaseDao {
        return gorillaDatabase.databaseDao()
    }

    @Singleton
    @Provides
    fun provideSyncStatusDAO(gorillaDatabase: GorillaDatabase) = gorillaDatabase.syncStatusDao()

    @Singleton
    @Provides
    fun provideTrackDAO(gorillaDatabase: GorillaDatabase) = gorillaDatabase.trackDao()

    @Singleton
    @Provides
    fun providePlaylistDAO(gorillaDatabase: GorillaDatabase) = gorillaDatabase.playlistDao()

    @Singleton
    @Provides
    fun providePlaylistTrackDAO(gorillaDatabase: GorillaDatabase) = gorillaDatabase.playlistTrackDao()

    @Singleton
    @Provides
    fun provideUserDAO(gorillaDatabase: GorillaDatabase) = gorillaDatabase.userDao()

    @Singleton
    @Provides
    fun provideReviewSourceDAO(gorillaDatabase: GorillaDatabase) = gorillaDatabase.reviewSourceDao()
}
