package com.example.ui.spiritual

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.data.repository.SermonAudioDraft
import com.example.data.repository.SpiritualResourceRepositoryImpl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.repository.BibleDocumentExtractor
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.SpiritualResourceEntity
import com.example.ui.components.ModernEmptyState
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver
import java.util.Locale

private fun formatAudioDurationMs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun maskVerseForMemoryPractice(verse: String): String {
    val words = verse.trim().split(Regex("\\s+"))
    if (words.size <= 3) return "[ ? ? ? ]"
    return words.mapIndexed { index, word ->
        val stripped = word.replace(Regex("[^\\p{L}\\p{N}]"), "")
        val shouldMask = stripped.length >= 4 && (index % 3 == 2 || index == words.lastIndex)
        if (shouldMask) "[ ? ? ? ]" else word
    }.joinToString(" ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiritualScreen(
    viewModel: SpiritualViewModel,
    session: SessionEntity
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLeadership = session.userRole.isLeadership()

    var showPublishDialog by remember { mutableStateOf(false) }
    var defaultPublishCategory by remember { mutableStateOf("SERMON") }
    var resourceToEdit by remember { mutableStateOf<SpiritualResourceEntity?>(null) }
    var resourceToDelete by remember { mutableStateOf<SpiritualResourceEntity?>(null) }
    // Default to true so the Scripture for the Day is visible immediately, and toggling switches to Memory Verse Practice
    var revealMemoryVerse by remember { mutableStateOf(true) }

    val activeLang = com.example.core.localization.LanguageManager.currentLanguage.value
    val appStrings = com.example.core.localization.LanguageManager.strings(activeLang)
    val dailyScriptureResource = uiState.resources.firstOrNull {
        it.category.equals("SCRIPTURE_OF_THE_DAY", ignoreCase = true)
    }

    // Real on-device TextToSpeech for Audio Sermon / Devotional playback
    var ttsReady by remember { mutableStateOf(false) }
    val ttsRef = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.getDefault()
                instance?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        viewModel.stopAudioPlayback()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        viewModel.stopAudioPlayback()
                    }
                })
                ttsReady = true
            }
        }
        ttsRef.value = instance
        onDispose {
            try {
                instance?.stop()
                instance?.shutdown()
            } catch (_: Exception) {
            }
            ttsRef.value = null
        }
    }

    LaunchedEffect(uiState.playingAudioResourceId, uiState.isPlayingAudio, ttsReady) {
        val tts = ttsRef.value ?: return@LaunchedEffect
        if (!ttsReady) return@LaunchedEffect
        val playingId = uiState.playingAudioResourceId
        val res = playingId?.let { id -> uiState.resources.find { it.id == id } }
        // Only use TTS for text-only resources; real audio sermons use MediaPlayer in SpiritualViewModel
        if (res != null && uiState.isPlayingAudio && !res.hasAudio) {
            val speechText = buildString {
                append(res.title).append(". ")
                if (res.scriptureReference.isNotBlank()) {
                    append("Scripture reference: ").append(res.scriptureReference).append(". ")
                }
                append(res.contentBody.ifBlank { res.summary })
            }
            tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "spiritual_tts_${res.id}")
        } else {
            if (tts.isSpeaking) {
                tts.stop()
            }
        }
    }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isLeadership) {
                FloatingActionButton(
                    onClick = {
                        defaultPublishCategory = "SERMON"
                        resourceToEdit = null
                        showPublishDialog = true
                    },
                    containerColor = FlameRedPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_publish_spiritual")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Publish Resource")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Scripture for the Day & Memory Verse Challenge Card
            item {
                val activeRef = dailyScriptureResource?.scriptureReference?.ifBlank { "Daniel 11:32b" } ?: "Daniel 11:32b"
                val rawVerse = dailyScriptureResource?.summary?.ifBlank {
                    dailyScriptureResource.contentBody.ifBlank {
                        "...but the people that do know their God shall be strong, and do exploits."
                    }
                } ?: "...but the people that do know their God shall be strong, and do exploits."
                val localizedFullVerse = com.example.core.localization.LanguageManager.getMemoryVerse(
                    reference = activeRef,
                    fallbackText = rawVerse,
                    lang = activeLang
                )
                val displayedVerse = if (revealMemoryVerse) {
                    localizedFullVerse
                } else {
                    maskVerseForMemoryPractice(localizedFullVerse)
                }

                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = GlobeBlack),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("card_memory_verse")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${appStrings.scriptureOfTheDayBadge} • MEMORY VERSE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = FlameGoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FlameRedPrimary)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Gideonites • ${activeLang.shortBadge}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = activeRef,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"$displayedVerse\"",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(alpha = 0.95f),
                                fontStyle = FontStyle.Italic
                            )
                        )
                        if (dailyScriptureResource != null && dailyScriptureResource.speakerOrAuthor.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Posted by ${dailyScriptureResource.speakerOrAuthor}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FlameGoldAccent.copy(alpha = 0.9f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { revealMemoryVerse = !revealMemoryVerse },
                                colors = ButtonDefaults.buttonColors(containerColor = FlameGoldAccent, contentColor = GlobeBlack),
                                modifier = Modifier.testTag("btn_toggle_memory_verse")
                            ) {
                                Icon(
                                    imageVector = if (revealMemoryVerse) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (revealMemoryVerse) "Practice Memory" else "Reveal Scripture",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isLeadership) {
                                TextButton(
                                    onClick = {
                                        defaultPublishCategory = "SCRIPTURE_OF_THE_DAY"
                                        resourceToEdit = null
                                        showPublishDialog = true
                                    },
                                    modifier = Modifier.testTag("btn_spiritual_post_scripture")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = FlameGoldAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = appStrings.postScriptureAction,
                                        color = FlameGoldAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isLeadership) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "ADMIN & EXECUTIVE PUBLISHING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FlameRedPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        defaultPublishCategory = "SERMON"
                                        resourceToEdit = null
                                        showPublishDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_quick_upload_sermon")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload Sermon", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        defaultPublishCategory = "DEVOTIONAL"
                                        resourceToEdit = null
                                        showPublishDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_quick_upload_devotional")
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp), tint = FlameRedPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Daily Devotional", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Category & Bookmark Filters
            item {
                val categories = listOf("All", "SCRIPTURE_OF_THE_DAY", "SERMON", "DEVOTIONAL", "BIBLE_STUDY", "PRAYER_POINTS")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.showBookmarksOnly,
                            onClick = { viewModel.toggleBookmarksFilter() },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (uiState.showBookmarksOnly) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Saved Offline", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameRedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = !uiState.showBookmarksOnly && uiState.selectedCategory == cat,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(cat.replace("_", " "), fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameRedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (uiState.isUploadingAudio) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FlameRedPrimary.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Uploading audio sermon & replacing previous recording...",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = FlameRedPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = FlameRedPrimary
                            )
                        }
                    }
                }
            }

            if (uiState.filteredResources.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.MenuBook,
                        title = if (uiState.showBookmarksOnly) "No Saved Resources" else "Word & Audio Library",
                        subtitle = if (uiState.showBookmarksOnly)
                            "You haven't bookmarked any sermons or devotionals yet. Tap the bookmark icon on any message to save it here."
                        else
                            "Sermon audios, daily devotionals, and prophetic prayer points published by pastoral leadership will be available here.",
                        accentColor = FlameRedPrimary
                    )
                }
            } else {
                items(uiState.filteredResources, key = { it.id }) { resource ->
                    val isThisActive = uiState.playingAudioResourceId == resource.id
                    SpiritualResourceCard(
                        resource = resource,
                        isActiveAudio = isThisActive,
                        isPlaying = isThisActive && uiState.isPlayingAudio,
                        isLoadingAudio = isThisActive && uiState.isLoadingAudio,
                        progress = if (isThisActive) uiState.audioProgress else 0f,
                        currentPositionMs = if (isThisActive) uiState.audioCurrentPositionMs else 0L,
                        durationMs = if (isThisActive) uiState.audioDurationMs else 0L,
                        canManage = isLeadership,
                        onToggleBookmark = { viewModel.toggleBookmark(resource) },
                        onToggleAudio = { viewModel.toggleAudioPlayback(resource.id) },
                        onSeekAudio = { fraction -> viewModel.seekToFraction(resource.id, fraction) },
                        onSkipAudio = { deltaMs -> viewModel.skipByMs(resource.id, deltaMs) },
                        onStopAudio = { viewModel.stopAudioPlayback() },
                        onRead = { viewModel.openReader(resource) },
                        onEdit = {
                            resourceToEdit = resource
                            defaultPublishCategory = resource.category
                            showPublishDialog = true
                        },
                        onDelete = { resourceToDelete = resource }
                    )
                }
            }
        }
    }

    uiState.selectedResourceForReading?.let { resource ->
        val isThisActive = uiState.playingAudioResourceId == resource.id
        ResourceReaderDialog(
            resource = resource,
            isActiveAudio = isThisActive,
            isPlayingAudio = isThisActive && uiState.isPlayingAudio,
            isLoadingAudio = isThisActive && uiState.isLoadingAudio,
            audioProgress = if (isThisActive) uiState.audioProgress else 0f,
            audioPositionMs = if (isThisActive) uiState.audioCurrentPositionMs else 0L,
            audioDurationMs = if (isThisActive) uiState.audioDurationMs else 0L,
            onDismiss = { viewModel.closeReader() },
            onBookmark = { viewModel.toggleBookmark(resource) },
            onToggleAudio = { viewModel.toggleAudioPlayback(resource.id) },
            onSeekAudio = { fraction -> viewModel.seekToFraction(resource.id, fraction) },
            onSkipAudio = { deltaMs -> viewModel.skipByMs(resource.id, deltaMs) },
            onShare = {
                val shareText = buildString {
                    append(resource.title)
                    if (resource.scriptureReference.isNotBlank()) {
                        append(" (").append(resource.scriptureReference).append(")")
                    }
                    append("\nBy: ").append(resource.speakerOrAuthor)
                    append("\n\n").append(resource.contentBody.ifBlank { resource.summary })
                    append("\n\n— Shared from The Gideonites (Youth Alive Fellowship)")
                }
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, resource.title)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Spiritual Message"))
            }
        )
    }

    resourceToDelete?.let { deleting ->
        AlertDialog(
            onDismissRequest = { resourceToDelete = null },
            title = { Text("Delete Spiritual Resource?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"${deleting.title}\"? This will remove it across all connected devices.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteResource(deleting.id)
                        resourceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { resourceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPublishDialog) {
        PublishResourceDialog(
            initialCategory = defaultPublishCategory,
            defaultAuthorName = session.userName.ifBlank { "Youth Pastor / Admin" },
            editingResource = resourceToEdit,
            onDismiss = {
                showPublishDialog = false
                resourceToEdit = null
                defaultPublishCategory = "SERMON"
            },
            onSubmit = { title, cat, author, scripture, summary, body, dur, audioDraft, audioRemoved ->
                val editing = resourceToEdit
                if (editing != null) {
                    viewModel.editResource(
                        resourceId = editing.id,
                        title = title,
                        category = cat,
                        author = author,
                        scripture = scripture,
                        summary = summary,
                        content = body,
                        duration = dur,
                        existingResource = editing,
                        audioDraft = audioDraft,
                        audioRemoved = audioRemoved
                    )
                } else {
                    viewModel.publishResource(title, cat, author, scripture, summary, body, dur, audioDraft)
                }
                showPublishDialog = false
                resourceToEdit = null
                defaultPublishCategory = "SERMON"
            }
        )
    }
}

@Composable
private fun SpiritualResourceCard(
    resource: SpiritualResourceEntity,
    isActiveAudio: Boolean,
    isPlaying: Boolean,
    isLoadingAudio: Boolean,
    progress: Float,
    currentPositionMs: Long,
    durationMs: Long,
    canManage: Boolean,
    onToggleBookmark: () -> Unit,
    onToggleAudio: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onSkipAudio: (Long) -> Unit,
    onStopAudio: () -> Unit,
    onRead: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag("res_card_${resource.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(FlameRedPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = resource.category.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = FlameRedPrimary)
                        )
                    }
                    if (resource.hasAudio) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(FlameGoldAccent)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🎧 AUDIO",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = GlobeBlack)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canManage) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp).testTag("btn_edit_res_${resource.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit resource",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp).testTag("btn_delete_res_${resource.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete resource",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (resource.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (resource.isBookmarked) FlameRedPrimary else ShieldSilver
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = resource.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (resource.scriptureReference.isNotBlank()) {
                Text(
                    text = "Ref: ${resource.scriptureReference} • By: ${resource.speakerOrAuthor}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            } else if (resource.speakerOrAuthor.isNotBlank()) {
                Text(
                    text = "By: ${resource.speakerOrAuthor}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = resource.summary,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            if (resource.hasAudio) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().testTag("sermon_audio_player_${resource.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = FlameRedPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = resource.audioFileName ?: "Sermon Audio",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                            val sizeLabel = SpiritualResourceRepositoryImpl.formatFileSize(resource.audioSizeBytes)
                            if (sizeLabel.isNotBlank()) {
                                Text(
                                    text = sizeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isLoadingAudio) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Loading sermon audio for playback...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FlameRedPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = FlameRedPrimary
                            )
                        } else {
                            Slider(
                                value = if (isActiveAudio) progress.coerceIn(0f, 1f) else 0f,
                                onValueChange = { fraction ->
                                    if (isActiveAudio) onSeekAudio(fraction)
                                },
                                enabled = isActiveAudio && durationMs > 0L,
                                colors = SliderDefaults.colors(
                                    thumbColor = FlameRedPrimary,
                                    activeTrackColor = FlameRedPrimary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("sermon_audio_slider_${resource.id}")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isActiveAudio && durationMs > 0L) formatAudioDurationMs(currentPositionMs) else "00:00",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isActiveAudio && durationMs > 0L) {
                                        formatAudioDurationMs(durationMs)
                                    } else {
                                        "~${resource.audioDurationMinutes.coerceAtLeast(1)}m"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onSkipAudio(-15_000L) },
                                enabled = isActiveAudio && !isLoadingAudio
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Rewind 15 seconds",
                                    tint = if (isActiveAudio) GlobeBlack else ShieldSilver
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = FlameRedPrimary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                IconButton(
                                    onClick = onToggleAudio,
                                    modifier = Modifier.fillMaxSize().testTag("btn_play_audio_sermon_${resource.id}")
                                ) {
                                    if (isLoadingAudio) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause Audio Sermon" else "Play Audio Sermon",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onSkipAudio(15_000L) },
                                enabled = isActiveAudio && !isLoadingAudio
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Forward 15 seconds",
                                    tint = if (isActiveAudio) GlobeBlack else ShieldSilver
                                )
                            }
                            if (isActiveAudio) {
                                IconButton(onClick = onStopAudio) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Audio",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (resource.audioDurationMinutes > 0 || isPlaying) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleAudio) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Audio Play",
                            tint = FlameRedPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPlaying) {
                                "Playing audio narration..."
                            } else {
                                "Audio Narration (${resource.audioDurationMinutes} mins)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { if (isPlaying) progress else 0f },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = FlameRedPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleAudio) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = FlameRedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            isPlaying -> "Pause Audio"
                            resource.hasAudio -> "Play Sermon Audio"
                            else -> "Listen Aloud"
                        },
                        color = FlameRedPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onRead,
                    colors = ButtonDefaults.buttonColors(containerColor = GlobeBlack)
                ) {
                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Read Full Message", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ResourceReaderDialog(
    resource: SpiritualResourceEntity,
    isActiveAudio: Boolean,
    isPlayingAudio: Boolean,
    isLoadingAudio: Boolean,
    audioProgress: Float,
    audioPositionMs: Long,
    audioDurationMs: Long,
    onDismiss: () -> Unit,
    onBookmark: () -> Unit,
    onToggleAudio: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onSkipAudio: (Long) -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = resource.title, fontWeight = FontWeight.Bold)
                Text(
                    text = "${resource.category.replace("_", " ")} • ${resource.speakerOrAuthor}",
                    style = MaterialTheme.typography.labelMedium.copy(color = FlameRedPrimary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (resource.scriptureReference.isNotBlank()) {
                    Surface(
                        color = FlameGoldAccent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "SCRIPTURE: ${resource.scriptureReference}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = GlobeBlack
                            ),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (resource.hasAudio) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🎧 ${resource.audioFileName ?: "Audio Sermon"}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (isLoadingAudio) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = FlameRedPrimary)
                            } else {
                                Slider(
                                    value = if (isActiveAudio) audioProgress.coerceIn(0f, 1f) else 0f,
                                    onValueChange = { if (isActiveAudio) onSeekAudio(it) },
                                    enabled = isActiveAudio && audioDurationMs > 0L,
                                    colors = SliderDefaults.colors(thumbColor = FlameRedPrimary, activeTrackColor = FlameRedPrimary)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isActiveAudio && audioDurationMs > 0L) formatAudioDurationMs(audioPositionMs) else "00:00",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onSkipAudio(-15_000L) }, enabled = isActiveAudio) {
                                            Icon(Icons.Default.FastRewind, contentDescription = "Rewind 15s")
                                        }
                                        IconButton(onClick = onToggleAudio) {
                                            Icon(
                                                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause Audio",
                                                tint = FlameRedPrimary
                                            )
                                        }
                                        IconButton(onClick = { onSkipAudio(15_000L) }, enabled = isActiveAudio) {
                                            Icon(Icons.Default.FastForward, contentDescription = "Forward 15s")
                                        }
                                    }
                                    Text(
                                        text = if (isActiveAudio && audioDurationMs > 0L) formatAudioDurationMs(audioDurationMs) else "~${resource.audioDurationMinutes}m",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = resource.contentBody.ifBlank { resource.summary },
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onToggleAudio) {
                        Icon(
                            imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = FlameRedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isPlayingAudio -> "Pause Audio"
                                resource.hasAudio -> "Play Sermon"
                                else -> "Listen Aloud"
                            },
                            color = FlameRedPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = FlameRedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = FlameRedPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onBookmark) {
                Icon(
                    imageVector = if (resource.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = FlameRedPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (resource.isBookmarked) "Bookmarked ✓" else "Save Offline")
            }
        }
    )
}

@Composable
fun PublishResourceDialog(
    initialCategory: String = "SERMON",
    defaultAuthorName: String = "Youth Pastor / Admin",
    editingResource: SpiritualResourceEntity? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String, Int, SermonAudioDraft?, Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val categories = listOf("SCRIPTURE_OF_THE_DAY", "SERMON", "DEVOTIONAL", "BIBLE_STUDY", "PRAYER_POINTS")
    var selectedCategory by remember(editingResource, initialCategory) {
        val startCat = editingResource?.category ?: initialCategory
        mutableStateOf(if (categories.contains(startCat)) startCat else categories.first())
    }
    val isScriptureOfTheDay = selectedCategory == "SCRIPTURE_OF_THE_DAY"
    val isSermon = selectedCategory == "SERMON"

    var title by remember(editingResource, initialCategory) {
        mutableStateOf(
            editingResource?.title
                ?: if (initialCategory == "SCRIPTURE_OF_THE_DAY") "Scripture for the Day" else ""
        )
    }
    var author by remember(editingResource, defaultAuthorName) {
        mutableStateOf(editingResource?.speakerOrAuthor?.takeIf { it.isNotBlank() } ?: defaultAuthorName)
    }
    var scripture by remember(editingResource, initialCategory) {
        mutableStateOf(
            editingResource?.scriptureReference
                ?: if (initialCategory == "SCRIPTURE_OF_THE_DAY") "Judges 6:12" else ""
        )
    }
    var summary by remember(editingResource) {
        mutableStateOf(editingResource?.summary ?: "")
    }
    var body by remember(editingResource) {
        mutableStateOf(editingResource?.contentBody ?: "")
    }
    var durationText by remember(editingResource, initialCategory) {
        mutableStateOf(
            (editingResource?.audioDurationMinutes?.takeIf { it > 0 } ?: 30).toString()
        )
    }
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var attachedAudioDraft by remember(editingResource) { mutableStateOf<SermonAudioDraft?>(null) }
    var existingAudioRemoved by remember(editingResource) { mutableStateOf(false) }
    var isImportingAudio by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    val discardStagedDraft: (SermonAudioDraft?) -> Unit = { draft ->
        if (draft != null && draft.audioUri != editingResource?.audioUri) {
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { java.io.File(draft.audioUri).delete() }
            }
        }
    }

    val handleSelectedAudioUri: (android.net.Uri) -> Unit = { uri ->
        isImportingAudio = true
        localValidationError = null
        coroutineScope.launch {
            val res = SpiritualResourceRepositoryImpl.importAudioFromContentUri(context, uri.toString())
            res.onSuccess { draft ->
                discardStagedDraft(attachedAudioDraft)
                attachedAudioDraft = draft
                existingAudioRemoved = false
                durationText = draft.durationMinutes.toString()
                if (title.isBlank()) {
                    title = draft.fileName.substringBeforeLast('.')
                        .replace('_', ' ')
                        .replace('-', ' ')
                        .trim()
                }
                localValidationError = null
            }.onFailure { ex ->
                localValidationError = ex.message ?: "Could not import selected audio file."
            }
            isImportingAudio = false
        }
    }

    // Opens Android's Music / Audio / Voice Recorder / Files audio picker directly
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) handleSelectedAudioUri(uri)
    }

    // Fallback unrestricted file browser for OEMs/Drive/WhatsApp where audio MIME filters grey out .m4a/.opus/.mp3 files
    val audioBroadPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) handleSelectedAudioUri(uri)
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            if (isSermon && SpiritualResourceRepositoryImpl.isExplicitAudioFile(context, uri)) {
                handleSelectedAudioUri(uri)
            } else {
                coroutineScope.launch {
                    val res = BibleDocumentExtractor.extractDocumentTextForResource(context, uri)
                    res.onSuccess { (extractedTitle, extractedBody) ->
                        attachedFileName = BibleDocumentExtractor.resolveFileName(context, uri)
                        if (title.isBlank()) {
                            title = extractedTitle
                        }
                        if (extractedBody.isNotBlank()) {
                            body = extractedBody.take(11500)
                            if (summary.isBlank()) {
                                summary = extractedBody.lineSequence()
                                    .map { it.trim() }
                                    .firstOrNull { it.length >= 20 }
                                    ?.take(180)
                                    ?: extractedBody.take(180)
                            }
                        }
                        localValidationError = null
                    }.onFailure { ex ->
                        localValidationError = "Could not import document: ${ex.message}"
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            discardStagedDraft(attachedAudioDraft)
            onDismiss()
        },
        title = {
            Text(
                text = when {
                    editingResource != null -> "Edit Spiritual Content"
                    isScriptureOfTheDay -> "Post Scripture for the Day"
                    selectedCategory == "SERMON" -> "Upload Audio / Text Sermon"
                    selectedCategory == "DEVOTIONAL" -> "Upload Daily Devotional"
                    else -> "Publish Youth Spiritual Content"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Category:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                localValidationError = null
                                if (cat == "SCRIPTURE_OF_THE_DAY" && title.isBlank()) {
                                    title = "Scripture for the Day"
                                } else if (cat != "SCRIPTURE_OF_THE_DAY" && title == "Scripture for the Day") {
                                    title = ""
                                }
                            },
                            label = { Text(cat.replace("_", " ")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameRedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (isSermon) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FlameRedPrimary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "AUDIO SERMON UPLOAD (SINGLE ACTIVE AUDIO)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FlameRedPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Uploading an audio sermon will automatically delete & replace the previous uploaded audio sermon across devices.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                enabled = !isImportingAudio,
                                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_upload_audio_sermon")
                            ) {
                                if (isImportingAudio) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Importing Audio...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (attachedAudioDraft != null || (!existingAudioRemoved && editingResource?.hasAudio == true)) {
                                            "Replace Selected Audio File"
                                        } else {
                                            "Select Audio File (.mp3, .m4a, .aac, .wav)"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { audioBroadPickerLauncher.launch(arrayOf("*/*")) },
                                enabled = !isImportingAudio,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_browse_all_audio_folders")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp), tint = FlameRedPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Browse All Folders / Voice Notes (Downloads, WhatsApp, Drive)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FlameRedPrimary
                                )
                            }

                            val activeDraft = attachedAudioDraft
                            val activeExistingFileName = if (!existingAudioRemoved) editingResource?.audioFileName else null
                            if (activeDraft != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activeDraft.fileName,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1
                                            )
                                            val durLabel = if (activeDraft.durationMs > 0L) formatAudioDurationMs(activeDraft.durationMs) else "${activeDraft.durationMinutes} min"
                                            Text(
                                                text = "$durLabel • ${SpiritualResourceRepositoryImpl.formatFileSize(activeDraft.sizeBytes)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                discardStagedDraft(attachedAudioDraft)
                                                attachedAudioDraft = null
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear selected audio", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            } else if (!activeExistingFileName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Attached: $activeExistingFileName",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1
                                            )
                                            val sizeLabel = SpiritualResourceRepositoryImpl.formatFileSize(editingResource?.audioSizeBytes ?: 0L)
                                            if (sizeLabel.isNotBlank()) {
                                                Text(
                                                    text = sizeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { existingAudioRemoved = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove attached audio", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!isScriptureOfTheDay) {
                    OutlinedButton(
                        onClick = { documentPickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_import_resource_file")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = FlameRedPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = attachedFileName?.let { "Imported Notes: $it (Tap to Change)" }
                                ?: if (isSermon) {
                                    "Optional: Import Sermon Notes (.pdf / .epub / .txt)"
                                } else {
                                    "Import Devotional / Study Notes (.pdf / .epub / .txt)"
                                },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            localValidationError = null
                        },
                        label = { Text("Message Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Speaker / Author") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = scripture,
                    onValueChange = {
                        scripture = it
                        localValidationError = null
                    },
                    label = {
                        Text(
                            if (isScriptureOfTheDay) {
                                "Scripture Reference * (e.g. Judges 6:12)"
                            } else {
                                "Key Scripture Reference (e.g. Daniel 11:32)"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = summary,
                    onValueChange = {
                        summary = it
                        localValidationError = null
                    },
                    label = {
                        Text(
                            when {
                                isScriptureOfTheDay -> "Scripture Verse Text *"
                                isSermon -> "Brief Summary (Optional if Audio attached)"
                                else -> "Brief Summary *"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (isScriptureOfTheDay) 2 else 1
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = body,
                    onValueChange = {
                        body = it
                        localValidationError = null
                    },
                    label = {
                        Text(
                            when {
                                isScriptureOfTheDay -> "Prophetic Charge / Daily Reflection (Optional)"
                                isSermon -> "Message Outline / Transcript (Optional if Audio attached)"
                                else -> "Full Message Outline / Prayer Points *"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                if (isSermon) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { input ->
                            durationText = input.filter { it.isDigit() }.take(3)
                        },
                        label = { Text("Audio Duration (Minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                localValidationError?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isImportingAudio,
                onClick = {
                    if (isScriptureOfTheDay) {
                        if (scripture.isBlank() || summary.isBlank()) {
                            localValidationError = "Please enter both the Scripture Reference and the Verse Text."
                            return@Button
                        }
                        val resolvedTitle = "Scripture for the Day • ${scripture.trim()}"
                        val resolvedBody = if (body.isNotBlank()) {
                            "${summary.trim()}\n\nGideonites Youth Charge:\n${body.trim()}"
                        } else {
                            summary.trim()
                        }
                        onSubmit(
                            resolvedTitle,
                            selectedCategory,
                            author.ifBlank { defaultAuthorName },
                            scripture.trim(),
                            summary.trim(),
                            resolvedBody,
                            0,
                            null,
                            false
                        )
                    } else {
                        val hasAudioAttached = isSermon &&
                            (attachedAudioDraft != null || (!existingAudioRemoved && editingResource?.hasAudio == true))
                        if (title.isBlank() || (summary.isBlank() && body.isBlank() && !hasAudioAttached)) {
                            localValidationError = "Please enter a Message Title and either an audio file or message content."
                            return@Button
                        }
                        val parsedDuration = if (isSermon) {
                            attachedAudioDraft?.durationMinutes ?: (durationText.toIntOrNull() ?: 30)
                        } else {
                            0
                        }
                        val defaultAudioLabel = attachedAudioDraft?.fileName ?: editingResource?.audioFileName ?: title.trim()
                        val resolvedSummary = summary.trim().ifBlank {
                            body.trim().take(140).ifBlank { "Audio Sermon • $defaultAudioLabel" }
                        }
                        val resolvedBody = body.trim().ifBlank { resolvedSummary }
                        onSubmit(
                            title.trim(),
                            selectedCategory,
                            author.ifBlank { defaultAuthorName },
                            scripture.trim(),
                            resolvedSummary,
                            resolvedBody,
                            parsedDuration,
                            if (isSermon) attachedAudioDraft else null,
                            existingAudioRemoved
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text(if (editingResource != null) "Save Changes" else "Publish to App")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                discardStagedDraft(attachedAudioDraft)
                onDismiss()
            }) { Text("Cancel") }
        }
    )
}
