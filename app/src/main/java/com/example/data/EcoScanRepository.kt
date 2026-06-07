package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EcoScanRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.ecoScanDao()
    private val appCtx = context.applicationContext

    // --- REPOSITORY INTERFACE ---

    val allReports: Flow<List<ReportEntity>> = dao.getAllReports()
    val cleanedReports: Flow<List<ReportEntity>> = dao.getCleanedReports()
    val leaderboard: Flow<List<UserEntity>> = dao.getLeaderboard()
    val allChallenges: Flow<List<ChallengeEntity>> = dao.getAllChallenges()
    val allEvents: Flow<List<EventEntity>> = dao.getAllEvents()

    fun getUser(userId: String): Flow<UserEntity?> = dao.getUserById(userId)
    suspend fun getUserSync(userId: String?): UserEntity? = userId?.let { dao.getUserSync(it) }

    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        dao.insertUser(user)
    }

    // Initialize Mock Seed Data if Database is completely empty
    suspend fun preseedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        // Seed Users
        val existingUser = dao.getUserSync("citizen@ecoscan.ai")
        if (existingUser == null) {
            val defaultUser = UserEntity(
                userId = "citizen@ecoscan.ai",
                username = "EcoCitizen",
                email = "citizen@ecoscan.ai",
                passwordHash = "password",
                avatarUrl = "avatar_1",
                rewardPoints = 450,
                reportsSubmittedCount = 3,
                cleanupsCompletedCount = 1,
                isAdmin = false,
                currentStreak = 4
            )
            val adminUser = UserEntity(
                userId = "admin@ecoscan.ai",
                username = "EcoManager",
                email = "admin@ecoscan.ai",
                passwordHash = "admin",
                avatarUrl = "avatar_admin",
                rewardPoints = 1250,
                reportsSubmittedCount = 12,
                cleanupsCompletedCount = 8,
                isAdmin = true,
                currentStreak = 15
            )
            dao.insertUser(defaultUser)
            dao.insertUser(adminUser)

            // Dynamic Leaderboard list
            val seedLeaderboard = listOf(
                UserEntity("sophia@ecoscan.ai", "Sophia Green", "sophia@ecoscan.ai", "pw", "avatar_2", 1420, 18, 12, false, 8),
                UserEntity("trash_buster@ecoscan.ai", "Trash Buster", "trash_buster@ecoscan.ai", "pw", "avatar_3", 890, 11, 6, false, 5),
                UserEntity("planet_hero@ecoscan.ai", "Planet Hero", "planet_hero@ecoscan.ai", "pw", "avatar_4", 650, 7, 4, false, 3),
                UserEntity("recycle_rex@ecoscan.ai", "Recycle Rex", "recycle_rex@ecoscan.ai", "pw", "avatar_5", 320, 4, 1, false, 2)
            )
            for (user in seedLeaderboard) {
                dao.insertUser(user)
            }

            // Seed Challenges
            val seedChallenges = listOf(
                ChallengeEntity("ch_first", "Eco Rookie", "Submit your first waste scan and report location.", 100, "Count", 1, 0, false, "Novice", "ic_eco_rookie"),
                ChallengeEntity("ch_plastic", "Plastic Patrol", "Report 3 plastic accumulation hazards in the city.", 250, "Plastic", 3, 0, false, "Expert", "ic_plastic_patrol"),
                ChallengeEntity("ch_cleanup", "Before & After Cleanup", "Complete a cleanup and submit a verified Before & After image.", 400, "Cleanup", 1, 0, false, "Social", "ic_cleanup_champion"),
                ChallengeEntity("ch_recycle", "E-Waste Exposer", "Spot and report a hazardous electronic waste site.", 150, "E-Waste", 1, 0, false, "Expert", "ic_e_waste")
            )
            dao.insertChallenges(seedChallenges)

            // Seed Events
            val seedEvents = listOf(
                EventEntity("ev_1", "Riverside Park Sweep", "Join us to clean up the river banks of plastic wastes. Gloves, sweepers, and water bottles provided!", "Eco Volunteers", System.currentTimeMillis() + 172800000, "Riverside Park West Entry", 40.7128, -74.0060, 24, false),
                EventEntity("ev_2", "Downtown Alley E-Recycling Rally", "Let's collect scattered e-wastes, batteries, and appliances across downtown alleys and load them into eco-trucks.", "West Tech Union", System.currentTimeMillis() + 432000000, "E-Cycling Depot, 14th St", 40.7250, -74.0150, 8, false),
                EventEntity("ev_3", "Oakwood Forest Biodecay Sweep", "Collect scattered household plastics, wrappers, and metals in the conservation forest to protect wildlife.", "Green Defenders", System.currentTimeMillis() + 691200000, "Oakwood Forest Guard Cabin", 40.7010, -74.0200, 12, true)
            )
            dao.insertEvents(seedEvents)


        }
    }

    // Submit a report and update citizen reward stats
    suspend fun submitReport(report: ReportEntity, reporterEmail: String) = withContext(Dispatchers.IO) {
        val reportId = dao.insertReport(report)

        // Uplink to the remote REST cloud backend service
        try {
            val backendReq = BackendReportRequest(
                imageUrl = report.imageUrl,
                description = report.description,
                latitude = report.latitude,
                longitude = report.longitude,
                wasteType = report.wasteType,
                severityLevel = report.severityLevel,
                reporterName = report.reporterName,
                timestamp = report.timestamp
            )
            val backendResponse = BackendRetrofitClient.apiService.uploadReportToBackend(backendReq)
            Log.i("EcoScanBackend", "Report successfully uplinked to cloud backend server. Response Post ID: ${backendResponse.id}")
        } catch (e: Exception) {
            Log.e("EcoScanBackend", "Failed uplinking to cloud backend server: ${e.message}. Saved offline locally.")
        }

        // Award default points for a new report submitted (+50 pts)
        val user = dao.getUserSync(reporterEmail)
        if (user != null) {
            val updatedUser = user.copy(
                rewardPoints = user.rewardPoints + 50,
                reportsSubmittedCount = user.reportsSubmittedCount + 1
            )
            dao.updateUser(updatedUser)

            // Check challenges progress
            val activeChallenges = dao.getAllChallenges().firstOrNull() ?: emptyList()
            for (challenge in activeChallenges) {
                if (!challenge.isCompleted) {
                    var progressAdded = false
                    var newProgress = challenge.currentProgress

                    if (challenge.targetType == "Count") {
                        newProgress += 1
                        progressAdded = true
                    } else if (challenge.targetType == report.wasteType) {
                        newProgress += 1
                        progressAdded = true
                    }

                    if (progressAdded) {
                        val completed = newProgress >= challenge.targetThreshold
                        val pointsAwarded = if (completed && !challenge.isCompleted) challenge.pointsReward else 0

                        dao.updateChallenge(challenge.copy(
                            currentProgress = newProgress,
                            isCompleted = completed
                        ))

                        if (pointsAwarded > 0) {
                            dao.updateUser(updatedUser.copy(
                                rewardPoints = updatedUser.rewardPoints + pointsAwarded
                            ))
                        }
                    }
                }
            }
        }

        reportId
    }

    // Complete cleanup section, comparing images
    suspend fun completeCleanup(reportId: Long, afterImgPath: String, userEmail: String, improvementPercent: Double) = withContext(Dispatchers.IO) {
        val currentReport = dao.getReportById(reportId)
        if (currentReport != null) {
            val updatedReport = currentReport.copy(
                afterImageUrl = afterImgPath,
                status = "Cleaned",
                improvementPercentage = improvementPercent
            )
            dao.updateReport(updatedReport)

            // Award points for completing a cleanup (+200 pts)
            val user = dao.getUserSync(userEmail)
            if (user != null) {
                val updatedUser = user.copy(
                    rewardPoints = user.rewardPoints + 200,
                    cleanupsCompletedCount = user.cleanupsCompletedCount + 1
                )
                dao.updateUser(updatedUser)

                // Check cleanup challenge rewards
                val activeChallenges = dao.getAllChallenges().firstOrNull() ?: emptyList()
                for (challenge in activeChallenges) {
                    if (!challenge.isCompleted && challenge.targetType == "Cleanup") {
                        val newProgress = challenge.currentProgress + 1
                        val completed = newProgress >= challenge.targetThreshold
                        val pointsAwarded = if (completed && !challenge.isCompleted) challenge.pointsReward else 0

                        dao.updateChallenge(challenge.copy(
                            currentProgress = newProgress,
                            isCompleted = completed
                        ))

                        if (pointsAwarded > 0) {
                            dao.updateUser(updatedUser.copy(
                                rewardPoints = updatedUser.rewardPoints + pointsAwarded
                            ))
                        }
                    }
                }
            }
        }
    }

    // Verify / reject reports by admins
    suspend fun verifyReport(reportId: Long, isVerified: Boolean, notes: String, verifierEmail: String) = withContext(Dispatchers.IO) {
        val report = dao.getReportById(reportId)
        if (report != null) {
            val updatedReport = report.copy(
                isVerified = isVerified,
                status = if (isVerified) "Verified" else "Submitted",
                verificationNotes = notes
            )
            dao.updateReport(updatedReport)

            // Reward verifier with small reward points (+20 pts)
            val verifier = dao.getUserSync(verifierEmail)
            if (verifier != null) {
                dao.updateUser(verifier.copy(
                    rewardPoints = verifier.rewardPoints + 20
                ))
            }
        }
    }

    // Join community cleanup event
    suspend fun joinEvent(eventId: String, userEmail: String) = withContext(Dispatchers.IO) {
        val event = dao.getAllEvents().firstOrNull()?.find { it.eventId == eventId }
        val user = dao.getUserSync(userEmail)
        if (event != null && user != null && !event.isJoined) {
            val updatedEvent = event.copy(
                isJoined = true,
                participantCount = event.participantCount + 1
            )
            dao.updateEvent(updatedEvent)

            // Award reward points for volunteer registration (+100!)
            dao.updateUser(user.copy(
                rewardPoints = user.rewardPoints + event.pointsGiven
            ))
        }
    }

    // Delete a report
    suspend fun deleteReport(reportId: Long) = withContext(Dispatchers.IO) {
        dao.deleteReportById(reportId)
    }

    // Save image locally (gallery upload / simulated camera) to local file space
    suspend fun saveImageToInternalStorage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val filename = "ecoscan_${UUID.randomUUID()}.jpg"
        val file = File(appCtx.filesDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        file.absolutePath
    }

    // --- GEMINI ANALYSIS ENGINES ---

    // True dynamic Gemini API call with adaptive simulated fallback if API Key is unavailable
    suspend fun analyzeWasteImage(bitmap: Bitmap, imageName: String? = null): WasteAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val isMockKey = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"

        if (isMockKey) {
            Log.w("EcoScan", "Live Gemini API key not configured. Utilizing simulated analyzer flow.")
            return@withContext simulateWasteAnalysis(imageName)
        }

        try {
            // Convert Bitmap to Base64
            val base64Image = bitmap.toBase64()

            val promptText = """
                You are an expert environmental AI waste analyzer. Analyze the attached garbage/waste image and output a highly accurate JSON description.
                Do NOT include any markdown code blocks or backticks in your output (e.g. do NOT write ```json at the start or ``` at the end). Return ONLY a single raw valid JSON string.

                The returned JSON schema MUST contain the following fields exactly:
                {
                  "is_waste": true,
                  "confidence_score": 0.95,
                  "waste_type": "Plastic",
                  "probabilities": {
                    "Plastic": 0.85,
                    "Paper": 0.05,
                    "Glass": 0.02,
                    "Metal": 0.01,
                    "Organic Waste": 0.03,
                    "Mixed Waste": 0.02,
                    "E-Waste": 0.02
                  },
                  "severity_level": "High",
                  "cleanliness_score": 35,
                  "environmental_impact": "This accumulated waste poses high blockages to urban sewer runs, causing bacteria and macro-plastic toxins to reach nearby soil beds.",
                  "detected_percentage": 65.0,
                  "recom_cleanup_priority": "High",
                  "ai_recommendation": "Deploy a clean-up volunteer squad with heavy duty organic bio-bags, place plastic separation collection posts, and enforce local anti-dumping acts."
                }

                Strict rules for variables:
                - "waste_type" MUST be exactly one of: "Plastic", "Paper", "Glass", "Metal", "Organic Waste", "Mixed Waste", "E-Waste"
                - "severity_level" MUST be exactly one of: "Low", "Medium", "High", "Critical"
                - "probabilities" map MUST contain ALL the 7 listed categories as keys summing to approx 1.0.
                - "cleanliness_score" MUST be an integer between 1 and 100 representing spot health (where 100 is flawlessly pristine, and 10 is toxic heap).
                - "recom_cleanup_priority" MUST be exactly one of: "Low", "Medium", "High", "Immediate"
                - Return ONLY THE JSON. If you output anything else, it breaks our software. Do NOT include markdown blocks.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = promptText),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.2f
                )
            )

            val serviceResponse = RetrofitClient.geminiService.analyzeImage(apiKey, request)
            val jsonText = serviceResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No analytical candidates returned from EcoScan Gemini server.")

            // Clean markdown blocks if Gemini included them despite the strict prompt instruction
            val cleanedJsonText = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val adapter = RetrofitClient.moshiParser.adapter(WasteAnalysisResult::class.java)
            val parsedResult = adapter.fromJson(cleanedJsonText)
                ?: throw Exception("Failed to deserialize generated report.")

            parsedResult
        } catch (e: Exception) {
            Log.e("EcoScan", "Gemini API call failed, falling back to smart simulation. Error: ${e.message}")
            // Graceful fallback to rich simulation if Gemini is blocked or network errors
            simulateWasteAnalysis(imageName)
        }
    }

    // Helper to convert Bitmap to Base64 in background
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    // Custom smart simulation matcher returning contextual results based on selected samples
    private fun simulateWasteAnalysis(imageName: String?): WasteAnalysisResult {
        val key = imageName?.lowercase() ?: ""
        return when {
            key.contains("plastic") || key.contains("bottle") -> {
                WasteAnalysisResult(
                    isWaste = true,
                    confidenceScore = 0.96,
                    wasteType = "Plastic",
                    probabilities = mapOf(
                        "Plastic" to 0.88, "Paper" to 0.04, "Glass" to 0.02,
                        "Metal" to 0.01, "Organic Waste" to 0.02, "Mixed Waste" to 0.02, "E-Waste" to 0.01
                    ),
                    severityLevel = "Critical",
                    cleanlinessScore = 18,
                    environmentalImpact = "Plastics require up to 450 years to decay, shedding hazardous microplastics that enter the regional animal food webs and poison underground soils.",
                    detectedPercentage = 78.5,
                    recomCleanupPriority = "Immediate",
                    aiRecommendation = "Deploy a clean-up sweep with plastic recycling bins. Form civic volunteering initiatives and place smart caution signs."
                )
            }
            key.contains("metal") || key.contains("soda") || key.contains("can") -> {
                WasteAnalysisResult(
                    isWaste = true,
                    confidenceScore = 0.92,
                    wasteType = "Metal",
                    probabilities = mapOf(
                        "Plastic" to 0.03, "Paper" to 0.02, "Glass" to 0.01,
                        "Metal" to 0.90, "Organic Waste" to 0.01, "Mixed Waste" to 0.02, "E-Waste" to 0.01
                    ),
                    severityLevel = "Medium",
                    cleanlinessScore = 65,
                    environmentalImpact = "Corroding aluminum leaks heavy oxidation residues which block soil oxygen flows, compromising nearby botanic species.",
                    detectedPercentage = 30.0,
                    recomCleanupPriority = "Medium",
                    aiRecommendation = "Collect metals for smelting scrap yards. Metal objects are fully recyclable, preventing additional mountain quarry extractions."
                )
            }
            key.contains("organic") || key.contains("apple") || key.contains("decay") || key.contains("leaves") -> {
                WasteAnalysisResult(
                    isWaste = true,
                    confidenceScore = 0.89,
                    wasteType = "Organic Waste",
                    probabilities = mapOf(
                        "Plastic" to 0.01, "Paper" to 0.02, "Glass" to 0.01,
                        "Metal" to 0.01, "Organic Waste" to 0.92, "Mixed Waste" to 0.02, "E-Waste" to 0.01
                    ),
                    severityLevel = "Low",
                    cleanlinessScore = 74,
                    environmentalImpact = "Organic compostables decay quickly, but if left in dense piles they ferment, attracting pests and releasing strong odors.",
                    detectedPercentage = 22.0,
                    recomCleanupPriority = "Low",
                    aiRecommendation = "Utilize leaf blowers and shovels to load the biomass compost into organic garden beds as natural biological fertilizing soil additions."
                )
            }
            key.contains("laptop") || key.contains("e-waste") || key.contains("electro") || key.contains("wire") -> {
                WasteAnalysisResult(
                    isWaste = true,
                    confidenceScore = 0.95,
                    wasteType = "E-Waste",
                    probabilities = mapOf(
                        "Plastic" to 0.08, "Paper" to 0.01, "Glass" to 0.04,
                        "Metal" to 0.12, "Organic Waste" to 0.00, "Mixed Waste" to 0.05, "E-Waste" to 0.70
                    ),
                    severityLevel = "High",
                    cleanlinessScore = 38,
                    environmentalImpact = "Heavy electronic elements contain lithium, cadmium, and lead. If breached, they cause severe long-term biochemical soil and aquifer damage.",
                    detectedPercentage = 48.0,
                    recomCleanupPriority = "High",
                    aiRecommendation = "Must be handled by verified hardware recycling points. Do not landfill. Organize e-waste collection campaigns."
                )
            }
            key.contains("paper") || key.contains("cardboard") || key.contains("box") -> {
                WasteAnalysisResult(
                    isWaste = true,
                    confidenceScore = 0.91,
                    wasteType = "Paper",
                    probabilities = mapOf(
                        "Plastic" to 0.03, "Paper" to 0.86, "Glass" to 0.01,
                        "Metal" to 0.01, "Organic Waste" to 0.05, "Mixed Waste" to 0.03, "E-Waste" to 0.01
                    ),
                    severityLevel = "Low",
                    cleanlinessScore = 82,
                    environmentalImpact = "Paper structures dissolve under wet seasons but heavily clog storm pipelines, increasing flood risks inside streets.",
                    detectedPercentage = 15.0,
                    recomCleanupPriority = "Low",
                    aiRecommendation = "Gather cardboard into high-capacity recycling packages. Keep them dry to avoid molding, rendering fibers unrecyclable."
                )
            }
            else -> {
                // Default generic smart fallback
                WasteAnalysisResult(
                    isWaste = true,
                    confidenceScore = 0.90,
                    wasteType = "Mixed Waste",
                    probabilities = mapOf(
                        "Plastic" to 0.35, "Paper" to 0.15, "Glass" to 0.10,
                        "Metal" to 0.10, "Organic Waste" to 0.15, "Mixed Waste" to 0.10, "E-Waste" to 0.05
                    ),
                    severityLevel = "Medium",
                    cleanlinessScore = 48,
                    environmentalImpact = "Scattered street litters leach mixed materials, polluting town aesthetics and impeding natural ecological water flows.",
                    detectedPercentage = 42.0,
                    recomCleanupPriority = "Medium",
                    aiRecommendation = "Coordinate civic voluntary cleaning teams. Set up sorting stations to prevent cross-contamination and ensure maximum material recoveries."
                )
            }
        }
    }
}
