package com.example.data.repository

import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.GameDao
import com.example.data.local.dao.WeeklyReportDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.GameQuizEntity
import com.example.data.local.entity.GameScoreEntity
import com.example.data.local.entity.WeeklyReportEntity
import com.example.data.model.DepartmentType
import com.example.data.model.QuizCategory
import com.example.data.model.ReportStatus
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow

interface GamesRepository {
    fun getAllQuizzes(): Flow<List<GameQuizEntity>>
    fun getQuizzesByCategory(category: QuizCategory): Flow<List<GameQuizEntity>>
    suspend fun getQuizById(id: String): GameQuizEntity?
    suspend fun insertQuiz(quiz: GameQuizEntity, adminUserId: String): Boolean
    suspend fun deleteQuiz(quizId: String, adminUserId: String): Boolean
    suspend fun submitScore(score: GameScoreEntity, userRole: UserRole): Boolean
    suspend fun deleteScore(scoreId: String, adminUserId: String): Boolean
    fun getLeaderboardForQuiz(quizId: String): Flow<List<GameScoreEntity>>
    fun getGlobalLeaderboard(): Flow<List<GameScoreEntity>>
    fun getScoresForUser(userId: String): Flow<List<GameScoreEntity>>
    suspend fun syncCloudQuizzesToLocalDb(cloudQuizzes: List<com.example.data.remote.CloudGameQuiz>) {}
    suspend fun syncCloudScoresToLocalDb(cloudScores: List<com.example.data.remote.CloudGameScore>) {}
}

class GamesRepositoryImpl(
    private val gameDao: GameDao,
    private val auditLogDao: AuditLogDao,
    private val cloudRepository: GideonitesCloudRepository? = null
) : GamesRepository {

    override fun getAllQuizzes(): Flow<List<GameQuizEntity>> = gameDao.getAllQuizzes()

    override fun getQuizzesByCategory(category: QuizCategory): Flow<List<GameQuizEntity>> =
        gameDao.getQuizzesByCategory(category)

    override suspend fun getQuizById(id: String): GameQuizEntity? = gameDao.getQuizById(id)

    override suspend fun insertQuiz(quiz: GameQuizEntity, adminUserId: String): Boolean {
        gameDao.insertQuiz(quiz)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = adminUserId,
                userRole = UserRole.ADMINISTRATOR,
                actionType = "QUIZ_CREATED",
                targetEntity = "GameQuiz",
                targetId = quiz.id,
                details = "Admin created quiz: ${quiz.title} (${quiz.category.displayName})"
            )
        )
        runCatching {
            cloudRepository?.upsertGameQuiz(
                quizId = quiz.id,
                createdById = adminUserId,
                title = quiz.title,
                description = quiz.description,
                category = quiz.category.name,
                questionsJson = quiz.questionsJson,
                totalQuestions = quiz.totalQuestions,
                timeLimitSeconds = quiz.timeLimitSeconds,
                difficulty = quiz.difficulty
            )
        }
        return true
    }

    override suspend fun deleteQuiz(quizId: String, adminUserId: String): Boolean {
        gameDao.deleteQuiz(quizId)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = adminUserId,
                userRole = UserRole.ADMINISTRATOR,
                actionType = "QUIZ_DELETED",
                targetEntity = "GameQuiz",
                targetId = quizId,
                details = "Admin deleted quiz ID: $quizId"
            )
        )
        runCatching {
            cloudRepository?.deleteGameQuiz(quizId)
        }
        return true
    }

    override suspend fun submitScore(score: GameScoreEntity, userRole: UserRole): Boolean {
        gameDao.insertScore(score)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = score.userId,
                userRole = userRole,
                actionType = "GAME_SCORE_SUBMITTED",
                targetEntity = "GameScore",
                targetId = score.id,
                details = "User scored ${score.score}/${score.totalQuestions} on ${score.quizTitle}"
            )
        )
        if (userRole != UserRole.GUEST) {
            runCatching {
                cloudRepository?.upsertGameScore(
                    scoreId = score.id,
                    quizId = score.quizId,
                    quizTitle = score.quizTitle,
                    userId = score.userId,
                    userName = score.userName,
                    score = score.score,
                    totalQuestions = score.totalQuestions,
                    timeTakenSeconds = score.timeTakenSeconds,
                    completedAt = score.timestamp
                )
            }
        }
        return true
    }

    override suspend fun deleteScore(scoreId: String, adminUserId: String): Boolean {
        gameDao.deleteScore(scoreId)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = adminUserId,
                userRole = UserRole.ADMINISTRATOR,
                actionType = "SCORE_MODERATED",
                targetEntity = "GameScore",
                targetId = scoreId,
                details = "Admin moderated/deleted score ID: $scoreId"
            )
        )
        runCatching {
            cloudRepository?.deleteGameScore(scoreId)
        }
        return true
    }

    override suspend fun syncCloudQuizzesToLocalDb(cloudQuizzes: List<com.example.data.remote.CloudGameQuiz>) {
        if (cloudQuizzes.isEmpty()) return
        val entities = cloudQuizzes.mapNotNull { cq ->
            if (cq.id.isBlank() || cq.title.isBlank()) return@mapNotNull null
            val category = enumValues<QuizCategory>().firstOrNull { it.name == cq.category }
                ?: QuizCategory.BIBLE_TRIVIA
            GameQuizEntity(
                id = cq.id,
                title = cq.title,
                description = cq.description.orEmpty(),
                category = category,
                questionsJson = cq.questionsJson.ifBlank { "[]" },
                totalQuestions = cq.totalQuestions.toInt().coerceAtLeast(1),
                timeLimitSeconds = cq.timeLimitSeconds.toInt().coerceAtLeast(10),
                difficulty = cq.difficulty.ifBlank { "Medium" }
            )
        }
        if (entities.isNotEmpty()) {
            gameDao.insertQuizzes(entities)
        }
    }

    override suspend fun syncCloudScoresToLocalDb(cloudScores: List<com.example.data.remote.CloudGameScore>) {
        if (cloudScores.isEmpty()) return
        for (cs in cloudScores) {
            if (cs.id.isBlank() || cs.userName.isBlank()) continue
            gameDao.insertScore(
                GameScoreEntity(
                    id = cs.id,
                    quizId = cs.quizId.ifBlank { "quiz" },
                    quizTitle = cs.quizTitle.ifBlank { "Scripture Challenge" },
                    userId = cs.userId.ifBlank { "member" },
                    userName = cs.userName,
                    score = cs.score.toInt().coerceAtLeast(0),
                    totalQuestions = cs.totalQuestions.toInt().coerceAtLeast(1),
                    timeTakenSeconds = cs.timeTakenSeconds.toInt().coerceAtLeast(0),
                    timestamp = cs.completedAt.takeIf { it > 0L }
                        ?: cs.createdAt?.toDate()?.time
                        ?: System.currentTimeMillis()
                )
            )
        }
    }

    override fun getLeaderboardForQuiz(quizId: String): Flow<List<GameScoreEntity>> =
        gameDao.getLeaderboardForQuiz(quizId)

    override fun getGlobalLeaderboard(): Flow<List<GameScoreEntity>> =
        gameDao.getGlobalLeaderboard()

    override fun getScoresForUser(userId: String): Flow<List<GameScoreEntity>> =
        gameDao.getScoresForUser(userId)
}

