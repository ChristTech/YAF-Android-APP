package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.DepartmentType
import com.example.data.model.QuizCategory
import com.example.data.model.ReportStatus

@Entity(
    tableName = "game_quizzes",
    indices = [
        Index(value = ["category"])
    ]
)
data class GameQuizEntity(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val description: String,
    val category: QuizCategory,
    val questionsJson: String, // JSON array of questions with options and answer
    val totalQuestions: Int,
    val timeLimitSeconds: Int = 120,
    val difficulty: String = "Medium"
)

@Entity(
    tableName = "game_scores",
    indices = [
        Index(value = ["quizId"]),
        Index(value = ["userId"]),
        Index(value = ["score"]),
        Index(value = ["timestamp"])
    ]
)
data class GameScoreEntity(
    @PrimaryKey
    val id: String, // UUID
    val quizId: String,
    val quizTitle: String,
    val userId: String,
    val userName: String,
    val score: Int,
    val totalQuestions: Int,
    val timeTakenSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weekly_reports",
    indices = [
        Index(value = ["departmentOrUnit"]),
        Index(value = ["weekStartDateMillis"]),
        Index(value = ["status"])
    ]
)
data class WeeklyReportEntity(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val departmentOrUnit: DepartmentType,
    val fellowshipGroup: String = "",
    val submittedById: String,
    val submittedByName: String,
    val submittedByRole: String,
    val weekStartDateMillis: Long,
    val weekEndDateMillis: Long,
    val meetingAttendance: Int = 0,
    val maleCount: Int = 0,
    val femaleCount: Int = 0,
    val firstTimersCount: Int = 0,
    val activitiesConducted: String,
    val keyAchievements: String,
    val challengesEncountered: String,
    val financialSummary: String = "",
    val nextWeekGoals: String,
    val submittedAt: Long = System.currentTimeMillis(),
    val status: ReportStatus = ReportStatus.SUBMITTED,
    val reviewedById: String? = null,
    val reviewerNotes: String? = null
)
