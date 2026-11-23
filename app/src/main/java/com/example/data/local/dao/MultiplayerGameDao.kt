package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MultiplayerGameSessionEntity
import com.example.data.local.entity.MultiplayerParticipantEntity
import com.example.data.local.entity.MultiplayerSubmissionEntity
import com.example.data.model.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MultiplayerGameDao {

    // --- SESSIONS ---
    @Query("SELECT * FROM multiplayer_game_sessions ORDER BY scheduledStartTime DESC")
    fun getAllSessions(): Flow<List<MultiplayerGameSessionEntity>>

    @Query("SELECT * FROM multiplayer_game_sessions WHERE status = :status ORDER BY scheduledStartTime ASC")
    fun getSessionsByStatus(status: SessionStatus): Flow<List<MultiplayerGameSessionEntity>>

    @Query("SELECT * FROM multiplayer_game_sessions WHERE status IN ('SCHEDULED', 'ACTIVE') ORDER BY scheduledStartTime ASC")
    fun getUpcomingAndActiveSessions(): Flow<List<MultiplayerGameSessionEntity>>

    @Query("SELECT * FROM multiplayer_game_sessions WHERE status = 'DRAFT' ORDER BY createdAt DESC")
    fun getDraftSessions(): Flow<List<MultiplayerGameSessionEntity>>

    @Query("SELECT * FROM multiplayer_game_sessions WHERE status = 'COMPLETED' ORDER BY actualEndTime DESC, scheduledStartTime DESC")
    fun getCompletedSessions(): Flow<List<MultiplayerGameSessionEntity>>

    @Query("SELECT * FROM multiplayer_game_sessions WHERE id = :id LIMIT 1")
    fun getSessionById(id: String): Flow<MultiplayerGameSessionEntity?>

    @Query("SELECT * FROM multiplayer_game_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionByIdSync(id: String): MultiplayerGameSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MultiplayerGameSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<MultiplayerGameSessionEntity>)

    @Update
    suspend fun updateSession(session: MultiplayerGameSessionEntity)

    @Query("UPDATE multiplayer_game_sessions SET status = 'CANCELLED' WHERE id = :id")
    suspend fun cancelSession(id: String)

    @Query("DELETE FROM multiplayer_game_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    // --- PARTICIPANTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: MultiplayerParticipantEntity)

    @Update
    suspend fun updateParticipant(participant: MultiplayerParticipantEntity)

    @Query("SELECT * FROM multiplayer_participants WHERE sessionId = :sessionId AND userId = :userId LIMIT 1")
    suspend fun getParticipant(sessionId: String, userId: String): MultiplayerParticipantEntity?

    @Query("DELETE FROM multiplayer_participants WHERE userId LIKE 'peer-%'")
    suspend fun deleteSimulatedPeers()

    @Query("SELECT * FROM multiplayer_participants WHERE sessionId = :sessionId AND userId NOT LIKE 'peer-%' ORDER BY currentScore DESC, lastAnswerTimestamp ASC, userName ASC")
    fun getLeaderboardForSession(sessionId: String): Flow<List<MultiplayerParticipantEntity>>

    @Query("SELECT * FROM multiplayer_participants WHERE sessionId = :sessionId AND userId NOT LIKE 'peer-%' ORDER BY currentScore DESC, lastAnswerTimestamp ASC, userName ASC")
    suspend fun getLeaderboardForSessionSync(sessionId: String): List<MultiplayerParticipantEntity>

    @Query("SELECT COUNT(*) FROM multiplayer_participants WHERE sessionId = :sessionId AND userId NOT LIKE 'peer-%'")
    fun getParticipantCount(sessionId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM multiplayer_participants WHERE sessionId = :sessionId AND userId NOT LIKE 'peer-%'")
    suspend fun getParticipantCountSync(sessionId: String): Int

    // --- SUBMISSIONS ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubmission(submission: MultiplayerSubmissionEntity)

    @Query("SELECT * FROM multiplayer_submissions WHERE sessionId = :sessionId AND questionIndex = :questionIndex AND userId = :userId LIMIT 1")
    suspend fun getSubmission(sessionId: String, questionIndex: Int, userId: String): MultiplayerSubmissionEntity?

    @Query("SELECT * FROM multiplayer_submissions WHERE sessionId = :sessionId AND questionIndex = :questionIndex")
    suspend fun getSubmissionsForQuestion(sessionId: String, questionIndex: Int): List<MultiplayerSubmissionEntity>

    @Query("SELECT * FROM multiplayer_submissions WHERE sessionId = :sessionId ORDER BY submissionTime ASC")
    suspend fun getAllSubmissionsForSession(sessionId: String): List<MultiplayerSubmissionEntity>
}