interface WeeklyReportRepository {
    fun getAllReports(): Flow<List<WeeklyReportEntity>>
    fun getReportsByDepartment(dept: DepartmentType): Flow<List<WeeklyReportEntity>>
    fun getReportsByUser(userId: String): Flow<List<WeeklyReportEntity>>
    fun getReportById(id: String): Flow<WeeklyReportEntity?>
    suspend fun submitReport(report: WeeklyReportEntity, actorRole: UserRole): Boolean
    suspend fun reviewReport(
        id: String,
        status: ReportStatus,
        reviewerId: String,
        reviewerRole: UserRole,
        notes: String
    ): Boolean
}

class WeeklyReportRepositoryImpl(
    private val weeklyReportDao: WeeklyReportDao,
    private val auditLogDao: AuditLogDao
) : WeeklyReportRepository {

    override fun getAllReports(): Flow<List<WeeklyReportEntity>> = weeklyReportDao.getAllReports()

    override fun getReportsByDepartment(dept: DepartmentType): Flow<List<WeeklyReportEntity>> =
        weeklyReportDao.getReportsByDepartment(dept)

    override fun getReportsByUser(userId: String): Flow<List<WeeklyReportEntity>> =
        weeklyReportDao.getReportsByUser(userId)

    override fun getReportById(id: String): Flow<WeeklyReportEntity?> =
        weeklyReportDao.getReportById(id)

    override suspend fun submitReport(report: WeeklyReportEntity, actorRole: UserRole): Boolean {
        weeklyReportDao.insertReport(report)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = report.submittedById,
                userRole = actorRole,
                actionType = "REPORT_SUBMITTED",
                targetEntity = "WeeklyReport",
                targetId = report.id,
                details = "Submitted report: ${report.title} for ${report.departmentOrUnit.displayName}"
            )
        )
        return true
    }

    override suspend fun reviewReport(
        id: String,
        status: ReportStatus,
        reviewerId: String,
        reviewerRole: UserRole,
        notes: String
    ): Boolean {
        weeklyReportDao.reviewReport(id, status, reviewerId, notes)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = reviewerId,
                userRole = reviewerRole,
                actionType = "REPORT_REVIEWED",
                targetEntity = "WeeklyReport",
                targetId = id,
                details = "Reviewed report $id with status ${status.name}"
            )
        )
        return true
    }
}
