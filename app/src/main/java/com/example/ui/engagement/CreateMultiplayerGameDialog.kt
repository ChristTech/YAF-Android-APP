package com.example.ui.engagement

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GameType
import com.example.data.model.MultiplayerQuestion
import com.example.data.model.PresetGameSessions
import com.example.data.model.scrambleVerse
import com.example.data.repository.BibleRepository
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateMultiplayerGameDialog(
    onDismiss: () -> Unit,
    bibleRepository: BibleRepository? = null,
    initialSelectedVerse: com.example.data.local.entity.BibleVerseEntity? = null,
    initialDraftSession: com.example.data.local.entity.MultiplayerGameSessionEntity? = null,
    onSaveDraft: (
        title: String,
        description: String,
        gameType: GameType,
        timeLimitSeconds: Int,
        baseScore: Int,
        speedBonusEnabled: Boolean,
        questions: List<MultiplayerQuestion>
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onPublishAndSchedule: (
        title: String,
        description: String,
        gameType: GameType,
        scheduledDelayMinutes: Int,
        timeLimitSeconds: Int,
        baseScore: Int,
        speedBonusEnabled: Boolean,
        questions: List<MultiplayerQuestion>
    ) -> Unit
) {
    var title by remember(initialSelectedVerse, initialDraftSession) {
        mutableStateOf(
            when {
                initialDraftSession != null -> initialDraftSession.title
                initialSelectedVerse != null -> "Youth Alive • ${initialSelectedVerse.reference} Scramble"
                else -> "Youth Alive • Gideonites Live Showdown"
            }
        )
    }
    var description by remember(initialSelectedVerse, initialDraftSession) {
        mutableStateOf(
            when {
                initialDraftSession != null -> initialDraftSession.description
                initialSelectedVerse != null -> "Scripture Scramble featuring ${initialSelectedVerse.reference} (${initialSelectedVerse.translation})."
                else -> "Synchronized multiplayer Bible competition for Kingdom Giants."
            }
        )
    }
    var selectedGameType by remember(initialSelectedVerse, initialDraftSession) {
        mutableStateOf(
            when {
                initialDraftSession != null -> initialDraftSession.gameType
                initialSelectedVerse != null -> GameType.SCRIPTURE_SCRAMBLE
                else -> GameType.TRIVIA_CHALLENGE
            }
        )
    }
    var scheduledDelayMinutes by remember(initialDraftSession) { mutableIntStateOf(0) } // 0 = Start Now/Immediately
    var timeLimitPerQuestion by remember(initialSelectedVerse, initialDraftSession) {
        mutableIntStateOf(
            when {
                initialDraftSession != null -> initialDraftSession.timeLimitPerQuestionSeconds
                initialSelectedVerse != null -> 30
                else -> 20
            }
        )
    }
    var baseScorePerQuestion by remember(initialDraftSession) {
        mutableIntStateOf(initialDraftSession?.baseScorePerQuestion ?: 100)
    }
    var speedBonusEnabled by remember(initialDraftSession) {
        mutableStateOf(initialDraftSession?.speedBonusEnabled ?: true)
    }

    // Questions State List
    val questions = remember(initialSelectedVerse, initialDraftSession) {
        mutableStateListOf<MultiplayerQuestion>().apply {
            if (initialDraftSession != null) {
                val parsed = parseDraftQuestions(initialDraftSession.questionsJson)
                if (parsed.isNotEmpty()) {
                    addAll(parsed)
                } else {
                    when (initialDraftSession.gameType) {
                        GameType.TRIVIA_CHALLENGE -> addAll(PresetGameSessions.getTriviaChallengeQuestions())
                        GameType.WHO_AM_I -> addAll(PresetGameSessions.getWhoAmIQuestions())
                        GameType.SCRIPTURE_SCRAMBLE -> addAll(PresetGameSessions.getScriptureScrambleQuestions())
                    }
                }
            } else if (initialSelectedVerse != null) {
                add(
                    MultiplayerQuestion(
                        id = 1,
                        gameType = GameType.SCRIPTURE_SCRAMBLE,
                        prompt = "Reconstruct the scripture from ${initialSelectedVerse.reference} (${initialSelectedVerse.translation}):",
                        correctAnswer = initialSelectedVerse.verseText,
                        scrambledWords = scrambleVerse(initialSelectedVerse.verseText),
                        scriptureReference = initialSelectedVerse.reference,
                        translation = initialSelectedVerse.translation,
                        explanation = "Theme: ${initialSelectedVerse.theme}",
                        timerSeconds = 30,
                        points = 100
                    )
                )
                PresetGameSessions.getScriptureScrambleQuestions().forEachIndexed { idx, q ->
                    if (!q.scriptureReference.equals(initialSelectedVerse.reference, ignoreCase = true)) {
                        add(q.copy(id = idx + 2))
                    }
                }
            } else {
                addAll(PresetGameSessions.getTriviaChallengeQuestions())
            }
        }
    }

    // Modal states for Question Editor & Preview
    var editingQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var isAddingNewQuestion by remember { mutableStateOf(false) }
    var previewQuestion by remember { mutableStateOf<MultiplayerQuestion?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showBibleDatabasePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag("create_multiplayer_game_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FlameRedPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (initialDraftSession != null) "EDITING SAVED DRAFT" else "EXECUTIVE CONSOLE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = FlameRedPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "Gideonites Games Command",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add, edit, preview, save drafts for later, or launch live sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_game_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Session Title & Description
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Game Session Title *") },
                        placeholder = { Text("e.g. Sunday Youth Gideonites Scripture Showdown") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_game_title"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Fellowship Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_game_description"),
                        singleLine = false,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Game Type Selector (Trivia, Who Am I?, Scripture Scramble)
                    Text(
                        text = "SELECT GAME TYPE *",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = FlameRedPrimary,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GameTypeSelectionCard(
                            gameType = GameType.TRIVIA_CHALLENGE,
                            icon = Icons.Default.SportsEsports,
                            isSelected = selectedGameType == GameType.TRIVIA_CHALLENGE,
                            onClick = {
                                selectedGameType = GameType.TRIVIA_CHALLENGE
                                questions.clear()
                                questions.addAll(PresetGameSessions.getTriviaChallengeQuestions())
                            },
                            modifier = Modifier.weight(1f)
                        )

                        GameTypeSelectionCard(
                            gameType = GameType.WHO_AM_I,
                            icon = Icons.Default.HelpOutline,
                            isSelected = selectedGameType == GameType.WHO_AM_I,
                            onClick = {
                                selectedGameType = GameType.WHO_AM_I
                                questions.clear()
                                questions.addAll(PresetGameSessions.getWhoAmIQuestions())
                            },
                            modifier = Modifier.weight(1f)
                        )

                        GameTypeSelectionCard(
                            gameType = GameType.SCRIPTURE_SCRAMBLE,
                            icon = Icons.Default.AutoStories,
                            isSelected = selectedGameType == GameType.SCRIPTURE_SCRAMBLE,
                            onClick = {
                                selectedGameType = GameType.SCRIPTURE_SCRAMBLE
                                questions.clear()
                                questions.addAll(PresetGameSessions.getScriptureScrambleQuestions())
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Scheduling, Timers & Scoring Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Session Scheduling & Synchronized Rules",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Start Schedule Option (FlowRow so all options remain visible on mobile widths)
                            Text(
                                text = "Start Schedule:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                listOf(
                                    0 to "🚀 Start Now",
                                    2 to "⏰ In 2 mins",
                                    5 to "⏰ In 5 mins",
                                    15 to "⏰ In 15 mins"
                                ).forEach { (delay, label) ->
                                    FilterChip(
                                        selected = scheduledDelayMinutes == delay,
                                        onClick = { scheduledDelayMinutes = delay },
                                        label = { Text(label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = FlameRedPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Question Timer Controls (Full-width FlowRow so all timer options remain visible)
                            Text(
                                text = "Question Timer:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                listOf(15, 20, 30, 45).forEach { seconds ->
                                    FilterChip(
                                        selected = timeLimitPerQuestion == seconds,
                                        onClick = { timeLimitPerQuestion = seconds },
                                        label = { Text("${seconds}s", fontSize = 12.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Base Score Controls (Full-width FlowRow so all score options remain visible)
                            Text(
                                text = "Base Score:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                listOf(50, 100, 200).forEach { pts ->
                                    FilterChip(
                                        selected = baseScorePerQuestion == pts,
                                        onClick = { baseScorePerQuestion = pts },
                                        label = { Text("${pts}p", fontSize = 12.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Speed Bonus (+50 pts max)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Awards extra points to players who answer faster",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = speedBonusEnabled,
                                    onCheckedChange = { speedBonusEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = FlameRedPrimary)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Questions Management Section (Add Question prominently visible in FlowRow)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "QUESTIONS & ROUNDS (${questions.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = GlobeBlack,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = "Tailored for ${selectedGameType.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { isAddingNewQuestion = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                modifier = Modifier.testTag("btn_add_question")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Question", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showBibleDatabasePicker = true },
                                modifier = Modifier.testTag("btn_browse_bible_for_questions")
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = FlameGoldAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bible DB", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    questions.clear()
                                    when (selectedGameType) {
                                        GameType.TRIVIA_CHALLENGE -> questions.addAll(PresetGameSessions.getTriviaChallengeQuestions())
                                        GameType.WHO_AM_I -> questions.addAll(PresetGameSessions.getWhoAmIQuestions())
                                        GameType.SCRIPTURE_SCRAMBLE -> questions.addAll(PresetGameSessions.getScriptureScrambleQuestions())
                                    }
                                },
                                modifier = Modifier.testTag("btn_load_preset_questions")
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Load Presets", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (questions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text("⚠️ No questions added yet.", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Tap '+ Add Question' or 'Load Presets' to configure questions before scheduling.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        questions.forEachIndexed { index, q ->
                            QuestionAdminCard(
                                question = q,
                                index = index,
                                totalCount = questions.size,
                                onMoveUp = {
                                    if (index > 0) {
                                        val item = questions.removeAt(index)
                                        questions.add(index - 1, item)
                                    }
                                },
                                onMoveDown = {
                                    if (index < questions.size - 1) {
                                        val item = questions.removeAt(index)
                                        questions.add(index + 1, item)
                                    }
                                },
                                onPreview = { previewQuestion = q },
                                onEdit = { editingQuestionIndex = index },
                                onDelete = { questions.removeAt(index) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    validationError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ $err",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons (Cancel, Save Draft, Publish/Launch)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    OutlinedButton(
                        onClick = {
                            if (title.isBlank()) {
                                validationError = "Please enter a game session title."
                                return@OutlinedButton
                            }
                            if (questions.isEmpty()) {
                                validationError = "Please add at least one question before saving."
                                return@OutlinedButton
                            }
                            onSaveDraft(
                                title.trim(),
                                description.trim(),
                                selectedGameType,
                                timeLimitPerQuestion,
                                baseScorePerQuestion,
                                speedBonusEnabled,
                                questions.toList()
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_save_game_draft")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Draft", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                validationError = "Please enter a game session title."
                                return@Button
                            }
                            if (questions.isEmpty()) {
                                validationError = "Please add at least one question before scheduling."
                                return@Button
                            }
                            onPublishAndSchedule(
                                title.trim(),
                                description.trim(),
                                selectedGameType,
                                scheduledDelayMinutes,
                                timeLimitPerQuestion,
                                baseScorePerQuestion,
                                speedBonusEnabled,
                                questions.toList()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_publish_and_schedule")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (scheduledDelayMinutes == 0) "Launch Showdown Now" else "Publish & Schedule Game",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modal: Tailored Question Editor (for Add or Edit)
    if (isAddingNewQuestion || editingQuestionIndex != null) {
        val editingIndex = editingQuestionIndex
        val initialQuestion = if (editingIndex != null && editingIndex < questions.size) {
            questions[editingIndex]
        } else {
            // Default fresh question tailored to game type
            when (selectedGameType) {
                GameType.TRIVIA_CHALLENGE -> MultiplayerQuestion(
                    id = questions.size + 1,
                    gameType = GameType.TRIVIA_CHALLENGE,
                    prompt = "",
                    options = listOf("", "", "", ""),
                    correctAnswer = "",
                    scriptureReference = "",
                    timerSeconds = timeLimitPerQuestion,
                    points = baseScorePerQuestion
                )
                GameType.WHO_AM_I -> MultiplayerQuestion(
                    id = questions.size + 1,
                    gameType = GameType.WHO_AM_I,
                    prompt = "Identify this Bible figure from the progressive clues:",
                    clues = listOf("", "", ""),
                    options = listOf("", "", "", ""),
                    correctAnswer = "",
                    scriptureReference = "",
                    timerSeconds = timeLimitPerQuestion,
                    points = baseScorePerQuestion
                )
                GameType.SCRIPTURE_SCRAMBLE -> MultiplayerQuestion(
                    id = questions.size + 1,
                    gameType = GameType.SCRIPTURE_SCRAMBLE,
                    prompt = "Reconstruct the verse in correct biblical sequence:",
                    correctAnswer = "",
                    scriptureReference = "",
                    translation = "KJV",
                    timerSeconds = timeLimitPerQuestion,
                    points = baseScorePerQuestion
                )
            }
        }

        TailoredQuestionEditorModal(
            gameType = selectedGameType,
            initialQuestion = initialQuestion,
            bibleRepository = bibleRepository,
            onDismiss = {
                isAddingNewQuestion = false
                editingQuestionIndex = null
            },
            onSave = { updatedQ ->
                if (editingIndex != null && editingIndex < questions.size) {
                    questions[editingIndex] = updatedQ
                } else {
                    questions.add(updatedQ.copy(id = questions.size + 1))
                }
                isAddingNewQuestion = false
                editingQuestionIndex = null
            }
        )
    }

    // Modal: Live Participant Question Preview
    previewQuestion?.let { q ->
        QuestionPreviewModal(
            question = q,
            onDismiss = { previewQuestion = null }
        )
    }

    // Modal: Bible Scriptures Database Picker & Uploader
    if (showBibleDatabasePicker) {
        BibleScripturePickerDialog(
            bibleRepository = bibleRepository,
            onDismiss = { showBibleDatabasePicker = false },
            onScriptureSelected = { selectedVerse ->
                val nextId = (questions.maxOfOrNull { it.id } ?: 0) + 1
                if (selectedGameType == GameType.SCRIPTURE_SCRAMBLE) {
                    questions.add(
                        MultiplayerQuestion(
                            id = nextId,
                            gameType = GameType.SCRIPTURE_SCRAMBLE,
                            prompt = "Reconstruct the scripture from ${selectedVerse.reference} (${selectedVerse.translation}):",
                            options = emptyList(),
                            correctAnswer = selectedVerse.verseText,
                            clues = emptyList(),
                            scrambledWords = scrambleVerse(selectedVerse.verseText),
                            scriptureReference = selectedVerse.reference,
                            translation = selectedVerse.translation,
                            explanation = "Theme: ${selectedVerse.theme}",
                            timerSeconds = timeLimitPerQuestion,
                            points = baseScorePerQuestion
                        )
                    )
                } else {
                    questions.add(
                        MultiplayerQuestion(
                            id = nextId,
                            gameType = selectedGameType,
                            prompt = if (selectedGameType == GameType.TRIVIA_CHALLENGE)
                                "According to ${selectedVerse.reference}, complete this scripture:"
                            else
                                "Identify the Bible character from this covenant scripture:",
                            options = if (selectedGameType == GameType.TRIVIA_CHALLENGE)
                                listOf(selectedVerse.verseText, "", "", "")
                            else emptyList(),
                            correctAnswer = if (selectedGameType == GameType.TRIVIA_CHALLENGE) selectedVerse.verseText else "",
                            clues = if (selectedGameType == GameType.WHO_AM_I) listOf("Scripture clue: ${selectedVerse.reference}", "") else emptyList(),
                            scrambledWords = emptyList(),
                            scriptureReference = selectedVerse.reference,
                            translation = selectedVerse.translation,
                            explanation = "Scripture context: “${selectedVerse.verseText}”",
                            timerSeconds = timeLimitPerQuestion,
                            points = baseScorePerQuestion
                        )
                    )
                    editingQuestionIndex = questions.size - 1
                }
            },
            initialTargetContext = selectedGameType.displayName
        )
    }
}

private fun parseDraftQuestions(questionsJson: String): List<MultiplayerQuestion> {
    if (questionsJson.isBlank()) return emptyList()
    val result = mutableListOf<MultiplayerQuestion>()
    try {
        val array = org.json.JSONArray(questionsJson)
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
    } catch (_: Exception) {
    }
    return result
}

@Composable
private fun GameTypeSelectionCard(
    gameType: GameType,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) FlameRedPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) FlameRedPrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.testTag("game_type_${gameType.name}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) FlameRedPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = gameType.displayName,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) FlameRedPrimary else MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun QuestionAdminCard(
    question: MultiplayerQuestion,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_question_card_$index")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            // Question badge
            Surface(
                shape = CircleShape,
                color = FlameGoldAccent.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GlobeBlack
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = question.gameType.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    if (question.scriptureReference.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = question.scriptureReference,
                            style = MaterialTheme.typography.labelSmall.copy(color = ShieldSilver)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = when (question.gameType) {
                        GameType.SCRIPTURE_SCRAMBLE -> "Verse: \"${question.correctAnswer}\" (${question.translation})"
                        GameType.WHO_AM_I -> "Character: ${question.correctAnswer} • Clues (${question.clues.size})"
                        GameType.TRIVIA_CHALLENGE -> question.prompt
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2
                )

                if (question.correctAnswer.isNotBlank()) {
                    Text(
                        text = "Answer: ${question.correctAnswer}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981)
                    )
                }
            }

            // Action Buttons: Move Up, Move Down, Preview, Edit, Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Move Up",
                        modifier = Modifier.size(16.dp),
                        tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "Move Down",
                        modifier = Modifier.size(16.dp),
                        tint = if (index < totalCount - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = onPreview,
                    modifier = Modifier.size(30.dp).testTag("btn_preview_question_$index")
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Preview",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(30.dp).testTag("btn_edit_question_$index")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp),
                        tint = FlameGoldAccent
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp).testTag("btn_delete_question_$index")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Question Editor tailored to the 3 game types:
 * 1. Trivia Challenge: Prompt, 2-4 options, select correct answer, timer, points
 * 2. Who Am I?: Character name, 2-4 clues (hardest to easiest), choices, timer, points
 * 3. Scripture Scramble: Verse text, reference, translation, auto-scramble words, timer, points
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TailoredQuestionEditorModal(
    gameType: GameType,
    initialQuestion: MultiplayerQuestion,
    bibleRepository: BibleRepository? = null,
    onDismiss: () -> Unit,
    onSave: (MultiplayerQuestion) -> Unit
) {
    var prompt by remember { mutableStateOf(initialQuestion.prompt) }
    var correctAnswer by remember { mutableStateOf(initialQuestion.correctAnswer) }
    var scriptureRef by remember { mutableStateOf(initialQuestion.scriptureReference) }
    var translation by remember { mutableStateOf(initialQuestion.translation) }
    var explanation by remember { mutableStateOf(initialQuestion.explanation) }
    var timerSeconds by remember { mutableIntStateOf(initialQuestion.timerSeconds) }
    var points by remember { mutableIntStateOf(initialQuestion.points) }
    var showBiblePicker by remember { mutableStateOf(false) }

    // Options for Trivia Challenge (2 to 4 options)
    val optionsList = remember {
        mutableStateListOf<String>().apply {
            if (initialQuestion.options.isNotEmpty()) {
                addAll(initialQuestion.options)
            } else {
                addAll(listOf("", "", "", ""))
            }
        }
    }
    var correctOptionIndex by remember {
        mutableIntStateOf(
            optionsList.indexOfFirst {
                correctAnswer.isNotBlank() && it.trim().equals(correctAnswer.trim(), ignoreCase = true)
            }.takeIf { it >= 0 } ?: 0
        )
    }

    // Clues for Who Am I? (2 to 4 clues, arranged from hardest to easiest)
    val cluesList = remember {
        mutableStateListOf<String>().apply {
            if (initialQuestion.clues.isNotEmpty()) {
                addAll(initialQuestion.clues)
            } else {
                addAll(listOf("", "", ""))
            }
        }
    }

    // Scrambled words for Scripture Scramble
    var liveScrambledWords by remember {
        mutableStateOf(
            if (initialQuestion.scrambledWords.isNotEmpty()) {
                initialQuestion.scrambledWords
            } else if (correctAnswer.isNotBlank()) {
                scrambleVerse(correctAnswer)
            } else {
                emptyList()
            }
        )
    }

    var editorError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .testTag("tailored_question_editor_modal"),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Question Editor: ${gameType.displayName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = when (gameType) {
                                GameType.TRIVIA_CHALLENGE -> "Enter prompt, 2–4 options, and select the correct answer"
                                GameType.WHO_AM_I -> "Enter Bible character and 2–4 clues from hardest to easiest"
                                GameType.SCRIPTURE_SCRAMBLE -> "Enter verse, reference & translation. System auto-scrambles words"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (gameType) {
                        // =================== 1. TRIVIA CHALLENGE ===================
                        GameType.TRIVIA_CHALLENGE -> {
                            OutlinedTextField(
                                value = prompt,
                                onValueChange = { prompt = it },
                                label = { Text("Bible Trivia Question *") },
                                placeholder = { Text("e.g. In which book of the Bible is...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_trivia_prompt")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "OPTIONS (2–4) & CORRECT ANSWER *",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            optionsList.forEachIndexed { optIdx, optText ->
                                val isThisCorrect = correctOptionIndex == optIdx
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    // Radio button to select correct answer
                                    IconButton(
                                        onClick = {
                                            correctOptionIndex = optIdx
                                            correctAnswer = optText
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isThisCorrect)
                                                Icons.Default.RadioButtonChecked
                                            else
                                                Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Select as correct answer",
                                            tint = if (isThisCorrect)
                                                Color(0xFF10B981)
                                            else
                                                ShieldSilver
                                        )
                                    }

                                    OutlinedTextField(
                                        value = optText,
                                        onValueChange = { newText ->
                                            optionsList[optIdx] = newText
                                            if (correctOptionIndex == optIdx) {
                                                correctAnswer = newText
                                            }
                                        },
                                        label = { Text("Option ${('A' + optIdx)}") },
                                        placeholder = { Text("Option choice") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_option_$optIdx"),
                                        singleLine = true
                                    )

                                    if (optionsList.size > 2) {
                                        IconButton(
                                            onClick = {
                                                optionsList.removeAt(optIdx)
                                                if (correctOptionIndex == optIdx) {
                                                    correctOptionIndex = 0
                                                    correctAnswer = optionsList.firstOrNull().orEmpty()
                                                } else if (correctOptionIndex > optIdx) {
                                                    correctOptionIndex -= 1
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove option", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }

                            if (optionsList.size < 4) {
                                TextButton(
                                    onClick = { optionsList.add("") },
                                    modifier = Modifier.testTag("btn_add_option")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Option (${optionsList.size}/4)")
                                }
                            }
                        }

                        // =================== 2. WHO AM I? ===================
                        GameType.WHO_AM_I -> {
                            OutlinedTextField(
                                value = correctAnswer,
                                onValueChange = { correctAnswer = it },
                                label = { Text("Bible Character Name (Correct Answer) *") },
                                placeholder = { Text("e.g. David, Esther, Elijah, Paul") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_who_am_i_character")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "CLUES (2–4, HARDEST TO EASIEST) *",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = "The system reveals clues sequentially. Earlier correct answers earn more points!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            cluesList.forEachIndexed { clueIdx, clueText ->
                                val difficultyLabel = when (clueIdx) {
                                    0 -> "Clue 1 (Hardest — High Points ⭐⭐⭐)"
                                    1 -> "Clue 2 (Medium — ⭐⭐)"
                                    2 -> "Clue 3 (Easiest — ⭐)"
                                    else -> "Clue 4 (Final Hint)"
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    OutlinedTextField(
                                        value = clueText,
                                        onValueChange = { cluesList[clueIdx] = it },
                                        label = { Text(difficultyLabel) },
                                        placeholder = { Text("Mystery clue details...") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_clue_$clueIdx"),
                                        maxLines = 2
                                    )

                                    if (cluesList.size > 2) {
                                        IconButton(
                                            onClick = { cluesList.removeAt(clueIdx) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove clue", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }

                            if (cluesList.size < 4) {
                                TextButton(
                                    onClick = { cluesList.add("") },
                                    modifier = Modifier.testTag("btn_add_clue")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Clue (${cluesList.size}/4)")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Multiple Choice Options for Who Am I
                            Text(
                                text = "Character Choices for Participants (2–4):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            optionsList.forEachIndexed { optIdx, optText ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    OutlinedTextField(
                                        value = optText,
                                        onValueChange = { optionsList[optIdx] = it },
                                        label = { Text("Choice ${optIdx + 1}") },
                                        placeholder = { Text("e.g. David, Solomon, Saul") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_who_choice_$optIdx"),
                                        singleLine = true
                                    )
                                    if (optionsList.size > 2) {
                                        IconButton(
                                            onClick = { optionsList.removeAt(optIdx) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                            if (optionsList.size < 4) {
                                TextButton(onClick = { optionsList.add("") }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Choice")
                                }
                            }
                        }

                        // =================== 3. SCRIPTURE SCRAMBLE ===================
                        GameType.SCRIPTURE_SCRAMBLE -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Bible Database Integration",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Pick scriptures directly from the fellowship Bible database",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    Button(
                                        onClick = { showBiblePicker = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("btn_browse_bible_for_scramble")
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Find Scripture", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = correctAnswer,
                                onValueChange = {
                                    correctAnswer = it
                                    liveScrambledWords = scrambleVerse(it)
                                },
                                label = { Text("Bible Verse (Full Text) *") },
                                placeholder = { Text("e.g. I can do all things through Christ who strengthens me") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_scramble_verse"),
                                minLines = 2
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = scriptureRef,
                                    onValueChange = { scriptureRef = it },
                                    label = { Text("Reference *") },
                                    placeholder = { Text("e.g. Philippians 4:13") },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .testTag("input_scramble_reference")
                                )

                                OutlinedTextField(
                                    value = translation,
                                    onValueChange = { translation = it },
                                    label = { Text("Translation *") },
                                    placeholder = { Text("KJV") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_scramble_translation")
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Automatic Scrambled Words Preview Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚡ System Automatic Scramble:",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        TextButton(
                                            onClick = {
                                                if (correctAnswer.isNotBlank()) {
                                                    liveScrambledWords = scrambleVerse(correctAnswer)
                                                }
                                            },
                                            modifier = Modifier.testTag("btn_rescramble_words")
                                        ) {
                                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Re-Scramble", fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (liveScrambledWords.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            liveScrambledWords.forEach { word ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = FlameGoldAccent.copy(alpha = 0.2f),
                                                    border = BorderStroke(1.dp, FlameGoldAccent.copy(alpha = 0.4f))
                                                ) {
                                                    Text(
                                                        text = word,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Type a Bible verse above to view automatic word tiles.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Common Fields: Scripture Reference, Explanation, Custom Timer & Points
                    if (gameType != GameType.SCRIPTURE_SCRAMBLE) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = scriptureRef,
                                onValueChange = { scriptureRef = it },
                                label = { Text("Scripture Reference") },
                                placeholder = { Text("e.g. Daniel 11:32") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_editor_scripture_ref")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showBiblePicker = true },
                                modifier = Modifier.testTag("btn_lookup_scripture_bible_db")
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = FlameGoldAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bible DB", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        label = { Text("Biblical Explanation / Covenant Context") },
                        placeholder = { Text("Displayed during round review to edify believers.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = timerSeconds.toString(),
                            onValueChange = { timerSeconds = it.toIntOrNull() ?: 20 },
                            label = { Text("Timer (Seconds)") },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = points.toString(),
                            onValueChange = { points = it.toIntOrNull() ?: 100 },
                            label = { Text("Points") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    editorError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ $err",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider()
                Spacer(modifier = Modifier.height(10.dp))

                // Save or Cancel
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            when (gameType) {
                                GameType.TRIVIA_CHALLENGE -> {
                                    if (prompt.isBlank()) {
                                        editorError = "Please enter the trivia question prompt."
                                        return@Button
                                    }
                                    val validOpts = optionsList.map { it.trim() }.filter { it.isNotBlank() }
                                    if (validOpts.size < 2) {
                                        editorError = "Trivia challenge requires at least 2 options."
                                        return@Button
                                    }
                                    if (correctAnswer.isBlank() || !validOpts.contains(correctAnswer.trim())) {
                                        editorError = "Please select or set the correct answer from the options."
                                        return@Button
                                    }
                                    onSave(
                                        initialQuestion.copy(
                                            gameType = GameType.TRIVIA_CHALLENGE,
                                            prompt = prompt.trim(),
                                            options = validOpts,
                                            correctAnswer = correctAnswer.trim(),
                                            scriptureReference = scriptureRef.trim(),
                                            explanation = explanation.trim(),
                                            timerSeconds = timerSeconds,
                                            points = points
                                        )
                                    )
                                }
                                GameType.WHO_AM_I -> {
                                    if (correctAnswer.isBlank()) {
                                        editorError = "Please enter the correct Bible character name."
                                        return@Button
                                    }
                                    val validClues = cluesList.map { it.trim() }.filter { it.isNotBlank() }
                                    if (validClues.size < 2) {
                                        editorError = "Who Am I requires at least 2 progressive clues."
                                        return@Button
                                    }
                                    val validOpts = optionsList.map { it.trim() }.filter { it.isNotBlank() }
                                    val trimmedAnswer = correctAnswer.trim()
                                    val finalOpts = when {
                                        validOpts.isEmpty() -> listOf(trimmedAnswer)
                                        validOpts.any { it.equals(trimmedAnswer, ignoreCase = true) } -> validOpts
                                        else -> (validOpts + trimmedAnswer).distinct()
                                    }
                                    onSave(
                                        initialQuestion.copy(
                                            gameType = GameType.WHO_AM_I,
                                            prompt = prompt.ifBlank { "Identify this Bible figure from progressive clues:" },
                                            clues = validClues,
                                            options = finalOpts,
                                            correctAnswer = trimmedAnswer,
                                            scriptureReference = scriptureRef.trim(),
                                            explanation = explanation.trim(),
                                            timerSeconds = timerSeconds,
                                            points = points
                                        )
                                    )
                                }
                                GameType.SCRIPTURE_SCRAMBLE -> {
                                    if (correctAnswer.isBlank()) {
                                        editorError = "Please enter the Bible verse text."
                                        return@Button
                                    }
                                    if (scriptureRef.isBlank()) {
                                        editorError = "Please enter the scripture reference."
                                        return@Button
                                    }
                                    val scrambled = if (liveScrambledWords.isNotEmpty()) liveScrambledWords else scrambleVerse(correctAnswer)
                                    onSave(
                                        initialQuestion.copy(
                                            gameType = GameType.SCRIPTURE_SCRAMBLE,
                                            prompt = prompt.ifBlank { "Reconstruct the verse in correct biblical sequence:" },
                                            correctAnswer = correctAnswer.trim(),
                                            scriptureReference = scriptureRef.trim(),
                                            translation = translation.ifBlank { "KJV" },
                                            scrambledWords = scrambled,
                                            options = listOf(correctAnswer.trim()),
                                            explanation = explanation.trim(),
                                            timerSeconds = timerSeconds,
                                            points = points
                                        )
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                        modifier = Modifier.testTag("btn_save_question")
                    ) {
                        Text("Save Question")
                    }
                }
            }
        }
    }

    if (showBiblePicker) {
        BibleScripturePickerDialog(
            bibleRepository = bibleRepository,
            onDismiss = { showBiblePicker = false },
            onScriptureSelected = { selectedVerse ->
                if (gameType == GameType.SCRIPTURE_SCRAMBLE) {
                    correctAnswer = selectedVerse.verseText
                    scriptureRef = selectedVerse.reference
                    translation = selectedVerse.translation
                    liveScrambledWords = scrambleVerse(selectedVerse.verseText)
                    explanation = "Theme: ${selectedVerse.theme}"
                    if (prompt.isBlank() || prompt.startsWith("Reconstruct")) {
                        prompt = "Reconstruct the scripture from ${selectedVerse.reference} (${selectedVerse.translation}):"
                    }
                } else {
                    scriptureRef = selectedVerse.reference
                    if (explanation.isBlank()) {
                        explanation = "Scripture context: “${selectedVerse.verseText}” (${selectedVerse.translation})"
                    }
                }
            },
            initialTargetContext = gameType.displayName
        )
    }
}

/**
 * Preview Modal showing exactly what participants will see in the live game.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionPreviewModal(
    question: MultiplayerQuestion,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("question_preview_modal")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = FlameGoldAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "PARTICIPANT LIVE PREVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GlobeBlack,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Header with Timer and Points
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = FlameRedPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${question.timerSeconds}s timer",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Surface(
                        color = FlameRedPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${question.points} PTS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = FlameRedPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (question.gameType) {
                    GameType.TRIVIA_CHALLENGE -> {
                        Text(
                            text = question.prompt,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (question.scriptureReference.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reference: ${question.scriptureReference}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldSilver
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        question.options.forEach { opt ->
                            val isCorrect = opt.trim().equals(question.correctAnswer.trim(), ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCorrect) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isCorrect) BorderStroke(1.dp, Color(0xFF10B981)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    if (isCorrect) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }

                    GameType.WHO_AM_I -> {
                        Text(
                            text = "Mystery Character: Reveal Progressively",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        question.clues.forEachIndexed { clueIdx, clue ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Clue ${clueIdx + 1}:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = FlameGoldAccent)
                                    )
                                    Text(
                                        text = clue,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Correct Answer: ${question.correctAnswer}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        )
                    }

                    GameType.SCRIPTURE_SCRAMBLE -> {
                        Text(
                            text = "${question.scriptureReference} (${question.translation})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Scrambled Word Tiles:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val displayWords = if (question.scrambledWords.isNotEmpty()) question.scrambledWords else scrambleVerse(question.correctAnswer)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            displayWords.forEach { word ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = word,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Target Verse: \"${question.correctAnswer}\"",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GlobeBlack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Preview")
                }
            }
        }
    }
}
