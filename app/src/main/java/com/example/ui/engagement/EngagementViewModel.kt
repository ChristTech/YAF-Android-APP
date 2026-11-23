package com.example.ui.engagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.GameQuizEntity
import com.example.data.local.entity.GameScoreEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.WeeklyReportEntity
import com.example.data.model.DepartmentType
import com.example.data.model.ReportStatus
import com.example.data.model.UserRole
import com.example.data.repository.GamesRepository
import com.example.data.repository.WeeklyReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

import com.example.data.model.QuizCategory
import org.json.JSONObject

import com.example.data.local.entity.MultiplayerGameSessionEntity
import com.example.data.local.entity.MultiplayerParticipantEntity
import com.example.data.local.entity.MultiplayerSubmissionEntity
import com.example.data.model.GameType
import com.example.data.model.MultiplayerLeaderboardEntry
import com.example.data.model.MultiplayerQuestion
import com.example.data.model.SessionStatus
import com.example.data.model.SessionSubstate
import com.example.data.model.SubmissionResult
import com.example.data.model.scrambleVerse
import com.example.data.repository.MultiplayerGameRepository
import kotlinx.coroutines.flow.emptyFlow

data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val scriptureRef: String = ""
)

data class ArenaCompetitor(
    val id: String,
    val name: String,
    val unit: String,
    val score: Int,
    val lastPoints: Int = 0,
    val isCurrentUser: Boolean = false,
    val previousRank: Int = 0,
    val rankMovement: Int = 0
)

data class ActiveQuizState(
    val quiz: GameQuizEntity? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val userScore: Int = 0, // Count of correct answers
    val totalGamePoints: Int = 0, // Total speed + accuracy points
    val isFinished: Boolean = false,
    val selectedOptionIndex: Int? = null,
    val lockedInTimeRemainingSeconds: Int = 0,
    val isAnswerChecked: Boolean = false,
    val isTimeUp: Boolean = false,
    val timeRemainingSeconds: Int = 15,
    val maxQuestionTimeSeconds: Int = 15,
    val intermissionTimeRemainingSeconds: Int = 7,
    val maxIntermissionTimeSeconds: Int = 7,
    val isIntermissionLeaderboard: Boolean = false,
    val pointsAwardedLastQuestion: Int = 0,
    val speedBonusLastQuestion: Int = 0,
    val competitors: List<ArenaCompetitor> = emptyList(),
    val currentUserRank: Int = 1,
    val currentUserRankMovement: Int = 0,
    val isLiveGameTimeSession: Boolean = false
)

data class EngagementUiState(
    val activeTab: Int = 0, // 0 = Games, 1 = Reports
    val activeQuiz: ActiveQuizState = ActiveQuizState(),
    val isSubmitReportOpen: Boolean = false,
    val isCreateQuizOpen: Boolean = false,
    val isCreateMultiplayerGameOpen: Boolean = false,
    val editingDraftSession: MultiplayerGameSessionEntity? = null,
    val isInMultiplayerArena: Boolean = false,
    val activeMultiplayerSessionId: String? = null,
    val selectedScrambleWords: List<String> = emptyList(),
    val scrambledPool: List<String> = emptyList(),
    val lastSubmissionResult: SubmissionResult? = null,
    val multiplayerTimeRemainingSeconds: Int = 0,
    val isReviewReportOpen: Boolean = false,
    val selectedReportForReview: WeeklyReportEntity? = null,
    val reportDepartmentFilter: DepartmentType? = null,
    val reportStatusFilter: ReportStatus? = null,
    val isGameTimeActive: Boolean = false,
    val liveParticipantsCount: Int = 0,
    val gameTimeAnnouncement: String = "Live Scripture Showdown arena is ready. Fellowship leaders can broadcast Game Time when starting.",
    val message: String? = null
)

