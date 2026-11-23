package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.GameType
import com.example.data.model.SessionStatus
import com.example.data.model.SessionSubstate

@Entity(
    tableName = "multiplayer_game_sessions",
    indices = [
        Index(value = ["status"]),
        Index(value = ["scheduledStartTime"]),
        Index(value = ["gameType"])
    ]
)
data class MultiplayerGameSessionEntity(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val description: String,
    val gameType: GameType,
    val scheduledStartTime: Long,
    val actualStartTime: Long? = null,
    val actualEndTime: Long? = null,
    val status: SessionStatus = SessionStatus.SCHEDULED,
    val currentQuestionIndex: Int = 0,
    val currentSubstate: SessionSubstate = SessionSubstate.WAITING_TO_START,
    val currentQuestionDeadline: Long = 0L,
    val leaderboardDeadline: Long = 0L,
    val timeLimitPerQuestionSeconds: Int = 20,
    val leaderboardDurationSeconds: Int = 6,
    val baseScorePerQuestion: Int = 100,
    val speedBonusEnabled: Boolean = true,
    val maxSpeedBonus: Int = 50,
    val penaltyForIncorrect: Boolean = false,
    val questionsJson: String, // JSON array of MultiplayerQuestion
    val totalQuestions: Int,
    val createdById: String,
    val createdByName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val roomPin: String = ""
)

@Entity(
    tableName = "multiplayer_participants",
    indices = [
        Index(value = ["sessionId", "userId"], unique = true),
        Index(value = ["sessionId", "currentScore"]),
        Index(value = ["userId"])
    ]
)
data class MultiplayerParticipantEntity(
    @PrimaryKey
    val id: String, // UUID
    val sessionId: String,
    val userId: String,
    val userName: String,
    val userRole: String = "Member",
    val joinedAt: Long = System.currentTimeMillis(),
    val joinedAtQuestionIndex: Int = 0,
    val currentScore: Int = 0,
    val lastQuestionPoints: Int = 0,
    val lastAnswerTimestamp: Long = 0L, // Tie-breaker: earlier total response time
    val answeredQuestionsCount: Int = 0,
    val isOnline: Boolean = true
)

@Entity(
    tableName = "multiplayer_submissions",
    indices = [
        Index(value = ["sessionId", "questionIndex", "userId"], unique = true),
        Index(value = ["sessionId", "questionIndex"])
    ]
)
data class MultiplayerSubmissionEntity(
    @PrimaryKey
    val id: String, // UUID
    val sessionId: String,
    val questionIndex: Int,
    val userId: String,
    val userName: String,
    val submittedAnswer: String,
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val speedBonus: Int = 0,
    val submissionTime: Long = System.currentTimeMillis(),
    val isLate: Boolean = false
)
