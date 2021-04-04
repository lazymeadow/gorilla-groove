package com.gorilla.gorillagroove.di

import com.google.gson.*
import com.gorilla.gorillagroove.network.NetworkApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Qualifier
import javax.inject.Singleton


@Module
@InstallIn(ApplicationComponent::class)
object NetworkModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OkHttpClientProvider

    @Singleton
    @Provides
    fun provideGsonBuilder(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Instant::class.java, object : JsonDeserializer<Instant> {
                override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instant {
                    return OffsetDateTime.parse(json.asJsonPrimitive.asString).toInstant()
                }
            })
            .create()
    }

    @Singleton
    @Provides
    fun provideRetrofit(gson: Gson): Retrofit.Builder {
        return Retrofit.Builder()
            .baseUrl("https://gorillagroove.net/")
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
    }

    @Singleton
    @Provides
    fun provideTrackService(retrofit: Retrofit.Builder): NetworkApi {
        return retrofit
            .build()
            .create(NetworkApi::class.java)
    }

    @Singleton
    @Provides
    @OkHttpClientProvider
    fun provideOkHttpClient(): OkHttpClient {
           return OkHttpClient.Builder()
               // Automatically add the authorization header if we have a valid token
               .addInterceptor { chain ->
                   val request = NetworkApi.apiToken?.let { authToken ->
                       chain.request().newBuilder().addHeader("Authorization", authToken).build()
                   } ?: run {
                       chain.request().newBuilder().build()
                   }

                   return@addInterceptor chain.proceed(request)
               }
               .build()
    }
}
