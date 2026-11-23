package com.example.ui.engagement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SessionEntity
import com.example.data.model.UserRole
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesAndReportsScreen(
    viewModel: EngagementViewModel,
    session: SessionEntity,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val quizzes by viewModel.quizzes.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val myScores by viewModel.myScores.collectAsState()
    val upcomingAndActiveSessions by viewModel.upcomingAndActiveSessions.collectAsState()
    val draftSessions by viewModel.draftSessions.collectAsState()
    val completedSessions by viewModel.completedSessions.collectAsState()
    val activeMultiplayerSession by viewModel.activeMultiplayerSession.collectAsState()
    val activeMultiplayerQuestions by viewModel.activeMultiplayerQuestions.collectAsState()
    val multiplayerLeaderboard by viewModel.multiplayerLeaderboard.collectAsState()
    val currentParticipant by viewModel.currentParticipant.collectAsState()
    val submissionForCurrentQuestion by viewModel.submissionForCurrentQuestion.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showBibleDatabaseDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var pendingSelectedBibleVerse by remember {
        androidx.compose.runtime.mutableStateOf<com.example.data.local.entity.BibleVerseEntity?>(null)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Admin or Youth Executive can quickly create/schedule multiplayer showdowns
            if (!uiState.isInMultiplayerArena && uiState.activeQuiz.quiz == null &&
                (session.userRole == UserRole.ADMINISTRATOR || session.userRole == UserRole.YOUTH_EXECUTIVE)) {
                FloatingActionButton(
                    onClick = {
                        pendingSelectedBibleVerse = null
                        viewModel.setCreateMultiplayerGameOpen(true)
                    },
                    containerColor = FlameRedPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_quiz_fab")
                ) {
                    Icon(imageVector = Icons.Default.SportsEsports, contentDescription = "Schedule Multiplayer Showdown")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // If in multiplayer arena: render full arena
            if (uiState.isInMultiplayerArena && activeMultiplayerSession != null) {
                MultiplayerGameArena(
                    session = activeMultiplayerSession!!,
                    questions = activeMultiplayerQuestions,
                    currentParticipant = currentParticipant,
                    submissionForCurrentQuestion = submissionForCurrentQuestion,
                    leaderboard = multiplayerLeaderboard,
                    currentUserSession = session,
                    timeRemainingSeconds = uiState.multiplayerTimeRemainingSeconds,
                    lastSubmissionResult = uiState.lastSubmissionResult,
                    scrambledPool = uiState.scrambledPool,
                    selectedScrambleWords = uiState.selectedScrambleWords,
                    onSelectOption = { viewModel.submitMultiplayerAnswer(it) },
                    onSubmitScramble = {
                        val sentence = uiState.selectedScrambleWords.joinToString(" ")
                        viewModel.submitMultiplayerAnswer(sentence)
                    },
                    onTapScrambleWord = { viewModel.tapScrambleWord(it) },
                    onRemoveScrambleWord = { viewModel.removeScrambleWord(it) },
                    onClearScrambleWords = { viewModel.clearScrambleWords() },
                    onStartNow = { viewModel.startMultiplayerGameImmediately(activeMultiplayerSession!!.id) },
                    onExit = { viewModel.leaveMultiplayerArena() }
                )
            } else if (uiState.activeQuiz.quiz != null) {
                // Solo quiz runner
                ActiveQuizRunner(
                    activeQuiz = uiState.activeQuiz,
                    session = session,
                    onSelectOption = { viewModel.selectOption(it) },
                    onNext = { viewModel.nextQuestion() },
                    onExit = { viewModel.exitQuiz() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                val activeLang = com.example.core.localization.LanguageManager.currentLanguage.value
                val appStrings = com.example.core.localization.LanguageManager.strings(activeLang)

                // Pure Games & Trivia Header Banner
                val headerTitle = when (session.userRole) {
                    UserRole.ADMINISTRATOR -> "The Gideonites - The Youth Alive Fellowship • Game Command"
                    UserRole.YOUTH_EXECUTIVE -> "The Gideonites - The Youth Alive Fellowship • Scripture Games"
                    UserRole.YOUTH_MEMBER -> "The Gideonites - The Youth Alive Fellowship • Kingdom Giants"
                    UserRole.GUEST -> "Welcome to The Gideonites - Youth Alive Games"
                }

                val headerSubtitle = when (session.userRole) {
                    UserRole.ADMINISTRATOR -> "Live Multiplayer Showdown"
                    UserRole.YOUTH_EXECUTIVE -> "Live Arena Showdowns"
                    UserRole.YOUTH_MEMBER -> "Live Arena Showdowns"
                    UserRole.GUEST -> "Live Arena Standby"
                }

                val roleAccentColor = when (session.userRole) {
                    UserRole.ADMINISTRATOR -> FlameRedPrimary
                    UserRole.YOUTH_EXECUTIVE -> FlameGoldAccent
                    UserRole.YOUTH_MEMBER -> Color(0xFF10B981)
                    UserRole.GUEST -> Color(0xFF3B82F6)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = GlobeBlack),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("games_header_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(roleAccentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = roleAccentColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.3.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = headerSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Screen Content: Exclusively Games & Trivia Hub
                BibleGamesTabContent(
                    quizzes = quizzes,
                    leaderboard = leaderboard,
                    myScores = myScores,
                    session = session,
                    isGameTimeActive = uiState.isGameTimeActive,
                    liveParticipantsCount = uiState.liveParticipantsCount,
                    upcomingAndActiveSessions = upcomingAndActiveSessions,
                    draftSessions = draftSessions,
                    completedSessions = completedSessions,
                    onEnterLiveGame = { viewModel.enterLiveGameShowdown() },
                    onEnterLiveGameWithMode = { gameType -> viewModel.enterLiveGameShowdown(gameType) },
                    onBroadcastGameTime = { viewModel.broadcastGameTimeAlert() },
                    onJoinMultiplayerSession = { viewModel.joinMultiplayerSession(it) },
                    onOpenCreateMultiplayerGame = {
                        pendingSelectedBibleVerse = null
                        viewModel.setCreateMultiplayerGameOpen(true)
                    },
                    onOpenBibleDatabase = { showBibleDatabaseDialog = true },
                    onStartMultiplayerGameImmediately = { viewModel.startMultiplayerGameImmediately(it) },
                    onCancelMultiplayerGame = { viewModel.cancelMultiplayerGame(it) },
                    onEditDraftGame = { draft ->
                        pendingSelectedBibleVerse = null
                        viewModel.setCreateMultiplayerGameOpen(true, draft)
                    },
                    onLaunchDraftGame = { viewModel.launchDraftSession(it) },
                    onDeleteDraftGame = { viewModel.deleteDraftSession(it) },
                    onStartQuiz = { viewModel.startQuiz(it) },
                    onCreateQuiz = { viewModel.setCreateQuizOpen(true) },
                    onDeleteQuiz = { viewModel.deleteQuiz(it) },
                    onDeleteScore = { viewModel.deleteScore(it) }
                )
            }
        }
    }

    // Dialog: Tailored Multiplayer Game Editor & Scheduling
    if (uiState.isCreateMultiplayerGameOpen) {
        val editingDraft = uiState.editingDraftSession
        CreateMultiplayerGameDialog(
            onDismiss = {
                pendingSelectedBibleVerse = null
                viewModel.setCreateMultiplayerGameOpen(false)
            },
            bibleRepository = viewModel.bibleRepository,
            initialSelectedVerse = pendingSelectedBibleVerse,
            initialDraftSession = editingDraft,
            onSaveDraft = { title, desc, gameType, timeLimit, baseScore, speedBonus, questions ->
                pendingSelectedBibleVerse = null
                viewModel.saveMultiplayerGameDraft(
                    title = title,
                    description = desc,
                    gameType = gameType,
                    timeLimitSeconds = timeLimit,
                    baseScore = baseScore,
                    speedBonusEnabled = speedBonus,
                    questions = questions,
                    existingSessionId = editingDraft?.id
                )
            },
            onPublishAndSchedule = { title, desc, gameType, delayMinutes, timeLimit, baseScore, speedBonus, questions ->
                pendingSelectedBibleVerse = null
                viewModel.scheduleMultiplayerGame(
                    title = title,
                    description = desc,
                    gameType = gameType,
                    delayMinutes = delayMinutes,
                    timeLimitSeconds = timeLimit,
                    baseScore = baseScore,
                    speedBonusEnabled = speedBonus,
                    questions = questions,
                    existingSessionId = editingDraft?.id
                )
            }
        )
    }

    // Dialog: Solo Quiz Creation
    if (uiState.isCreateQuizOpen) {
        CreateQuizDialog(
            onDismiss = { viewModel.setCreateQuizOpen(false) },
            bibleRepository = viewModel.bibleRepository,
            onCreate = { title, desc, cat, diff, limit, questions ->
                viewModel.createQuiz(title, desc, cat, diff, limit, questions)
            }
        )
    }

    // Dialog: Full Bible Scriptures Database & Uploader
    if (showBibleDatabaseDialog) {
        BibleScripturePickerDialog(
            bibleRepository = viewModel.bibleRepository,
            onDismiss = { showBibleDatabaseDialog = false },
            onScriptureSelected = { selectedVerse ->
                pendingSelectedBibleVerse = selectedVerse
                showBibleDatabaseDialog = false
                viewModel.setCreateMultiplayerGameOpen(true)
            },
            initialTargetContext = "The Gideonites - Youth Alive Games"
        )
    }
}
