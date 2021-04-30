package com.gorilla.gorillagroove.network

import android.content.Context
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.network.login.LoginRequest
import com.gorilla.gorillagroove.network.login.LoginResponseNetworkEntity
import com.gorilla.gorillagroove.network.login.UpdateDeviceVersionRequest
import com.gorilla.gorillagroove.network.track.*
import com.gorilla.gorillagroove.service.sync.*
import com.gorilla.gorillagroove.util.Constants
import com.gorilla.gorillagroove.util.Constants.SHARED_PREFERENCES_NAME
import okhttp3.MultipartBody
import retrofit2.http.*

interface NetworkApi {

    @GET("/api/file/link/{id}")
    suspend fun getTrackLink(
        @Path("id") trackId: Long,
        @Query("artSize") size: String = "LARGE",
    ): TrackLinkResponse

    @POST("/api/authentication/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponseNetworkEntity

    @PUT("api/track/simple-update")
    suspend fun updateTrack(@Body updateTrackJson: TrackUpdate): MultiTrackResponse

    @POST("api/track/mark-listened")
    suspend fun markTrackListened(@Body body: MarkListenedRequest)

    @Multipart
    @POST("api/crash-report")
    suspend fun uploadCrashReport(@Part zip: MultipartBody.Part)

    @PUT("api/device/version")
    suspend fun updateDeviceVersion(@Body body: UpdateDeviceVersionRequest)

    @DELETE("api/review-queue/track/{id}")
    suspend fun rejectReviewTrack(@Path("id") trackId: Long)

    @POST("api/review-queue/track/{id}/approve")
    suspend fun approveReviewTrack(@Path("id") trackId: Long)

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

    @GET("api/sync/entity-type/USER/minimum/{minimum}/maximum/{maximum}")
    suspend fun getUserSyncEntities(
        @Path("minimum") minimum: Long,
        @Path("maximum") maximum: Long,
        @Query("page") page: Int,
        @Query("size") size: Int = 400,
    ): EntityChangeResponse<UserResponse>

    @GET("api/sync/entity-type/PLAYLIST/minimum/{minimum}/maximum/{maximum}")
    suspend fun getPlaylistSyncEntities(
        @Path("minimum") minimum: Long,
        @Path("maximum") maximum: Long,
        @Query("page") page: Int,
        @Query("size") size: Int = 400,
    ): EntityChangeResponse<PlaylistResponse>

    @GET("api/sync/entity-type/PLAYLIST_TRACK/minimum/{minimum}/maximum/{maximum}")
    suspend fun getPlaylistTrackSyncEntities(
        @Path("minimum") minimum: Long,
        @Path("maximum") maximum: Long,
        @Query("page") page: Int,
        @Query("size") size: Int = 400,
    ): EntityChangeResponse<PlaylistTrackResponse>

    @GET("api/sync/entity-type/REVIEW_SOURCE/minimum/{minimum}/maximum/{maximum}")
    suspend fun getReviewSourceSyncEntities(
        @Path("minimum") minimum: Long,
        @Path("maximum") maximum: Long,
        @Query("page") page: Int,
        @Query("size") size: Int = 400,
    ): EntityChangeResponse<ReviewSourceResponse>

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
