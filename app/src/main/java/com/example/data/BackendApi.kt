package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class BackendReportRequest(
    @Json(name = "imageUrl") val imageUrl: String,
    @Json(name = "description") val description: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "wasteType") val wasteType: String,
    @Json(name = "severityLevel") val severityLevel: String,
    @Json(name = "reporterName") val reporterName: String,
    @Json(name = "timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class BackendReportResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String? = null,
    @Json(name = "body") val body: String? = null
)

interface BackendApiService {
    @POST("posts")
    suspend fun uploadReportToBackend(@Body request: BackendReportRequest): BackendReportResponse
}

object BackendRetrofitClient {
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: BackendApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BackendApiService::class.java)
    }
}
