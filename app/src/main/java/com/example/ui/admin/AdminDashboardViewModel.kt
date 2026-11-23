package com.example.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.integration.ChmsSyncResult
import com.example.core.integration.DataExporter
import com.example.core.integration.MockChmsSyncAdapter
import com.example.core.integration.MockSmsGatewayAdapter
import com.example.core.integration.SmsBroadcastResult
import com.example.core.notification.FellowshipNotificationService
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CommunityPostEntity
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.SpiritualResourceEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.AccountStatus
import com.example.data.repository.AnnouncementRepository
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.BibleRepository
import com.example.data.repository.CommunityRepository
import com.example.data.repository.EventRepository
import com.example.data.repository.FinanceRepository
import com.example.data.repository.SpiritualResourceRepository
import com.example.data.repository.UploadedBibleEdition
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class UniversalSearchResult(
    val events: List<EventEntity> = emptyList(),
    val announcements: List<AnnouncementEntity> = emptyList(),
    val forumPosts: List<CommunityPostEntity> = emptyList(),
    val spiritualResources: List<SpiritualResourceEntity> = emptyList(),
    val members: List<UserEntity> = emptyList()
) {
    val totalCount: Int get() = events.size + announcements.size + forumPosts.size + spiritualResources.size + members.size
}

data class AdminDashboardUiState(
    val totalMembers: Int = 0,
    val pendingApprovalsCount: Int = 0,
    val pendingExecutives: List<UserEntity> = emptyList(),
    val totalGivingAmount: Double = 0.0,
    val totalAttendanceAcrossServices: Int = 0,
    val totalEventsCount: Int = 0,
    val spiritualResources: List<SpiritualResourceEntity> = emptyList(),
    val uploadedBibles: List<UploadedBibleEdition> = emptyList(),
    val searchQuery: String = "",
    val searchResult: UniversalSearchResult = UniversalSearchResult(),
    val recentAuditLogs: List<AuditLogEntity> = emptyList(),
    val activeTab: Int = 0, // 0: Overview, 1: Search, 2: Audit Logs & Sync
    val lastSyncResult: ChmsSyncResult? = null,
    val lastSmsResult: SmsBroadcastResult? = null,
    val exportedDataContent: String? = null,
    val exportedDataTitle: String? = null,
    val isExportDialogVisible: Boolean = false,
    val statusMessage: String? = null
)

