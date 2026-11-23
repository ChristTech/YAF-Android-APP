package com.example.ui.admin

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.SpiritualResourceEntity
import com.example.data.model.UserRole
import com.example.data.repository.BibleRepository
import com.example.data.repository.UploadedBibleEdition
import com.example.ui.components.RoleBadge
import com.example.ui.engagement.BibleScripturePickerDialog
import com.example.ui.engagement.UploadBibleDatabaseDialog
import com.example.ui.engagement.UploadedBibleDocumentReaderDialog
import com.example.ui.spiritual.PublishResourceDialog
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    session: SessionEntity,
    bibleRepository: BibleRepository? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSmsDialog by remember { mutableStateOf(false) }
    var showClearDatabaseDialog by remember { mutableStateOf(false) }
    var publishDialogCategory by remember { mutableStateOf<String?>(null) }
    var showUploadBibleDialog by remember { mutableStateOf(false) }
    var showBibleReferenceDialog by remember { mutableStateOf(false) }
    var activeDocumentEdition by remember { mutableStateOf<UploadedBibleEdition?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = uiState.activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = FlameRedPrimary
        ) {
            Tab(
                selected = uiState.activeTab == 0,
                onClick = { viewModel.setTab(0) },
                text = { Text("KPIs", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_executive_kpis")
            )
            Tab(
                selected = uiState.activeTab == 1,
                onClick = { viewModel.setTab(1) },
                text = { Text("Search", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_universal_search")
            )
            Tab(
                selected = uiState.activeTab == 2,
                onClick = { viewModel.setTab(2) },
                text = { Text(if (session.userRole == UserRole.ADMINISTRATOR) "Audit & Sync" else "Ministry Reports", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_audit_sync")
            )
        }

        uiState.statusMessage?.let { msg ->
            Surface(
                color = Color(0xFF16A34A).copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.clearStatusMessage() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearStatusMessage() }) {
                        Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        when (uiState.activeTab) {
            0 -> ExecutiveOverviewTab(
                uiState = uiState,
                session = session,
                hasBibleRepository = bibleRepository != null,
                onApproveExecutive = { userId -> viewModel.approveExecutive(userId, session.userId) },
                onDeclineExecutive = { userId -> viewModel.declineExecutive(userId, session.userId) },
                onOpenPublishResource = { category -> publishDialogCategory = category },
                onDeleteSpiritualResource = { res -> viewModel.deleteSpiritualResource(res, session.userId, session.userRole) },
                onOpenUploadBible = { showUploadBibleDialog = true },
                onOpenBibleReference = { showBibleReferenceDialog = true },
                onOpenUploadedBibleEdition = { edition -> activeDocumentEdition = edition },
                onRequestClearDatabase = { showClearDatabaseDialog = true }
            )
            1 -> UniversalSearchTab(
                query = uiState.searchQuery,
                results = uiState.searchResult,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )
            2 -> AuditAndSyncTab(
                uiState = uiState,
                isAdmin = session.userRole == UserRole.ADMINISTRATOR,
                onSyncChms = { viewModel.triggerChmsSync() },
                onOpenSmsDialog = { showSmsDialog = true },
                onExportFinances = { viewModel.exportFinancesCsv() },
                onExportAttendance = { viewModel.exportAttendanceCsv() },
                onExportMembers = { viewModel.exportMembersCsv() },
                onRequestClearDatabase = { showClearDatabaseDialog = true }
            )
        }
    }

    publishDialogCategory?.let { category ->
        PublishResourceDialog(
            initialCategory = category,
            defaultAuthorName = session.userName,
            onDismiss = { publishDialogCategory = null },
            onSubmit = { title, cat, author, scripture, summary, content, duration, audioDraft, _ ->
                viewModel.publishSpiritualResource(
                    title = title,
                    category = cat,
                    author = author,
                    scriptureReference = scripture,
                    summary = summary,
                    contentBody = content,
                    durationMinutes = duration,
                    adminUserId = session.userId,
                    adminUserRole = session.userRole,
                    audioDraft = audioDraft
                )
                publishDialogCategory = null
            }
        )
    }

    if (showUploadBibleDialog && bibleRepository != null) {
        UploadBibleDatabaseDialog(
            bibleRepository = bibleRepository,
            onDismiss = { showUploadBibleDialog = false },
            onImportSuccess = {
                showUploadBibleDialog = false
            }
        )
    }

    if (showBibleReferenceDialog && bibleRepository != null) {
        BibleScripturePickerDialog(
            bibleRepository = bibleRepository,
            initialTargetContext = "Executive Bible Reference & PDF/EPUB Library",
            initialTabIndex = if (uiState.uploadedBibles.isNotEmpty()) 1 else 0,
            onDismiss = { showBibleReferenceDialog = false },
            onScriptureSelected = {
                showBibleReferenceDialog = false
            }
        )
    }

    activeDocumentEdition?.let { edition ->
        UploadedBibleDocumentReaderDialog(
            edition = edition,
            onDismiss = { activeDocumentEdition = null },
            onSelectScripture = null
        )
    }

    if (showSmsDialog) {
        SmsBroadcastDialog(
            onDismiss = { showSmsDialog = false },
            onSend = { msg ->
                viewModel.sendSmsBroadcast(msg)
                showSmsDialog = false
            }
        )
    }

    if (uiState.isExportDialogVisible && uiState.exportedDataContent != null) {
        DataExportPreviewDialog(
            title = uiState.exportedDataTitle ?: "Exported Data",
            content = uiState.exportedDataContent!!,
            onDismiss = { viewModel.dismissExportDialog() }
        )
    }

    if (showClearDatabaseDialog) {
        AlertDialog(
            onDismissRequest = { showClearDatabaseDialog = false },
            title = {
                Text(
                    text = "Clear Database for Real User Testing?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently wipe all test users, forum posts, comments, inquiries, events, registrations, financial records, attendance logs, notifications, and game sessions from both this device and the online Cloud Firestore database.\n\nOnly the official Admin account ('Admin' • admin@youthalive.org) will be preserved.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDatabaseDialog = false
                        viewModel.clearDatabasePreserveAdmin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_clear_database")
                ) {
                    Text("Yes, Clear Database (Keep Admin Only)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDatabaseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExecutiveOverviewTab(
    uiState: AdminDashboardUiState,
    session: SessionEntity,
    hasBibleRepository: Boolean,
    onApproveExecutive: (String) -> Unit,
    onDeclineExecutive: (String) -> Unit,
    onOpenPublishResource: (String) -> Unit,
    onDeleteSpiritualResource: (SpiritualResourceEntity) -> Unit,
    onOpenUploadBible: () -> Unit,
    onOpenBibleReference: () -> Unit,
    onOpenUploadedBibleEdition: (UploadedBibleEdition) -> Unit,
    onRequestClearDatabase: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "NG"))
    currencyFormat.maximumFractionDigits = 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = GlobeBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_church_logo),
                                        contentDescription = "Church Logo",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (session.userRole == UserRole.ADMINISTRATOR) "ADMIN COMMAND CENTRE" else "EXECUTIVE COMMAND CENTRE",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = FlameGoldAccent,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (session.userRole == UserRole.ADMINISTRATOR)
                                        "System Oversight, Governance & Security Audit"
                                    else
                                        "Ministry Coordination & Operations Oversight",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                                )
                            }
                        }
                        RoleBadge(role = session.userRole)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "MEMBERS",
                    value = "${uiState.totalMembers}",
                    subtext = "${uiState.pendingApprovalsCount} pending approval",
                    icon = Icons.Default.Group,
                    color = FlameRedPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "ATTENDANCE",
                    value = "${uiState.totalAttendanceAcrossServices}",
                    subtext = "Across all services",
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "FINANCES",
                    value = currencyFormat.format(uiState.totalGivingAmount),
                    subtext = "Tithes, Pledges, Dues",
                    icon = Icons.Default.Paid,
                    color = FlameGoldAccent,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "PROGRAMMES",
                    value = "${uiState.totalEventsCount}",
                    subtext = "Active events listed",
                    icon = Icons.Default.Event,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Spiritual Publishing & PDF/EPUB Bible Backend Console
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, FlameGoldAccent.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_ministry_publishing_console")
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
                                tint = FlameRedPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "SERMONS, DEVOTIONALS & BIBLE BACKEND",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Upload Audio/Text Sermons, Daily Devotionals, and PDF/EPUB Bibles for Executive game & scripture reference",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 1: Upload Sermon & Upload Daily Devotional
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenPublishResource("SERMON") },
                            colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_btn_upload_sermon")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Sermon", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Button(
                            onClick = { onOpenPublishResource("DEVOTIONAL") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FlameGoldAccent,
                                contentColor = GlobeBlack
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_btn_upload_devotional")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Daily Devotional", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    if (hasBibleRepository) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Row 2: Upload PDF/EPUB Bible & Open Bible Reference
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenUploadBible,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_btn_upload_bible_pdf_epub")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = FlameRedPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload PDF / EPUB Bible", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = onOpenBibleReference,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_btn_open_bible_reference")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = FlameGoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Bible Reference", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    // Show Uploaded PDF/EPUB Bibles in Backend
                    if (uiState.uploadedBibles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "BACKEND PDF / EPUB BIBLES (${uiState.uploadedBibles.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FlameGoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        uiState.uploadedBibles.take(4).forEach { edition ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { onOpenUploadedBibleEdition(edition) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${edition.fileFormat} • ${edition.title}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${edition.sections.size} sections • ${edition.extractedVersesCount} indexed verses • By ${edition.uploadedBy}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Open",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = FlameRedPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Show Recent Sermons & Daily Devotionals
                    if (uiState.spiritualResources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        val sermonsCount = uiState.spiritualResources.count { it.category.equals("SERMON", ignoreCase = true) }
                        val devotionalsCount = uiState.spiritualResources.count { it.category.equals("DEVOTIONAL", ignoreCase = true) }
                        Text(
                            text = "PUBLISHED SERMONS ($sermonsCount) & DEVOTIONALS ($devotionalsCount)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FlameRedPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        uiState.spiritualResources.take(4).forEach { res ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = buildString {
                                            append("[${res.category}] ")
                                            if (res.hasAudio) append("🎧 ")
                                            append(res.title)
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${res.scriptureReference} • ${res.speakerOrAuthor}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSpiritualResource(res) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete ${res.title}",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Admin-Only Executive Approval Queue
        if (session.userRole == UserRole.ADMINISTRATOR) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (uiState.pendingExecutives.isNotEmpty()) FlameGoldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_executive_approvals_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = FlameRedPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "EXECUTIVE SIGN-UP APPROVALS (${uiState.pendingExecutives.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (uiState.pendingExecutives.isEmpty()) {
                            Text(
                                text = "No pending Youth Executive sign-up requests.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.pendingExecutives.forEach { pending ->
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    com.example.ui.components.UserAvatar(
                                        photoUri = pending.profilePhotoUri,
                                        fullName = pending.fullName,
                                        sizeDp = 42
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pending.fullName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${pending.role.displayName} • ${pending.department.displayName}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = FlameRedPrimary, fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "${pending.email} • ${pending.phone}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { onDeclineExecutive(pending.id) },
                                            modifier = Modifier.testTag("admin_decline_exco_${pending.id}")
                                        ) {
                                            Text("Decline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                        Button(
                                            onClick = { onApproveExecutive(pending.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                            modifier = Modifier.testTag("admin_approve_exco_${pending.id}")
                                        ) {
                                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GOVERNANCE CHECKLIST & HEALTH",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ChecklistRow("RBAC Security & Role Verification", "Enforced")
                    ChecklistRow("Financial Ledger Auditability", "Active")
                    ChecklistRow("Offline-First Room Sync", "Operational")
                    ChecklistRow("Member Data Protection", "Compliant")
                    ChecklistRow("Multilingual Resource Packs", "Available (EN, YO, HA, IG)")
                }
            }
        }

        if (session.userRole == UserRole.ADMINISTRATOR) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_clear_database_admin")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RESET DATABASE FOR REAL USER TESTING",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Wipes all test accounts, posts, comments, events, finances, and game sessions from both the local database and Cloud Firestore while preserving only the official Admin account ('Admin' • admin@youthalive.org).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestClearDatabase,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_open_clear_database")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Database (Preserve Admin Only)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditAndSyncTab(
    uiState: AdminDashboardUiState,
    isAdmin: Boolean = false,
    onSyncChms: () -> Unit,
    onOpenSmsDialog: () -> Unit,
    onExportFinances: () -> Unit,
    onExportAttendance: () -> Unit,
    onExportMembers: () -> Unit,
    onRequestClearDatabase: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Data Export Actions
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = FlameRedPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DATA EXPORT & AUDIT ARCHIVES (CSV)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Generate structured audit-ready CSV exports for Youth Executive council, fellowship secretariat, or financial committee:",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportFinances,
                            modifier = Modifier.weight(1f).testTag("btn_export_finances")
                        ) {
                            Text("Finances", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = onExportAttendance,
                            modifier = Modifier.weight(1f).testTag("btn_export_attendance")
                        ) {
                            Text("Attendance", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = onExportMembers,
                            modifier = Modifier.weight(1f).testTag("btn_export_members")
                        ) {
                            Text("Roster", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // External System Integration Hooks
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GlobeBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "EXTERNAL INTEGRATIONS",
                        style = MaterialTheme.typography.titleSmall.copy(color = FlameGoldAccent, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Connect with external Church Management Systems (ChMS) and SMS broadcast gateways:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSyncChms,
                            colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                            modifier = Modifier.weight(1f).testTag("btn_sync_chms")
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync ChMS", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = onOpenSmsDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = ShieldSilver),
                            modifier = Modifier.weight(1f).testTag("btn_broadcast_sms")
                        ) {
                            Icon(imageVector = Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SMS Blast", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    uiState.lastSyncResult?.let { sync ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "✓ ${sync.message} (${sync.syncedMembersCount} members, ${sync.syncedEventsCount} events)",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4ADE80))
                            )
                        }
                    }
                    uiState.lastSmsResult?.let { sms ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "✓ ${sms.statusMessage}",
                                style = MaterialTheme.typography.labelSmall.copy(color = FlameGoldAccent)
                            )
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                OutlinedButton(
                    onClick = onRequestClearDatabase,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_audit_tab_clear_database")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clear Database for Real User Testing (Keep Admin Only)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Immutable Audit Trail
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SECURITY AUDIT TRAIL",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "${uiState.recentAuditLogs.size} logs",
                    style = MaterialTheme.typography.labelSmall.copy(color = ShieldSilver)
                )
            }
        }

        items(uiState.recentAuditLogs) { log ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.actionType,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = FlameRedPrimary)
                        )
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Target: ${log.targetEntity}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = log.details,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmsBroadcastDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dispatch Emergency SMS Broadcast", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Dispatches SMS to all verified member mobile phone numbers for emergency announcements or service reminders:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("SMS Message Text (Max 160 chars)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${message.length}/160 characters",
                    style = MaterialTheme.typography.labelSmall.copy(color = ShieldSilver)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (message.isNotBlank()) onSend(message) },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text("Dispatch Broadcast")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DataExportPreviewDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Ready to download or copy into church archive spreadsheets:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlobeBlack)
                        .padding(10.dp)
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF4ADE80),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun ChecklistRow(title: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "• $title", style = MaterialTheme.typography.bodySmall)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFDCFCE7))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(text = status, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF166534), fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun UniversalSearchTab(
    query: String,
    results: UniversalSearchResult,
    onQueryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search programmes, alerts, forum, sermons, members...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("input_universal_search"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Type keywords above to search across the entire The Gideonites - The Youth Alive Fellowship platform.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = ShieldSilver),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else if (results.totalCount == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No records found matching \"$query\".",
                    style = MaterialTheme.typography.bodyMedium.copy(color = ShieldSilver)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (results.events.isNotEmpty()) {
                    item { SectionHeader("Programmes & Events (${results.events.size})") }
                    items(results.events) { event ->
                        SearchResultCard(
                            title = event.title,
                            snippet = "${event.venue} • ${event.category.name}",
                            category = "Event",
                            icon = Icons.Default.Event,
                            color = FlameRedPrimary
                        )
                    }
                }

                if (results.announcements.isNotEmpty()) {
                    item { SectionHeader("Announcements (${results.announcements.size})") }
                    items(results.announcements) { ann ->
                        SearchResultCard(
                            title = ann.title,
                            snippet = ann.content,
                            category = "Alert",
                            icon = Icons.Default.Campaign,
                            color = FlameGoldAccent
                        )
                    }
                }

                if (results.spiritualResources.isNotEmpty()) {
                    item { SectionHeader("Spiritual Library (${results.spiritualResources.size})") }
                    items(results.spiritualResources) { res ->
                        SearchResultCard(
                            title = res.title,
                            snippet = "${res.speakerOrAuthor} • ${res.summary}",
                            category = res.category,
                            icon = Icons.Default.MenuBook,
                            color = Color(0xFF16A34A)
                        )
                    }
                }

                if (results.forumPosts.isNotEmpty()) {
                    item { SectionHeader("Forum Discussions (${results.forumPosts.size})") }
                    items(results.forumPosts) { post ->
                        SearchResultCard(
                            title = post.title,
                            snippet = "${post.authorName}: ${post.content}",
                            category = post.departmentOrCategory,
                            icon = Icons.Default.Forum,
                            color = GlobeBlack
                        )
                    }
                }

                if (results.members.isNotEmpty()) {
                    item { SectionHeader("Members & Roster (${results.members.size})") }
                    items(results.members) { user ->
                        SearchResultCard(
                            title = user.fullName,
                            snippet = "${user.role.displayName} • ${user.department.displayName} • ${user.email}",
                            category = "Member",
                            icon = Icons.Default.Person,
                            color = ShieldSilver
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SearchResultCard(
    title: String,
    snippet: String,
    category: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = category, style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.Bold))
            }
        }
    }
}
