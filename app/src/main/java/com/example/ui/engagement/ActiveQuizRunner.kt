package com.example.ui.engagement

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SessionEntity
import com.example.data.model.UserRole
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ActiveQuizRunner(
    activeQuiz: ActiveQuizState,
    session: SessionEntity,
    onSelectOption: (Int) -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onExit)
    when {
        activeQuiz.isFinished -> {
            LiveGamePodiumView(
                activeQuiz = activeQuiz,
                session = session,
                onExit = onExit,
                modifier = modifier
            )
        }
        activeQuiz.isIntermissionLeaderboard -> {
            LiveIntermissionLeaderboardView(
                activeQuiz = activeQuiz,
                session = session,
                onNext = onNext,
                onExit = onExit,
                modifier = modifier
            )
        }
        else -> {
            LiveQuestionActiveView(
                activeQuiz = activeQuiz,
                session = session,
                onSelectOption = onSelectOption,
                onExit = onExit,
                modifier = modifier
            )
        }
    }
}

/**
 * 1. Active Question Gameplay:
 * User locks in their answer, but correctness & points are NOT revealed until the timer reaches 0!
 */
@Composable
private fun LiveQuestionActiveView(
    activeQuiz: ActiveQuizState,
    session: SessionEntity,
    onSelectOption: (Int) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLang = com.example.core.localization.LanguageManager.currentLanguage.value
    val appStrings = com.example.core.localization.LanguageManager.strings(activeLang)
    val rawQuestion = activeQuiz.questions.getOrNull(activeQuiz.currentQuestionIndex) ?: return
    val currentQuestion = com.example.data.model.PresetGameSessions.localizeQuizQuestion(rawQuestion, activeLang)
    val total = activeQuiz.questions.size
    val timeLeft = activeQuiz.timeRemainingSeconds
    val maxTime = activeQuiz.maxQuestionTimeSeconds.coerceAtLeast(1)
    val lockedOptionIndex = activeQuiz.selectedOptionIndex
    val isAnswerLocked = lockedOptionIndex != null

    val timerProgress by animateFloatAsState(
        targetValue = (timeLeft.toFloat() / maxTime.toFloat()).coerceIn(0f, 1f),
        label = "timer_progress"
    )

    val timerColor = when {
        timeLeft <= 4 -> Color(0xFFEF4444) // Urgent red
        timeLeft <= 8 -> FlameGoldAccent // Warning amber
        else -> Color(0xFF10B981) // Normal emerald green
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header: Question count, Timer pill, total game points, Exit
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Question ${activeQuiz.currentQuestionIndex + 1} of $total",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FlameRedPrimary
                )
                Text(
                    text = "${activeQuiz.totalGamePoints} pts",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = FlameGoldAccent
                )
            }

            // Animated Countdown Timer Badge
            Surface(
                color = timerColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, timerColor),
                modifier = Modifier.testTag("timer_badge")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = timerColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${timeLeft}s",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = timerColor
                    )
                }
            }

            IconButton(onClick = onExit) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Quiz")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Timer Progress Bar
        LinearProgressIndicator(
            progress = { timerProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = timerColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Question Prompt Card
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = currentQuestion.question,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlobeBlack,
                        lineHeight = 24.sp
                    )
                )

                if (currentQuestion.scriptureRef.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = FlameRedPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentQuestion.scriptureRef,
                                style = MaterialTheme.typography.labelSmall,
                                color = FlameRedPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4 Fast-Action Option Cards (Red, Blue, Amber, Green)
        val optionColors = listOf(
            Color(0xFFE21B3C), // Red (Triangle ▲)
            Color(0xFF1368CE), // Blue (Diamond ◆)
            Color(0xFFD89E00), // Amber (Circle ●)
            Color(0xFF26890C)  // Green (Square ■)
        )
        val optionSymbols = listOf("▲", "◆", "●", "■")

        currentQuestion.options.forEachIndexed { index, option ->
            val baseBg = optionColors.getOrElse(index) { Color(0xFF6B7280) }
            val symbol = optionSymbols.getOrElse(index) { "${'A' + index}" }
            val isSelected = lockedOptionIndex == index
            val cardAlpha = if (isAnswerLocked && !isSelected) 0.45f else 1f

            Surface(
                color = baseBg,
                shape = RoundedCornerShape(14.dp),
                border = if (isSelected) BorderStroke(3.dp, FlameGoldAccent) else null,
                shadowElevation = if (isSelected) 6.dp else 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .alpha(cardAlpha)
                    .clickable(enabled = !isAnswerLocked && timeLeft > 0) { onSelectOption(index) }
                    .testTag("option_button_$index")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Surface(
                            color = GlobeBlack.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = FlameGoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SELECTED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FlameGoldAccent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedVisibility(
            visible = isAnswerLocked,
            enter = fadeIn() + slideInVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GlobeBlack),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, FlameGoldAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("answer_locked_waiting_banner")
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
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FlameGoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Answer Submitted • ${timeLeft}s remaining",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

/**
 * 2. Intermission: Simultaneous Reveal of Correct Answer, BIG Countdown Timer to Next Question,
 *    Top 5 Public Leaderboard with Subtle Rank Movement Animations, and Individual Standing Card.
 */
@Composable
private fun LiveIntermissionLeaderboardView(
    activeQuiz: ActiveQuizState,
    session: SessionEntity,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLang = com.example.core.localization.LanguageManager.currentLanguage.value
    val total = activeQuiz.questions.size
    val isLast = activeQuiz.currentQuestionIndex + 1 == total
    val rawQuestion = activeQuiz.questions.getOrNull(activeQuiz.currentQuestionIndex)
    val question = rawQuestion?.let { com.example.data.model.PresetGameSessions.localizeQuizQuestion(it, activeLang) }
    val correctOptionText = question?.options?.getOrNull(question.correctAnswerIndex) ?: ""
    val selectedOptionText = activeQuiz.selectedOptionIndex?.let { idx -> question?.options?.getOrNull(idx) }
    val isCorrect = activeQuiz.pointsAwardedLastQuestion > 0
    val isTimeout = activeQuiz.isTimeUp
    val nextCountdown = activeQuiz.intermissionTimeRemainingSeconds
    val maxIntermission = activeQuiz.maxIntermissionTimeSeconds.coerceAtLeast(1)
    val totalParticipants = activeQuiz.competitors.size.coerceAtLeast(1)

    val intermissionProgress by animateFloatAsState(
        targetValue = (nextCountdown.toFloat() / maxIntermission.toFloat()).coerceIn(0f, 1f),
        label = "intermission_countdown_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. BIGGER COUNTDOWN TIMER TO NEXT QUESTION
        Card(
            colors = CardDefaults.cardColors(containerColor = GlobeBlack),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, FlameGoldAccent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("next_question_big_timer_card")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
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
                            text = if (isLast) "FINAL PODIUM REVEAL IN" else "NEXT QUESTION STARTS IN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = FlameGoldAccent,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Question ${activeQuiz.currentQuestionIndex + 1} of $total Complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShieldSilver
                        )
                    }

                    // Extra-Large Countdown Display
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
                                text = "${nextCountdown}s",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { intermissionProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = FlameGoldAccent,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. SIMULTANEOUS CORRECT ANSWER & USER OUTCOME REVEAL BANNER
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isCorrect -> Color(0xFF10B981) // Emerald Green
                    isTimeout -> Color(0xFFF59E0B) // Amber
                    else -> Color(0xFFEF4444) // Crimson Red
                }
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("round_result_banner")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when {
                            isCorrect -> Icons.Default.CheckCircle
                            isTimeout -> Icons.Default.HourglassBottom
                            else -> Icons.Default.Close
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isCorrect -> "TIME'S UP — YOU GOT IT RIGHT! 🎉"
                            isTimeout -> "TIME'S UP — NO ANSWER ⏰"
                            else -> "TIME'S UP — INCORRECT ANSWER ❌"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Official Correct Answer Pill shown to ALL users
                if (correctOptionText.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                                text = "✓ $correctOptionText",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                ),
                                textAlign = TextAlign.Center
                            )
                            if (selectedOptionText != null && !isCorrect) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Your answer: $selectedOptionText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                if (isCorrect) {
                    Text(
                        text = "+${activeQuiz.pointsAwardedLastQuestion} pts earned (includes +${activeQuiz.speedBonusLastQuestion} speed bonus)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "+0 pts this round • Get ready for the next question!",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. TOP 5 SHARED LEADERBOARD WITH SUBTLE RANK-MOVEMENT ANIMATIONS
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = FlameGoldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Top 5 Leaderboard",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = GlobeBlack
                )
            }
            Surface(
                color = FlameGoldAccent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "TOP 5",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = GlobeBlack
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Top 5 users shown on the main leaderboard
        activeQuiz.competitors.take(5).forEachIndexed { index, comp ->
            val rank = index + 1
            key(comp.id) {
                AnimatedQuizLeaderboardRow(
                    rank = rank,
                    competitor = comp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. INDIVIDUAL STANDING CARD
        val isInTop5 = activeQuiz.currentUserRank <= 5
        val fifthPlaceScore = activeQuiz.competitors.getOrNull(4)?.score ?: 0
        val pointsBehindTop5 = (fifthPlaceScore - activeQuiz.totalGamePoints).coerceAtLeast(0)

        Card(
            colors = CardDefaults.cardColors(containerColor = GlobeBlack),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (isInTop5) FlameGoldAccent else FlameRedPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("individual_standing_card")
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
                            text = if (isInTop5) "★ TOP 5" else "RANK #${activeQuiz.currentUserRank}",
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
                            text = "Rank #${activeQuiz.currentUserRank} of $totalParticipants",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        if (!isInTop5 && pointsBehindTop5 > 0) {
                            Text(
                                text = "$pointsBehindTop5 pts from Top 5",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldSilver
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${activeQuiz.totalGamePoints} pts",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = FlameGoldAccent
                            )
                        )
                        val rankChangeText = when {
                            activeQuiz.currentUserRankMovement > 0 -> "▲ Moved Up +${activeQuiz.currentUserRankMovement}"
                            activeQuiz.currentUserRankMovement < 0 -> "▼ Dropped ${activeQuiz.currentUserRankMovement}"
                            else -> "• Held Rank #${activeQuiz.currentUserRank}"
                        }
                        val rankChangeColor = when {
                            activeQuiz.currentUserRankMovement > 0 -> Color(0xFF10B981)
                            activeQuiz.currentUserRankMovement < 0 -> Color(0xFFEF4444)
                            else -> ShieldSilver
                        }
                        Text(
                            text = rankChangeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = rankChangeColor
                        )
                    }
                }
            }
        }

        // Scripture citation explanation if available
        if (question != null && question.scriptureRef.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = FlameRedPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scripture Insight: ${question.scriptureRef}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = GlobeBlack
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Next Question Button (also auto-advances when countdown reaches 0)
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_next_question")
        ) {
            Text(
                text = if (isLast) "See Final Results (${nextCountdown}s) 🏆" else "Skip Wait → Next Question (${nextCountdown}s)"
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Animated Leaderboard Row that subtly slides up when a player moves past another player
 * or slides down when dropping from their previous position.
 */
@Composable
private fun AnimatedQuizLeaderboardRow(
    rank: Int,
    competitor: ArenaCompetitor
) {
    val isUser = competitor.isCurrentUser
    val movement = competitor.rankMovement

    // Vertical slide animation: positive movement (climbed up) starts lower (+offset) and slides UP to 0;
    // negative movement (dropped down) starts higher (-offset) and slides DOWN to 0.
    val verticalOffsetPx = remember(rank, movement, competitor.score) {
        Animatable((movement.coerceIn(-4, 4) * 32f))
    }
    val rowScale = remember(rank, movement, competitor.score) {
        Animatable(if (movement != 0) 1.03f else 1.0f)
    }

    LaunchedEffect(rank, movement, competitor.score) {
        verticalOffsetPx.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(rank, movement, competitor.score) {
        if (movement != 0) {
            rowScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 550)
            )
        }
    }

    val movementAccentColor by animateColorAsState(
        targetValue = when {
            movement > 0 -> Color(0xFF10B981) // Climbed past someone
            movement < 0 -> Color(0xFFEF4444) // Dropped down
            isUser -> FlameGoldAccent
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(durationMillis = 450),
        label = "movement_border_color"
    )

    Surface(
        color = when {
            isUser -> FlameGoldAccent.copy(alpha = 0.15f)
            movement > 0 -> Color(0xFF10B981).copy(alpha = 0.07f)
            movement < 0 -> Color(0xFFEF4444).copy(alpha = 0.05f)
            else -> MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isUser || movement != 0) 1.5.dp else 0.5.dp,
            color = movementAccentColor
        ),
        shadowElevation = if (isUser || movement > 0) 2.dp else 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .offset { IntOffset(0, verticalOffsetPx.value.roundToInt()) }
            .scale(rowScale.value)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> FlameGoldAccent
                            2 -> Color(0xFF94A3B8)
                            3 -> Color(0xFFB45309)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = competitor.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.SemiBold
                        ),
                        color = GlobeBlack
                    )
                    if (isUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = FlameRedPrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "★ YOU",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = competitor.unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = ShieldSilver
                    )
                    if (movement != 0 && competitor.previousRank > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (movement > 0) "• Moved up from #${competitor.previousRank}" else "• Dropped from #${competitor.previousRank}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = if (movement > 0) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
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
                    text = "${competitor.score} pts",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (isUser) FlameRedPrimary else GlobeBlack
                )
                if (competitor.lastPoints > 0) {
                    Text(
                        text = "+${competitor.lastPoints}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

/**
 * 3. Grand Live Podium View at Game End (Top 5 Final Leaderboard + Individual Standing)
 */
@Composable
private fun LiveGamePodiumView(
    activeQuiz: ActiveQuizState,
    session: SessionEntity,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topCompetitors = activeQuiz.competitors.take(3)
    val first = topCompetitors.getOrNull(0)
    val second = topCompetitors.getOrNull(1)
    val third = topCompetitors.getOrNull(2)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_podium_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Fellowship Showdown Podium 🏆",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = GlobeBlack,
                textAlign = TextAlign.Center
            )
            Text(
                text = "The Gideonites - The Youth Alive Fellowship • Kingdom Giants",
                style = MaterialTheme.typography.labelSmall,
                color = ShieldSilver
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3-Tier Podium (2nd on left, 1st in middle, 3rd on right)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 2nd Place (Silver)
                if (second != null) {
                    PodiumPillar(
                        rank = 2,
                        name = second.name,
                        score = second.score,
                        height = 100.dp,
                        pillarColor = Color(0xFF94A3B8),
                        isCurrentUser = second.isCurrentUser,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 1st Place (Gold)
                if (first != null) {
                    PodiumPillar(
                        rank = 1,
                        name = first.name,
                        score = first.score,
                        height = 140.dp,
                        pillarColor = FlameGoldAccent,
                        isCurrentUser = first.isCurrentUser,
                        modifier = Modifier.weight(1.1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 3rd Place (Bronze)
                if (third != null) {
                    PodiumPillar(
                        rank = 3,
                        name = third.name,
                        score = third.score,
                        height = 80.dp,
                        pillarColor = Color(0xFFB45309),
                        isCurrentUser = third.isCurrentUser,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Final Top 5 Standings List
            Text(
                text = "FINAL TOP 5 STANDINGS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = ShieldSilver
                ),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            activeQuiz.competitors.take(5).forEachIndexed { idx, comp ->
                key(comp.id) {
                    AnimatedQuizLeaderboardRow(rank = idx + 1, competitor = comp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User individual performance summary card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Your Individual Standing",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GlobeBlack
                        )
                        Surface(
                            color = FlameGoldAccent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Rank #${activeQuiz.currentUserRank} of ${activeQuiz.competitors.size.coerceAtLeast(1)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Total Game Points:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${activeQuiz.totalGamePoints} pts",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = FlameRedPrimary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Correct Answers:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${activeQuiz.userScore} of ${activeQuiz.questions.size}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Role-specific congratulatory callout
            when (session.userRole) {
                UserRole.ADMINISTRATOR -> {
                    Text(
                        text = "Admin Session Complete: Participant scores logged to the fellowship records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShieldSilver,
                        textAlign = TextAlign.Center
                    )
                }
                UserRole.YOUTH_EXECUTIVE -> {
                    Text(
                        text = "Great leadership example! Invite your unit members to challenge your score.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlobeBlack,
                        textAlign = TextAlign.Center
                    )
                }
                UserRole.YOUTH_MEMBER -> {
                    Text(
                        text = "Your score has been permanently recorded on the Youth Global Leaderboard! 🌟",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF10B981),
                        textAlign = TextAlign.Center
                    )
                }
                UserRole.GUEST -> {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Register as a Full Member to keep your score on the permanent leaderboard!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_return_leaderboard")
            ) {
                Text("Return to Games Hub")
            }
        }
    }
}

@Composable
private fun PodiumPillar(
    rank: Int,
    name: String,
    score: Int,
    height: androidx.compose.ui.unit.Dp,
    pillarColor: Color,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(pillarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (rank) {
                    1 -> "👑"
                    2 -> "🥈"
                    else -> "🥉"
                },
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isCurrentUser) "$name ★" else name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isCurrentUser) FontWeight.ExtraBold else FontWeight.Medium
            ),
            color = if (isCurrentUser) FlameRedPrimary else GlobeBlack,
            maxLines = 1
        )
        Text(
            text = "$score pts",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = pillarColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Pillar block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(pillarColor.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
        }
    }
}
