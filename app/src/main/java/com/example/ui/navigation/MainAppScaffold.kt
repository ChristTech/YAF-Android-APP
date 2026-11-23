package com.example.ui.navigation

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ChurchYouthApp
import com.example.R
import com.example.data.local.entity.SessionEntity
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.admin.AdminDashboardViewModel
import com.example.ui.announcements.AnnouncementsScreen
import com.example.ui.announcements.AnnouncementsViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.community.CommunityScreen
import com.example.ui.community.CommunityViewModel
import com.example.ui.components.RoleBadge
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.ProgrammeDashboardScreen
import com.example.ui.departments.DepartmentScreen
import com.example.ui.departments.DepartmentViewModel
import com.example.ui.engagement.EngagementViewModel
import com.example.ui.engagement.GamesAndReportsScreen
import com.example.ui.finance.FinanceScreen
import com.example.ui.finance.FinanceViewModel
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.ProfileViewModel
import com.example.ui.spiritual.SpiritualScreen
import com.example.ui.spiritual.SpiritualViewModel
import com.example.ui.notifications.NotificationCenterDialog
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver
import com.example.ui.theme.ThemeManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class AppNavTab(val label: String, val icon: ImageVector, val tag: String) {
    PROGRAMMES("Home", Icons.Default.Home, "nav_programmes"),
    COMMUNITY("Community", Icons.Default.Group, "nav_community"),
    GAMES("Games", Icons.Default.SportsEsports, "nav_games"),
    SPIRITUAL("Spiritual", Icons.Default.AutoStories, "nav_spiritual"),
    PROFILE("Profile", Icons.Default.Person, "nav_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    initialOpenNotifications: Boolean = false,
    onNotificationsIntentConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as ChurchYouthApp
    val container = app.container

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.provideFactory(container.userRepository)
    )
    val authUiState by authViewModel.uiState.collectAsState()
    val session by authViewModel.currentSession.collectAsState()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showAnnouncementsDialog by rememberSaveable { mutableStateOf(false) }
    var showNotificationsDialog by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(initialOpenNotifications, session?.userId) {
        if (initialOpenNotifications && session != null) {
            showNotificationsDialog = true
            onNotificationsIntentConsumed()
        }
    }
    val unreadNotificationCount by container.notificationService.unreadCount.collectAsState(initial = 0)
    val isDark = ThemeManager.isDarkMode.value
    val activeLang = com.example.core.localization.LanguageManager.currentLanguage.value
    val preferredLang = com.example.core.localization.LanguageManager.preferredLanguage.value
    val isEnglishOverride = com.example.core.localization.LanguageManager.isEnglishOverride.value
    val appStrings = com.example.core.localization.LanguageManager.strings(activeLang)
    val updateState by container.updateManager.state.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        container.updateManager.checkForUpdates(manual = false)
    }

    if (session == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            selectedTabIndex = 0
            launch {
                try {
                    container.userRepository.pushLocalUsersToCloud()
                    container.userRepository.refreshUsersFromCloud()
                } catch (e: Exception) {
                    // Offline fallback
                }
            }
            launch {
                try {
                    container.gideonitesCloudRepository.observeFellowshipAccounts().collect { accounts ->
                        container.userRepository.syncCloudAccountsToLocalDb(accounts)
                    }
                } catch (e: Exception) {
                    // Offline fallback
                }
            }
        }
        AuthScreen(
            uiState = authUiState,
            onLogin = { email, pass -> authViewModel.login(email, pass) },
            onRegister = { name, email, phone, pass, role, dept, prefLang, photoUri ->
                authViewModel.register(name, email, phone, pass, role, dept, prefLang, photoUri)
            },
            onToggleMode = { authViewModel.toggleAuthMode() },
            onQuickLogin = { email, pass -> authViewModel.quickLoginDemo(email, pass) },
            onGoogleAuthSuccess = { uid, name, email, prefLang ->
                authViewModel.onGoogleAuthSuccess(uid, name, email, prefLang)
            },
            onGoogleAuthError = { errorMsg ->
                authViewModel.onGoogleAuthError(errorMsg)
            }
        )
    } else {
        val rawSession = session!!
        val currentSession = if (
            rawSession.userRole == com.example.data.model.UserRole.ADMINISTRATOR ||
            rawSession.userId == com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_ID
        ) {
            rawSession.copy(userName = com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_NAME)
        } else {
            rawSession
        }
        val syncScope = androidx.compose.runtime.rememberCoroutineScope()

        // Request POST_NOTIFICATIONS permission at runtime on Android 13+ (API 33+)
        val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                syncScope.launch {
                    container.notificationService.deliverUnreadNotificationsToDeviceForUser(currentSession.userId)
                }
            }
        }

        androidx.compose.runtime.LaunchedEffect(currentSession.userId) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!hasPerm) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // Deliver any unread notifications to the Android OS status bar for the active user on this device
        androidx.compose.runtime.LaunchedEffect(currentSession.userId) {
            container.notificationService.allNotifications.collect {
                container.notificationService.deliverUnreadNotificationsToDeviceForUser(currentSession.userId)
            }
        }

        androidx.compose.runtime.LaunchedEffect(currentSession.userId, currentSession.token) {
            selectedTabIndex = 0
        }

        androidx.compose.runtime.LaunchedEffect(currentSession.userId, currentSession.preferredLanguage) {
            if (currentSession.preferredLanguage != com.example.core.localization.AppLanguage.ENGLISH ||
                com.example.core.localization.LanguageManager.preferredLanguage.value == com.example.core.localization.AppLanguage.ENGLISH
            ) {
                com.example.core.localization.LanguageManager.setPreferredLanguage(currentSession.preferredLanguage)
            }
        }

        // Automatically purge expired past-date events and expired forum posts, and ensure background sync is scheduled
        androidx.compose.runtime.LaunchedEffect(currentSession.userId) {
            com.example.core.notification.FellowshipBackgroundSyncWorker.schedulePeriodicSync(context)
            try {
                container.eventRepository.purgeExpiredEvents()
            } catch (_: Exception) {
            }
            try {
                container.communityRepository.purgeExpiredPosts()
            } catch (e: Exception) {
                // Ignore if already clean
            }
        }

        val liveBannerNotification by container.notificationService.liveBannerNotification.collectAsState()
        androidx.compose.runtime.LaunchedEffect(liveBannerNotification?.id) {
            if (liveBannerNotification != null) {
                kotlinx.coroutines.delay(6500L)
                container.notificationService.dismissLiveBanner()
            }
        }

        // Cross-Device & Cross-Platform Real-Time Cloud Sync for ALL authenticated sessions (Email/Phone + Password & Google Sign-In)
        androidx.compose.runtime.LaunchedEffect(currentSession.userId) {
            container.gideonitesCloudRepository.activeSessionUserId = currentSession.userId

            // Push any existing local records & notifications from this device to Firestore so other devices receive them immediately
            launch {
                try {
                    container.userRepository.pushLocalUsersToCloud()
                    container.eventRepository.pushLocalEventsToCloud()
                    container.announcementRepository.pushLocalAnnouncementsToCloud()
                    container.communityRepository.pushLocalPostsToCloud()
                    container.multiplayerGameRepository.pushLocalGameSessionsToCloud()
                    container.spiritualResourceRepository.pushLocalResourcesToCloud()
                    container.notificationService.pushLocalNotificationsToCloud()
                } catch (e: Exception) {
                    // Graceful offline fallback
                }
            }

            // 0. Real-Time Cross-Device Notifications Stream (/fellowship_notifications)
            launch {
                while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive != false) {
                    try {
                        container.gideonitesCloudRepository.observeFellowshipNotifications().collect { cloudNotifs ->
                            cloudNotifs.forEach { cn ->
                                if (cn.id.isNotBlank() && cn.title.isNotBlank()) {
                                    val resolvedType = runCatching {
                                        com.example.data.local.entity.NotificationType.valueOf(cn.type)
                                    }.getOrDefault(com.example.data.local.entity.NotificationType.ANNOUNCEMENT)
                                    val tsMillis = cn.timestampMillis.takeIf { it > 0L }
                                        ?: cn.createdAt?.toDate()?.time
                                        ?: System.currentTimeMillis()
                                    container.notificationService.syncRemoteNotificationIfNew(
                                        id = cn.id,
                                        title = cn.title,
                                        message = cn.message,
                                        type = resolvedType,
                                        authorName = cn.authorName.ifBlank { "Youth Leadership" },
                                        authorRole = cn.authorRole.ifBlank { "Executive" },
                                        isUrgent = cn.isUrgent,
                                        timestamp = tsMillis,
                                        senderDeviceId = cn.senderDeviceId,
                                        recipientUserId = cn.recipientUserId,
                                        activeUserId = currentSession.userId
                                    )
                                }
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        kotlinx.coroutines.delay(3000L)
                    }
                }
            }

            // 1. Sync Fellowship User Accounts, Roles & Executive Approvals across all devices
            launch {
                while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive != false) {
                    try {
                        container.gideonitesCloudRepository.observeFellowshipAccounts().collect { accounts ->
                            container.userRepository.syncCloudAccountsToLocalDb(accounts)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        kotlinx.coroutines.delay(3000L)
                    }
                }
            }

            // 2. Sync & Notify for Youth Programmes across all devices
            launch {
                while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive != false) {
                    try {
                        container.gideonitesCloudRepository.observeYouthProgrammes().collect { programmes ->
                            container.eventRepository.syncCloudProgrammesToLocalDb(programmes)
                            val nowMillis = System.currentTimeMillis()
                            programmes.forEach { prog ->
                                val resolvedDate = prog.startDateMillis.takeIf { it > 0L } ?: nowMillis
                                if (!com.example.data.repository.EventRepositoryImpl.isEventDatePassed(resolvedDate, nowMillis)) {
                                    val rawId = prog.id.removePrefix("prog_")
                                    val tsMillis = prog.createdAt?.toDate()?.time ?: nowMillis
                                    container.notificationService.syncRemoteNotificationIfNew(
                                        id = "notif_event_$rawId",
                                        title = "📅 Upcoming Youth Programme: ${prog.titleEn}",
                                        message = "${prog.venue} • Minister: ${prog.speaker?.takeIf { it.isNotBlank() } ?: "Youth Alive Ministry"}. ${prog.descriptionEn}",
                                        type = com.example.data.local.entity.NotificationType.EVENT,
                                        authorName = prog.contactPerson?.takeIf { it.isNotBlank() } ?: "Youth Leadership",
                                        authorRole = "Executive",
                                        timestamp = tsMillis,
                                        activeUserId = currentSession.userId
                                    )
                                }
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        kotlinx.coroutines.delay(3000L)
                    }
                }
            }

            // 3. Sync & Notify for Community Forum Posts (with 3/7-day auto-deletion) & Announcements across all devices
            launch {
                while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive != false) {
                    try {
                        container.gideonitesCloudRepository.observeCommunityPosts().collect { posts ->
                            container.communityRepository.syncCloudPostsToLocalDb(
                                cloudPosts = posts,
                                announcementDao = container.database.announcementDao()
                            )
                            val now = System.currentTimeMillis()
                            posts.filter { !it.isHidden }.forEach { post ->
                                val createdAtMillis = post.createdAt?.toDate()?.time ?: now
                                val isAnnouncement = post.category.startsWith("ANNOUNCEMENT", ignoreCase = true)
                                val isUrgent = post.category.equals("ANNOUNCEMENT_URGENT", ignoreCase = true)
                                val retentionMillis = com.example.data.local.entity.CommunityPostEntity.retentionDurationForCategory(post.category)
                                // Only notify for non-expired forum posts (< 3 days for Work/Skills, < 7 days for others) or announcements
                                if (isAnnouncement || (now - createdAtMillis < retentionMillis)) {
                                    container.notificationService.syncRemoteNotificationIfNew(
                                        id = if (post.id.startsWith("ann_")) "notif_${post.id}" else "notif_post_${post.id}",
                                        title = when {
                                            isUrgent -> "🚨 URGENT: ${post.title}"
                                            isAnnouncement -> "📢 ${post.title}"
                                            else -> "💬 New Forum Post (${post.category}): ${post.title}"
                                        },
                                        message = post.content,
                                        type = if (isAnnouncement) {
                                            com.example.data.local.entity.NotificationType.ANNOUNCEMENT
                                        } else {
                                            com.example.data.local.entity.NotificationType.COMMUNITY_POST
                                        },
                                        authorName = post.authorName,
                                        authorRole = post.authorRole,
                                        isUrgent = isUrgent,
                                        timestamp = createdAtMillis,
                                        activeUserId = currentSession.userId
                                    )
                                }
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        kotlinx.coroutines.delay(3000L)
                    }
                }
            }

            // 4. Sync & Notify for Live Multiplayer Games & Quiz Sessions across all devices
            launch {
                while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive != false) {
                    try {
                        container.gideonitesCloudRepository.observeCloudQuizSessions().collect { quizSessions ->
                            container.multiplayerGameRepository.syncCloudQuizSessionsToLocalDb(
                                cloudSessions = quizSessions,
                                gameDao = container.database.gameDao()
                            )
                            quizSessions.forEach { cs ->
                                if (!cs.status.equals("DRAFT", ignoreCase = true) &&
                                    !cs.status.equals("CANCELLED", ignoreCase = true) &&
                                    !cs.status.equals("COMPLETED", ignoreCase = true)
                                ) {
                                    val rawQuizId = cs.id.removePrefix("quiz_")
                                    val tsMillis = cs.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                                    container.notificationService.syncRemoteNotificationIfNew(
                                        id = "notif_game_$rawQuizId",
                                        title = "🎮 Live Fellowship Game: ${cs.titleEn}",
                                        message = "Hosted by ${cs.hostName} • ${cs.totalQuestions} rounds.",
                                        type = com.example.data.local.entity.NotificationType.GAME_TIME,
                                        authorName = cs.hostName.ifBlank { "Youth Leadership" },
                                        authorRole = "Executive",
                                        isUrgent = true,
                                        timestamp = tsMillis,
                                        activeUserId = currentSession.userId
                                    )
                                }
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        kotlinx.coroutines.delay(3000L)
                    }
                }
            }

            // 4b. Sync Solo Scripture Quizzes & Global Leaderboard Scores across all devices
            launch {
                while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive != false) {
                    try {
                        container.gideonitesCloudRepository.observeGameQuizzes().collect { cloudQuizzes ->
                            container.gamesRepository.syncCloudQuizzesToLocalDb(cloudQuizzes)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        kotlinx.coroutines.delay(3000L)
                    }
                }
            }

            launch {
                while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive != false) {
                    try {
                        container.gideonitesCloudRepository.observeGameScores().collect { cloudScores ->
                            container.gamesRepository.syncCloudScoresToLocalDb(cloudScores)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        kotlinx.coroutines.delay(3000L)
                    }
                }
            }

            // 5. Sync Executive Inquiries & Pastoral Responses across all devices
            launch {
                while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive != false) {
                    try {
                        container.gideonitesCloudRepository.observeExecutiveInquiries().collect { inquiries ->
                            container.communityRepository.syncCloudInquiriesToLocalDb(inquiries)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        kotlinx.coroutines.delay(3000L)
                    }
                }
            }

            // 6. Sync & Notify for Daily Scriptures across ALL sessions (Email/Phone & Google Sign-In)
            launch {
                while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive != false) {
                    try {
                        container.gideonitesCloudRepository.observeDailyScriptures().collect { scriptures ->
                            container.spiritualResourceRepository.syncCloudScripturesToLocalDb(scriptures)
                            container.bibleRepository.syncCloudBibleRecordsToLocalDb(scriptures)
                            scriptures.forEach { s ->
                                if (s.id.isNotBlank()) {
                                    val rawId = s.id.removePrefix("scripture_")
                                    val tsMillis = s.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                                    val prompt = s.prayerPromptEn.trim()
                                    val isGeneralResource = prompt.startsWith("{") && !prompt.contains("\"SCRIPTURE_OF_THE_DAY\"")
                                    if (!isGeneralResource) {
                                        container.notificationService.syncRemoteNotificationIfNew(
                                            id = "notif_scripture_$rawId",
                                            title = "📖 Scripture for the Day: ${s.referenceEn}",
                                            message = "\"${s.verseTextEn}\" — Posted by ${s.authorName.ifBlank { "Youth Leadership" }} for The Gideonites - The Youth Alive Fellowship.",
                                            type = com.example.data.local.entity.NotificationType.ANNOUNCEMENT,
                                            authorName = s.authorName.ifBlank { "Youth Leadership" },
                                            authorRole = "Youth Leadership",
                                            timestamp = tsMillis,
                                            activeUserId = currentSession.userId
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        kotlinx.coroutines.delay(3000L)
                    }
                }
            }

            val firebaseUser = try {
                com.google.firebase.Firebase.auth.currentUser
            } catch (e: Exception) {
                null
            }
            if (firebaseUser != null) {
                launch {
                    try {
                        container.gideonitesCloudRepository.observeCurrentUserProfile().collect { cloudProfile ->
                            if (cloudProfile != null) {
                                val cloudLang = com.example.core.localization.AppLanguage.fromCodeOrName(cloudProfile.preferredLanguage)
                                if (cloudLang != com.example.core.localization.LanguageManager.preferredLanguage.value) {
                                    com.example.core.localization.LanguageManager.setPreferredLanguage(cloudLang)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Graceful offline fallback to Room
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(3.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_church_logo),
                                        contentDescription = "Youth Alive Insignia",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "THE GIDEONITES - YOUTH ALIVE",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp,
                                        color = Color.White
                                    ),
                                    maxLines = 1
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (currentSession.userRole == com.example.data.model.UserRole.ADMINISTRATOR) {
                                            "Admin"
                                        } else {
                                            "${currentSession.userRole.displayName} • ${currentSession.userName}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.95f),
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Official Announcements Board Button
                        IconButton(
                            onClick = { showAnnouncementsDialog = true },
                            modifier = Modifier.testTag("btn_announcements_board")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Official Announcements",
                                tint = Color.White
                            )
                        }
                        // Alerts / Announcements Notification Bell with Unread Badge
                        IconButton(
                            onClick = { showNotificationsDialog = true },
                            modifier = Modifier.testTag("btn_alerts_toggle")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationCount > 0) {
                                        Badge(
                                            containerColor = FlameGoldAccent,
                                            contentColor = GlobeBlack
                                        ) {
                                            Text("$unreadNotificationCount", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications & Alerts ($unreadNotificationCount unread)",
                                    tint = Color.White
                                )
                            }
                        }
                        // 1-Tap English Override Toggle (switches between Signup Preferred Language and English & syncs to Cloud)
                        if (preferredLang != com.example.core.localization.AppLanguage.ENGLISH) {
                            Surface(
                                onClick = {
                                    com.example.core.localization.LanguageManager.toggleEnglishOverride()
                                    val newOverride = com.example.core.localization.LanguageManager.isEnglishOverride.value
                                    syncScope.launch {
                                        try {
                                            if (com.google.firebase.Firebase.auth.currentUser != null) {
                                                container.gideonitesCloudRepository.updateLanguagePreference(
                                                    preferredLanguage = preferredLang.code,
                                                    englishOverride = newOverride
                                                )
                                            }
                                        } catch (e: Exception) {
                                            // Local toggle remains active
                                        }
                                    }
                                },
                                color = if (isEnglishOverride) FlameGoldAccent else FlameRedPrimary,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("btn_english_language_toggle")
                            ) {
                                Text(
                                    text = if (isEnglishOverride) "EN ⇄ ${preferredLang.shortBadge}" else "${preferredLang.shortBadge} ⇄ EN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isEnglishOverride) GlobeBlack else Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        } else {
                            // When default language is English, allow cycling/toggling to preview native language + English toggle
                            Surface(
                                onClick = {
                                    com.example.core.localization.LanguageManager.setPreferredLanguage(
                                        com.example.core.localization.AppLanguage.YORUBA
                                    )
                                },
                                color = Color.White.copy(alpha = 0.16f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("btn_english_language_toggle")
                            ) {
                                Text(
                                    text = "EN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                        // Light/Dark Mode Switch button in top bar
                        IconButton(
                            onClick = { ThemeManager.toggle() },
                            modifier = Modifier.testTag("btn_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = Color.White
                            )
                        }
                        RoleBadge(
                            role = currentSession.userRole,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GlobeBlack,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        AppNavTab.values().forEachIndexed { index, tab ->
                            val isSelected = selectedTabIndex == index
                            val localizedLabel = when (tab) {
                                AppNavTab.PROGRAMMES -> appStrings.navProgrammes
                                AppNavTab.COMMUNITY -> appStrings.navCommunity
                                AppNavTab.GAMES -> appStrings.navGames
                                AppNavTab.SPIRITUAL -> appStrings.navSpiritual
                                AppNavTab.PROFILE -> appStrings.navProfile
                            }
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTabIndex = index },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = localizedLabel,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = localizedLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FlameRedPrimary,
                                    selectedTextColor = FlameRedPrimary,
                                    indicatorColor = FlameRedPrimary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // Tab 0: Home / Programmes Dashboard (FR-005 to FR-009)
                        val dashboardViewModel: DashboardViewModel = viewModel(
                            factory = DashboardViewModel.provideFactory(
                                container.eventRepository,
                                container.notificationService,
                                container.spiritualResourceRepository,
                                container.userRepository
                            )
                        )
                        ProgrammeDashboardScreen(
                            viewModel = dashboardViewModel,
                            session = currentSession
                        )
                    }
                    1 -> {
                        // Tab 1: Community Hub (FR-013 to FR-019)
                        CommunityHubScreen(container = container, session = currentSession)
                    }
                    2 -> {
                        // Tab 2: Youth Engagement Games Session (FR-023, FR-024)
                        val engagementViewModel: EngagementViewModel = viewModel(
                            key = currentSession.userId,
                            factory = EngagementViewModel.provideFactory(
                                container.gamesRepository,
                                container.weeklyReportRepository,
                                currentSession,
                                container.notificationService,
                                container.multiplayerGameRepository,
                                container.bibleRepository
                            )
                        )
                        GamesAndReportsScreen(
                            viewModel = engagementViewModel,
                            session = currentSession
                        )
                    }
                    3 -> {
                        // Tab 3: Spiritual Growth & Word Library (FR-020)
                        SpiritualHubScreen(container = container, session = currentSession)
                    }
                    4 -> {
                        // Tab 4: Profile, Giving & Executive Console (FR-002, FR-021, FR-022, FR-025, FR-026)
                        ProfileHubScreen(container = container, session = currentSession)
                    }
                }

                // Live In-App Cross-Device Notification Banner
                androidx.compose.animation.AnimatedVisibility(
                    visible = liveBannerNotification != null,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) +
                        androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) +
                        androidx.compose.animation.fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    liveBannerNotification?.let { banner ->
                        Card(
                            onClick = {
                                container.notificationService.dismissLiveBanner()
                                showNotificationsDialog = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = GlobeBlack
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (banner.isUrgent) FlameRedPrimary else FlameGoldAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("live_cross_device_notification_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (banner.isUrgent) FlameRedPrimary else FlameGoldAccent,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = "New Fellowship Alert",
                                            tint = if (banner.isUrgent) Color.White else GlobeBlack,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = banner.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = banner.message,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.88f)
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        container.notificationService.dismissLiveBanner()
                                        showNotificationsDialog = true
                                    }
                                ) {
                                    Text(
                                        text = "VIEW",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = FlameGoldAccent
                                        )
                                    )
                                }
                                IconButton(
                                    onClick = { container.notificationService.dismissLiveBanner() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss notification banner",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showNotificationsDialog) {
            NotificationCenterDialog(
                notificationService = container.notificationService,
                onDismiss = { showNotificationsDialog = false }
            )
        }

        if (showAnnouncementsDialog) {
            Dialog(
                onDismissRequest = { showAnnouncementsDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = { Text("Fellowship Announcements", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { showAnnouncementsDialog = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = FlameRedPrimary,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White
                            )
                        )
                        val announcementsViewModel: AnnouncementsViewModel = viewModel(
                            key = currentSession.userRole.name + "_modal",
                            factory = AnnouncementsViewModel.provideFactory(
                                container.announcementRepository,
                                currentSession.userRole,
                                container.notificationService
                            )
                        )
                        AnnouncementsScreen(
                            viewModel = announcementsViewModel,
                            session = currentSession
                        )
                    }
                }
            }
        }
    }

    val activeUpdate = updateState as? com.example.core.update.UpdateCheckState.UpdateAvailable
    if (activeUpdate != null) {
        com.example.ui.components.AppUpdateDialog(
            release = activeUpdate.release,
            onDownloadClick = {
                container.updateManager.openDownloadInBrowser(
                    context = context,
                    url = activeUpdate.release.apkDownloadUrl ?: activeUpdate.release.htmlUrl
                )
                container.updateManager.dismissUpdateDialog(activeUpdate.release.tagName, skipVersion = false)
            },
            onViewReleasePageClick = {
                container.updateManager.openDownloadInBrowser(
                    context = context,
                    url = activeUpdate.release.htmlUrl
                )
            },
            onLaterClick = {
                container.updateManager.dismissUpdateDialog(activeUpdate.release.tagName, skipVersion = false)
            },
            onSkipVersionClick = {
                container.updateManager.dismissUpdateDialog(activeUpdate.release.tagName, skipVersion = true)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityHubScreen(
    container: com.example.core.di.AppContainer,
    session: SessionEntity
) {
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    val appStrings = com.example.core.localization.LanguageManager.strings()

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = FlameRedPrimary
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text(appStrings.tabForum, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("subtab_forum")
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text(appStrings.tabUnits, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("subtab_departments")
            )
        }

        when (subTab) {
            0 -> {
                val communityViewModel: CommunityViewModel = viewModel(
                    factory = CommunityViewModel.provideFactory(container.communityRepository)
                )
                CommunityScreen(viewModel = communityViewModel, session = session)
            }
            1 -> {
                val departmentViewModel: DepartmentViewModel = viewModel(
                    factory = DepartmentViewModel.provideFactory(
                        container.departmentRepository,
                        container.attendanceRepository,
                        container.userRepository
                    )
                )
                val engagementViewModel: EngagementViewModel = viewModel(
                    key = "${session.userId}_reports",
                    factory = EngagementViewModel.provideFactory(
                        container.gamesRepository,
                        container.weeklyReportRepository,
                        session,
                        container.notificationService,
                        container.multiplayerGameRepository,
                        container.bibleRepository
                    )
                )
                DepartmentScreen(
                    viewModel = departmentViewModel,
                    session = session,
                    engagementViewModel = engagementViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpiritualHubScreen(
    container: com.example.core.di.AppContainer,
    session: SessionEntity
) {
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    val appStrings = com.example.core.localization.LanguageManager.strings()

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = FlameRedPrimary
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text(appStrings.tabDevotionals, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("subtab_spiritual_library")
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text(appStrings.tabBibleExplorer, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("subtab_bible_explorer")
            )
        }

        when (subTab) {
            0 -> {
                val spiritualViewModel: SpiritualViewModel = viewModel(
                    factory = SpiritualViewModel.provideFactory(
                        container.spiritualResourceRepository,
                        container.notificationService
                    )
                )
                SpiritualScreen(viewModel = spiritualViewModel, session = session)
            }
            1 -> {
                com.example.ui.spiritual.BibleReaderScreen(
                    bibleRepository = container.bibleRepository,
                    session = session
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileHubScreen(
    container: com.example.core.di.AppContainer,
    session: SessionEntity
) {
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    val isLeadership = session.userRole.isLeadership()
    val appStrings = com.example.core.localization.LanguageManager.strings()

    androidx.compose.runtime.LaunchedEffect(isLeadership) {
        if (!isLeadership && subTab > 1) {
            subTab = 0
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = FlameRedPrimary
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text(appStrings.tabMyProfile, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("subtab_profile")
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text(appStrings.tabGiving, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("subtab_finance")
            )
            if (isLeadership) {
                Tab(
                    selected = subTab == 2,
                    onClick = { subTab = 2 },
                    text = { Text(appStrings.tabCommandCentre, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("subtab_admin_hub")
                )
            }
        }

        when (subTab) {
            0 -> {
                val profileViewModel: ProfileViewModel = viewModel(
                    key = session.userId,
                    factory = ProfileViewModel.provideFactory(
                        container.userRepository,
                        session
                    )
                )
                ProfileScreen(
                    viewModel = profileViewModel,
                    session = session,
                    updateManager = container.updateManager
                )
            }
            1 -> {
                val financeViewModel: FinanceViewModel = viewModel(
                    key = session.userId,
                    factory = FinanceViewModel.provideFactory(
                        container.financeRepository,
                        session
                    )
                )
                FinanceScreen(viewModel = financeViewModel, session = session)
            }
            2 -> {
                val adminDashboardViewModel: AdminDashboardViewModel = viewModel(
                    factory = AdminDashboardViewModel.provideFactory(
                        container.userRepository,
                        container.eventRepository,
                        container.announcementRepository,
                        container.financeRepository,
                        container.attendanceRepository,
                        container.communityRepository,
                        container.spiritualResourceRepository,
                        container.database.auditLogDao(),
                        container.bibleRepository,
                        container.notificationService
                    )
                )
                AdminDashboardScreen(
                    viewModel = adminDashboardViewModel,
                    session = session,
                    bibleRepository = container.bibleRepository
                )
            }
        }
    }
}
