package com.gorilla.gorillagroove.network

import android.content.Context
import com.google.gson.internal.LinkedTreeMap
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.network.login.LoginRequest
import com.gorilla.gorillagroove.network.login.LoginResponseNetworkEntity
import com.gorilla.gorillagroove.network.login.UpdateDeviceVersionRequest
import com.gorilla.gorillagroove.network.playlist.PlaylistKeyNetworkEntity
import com.gorilla.gorillagroove.network.playlist.PlaylistNetworkEntity
import com.gorilla.gorillagroove.network.track.*
import com.gorilla.gorillagroove.service.sync.*
import com.gorilla.gorillagroove.util.Constants
import com.gorilla.gorillagroove.util.Constants.SHARED_PREFERENCES_NAME
import okhttp3.MultipartBody
import retrofit2.http.*

interface NetworkApi {
    @GET("/api/track?page=0&size=4000&sort=id,asc")
    suspend fun get(): TrackWrapper

    @GET("api/track/{id}")
    suspend fun getTrack(@Path("id") songId: Long): TrackNetworkEntity

    @GET("/api/file/link/{id}?artSize=SMALL")
    suspend fun getTrackLink(@Path("id") songId: Long): TrackLinkResponse

    @POST("/api/authentication/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponseNetworkEntity

    @GET("api/user")
    suspend fun getAllUsers(): List<UserNetworkEntity>

    @GET("api/playlist")
    suspend fun getAllPlaylists(): List<PlaylistKeyNetworkEntity>

    @GET("api/playlist/track")
    suspend fun getAllPlaylistTracks(
        @Query("playlistId") playlistId: Long,
        @Query("sort") sort: String,
        @Query("size") size: Long
    ): PlaylistNetworkEntity

    @PUT("api/track/simple-update")
    suspend fun updateTrack(@Body updateTrackJson: TrackUpdate)

    @POST("api/track/mark-listened")
    suspend fun markTrackListened(@Body body: MarkListenedRequest)

    @Multipart
    @POST("api/crash-report")
    suspend fun uploadCrashReport(@Part zip: MultipartBody.Part)

    @PUT("api/device/version")
    suspend fun updateDeviceVersion(@Body body: UpdateDeviceVersionRequest)

    // I absolutely hate that I have a different request for each of these. But either Retrofit or GSON isn't smart enough to figure out a generic type.
    // When I use one, it converts everything into just a GSON tree and then tries to cast it to the class and fails. I've seen other people complain about
    // this on the internet but haven't seen a good solution that allows for using generics, and I don't want to keep messing with it right now.
    @GET("api/sync/entity-type/TRACK/minimum/{minimum}/maximum/{maximum}")
    suspend fun getTrackSyncEntities(
        @Path("minimum") minimum: Long,
        @Path("maximum") maximum: Long,
        @Query("page") page: Int,
        @Query("size") size: Int = 400,
    ): EntityChangeResponse<TrackResponse>

    @GET("api/sync/last-modified")
    suspend fun getLastModifiedTimestamps(): LastModifiedTimesResponse

    companion object {
        val apiToken: String? get() {
            return GGApplication
                .application
                .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(Constants.KEY_USER_TOKEN, null)
        }
    }
}
