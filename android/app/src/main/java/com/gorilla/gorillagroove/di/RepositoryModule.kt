package com.gorilla.gorillagroove.di

import android.content.SharedPreferences
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.network.NetworkApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object RepositoryModule {

    @Singleton
    @Provides
    fun provideMainRepository(
        retrofit: NetworkApi,
        sharedPreferences: SharedPreferences,
        @Network.OkHttpClientProvider okClient: OkHttpClient
    ): MainRepository {
        return MainRepository(retrofit, sharedPreferences, okClient)
    }
}