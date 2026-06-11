package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class InstagramUserResponse(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class InstagramMediaData(
    @Json(name = "id") val id: String,
    @Json(name = "caption") val caption: String?,
    @Json(name = "media_url") val mediaUrl: String,
    @Json(name = "media_type") val mediaType: String,
    @Json(name = "timestamp") val timestamp: String,
    @Json(name = "username") val username: String
)

@JsonClass(generateAdapter = true)
data class InstagramMediaResponse(
    @Json(name = "data") val data: List<InstagramMediaData>
)

interface InstagramApi {
    @GET("me")
    suspend fun getProfile(
        @Query("fields") fields: String = "id,username,name",
        @Query("access_token") accessToken: String
    ): InstagramUserResponse

    @GET("me/media")
    suspend fun getUserMedia(
        @Query("fields") fields: String = "id,caption,media_url,media_type,timestamp,username",
        @Query("access_token") accessToken: String
    ): InstagramMediaResponse

    companion object {
        private const val BASE_URL = "https://graph.instagram.com/"

        fun create(): InstagramApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            return retrofit.create(InstagramApi::class.java)
        }
    }
}
