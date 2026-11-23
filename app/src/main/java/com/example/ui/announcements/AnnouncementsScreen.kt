package com.example.ui.announcements

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.model.AnnouncementAudience
import com.example.data.model.AnnouncementPriority
import com.example.ui.components.ModernEmptyState
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    viewModel: AnnouncementsViewModel,
    session: SessionEntity,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val filteredList = remember(announcements, uiState.selectedPriority) {
        if (uiState.selectedPriority == null) announcements
        else announcements.filter { it.priority == uiState.selectedPriority }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (session.userRole.canPublishAnnouncements()) {
                FloatingActionButton(
                    onClick = { viewModel.setCreateOpen(true) },
                    containerColor = FlameRedPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_announcement_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Publish Announcement")
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
            // Header Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(FlameRedPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = FlameRedPrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Announcement Portal",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Official church communications & targeted unit alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Priority Filters
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedPriority == null,
                            onClick = { viewModel.filterByPriority(null) },
                            label = { Text("All Broadcasts") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameRedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(AnnouncementPriority.values()) { priority ->
                        FilterChip(
                            selected = uiState.selectedPriority == priority,
                            onClick = { viewModel.filterByPriority(priority) },
                            label = { Text(priority.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (priority == AnnouncementPriority.URGENT) FlameRedPrimary else FlameGoldAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (filteredList.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.Campaign,
                        title = if (uiState.selectedPriority != null) "No ${uiState.selectedPriority?.name} Broadcasts" else "No Announcements Yet",
                        subtitle = if (session.userRole.canPublishAnnouncements())
                            "Keep fellowship members engaged and informed. Tap below or use the '+' button to broadcast an update to all youth."
                        else
                            "Official fellowship broadcasts and ministry alerts from the pastorate and youth executives will appear here.",
                        actionText = if (session.userRole.canPublishAnnouncements()) "+ Publish First Announcement" else null,
                        onAction = if (session.userRole.canPublishAnnouncements()) { { viewModel.setCreateOpen(true) } } else null
                    )
                }
            } else {
                items(filteredList, key = { it.id }) { announcement ->
                    AnnouncementCard(
                        announcement = announcement,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (uiState.isCreateOpen) {
        CreateAnnouncementDialog(
            onDismiss = { viewModel.setCreateOpen(false) },
            onPublish = { title, content, priority, audience ->
                viewModel.publishAnnouncement(title, content, priority, audience, session)
            }
        )
    }
}

@Composable
fun AnnouncementCard(
    announcement: AnnouncementEntity,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (announcement.priority == AnnouncementPriority.URGENT) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (announcement.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = FlameRedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    PriorityBadge(priority = announcement.priority)
                }

                Text(
                    text = dateFormat.format(Date(announcement.publishedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlobeBlack
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = ShieldSilver,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${announcement.authorName} (${announcement.authorRole})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnouncementDialog(
    onDismiss: () -> Unit,
    onPublish: (
        title: String,
        content: String,
        priority: AnnouncementPriority,
        audience: AnnouncementAudience
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(AnnouncementPriority.NORMAL) }
    var audience by remember { mutableStateOf(AnnouncementAudience.ALL) }

    var priorityDropdownExpanded by remember { mutableStateOf(false) }
    var audienceDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Broadcast Announcement",
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
                    label = { Text("Announcement Headline *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = priorityDropdownExpanded,
                    onExpandedChange = { priorityDropdownExpanded = !priorityDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = priority.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority Level") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = priorityDropdownExpanded,
                        onDismissRequest = { priorityDropdownExpanded = false }
                    ) {
                        AnnouncementPriority.values().forEach { prio ->
                            DropdownMenuItem(
                                text = { Text(prio.name) },
                                onClick = {
                                    priority = prio
                                    priorityDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = audienceDropdownExpanded,
                    onExpandedChange = { audienceDropdownExpanded = !audienceDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = audience.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Audience") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audienceDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = audienceDropdownExpanded,
                        onDismissRequest = { audienceDropdownExpanded = false }
                    ) {
                        AnnouncementAudience.values().forEach { aud ->
                            DropdownMenuItem(
                                text = { Text(aud.name) },
                                onClick = {
                                    audience = aud
                                    audienceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Announcement Body *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(title, content, priority, audience) },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text("Broadcast")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
