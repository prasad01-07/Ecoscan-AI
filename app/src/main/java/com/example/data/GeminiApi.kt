package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Call Structures ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String // Base64 encoded JPEG
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null, // Set to "application/json"
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- Domain Models for Analysis Responses ---

@JsonClass(generateAdapter = true)
data class WasteAnalysisResult(
    @Json(name = "is_waste") val isWaste: Boolean,
    @Json(name = "confidence_score") val confidenceScore: Double,
    @Json(name = "waste_type") val wasteType: String, // "Plastic", "Paper", "Glass", "Metal", "Organic Waste", "Mixed Waste", "E-Waste"
    @Json(name = "probabilities") val probabilities: Map<String, Double>?,
    @Json(name = "severity_level") val severityLevel: String, // "Low", "Medium", "High", "Critical"
    @Json(name = "cleanliness_score") val cleanlinessScore: Int, // 1 to 100
    @Json(name = "environmental_impact") val environmentalImpact: String,
    @Json(name = "detected_percentage") val detectedPercentage: Double,
    @Json(name = "recom_cleanup_priority") val recomCleanupPriority: String,
    @Json(name = "ai_recommendation") val aiRecommendation: String
)

// --- Retrofit Config ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun analyzeImage(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val moshiParser: Moshi by lazy { moshi }
}