class AdminDashboardViewModel(
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val announcementRepository: AnnouncementRepository,
    private val financeRepository: FinanceRepository,
    private val attendanceRepository: AttendanceRepository,
    private val communityRepository: CommunityRepository,
    private val spiritualResourceRepository: SpiritualResourceRepository,
    private val auditLogDao: AuditLogDao,
    private val bibleRepository: BibleRepository? = null,
    private val notificationService: FellowshipNotificationService? = null
) : ViewModel() {

    private val chmsAdapter = MockChmsSyncAdapter()
    private val smsAdapter = MockSmsGatewayAdapter()

    private val _searchQuery = MutableStateFlow("")
    private val _searchResult = MutableStateFlow(UniversalSearchResult())
    private val _activeTab = MutableStateFlow(0)
    private val _syncResult = MutableStateFlow<ChmsSyncResult?>(null)
    private val _smsResult = MutableStateFlow<SmsBroadcastResult?>(null)
    private val _exportDialog = MutableStateFlow<Pair<String, String>?>(null) // Title to Content
    private val _statusMessage = MutableStateFlow<String?>(null)

    private val _kpis = MutableStateFlow(AdminDashboardUiState())

    init {
        viewModelScope.launch {
            combine(
                userRepository.getAllUsers(),
                financeRepository.getTotalCompletedAmount(),
                attendanceRepository.getTotalAttendeesAcrossServices(),
                eventRepository.getAllEvents()
            ) { users, totalFinance, totalAtts, events ->
                val totalMembers = users.size
                val pendingList = users.filter { it.status == AccountStatus.PENDING_APPROVAL }
                val totalGiving = totalFinance ?: 0.0
                val eventsCount = events.size

                AdminDashboardUiState(
                    totalMembers = totalMembers,
                    pendingApprovalsCount = pendingList.size,
                    pendingExecutives = pendingList,
                    totalGivingAmount = totalGiving,
                    totalAttendanceAcrossServices = totalAtts ?: 0,
                    totalEventsCount = eventsCount
                )
            }.collect { kpiData ->
                _kpis.value = kpiData
            }
        }
    }

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        _kpis,
        _searchQuery,
        _searchResult,
        _activeTab
    ) { kpis, query, result, tab ->
        kpis.copy(
            searchQuery = query,
            searchResult = result,
            activeTab = tab
        )
    }.combine(spiritualResourceRepository.getAllResources()) { state, resources ->
        state.copy(spiritualResources = resources)
    }.combine(bibleRepository?.getUploadedEditionsFlow() ?: flowOf(emptyList())) { state, bibles ->
        state.copy(uploadedBibles = bibles)
    }.combine(auditLogDao.getRecentLogs(50)) { state, logs ->
        state.copy(recentAuditLogs = logs)
    }.combine(_syncResult) { state, sync ->
        state.copy(lastSyncResult = sync)
    }.combine(_smsResult) { state, sms ->
        state.copy(lastSmsResult = sms)
    }.combine(_exportDialog) { state, exportPair ->
        state.copy(
            isExportDialogVisible = exportPair != null,
            exportedDataTitle = exportPair?.first,
            exportedDataContent = exportPair?.second
        )
    }.combine(_statusMessage) { state, msg ->
        state.copy(statusMessage = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardUiState())

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun publishSpiritualResource(
        title: String,
        category: String,
        author: String,
        scriptureReference: String,
        summary: String,
        contentBody: String,
        durationMinutes: Int,
        adminUserId: String,
        adminUserRole: com.example.data.model.UserRole = com.example.data.model.UserRole.ADMINISTRATOR,
        audioDraft: com.example.data.repository.SermonAudioDraft? = null
    ) {
        val hasUploadedAudio = audioDraft != null && category.equals("SERMON", ignoreCase = true)
        if (title.isBlank() || (contentBody.isBlank() && summary.isBlank() && !hasUploadedAudio)) return
        viewModelScope.launch {
            val normalizedCategory = category.uppercase().trim()
            val resolvedSummary = summary.trim().ifBlank {
                contentBody.trim().take(120).ifBlank { "Audio Sermon • ${audioDraft?.fileName ?: title.trim()}" }
            }
            val resolvedBody = contentBody.trim().ifBlank { resolvedSummary }
            val resolvedDuration = if (normalizedCategory == "SERMON") {
                (audioDraft?.durationMinutes ?: durationMinutes).coerceAtLeast(1)
            } else {
                durationMinutes.coerceAtLeast(0)
            }
            val resource = SpiritualResourceEntity(
                id = "res_${UUID.randomUUID().toString().take(8)}",
                title = title.trim(),
                category = normalizedCategory,
                speakerOrAuthor = author.trim().ifBlank { "ECWA Youth Ministry" },
                scriptureReference = scriptureReference.trim().ifBlank { "2 Timothy 2:15" },
                summary = resolvedSummary,
                contentBody = resolvedBody,
                audioDurationMinutes = resolvedDuration,
                publishedAt = System.currentTimeMillis(),
                isBookmarked = false,
                audioUri = if (hasUploadedAudio) audioDraft?.audioUri else null,
                audioCloudId = if (hasUploadedAudio) audioDraft?.audioCloudId else null,
                audioFileName = if (hasUploadedAudio) audioDraft?.fileName else null,
                audioMimeType = if (hasUploadedAudio) audioDraft?.mimeType else null,
                audioSizeBytes = if (hasUploadedAudio) audioDraft?.sizeBytes ?: 0L else 0L,
                audioChunkCount = 0
            )
            spiritualResourceRepository.addResource(resource)
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = adminUserId,
                    userRole = adminUserRole,
                    actionType = "PUBLISH_${normalizedCategory}",
                    targetEntity = "SpiritualResource",
                    targetId = resource.id,
                    details = if (hasUploadedAudio) {
                        "Uploaded audio sermon '${resource.title}' (${audioDraft?.fileName}) replacing previous audio sermon"
                    } else {
                        "Published $normalizedCategory '${resource.title}' (${resource.scriptureReference})"
                    }
                )
            )
            val label = when {
                hasUploadedAudio -> "Audio Sermon"
                normalizedCategory == "DEVOTIONAL" -> "Daily Devotional"
                normalizedCategory == "SERMON" -> "Sermon"
                else -> "Bible Study"
            }
            notificationService?.postNotification(
                title = "New $label Published: ${resource.title}",
                message = "${resource.scriptureReference} — ${resource.summary}",
                type = com.example.data.local.entity.NotificationType.ANNOUNCEMENT,
                authorName = resource.speakerOrAuthor,
                authorRole = adminUserRole.displayName
            )
            _statusMessage.value = if (hasUploadedAudio) {
                "Audio Sermon '${resource.title}' uploaded & replaced previous sermon audio across devices."
            } else {
                "$label '${resource.title}' published & synced across devices."
            }
        }
    }

    fun deleteSpiritualResource(
        resource: SpiritualResourceEntity,
        adminUserId: String,
        adminUserRole: com.example.data.model.UserRole = com.example.data.model.UserRole.ADMINISTRATOR
    ) {
        viewModelScope.launch {
            spiritualResourceRepository.deleteResource(resource.id)
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = adminUserId,
                    userRole = adminUserRole,
                    actionType = "DELETE_SPIRITUAL_RESOURCE",
                    targetEntity = "SpiritualResource",
                    targetId = resource.id,
                    details = "Deleted ${resource.category} '${resource.title}'"
                )
            )
            _statusMessage.value = "'${resource.title}' removed."
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResult.value = UniversalSearchResult()
            } else {
                val q = query.trim().lowercase()
                val events = eventRepository.getAllEvents().first().filter {
                    it.title.lowercase().contains(q) || it.venue.lowercase().contains(q)
                }
                val announcements = announcementRepository.getAllAnnouncements().first().filter {
                    it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
                }
                val posts = communityRepository.getActivePosts().first().filter {
                    it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
                }
                val resources = spiritualResourceRepository.getAllResources().first().filter {
                    it.title.lowercase().contains(q) || it.summary.lowercase().contains(q)
                }
                val members = userRepository.getAllUsers().first().filter {
                    it.fullName.lowercase().contains(q) || it.email.lowercase().contains(q)
                }

                _searchResult.value = UniversalSearchResult(
                    events = events,
                    announcements = announcements,
                    forumPosts = posts,
                    spiritualResources = resources,
                    members = members
                )
            }
        }
    }

    fun setTab(tabIndex: Int) {
        _activeTab.value = tabIndex
    }

    fun approveExecutive(userId: String, adminUserId: String) {
        viewModelScope.launch {
            userRepository.updateUserStatus(userId, AccountStatus.APPROVED, adminUserId)
        }
    }

    fun declineExecutive(userId: String, adminUserId: String) {
        viewModelScope.launch {
            userRepository.updateUserStatus(userId, AccountStatus.SUSPENDED, adminUserId)
        }
    }

    fun clearDatabasePreserveAdmin() {
        viewModelScope.launch {
            _statusMessage.value = "Clearing local & online database (preserving Admin only)..."
            val result = userRepository.clearAllDatabasesPreserveAdmin()
            result.fold(
                onSuccess = { msg ->
                    _searchQuery.value = ""
                    _searchResult.value = UniversalSearchResult()
                    _statusMessage.value = msg
                },
                onFailure = { err ->
                    _statusMessage.value = "Database clear error: ${err.localizedMessage ?: "Please try again."}"
                }
            )
        }
    }

    fun triggerChmsSync() {
        viewModelScope.launch {
            val res = chmsAdapter.synchronizeWithCentralChurchSystem()
            _syncResult.value = res
        }
    }

    fun sendSmsBroadcast(message: String) {
        viewModelScope.launch {
            val users = userRepository.getAllUsers().first()
            val phoneList = users.map { it.phone }.filter { it.isNotBlank() }
            val res = smsAdapter.sendSmsBroadcast(phoneList, message)
            _smsResult.value = res
        }
    }

    fun exportFinancesCsv() {
        viewModelScope.launch {
            val records = financeRepository.getAllFinancialRecords().first()
            val csv = DataExporter.exportFinancesToCsv(records)
            _exportDialog.value = "Financial Ledger Export (CSV)" to csv
        }
    }

    fun exportAttendanceCsv() {
        viewModelScope.launch {
            val records = attendanceRepository.getAllAttendance().first()
            val csv = DataExporter.exportAttendanceToCsv(records)
            _exportDialog.value = "Attendance Registry Export (CSV)" to csv
        }
    }

    fun exportMembersCsv() {
        viewModelScope.launch {
            val members = userRepository.getAllUsers().first()
            val csv = DataExporter.exportMembersToCsv(members)
            _exportDialog.value = "Member Roster Export (CSV)" to csv
        }
    }

    fun dismissExportDialog() {
        _exportDialog.value = null
    }

    companion object {
        fun provideFactory(
            userRepository: UserRepository,
            eventRepository: EventRepository,
            announcementRepository: AnnouncementRepository,
            financeRepository: FinanceRepository,
            attendanceRepository: AttendanceRepository,
            communityRepository: CommunityRepository,
            spiritualResourceRepository: SpiritualResourceRepository,
            auditLogDao: AuditLogDao,
            bibleRepository: BibleRepository? = null,
            notificationService: FellowshipNotificationService? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AdminDashboardViewModel(
                    userRepository = userRepository,
                    eventRepository = eventRepository,
                    announcementRepository = announcementRepository,
                    financeRepository = financeRepository,
                    attendanceRepository = attendanceRepository,
                    communityRepository = communityRepository,
                    spiritualResourceRepository = spiritualResourceRepository,
                    auditLogDao = auditLogDao,
                    bibleRepository = bibleRepository,
                    notificationService = notificationService
                ) as T
            }
        }
    }
}
