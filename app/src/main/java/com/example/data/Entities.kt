package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUrl: String, // Path or URI of the waste image (before)
    val afterImageUrl: String? = null, // Path or URI of the after-cleanup image
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "Submitted", "Verified", "In Progress", "Cleaned"
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val wasteType: String, // "Plastic", "Paper", "Glass", "Metal", "Organic Waste", "Mixed Waste", "E-Waste"
    val wasteConfidence: Double, // probability (0.0 to 1.0)
    val severityLevel: String, // "Low", "Medium", "High", "Critical"
    val cleanlinessScore: Int, // 1 to 100 (where 100 is pristine)
    val environmentalImpact: String, // description of impact
    val reporterName: String,
    val improvementPercentage: Double? = null, // improvement after cleanup
    val isVerified: Boolean = false,
    val verificationNotes: String? = null
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String, // unique email or username key
    val username: String,
    val email: String,
    val passwordHash: String,
    val avatarUrl: String, // name of default seed avatar, or uri
    val rewardPoints: Int = 0,
    val reportsSubmittedCount: Int = 0,
    val cleanupsCompletedCount: Int = 0,
    val isAdmin: Boolean = false,
    val currentStreak: Int = 0,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val challengeId: String,
    val title: String,
    val description: String,
    val pointsReward: Int,
    val targetType: String, // "Any", "Plastic", "Organic", "Count", "Cleanup"
    val targetThreshold: Int, // e.g. submit 3 reports
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val category: String, // "Novice", "Expert", "Social"
    val iconName: String
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val eventId: String,
    val title: String,
    val description: String,
    val organizer: String,
    val scheduledTime: Long,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val participantCount: Int = 0,
    val isJoined: Boolean = false,
    val pointsGiven: Int = 150
)
