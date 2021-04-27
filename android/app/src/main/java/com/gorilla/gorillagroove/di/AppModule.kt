package com.gorilla.gorillagroove.di

import android.content.ComponentName
import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.service.MusicService
import com.gorilla.gorillagroove.service.MusicServiceConnection
import com.gorilla.gorillagroove.util.Constants.SHARED_PREFERENCES_NAME
import com.gorilla.gorillagroove.database.dao.*
import com.gorilla.gorillagroove.network.NetworkApi
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.service.MarkListenedService
import com.gorilla.gorillagroove.service.sync.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions(
        RequestOptions()
            .placeholder(R.drawable.ic_image)
            .error(R.drawable.ic_image)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
    )

    @Singleton
    @Provides
    fun provideMusicServiceConnection(
        @ApplicationContext context: Context
    ): MusicServiceConnection {
        return MusicServiceConnection.getInstance(
            context,
            ComponentName(context, MusicService::class.java)
        )
    }

    @Singleton
    @Provides
    fun provideMarkListenedService(
        mainRepository: MainRepository,
        musicServiceConnection: MusicServiceConnection
    ) = MarkListenedService.getInstance(
        mainRepository,
        musicServiceConnection
    )

    @Singleton
    @Provides
    fun provideServerSynchronizer(
        syncStatusDao: SyncStatusDao,
        networkApi: NetworkApi,
        trackSynchronizer: TrackSynchronizer,
        userSynchronizer: UserSynchronizer,
        playlistSynchronizer: PlaylistSynchronizer,
        playlistTrackSynchronizer: PlaylistTrackSynchronizer,
        reviewSourceSynchronizer: ReviewSourceSynchronizer
    ) = ServerSynchronizer(
        syncStatusDao,
        networkApi,
        trackSynchronizer,
        userSynchronizer,
        playlistSynchronizer,
        playlistTrackSynchronizer,
        reviewSourceSynchronizer
    )

    @Singleton
    @Provides
    fun provideTrackSynchronizer(networkApi: NetworkApi, trackDao: TrackDao) = TrackSynchronizer(networkApi, trackDao)

    @Singleton
    @Provides
    fun providePlaylistSynchronizer(networkApi: NetworkApi, playlistDao: PlaylistDao) = PlaylistSynchronizer(networkApi, playlistDao)

    @Singleton
    @Provides
    fun providePlaylistTrackSynchronizer(networkApi: NetworkApi, playlistTrackDao: PlaylistTrackDao) = PlaylistTrackSynchronizer(networkApi, playlistTrackDao)

    @Singleton
    @Provides
    fun provideUserSynchronizer(networkApi: NetworkApi, userDao: UserDao) = UserSynchronizer(networkApi, userDao)

    @Singleton
    @Provides
    fun provideReviewSourceSynchronizer(networkApi: NetworkApi, reviewSourceDao: ReviewSourceDao) = ReviewSourceSynchronizer(networkApi, reviewSourceDao)

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext app: Context) =
        app.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
}
