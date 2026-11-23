package com.example.data.repository

import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.MultiplayerGameDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.MultiplayerGameSessionEntity
import com.example.data.local.entity.MultiplayerParticipantEntity
import com.example.data.local.entity.MultiplayerSubmissionEntity
import com.example.data.model.GameType
import com.example.data.model.MultiplayerLeaderboardEntry
import com.example.data.model.MultiplayerQuestion
import com.example.data.model.SessionStatus
import com.example.data.model.SessionSubstate
import com.example.data.model.SubmissionResult
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface MultiplayerGameRepository {
    fun getUpcomingAndActiveSessions(): Flow<List<MultiplayerGameSessionEntity>>
    fun getDraftSessions(): Flow<List<MultiplayerGameSessionEntity>>
    fun getCompletedSessions(): Flow<List<MultiplayerGameSessionEntity>>
    fun getSessionById(id: String): Flow<MultiplayerGameSessionEntity?>
    suspend fun getSessionByIdSync(id: String): MultiplayerGameSessionEntity?
    fun getLeaderboard(sessionId: String, currentUserId: String): Flow<List<MultiplayerLeaderboardEntry>>
    suspend fun getLeaderboardSync(sessionId: String, currentUserId: String): List<MultiplayerLeaderboardEntry>
    suspend fun getParticipant(sessionId: String, userId: String): MultiplayerParticipantEntity?
    suspend fun getSubmission(sessionId: String, questionIndex: Int, userId: String): MultiplayerSubmissionEntity?

    suspend fun createAndScheduleSession(
        title: String,
        description: String,
        gameType: GameType,
        scheduledStartTime: Long,
        timeLimitPerQuestionSeconds: Int = 20,
        leaderboardDurationSeconds: Int = 6,
        baseScorePerQuestion: Int = 100,
        speedBonusEnabled: Boolean = true,
        maxSpeedBonus: Int = 50,
        penaltyForIncorrect: Boolean = false,
        questions: List<MultiplayerQuestion>,
        actorId: String,
        actorName: String,
        actorRole: UserRole,
        existingSessionId: String? = null
    ): String

    suspend fun saveDraftSession(
        title: String,
        description: String,
        gameType: GameType,
        timeLimitPerQuestionSeconds: Int = 20,
        leaderboardDurationSeconds: Int = 6,
        baseScorePerQuestion: Int = 100,
        speedBonusEnabled: Boolean = true,
        maxSpeedBonus: Int = 50,
        penaltyForIncorrect: Boolean = false,
        questions: List<MultiplayerQuestion>,
        actorId: String,
        actorName: String,
        actorRole: UserRole,
        existingSessionId: String? = null
    ): String

    suspend fun launchDraftSession(
        sessionId: String,
        delayMinutes: Int = 0,
        currentTime: Long = System.currentTimeMillis()
    ): MultiplayerGameSessionEntity?

    suspend fun deleteDraftSession(sessionId: String, actorId: String, actorRole: UserRole)

    suspend fun joinSession(
        sessionId: String,
        userId: String,
        userName: String,
        userRole: UserRole
    ): MultiplayerParticipantEntity

    suspend fun startSessionImmediately(
        sessionId: String,
        currentTime: Long = System.currentTimeMillis()
    ): MultiplayerGameSessionEntity?

    suspend fun syncSessionState(
        sessionId: String,
        currentTime: Long = System.currentTimeMillis()
    ): MultiplayerGameSessionEntity?

    suspend fun submitAnswer(
        sessionId: String,
        questionIndex: Int,
        userId: String,
        userName: String,
        submittedAnswer: String,
        submissionTime: Long = System.currentTimeMillis()
    ): SubmissionResult

    suspend fun cancelSession(sessionId: String, actorId: String, actorRole: UserRole)

    suspend fun ensureFellowshipPeersInSession(sessionId: String) {}
    suspend fun simulatePeerAnswersForRound(sessionId: String, questionIndex: Int) {}

    suspend fun syncCloudQuizSessionsToLocalDb(
        cloudSessions: List<com.example.data.remote.CloudQuizSession>,
        gameDao: com.example.data.local.dao.GameDao? = null
    )
    suspend fun pushLocalGameSessionsToCloud()

    fun parseQuestions(questionsJson: String): List<MultiplayerQuestion>
    fun serializeQuestions(questions: List<MultiplayerQuestion>): String
}

