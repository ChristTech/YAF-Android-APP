package com.example.ui.spiritual

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.NotificationType
import com.example.data.local.entity.SpiritualResourceEntity
import com.example.data.repository.SermonAudioDraft
import com.example.data.repository.SpiritualResourceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class SpiritualUiState(
    val resources: List<SpiritualResourceEntity> = emptyList(),
    val filteredResources: List<SpiritualResourceEntity> = emptyList(),
    val selectedCategory: String = "All",
    val showBookmarksOnly: Boolean = false,
    val selectedResourceForReading: SpiritualResourceEntity? = null,
    val playingAudioResourceId: String? = null,
    val audioProgress: Float = 0f,
    val audioCurrentPositionMs: Long = 0L,
    val audioDurationMs: Long = 0L,
    val isPlayingAudio: Boolean = false,
    val isLoadingAudio: Boolean = false,
    val isUploadingAudio: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class SpiritualViewModel(
    private val spiritualResourceRepository: SpiritualResourceRepository,
    private val notificationService: com.example.core.notification.FellowshipNotificationService? = null
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("All")
    private val _showBookmarksOnly = MutableStateFlow(false)
    private val _selectedForReadingId = MutableStateFlow<String?>(null)
    private val _playingAudioId = MutableStateFlow<String?>(null)
    private val _isPlaying = MutableStateFlow(false)
    private val _isLoadingAudio = MutableStateFlow(false)
    private val _isUploadingAudio = MutableStateFlow(false)
    private val _audioProgress = MutableStateFlow(0f)
    private val _audioCurrentPositionMs = MutableStateFlow(0L)
    private val _audioDurationMs = MutableStateFlow(0L)
    private val _successMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private var audioTickerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var preparedAudioResourceId: String? = null
    private var playbackJob: Job? = null

    private val resourcesList = MutableStateFlow<List<SpiritualResourceEntity>>(emptyList())

    val uiState: StateFlow<SpiritualUiState> = combine(
        spiritualResourceRepository.getAllResources(),
        _selectedCategory,
        _showBookmarksOnly,
        _selectedForReadingId
    ) { res, cat, bookmarksOnly, readingId ->
        resourcesList.value = res
        var list = res
        if (bookmarksOnly) {
            list = list.filter { it.isBookmarked }
        } else if (cat != "All") {
            list = list.filter { it.category.equals(cat, ignoreCase = true) }
        }
        val activeReading = readingId?.let { id -> res.find { it.id == id } }
        SpiritualUiState(
            resources = res,
            filteredResources = list,
            selectedCategory = cat,
            showBookmarksOnly = bookmarksOnly,
            selectedResourceForReading = activeReading
        )
    }.combine(_playingAudioId) { s, id ->
        s.copy(playingAudioResourceId = id)
    }.combine(_isPlaying) { s, playing ->
        s.copy(isPlayingAudio = playing)
    }.combine(_isLoadingAudio) { s, loading ->
        s.copy(isLoadingAudio = loading)
    }.combine(_isUploadingAudio) { s, uploading ->
        s.copy(isUploadingAudio = uploading)
    }.combine(_audioProgress) { s, prog ->
        s.copy(audioProgress = prog)
    }.combine(_audioCurrentPositionMs) { s, pos ->
        s.copy(audioCurrentPositionMs = pos)
    }.combine(_audioDurationMs) { s, dur ->
        s.copy(audioDurationMs = dur)
    }.combine(_successMessage) { s, msg ->
        s.copy(successMessage = msg)
    }.combine(_errorMessage) { s, err ->
        s.copy(errorMessage = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpiritualUiState())

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
        _showBookmarksOnly.value = false
    }

    fun toggleBookmarksFilter() {
        _showBookmarksOnly.value = !_showBookmarksOnly.value
    }

    fun toggleBookmark(resource: SpiritualResourceEntity) {
        viewModelScope.launch {
            spiritualResourceRepository.toggleBookmark(resource.id, resource.isBookmarked)
            _successMessage.value = if (!resource.isBookmarked) "Saved to offline bookmarks" else "Removed from bookmarks"
        }
    }

    fun openReader(resource: SpiritualResourceEntity) {
        _selectedForReadingId.value = resource.id
    }

    fun closeReader() {
        _selectedForReadingId.value = null
    }

    fun toggleAudioPlayback(resourceId: String) {
        val target = resourcesList.value.find { it.id == resourceId }
        if (target == null) {
            stopAudioPlayback()
            return
        }

        if (!target.hasAudio) {
            // Legacy TTS fallback for text-only devotionals & scriptures
            releaseMediaPlayerOnly()
            if (_playingAudioId.value == resourceId) {
                val nextPlaying = !_isPlaying.value
                _isPlaying.value = nextPlaying
                if (nextPlaying) {
                    startSimulatedTtsProgressTicker()
                } else {
                    audioTickerJob?.cancel()
                }
            } else {
                _playingAudioId.value = resourceId
                _audioProgress.value = 0.04f
                _audioCurrentPositionMs.value = 0L
                _audioDurationMs.value = 0L
                _isPlaying.value = true
                startSimulatedTtsProgressTicker()
            }
            return
        }

        // Real audio sermon playback with MediaPlayer
        val activePlayer = mediaPlayer
        if (_playingAudioId.value == resourceId && preparedAudioResourceId == resourceId && activePlayer != null) {
            try {
                if (activePlayer.isPlaying) {
                    activePlayer.pause()
                    _isPlaying.value = false
                    audioTickerJob?.cancel()
                } else {
                    activePlayer.start()
                    _isPlaying.value = true
                    startRealAudioProgressTicker()
                }
                return
            } catch (_: Exception) {
                // Fall through to re-prepare
            }
        }

        playbackJob?.cancel()
        releaseMediaPlayerOnly()
        _playingAudioId.value = resourceId
        _isLoadingAudio.value = true
        _isPlaying.value = false
        _audioProgress.value = 0f
        _audioCurrentPositionMs.value = 0L
        _audioDurationMs.value = 0L

        playbackJob = viewModelScope.launch {
            val fileResult = spiritualResourceRepository.ensureAudioFileReady(target)
            val file = fileResult.getOrElse { err ->
                _isLoadingAudio.value = false
                _playingAudioId.value = null
                _errorMessage.value = err.message ?: "Unable to load audio sermon for playback."
                return@launch
            }

            if (_playingAudioId.value != resourceId) return@launch

            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    setOnPreparedListener { player ->
                        if (_playingAudioId.value != resourceId) {
                            runCatching { player.release() }
                            return@setOnPreparedListener
                        }
                        preparedAudioResourceId = resourceId
                        _isLoadingAudio.value = false
                        val dur = player.duration.toLong().coerceAtLeast(1L)
                        _audioDurationMs.value = dur
                        _audioCurrentPositionMs.value = 0L
                        _audioProgress.value = 0f
                        player.start()
                        _isPlaying.value = true
                        startRealAudioProgressTicker()
                    }
                    setOnCompletionListener {
                        audioTickerJob?.cancel()
                        _isPlaying.value = false
                        _audioProgress.value = 0f
                        _audioCurrentPositionMs.value = 0L
                        runCatching { it.seekTo(0) }
                    }
                    setOnErrorListener { _, what, extra ->
                        audioTickerJob?.cancel()
                        _isLoadingAudio.value = false
                        _isPlaying.value = false
                        _playingAudioId.value = null
                        _errorMessage.value = "Audio playback failed (code $what/$extra). Try re-uploading an MP3/M4A file."
                        releaseMediaPlayerOnly()
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                _isLoadingAudio.value = false
                _isPlaying.value = false
                _playingAudioId.value = null
                _errorMessage.value = "Unable to play audio sermon: ${e.message}"
                releaseMediaPlayerOnly()
            }
        }
    }

    fun seekToFraction(resourceId: String, fraction: Float) {
        val player = mediaPlayer ?: return
        if (_playingAudioId.value != resourceId || preparedAudioResourceId != resourceId) return
        try {
            val dur = player.duration.coerceAtLeast(1)
            val targetMs = (fraction.coerceIn(0f, 1f) * dur).toInt()
            player.seekTo(targetMs)
            _audioCurrentPositionMs.value = targetMs.toLong()
            _audioDurationMs.value = dur.toLong()
            _audioProgress.value = (targetMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
        } catch (_: Exception) {
        }
    }

    fun skipByMs(resourceId: String, deltaMs: Long) {
        val player = mediaPlayer ?: return
        if (_playingAudioId.value != resourceId || preparedAudioResourceId != resourceId) return
        try {
            val dur = player.duration.coerceAtLeast(1)
            val nextMs = (player.currentPosition.toLong() + deltaMs).coerceIn(0L, dur.toLong())
            player.seekTo(nextMs.toInt())
            _audioCurrentPositionMs.value = nextMs
            _audioDurationMs.value = dur.toLong()
            _audioProgress.value = (nextMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
        } catch (_: Exception) {
        }
    }

    fun stopAudioPlayback() {
        playbackJob?.cancel()
        audioTickerJob?.cancel()
        releaseMediaPlayerOnly()
        _isLoadingAudio.value = false
        _isPlaying.value = false
        _audioProgress.value = 0f
        _audioCurrentPositionMs.value = 0L
        _audioDurationMs.value = 0L
        _playingAudioId.value = null
    }

    private fun releaseMediaPlayerOnly() {
        preparedAudioResourceId = null
        val old = mediaPlayer
        mediaPlayer = null
        if (old != null) {
            runCatching {
                if (old.isPlaying) old.stop()
            }
            runCatching { old.release() }
        }
    }

    private fun startRealAudioProgressTicker() {
        audioTickerJob?.cancel()
        audioTickerJob = viewModelScope.launch {
            while (isActive && _isPlaying.value) {
                val player = mediaPlayer ?: break
                try {
                    val dur = player.duration.toLong().coerceAtLeast(1L)
                    val pos = player.currentPosition.toLong().coerceIn(0L, dur)
                    _audioDurationMs.value = dur
                    _audioCurrentPositionMs.value = pos
                    _audioProgress.value = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    if (!player.isPlaying) {
                        _isPlaying.value = false
                        break
                    }
                } catch (_: Exception) {
                    break
                }
                delay(250L)
            }
        }
    }

    private fun startSimulatedTtsProgressTicker() {
        audioTickerJob?.cancel()
        audioTickerJob = viewModelScope.launch {
            while (isActive && _isPlaying.value) {
                delay(600L)
                val next = _audioProgress.value + 0.015f
                if (next >= 1f) {
                    _audioProgress.value = 1f
                    _isPlaying.value = false
                    break
                } else {
                    _audioProgress.value = next
                }
            }
        }
    }

    fun publishResource(
        title: String,
        category: String,
        author: String,
        scripture: String,
        summary: String,
        content: String,
        duration: Int,
        audioDraft: SermonAudioDraft? = null
    ) {
        val isScriptureOfTheDay = category.equals("SCRIPTURE_OF_THE_DAY", ignoreCase = true)
        val cleanScripture = scripture.trim()
        val cleanSummary = summary.trim()
        val cleanContent = content.trim()
        val cleanTitle = if (isScriptureOfTheDay) {
            title.trim().ifBlank { "Scripture for the Day • ${cleanScripture.ifBlank { "Judges 6:12" }}" }
        } else {
            title.trim()
        }

        if (cleanTitle.isBlank() || (cleanSummary.isBlank() && cleanContent.isBlank() && audioDraft == null)) {
            _errorMessage.value = "Please provide a title/reference and scripture, message content, or audio file."
            return
        }

        viewModelScope.launch {
            val hasUploadedAudio = audioDraft != null && category.equals("SERMON", ignoreCase = true)
            if (hasUploadedAudio) {
                stopAudioPlayback()
                _isUploadingAudio.value = true
            }
            try {
                val resolvedSummary = cleanSummary.ifBlank {
                    cleanContent.take(160).ifBlank { "Audio Sermon • ${audioDraft?.fileName ?: cleanTitle}" }
                }
                val resolvedBody = cleanContent.ifBlank { resolvedSummary }
                val resolvedDuration = if (category.equals("SERMON", ignoreCase = true)) {
                    (audioDraft?.durationMinutes ?: duration).coerceAtLeast(1)
                } else {
                    0
                }
                val resource = SpiritualResourceEntity(
                    id = UUID.randomUUID().toString(),
                    title = cleanTitle,
                    category = category,
                    speakerOrAuthor = author.trim().ifBlank { "Youth Leadership" },
                    scriptureReference = cleanScripture,
                    summary = resolvedSummary,
                    contentBody = resolvedBody,
                    audioDurationMinutes = resolvedDuration,
                    isBookmarked = false,
                    publishedAt = System.currentTimeMillis(),
                    audioUri = if (hasUploadedAudio) audioDraft?.audioUri else null,
                    audioCloudId = if (hasUploadedAudio) audioDraft?.audioCloudId else null,
                    audioFileName = if (hasUploadedAudio) audioDraft?.fileName else null,
                    audioMimeType = if (hasUploadedAudio) audioDraft?.mimeType else null,
                    audioSizeBytes = if (hasUploadedAudio) audioDraft?.sizeBytes ?: 0L else 0L,
                    audioChunkCount = 0
                )
                val result = spiritualResourceRepository.addResource(resource)
                result.onFailure { err ->
                    _errorMessage.value = err.message ?: "Unable to publish spiritual resource."
                    return@launch
                }

                if (isScriptureOfTheDay) {
                    notificationService?.postNotification(
                        id = "notif_scripture_${resource.id}",
                        title = "📖 Scripture for the Day: ${cleanScripture.ifBlank { cleanTitle }}",
                        message = "\"$resolvedSummary\" — Posted by ${resource.speakerOrAuthor} for The Gideonites - The Youth Alive Fellowship.",
                        type = NotificationType.ANNOUNCEMENT,
                        authorName = resource.speakerOrAuthor,
                        authorRole = "Youth Leadership"
                    )
                }

                _successMessage.value = when {
                    hasUploadedAudio -> "Audio sermon uploaded & replaced previous audio sermon!"
                    isScriptureOfTheDay -> "Scripture for the Day published across all devices!"
                    else -> "Spiritual resource published across all devices!"
                }
            } finally {
                _isUploadingAudio.value = false
            }
        }
    }

    fun editResource(
        resourceId: String,
        title: String,
        category: String,
        author: String,
        scripture: String,
        summary: String,
        content: String,
        duration: Int,
        existingResource: SpiritualResourceEntity,
        audioDraft: SermonAudioDraft? = null,
        audioRemoved: Boolean = false
    ) {
        val isScriptureOfTheDay = category.equals("SCRIPTURE_OF_THE_DAY", ignoreCase = true)
        val cleanScripture = scripture.trim()
        val cleanSummary = summary.trim()
        val cleanContent = content.trim()
        val cleanTitle = if (isScriptureOfTheDay) {
            title.trim().ifBlank { "Scripture for the Day • ${cleanScripture.ifBlank { "Judges 6:12" }}" }
        } else {
            title.trim()
        }

        val keepsOrHasAudio = category.equals("SERMON", ignoreCase = true) &&
            (audioDraft != null || (!audioRemoved && existingResource.hasAudio))

        if (cleanTitle.isBlank() || (cleanSummary.isBlank() && cleanContent.isBlank() && !keepsOrHasAudio)) {
            _errorMessage.value = "Please provide a title/reference and message content or audio file."
            return
        }

        viewModelScope.launch {
            if (audioDraft != null || audioRemoved) {
                if (_playingAudioId.value == resourceId) {
                    stopAudioPlayback()
                }
                if (audioDraft != null) {
                    _isUploadingAudio.value = true
                }
            }
            try {
                val resolvedSummary = cleanSummary.ifBlank {
                    cleanContent.take(160).ifBlank {
                        "Audio Sermon • ${audioDraft?.fileName ?: existingResource.audioFileName ?: cleanTitle}"
                    }
                }
                val resolvedBody = cleanContent.ifBlank { resolvedSummary }
                val resolvedDuration = if (category.equals("SERMON", ignoreCase = true)) {
                    (audioDraft?.durationMinutes ?: duration).coerceAtLeast(1)
                } else {
                    0
                }
                val updated = when {
                    !category.equals("SERMON", ignoreCase = true) || audioRemoved -> existingResource.copy(
                        id = resourceId,
                        title = cleanTitle,
                        category = category,
                        speakerOrAuthor = author.trim().ifBlank { existingResource.speakerOrAuthor },
                        scriptureReference = cleanScripture,
                        summary = resolvedSummary,
                        contentBody = resolvedBody,
                        audioDurationMinutes = resolvedDuration,
                        audioUri = null,
                        audioCloudId = null,
                        audioFileName = null,
                        audioMimeType = null,
                        audioSizeBytes = 0L,
                        audioChunkCount = 0
                    )
                    audioDraft != null -> existingResource.copy(
                        id = resourceId,
                        title = cleanTitle,
                        category = category,
                        speakerOrAuthor = author.trim().ifBlank { existingResource.speakerOrAuthor },
                        scriptureReference = cleanScripture,
                        summary = resolvedSummary,
                        contentBody = resolvedBody,
                        audioDurationMinutes = resolvedDuration,
                        audioUri = audioDraft.audioUri,
                        audioCloudId = audioDraft.audioCloudId,
                        audioFileName = audioDraft.fileName,
                        audioMimeType = audioDraft.mimeType,
                        audioSizeBytes = audioDraft.sizeBytes,
                        audioChunkCount = 0
                    )
                    else -> existingResource.copy(
                        id = resourceId,
                        title = cleanTitle,
                        category = category,
                        speakerOrAuthor = author.trim().ifBlank { existingResource.speakerOrAuthor },
                        scriptureReference = cleanScripture,
                        summary = resolvedSummary,
                        contentBody = resolvedBody,
                        audioDurationMinutes = resolvedDuration
                    )
                }
                val result = spiritualResourceRepository.updateResource(updated)
                result.onFailure { err ->
                    _errorMessage.value = err.message ?: "Unable to update spiritual resource."
                    return@launch
                }
                _successMessage.value = if (audioDraft != null) {
                    "Audio sermon updated & previous sermon audio replaced!"
                } else {
                    "Spiritual resource updated across all devices!"
                }
            } finally {
                _isUploadingAudio.value = false
            }
        }
    }

    fun deleteResource(resourceId: String) {
        viewModelScope.launch {
            if (_playingAudioId.value == resourceId) {
                stopAudioPlayback()
            }
            if (_selectedForReadingId.value == resourceId) {
                _selectedForReadingId.value = null
            }
            val result = spiritualResourceRepository.deleteResource(resourceId)
            result.onSuccess {
                _successMessage.value = "Resource deleted"
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Unable to delete resource."
            }
        }
    }

    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioPlayback()
    }

    companion object {
        fun provideFactory(
            spiritualResourceRepository: SpiritualResourceRepository,
            notificationService: com.example.core.notification.FellowshipNotificationService? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SpiritualViewModel(spiritualResourceRepository, notificationService) as T
            }
        }
    }
}

