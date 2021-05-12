package com.gorilla.gorillagroove.di

import com.gorilla.gorillagroove.repository.MainRepository
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
        @Network.OkHttpClientProvider okClient: OkHttpClient
    ): MainRepository {
        return MainRepository(okClient)
    }
}