package com.example.ui.engagement

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import com.example.data.local.entity.MultiplayerGameSessionEntity
import com.example.data.local.entity.MultiplayerParticipantEntity
import com.example.data.local.entity.MultiplayerSubmissionEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.model.GameType
import com.example.data.model.MultiplayerLeaderboardEntry
import com.example.data.model.MultiplayerQuestion
import com.example.data.model.SessionStatus
import com.example.data.model.SessionSubstate
import com.example.data.model.SubmissionResult
import com.example.data.model.UserRole
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver

@Composable
fun MultiplayerGameArena(
    session: MultiplayerGameSessionEntity,
    questions: List<MultiplayerQuestion>,
    currentParticipant: MultiplayerParticipantEntity?,
    submissionForCurrentQuestion: MultiplayerSubmissionEntity?,
    leaderboard: List<MultiplayerLeaderboardEntry>,
    currentUserSession: SessionEntity,
    timeRemainingSeconds: Int,
    lastSubmissionResult: SubmissionResult?,
    scrambledPool: List<String>,
    selectedScrambleWords: List<String>,
    onSelectOption: (String) -> Unit,
    onSubmitScramble: () -> Unit,
    onTapScrambleWord: (String) -> Unit,
    onRemoveScrambleWord: (Int) -> Unit,
    onClearScrambleWords: () -> Unit,
    onStartNow: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onExit)
    val activeLang by com.example.core.localization.LanguageManager.currentLanguage
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("multiplayer_game_arena")
    ) {
        when {
            // 0. Session Cancelled by Host
            session.status == SessionStatus.CANCELLED -> {
                MultiplayerCancelledView(
                    session = session,
                    onExit = onExit
                )
            }

            // 1. Session Completed: Final Podium View
            session.status == SessionStatus.COMPLETED -> {
                MultiplayerPodiumView(
                    session = session,
                    leaderboard = leaderboard,
                    currentUserSession = currentUserSession,
                    onExit = onExit
                )
            }

            // 2. Scheduled or Waiting to Start: Lobby Countdown View
            session.status == SessionStatus.SCHEDULED || session.currentSubstate == SessionSubstate.WAITING_TO_START -> {
                MultiplayerLobbyView(
                    session = session,
                    questionsCount = questions.size,
                    participantCount = leaderboard.size,
                    timeRemainingSeconds = timeRemainingSeconds,
                    isAdmin = currentUserSession.userRole == UserRole.ADMINISTRATOR || currentUserSession.userRole == UserRole.YOUTH_EXECUTIVE,
                    onStartNow = onStartNow,
                    onExit = onExit
                )
            }

            // 3. Question Open: Synchronized Round Gameplay View
            session.currentSubstate == SessionSubstate.QUESTION_OPEN -> {
                val rawQ = questions.getOrNull(session.currentQuestionIndex)
                val currentQ = rawQ?.let { com.example.data.model.PresetGameSessions.localizeMultiplayerQuestion(it, activeLang) }
                if (currentQ != null) {
                    SynchronizedQuestionView(
                        session = session,
                        question = currentQ,
                        questionIndex = session.currentQuestionIndex,
                        totalQuestions = session.totalQuestions,
                        timeRemainingSeconds = timeRemainingSeconds,
                        currentParticipant = currentParticipant,
                        submission = submissionForCurrentQuestion,
                        lastResult = lastSubmissionResult,
                        scrambledPool = scrambledPool,
                        selectedScrambleWords = selectedScrambleWords,
                        onSelectOption = onSelectOption,
                        onSubmitScramble = onSubmitScramble,
                        onTapScrambleWord = onTapScrambleWord,
                        onRemoveScrambleWord = onRemoveScrambleWord,
                        onClearScrambleWords = onClearScrambleWords,
                        onExit = onExit
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading next round...", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // 4. Intermission: Leaderboard updating after every question
            session.currentSubstate == SessionSubstate.LEADERBOARD -> {
                val rawQ = questions.getOrNull(session.currentQuestionIndex)
                val currentQ = rawQ?.let { com.example.data.model.PresetGameSessions.localizeMultiplayerQuestion(it, activeLang) }
                MultiplayerIntermissionLeaderboardView(
                    session = session,
                    question = currentQ,
                    questionIndex = session.currentQuestionIndex,
                    totalQuestions = session.totalQuestions,
                    timeRemainingSeconds = timeRemainingSeconds,
                    submission = submissionForCurrentQuestion,
                    leaderboard = leaderboard,
                    currentUserSession = currentUserSession,
                    onExit = onExit
                )
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Synchronizing game arena...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MultiplayerCancelledView(
    session: MultiplayerGameSessionEntity,
    onExit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("multiplayer_cancelled_view")
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Game Session Cancelled",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\"${session.title}\" was ended by leadership.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onExit,
            colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_exit_cancelled_session")
        ) {
            Text("Return to Games Hub", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Lobby View when game is waiting to start automatically.
 */
@Composable
private fun MultiplayerLobbyView(
    session: MultiplayerGameSessionEntity,
    questionsCount: Int,
    participantCount: Int,
    timeRemainingSeconds: Int,
    isAdmin: Boolean,
    onStartNow: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = FlameGoldAccent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FlameGoldAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MULTIPLAYER LOBBY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = GlobeBlack
                    )
                }
            }

            IconButton(onClick = onExit) {
                Icon(Icons.Default.Close, contentDescription = "Exit Lobby")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Game Emblem
        Surface(
            shape = CircleShape,
            color = FlameRedPrimary.copy(alpha = 0.12f),
            modifier = Modifier.size(84.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (session.gameType) {
                        GameType.TRIVIA_CHALLENGE -> Icons.Default.SportsEsports
                        GameType.WHO_AM_I -> Icons.Default.HelpOutline
                        GameType.SCRIPTURE_SCRAMBLE -> Icons.Default.AutoStories
                    },
                    contentDescription = null,
                    tint = FlameRedPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = session.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "${session.gameType.displayName} • $questionsCount Rounds",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = session.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Countdown Card
        Card(
            colors = CardDefaults.cardColors(containerColor = GlobeBlack),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "SYNCHRONIZED AUTO-START",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FlameGoldAccent,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                val formattedTime = if (timeRemainingSeconds > 0) {
                    val m = timeRemainingSeconds / 60
                    val s = timeRemainingSeconds % 60
                    String.format("%02d:%02d", m, s)
                } else {
                    "00:00"
                }

                Text(
                    text = formattedTime,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (timeRemainingSeconds > 0) "Starting soon..." else "Starting Round 1...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Participants Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = FlameGoldAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val playerWord = if (participantCount.coerceAtLeast(1) == 1) "Player" else "Players"
                Text(
                    text = "${participantCount.coerceAtLeast(1)} Connected $playerWord",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Rules summary card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Game Info:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("• ${session.timeLimitPerQuestionSeconds} seconds per question", style = MaterialTheme.typography.bodySmall)
                Text("• ${session.baseScorePerQuestion} base points per correct answer", style = MaterialTheme.typography.bodySmall)
                if (session.speedBonusEnabled) {
                    Text("• Up to +${session.maxSpeedBonus} speed bonus points", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isAdmin) {
            Button(
                onClick = onStartNow,
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_lobby_start_now")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("🚀 Start Game Now", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = onExit,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Leave Lobby")
        }
    }
}

/**
 * Question Active View tailored to each game type with live timer and late entry support.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SynchronizedQuestionView(
    session: MultiplayerGameSessionEntity,
    question: MultiplayerQuestion,
    questionIndex: Int,
    totalQuestions: Int,
    timeRemainingSeconds: Int,
    currentParticipant: MultiplayerParticipantEntity?,
    submission: MultiplayerSubmissionEntity?,
    lastResult: SubmissionResult?,
    scrambledPool: List<String>,
    selectedScrambleWords: List<String>,
    onSelectOption: (String) -> Unit,
    onSubmitScramble: () -> Unit,
    onTapScrambleWord: (String) -> Unit,
    onRemoveScrambleWord: (Int) -> Unit,
    onClearScrambleWords: () -> Unit,
    onExit: () -> Unit
) {
    val totalTime = session.timeLimitPerQuestionSeconds.coerceAtLeast(1)
    val progress = (timeRemainingSeconds.toFloat() / totalTime.toFloat()).coerceIn(0f, 1f)

    val timerColor by animateColorAsState(
        targetValue = when {
            timeRemainingSeconds > 10 -> Color(0xFF10B981) // Green
            timeRemainingSeconds > 5 -> FlameGoldAccent // Amber
            else -> FlameRedPrimary // Red
        },
        label = "timerColor"
    )

    // Check if user is a late joiner for this specific question
    val isLateForThisQuestion = currentParticipant != null && questionIndex < currentParticipant.joinedAtQuestionIndex
    val isAlreadySubmitted = submission != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Navigation Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FlameRedPrimary
                ) {
                    Text(
                        text = "ROUND ${questionIndex + 1} OF $totalQuestions",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = session.gameType.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FlameGoldAccent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = com.example.core.localization.LanguageManager.currentLanguage.value.shortBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = GlobeBlack),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            IconButton(onClick = onExit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Exit")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Late Entry Alert Banner
        if (isLateForThisQuestion) {
            Surface(
                color = FlameGoldAccent.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = GlobeBlack, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚡ Joined at Round ${currentParticipant.joinedAtQuestionIndex + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GlobeBlack
                    )
                }
            }
        }

        // Live Timer Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GlobeBlack),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TIME REMAINING",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ShieldSilver)
                        )
                    }

                    Text(
                        text = "${timeRemainingSeconds}s",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = timerColor)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = timerColor,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ================= TAILORED GAMEPLAY CONTENT =================
        when (session.gameType) {
            // 1. TRIVIA CHALLENGE GAMEPLAY
            GameType.TRIVIA_CHALLENGE -> {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        if (question.scriptureReference.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FlameGoldAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = question.scriptureReference,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GlobeBlack,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = question.prompt,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isAlreadySubmitted) "YOUR ANSWER:" else "SELECT YOUR ANSWER:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = ShieldSilver)
                )

                Spacer(modifier = Modifier.height(8.dp))

                question.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = submission?.submittedAnswer?.trim()?.equals(optionText.trim(), ignoreCase = true) == true
                    val isLocked = isAlreadySubmitted || isLateForThisQuestion || timeRemainingSeconds <= 0

                    Surface(
                        onClick = {
                            if (!isLocked) onSelectOption(optionText)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isSelected -> FlameRedPrimary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) FlameRedPrimary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("game_option_$optIndex")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) FlameRedPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${('A' + optIndex)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Surface(
                                    color = GlobeBlack,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = FlameGoldAccent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "SELECTED",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = FlameGoldAccent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. WHO AM I? GAMEPLAY (PROGRESSIVE CLUES)
            GameType.WHO_AM_I -> {
                val numClues = question.clues.size.coerceAtLeast(1)
                val timeElapsed = (totalTime - timeRemainingSeconds).coerceAtLeast(0)
                val clueInterval = (totalTime.toFloat() / numClues.toFloat()).coerceAtLeast(1f)
                val currentClueIndex = (timeElapsed / clueInterval).toInt().coerceIn(0, numClues - 1)
                val revealedCount = (currentClueIndex + 1).coerceIn(1, numClues)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🕵️ WHO AM I?",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = FlameRedPrimary)
                            )

                            // Dynamic Point Bonus Pill synchronized with server clueMultiplier
                            val clueMultiplierText = when (currentClueIndex) {
                                0 -> "🌟 150% Clue 1 Max Points!"
                                1 -> "⚡ 125% Clue 2 Points"
                                else -> "Standard Points"
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FlameGoldAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = clueMultiplierText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = question.prompt,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "PROGRESSIVE CLUES ($revealedCount of $numClues REVEALED):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = ShieldSilver)
                )

                Spacer(modifier = Modifier.height(6.dp))

                question.clues.forEachIndexed { clueIdx, clueText ->
                    val isRevealed = clueIdx < revealedCount
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRevealed) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (isRevealed) BorderStroke(1.dp, FlameGoldAccent.copy(alpha = 0.5f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isRevealed) FlameGoldAccent else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${clueIdx + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GlobeBlack)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            if (isRevealed) {
                                Text(
                                    text = clueText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = ShieldSilver, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Clue ${clueIdx + 1} unlocks as time runs...",
                                        style = MaterialTheme.typography.bodySmall.copy(color = ShieldSilver)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Character Options to pick
                Text(
                    text = "IDENTIFY THE CHARACTER:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = ShieldSilver)
                )
                Spacer(modifier = Modifier.height(6.dp))

                question.options.forEachIndexed { optIndex, choice ->
                    val isSelected = submission?.submittedAnswer?.trim()?.equals(choice.trim(), ignoreCase = true) == true
                    val isLocked = isAlreadySubmitted || isLateForThisQuestion || timeRemainingSeconds <= 0

                    Surface(
                        onClick = {
                            if (!isLocked) onSelectOption(choice)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) FlameRedPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) FlameRedPrimary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("who_choice_$optIndex")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = choice,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Surface(
                                    color = GlobeBlack,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = FlameGoldAccent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "SELECTED",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = FlameGoldAccent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SCRIPTURE SCRAMBLE GAMEPLAY
            GameType.SCRIPTURE_SCRAMBLE -> {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FlameGoldAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${question.scriptureReference} • ${question.translation}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = GlobeBlack,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "🧩 Scripture Scramble",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tap words below in the correct biblical order to reconstruct the verse:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reconstructed Tray
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "RECONSTRUCTED VERSE TRAY:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = GlobeBlack)
                            )

                            if (selectedScrambleWords.isNotEmpty() && !isAlreadySubmitted) {
                                TextButton(
                                    onClick = onClearScrambleWords,
                                    modifier = Modifier.size(height = 28.dp, width = 70.dp)
                                ) {
                                    Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedScrambleWords.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tap word tiles below to place them here...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ShieldSilver
                                )
                            }
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                selectedScrambleWords.forEachIndexed { wordIdx, word ->
                                    Surface(
                                        onClick = {
                                            if (!isAlreadySubmitted && !isLateForThisQuestion) {
                                                onRemoveScrambleWord(wordIdx)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = FlameGoldAccent.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, FlameGoldAccent)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = word,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            if (!isAlreadySubmitted) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Available Scrambled Words Pool
                Text(
                    text = "AVAILABLE WORD TILES:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = ShieldSilver)
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    scrambledPool.forEach { word ->
                        Surface(
                            onClick = {
                                if (!isAlreadySubmitted && !isLateForThisQuestion && timeRemainingSeconds > 0) {
                                    onTapScrambleWord(word)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("scramble_tile_$word")
                        ) {
                            Text(
                                text = word,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isAlreadySubmitted) {
                    Button(
                        onClick = onSubmitScramble,
                        enabled = selectedScrambleWords.isNotEmpty() && !isLateForThisQuestion && timeRemainingSeconds > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_submit_scramble")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Verse", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isAlreadySubmitted) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GlobeBlack),
                border = BorderStroke(1.5.dp, FlameGoldAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FlameGoldAccent.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FlameGoldAccent, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Answer Submitted • ${timeRemainingSeconds}s remaining",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White)
                    )
                }
            }
        }
    }
}

/**
 * Live Leaderboard Intermission View:
 * Simultaneous Reveal of Correct Answer, Bigger Countdown Timer to Next Question,
 * ONLY Top 5 Users Shown on Leaderboard with Rank Animations, and Individual Standing Card.
 */
@Composable
private fun MultiplayerIntermissionLeaderboardView(
    session: MultiplayerGameSessionEntity,
    question: MultiplayerQuestion?,
    questionIndex: Int,
    totalQuestions: Int,
    timeRemainingSeconds: Int,
    submission: MultiplayerSubmissionEntity?,
    leaderboard: List<MultiplayerLeaderboardEntry>,
    currentUserSession: SessionEntity,
    onExit: () -> Unit
) {
    val isLastRound = questionIndex + 1 >= totalQuestions
    val maxIntermission = session.leaderboardDurationSeconds.coerceAtLeast(1)
    val countdownProgress by animateFloatAsState(
        targetValue = (timeRemainingSeconds.toFloat() / maxIntermission.toFloat()).coerceIn(0f, 1f),
        label = "mp_intermission_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. BIGGER COUNTDOWN TIMER TO NEXT QUESTION
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = GlobeBlack),
            border = BorderStroke(1.5.dp, FlameGoldAccent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mp_next_question_big_timer")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = if (isLastRound) "FINAL PODIUM IN" else "NEXT QUESTION STARTS IN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = FlameGoldAccent,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Round ${questionIndex + 1} of $totalQuestions Complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShieldSilver
                        )
                    }

                    // Extra-Large Countdown Badge
                    Surface(
                        color = FlameRedPrimary.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, FlameRedPrimary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = FlameGoldAccent,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${timeRemainingSeconds}s",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { countdownProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = FlameGoldAccent,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. SIMULTANEOUS CORRECT ANSWER & PARTICIPANT RESULT REVEAL CARD
        if (question != null) {
            val didSubmit = submission != null
            val isCorrect = submission?.isCorrect == true
            val pointsEarned = submission?.pointsAwarded ?: 0

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCorrect -> Color(0xFF10B981)
                        didSubmit -> Color(0xFFEF4444)
                        else -> Color(0xFFF59E0B)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                isCorrect -> Icons.Default.CheckCircle
                                didSubmit -> Icons.Default.Close
                                else -> Icons.Default.HourglassBottom
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isCorrect -> "TIME'S UP — CORRECT! (+$pointsEarned PTS) 🎉"
                                didSubmit -> "TIME'S UP — INCORRECT (+0 PTS) ❌"
                                else -> "TIME'S UP — NO ANSWER (+0 PTS) ⏰"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color.Black.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = "CORRECT ANSWER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    letterSpacing = 0.8.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "✓ ${question.correctAnswer}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                ),
                                textAlign = TextAlign.Center
                            )
                            if (submission != null && !isCorrect) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Your answer: ${submission.submittedAnswer}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    if (question.scriptureReference.isNotBlank() || question.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = listOf(question.scriptureReference, question.explanation).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 3. TOP 5 USERS SHOWN ON THE PUBLIC LEADERBOARD (WITH RANK ANIMATIONS)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = FlameGoldAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TOP 5 LEADERBOARD",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = GlobeBlack)
                )
            }
            Surface(
                color = FlameGoldAccent.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "TOP 5 OF ${leaderboard.size}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = GlobeBlack),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        leaderboard.take(5).forEach { entry ->
            key(entry.userId) {
                AnimatedMultiplayerLeaderboardRow(
                    entry = entry,
                    isCurrent = entry.userId == currentUserSession.userId
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. INDIVIDUAL STANDING CARD ON THE PARTICIPANT'S OWN SCREEN
        val myEntry = leaderboard.find { it.userId == currentUserSession.userId }
        val totalPlayers = leaderboard.size.coerceAtLeast(1)
        val myRank = myEntry?.rank ?: totalPlayers
        val myScore = myEntry?.score ?: 0
        val myMovement = myEntry?.rankMovement ?: 0
        val myLastPoints = myEntry?.pointsFromLastQuestion ?: 0
        val isInTop5 = myRank <= 5
        val fifthScore = leaderboard.getOrNull(4)?.score ?: 0
        val pointsToTop5 = (fifthScore - myScore).coerceAtLeast(0)

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GlobeBlack),
            border = BorderStroke(1.5.dp, if (isInTop5) FlameGoldAccent else FlameRedPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mp_individual_standing_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = FlameGoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "YOUR STANDING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = FlameGoldAccent,
                                letterSpacing = 0.6.sp
                            )
                        )
                    }

                    Surface(
                        color = if (isInTop5) Color(0xFF10B981).copy(alpha = 0.2f) else FlameRedPrimary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isInTop5) "★ TOP 5" else "RANK #$myRank",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isInTop5) Color(0xFF10B981) else Color(0xFFFCA5A5)
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Rank #$myRank of $totalPlayers",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        if (!isInTop5 && pointsToTop5 > 0) {
                            Text(
                                text = "$pointsToTop5 pts from Top 5",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldSilver
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$myScore pts" + if (myLastPoints > 0) " (+$myLastPoints)" else "",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = FlameGoldAccent
                            )
                        )
                        val moveLabel = when {
                            myMovement > 0 -> "▲ Moved Up +$myMovement"
                            myMovement < 0 -> "▼ Dropped -${abs(myMovement)}"
                            else -> "• Held Rank #$myRank"
                        }
                        val moveColor = when {
                            myMovement > 0 -> Color(0xFF10B981)
                            myMovement < 0 -> Color(0xFFEF4444)
                            else -> ShieldSilver
                        }
                        Text(
                            text = moveLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = moveColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Exit to Games Hub")
        }
    }
}

@Composable
private fun AnimatedMultiplayerLeaderboardRow(
    entry: MultiplayerLeaderboardEntry,
    isCurrent: Boolean
) {
    val movement = entry.rankMovement

    // Vertical slide animation when climbing past someone (+offset -> 0) or dropping down (-offset -> 0)
    val verticalOffsetPx = remember(entry.rank, movement, entry.score) {
        Animatable((movement.coerceIn(-4, 4) * 32f))
    }
    val rowScale = remember(entry.rank, movement, entry.score) {
        Animatable(if (movement != 0) 1.03f else 1.0f)
    }

    LaunchedEffect(entry.rank, movement, entry.score) {
        verticalOffsetPx.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(entry.rank, movement, entry.score) {
        if (movement != 0) {
            rowScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 550)
            )
        }
    }

    val borderColor by animateColorAsState(
        targetValue = when {
            movement > 0 -> Color(0xFF10B981)
            movement < 0 -> Color(0xFFEF4444)
            isCurrent -> FlameGoldAccent
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(durationMillis = 450),
        label = "mp_row_border"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrent -> FlameGoldAccent.copy(alpha = 0.15f)
                movement > 0 -> Color(0xFF10B981).copy(alpha = 0.08f)
                movement < 0 -> Color(0xFFEF4444).copy(alpha = 0.05f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isCurrent || movement != 0) 1.5.dp else 0.5.dp,
            color = borderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .offset { IntOffset(0, verticalOffsetPx.value.roundToInt()) }
            .scale(rowScale.value)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            val rankBadge = when (entry.rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "#${entry.rank}"
            }

            Text(
                text = rankBadge,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(36.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.userName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = FlameRedPrimary
                        ) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${entry.userRole} • ${entry.answeredCount} answered",
                    style = MaterialTheme.typography.labelSmall,
                    color = ShieldSilver
                )
            }

            // Rank movement badge (▲ +N / ▼ -N)
            val badgeText = when {
                movement > 0 -> "▲ +$movement"
                movement < 0 -> "▼ -${abs(movement)}"
                else -> "• 0"
            }
            val badgeColor = when {
                movement > 0 -> Color(0xFF10B981)
                movement < 0 -> Color(0xFFEF4444)
                else -> ShieldSilver
            }
            Surface(
                color = badgeColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.score} pts",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = FlameRedPrimary)
                )
                if (entry.pointsFromLastQuestion > 0) {
                    Text(
                        text = "+${entry.pointsFromLastQuestion} round",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    )
                }
            }
        }
    }
}

/**
 * Final Victory Podium View when all rounds are completed.
 */
@Composable
private fun MultiplayerPodiumView(
    session: MultiplayerGameSessionEntity,
    leaderboard: List<MultiplayerLeaderboardEntry>,
    currentUserSession: SessionEntity,
    onExit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "👑 TOURNAMENT COMPLETED 👑",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = FlameGoldAccent,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = session.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Top 3 Podium
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            val first = leaderboard.getOrNull(0)
            val second = leaderboard.getOrNull(1)
            val third = leaderboard.getOrNull(2)

            // Second Place (Left)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("🥈", fontSize = 28.sp)
                Text(second?.userName ?: "—", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                Text("${second?.score ?: 0} pts", style = MaterialTheme.typography.labelSmall, color = ShieldSilver)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    color = ShieldSilver.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("#2", fontWeight = FontWeight.Bold, color = GlobeBlack)
                    }
                }
            }

            // First Place (Center, Tallest)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("👑", fontSize = 36.sp)
                Text(first?.userName ?: "—", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
                Text("${first?.score ?: 0} pts", style = MaterialTheme.typography.labelSmall.copy(color = FlameGoldAccent, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    color = FlameGoldAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("#1 🏆", fontWeight = FontWeight.ExtraBold, color = GlobeBlack)
                    }
                }
            }

            // Third Place (Right)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("🥉", fontSize = 28.sp)
                Text(third?.userName ?: "—", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                Text("${third?.score ?: 0} pts", style = MaterialTheme.typography.labelSmall, color = ShieldSilver)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    color = Color(0xFFCD7F32).copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("#3", fontWeight = FontWeight.Bold, color = GlobeBlack)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Current User Standing
        val myEntry = leaderboard.find { it.userId == currentUserSession.userId }
        if (myEntry != null) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GlobeBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "YOUR FINAL RESULT",
                            style = MaterialTheme.typography.labelSmall.copy(color = FlameGoldAccent, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Rank #${myEntry.rank} of ${leaderboard.size}",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "${myEntry.score} PTS",
                        style = MaterialTheme.typography.headlineSmall.copy(color = FlameRedPrimary, fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onExit,
            colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Return to Fellowship Games Hub", fontWeight = FontWeight.Bold)
        }
    }
}
