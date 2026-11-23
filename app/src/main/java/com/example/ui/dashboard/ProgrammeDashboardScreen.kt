package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.model.EventCategory
import com.example.data.model.UserRole
import com.example.ui.components.ChurchEmblemHeader
import com.example.ui.components.ModernEmptyState
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver
import com.example.ui.theme.ThemeManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammeDashboardScreen(
    viewModel: DashboardViewModel,
    session: SessionEntity,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val allRegistrations by viewModel.allRegistrations.collectAsState()
    val dailyScripture by viewModel.dailyScripture.collectAsState()
    val userRegistrations by viewModel.observeUserRegistrations(session.userId).collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeLang = com.example.core.localization.LanguageManager.currentLanguage.value
    val appStrings = com.example.core.localization.LanguageManager.strings(activeLang)

    LaunchedEffect(session.userId, allEvents.size) {
        viewModel.purgeExpiredEvents()
        viewModel.processScheduledEmailReminders(session)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val filteredEvents = remember(allEvents, uiState.selectedCategory) {
        if (uiState.selectedCategory == null) {
            allEvents
        } else {
            allEvents.filter { it.category == uiState.selectedCategory }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (session.userRole.canManageEvents()) {
                FloatingActionButton(
                    onClick = { viewModel.setCreateEventOpen(true) },
                    containerColor = FlameRedPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_event_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Publish Event")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero Banner
            // Modern Fellowship Workspace Hero
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (ThemeManager.isDarkMode.value) Color(0xFF131B2E) else GlobeBlack
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = FlameGoldAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(FlameGoldAccent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "THE GIDEONITES • KINGDOM GIANTS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = FlameGoldAccent,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp
                                        )
                                    )
                                }
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Youth Platform",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_church_logo),
                                        contentDescription = "Church Logo",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "The Gideonites - The Youth Alive Fellowship",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FlameGoldAccent,
                                        letterSpacing = 0.2.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Grace & Peace, ${session.userName.ifBlank { "Kingdom Giant" }}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = (-0.2).sp
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The Gideonites - The Youth Alive Fellowship platform is ready. Explore daily scriptures, upcoming youth summits, retreats, and spiritual encounters.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.82f),
                                lineHeight = 18.sp
                            )
                        )

                        if (session.userRole.canManageEvents()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.setCreateEventOpen(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_quick_schedule_programme")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(appStrings.postProgrammeAction, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.setPostScriptureOpen(true) },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, FlameGoldAccent),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FlameGoldAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_post_scripture_of_day")
                                ) {
                                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(appStrings.postScriptureAction, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Scripture for the Day Card (Auto-Localized & Admin Editable)
            item {
                val activeRef = dailyScripture?.scriptureReference?.ifBlank { "Judges 6:12" } ?: "Judges 6:12"
                val fallbackVerse = dailyScripture?.summary?.ifBlank {
                    "The LORD is with thee, thou mighty man of valour."
                } ?: "The LORD is with thee, thou mighty man of valour."
                val localizedVerseText = com.example.core.localization.LanguageManager.getMemoryVerse(
                    reference = activeRef,
                    fallbackText = fallbackVerse,
                    lang = activeLang
                )
                val postedBy = dailyScripture?.speakerOrAuthor ?: "Youth Pastor / Admin"

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FlameGoldAccent.copy(alpha = 0.65f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("card_scripture_of_the_day")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = FlameRedPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = appStrings.scriptureOfTheDayBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FlameRedPrimary,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                            }

                            Surface(
                                color = FlameGoldAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${activeRef} • ${activeLang.shortBadge}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GlobeBlack
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "\"$localizedVerseText\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                lineHeight = 21.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "The Gideonites Daily Charge • By $postedBy",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldSilver
                            )

                            if (session.userRole.canManageEvents()) {
                                TextButton(
                                    onClick = { viewModel.setPostScriptureOpen(true) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("btn_update_daily_scripture")
                                ) {
                                    Text(
                                        text = "Post / Update",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = FlameRedPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Tailored Welcome Banner for GUEST User Type
            if (session.userRole == UserRole.GUEST) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("banner_guest_programmes")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(FlameRedPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Welcome, Honored Guest! 👋",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "You are warmly invited to attend our youth gatherings, praise nights & spiritual summits. Feel free to RSVP for any programme!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Upcoming Youth Programmes Header + Category Filter Chips
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = appStrings.upcomingProgrammesTitle,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = GlobeBlack,
                            letterSpacing = 0.8.sp
                        )
                    )
                    if (session.userRole.canManageEvents()) {
                        TextButton(
                            onClick = { viewModel.setCreateEventOpen(true) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = FlameRedPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = appStrings.postProgrammeAction,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = FlameRedPrimary)
                            )
                        }
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            shape = RoundedCornerShape(20.dp),
                            label = { Text("All Programmes", fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameRedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(EventCategory.values()) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            shape = RoundedCornerShape(20.dp),
                            label = { Text(category.displayName, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameRedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Events List
            if (filteredEvents.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.CalendarMonth,
                        title = if (uiState.selectedCategory != null) "No ${uiState.selectedCategory?.displayName} Programmes" else "No Scheduled Programmes",
                        subtitle = if (session.userRole.canManageEvents())
                            "Your fellowship calendar is fresh and ready. Tap below to schedule conferences, vigils, workshops, or rallies."
                        else
                            "Upcoming youth programmes, retreats, and spiritual summits will appear here once announced by fellowship leadership.",
                        actionText = if (session.userRole.canManageEvents()) "+ Schedule First Programme" else null,
                        onAction = if (session.userRole.canManageEvents()) { { viewModel.setCreateEventOpen(true) } } else null
                    )
                }
            } else {
                items(filteredEvents, key = { it.id }) { event ->
                    val eventRegistrations = allRegistrations.filter { it.eventId == event.id }
                    val currentRegistration = userRegistrations.firstOrNull { it.eventId == event.id }
                        ?: eventRegistrations.firstOrNull { it.userId == session.userId }
                    EventCard(
                        event = event,
                        registration = currentRegistration,
                        eventRegistrations = eventRegistrations,
                        onViewDetails = { viewModel.selectEvent(event) },
                        onRegister = { viewModel.registerForEvent(event, session) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Detail Dialog
    uiState.selectedEvent?.let { selectedEv ->
        val event = allEvents.firstOrNull { it.id == selectedEv.id } ?: selectedEv
        val eventRegistrations = allRegistrations.filter { it.eventId == event.id }
        val currentRegistration = userRegistrations.firstOrNull { it.eventId == event.id }
            ?: eventRegistrations.firstOrNull { it.userId == session.userId }
        EventDetailDialog(
            event = event,
            registration = currentRegistration,
            eventRegistrations = eventRegistrations,
            onDismiss = { viewModel.selectEvent(null) },
            onRegister = {
                viewModel.registerForEvent(event, session)
                viewModel.selectEvent(null)
            },
            onSendPassToEmail = { reg ->
                val passRef = "GID-${event.id.replace(Regex("[^a-zA-Z0-9]"), "").take(4).uppercase()}-${reg.userId.replace(Regex("[^a-zA-Z0-9]"), "").takeLast(4).uppercase()}"
                com.example.core.notification.FellowshipEmailService.launchEmailConfirmationIntent(
                    context = context,
                    event = event,
                    userName = reg.userName.ifBlank { session.userName },
                    userEmail = reg.userEmail,
                    passReference = passRef
                )
            },
            onAddCalendarReminder = { reg ->
                val passRef = "GID-${event.id.replace(Regex("[^a-zA-Z0-9]"), "").take(4).uppercase()}-${reg.userId.replace(Regex("[^a-zA-Z0-9]"), "").takeLast(4).uppercase()}"
                com.example.core.notification.FellowshipEmailService.launchCalendarReminderIntent(
                    context = context,
                    event = event,
                    userName = reg.userName.ifBlank { session.userName },
                    userEmail = reg.userEmail,
                    passReference = passRef
                )
            }
        )
    }

    // Official Registration Pass & Email/Calendar Confirmation Dialog
    uiState.confirmedPassEvent?.let { passEvent ->
        RegistrationPassConfirmationDialog(
            event = passEvent,
            userName = session.userName,
            userEmail = uiState.confirmedPassEmail,
            passReference = uiState.confirmedPassReference,
            backgroundEmailDispatched = uiState.backgroundEmailDispatched,
            onDismiss = { viewModel.dismissConfirmedPass() },
            onSendToEmailClient = {
                com.example.core.notification.FellowshipEmailService.launchEmailConfirmationIntent(
                    context = context,
                    event = passEvent,
                    userName = session.userName,
                    userEmail = uiState.confirmedPassEmail,
                    passReference = uiState.confirmedPassReference
                )
            },
            onAddToCalendar = {
                com.example.core.notification.FellowshipEmailService.launchCalendarReminderIntent(
                    context = context,
                    event = passEvent,
                    userName = session.userName,
                    userEmail = uiState.confirmedPassEmail,
                    passReference = uiState.confirmedPassReference
                )
            }
        )
    }

    // Create Event Dialog (Executives & Admin)
    if (uiState.isCreateEventOpen) {
        CreateEventDialog(
            onDismiss = { viewModel.setCreateEventOpen(false) },
            onCreate = { title, desc, days, venue, speaker, cat, limit ->
                viewModel.createEvent(title, desc, days, venue, speaker, cat, limit, session)
            }
        )
    }

    // Post Scripture for the Day Dialog (Executives & Admin)
    if (uiState.isPostScriptureOpen) {
        PostScriptureOfTheDayDialog(
            onDismiss = { viewModel.setPostScriptureOpen(false) },
            onPost = { ref, verse, charge ->
                viewModel.postScriptureOfTheDay(ref, verse, charge, session)
            }
        )
    }
}

@Composable
fun PostScriptureOfTheDayDialog(
    onDismiss: () -> Unit,
    onPost: (reference: String, verseText: String, propheticCharge: String) -> Unit
) {
    val presetScriptures = listOf(
        "Judges 6:12" to "And the angel of the LORD appeared unto him, and said unto him, The LORD is with thee, thou mighty man of valour.",
        "Daniel 11:32b" to "...but the people that do know their God shall be strong, and do exploits.",
        "1 Timothy 4:12" to "Let no man despise thy youth; but be thou an example of the believers, in word, in conversation, in charity, in spirit, in faith, in purity.",
        "Joshua 1:9" to "Have not I commanded thee? Be strong and of a good courage; be not afraid, neither be thou dismayed: for the LORD thy God is with thee whithersoever thou goest.",
        "Isaiah 40:31" to "But they that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint."
    )

    var reference by remember { mutableStateOf(presetScriptures.first().first) }
    var verseText by remember { mutableStateOf(presetScriptures.first().second) }
    var propheticCharge by remember { mutableStateOf("Rise as mighty Gideonites today and do Kingdom exploits!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Post Scripture for the Day",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Quick Select Multilingual Youth Scripture (EN/YO/HA/IG):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ShieldSilver)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetScriptures) { (ref, text) ->
                        FilterChip(
                            selected = reference == ref,
                            onClick = {
                                reference = ref
                                verseText = text
                            },
                            label = { Text(ref, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameRedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Scripture Reference * (e.g. Judges 6:12)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_daily_scripture_ref")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = verseText,
                    onValueChange = { verseText = it },
                    label = { Text("Scripture Verse Text *") },
                    modifier = Modifier.fillMaxWidth().testTag("input_daily_scripture_text"),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = propheticCharge,
                    onValueChange = { propheticCharge = it },
                    label = { Text("Gideonites Youth Charge / Meditation") },
                    modifier = Modifier.fillMaxWidth().testTag("input_daily_scripture_charge"),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPost(reference, verseText, propheticCharge) },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                modifier = Modifier.testTag("btn_confirm_post_scripture")
            ) {
                Text("Post Scripture")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EventCard(
    event: EventEntity,
    onViewDetails: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
    registration: com.example.data.local.entity.EventRegistrationEntity? = null,
    eventRegistrations: List<com.example.data.local.entity.EventRegistrationEntity> = emptyList()
) {
    val dateMonthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val dateDayFormat = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    val shortDateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    val eventDate = Date(event.dateTimeMillis)
    val fiveDayDate = Date(event.dateTimeMillis - (5L * 24 * 60 * 60 * 1000L))
    val oneDayDate = Date(event.dateTimeMillis - (1L * 24 * 60 * 60 * 1000L))
    val totalRegistered = maxOf(event.registeredCount, eventRegistrations.size)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.clickable { onViewDetails() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Calendar Date Badge
                Surface(
                    color = FlameRedPrimary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = dateMonthFormat.format(eventDate).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = dateDayFormat.format(eventDate),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and Meta
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = event.category.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlobeBlack
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ShieldSilver,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = event.venue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (event.speaker.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = ShieldSilver,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Minister: ${event.speaker}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5-Day & 1-Day Reminder Status Banner
            if (registration != null) {
                Surface(
                    color = Color(0xFF16A34A).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Registered ✓ • Event reminders enabled",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF15803D)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Capacity & Registration Bar + Synced Attendees
            Column {
                val limit = event.participantLimit
                val progress = if (limit > 0) (totalRegistered.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Registered Attendance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (limit > 0) "$totalRegistered / $limit registered" else "$totalRegistered registered",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (limit > 0 && progress >= 1f) FlameRedPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (limit > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (progress >= 1f) FlameRedPrimary else FlameGoldAccent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                if (eventRegistrations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = FlameRedPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val attendeeSummary = eventRegistrations.joinToString(", ", limit = 3) {
                            it.userName.ifBlank { "Member" }
                        }
                        val extraCount = (eventRegistrations.size - 3).coerceAtLeast(0)
                        Text(
                            text = if (extraCount > 0) {
                                "Registered: $attendeeSummary +$extraCount more"
                            } else {
                                "Registered: $attendeeSummary"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons (No Cancellation Allowed Once Registered)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details")
                }

                if (event.isRegistrationEnabled) {
                    if (registration != null) {
                        Surface(
                            color = Color(0xFF16A34A).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable { onViewDetails() }
                                .testTag("badge_registered_${event.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Registered ✓",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onRegister,
                            colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_register_${event.id}")
                        ) {
                            Text("Register (RSVP)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventDetailDialog(
    event: EventEntity,
    onDismiss: () -> Unit,
    onRegister: () -> Unit,
    registration: com.example.data.local.entity.EventRegistrationEntity? = null,
    eventRegistrations: List<com.example.data.local.entity.EventRegistrationEntity> = emptyList(),
    onSendPassToEmail: ((com.example.data.local.entity.EventRegistrationEntity) -> Unit)? = null,
    onAddCalendarReminder: ((com.example.data.local.entity.EventRegistrationEntity) -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM dd, yyyy • h:mm a", Locale.getDefault()) }
    val reminderFormat = remember { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()) }
    val fiveDayMillis = event.dateTimeMillis - (5L * 24 * 60 * 60 * 1000L)
    val oneDayMillis = event.dateTimeMillis - (1L * 24 * 60 * 60 * 1000L)
    val totalRegistered = maxOf(event.registeredCount, eventRegistrations.size)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = event.category.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = FlameRedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateFormat.format(Date(event.dateTimeMillis)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = FlameRedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.venue,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (event.speaker.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = FlameRedPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Speaker/Minister: ${event.speaker}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "About this Programme:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Synced Registered Participants Section
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = FlameRedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Registered Participants ($totalRegistered${if (event.participantLimit > 0) " / ${event.participantLimit}" else ""})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GlobeBlack
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (eventRegistrations.isEmpty()) {
                            Text(
                                text = if (totalRegistered > 0) "$totalRegistered member(s) registered." else "No registrations yet. Be the first to register!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            eventRegistrations.forEachIndexed { idx, reg ->
                                Text(
                                    text = "${idx + 1}. ${reg.userName.ifBlank { "Fellowship Member" }} ✓",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📧 Confirmation & Reminder Schedule",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FlameRedPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• 5 Days Before: ${reminderFormat.format(Date(fiveDayMillis))} (${if (registration?.reminder5DaysSent == true) "Delivered ✓" else if (registration != null) "Scheduled" else "Upon Registration"})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• 1 Day Before: ${reminderFormat.format(Date(oneDayMillis))} (${if (registration?.reminder1DaySent == true) "Delivered ✓" else if (registration != null) "Scheduled" else "Upon Registration"})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (registration != null && registration.userEmail.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Registered Email: ${registration.userEmail}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { onSendPassToEmail?.invoke(registration) },
                                    modifier = Modifier.weight(1f).testTag("btn_email_pass_${event.id}")
                                ) {
                                    Text("Email My Pass", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { onAddCalendarReminder?.invoke(registration) },
                                    modifier = Modifier.weight(1f).testTag("btn_calendar_pass_${event.id}")
                                ) {
                                    Text("Add Calendar Alert", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                if (event.contactPerson.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Contact Coordinator: ${event.contactPerson}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (event.isRegistrationEnabled) {
                if (registration != null) {
                    Surface(
                        color = Color(0xFF16A34A).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A))
                    ) {
                        Text(
                            text = "Registered ✓ (Confirmed)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onRegister,
                        colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
                    ) {
                        Text("Register Now")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun RegistrationPassConfirmationDialog(
    event: EventEntity,
    userName: String,
    userEmail: String,
    passReference: String,
    backgroundEmailDispatched: Boolean,
    onDismiss: () -> Unit,
    onSendToEmailClient: () -> Unit,
    onAddToCalendar: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM dd, yyyy • h:mm a", Locale.getDefault()) }
    val reminderFormat = remember { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()) }
    val fiveDayMillis = event.dateTimeMillis - (5L * 24 * 60 * 60 * 1000L)
    val oneDayMillis = event.dateTimeMillis - (1L * 24 * 60 * 60 * 1000L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Registration Confirmed!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    color = GlobeBlack,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "OFFICIAL PROGRAMME PASS • #$passReference",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FlameGoldAccent,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${dateFormat.format(Date(event.dateTimeMillis))} • ${event.venue}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Attendee: $userName ($userEmail)",
                            style = MaterialTheme.typography.labelSmall.copy(color = FlameGoldAccent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (backgroundEmailDispatched) {
                        "Your registration is synced across all fellowship devices and a confirmation email has been dispatched to $userEmail."
                    } else {
                        "Your registration is synced across all fellowship devices, and 5-day (${reminderFormat.format(Date(fiveDayMillis))}) & 1-day (${reminderFormat.format(Date(oneDayMillis))}) reminders are active on your device. Tap below to send a copy of this pass directly to your $userEmail inbox or Google Calendar:"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onSendToEmailClient,
                    colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("btn_send_pass_to_email")
                ) {
                    Text("Send Confirmation Pass to $userEmail")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onAddToCalendar,
                    modifier = Modifier.fillMaxWidth().testTag("btn_add_pass_to_calendar")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add 5-Day & 1-Day Alerts to Google Calendar")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDialog(
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        desc: String,
        daysInFuture: Long,
        venue: String,
        speaker: String,
        category: EventCategory,
        limit: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var daysInFuture by remember { mutableStateOf("7") }
    var venue by remember { mutableStateOf("") }
    var speaker by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EventCategory.YOUTH_SERVICE) }
    var limit by remember { mutableStateOf("100") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Post Upcoming Youth Programme",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Programme Title *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Location *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = speaker,
                    onValueChange = { speaker = it },
                    label = { Text("Minister / Speaker") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Programme Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        EventCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    category = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = daysInFuture,
                        onValueChange = { daysInFuture = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Days from now") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Capacity limit") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Agenda") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysInFuture.toLongOrNull() ?: 7L
                    val lim = limit.toIntOrNull() ?: 100
                    onCreate(title, description, days, venue, speaker, category, lim)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
