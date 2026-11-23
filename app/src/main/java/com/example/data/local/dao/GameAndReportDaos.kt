package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.GameQuizEntity
import com.example.data.local.entity.GameScoreEntity
import com.example.data.local.entity.WeeklyReportEntity
import com.example.data.model.DepartmentType
import com.example.data.model.QuizCategory
import com.example.data.model.ReportStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM game_quizzes ORDER BY title ASC")
    fun getAllQuizzes(): Flow<List<GameQuizEntity>>

    @Query("SELECT * FROM game_quizzes WHERE category = :category ORDER BY title ASC")
    fun getQuizzesByCategory(category: QuizCategory): Flow<List<GameQuizEntity>>

    @Query("SELECT * FROM game_quizzes WHERE id = :id LIMIT 1")
    suspend fun getQuizById(id: String): GameQuizEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: GameQuizEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizzes(quizzes: List<GameQuizEntity>)

    // Scores & Leaderboards
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: GameScoreEntity)

    @Query("SELECT * FROM game_scores WHERE quizId = :quizId ORDER BY score DESC, timeTakenSeconds ASC LIMIT :limit")
    fun getLeaderboardForQuiz(quizId: String, limit: Int = 20): Flow<List<GameScoreEntity>>

    @Query("SELECT * FROM game_scores ORDER BY score DESC, timeTakenSeconds ASC LIMIT :limit")
    fun getGlobalLeaderboard(limit: Int = 20): Flow<List<GameScoreEntity>>

    @Query("SELECT * FROM game_scores WHERE userId = :userId ORDER BY timestamp DESC")
    fun getScoresForUser(userId: String): Flow<List<GameScoreEntity>>

    @Query("DELETE FROM game_scores WHERE id = :scoreId")
    suspend fun deleteScore(scoreId: String)

    @Query("DELETE FROM game_quizzes WHERE id = :quizId")
    suspend fun deleteQuiz(quizId: String)
}

@Dao
interface WeeklyReportDao {

    @Query("SELECT * FROM weekly_reports ORDER BY weekStartDateMillis DESC")
    fun getAllReports(): Flow<List<WeeklyReportEntity>>

    @Query("SELECT * FROM weekly_reports WHERE departmentOrUnit = :dept ORDER BY weekStartDateMillis DESC")
    fun getReportsByDepartment(dept: DepartmentType): Flow<List<WeeklyReportEntity>>

    @Query("SELECT * FROM weekly_reports WHERE submittedById = :userId ORDER BY weekStartDateMillis DESC")
    fun getReportsByUser(userId: String): Flow<List<WeeklyReportEntity>>

    @Query("SELECT * FROM weekly_reports WHERE id = :id LIMIT 1")
    fun getReportById(id: String): Flow<WeeklyReportEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: WeeklyReportEntity)

    @Update
    suspend fun updateReport(report: WeeklyReportEntity)

    @Query("UPDATE weekly_reports SET status = :status, reviewedById = :reviewedById, reviewerNotes = :notes WHERE id = :id")
    suspend fun reviewReport(id: String, status: ReportStatus, reviewedById: String, notes: String)

    @Query("DELETE FROM weekly_reports WHERE id = :id")
    suspend fun deleteReport(id: String)
}
