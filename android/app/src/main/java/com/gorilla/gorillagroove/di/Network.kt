package com.gorilla.gorillagroove.di

import android.content.Context
import com.google.gson.*
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.network.NetworkApi
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGSettings
import com.gorilla.gorillagroove.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.Buffer
import okio.IOException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Qualifier
import javax.inject.Singleton


@Module
@InstallIn(ApplicationComponent::class)
object Network {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OkHttpClientProvider

    private fun provideGsonBuilder(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Instant::class.java, object : JsonDeserializer<Instant> {
                override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instant {
                    return OffsetDateTime.parse(json.asJsonPrimitive.asString).toInstant()
                }
            })
            .create()
    }

    private fun provideRetrofit(gson: Gson): Retrofit.Builder {
        return Retrofit.Builder()
            .baseUrl("https://gorillagroove.net/")
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
    }

    private fun provideNetworkApi(retrofit: Retrofit.Builder): NetworkApi {
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
                   val req = chain.request()
                   val body = req.body?.toReadableString() ?: ""
                   logDebug("${req.method} ${req.url} $body")

                   if (GGSettings.offlineModeEnabled) {
                       throw OfflineModeEnabledException()
                   }

                   val request = apiToken?.let { authToken ->
                       chain.request().newBuilder().addHeader("Authorization", authToken).build()
                   } ?: run {
                       chain.request().newBuilder().build()
                   }

                   return@addInterceptor chain.proceed(request)
               }
               .build()
    }

    val api = provideNetworkApi(provideRetrofit(provideGsonBuilder()))

    private val apiToken: String? get() {
        return GGApplication
            .application
            .getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(Constants.KEY_USER_TOKEN, null)
    }
}

fun RequestBody.toReadableString(): String {
    return try {
        val copy = this
        val buffer = Buffer()
        copy.writeTo(buffer)
        buffer.readUtf8()
    } catch (e: IOException) {
        "--Unparsable--"
    }
}

class OfflineModeEnabledException : RuntimeException("Offline mode is enabled")
