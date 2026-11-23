package com.example.ui.engagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GameQuizEntity
import com.example.data.local.entity.GameScoreEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.MultiplayerGameSessionEntity
import com.example.data.model.GameType
import com.example.data.model.SessionStatus
import com.example.data.model.UserRole
import com.example.ui.components.ModernEmptyState
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BibleGamesTabContent(
    quizzes: List<GameQuizEntity>,
    leaderboard: List<GameScoreEntity>,
    myScores: List<GameScoreEntity>,
    session: SessionEntity,
    isGameTimeActive: Boolean = false,
    liveParticipantsCount: Int = 0,
    upcomingAndActiveSessions: List<MultiplayerGameSessionEntity> = emptyList(),
    draftSessions: List<MultiplayerGameSessionEntity> = emptyList(),
    completedSessions: List<MultiplayerGameSessionEntity> = emptyList(),
    onEnterLiveGame: () -> Unit = {},
    onEnterLiveGameWithMode: (GameType) -> Unit = {},
    onBroadcastGameTime: () -> Unit = {},
    onJoinMultiplayerSession: (String) -> Unit = {},
    onOpenCreateMultiplayerGame: () -> Unit = {},
    onOpenBibleDatabase: () -> Unit = {},
    onStartMultiplayerGameImmediately: (String) -> Unit = {},
    onCancelMultiplayerGame: (String) -> Unit = {},
    onEditDraftGame: (MultiplayerGameSessionEntity) -> Unit = {},
    onLaunchDraftGame: (String) -> Unit = {},
    onDeleteDraftGame: (String) -> Unit = {},
    onStartQuiz: (GameQuizEntity) -> Unit,
    onCreateQuiz: () -> Unit,
    onDeleteQuiz: (String) -> Unit,
    onDeleteScore: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bestScore = myScores.maxOfOrNull { it.score } ?: 0
    val quizzesCompleted = myScores.size
    val kingdomRankTitle = when {
        bestScore >= 300 || (bestScore in 8..50) -> "Kingdom Giant 👑"
        bestScore >= 180 || (bestScore in 5..7) -> "Bible Scholar 📜"
        bestScore >= 80 || (bestScore in 2..4) -> "Faith Seeker 🌱"
        else -> "Bible Explorer 🧭"
    }

    val activeMultiplayerSession = upcomingAndActiveSessions.find { it.status == SessionStatus.ACTIVE }
    val scheduledMultiplayerSessions = upcomingAndActiveSessions.filter { it.status == SessionStatus.SCHEDULED }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 0. Active Multiplayer Showdown Banner
        if (activeMultiplayerSession != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = FlameRedPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().testTag("live_game_arena_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.25f),
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
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE SHOWDOWN ACTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White
                                    )
                                }
                            }

                            Text(
                                text = "Live Multiplayer",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${activeMultiplayerSession.title} 🎮",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${activeMultiplayerSession.gameType.displayName} • Round ${activeMultiplayerSession.currentQuestionIndex + 1} of ${activeMultiplayerSession.totalQuestions}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onJoinMultiplayerSession(activeMultiplayerSession.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = FlameRedPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("btn_enter_live_arena")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Jump Into Live Showdown", fontWeight = FontWeight.ExtraBold)
                            }

                            if (session.userRole == UserRole.ADMINISTRATOR || session.userRole == UserRole.YOUTH_EXECUTIVE) {
                                OutlinedButton(
                                    onClick = { onCancelMultiplayerGame(activeMultiplayerSession.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("btn_cancel_session_${activeMultiplayerSession.id}")
                                ) {
                                    Text("End Game", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else if (isGameTimeActive) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().testTag("live_game_arena_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = FlameRedPrimary,
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
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE GAME ARENA",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White
                                    )
                                }
                            }

                            Text(
                                text = "Live Multiplayer",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Sunday Youth Live Showdown 🎮",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "It's Game Time! Compete against fellowship members in real time. Fast timers per question, speed bonuses, and live round standings after every question!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onEnterLiveGame,
                                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("btn_enter_live_arena")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enter Live Arena Now", fontWeight = FontWeight.Bold)
                            }

                            if (session.userRole == UserRole.ADMINISTRATOR || session.userRole == UserRole.YOUTH_EXECUTIVE) {
                                OutlinedButton(
                                    onClick = onBroadcastGameTime,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("btn_broadcast_game_alert")
                                ) {
                                    Text("📢 Alert All")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 0.5 Scheduled Upcoming Multiplayer Showdowns (Automatic Scheduled Starts)
        if (scheduledMultiplayerSessions.isNotEmpty()) {
            item {
                Text(
                    text = "SCHEDULED MULTIPLAYER SHOWDOWNS (${scheduledMultiplayerSessions.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = GlobeBlack,
                        letterSpacing = 0.8.sp
                    )
                )
            }

            items(scheduledMultiplayerSessions, key = { it.id }) { s ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, FlameGoldAccent.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("scheduled_session_${s.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    text = s.gameType.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GlobeBlack,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = "⏰ Scheduled",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = ShieldSilver
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = s.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "${s.totalQuestions} Rounds • ${s.timeLimitPerQuestionSeconds}s per question",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onJoinMultiplayerSession(s.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("btn_join_session_${s.id}")
                            ) {
                                Text("Enter Lobby", fontWeight = FontWeight.Bold)
                            }

                            if (session.userRole == UserRole.ADMINISTRATOR || session.userRole == UserRole.YOUTH_EXECUTIVE) {
                                OutlinedButton(
                                    onClick = { onStartMultiplayerGameImmediately(s.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_start_now_${s.id}")
                                ) {
                                    Text("🚀 Start")
                                }
                                OutlinedButton(
                                    onClick = { onCancelMultiplayerGame(s.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_cancel_session_${s.id}")
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 0.6 Saved Game Drafts (for Admin / Youth Executive to Edit & Launch later)
        if (draftSessions.isNotEmpty() &&
            (session.userRole == UserRole.ADMINISTRATOR || session.userRole == UserRole.YOUTH_EXECUTIVE)
        ) {
            item {
                Text(
                    text = "SAVED GAME DRAFTS (${draftSessions.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = GlobeBlack,
                        letterSpacing = 0.8.sp
                    )
                )
            }

            items(draftSessions, key = { "draft_${it.id}" }) { draft ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("draft_session_${draft.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = draft.gameType.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ShieldSilver.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "DRAFT • SAVED FOR LATER",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = draft.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "${draft.totalQuestions} Rounds • ${draft.timeLimitPerQuestionSeconds}s/q • ${draft.baseScorePerQuestion}p base",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onEditDraftGame(draft) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_edit_draft_${draft.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Draft", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { onLaunchDraftGame(draft.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_launch_draft_${draft.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Launch Now", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onDeleteDraftGame(draft.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_delete_draft_${draft.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Draft", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }

        // Standby Arena Banner (when no live games are currently running)
        if (activeMultiplayerSession == null && !isGameTimeActive) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("live_game_arena_standby_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
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
                                            .background(ShieldSilver)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE ARENA • STANDBY",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "Live Multiplayer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Live Scripture Showdown Arena 🎮",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (session.userRole == UserRole.ADMINISTRATOR || session.userRole == UserRole.YOUTH_EXECUTIVE)
                                "The live competition arena is ready on standby. Schedule or launch a multiplayer showdown across Trivia Challenge, Who Am I?, or Scripture Scramble!"
                            else
                                "The live competition arena opens when fellowship leadership launches Game Time. You can also launch an instant showdown in any of the 3 game modes below!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (session.userRole == UserRole.ADMINISTRATOR || session.userRole == UserRole.YOUTH_EXECUTIVE) {
                            Spacer(modifier = Modifier.height(14.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onOpenCreateMultiplayerGame,
                                    colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("btn_schedule_multiplayer_game")
                                ) {
                                    Icon(imageVector = Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("🎮 Create / Schedule Game", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onBroadcastGameTime,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("btn_broadcast_game_alert")
                                ) {
                                    Text("📢 Alert All")
                                }

                                OutlinedButton(
                                    onClick = onOpenBibleDatabase,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("btn_open_bible_db_standby")
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp), tint = FlameGoldAccent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bible DB")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 0.75 Dedicated 3 Game Modes Quick-Launcher Showcase
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("game_modes_showcase_section")
            ) {
                Text(
                    text = "3 MULTIPLAYER GAME MODES • INSTANT ARENA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = GlobeBlack,
                        letterSpacing = 0.8.sp
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GameType.values().forEach { mode ->
                        val modeIcon = when (mode) {
                            GameType.TRIVIA_CHALLENGE -> "⚡"
                            GameType.WHO_AM_I -> "🕵️"
                            GameType.SCRIPTURE_SCRAMBLE -> "🧩"
                        }
                        val modeSubtitle = when (mode) {
                            GameType.TRIVIA_CHALLENGE -> "4 Options & Speed"
                            GameType.WHO_AM_I -> "Progressive Clues"
                            GameType.SCRIPTURE_SCRAMBLE -> "Verse Scramble"
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_launch_mode_${mode.name}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Text(text = modeIcon, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GlobeBlack,
                                    maxLines = 1
                                )
                                Text(
                                    text = modeSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShieldSilver,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onEnterLiveGameWithMode(mode) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                        .testTag("btn_launch_mode_${mode.name}")
                                ) {
                                    Text("Play Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 0.9 Recent Completed Multiplayer Showdowns (View Final Podium & Standings)
        if (completedSessions.isNotEmpty()) {
            item {
                Text(
                    text = "RECENT MULTIPLAYER SHOWDOWNS (${completedSessions.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = GlobeBlack,
                        letterSpacing = 0.8.sp
                    )
                )
            }

            items(completedSessions.take(5), key = { "completed_${it.id}" }) { completed ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth().testTag("completed_session_${completed.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "COMPLETED • ${completed.gameType.displayName}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = completed.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = GlobeBlack
                            )
                            Text(
                                text = "${completed.totalQuestions} Rounds • Final Podium & Standings Recorded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { onJoinMultiplayerSession(completed.id) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_view_podium_${completed.id}")
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = FlameGoldAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Podium", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1. Role-specific Top Banner / Card
        when (session.userRole) {
            UserRole.ADMINISTRATOR -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_games_controls_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = FlameRedPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Quiz & Leaderboard Administration",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = GlobeBlack
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Publish tailored Scripture quizzes for fellowships, retreats, or youth week, and inspire healthy Bible knowledge competition.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onOpenCreateMultiplayerGame,
                                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_schedule_multiplayer_game")
                            ) {
                                Icon(imageVector = Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🎮 Schedule / Launch Multiplayer Showdown", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onCreateQuiz,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_create_quiz")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Solo Scripture Quiz")
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onOpenBibleDatabase,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_open_bible_database")
                            ) {
                                Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp), tint = FlameGoldAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📖 PDF / EPUB Bible Reference & Uploader")
                            }
                        }
                    }
                }
            }
            UserRole.YOUTH_EXECUTIVE -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("executive_games_controls_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = FlameGoldAccent,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Executive Games & Bible Reference Console 🏆",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Set fellowship games & quizzes and access the uploaded PDF/EPUB Bible whenever you need a scripture reference.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onOpenCreateMultiplayerGame,
                                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_schedule_multiplayer_game")
                            ) {
                                Icon(imageVector = Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🎮 Schedule / Launch Multiplayer Showdown", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onCreateQuiz,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_create_quiz")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Solo Scripture Quiz")
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onOpenBibleDatabase,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_open_bible_database")
                            ) {
                                Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp), tint = FlameGoldAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📖 Access PDF / EPUB Bible & Scripture Reference")
                            }
                        }
                    }
                }
            }
            UserRole.YOUTH_MEMBER -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("member_stats_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoGraph,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Your Trivia Progress",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = GlobeBlack
                                        )
                                        Text(
                                            text = "The Gideonites - The Youth Alive Fellowship • Kingdom Giants",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ShieldSilver
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = kingdomRankTitle,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$quizzesCompleted",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = FlameRedPrimary
                                        )
                                    )
                                    Text(
                                        text = "Quizzes Taken",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$bestScore pts",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = FlameGoldAccent
                                        )
                                    )
                                    Text(
                                        text = "Personal Best",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "“Let the word of Christ dwell in you richly in all wisdom...” — Colossians 3:16",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldSilver
                            )
                        }
                    }
                }
            }
            UserRole.GUEST -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("banner_guest_games")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = FlameRedPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Play & Challenge Your Bible Knowledge! 🎮",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Test your Scripture acumen in our interactive trivia. Register as a Member to save your rank permanently on the Global Leaderboard!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Featured Quizzes Section Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Featured Quizzes & Scripture Challenges",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlobeBlack
                    )
                )
                Text(
                    text = "${quizzes.size} available",
                    style = MaterialTheme.typography.labelSmall,
                    color = ShieldSilver
                )
            }
        }

        // Quizzes list
        if (quizzes.isEmpty()) {
            item {
                ModernEmptyState(
                    icon = Icons.Default.SportsEsports,
                    title = "No Scripture Quizzes Yet",
                    subtitle = if (session.userRole == UserRole.ADMINISTRATOR)
                        "The quiz arena is ready for the fellowship. Tap below to create custom Bible trivia, character identification, or scripture memory puzzles."
                    else
                        "Custom scripture quizzes created by leadership will appear here. In the meantime, join the Live Game Arena above!",
                    actionText = if (session.userRole == UserRole.ADMINISTRATOR) "+ Create First Scripture Quiz" else null,
                    onAction = if (session.userRole == UserRole.ADMINISTRATOR) onCreateQuiz else null
                )
            }
        } else {
            items(quizzes, key = { it.id }) { quiz ->
                QuizItemCard(
                    quiz = quiz,
                    isAdmin = session.userRole == UserRole.ADMINISTRATOR,
                    onStart = { onStartQuiz(quiz) },
                    onDelete = { onDeleteQuiz(quiz.id) }
                )
            }
        }

        // 3. Global Leaderboard Section Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = FlameGoldAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Youth Global Leaderboard",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlobeBlack
                        )
                    )
                }
                if (session.userRole == UserRole.ADMINISTRATOR) {
                    Surface(
                        color = FlameRedPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Live Rankings",
                            style = MaterialTheme.typography.labelSmall,
                            color = FlameRedPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Leaderboard entries
        if (leaderboard.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text("👑", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Leaderboard is Fresh",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "No recorded scores yet. Enter the Live Arena above or complete a quiz to claim #1!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(leaderboard.take(20), key = { it.id }) { score ->
                val isMyScore = score.userId == session.userId
                LeaderboardItemRow(
                    score = score,
                    isCurrentUser = isMyScore,
                    isAdmin = session.userRole == UserRole.ADMINISTRATOR,
                    onDeleteScore = { onDeleteScore(score.id) }
                )
            }
        }

        if (session.userRole == UserRole.GUEST) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = FlameGoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guest Note: High scores on this official board belong to registered Kingdom Giants. Upgrade your account in the Profile tab to compete!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuizItemCard(
    quiz: GameQuizEntity,
    isAdmin: Boolean,
    onStart: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                color = FlameGoldAccent.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = FlameGoldAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GlobeBlack
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${quiz.totalQuestions} Questions • ${quiz.timeLimitSeconds}s Limit • ${quiz.difficulty}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (quiz.description.isNotBlank()) {
                    Text(
                        text = quiz.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ShieldSilver,
                        maxLines = 1
                    )
                }
            }

            if (isAdmin) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Quiz",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play")
            }
        }
    }
}

@Composable
fun LeaderboardItemRow(
    score: GameScoreEntity,
    isCurrentUser: Boolean,
    isAdmin: Boolean,
    onDeleteScore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isCurrentUser) Color(0xFF10B981).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isCurrentUser) Modifier.border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(FlameGoldAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = FlameGoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = score.userName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GlobeBlack
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF10B981),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "★ YOU",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = score.quizTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${score.score} / ${score.totalQuestions} pts",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = FlameRedPrimary
                    )
                )

                if (isAdmin) {
                    IconButton(onClick = onDeleteScore) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Moderate Score",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