class MultiplayerGameRepositoryImpl(
    private val multiplayerGameDao: MultiplayerGameDao,
    private val auditLogDao: AuditLogDao,
    private val cloudRepository: GideonitesCloudRepository? = null,
    private val notificationService: com.example.core.notification.FellowshipNotificationService? = null
) : MultiplayerGameRepository {

    private val previousRanksBySession = java.util.concurrent.ConcurrentHashMap<String, Map<String, Int>>()
    private val simulatedRoundsBySession = java.util.concurrent.ConcurrentHashMap<String, MutableSet<Int>>()

    override fun getUpcomingAndActiveSessions(): Flow<List<MultiplayerGameSessionEntity>> =
        multiplayerGameDao.getUpcomingAndActiveSessions()

    override fun getDraftSessions(): Flow<List<MultiplayerGameSessionEntity>> =
        multiplayerGameDao.getDraftSessions()

    override fun getCompletedSessions(): Flow<List<MultiplayerGameSessionEntity>> =
        multiplayerGameDao.getCompletedSessions()

    override fun getSessionById(id: String): Flow<MultiplayerGameSessionEntity?> =
        multiplayerGameDao.getSessionById(id)

    override suspend fun getSessionByIdSync(id: String): MultiplayerGameSessionEntity? =
        multiplayerGameDao.getSessionByIdSync(id)

    override fun getLeaderboard(sessionId: String, currentUserId: String): Flow<List<MultiplayerLeaderboardEntry>> =
        multiplayerGameDao.getLeaderboardForSession(sessionId).map { participants ->
            mapParticipantsToLeaderboard(participants, currentUserId)
        }

    override suspend fun getLeaderboardSync(sessionId: String, currentUserId: String): List<MultiplayerLeaderboardEntry> {
        val participants = multiplayerGameDao.getLeaderboardForSessionSync(sessionId)
        return mapParticipantsToLeaderboard(participants, currentUserId)
    }

    override suspend fun getParticipant(sessionId: String, userId: String): MultiplayerParticipantEntity? =
        multiplayerGameDao.getParticipant(sessionId, userId)

    override suspend fun getSubmission(sessionId: String, questionIndex: Int, userId: String): MultiplayerSubmissionEntity? =
        multiplayerGameDao.getSubmission(sessionId, questionIndex, userId)

    private suspend fun snapshotRanksForSession(sessionId: String) {
        val participants = multiplayerGameDao.getLeaderboardForSessionSync(sessionId)
        val sorted = participants.sortedWith(
            compareByDescending<MultiplayerParticipantEntity> { it.currentScore }
                .thenBy { if (it.lastAnswerTimestamp > 0L) it.lastAnswerTimestamp else Long.MAX_VALUE }
                .thenBy { it.userName }
        )
        previousRanksBySession[sessionId] = sorted.mapIndexed { idx, p -> p.userId to (idx + 1) }.toMap()
    }

    private fun serializeParticipants(participants: List<MultiplayerParticipantEntity>): String {
        val arr = JSONArray()
        for (p in participants) {
            if (p.userId.startsWith("peer-")) continue
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("userId", p.userId)
            obj.put("userName", p.userName)
            obj.put("userRole", p.userRole)
            obj.put("joinedAt", p.joinedAt)
            obj.put("joinedAtQuestionIndex", p.joinedAtQuestionIndex)
            obj.put("currentScore", p.currentScore)
            obj.put("lastQuestionPoints", p.lastQuestionPoints)
            obj.put("lastAnswerTimestamp", p.lastAnswerTimestamp)
            obj.put("answeredQuestionsCount", p.answeredQuestionsCount)
            arr.put(obj)
        }
        return arr.toString()
    }

    private suspend fun mergeCloudParticipants(sessionId: String, participantsJson: String?) {
        multiplayerGameDao.deleteSimulatedPeers()
        val cloudScoresByUser = mutableMapOf<String, Int>()
        if (!participantsJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(participantsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val userId = obj.optString("userId", "").trim()
                    if (userId.isBlank() || userId.startsWith("peer-")) continue
                    val existing = multiplayerGameDao.getParticipant(sessionId, userId)
                    val cloudScore = obj.optInt("currentScore", 0)
                    val cloudLastPoints = obj.optInt("lastQuestionPoints", 0)
                    val cloudLastTs = obj.optLong("lastAnswerTimestamp", 0L)
                    val cloudAnswered = obj.optInt("answeredQuestionsCount", 0)
                    val cloudName = obj.optString("userName", "Youth Member")
                    val cloudRole = obj.optString("userRole", "Youth Member")
                    cloudScoresByUser[userId] = cloudScore

                    if (existing == null) {
                        multiplayerGameDao.insertParticipant(
                            MultiplayerParticipantEntity(
                                id = obj.optString("id", "part_${sessionId}_$userId"),
                                sessionId = sessionId,
                                userId = userId,
                                userName = cloudName,
                                userRole = cloudRole,
                                joinedAt = obj.optLong("joinedAt", System.currentTimeMillis()),
                                joinedAtQuestionIndex = obj.optInt("joinedAtQuestionIndex", 0),
                                currentScore = cloudScore,
                                lastQuestionPoints = cloudLastPoints,
                                lastAnswerTimestamp = cloudLastTs,
                                answeredQuestionsCount = cloudAnswered,
                                isOnline = true
                            )
                        )
                    } else if (
                        cloudScore > existing.currentScore ||
                        cloudAnswered > existing.answeredQuestionsCount ||
                        (cloudScore == existing.currentScore && cloudLastPoints != existing.lastQuestionPoints) ||
                        existing.userName != cloudName
                    ) {
                        multiplayerGameDao.updateParticipant(
                            existing.copy(
                                userName = cloudName.ifBlank { existing.userName },
                                userRole = cloudRole.ifBlank { existing.userRole },
                                currentScore = maxOf(existing.currentScore, cloudScore),
                                lastQuestionPoints = if (cloudScore > existing.currentScore || cloudAnswered >= existing.answeredQuestionsCount) cloudLastPoints else existing.lastQuestionPoints,
                                lastAnswerTimestamp = maxOf(existing.lastAnswerTimestamp, cloudLastTs),
                                answeredQuestionsCount = maxOf(existing.answeredQuestionsCount, cloudAnswered),
                                isOnline = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore malformed JSON
            }
        }

        // If any local real participant is missing from the cloud or has a higher local score than the cloud snapshot, push to cloud so all devices converge
        val localParticipants = multiplayerGameDao.getLeaderboardForSessionSync(sessionId)
        val needsCloudPush = localParticipants.any { local ->
            !local.userId.startsWith("peer-") && local.currentScore > (cloudScoresByUser[local.userId] ?: -1)
        }
        if (needsCloudPush) {
            pushSessionParticipantsToCloud(sessionId)
        }
    }

    private fun isCurrentDeviceHost(session: MultiplayerGameSessionEntity): Boolean {
        val authUid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
        val localUserId = cloudRepository?.activeSessionUserId
        return (authUid == null && localUserId == null) ||
            (authUid != null && session.createdById == authUid) ||
            (localUserId != null && session.createdById == localUserId)
    }

    private suspend fun pushSessionParticipantsToCloud(sessionId: String) {
        val cloudRepo = cloudRepository ?: return
        try {
            val realParticipants = multiplayerGameDao.getLeaderboardForSessionSync(sessionId)
                .filter { !it.userId.startsWith("peer-") }
            val json = serializeParticipants(realParticipants)
            cloudRepo.updateCloudQuizSessionState(
                sessionId = sessionId,
                participantCount = realParticipants.size.coerceAtLeast(1).toLong(),
                participantsJson = json
            )
        } catch (e: Exception) {
            // Ignore when offline
        }
    }

    private suspend fun pushSessionTimingStateToCloud(
        session: MultiplayerGameSessionEntity,
        includeParticipants: Boolean = false,
        forceCloudPush: Boolean = false
    ) {
        val cloudRepo = cloudRepository ?: return
        if (!forceCloudPush && !isCurrentDeviceHost(session)) return
        try {
            val realParticipants = if (includeParticipants) {
                multiplayerGameDao.getLeaderboardForSessionSync(session.id)
                    .filter { !it.userId.startsWith("peer-") }
            } else {
                emptyList()
            }
            cloudRepo.updateCloudQuizSessionState(
                sessionId = session.id,
                status = session.status.name,
                currentQuestionIndex = session.currentQuestionIndex.toLong(),
                currentSubstate = session.currentSubstate.name,
                currentQuestionDeadline = session.currentQuestionDeadline,
                leaderboardDeadline = session.leaderboardDeadline,
                participantCount = if (includeParticipants) realParticipants.size.coerceAtLeast(1).toLong() else null,
                participantsJson = if (includeParticipants && realParticipants.isNotEmpty()) serializeParticipants(realParticipants) else null,
                scheduledStartTime = session.scheduledStartTime
            )
        } catch (e: Exception) {
            // Ignore when offline
        }
    }

    override suspend fun pushLocalGameSessionsToCloud() {
        val cloudRepo = cloudRepository ?: return
        try {
            val sessionsToSync = multiplayerGameDao.getUpcomingAndActiveSessions().firstOrNull().orEmpty() +
                multiplayerGameDao.getDraftSessions().firstOrNull().orEmpty()
            for (s in sessionsToSync) {
                try {
                    val participants = multiplayerGameDao.getLeaderboardForSessionSync(s.id)
                    val resolvedPin = s.roomPin.ifBlank {
                        s.id.removePrefix("quiz_").replace("-", "").uppercase().take(6).padEnd(4, '0')
                    }
                    cloudRepo.createCloudQuizSession(
                        sessionId = s.id,
                        hostId = s.createdById,
                        hostName = s.createdByName,
                        roomPin = resolvedPin,
                        titleEn = s.title,
                        description = s.description,
                        gameType = s.gameType.name,
                        status = s.status.name,
                        scheduledStartTime = s.scheduledStartTime,
                        timeLimitPerQuestionSeconds = s.timeLimitPerQuestionSeconds.toLong(),
                        leaderboardDurationSeconds = s.leaderboardDurationSeconds.toLong(),
                        currentQuestionIndex = s.currentQuestionIndex.toLong(),
                        currentSubstate = s.currentSubstate.name,
                        currentQuestionDeadline = s.currentQuestionDeadline,
                        leaderboardDeadline = s.leaderboardDeadline,
                        baseScorePerQuestion = s.baseScorePerQuestion.toLong(),
                        speedBonusEnabled = s.speedBonusEnabled,
                        maxSpeedBonus = s.maxSpeedBonus.toLong(),
                        penaltyForIncorrect = s.penaltyForIncorrect,
                        totalQuestions = s.totalQuestions.coerceAtLeast(1),
                        questionsJson = s.questionsJson,
                        participantsJson = if (participants.isNotEmpty()) serializeParticipants(participants) else null
                    )
                } catch (e: Exception) {
                    // Continue syncing next session
                }
            }
        } catch (e: Exception) {
            // Ignore when offline
        }
    }

    private fun mapParticipantsToLeaderboard(
        participants: List<MultiplayerParticipantEntity>,
        currentUserId: String
    ): List<MultiplayerLeaderboardEntry> {
        val sessionId = participants.firstOrNull()?.sessionId ?: ""
        val prevRanks = previousRanksBySession[sessionId] ?: emptyMap()

        // Deterministic sorting rule:
        // 1. Highest score descending
        // 2. Earliest lastAnswerTimestamp ascending (faster overall speed breaks ties)
        // 3. User name alphabetical ascending
        val sorted = participants.sortedWith(
            compareByDescending<MultiplayerParticipantEntity> { it.currentScore }
                .thenBy { if (it.lastAnswerTimestamp > 0L) it.lastAnswerTimestamp else Long.MAX_VALUE }
                .thenBy { it.userName }
        )

        return sorted.mapIndexed { index, p ->
            val newRank = index + 1
            val prevRank = prevRanks[p.userId] ?: newRank
            val movement = prevRank - newRank
            MultiplayerLeaderboardEntry(
                rank = newRank,
                userId = p.userId,
                userName = p.userName,
                userRole = p.userRole,
                score = p.currentScore,
                pointsFromLastQuestion = p.lastQuestionPoints,
                rankMovement = movement,
                isCurrentUser = p.userId == currentUserId,
                answeredCount = p.answeredQuestionsCount
            )
        }
    }

    override suspend fun createAndScheduleSession(
        title: String,
        description: String,
        gameType: GameType,
        scheduledStartTime: Long,
        timeLimitPerQuestionSeconds: Int,
        leaderboardDurationSeconds: Int,
        baseScorePerQuestion: Int,
        speedBonusEnabled: Boolean,
        maxSpeedBonus: Int,
        penaltyForIncorrect: Boolean,
        questions: List<MultiplayerQuestion>,
        actorId: String,
        actorName: String,
        actorRole: UserRole,
        existingSessionId: String?
    ): String {
        val cleanUuid = UUID.randomUUID().toString().replace("-", "")
        val existing = existingSessionId?.let { multiplayerGameDao.getSessionByIdSync(it) }
        val sessionId = existing?.id ?: existingSessionId?.takeIf { it.isNotBlank() } ?: "quiz_${cleanUuid.take(24)}"
        val roomPin = existing?.roomPin?.takeIf { it.isNotBlank() } ?: cleanUuid.take(6).uppercase()
        val questionsJson = serializeQuestions(questions)
        val session = MultiplayerGameSessionEntity(
            id = sessionId,
            title = title,
            description = description,
            gameType = gameType,
            scheduledStartTime = scheduledStartTime,
            status = SessionStatus.SCHEDULED,
            currentQuestionIndex = 0,
            currentSubstate = SessionSubstate.WAITING_TO_START,
            currentQuestionDeadline = 0L,
            leaderboardDeadline = 0L,
            timeLimitPerQuestionSeconds = timeLimitPerQuestionSeconds,
            leaderboardDurationSeconds = leaderboardDurationSeconds,
            baseScorePerQuestion = baseScorePerQuestion,
            speedBonusEnabled = speedBonusEnabled,
            maxSpeedBonus = maxSpeedBonus,
            penaltyForIncorrect = penaltyForIncorrect,
            questionsJson = questionsJson,
            totalQuestions = questions.size,
            createdById = existing?.createdById?.takeIf { it.isNotBlank() } ?: actorId,
            createdByName = existing?.createdByName?.takeIf { it.isNotBlank() } ?: actorName,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            roomPin = roomPin
        )
        multiplayerGameDao.insertSession(session)

        notificationService?.postNotification(
            id = "notif_game_$sessionId",
            title = "🎮 Live Fellowship Game: $title",
            message = "${gameType.displayName} hosted by $actorName • ${questions.size} rounds.",
            type = com.example.data.local.entity.NotificationType.GAME_TIME,
            authorName = actorName,
            authorRole = actorRole.displayName,
            isUrgent = true
        )

        try {
            cloudRepository?.createCloudQuizSession(
                sessionId = sessionId,
                hostId = session.createdById,
                hostName = session.createdByName,
                roomPin = roomPin,
                titleEn = title,
                description = description,
                gameType = gameType.name,
                status = SessionStatus.SCHEDULED.name,
                scheduledStartTime = scheduledStartTime,
                timeLimitPerQuestionSeconds = timeLimitPerQuestionSeconds.toLong(),
                leaderboardDurationSeconds = leaderboardDurationSeconds.toLong(),
                currentQuestionIndex = 0L,
                currentSubstate = SessionSubstate.WAITING_TO_START.name,
                currentQuestionDeadline = 0L,
                leaderboardDeadline = 0L,
                baseScorePerQuestion = baseScorePerQuestion.toLong(),
                speedBonusEnabled = speedBonusEnabled,
                maxSpeedBonus = maxSpeedBonus.toLong(),
                penaltyForIncorrect = penaltyForIncorrect,
                totalQuestions = questions.size.coerceAtLeast(1),
                questionsJson = questionsJson
            )
        } catch (e: Exception) {
            // Local Room write already succeeded
        }

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = actorId,
                userRole = actorRole,
                actionType = "MULTIPLAYER_GAME_SCHEDULED",
                targetEntity = "MultiplayerGameSession",
                targetId = sessionId,
                details = "Scheduled ${gameType.displayName}: '$title' with ${questions.size} questions."
            )
        )
        return sessionId
    }

    override suspend fun saveDraftSession(
        title: String,
        description: String,
        gameType: GameType,
        timeLimitPerQuestionSeconds: Int,
        leaderboardDurationSeconds: Int,
        baseScorePerQuestion: Int,
        speedBonusEnabled: Boolean,
        maxSpeedBonus: Int,
        penaltyForIncorrect: Boolean,
        questions: List<MultiplayerQuestion>,
        actorId: String,
        actorName: String,
        actorRole: UserRole,
        existingSessionId: String?
    ): String {
        val cleanUuid = UUID.randomUUID().toString().replace("-", "")
        val existing = existingSessionId?.let { multiplayerGameDao.getSessionByIdSync(it) }
        val sessionId = existing?.id ?: existingSessionId?.takeIf { it.isNotBlank() } ?: "quiz_${cleanUuid.take(24)}"
        val roomPin = existing?.roomPin?.takeIf { it.isNotBlank() } ?: cleanUuid.take(6).uppercase()
        val questionsJson = serializeQuestions(questions)
        val draftSession = MultiplayerGameSessionEntity(
            id = sessionId,
            title = title,
            description = description,
            gameType = gameType,
            scheduledStartTime = 0L,
            status = SessionStatus.DRAFT,
            currentQuestionIndex = 0,
            currentSubstate = SessionSubstate.WAITING_TO_START,
            currentQuestionDeadline = 0L,
            leaderboardDeadline = 0L,
            timeLimitPerQuestionSeconds = timeLimitPerQuestionSeconds,
            leaderboardDurationSeconds = leaderboardDurationSeconds,
            baseScorePerQuestion = baseScorePerQuestion,
            speedBonusEnabled = speedBonusEnabled,
            maxSpeedBonus = maxSpeedBonus,
            penaltyForIncorrect = penaltyForIncorrect,
            questionsJson = questionsJson,
            totalQuestions = questions.size,
            createdById = existing?.createdById?.takeIf { it.isNotBlank() } ?: actorId,
            createdByName = existing?.createdByName?.takeIf { it.isNotBlank() } ?: actorName,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            roomPin = roomPin
        )
        multiplayerGameDao.insertSession(draftSession)

        try {
            cloudRepository?.createCloudQuizSession(
                sessionId = sessionId,
                hostId = draftSession.createdById,
                hostName = draftSession.createdByName,
                roomPin = roomPin,
                titleEn = title,
                description = description,
                gameType = gameType.name,
                status = SessionStatus.DRAFT.name,
                scheduledStartTime = 0L,
                timeLimitPerQuestionSeconds = timeLimitPerQuestionSeconds.toLong(),
                leaderboardDurationSeconds = leaderboardDurationSeconds.toLong(),
                currentQuestionIndex = 0L,
                currentSubstate = SessionSubstate.WAITING_TO_START.name,
                currentQuestionDeadline = 0L,
                leaderboardDeadline = 0L,
                baseScorePerQuestion = baseScorePerQuestion.toLong(),
                speedBonusEnabled = speedBonusEnabled,
                maxSpeedBonus = maxSpeedBonus.toLong(),
                penaltyForIncorrect = penaltyForIncorrect,
                totalQuestions = questions.size.coerceAtLeast(1),
                questionsJson = questionsJson
            )
        } catch (e: Exception) {
            // Local Room draft write already succeeded
        }

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = actorId,
                userRole = actorRole,
                actionType = "MULTIPLAYER_GAME_DRAFT_SAVED",
                targetEntity = "MultiplayerGameSession",
                targetId = sessionId,
                details = "Saved draft ${gameType.displayName}: '$title' with ${questions.size} questions."
            )
        )
        return sessionId
    }

    override suspend fun launchDraftSession(
        sessionId: String,
        delayMinutes: Int,
        currentTime: Long
    ): MultiplayerGameSessionEntity? {
        val draft = multiplayerGameDao.getSessionByIdSync(sessionId) ?: return null
        if (delayMinutes <= 0) {
            return startSessionImmediately(sessionId, currentTime)
        }
        val scheduledStartTime = currentTime + (delayMinutes * 60 * 1000L)
        val scheduled = draft.copy(
            scheduledStartTime = scheduledStartTime,
            status = SessionStatus.SCHEDULED,
            currentQuestionIndex = 0,
            currentSubstate = SessionSubstate.WAITING_TO_START,
            currentQuestionDeadline = 0L,
            leaderboardDeadline = 0L
        )
        multiplayerGameDao.updateSession(scheduled)
        pushSessionTimingStateToCloud(scheduled, includeParticipants = false, forceCloudPush = true)
        notificationService?.postNotification(
            id = "notif_game_$sessionId",
            title = "🎮 Live Fellowship Game: ${scheduled.title}",
            message = "${scheduled.gameType.displayName} hosted by ${scheduled.createdByName} • ${scheduled.totalQuestions} rounds.",
            type = com.example.data.local.entity.NotificationType.GAME_TIME,
            authorName = scheduled.createdByName,
            authorRole = "Game Host",
            isUrgent = true
        )
        return scheduled
    }

    override suspend fun deleteDraftSession(sessionId: String, actorId: String, actorRole: UserRole) {
        multiplayerGameDao.deleteSession(sessionId)
        runCatching {
            cloudRepository?.deleteCloudQuizSession(sessionId)
        }
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = actorId,
                userRole = actorRole,
                actionType = "MULTIPLAYER_GAME_DRAFT_DELETED",
                targetEntity = "MultiplayerGameSession",
                targetId = sessionId,
                details = "Executive deleted saved game draft $sessionId"
            )
        )
    }

    override suspend fun syncCloudQuizSessionsToLocalDb(
        cloudSessions: List<com.example.data.remote.CloudQuizSession>,
        gameDao: com.example.data.local.dao.GameDao?
    ) {
        val defaultQuestions = listOf(
            MultiplayerQuestion(
                id = 1,
                gameType = GameType.TRIVIA_CHALLENGE,
                prompt = "Who did the Angel of the Lord call a 'mighty man of valour' in Judges 6:12?",
                options = listOf("Gideon", "Samson", "Barak", "Jephthah"),
                correctAnswer = "Gideon",
                scriptureReference = "Judges 6:12"
            ),
            MultiplayerQuestion(
                id = 2,
                gameType = GameType.TRIVIA_CHALLENGE,
                prompt = "How many chosen men did Gideon lead to victory with trumpets and pitchers?",
                options = listOf("300", "450", "1,000", "10,000"),
                correctAnswer = "300",
                scriptureReference = "Judges 7:7"
            ),
            MultiplayerQuestion(
                id = 3,
                gameType = GameType.TRIVIA_CHALLENGE,
                prompt = "Let no man despise thy youth; but be thou an example of the believers (1 Timothy 4:12). Who wrote this charge?",
                options = listOf("Apostle Paul", "Apostle Peter", "Apostle John", "James"),
                correctAnswer = "Apostle Paul",
                scriptureReference = "1 Timothy 4:12"
            )
        )
        val defaultJson = serializeQuestions(defaultQuestions)

        for (cs in cloudSessions) {
            if (cs.id.isBlank()) continue
            val rawId = cs.id.removePrefix("quiz_")
            val existingByRaw = multiplayerGameDao.getSessionByIdSync(rawId)
            val existingByCloud = multiplayerGameDao.getSessionByIdSync(cs.id)
            val existing = existingByRaw ?: existingByCloud
            val targetId = existing?.id ?: cs.id

            val resolvedType = try {
                GameType.valueOf(cs.gameType)
            } catch (e: Exception) {
                GameType.TRIVIA_CHALLENGE
            }
            val resolvedStatus = try {
                SessionStatus.valueOf(cs.status)
            } catch (e: Exception) {
                SessionStatus.SCHEDULED
            }
            val cloudSubstate = cs.currentSubstate?.let { rawSub ->
                enumValues<SessionSubstate>().firstOrNull { it.name == rawSub }
            }
            val createdAtMillis = existing?.createdAt ?: (cs.createdAt?.toDate()?.time ?: System.currentTimeMillis())

            val mergedStatus = when {
                resolvedStatus == SessionStatus.COMPLETED || resolvedStatus == SessionStatus.CANCELLED -> resolvedStatus
                existing?.status == SessionStatus.COMPLETED || existing?.status == SessionStatus.CANCELLED -> existing.status
                resolvedStatus == SessionStatus.ACTIVE || existing?.status == SessionStatus.ACTIVE -> SessionStatus.ACTIVE
                else -> resolvedStatus
            }

            fun substateRank(sub: SessionSubstate): Int = when (sub) {
                SessionSubstate.WAITING_TO_START -> 0
                SessionSubstate.QUESTION_OPEN -> 1
                SessionSubstate.LEADERBOARD -> 2
            }

            val cloudIdx = cs.currentQuestionIndex.toInt().coerceAtLeast(0)
            val localIdx = existing?.currentQuestionIndex ?: 0
            val mergedQuestionIndex = maxOf(cloudIdx, localIdx)

            val (mergedSubstate, mergedQDeadline, mergedLbDeadline) = when {
                mergedStatus == SessionStatus.COMPLETED -> Triple(
                    SessionSubstate.LEADERBOARD,
                    existing?.currentQuestionDeadline ?: (cs.currentQuestionDeadline ?: 0L),
                    existing?.leaderboardDeadline ?: (cs.leaderboardDeadline ?: 0L)
                )
                existing == null -> Triple(
                    cloudSubstate ?: if (mergedStatus == SessionStatus.ACTIVE) SessionSubstate.QUESTION_OPEN else SessionSubstate.WAITING_TO_START,
                    cs.currentQuestionDeadline ?: 0L,
                    cs.leaderboardDeadline ?: 0L
                )
                cloudIdx > localIdx -> Triple(
                    cloudSubstate ?: SessionSubstate.QUESTION_OPEN,
                    cs.currentQuestionDeadline ?: 0L,
                    cs.leaderboardDeadline ?: 0L
                )
                cloudIdx == localIdx && cloudSubstate != null && substateRank(cloudSubstate) > substateRank(existing.currentSubstate) -> Triple(
                    cloudSubstate,
                    cs.currentQuestionDeadline?.takeIf { it > 0L } ?: existing.currentQuestionDeadline,
                    cs.leaderboardDeadline?.takeIf { it > 0L } ?: existing.leaderboardDeadline
                )
                cloudIdx == localIdx && (cloudSubstate == null || cloudSubstate == existing.currentSubstate) -> Triple(
                    existing.currentSubstate,
                    if (existing.currentQuestionDeadline <= 0L) (cs.currentQuestionDeadline ?: 0L)
                    else maxOf(existing.currentQuestionDeadline, cs.currentQuestionDeadline ?: 0L),
                    if (existing.leaderboardDeadline <= 0L) (cs.leaderboardDeadline ?: 0L)
                    else maxOf(existing.leaderboardDeadline, cs.leaderboardDeadline ?: 0L)
                )
                else -> Triple(
                    existing.currentSubstate,
                    existing.currentQuestionDeadline,
                    existing.leaderboardDeadline
                )
            }

            val resolvedPin = cs.roomPin.ifBlank {
                existing?.roomPin?.ifBlank { null }
                    ?: targetId.removePrefix("quiz_").replace("-", "").uppercase().take(6).padEnd(4, '0')
            }

            val syncedSession = MultiplayerGameSessionEntity(
                id = targetId,
                title = cs.titleEn.ifBlank { existing?.title ?: "Live Scripture Showdown" },
                description = cs.description?.takeIf { it.isNotBlank() }
                    ?: existing?.description
                    ?: "Room PIN: $resolvedPin • Hosted by ${cs.hostName}",
                gameType = resolvedType,
                scheduledStartTime = if (mergedStatus == SessionStatus.DRAFT) {
                    cs.scheduledStartTime?.coerceAtLeast(0L) ?: existing?.scheduledStartTime ?: 0L
                } else {
                    cs.scheduledStartTime?.takeIf { it > 0L }
                        ?: existing?.scheduledStartTime?.takeIf { it > 0L }
                        ?: createdAtMillis
                },
                actualStartTime = existing?.actualStartTime
                    ?: if (mergedStatus == SessionStatus.ACTIVE || mergedStatus == SessionStatus.COMPLETED) (cs.scheduledStartTime ?: createdAtMillis) else null,
                actualEndTime = existing?.actualEndTime
                    ?: if (mergedStatus == SessionStatus.COMPLETED) System.currentTimeMillis() else null,
                status = mergedStatus,
                currentQuestionIndex = mergedQuestionIndex,
                currentSubstate = mergedSubstate,
                currentQuestionDeadline = mergedQDeadline,
                leaderboardDeadline = mergedLbDeadline,
                timeLimitPerQuestionSeconds = cs.timeLimitPerQuestionSeconds?.toInt()
                    ?: existing?.timeLimitPerQuestionSeconds
                    ?: 20,
                leaderboardDurationSeconds = cs.leaderboardDurationSeconds?.toInt()
                    ?: existing?.leaderboardDurationSeconds
                    ?: 6,
                baseScorePerQuestion = cs.baseScorePerQuestion?.toInt()
                    ?: existing?.baseScorePerQuestion
                    ?: 100,
                speedBonusEnabled = cs.speedBonusEnabled
                    ?: existing?.speedBonusEnabled
                    ?: true,
                maxSpeedBonus = cs.maxSpeedBonus?.toInt()
                    ?: existing?.maxSpeedBonus
                    ?: 50,
                penaltyForIncorrect = cs.penaltyForIncorrect
                    ?: existing?.penaltyForIncorrect
                    ?: false,
                questionsJson = cs.questionsJson?.takeIf { it.isNotBlank() }
                    ?: existing?.questionsJson?.takeIf { it.isNotBlank() }
                    ?: defaultJson,
                totalQuestions = if (mergedStatus == SessionStatus.DRAFT) {
                    cs.totalQuestions.toInt().coerceAtLeast(1)
                } else {
                    maxOf(cs.totalQuestions.toInt(), existing?.totalQuestions ?: defaultQuestions.size)
                },
                createdById = existing?.createdById?.takeIf { it.isNotBlank() } ?: cs.hostId.ifBlank { "admin-1" },
                createdByName = cs.hostName.ifBlank { existing?.createdByName ?: "Youth Leadership" },
                createdAt = createdAtMillis,
                roomPin = resolvedPin
            )
            multiplayerGameDao.insertSession(syncedSession)
            mergeCloudParticipants(targetId, cs.participantsJson)
        }
    }

    override suspend fun joinSession(
        sessionId: String,
        userId: String,
        userName: String,
        userRole: UserRole
    ): MultiplayerParticipantEntity {
        // Reconnection check: restore existing participant without duplicate!
        val existing = multiplayerGameDao.getParticipant(sessionId, userId)
        if (existing != null) {
            val updated = existing.copy(isOnline = true)
            multiplayerGameDao.updateParticipant(updated)
            pushSessionParticipantsToCloud(sessionId)
            return updated
        }

        // Retrieve session to determine starting question index (Late Entry Rule)
        val session = multiplayerGameDao.getSessionByIdSync(sessionId)
        val initialQuestionIndex = if (session != null && session.status == SessionStatus.ACTIVE) {
            session.currentQuestionIndex
        } else {
            0
        }

        val newParticipant = MultiplayerParticipantEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            userId = userId,
            userName = userName,
            userRole = userRole.displayName,
            joinedAt = System.currentTimeMillis(),
            joinedAtQuestionIndex = initialQuestionIndex,
            currentScore = 0, // Late participants start with 0 points for missed questions!
            lastQuestionPoints = 0,
            lastAnswerTimestamp = 0L,
            answeredQuestionsCount = 0,
            isOnline = true
        )
        multiplayerGameDao.insertParticipant(newParticipant)
        pushSessionParticipantsToCloud(sessionId)
        return newParticipant
    }

    override suspend fun ensureFellowshipPeersInSession(sessionId: String) {
        multiplayerGameDao.deleteSimulatedPeers()
        if (!previousRanksBySession.containsKey(sessionId)) {
            snapshotRanksForSession(sessionId)
        }
    }

    override suspend fun simulatePeerAnswersForRound(sessionId: String, questionIndex: Int) {
        // Real connected participants only — no fake simulated peers
        multiplayerGameDao.deleteSimulatedPeers()
    }

    override suspend fun startSessionImmediately(
        sessionId: String,
        currentTime: Long
    ): MultiplayerGameSessionEntity? {
        val session = multiplayerGameDao.getSessionByIdSync(sessionId) ?: return null
        snapshotRanksForSession(sessionId)
        val updated = session.copy(
            scheduledStartTime = currentTime,
            status = SessionStatus.ACTIVE,
            actualStartTime = currentTime,
            currentQuestionIndex = 0,
            currentSubstate = SessionSubstate.QUESTION_OPEN,
            currentQuestionDeadline = currentTime + (session.timeLimitPerQuestionSeconds * 1000L),
            leaderboardDeadline = 0L
        )
        multiplayerGameDao.updateSession(updated)
        pushSessionTimingStateToCloud(updated, includeParticipants = true, forceCloudPush = true)
        notificationService?.postNotification(
            id = "notif_game_live_${sessionId}_$currentTime",
            title = "🔥 LIVE GAME STARTED: ${session.title}",
            message = "${session.createdByName} launched ${session.gameType.displayName}! Enter the arena now!",
            type = com.example.data.local.entity.NotificationType.GAME_TIME,
            authorName = session.createdByName,
            authorRole = "Game Host",
            isUrgent = true
        )
        return updated
    }

    override suspend fun syncSessionState(
        sessionId: String,
        currentTime: Long
    ): MultiplayerGameSessionEntity? {
        val session = multiplayerGameDao.getSessionByIdSync(sessionId) ?: return null
        val isHost = isCurrentDeviceHost(session)
        // Non-host devices wait a brief grace window so the host's authoritative cloud state arrives first
        val nonHostGraceMs = if (!isHost && cloudRepository != null) 1500L else 0L

        when (session.status) {
            SessionStatus.SCHEDULED -> {
                // Check if scheduled start time reached: automatic start
                if (currentTime >= session.scheduledStartTime + nonHostGraceMs) {
                    snapshotRanksForSession(sessionId)
                    val updated = session.copy(
                        status = SessionStatus.ACTIVE,
                        actualStartTime = currentTime,
                        currentQuestionIndex = 0,
                        currentSubstate = SessionSubstate.QUESTION_OPEN,
                        currentQuestionDeadline = currentTime + (session.timeLimitPerQuestionSeconds * 1000L),
                        leaderboardDeadline = 0L
                    )
                    multiplayerGameDao.updateSession(updated)
                    pushSessionTimingStateToCloud(updated)
                    return updated
                }
            }
            SessionStatus.ACTIVE -> {
                when (session.currentSubstate) {
                    SessionSubstate.WAITING_TO_START -> {
                        // Immediately transition to question 0 if deadline is unset
                        snapshotRanksForSession(sessionId)
                        val deadline = if (session.currentQuestionDeadline > 0L) {
                            session.currentQuestionDeadline
                        } else {
                            currentTime + (session.timeLimitPerQuestionSeconds * 1000L)
                        }
                        val updated = session.copy(
                            currentSubstate = SessionSubstate.QUESTION_OPEN,
                            currentQuestionDeadline = deadline,
                            leaderboardDeadline = 0L
                        )
                        multiplayerGameDao.updateSession(updated)
                        pushSessionTimingStateToCloud(updated)
                        return updated
                    }
                    SessionSubstate.QUESTION_OPEN -> {
                        if (session.currentQuestionDeadline <= 0L) {
                            val initialized = session.copy(
                                currentQuestionDeadline = currentTime + (session.timeLimitPerQuestionSeconds * 1000L)
                            )
                            multiplayerGameDao.updateSession(initialized)
                            pushSessionTimingStateToCloud(initialized)
                            return initialized
                        }
                        // Check if question deadline has elapsed
                        if (currentTime >= session.currentQuestionDeadline + nonHostGraceMs) {
                            val updated = session.copy(
                                currentSubstate = SessionSubstate.LEADERBOARD,
                                leaderboardDeadline = currentTime + (session.leaderboardDurationSeconds * 1000L)
                            )
                            multiplayerGameDao.updateSession(updated)
                            pushSessionTimingStateToCloud(updated)
                            return updated
                        }
                    }
                    SessionSubstate.LEADERBOARD -> {
                        if (session.leaderboardDeadline <= 0L) {
                            val initialized = session.copy(
                                leaderboardDeadline = currentTime + (session.leaderboardDurationSeconds * 1000L)
                            )
                            multiplayerGameDao.updateSession(initialized)
                            pushSessionTimingStateToCloud(initialized)
                            return initialized
                        }
                        // Check if leaderboard display duration has elapsed
                        if (currentTime >= session.leaderboardDeadline + nonHostGraceMs) {
                            val nextIndex = session.currentQuestionIndex + 1
                            if (nextIndex < session.totalQuestions) {
                                // Snapshot current leaderboard ranks before opening next question
                                snapshotRanksForSession(sessionId)
                                // Advance to next synchronized question
                                val updated = session.copy(
                                    currentQuestionIndex = nextIndex,
                                    currentSubstate = SessionSubstate.QUESTION_OPEN,
                                    currentQuestionDeadline = currentTime + (session.timeLimitPerQuestionSeconds * 1000L),
                                    leaderboardDeadline = 0L
                                )
                                multiplayerGameDao.updateSession(updated)
                                pushSessionTimingStateToCloud(updated)
                                return updated
                            } else {
                                // All questions completed
                                val updated = session.copy(
                                    status = SessionStatus.COMPLETED,
                                    actualEndTime = currentTime,
                                    currentSubstate = SessionSubstate.LEADERBOARD
                                )
                                multiplayerGameDao.updateSession(updated)
                                pushSessionTimingStateToCloud(updated, includeParticipants = true)
                                return updated
                            }
                        }
                    }
                }
            }
            SessionStatus.COMPLETED, SessionStatus.CANCELLED, SessionStatus.DRAFT -> {
                // Terminal or non-running states
            }
        }
        return session
    }

    override suspend fun submitAnswer(
        sessionId: String,
        questionIndex: Int,
        userId: String,
        userName: String,
        submittedAnswer: String,
        submissionTime: Long
    ): SubmissionResult {
        val session = multiplayerGameDao.getSessionByIdSync(sessionId)
            ?: return SubmissionResult.Error("Game session not found.")

        if (session.status != SessionStatus.ACTIVE || session.currentSubstate != SessionSubstate.QUESTION_OPEN) {
            return SubmissionResult.Error("Question is currently closed for submissions.")
        }

        if (session.currentQuestionIndex != questionIndex) {
            return SubmissionResult.Error("Submission does not match the active question index.")
        }

        val participant = multiplayerGameDao.getParticipant(sessionId, userId)
            ?: return SubmissionResult.Error("Participant not registered in this session.")

        // Late Entry Rule: Cannot answer questions missed prior to joining
        if (questionIndex < participant.joinedAtQuestionIndex) {
            return SubmissionResult.MissedQuestion(
                "You joined after this question was completed. Missed questions receive 0 points."
            )
        }

        // Duplicate submission prevention rule
        val existingSubmission = multiplayerGameDao.getSubmission(sessionId, questionIndex, userId)
        if (existingSubmission != null) {
            return SubmissionResult.DuplicateRejected(
                "Duplicate submission rejected. Only one submission is permitted per question."
            )
        }

        // Server-Authoritative Deadline check
        if (submissionTime > session.currentQuestionDeadline) {
            val lateSubmission = MultiplayerSubmissionEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                questionIndex = questionIndex,
                userId = userId,
                userName = userName,
                submittedAnswer = submittedAnswer,
                isCorrect = false,
                pointsAwarded = 0,
                speedBonus = 0,
                submissionTime = submissionTime,
                isLate = true
            )
            multiplayerGameDao.insertSubmission(lateSubmission)
            return SubmissionResult.DeadlineExpired("Submission received after the question deadline expired.")
        }

        // Evaluate Answer Correctness
        val questions = parseQuestions(session.questionsJson)
        val question = questions.getOrNull(questionIndex)
            ?: return SubmissionResult.Error("Question definition not found.")

        val isCorrect = when (session.gameType) {
            GameType.TRIVIA_CHALLENGE -> {
                submittedAnswer.trim().equals(question.correctAnswer.trim(), ignoreCase = true)
            }
            GameType.WHO_AM_I -> {
                submittedAnswer.trim().equals(question.correctAnswer.trim(), ignoreCase = true)
            }
            GameType.SCRIPTURE_SCRAMBLE -> {
                // Compare normalized sequence of words
                normalizeWords(submittedAnswer) == normalizeWords(question.correctAnswer)
            }
        }

        var speedBonus = 0
        var totalPoints = 0

        if (isCorrect) {
            val base = if (question.points > 0) question.points else session.baseScorePerQuestion
            
            // For WHO_AM_I: earlier answers (answering while fewer/harder clues are revealed) get higher points!
            val clueMultiplier = if (session.gameType == GameType.WHO_AM_I && question.clues.isNotEmpty()) {
                val numClues = question.clues.size
                val totalWindow = (session.timeLimitPerQuestionSeconds * 1000L).coerceAtLeast(1000L)
                val timeRemaining = (session.currentQuestionDeadline - submissionTime).coerceAtLeast(0L)
                val timeElapsed = (totalWindow - timeRemaining).coerceAtLeast(0L)
                val clueInterval = (totalWindow / numClues).coerceAtLeast(1000L)
                val currentClueIndex = (timeElapsed / clueInterval).toInt().coerceIn(0, numClues - 1)
                when (currentClueIndex) {
                    0 -> 1.5 // Solved on Hardest Clue 1: 50% bonus points!
                    1 -> 1.25 // Solved on Clue 2: 25% bonus points!
                    else -> 1.0 // Solved on later/easiest clues: base points
                }
            } else {
                1.0
            }

            val adjustedBase = (base * clueMultiplier).toInt()

            if (session.speedBonusEnabled && session.timeLimitPerQuestionSeconds > 0) {
                val timeRemaining = (session.currentQuestionDeadline - submissionTime).coerceAtLeast(0L)
                val totalWindow = session.timeLimitPerQuestionSeconds * 1000L
                val fraction = (timeRemaining.toDouble() / totalWindow.toDouble()).coerceIn(0.0, 1.0)
                speedBonus = (fraction * session.maxSpeedBonus).toInt()
            }
            totalPoints = adjustedBase + speedBonus
        } else {
            totalPoints = if (session.penaltyForIncorrect) -20 else 0
        }

        val submission = MultiplayerSubmissionEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            questionIndex = questionIndex,
            userId = userId,
            userName = userName,
            submittedAnswer = submittedAnswer,
            isCorrect = isCorrect,
            pointsAwarded = totalPoints,
            speedBonus = speedBonus,
            submissionTime = submissionTime,
            isLate = false
        )
        multiplayerGameDao.insertSubmission(submission)

        // Update participant score and tie-breaker timestamp
        val updatedScore = (participant.currentScore + totalPoints).coerceAtLeast(0)
        val updatedParticipant = participant.copy(
            currentScore = updatedScore,
            lastQuestionPoints = totalPoints,
            lastAnswerTimestamp = submissionTime,
            answeredQuestionsCount = participant.answeredQuestionsCount + 1
        )
        multiplayerGameDao.updateParticipant(updatedParticipant)
        pushSessionParticipantsToCloud(sessionId)

        val message = if (isCorrect) {
            if (speedBonus > 0) "Correct! +$totalPoints points (includes +$speedBonus speed bonus)." else "Correct! +$totalPoints points."
        } else {
            if (totalPoints < 0) "Incorrect answer. $totalPoints points." else "Incorrect answer. 0 points."
        }

        return SubmissionResult.Success(
            pointsAwarded = totalPoints,
            isCorrect = isCorrect,
            speedBonus = speedBonus,
            message = message
        )
    }

    private fun normalizeWords(text: String): String =
        text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ")

    override suspend fun cancelSession(sessionId: String, actorId: String, actorRole: UserRole) {
        multiplayerGameDao.cancelSession(sessionId)
        runCatching {
            cloudRepository?.updateCloudQuizSessionState(
                sessionId = sessionId,
                status = SessionStatus.CANCELLED.name
            )
        }
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = actorId,
                userRole = actorRole,
                actionType = "MULTIPLAYER_GAME_CANCELLED",
                targetEntity = "MultiplayerGameSession",
                targetId = sessionId,
                details = "Executive cancelled game session $sessionId"
            )
        )
    }

    override fun parseQuestions(questionsJson: String): List<MultiplayerQuestion> {
        val result = mutableListOf<MultiplayerQuestion>()
        try {
            val array = JSONArray(questionsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeStr = obj.optString("gameType", GameType.TRIVIA_CHALLENGE.name)
                val gameType = runCatching { GameType.valueOf(typeStr) }.getOrDefault(GameType.TRIVIA_CHALLENGE)

                val options = mutableListOf<String>()
                val optArr = obj.optJSONArray("options")
                if (optArr != null) {
                    for (j in 0 until optArr.length()) {
                        options.add(optArr.getString(j))
                    }
                }

                val clues = mutableListOf<String>()
                val clueArr = obj.optJSONArray("clues")
                if (clueArr != null) {
                    for (j in 0 until clueArr.length()) {
                        clues.add(clueArr.getString(j))
                    }
                }

                val scrambledWords = mutableListOf<String>()
                val scrambleArr = obj.optJSONArray("scrambledWords")
                if (scrambleArr != null) {
                    for (j in 0 until scrambleArr.length()) {
                        scrambledWords.add(scrambleArr.getString(j))
                    }
                }

                result.add(
                    MultiplayerQuestion(
                        id = obj.optInt("id", i + 1),
                        gameType = gameType,
                        prompt = obj.optString("prompt", ""),
                        options = options,
                        correctAnswer = obj.optString("correctAnswer", ""),
                        clues = clues,
                        scrambledWords = scrambledWords,
                        scriptureReference = obj.optString("scriptureReference", ""),
                        translation = obj.optString("translation", "KJV"),
                        timerSeconds = obj.optInt("timerSeconds", 20),
                        points = obj.optInt("points", 100),
                        explanation = obj.optString("explanation", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    override fun serializeQuestions(questions: List<MultiplayerQuestion>): String {
        val array = JSONArray()
        for (q in questions) {
            val obj = JSONObject()
            obj.put("id", q.id)
            obj.put("gameType", q.gameType.name)
            obj.put("prompt", q.prompt)
            obj.put("correctAnswer", q.correctAnswer)
            obj.put("scriptureReference", q.scriptureReference)
            obj.put("translation", q.translation)
            obj.put("timerSeconds", q.timerSeconds)
            obj.put("points", q.points)
            obj.put("explanation", q.explanation)

            val optArr = JSONArray()
            q.options.forEach { optArr.put(it) }
            obj.put("options", optArr)

            val clueArr = JSONArray()
            q.clues.forEach { clueArr.put(it) }
            obj.put("clues", clueArr)

            val scrambleArr = JSONArray()
            q.scrambledWords.forEach { scrambleArr.put(it) }
            obj.put("scrambledWords", scrambleArr)

            array.put(obj)
        }
        return array.toString()
    }
}
