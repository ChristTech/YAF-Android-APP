package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.SpiritualResourceEntity
import com.example.data.model.EventCategory
import com.example.data.model.EventStatus
import com.example.data.model.UserRole
import com.example.data.repository.EventRepository
import com.example.data.repository.RegistrationResult
import com.example.data.repository.SpiritualResourceRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardUiState(
    val selectedCategory: EventCategory? = null,
    val selectedEvent: EventEntity? = null,
    val isCreateEventOpen: Boolean = false,
    val isPostScriptureOpen: Boolean = false,
    val confirmedPassEvent: EventEntity? = null,
    val confirmedPassEmail: String = "",
    val confirmedPassReference: String = "",
    val backgroundEmailDispatched: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class DashboardViewModel(
    private val eventRepository: EventRepository,
    private val notificationService: com.example.core.notification.FellowshipNotificationService? = null,
    private val spiritualResourceRepository: SpiritualResourceRepository? = null,
    private val userRepository: com.example.data.repository.UserRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val allEvents: StateFlow<List<EventEntity>> = eventRepository.getAllEvents()
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { !com.example.data.repository.EventRepositoryImpl.isEventDatePassed(it.dateTimeMillis, now) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        purgeExpiredEvents()
    }

    fun purgeExpiredEvents() {
        viewModelScope.launch {
            try {
                eventRepository.purgeExpiredEvents()
            } catch (_: Exception) {
            }
        }
    }

    val allRegistrations: StateFlow<List<com.example.data.local.entity.EventRegistrationEntity>> =
        eventRepository.getAllRegistrations()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val dailyScripture: StateFlow<SpiritualResourceEntity?> =
        (spiritualResourceRepository?.getAllResources() ?: MutableStateFlow(emptyList()))
            .map { list ->
                list.firstOrNull { it.category.equals("SCRIPTURE_OF_THE_DAY", ignoreCase = true) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    fun observeUserRegistrations(userId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.EventRegistrationEntity>> =
        eventRepository.getRegistrationsForUser(userId)

    fun selectCategory(category: EventCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun selectEvent(event: EventEntity?) {
        _uiState.value = _uiState.value.copy(selectedEvent = event)
    }

    fun dismissConfirmedPass() {
        _uiState.value = _uiState.value.copy(confirmedPassEvent = null)
    }

    fun setCreateEventOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isCreateEventOpen = open)
    }

    fun setPostScriptureOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isPostScriptureOpen = open)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun postScriptureOfTheDay(
        reference: String,
        verseText: String,
        propheticCharge: String,
        session: SessionEntity
    ) {
        if (reference.isBlank() || verseText.isBlank()) {
            _uiState.value = _uiState.value.copy(
                message = "Scripture reference and verse text are required.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            val resource = SpiritualResourceEntity(
                id = UUID.randomUUID().toString(),
                title = "Scripture for the Day • ${reference.trim()}",
                category = "SCRIPTURE_OF_THE_DAY",
                speakerOrAuthor = session.userName.ifBlank { "Youth Pastor / Admin" },
                scriptureReference = reference.trim(),
                summary = verseText.trim(),
                contentBody = if (propheticCharge.isNotBlank()) {
                    "${verseText.trim()}\n\nGideonites Youth Charge:\n${propheticCharge.trim()}"
                } else {
                    verseText.trim()
                },
                audioDurationMinutes = 0,
                isBookmarked = false,
                publishedAt = System.currentTimeMillis()
            )
            spiritualResourceRepository?.addResource(resource)

            notificationService?.postNotification(
                id = "notif_scripture_${resource.id}",
                title = "📖 Scripture for the Day: ${reference.trim()}",
                message = "\"${verseText.trim()}\" — Posted by ${session.userName} for The Gideonites - The Youth Alive Fellowship.",
                type = com.example.data.local.entity.NotificationType.ANNOUNCEMENT,
                authorName = session.userName,
                authorRole = session.userRole.displayName
            )

            _uiState.value = _uiState.value.copy(
                isPostScriptureOpen = false,
                message = "Scripture for the Day (${reference.trim()}) posted for The Gideonites!",
                isError = false
            )
        }
    }

    fun processScheduledEmailReminders(session: SessionEntity) {
        viewModelScope.launch {
            val dueReminders = eventRepository.checkAndMarkDueEmailReminders(session.userId)
            val dateFmt = java.text.SimpleDateFormat("EEE, MMM dd • h:mm a", java.util.Locale.getDefault())
            for ((event, reg, daysBefore) in dueReminders) {
                val targetEmail = reg.userEmail.ifBlank { "your registered email" }
                val label = if (daysBefore == 5) "5 Days Before" else "1 Day Before (Tomorrow)"
                val subject = "Reminder ($label): ${event.title} • The Gideonites"
                val body = "Grace & Peace ${reg.userName},\n\nThis is your $label reminder that you are registered for '${event.title}' happening on ${dateFmt.format(java.util.Date(event.dateTimeMillis))} at ${event.venue}.\n\nIn His Service,\nThe Gideonites — The Youth Alive Fellowship"
                val sentViaApi = com.example.core.notification.FellowshipEmailService.trySendBackgroundEmail(
                    recipientEmail = targetEmail,
                    subject = subject,
                    textBody = body
                )
                notificationService?.postNotification(
                    title = "⏰ Programme Reminder ($label): ${event.title}",
                    message = if (sentViaApi) {
                        "Emailed to $targetEmail — '${event.title}' is happening in $daysBefore ${if (daysBefore == 1) "day" else "days"} on ${dateFmt.format(java.util.Date(event.dateTimeMillis))} at ${event.venue}."
                    } else {
                        "Reminder for ${reg.userName} ($targetEmail): '${event.title}' is happening in $daysBefore ${if (daysBefore == 1) "day" else "days"} on ${dateFmt.format(java.util.Date(event.dateTimeMillis))} at ${event.venue}."
                    },
                    type = com.example.data.local.entity.NotificationType.EVENT,
                    authorName = "The Gideonites Programme Desk",
                    authorRole = "Scheduled Reminder",
                    isUrgent = daysBefore == 1,
                    publishToCloud = false
                )
            }
        }
    }

    fun registerForEvent(
        event: EventEntity,
        session: SessionEntity
    ) {
        viewModelScope.launch {
            val userEntity = userRepository?.getUserByIdSync(session.userId)
            val firebaseEmail = try {
                com.google.firebase.Firebase.auth.currentUser?.email
            } catch (_: Exception) {
                null
            }
            val fallbackEmail = "${session.userName.lowercase().replace(" ", ".")}@youthalive.org"
            val resolvedEmail = userEntity?.email?.takeIf { it.isNotBlank() }
                ?: firebaseEmail?.takeIf { it.isNotBlank() }
                ?: fallbackEmail
            val resolvedPhone = userEntity?.phone ?: ""

            when (val result = eventRepository.registerForEvent(
                eventId = event.id,
                userId = session.userId,
                userName = session.userName,
                userEmail = resolvedEmail,
                userPhone = resolvedPhone,
                userRole = session.userRole
            )) {
                is RegistrationResult.Success -> {
                    val dateFmt = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    val fiveDaysBeforeMillis = event.dateTimeMillis - (5L * 24 * 60 * 60 * 1000L)
                    val oneDayBeforeMillis = event.dateTimeMillis - (1L * 24 * 60 * 60 * 1000L)
                    val fiveDayDateStr = dateFmt.format(java.util.Date(fiveDaysBeforeMillis))
                    val oneDayDateStr = dateFmt.format(java.util.Date(oneDayBeforeMillis))
                    val passRef = "GID-${event.id.replace(Regex("[^a-zA-Z0-9]"), "").take(4).uppercase()}-${session.userId.replace(Regex("[^a-zA-Z0-9]"), "").takeLast(4).uppercase()}"

                    val emailSubject = com.example.core.notification.FellowshipEmailService.buildConfirmationEmailSubject(event)
                    val emailBody = com.example.core.notification.FellowshipEmailService.buildConfirmationEmailBody(
                        event = event,
                        userName = session.userName,
                        userEmail = resolvedEmail,
                        passReference = passRef
                    )
                    val sentInBackground = com.example.core.notification.FellowshipEmailService.trySendBackgroundEmail(
                        recipientEmail = resolvedEmail,
                        subject = emailSubject,
                        textBody = emailBody
                    )

                    notificationService?.postNotification(
                        title = "🎟️ Registered: ${event.title} (Pass #$passRef)",
                        message = if (sentInBackground) {
                            "Confirmation email dispatched to $resolvedEmail. Automated reminders scheduled for $fiveDayDateStr (5 days before) and $oneDayDateStr (1 day before)."
                        } else {
                            "Registration Pass #$passRef confirmed! 5-day ($fiveDayDateStr) & 1-day ($oneDayDateStr) device reminders are active. Use 'Send Pass to My Email' to save a copy to $resolvedEmail."
                        },
                        type = com.example.data.local.entity.NotificationType.EVENT,
                        authorName = "The Gideonites Programme Desk",
                        authorRole = "Registration Confirmed",
                        publishToCloud = false
                    )

                    // Immediately dispatch if the event is already within the 5-day or 1-day window
                    processScheduledEmailReminders(session)

                    _uiState.value = _uiState.value.copy(
                        confirmedPassEvent = event.copy(registeredCount = event.registeredCount + 1),
                        confirmedPassEmail = resolvedEmail,
                        confirmedPassReference = passRef,
                        backgroundEmailDispatched = sentInBackground,
                        message = "Registration confirmed for ${event.title} (Pass #$passRef)!",
                        isError = false
                    )
                }
                is RegistrationResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        message = result.message,
                        isError = true
                    )
                }
            }
        }
    }

    fun createEvent(
        title: String,
        description: String,
        daysInFuture: Long,
        venue: String,
        speaker: String,
        category: EventCategory,
        limit: Int,
        session: SessionEntity
    ) {
        if (title.isBlank() || venue.isBlank()) {
            _uiState.value = _uiState.value.copy(
                message = "Title and venue are required.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            val dateTime = System.currentTimeMillis() + (daysInFuture * 24 * 60 * 60 * 1000L)
            val newEvent = EventEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                description = description.trim(),
                dateTimeMillis = dateTime,
                venue = venue.trim(),
                speaker = speaker.trim(),
                contactPerson = session.userName,
                category = category,
                participantLimit = limit,
                registeredCount = 0,
                status = EventStatus.UPCOMING,
                createdById = session.userId
            )

            eventRepository.createEvent(newEvent, session.userRole)

            _uiState.value = _uiState.value.copy(
                isCreateEventOpen = false,
                message = "Upcoming Youth Programme published successfully!",
                isError = false
            )
        }
    }

    companion object {
        fun provideFactory(
            eventRepository: EventRepository,
            notificationService: com.example.core.notification.FellowshipNotificationService? = null,
            spiritualResourceRepository: SpiritualResourceRepository? = null,
            userRepository: com.example.data.repository.UserRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(
                        eventRepository,
                        notificationService,
                        spiritualResourceRepository,
                        userRepository
                    ) as T
                }
            }
    }
}
