package com.gorilla.gorillagroove.network

import com.gorilla.gorillagroove.network.login.LoginRequest
import com.gorilla.gorillagroove.network.login.LoginResponseNetworkEntity
import com.gorilla.gorillagroove.network.login.UpdateDeviceVersionRequest
import com.gorilla.gorillagroove.network.track.*
import com.gorilla.gorillagroove.service.BackgroundTaskResponse
import com.gorilla.gorillagroove.service.sync.*
import com.gorilla.gorillagroove.ui.multiselectlist.AddToPlaylistRequest
import com.gorilla.gorillagroove.ui.multiselectlist.AddToPlaylistResponse
import com.gorilla.gorillagroove.ui.multiselectlist.RecommendTrackRequest
import com.gorilla.gorillagroove.ui.playlists.UpdatePlaylistRequest
import com.gorilla.gorillagroove.ui.reviewqueue.*
import com.gorilla.gorillagroove.ui.users.LiveTrackResponse
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

    @GET("/api/user/self")
    suspend fun getSelf(): UserResponse

    @PUT("api/track/simple-update")
    suspend fun updateTrack(@Body updateTrackJson: TrackUpdate): MultiTrackResponse

    @DELETE("api/track")
    suspend fun deleteTracks(@Query("trackIds") trackIds: List<Long>)

    @GET("api/track")
    suspend fun getTracks(
        @Query("userId") userId: Long,
        @Query("showHidden") showHidden: Boolean,
        @Query("sort") sortString: List<String>, // looks like "sort=artist,ASC&sort=album,ASC&sort=trackNumber,ASC" over the wire
        @Query("page") page: Int = 0,
        @Query("size") pageSize: Int = 500_000,
    ): LiveTrackResponse

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

    @GET("api/search/autocomplete/spotify/artist-name/{name}")
    suspend fun getSpotifyAutocompleteResult(@Path("name") name: String): AutocompleteResult

    @GET("api/search/autocomplete/youtube/channel-name/{name}")
    suspend fun getYouTubeAutocompleteResult(@Path("name") name: String): AutocompleteResult

    @POST("api/review-queue/subscribe/artist")
    suspend fun subscribeToSpotifyArtist(@Body body: AddArtistSourceRequest): ReviewSourceResponse

    @GET("api/search/spotify/artist/{artist}")
    suspend fun searchSpotifyByArtist(@Path("artist") artist: String): MetadataSearchResponse

    @POST("api/review-queue/subscribe/youtube-channel")
    suspend fun subscribeToYoutubeChannel(@Body body: AddYoutubeChannelRequest): ReviewSourceResponse

    @DELETE("api/review-queue/{id}")
    suspend fun deleteReviewSource(@Path("id") reviewSourceId: Long)

    @POST("api/background-task/youtube-dl")
    suspend fun queueYoutubeBackgroundTask(@Body body: DownloadYTVideoRequest): BackgroundTaskResponse

    @POST("api/background-task/metadata-dl")
    suspend fun queueMetadataDownloadTask(@Body body: MetadataImportRequest): BackgroundTaskResponse

    @GET("api/background-task")
    suspend fun getActiveBackgroundTasks(@Query("ids") ids: String): BackgroundTaskResponse

    @PUT("api/playlist/{id}")
    suspend fun updatePlaylist(
        @Path("id") playlistId: Long,
        @Body body: UpdatePlaylistRequest
    )

    @POST("api/playlist")
    suspend fun createPlaylist(@Body body: UpdatePlaylistRequest): PlaylistResponse

    @DELETE("api/playlist/{id}")
    suspend fun deletePlaylist(@Path("id") playlistId: Long)

    @POST("api/playlist/track")
    suspend fun addTracksToPlaylists(@Body body: AddToPlaylistRequest): AddToPlaylistResponse

    @POST("api/review-queue/recommend")
    suspend fun recommendTracks(@Body body: RecommendTrackRequest)

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
}
