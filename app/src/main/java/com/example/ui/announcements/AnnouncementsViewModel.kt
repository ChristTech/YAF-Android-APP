package com.example.ui.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.model.AnnouncementAudience
import com.example.data.model.AnnouncementPriority
import com.example.data.model.UserRole
import com.example.data.repository.AnnouncementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class AnnouncementsUiState(
    val selectedPriority: AnnouncementPriority? = null,
    val isCreateOpen: Boolean = false,
    val message: String? = null
)

class AnnouncementsViewModel(
    private val announcementRepository: AnnouncementRepository,
    sessionRole: UserRole,
    private val notificationService: com.example.core.notification.FellowshipNotificationService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnnouncementsUiState())
    val uiState: StateFlow<AnnouncementsUiState> = _uiState.asStateFlow()

    val announcements: StateFlow<List<AnnouncementEntity>> = announcementRepository
        .getAnnouncementsForAudience(sessionRole)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun filterByPriority(priority: AnnouncementPriority?) {
        _uiState.value = _uiState.value.copy(selectedPriority = priority)
    }

    fun setCreateOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isCreateOpen = open)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun publishAnnouncement(
        title: String,
        content: String,
        priority: AnnouncementPriority,
        audience: AnnouncementAudience,
        session: SessionEntity
    ) {
        if (title.isBlank() || content.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Title and content cannot be empty.")
            return
        }

        viewModelScope.launch {
            val announcement = AnnouncementEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                content = content.trim(),
                priority = priority,
                targetAudience = audience,
                authorId = session.userId,
                authorName = session.userName,
                authorRole = session.userRole.displayName,
                publishedAt = System.currentTimeMillis(),
                isPinned = priority == AnnouncementPriority.URGENT
            )

            announcementRepository.publishAnnouncement(announcement, session.userRole)

            _uiState.value = _uiState.value.copy(
                isCreateOpen = false,
                message = "Official announcement published to The Gideonites - The Youth Alive Fellowship!"
            )
        }
    }

    companion object {
        fun provideFactory(
            announcementRepository: AnnouncementRepository,
            sessionRole: UserRole,
            notificationService: com.example.core.notification.FellowshipNotificationService? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AnnouncementsViewModel(announcementRepository, sessionRole, notificationService) as T
                }
            }
    }
}