class EngagementViewModel(
    private val gamesRepository: GamesRepository,
    private val weeklyReportRepository: WeeklyReportRepository,
    private val session: SessionEntity,
    private val notificationService: com.example.core.notification.FellowshipNotificationService? = null,
    private val multiplayerGameRepository: MultiplayerGameRepository? = null,
    val bibleRepository: com.example.data.repository.BibleRepository? = null
) : ViewModel() {

    init {
        viewModelScope.launch {
            try {
                bibleRepository?.ensureSeeded()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _uiState = MutableStateFlow(EngagementUiState())
    val uiState: StateFlow<EngagementUiState> = _uiState.asStateFlow()

    val quizzes: StateFlow<List<GameQuizEntity>> = gamesRepository.getAllQuizzes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val leaderboard: StateFlow<List<GameScoreEntity>> = gamesRepository.getGlobalLeaderboard()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val myScores: StateFlow<List<GameScoreEntity>> = gamesRepository.getScoresForUser(session.userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val weeklyReports: StateFlow<List<WeeklyReportEntity>> = weeklyReportRepository.getAllReports()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ================= MULTIPLAYER SESSIONS FLOWS =================
    val upcomingAndActiveSessions: StateFlow<List<MultiplayerGameSessionEntity>> =
        multiplayerGameRepository?.getUpcomingAndActiveSessions()
            ?.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            ) ?: MutableStateFlow(emptyList())

    val draftSessions: StateFlow<List<MultiplayerGameSessionEntity>> =
        multiplayerGameRepository?.getDraftSessions()
            ?.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            ) ?: MutableStateFlow(emptyList())

    val completedSessions: StateFlow<List<MultiplayerGameSessionEntity>> =
        multiplayerGameRepository?.getCompletedSessions()
            ?.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            ) ?: MutableStateFlow(emptyList())

    private val _activeMultiplayerSession = MutableStateFlow<MultiplayerGameSessionEntity?>(null)
    val activeMultiplayerSession: StateFlow<MultiplayerGameSessionEntity?> = _activeMultiplayerSession.asStateFlow()

    private val _activeMultiplayerQuestions = MutableStateFlow<List<MultiplayerQuestion>>(emptyList())
    val activeMultiplayerQuestions: StateFlow<List<MultiplayerQuestion>> = _activeMultiplayerQuestions.asStateFlow()

    private val _multiplayerLeaderboard = MutableStateFlow<List<MultiplayerLeaderboardEntry>>(emptyList())
    val multiplayerLeaderboard: StateFlow<List<MultiplayerLeaderboardEntry>> = _multiplayerLeaderboard.asStateFlow()

    private val _currentParticipant = MutableStateFlow<MultiplayerParticipantEntity?>(null)
    val currentParticipant: StateFlow<MultiplayerParticipantEntity?> = _currentParticipant.asStateFlow()

    private val _submissionForCurrentQuestion = MutableStateFlow<MultiplayerSubmissionEntity?>(null)
    val submissionForCurrentQuestion: StateFlow<MultiplayerSubmissionEntity?> = _submissionForCurrentQuestion.asStateFlow()

    private var multiplayerTickerJob: Job? = null

    fun setActiveTab(tab: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun setSubmitReportOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isSubmitReportOpen = open)
    }

    fun setCreateQuizOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isCreateQuizOpen = open)
    }

    fun setReportDepartmentFilter(dept: DepartmentType?) {
        _uiState.value = _uiState.value.copy(reportDepartmentFilter = dept)
    }

    fun setReportStatusFilter(status: ReportStatus?) {
        _uiState.value = _uiState.value.copy(reportStatusFilter = status)
    }

    fun openReviewReport(report: WeeklyReportEntity) {
        _uiState.value = _uiState.value.copy(
            isReviewReportOpen = true,
            selectedReportForReview = report
        )
    }

    fun closeReviewReport() {
        _uiState.value = _uiState.value.copy(
            isReviewReportOpen = false,
            selectedReportForReview = null
        )
    }

    fun reviewReport(reportId: String, status: ReportStatus, notes: String) {
        viewModelScope.launch {
            weeklyReportRepository.reviewReport(
                id = reportId,
                status = status,
                reviewerId = session.userId,
                reviewerRole = session.userRole,
                notes = notes
            )
            _uiState.value = _uiState.value.copy(
                isReviewReportOpen = false,
                selectedReportForReview = null,
                message = "Report status updated to ${status.name} with reviewer notes."
            )
        }
    }

    fun createQuiz(
        title: String,
        description: String,
        category: QuizCategory,
        difficulty: String,
        timeLimitSeconds: Int,
        questions: List<QuizQuestion>
    ) {
        if (title.isBlank() || questions.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "Title and at least one question are required.")
            return
        }

        viewModelScope.launch {
            val jsonArray = JSONArray()
            questions.forEachIndexed { idx, q ->
                val obj = JSONObject()
                obj.put("id", idx + 1)
                obj.put("question", q.question)
                val optArr = JSONArray()
                q.options.forEach { optArr.put(it) }
                obj.put("options", optArr)
                obj.put("correctAnswerIndex", q.correctAnswerIndex)
                if (q.scriptureRef.isNotBlank()) {
                    obj.put("scriptureRef", q.scriptureRef)
                }
                jsonArray.put(obj)
            }

            val newQuiz = GameQuizEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                description = description.trim().ifEmpty { "Scripture challenge created by The Gideonites - Youth Alive Admin." },
                category = category,
                questionsJson = jsonArray.toString(),
                totalQuestions = questions.size,
                timeLimitSeconds = timeLimitSeconds,
                difficulty = difficulty
            )

            gamesRepository.insertQuiz(newQuiz, session.userId)
            _uiState.value = _uiState.value.copy(
                isCreateQuizOpen = false,
                message = "New quiz '${newQuiz.title}' created successfully!"
            )
        }
    }

    fun deleteQuiz(quizId: String) {
        viewModelScope.launch {
            gamesRepository.deleteQuiz(quizId, session.userId)
            _uiState.value = _uiState.value.copy(message = "Quiz removed from catalog.")
        }
    }

    fun deleteScore(scoreId: String) {
        viewModelScope.launch {
            gamesRepository.deleteScore(scoreId, session.userId)
            _uiState.value = _uiState.value.copy(message = "Score entry removed by Admin.")
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private var questionTimerJob: Job? = null
    private var intermissionTimerJob: Job? = null

    private fun createInitialCompetitors(): List<ArenaCompetitor> {
        val userName = session.userName.ifBlank { "You" }
        return listOf(
            ArenaCompetitor(id = "comp-1", name = "Sister Grace", unit = "Choir Unit", score = 0, previousRank = 1),
            ArenaCompetitor(id = "comp-2", name = "Brother Samuel", unit = "Media Unit", score = 0, previousRank = 2),
            ArenaCompetitor(id = "comp-3", name = "Brother David O.", unit = "Prayer Unit", score = 0, previousRank = 3),
            ArenaCompetitor(id = "comp-user", name = userName, unit = "Kingdom Giants", score = 0, isCurrentUser = true, previousRank = 4),
            ArenaCompetitor(id = "comp-4", name = "Sister Faith", unit = "Ushering Unit", score = 0, previousRank = 5),
            ArenaCompetitor(id = "comp-5", name = "Brother Emmanuel", unit = "Cell Fellowship", score = 0, previousRank = 6),
            ArenaCompetitor(id = "comp-6", name = "Sister Chisom", unit = "Sanctuary Keepers", score = 0, previousRank = 7)
        )
    }

    fun startQuiz(quiz: GameQuizEntity, isLiveGame: Boolean = false) {
        questionTimerJob?.cancel()
        intermissionTimerJob?.cancel()
        val parsedQuestions = parseQuestions(quiz.questionsJson)
        val initialCompetitors = createInitialCompetitors()
        val qCount = parsedQuestions.size.coerceAtLeast(1)
        val perQuestionSeconds = if (quiz.timeLimitSeconds >= qCount * 5) {
            (quiz.timeLimitSeconds / qCount).coerceIn(5, 120)
        } else {
            quiz.timeLimitSeconds.coerceIn(5, 120)
        }

        _uiState.update { state ->
            state.copy(
                activeQuiz = ActiveQuizState(
                    quiz = quiz,
                    questions = parsedQuestions,
                    currentQuestionIndex = 0,
                    userScore = 0,
                    totalGamePoints = 0,
                    isFinished = false,
                    selectedOptionIndex = null,
                    lockedInTimeRemainingSeconds = 0,
                    isAnswerChecked = false,
                    isTimeUp = false,
                    timeRemainingSeconds = perQuestionSeconds,
                    maxQuestionTimeSeconds = perQuestionSeconds,
                    intermissionTimeRemainingSeconds = 7,
                    maxIntermissionTimeSeconds = 7,
                    isIntermissionLeaderboard = false,
                    pointsAwardedLastQuestion = 0,
                    speedBonusLastQuestion = 0,
                    competitors = initialCompetitors,
                    currentUserRank = 4,
                    currentUserRankMovement = 0,
                    isLiveGameTimeSession = isLiveGame
                )
            )
        }
        startQuestionTimer(perQuestionSeconds)
    }

    private fun startQuestionTimer(seconds: Int = 15) {
        questionTimerJob?.cancel()
        intermissionTimerJob?.cancel()
        questionTimerJob = viewModelScope.launch {
            for (sec in seconds downTo 1) {
                _uiState.update { state ->
                    val quiz = state.activeQuiz
                    if (quiz.isAnswerChecked || quiz.isIntermissionLeaderboard || quiz.isFinished) {
                        state
                    } else {
                        state.copy(activeQuiz = quiz.copy(timeRemainingSeconds = sec))
                    }
                }
                delay(1000L)
            }
            // Timer ran out! Now reveal correct answer & ranked leaderboard to all players simultaneously.
            handleQuestionTimeUp()
        }
    }

    private fun startIntermissionTimer() {
        intermissionTimerJob?.cancel()
        intermissionTimerJob = viewModelScope.launch {
            for (sec in 7 downTo 1) {
                _uiState.update { state ->
                    val quiz = state.activeQuiz
                    if (!quiz.isIntermissionLeaderboard || quiz.isFinished) {
                        state
                    } else {
                        state.copy(activeQuiz = quiz.copy(intermissionTimeRemainingSeconds = sec))
                    }
                }
                delay(1000L)
            }
            val currentQuiz = _uiState.value.activeQuiz
            if (currentQuiz.isIntermissionLeaderboard && !currentQuiz.isFinished) {
                nextQuestion()
            }
        }
    }

    private fun handleQuestionTimeUp() {
        val currentQuiz = _uiState.value.activeQuiz
        if (currentQuiz.isAnswerChecked || currentQuiz.isFinished || currentQuiz.isIntermissionLeaderboard) return

        val question = currentQuiz.questions.getOrNull(currentQuiz.currentQuestionIndex)
        val lockedOption = currentQuiz.selectedOptionIndex
        val isTimeout = lockedOption == null
        val isCorrect = question != null && lockedOption != null && lockedOption == question.correctAnswerIndex
        val lockedTimeLeft = currentQuiz.lockedInTimeRemainingSeconds.coerceAtLeast(1)

        val maxQTime = currentQuiz.maxQuestionTimeSeconds.coerceAtLeast(1)
        val speedBonus = if (isCorrect) ((lockedTimeLeft.toDouble() / maxQTime.toDouble()) * 450.0).toInt().coerceIn(0, 450) else 0
        val pointsAwarded = if (isCorrect) 550 + speedBonus else 0
        val newTotalPoints = currentQuiz.totalGamePoints + pointsAwarded
        val newUserCorrectCount = if (isCorrect) currentQuiz.userScore + 1 else currentQuiz.userScore

        val prevRank = currentQuiz.currentUserRank
        val updatedCompetitors = simulateCompetitorScores(currentQuiz.competitors, userPoints = pointsAwarded)
        val newRank = updatedCompetitors.indexOfFirst { it.isCurrentUser }.takeIf { it >= 0 }?.plus(1) ?: prevRank
        val movement = prevRank - newRank

        _uiState.update { state ->
            state.copy(
                activeQuiz = currentQuiz.copy(
                    isAnswerChecked = true,
                    isTimeUp = isTimeout,
                    timeRemainingSeconds = 0,
                    intermissionTimeRemainingSeconds = 7,
                    maxIntermissionTimeSeconds = 7,
                    userScore = newUserCorrectCount,
                    totalGamePoints = newTotalPoints,
                    pointsAwardedLastQuestion = pointsAwarded,
                    speedBonusLastQuestion = speedBonus,
                    competitors = updatedCompetitors,
                    currentUserRank = newRank,
                    currentUserRankMovement = movement,
                    isIntermissionLeaderboard = true
                )
            )
        }
        startIntermissionTimer()
    }

    fun selectOption(optionIndex: Int) {
        val currentQuiz = _uiState.value.activeQuiz
        if (currentQuiz.isAnswerChecked || currentQuiz.isFinished || currentQuiz.isIntermissionLeaderboard) return
        if (currentQuiz.selectedOptionIndex != null) return // Already locked in

        // Lock in the user's answer & timestamp without revealing correctness until the timer finishes!
        _uiState.update { state ->
            state.copy(
                activeQuiz = currentQuiz.copy(
                    selectedOptionIndex = optionIndex,
                    lockedInTimeRemainingSeconds = currentQuiz.timeRemainingSeconds
                )
            )
        }
    }

    private fun simulateCompetitorScores(
        currentList: List<ArenaCompetitor>,
        userPoints: Int
    ): List<ArenaCompetitor> {
        val prevRankMap = currentList.mapIndexed { idx, comp -> comp.id to (idx + 1) }.toMap()
        val updated = currentList.map { competitor ->
            if (competitor.isCurrentUser) {
                competitor.copy(
                    score = competitor.score + userPoints,
                    lastPoints = userPoints
                )
            } else {
                // Realistic multiplayer simulation based on peer skill & response speed
                val (isCorrect, simulatedSpeedBonus) = when (competitor.id) {
                    "comp-1" -> (Math.random() > 0.20) to (220..420).random() // Sister Grace
                    "comp-2" -> (Math.random() > 0.28) to (180..380).random() // Brother Samuel
                    "comp-3" -> (Math.random() > 0.35) to (150..340).random() // Brother David O.
                    "comp-4" -> (Math.random() > 0.40) to (110..290).random() // Sister Faith
                    "comp-5" -> (Math.random() > 0.45) to (90..250).random()  // Brother Emmanuel
                    else -> (Math.random() > 0.50) to (70..220).random()      // Sister Chisom
                }
                val peerPoints = if (isCorrect) 550 + simulatedSpeedBonus else 0
                competitor.copy(
                    score = competitor.score + peerPoints,
                    lastPoints = peerPoints
                )
            }
        }
        return updated
            .sortedByDescending { it.score }
            .mapIndexed { newIdx, comp ->
                val newRank = newIdx + 1
                val prevRank = prevRankMap[comp.id] ?: newRank
                comp.copy(
                    previousRank = prevRank,
                    rankMovement = prevRank - newRank
                )
            }
    }

    fun nextQuestion() {
        intermissionTimerJob?.cancel()
        val currentQuiz = _uiState.value.activeQuiz
        val nextIdx = currentQuiz.currentQuestionIndex + 1

        if (nextIdx < currentQuiz.questions.size) {
            val perQSeconds = currentQuiz.maxQuestionTimeSeconds.coerceIn(5, 120)
            _uiState.update { state ->
                state.copy(
                    activeQuiz = currentQuiz.copy(
                        currentQuestionIndex = nextIdx,
                        selectedOptionIndex = null,
                        lockedInTimeRemainingSeconds = 0,
                        isAnswerChecked = false,
                        isTimeUp = false,
                        timeRemainingSeconds = perQSeconds,
                        intermissionTimeRemainingSeconds = 7,
                        isIntermissionLeaderboard = false,
                        pointsAwardedLastQuestion = 0,
                        speedBonusLastQuestion = 0
                    )
                )
            }
            startQuestionTimer(perQSeconds)
        } else {
            // Finish quiz and persist score
            questionTimerJob?.cancel()
            intermissionTimerJob?.cancel()
            _uiState.update { state ->
                state.copy(
                    activeQuiz = currentQuiz.copy(
                        isFinished = true,
                        isIntermissionLeaderboard = false
                    )
                )
            }

            currentQuiz.quiz?.let { q ->
                viewModelScope.launch {
                    val scoreEntry = GameScoreEntity(
                        id = UUID.randomUUID().toString(),
                        quizId = q.id,
                        quizTitle = q.title,
                        userId = session.userId,
                        userName = session.userName,
                        score = currentQuiz.userScore,
                        totalQuestions = currentQuiz.questions.size,
                        timeTakenSeconds = currentQuiz.maxQuestionTimeSeconds * currentQuiz.questions.size
                    )
                    gamesRepository.submitScore(scoreEntry, session.userRole)
                }
            }
        }
    }

    fun exitQuiz() {
        questionTimerJob?.cancel()
        intermissionTimerJob?.cancel()
        _uiState.update { state ->
            state.copy(activeQuiz = ActiveQuizState())
        }
    }

    // ================= MULTIPLAYER GAME ACTIONS =================

    fun setCreateMultiplayerGameOpen(
        open: Boolean,
        draftSession: MultiplayerGameSessionEntity? = null
    ) {
        _uiState.update {
            it.copy(
                isCreateMultiplayerGameOpen = open,
                editingDraftSession = if (open) draftSession else null
            )
        }
    }

    fun scheduleMultiplayerGame(
        title: String,
        description: String,
        gameType: GameType,
        delayMinutes: Int,
        timeLimitSeconds: Int,
        baseScore: Int,
        speedBonusEnabled: Boolean,
        questions: List<MultiplayerQuestion>,
        existingSessionId: String? = null
    ) {
        val repo = multiplayerGameRepository ?: return
        viewModelScope.launch {
            val scheduledStartTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
            val sessionId = repo.createAndScheduleSession(
                title = title,
                description = description,
                gameType = gameType,
                scheduledStartTime = scheduledStartTime,
                timeLimitPerQuestionSeconds = timeLimitSeconds,
                leaderboardDurationSeconds = 8,
                baseScorePerQuestion = baseScore,
                speedBonusEnabled = speedBonusEnabled,
                questions = questions,
                actorId = session.userId,
                actorName = session.userName,
                actorRole = session.userRole,
                existingSessionId = existingSessionId
            )

            _uiState.update {
                it.copy(
                    isCreateMultiplayerGameOpen = false,
                    editingDraftSession = null,
                    isGameTimeActive = true,
                    message = if (delayMinutes == 0) "Showdown launched! Entering arena..." else "Showdown scheduled successfully for Gideonites."
                )
            }

            if (delayMinutes == 0) {
                joinMultiplayerSession(sessionId)
            }
        }
    }

    fun saveMultiplayerGameDraft(
        title: String,
        description: String,
        gameType: GameType,
        timeLimitSeconds: Int,
        baseScore: Int,
        speedBonusEnabled: Boolean,
        questions: List<MultiplayerQuestion>,
        existingSessionId: String? = null
    ) {
        val repo = multiplayerGameRepository ?: return
        viewModelScope.launch {
            repo.saveDraftSession(
                title = title,
                description = description,
                gameType = gameType,
                timeLimitPerQuestionSeconds = timeLimitSeconds,
                leaderboardDurationSeconds = 8,
                baseScorePerQuestion = baseScore,
                speedBonusEnabled = speedBonusEnabled,
                questions = questions,
                actorId = session.userId,
                actorName = session.userName,
                actorRole = session.userRole,
                existingSessionId = existingSessionId
            )

            _uiState.update {
                it.copy(
                    isCreateMultiplayerGameOpen = false,
                    editingDraftSession = null,
                    message = "Game saved as draft! Edit or launch it anytime from Saved Game Drafts."
                )
            }
        }
    }

    fun launchDraftSession(sessionId: String) {
        val repo = multiplayerGameRepository ?: return
        viewModelScope.launch {
            repo.launchDraftSession(sessionId, delayMinutes = 0, currentTime = System.currentTimeMillis())
            _uiState.update {
                it.copy(
                    isGameTimeActive = true,
                    message = "Saved draft launched live! Entering arena..."
                )
            }
            joinMultiplayerSession(sessionId)
        }
    }

    fun deleteDraftSession(sessionId: String) {
        val repo = multiplayerGameRepository ?: return
        viewModelScope.launch {
            repo.deleteDraftSession(sessionId, session.userId, session.userRole)
            _uiState.update { it.copy(message = "Saved game draft deleted.") }
        }
    }

    fun startMultiplayerGameImmediately(sessionId: String) {
        val repo = multiplayerGameRepository ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repo.startSessionImmediately(sessionId, now)
            joinMultiplayerSession(sessionId)
        }
    }

    fun cancelMultiplayerGame(sessionId: String) {
        val repo = multiplayerGameRepository ?: return
        viewModelScope.launch {
            repo.cancelSession(sessionId, session.userId, session.userRole)
            _uiState.update { it.copy(message = "Multiplayer game session cancelled.") }
        }
    }

    fun joinMultiplayerSession(sessionId: String) {
        val repo = multiplayerGameRepository ?: return
        multiplayerTickerJob?.cancel()

        viewModelScope.launch {
            // Server-Authoritative Join: Late entry starts at current question, reconnection restores participant
            val participant = repo.joinSession(
                sessionId = sessionId,
                userId = session.userId,
                userName = session.userName,
                userRole = session.userRole
            )
            // Ensure fellowship peer participants are present so Top 5 vs Individual Standing is populated
            repo.ensureFellowshipPeersInSession(sessionId)
            _currentParticipant.value = participant

            val sessionEntity = repo.getSessionByIdSync(sessionId)
            _activeMultiplayerSession.value = sessionEntity

            if (sessionEntity != null) {
                val parsedQs = repo.parseQuestions(sessionEntity.questionsJson)
                _activeMultiplayerQuestions.value = parsedQs

                // Initialize scramble pool if first question is Scripture Scramble
                val currentQ = parsedQs.getOrNull(sessionEntity.currentQuestionIndex)
                if (currentQ != null && sessionEntity.gameType == GameType.SCRIPTURE_SCRAMBLE) {
                    val words = if (currentQ.scrambledWords.isNotEmpty()) currentQ.scrambledWords else scrambleVerse(currentQ.correctAnswer)
                    _uiState.update {
                        it.copy(
                            scrambledPool = words,
                            selectedScrambleWords = emptyList()
                        )
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isInMultiplayerArena = true,
                    activeMultiplayerSessionId = sessionId,
                    lastSubmissionResult = null
                )
            }

            // Launch synchronized session ticker loop
            startMultiplayerTicker(sessionId)
        }
    }

    private fun startMultiplayerTicker(sessionId: String) {
        val repo = multiplayerGameRepository ?: return
        multiplayerTickerJob = viewModelScope.launch {
            var lastObservedQIndex = -1
            var lastObservedSubstate: SessionSubstate? = null
            var lastObservedQuestionsJson: String? = null

            while (true) {
                val now = System.currentTimeMillis()
                val updatedSession = repo.syncSessionState(sessionId, now)
                _activeMultiplayerSession.value = updatedSession

                if (updatedSession != null) {
                    if (updatedSession.questionsJson != lastObservedQuestionsJson) {
                        lastObservedQuestionsJson = updatedSession.questionsJson
                        val reparsed = repo.parseQuestions(updatedSession.questionsJson)
                        if (reparsed.isNotEmpty()) {
                            _activeMultiplayerQuestions.value = reparsed
                        }
                    }

                    // Update Leaderboard
                    val lb = repo.getLeaderboardSync(sessionId, session.userId)
                    _multiplayerLeaderboard.value = lb

                    // Update Current Participant
                    val part = repo.getParticipant(sessionId, session.userId)
                    _currentParticipant.value = part

                    // Calculate real-time seconds remaining
                    val remainingSeconds = when (updatedSession.currentSubstate) {
                        SessionSubstate.WAITING_TO_START -> {
                            ((updatedSession.scheduledStartTime - now) / 1000L).coerceAtLeast(0L).toInt()
                        }
                        SessionSubstate.QUESTION_OPEN -> {
                            ((updatedSession.currentQuestionDeadline - now) / 1000L).coerceAtLeast(0L).toInt()
                        }
                        SessionSubstate.LEADERBOARD -> {
                            ((updatedSession.leaderboardDeadline - now) / 1000L).coerceAtLeast(0L).toInt()
                        }
                    }
                    _uiState.update { it.copy(multiplayerTimeRemainingSeconds = remainingSeconds) }

                    // When round advances or substate changes: reset scramble words and fetch user submission
                    if (updatedSession.currentQuestionIndex != lastObservedQIndex || updatedSession.currentSubstate != lastObservedSubstate) {
                        lastObservedQIndex = updatedSession.currentQuestionIndex
                        lastObservedSubstate = updatedSession.currentSubstate

                        val sub = repo.getSubmission(sessionId, updatedSession.currentQuestionIndex, session.userId)
                        _submissionForCurrentQuestion.value = sub

                        val currentQ = _activeMultiplayerQuestions.value.getOrNull(updatedSession.currentQuestionIndex)
                        if (currentQ != null && updatedSession.gameType == GameType.SCRIPTURE_SCRAMBLE) {
                            val words = if (currentQ.scrambledWords.isNotEmpty()) currentQ.scrambledWords else scrambleVerse(currentQ.correctAnswer)
                            _uiState.update {
                                it.copy(
                                    scrambledPool = words,
                                    selectedScrambleWords = emptyList(),
                                    lastSubmissionResult = null
                                )
                            }
                        } else {
                            _uiState.update { it.copy(lastSubmissionResult = null) }
                        }
                    } else {
                        // Check if submission exists
                        val sub = repo.getSubmission(sessionId, updatedSession.currentQuestionIndex, session.userId)
                        _submissionForCurrentQuestion.value = sub
                    }

                    if (updatedSession.status == SessionStatus.CANCELLED) {
                        break
                    }

                    if (updatedSession.status == SessionStatus.COMPLETED) {
                        recordCompletedMultiplayerScoreIfNeeded(updatedSession, part)
                        delay(2000L)
                        continue
                    }
                }

                delay(500L)
            }
        }
    }

    private val syncedCompletedMultiplayerSessions = mutableSetOf<String>()

    suspend fun recordCompletedMultiplayerScoreIfNeeded(
        completedSession: MultiplayerGameSessionEntity,
        participant: MultiplayerParticipantEntity?
    ) {
        if (completedSession.status != SessionStatus.COMPLETED) return
        if (session.userRole == UserRole.GUEST) return
        val part = participant ?: return
        if (syncedCompletedMultiplayerSessions.contains(completedSession.id)) return

        syncedCompletedMultiplayerSessions.add(completedSession.id)
        val maxPossiblePoints = (completedSession.totalQuestions * completedSession.baseScorePerQuestion).coerceAtLeast(100)
        val scoreEntity = GameScoreEntity(
            id = "mp-score-${completedSession.id}-${session.userId}",
            quizId = completedSession.id,
            quizTitle = "${completedSession.title} (${completedSession.gameType.displayName})",
            userId = session.userId,
            userName = session.userName,
            score = part.currentScore,
            totalQuestions = maxPossiblePoints,
            timeTakenSeconds = completedSession.totalQuestions * completedSession.timeLimitPerQuestionSeconds
        )
        runCatching {
            gamesRepository.submitScore(scoreEntity, session.userRole)
        }
    }

    fun submitMultiplayerAnswer(submittedAnswer: String) {
        val repo = multiplayerGameRepository ?: return
        val currentSession = _activeMultiplayerSession.value ?: return

        viewModelScope.launch {
            val result = repo.submitAnswer(
                sessionId = currentSession.id,
                questionIndex = currentSession.currentQuestionIndex,
                userId = session.userId,
                userName = session.userName,
                submittedAnswer = submittedAnswer
            )

            val sub = repo.getSubmission(currentSession.id, currentSession.currentQuestionIndex, session.userId)
            _submissionForCurrentQuestion.value = sub

            _uiState.update {
                it.copy(
                    lastSubmissionResult = result,
                    message = when (result) {
                        is SubmissionResult.Success -> null
                        is SubmissionResult.DuplicateRejected -> result.message
                        is SubmissionResult.DeadlineExpired -> result.message
                        is SubmissionResult.MissedQuestion -> result.message
                        is SubmissionResult.Error -> result.message
                    }
                )
            }
        }
    }

    fun tapScrambleWord(word: String) {
        _uiState.update { state ->
            val pool = state.scrambledPool.toMutableList()
            val removedIndex = pool.indexOf(word)
            if (removedIndex != -1) {
                pool.removeAt(removedIndex)
                state.copy(
                    scrambledPool = pool,
                    selectedScrambleWords = state.selectedScrambleWords + word
                )
            } else {
                state
            }
        }
    }

    fun removeScrambleWord(index: Int) {
        _uiState.update { state ->
            val selected = state.selectedScrambleWords.toMutableList()
            if (index in selected.indices) {
                val removedWord = selected.removeAt(index)
                state.copy(
                    selectedScrambleWords = selected,
                    scrambledPool = state.scrambledPool + removedWord
                )
            } else {
                state
            }
        }
    }

    fun clearScrambleWords() {
        val currentQ = _activeMultiplayerQuestions.value.getOrNull(_activeMultiplayerSession.value?.currentQuestionIndex ?: 0)
        val originalWords = if (currentQ != null) {
            if (currentQ.scrambledWords.isNotEmpty()) currentQ.scrambledWords else scrambleVerse(currentQ.correctAnswer)
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                selectedScrambleWords = emptyList(),
                scrambledPool = originalWords
            )
        }
    }

    fun leaveMultiplayerArena() {
        multiplayerTickerJob?.cancel()
        _uiState.update {
            it.copy(
                isInMultiplayerArena = false,
                activeMultiplayerSessionId = null,
                selectedScrambleWords = emptyList(),
                scrambledPool = emptyList(),
                lastSubmissionResult = null
            )
        }
    }

    fun enterLiveGameShowdown(preferredGameType: GameType? = null) {
        // Check if there is an active or upcoming multiplayer session matching preferredGameType (or any if null)
        val matchingSession = if (preferredGameType != null) {
            upcomingAndActiveSessions.value.firstOrNull { it.gameType == preferredGameType }
        } else {
            upcomingAndActiveSessions.value.firstOrNull()
        }
        if (matchingSession != null) {
            joinMultiplayerSession(matchingSession.id)
            return
        }

        val targetGameType = preferredGameType ?: GameType.TRIVIA_CHALLENGE

        // If no matching multiplayer session is present, create and launch an instant Gideonites Showdown!
        if (multiplayerGameRepository != null) {
            viewModelScope.launch {
                val activeLang = com.example.core.localization.LanguageManager.currentLanguage.value
                val presetQuestions = when (targetGameType) {
                    GameType.TRIVIA_CHALLENGE -> com.example.data.model.PresetGameSessions.getTriviaChallengeQuestions(activeLang)
                    GameType.WHO_AM_I -> com.example.data.model.PresetGameSessions.getWhoAmIQuestions(activeLang)
                    GameType.SCRIPTURE_SCRAMBLE -> com.example.data.model.PresetGameSessions.getScriptureScrambleQuestions(activeLang)
                }
                val defaultTimer = when (targetGameType) {
                    GameType.TRIVIA_CHALLENGE -> 20
                    GameType.WHO_AM_I -> 30
                    GameType.SCRIPTURE_SCRAMBLE -> 30
                }
                val sessionId = multiplayerGameRepository.createAndScheduleSession(
                    title = "The Gideonites - Youth Alive • ${targetGameType.displayName} Showdown",
                    description = targetGameType.description,
                    gameType = targetGameType,
                    scheduledStartTime = System.currentTimeMillis(),
                    timeLimitPerQuestionSeconds = defaultTimer,
                    baseScorePerQuestion = 100,
                    speedBonusEnabled = true,
                    questions = presetQuestions,
                    actorId = session.userId,
                    actorName = session.userName,
                    actorRole = session.userRole
                )
                joinMultiplayerSession(sessionId)
            }
            return
        }

        // Fallback to local solo quiz
        val activeQuizEntity = quizzes.value.firstOrNull() ?: GameQuizEntity(
            id = "live-showdown-01",
            title = "Sunday Fellowship Live Showdown",
            description = "Live multiplayer Scripture showdown for all The Gideonites - The Youth Alive Fellowship Kingdom Giants.",
            category = QuizCategory.BIBLE_TRIVIA,
            questionsJson = """[
                {"id":1,"question":"In which book is '...the people that do know their God shall be strong, and do exploits' found?","options":["Daniel","Isaiah","Ezekiel","Zechariah"],"correctAnswerIndex":0,"scriptureRef":"Daniel 11:32"},
                {"id":2,"question":"How many days did Esther and the Jews fast before she went before the king?","options":["7 days","3 days","40 days","1 day"],"correctAnswerIndex":1,"scriptureRef":"Esther 4:16"},
                {"id":3,"question":"How many days did it take Nehemiah to rebuild the broken walls of Jerusalem?","options":["120 days","52 days","70 days","30 days"],"correctAnswerIndex":1,"scriptureRef":"Nehemiah 6:15"},
                {"id":4,"question":"Which Old Testament king began to reign over Judah at just seven years of age?","options":["Josiah","Joash","Hezekiah","David"],"correctAnswerIndex":1,"scriptureRef":"2 Kings 11:21"}
            ]""".trimIndent(),
            totalQuestions = 4,
            timeLimitSeconds = 60,
            difficulty = "Live Showdown"
        )
        startQuiz(activeQuizEntity, isLiveGame = true)
    }

    fun broadcastGameTimeAlert() {
        notificationService?.postNotification(
            title = "🎮 IT'S GAME TIME! Live Bible Showdown",
            message = "Sunday Fellowship Live Showdown is live now! Tap to enter the arena and play with fellowship members!",
            type = com.example.data.local.entity.NotificationType.GAME_TIME,
            authorName = session.userName,
            authorRole = session.userRole.displayName,
            isUrgent = true
        )

        _uiState.update { state ->
            state.copy(
                isGameTimeActive = true,
                message = "🔔 Live Game Time notification sent to all members!"
            )
        }
    }

    fun toggleGameTime(active: Boolean) {
        _uiState.update { state ->
            state.copy(
                isGameTimeActive = active,
                message = if (active) "Game Time activated! Youth notified to enter the arena." else "Game Time ended."
            )
        }
    }

    fun submitWeeklyReport(
        title: String,
        dept: DepartmentType,
        fellowship: String,
        attendance: Int,
        males: Int,
        females: Int,
        firstTimers: Int,
        activities: String,
        achievements: String,
        challenges: String,
        finSummary: String,
        nextWeekGoals: String
    ) {
        if (title.isBlank() || activities.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Title and activities summary are required.")
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val report = WeeklyReportEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                departmentOrUnit = dept,
                fellowshipGroup = fellowship.trim(),
                submittedById = session.userId,
                submittedByName = session.userName,
                submittedByRole = session.userRole.displayName,
                weekStartDateMillis = now - (7 * 24 * 60 * 60 * 1000L),
                weekEndDateMillis = now,
                meetingAttendance = attendance,
                maleCount = males,
                femaleCount = females,
                firstTimersCount = firstTimers,
                activitiesConducted = activities.trim(),
                keyAchievements = achievements.trim(),
                challengesEncountered = challenges.trim(),
                financialSummary = finSummary.trim(),
                nextWeekGoals = nextWeekGoals.trim(),
                status = ReportStatus.SUBMITTED
            )

            weeklyReportRepository.submitReport(report, session.userRole)
            notificationService?.postNotification(
                id = "notif_report_${report.id}",
                title = "📊 Weekly Report Submitted: ${report.title}",
                message = "${dept.displayName} (${report.fellowshipGroup}) • Attendance: $attendance ($firstTimers first-timers) by ${session.userName}.",
                type = com.example.data.local.entity.NotificationType.REPORT_REVIEW,
                authorName = session.userName,
                authorRole = session.userRole.displayName
            )
            _uiState.value = _uiState.value.copy(
                isSubmitReportOpen = false,
                message = "Weekly report submitted to Youth Executive successfully!"
            )
        }
    }

    private fun parseQuestions(jsonString: String): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        runCatching {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optInt("id", i)
                val qText = obj.getString("question")
                val optArray = obj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optArray.length()) {
                    options.add(optArray.getString(j))
                }
                val correctIndex = obj.getInt("correctAnswerIndex")
                val scriptureRef = obj.optString("scriptureRef", "")
                list.add(QuizQuestion(id, qText, options, correctIndex, scriptureRef))
            }
        }
        return list
    }

    companion object {
        fun provideFactory(
            gamesRepository: GamesRepository,
            weeklyReportRepository: WeeklyReportRepository,
            session: SessionEntity,
            notificationService: com.example.core.notification.FellowshipNotificationService? = null,
            multiplayerGameRepository: MultiplayerGameRepository? = null,
            bibleRepository: com.example.data.repository.BibleRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EngagementViewModel(
                        gamesRepository,
                        weeklyReportRepository,
                        session,
                        notificationService,
                        multiplayerGameRepository,
                        bibleRepository
                    ) as T
                }
            }
    }
}

