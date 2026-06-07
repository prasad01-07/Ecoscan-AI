package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EcoScanDao {

    // --- REPORT OPERATIONS ---
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getReportById(id: Long): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteReportById(id: Long)

    @Query("SELECT * FROM reports WHERE status = 'Cleaned'")
    fun getCleanedReports(): Flow<List<ReportEntity>>

    // --- USER OPERATIONS ---
    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserSync(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users ORDER BY rewardPoints DESC LIMIT 10")
    fun getLeaderboard(): Flow<List<UserEntity>>

    // --- CHALLENGE OPERATIONS ---
    @Query("SELECT * FROM challenges ORDER BY isCompleted ASC, pointsReward DESC")
    fun getAllChallenges(): Flow<List<ChallengeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<ChallengeEntity>)

    @Update
    suspend fun updateChallenge(challenge: ChallengeEntity)

    // --- EVENT OPERATIONS ---
    @Query("SELECT * FROM events ORDER BY scheduledTime ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Update
    suspend fun updateEvent(event: EventEntity)
}
