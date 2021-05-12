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
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.service.MarkListenedService
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
        musicServiceConnection: MusicServiceConnection
    ) = MarkListenedService.getInstance(
        musicServiceConnection
    )

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext app: Context) =
        app.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
}
