package com.example.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.notification.FellowshipNotificationService
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.NotificationType
import com.example.ui.components.ModernEmptyState
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.OutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterDialog(
    notificationService: FellowshipNotificationService,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val notifications by notificationService.allNotifications.collectAsState(initial = emptyList())
    val unreadCount by notificationService.unreadCount.collectAsState(initial = 0)
    var selectedFilter by remember { mutableStateOf<NotificationType?>(null) }
    var hasOsPermission by remember { mutableStateOf(notificationService.hasNotificationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasOsPermission = granted || notificationService.hasNotificationPermission()
    }

    val filteredList = remember(notifications, selectedFilter) {
        if (selectedFilter == null) notifications
        else notifications.filter { it.type == selectedFilter }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TopAppBar
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Notifications & Alerts",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (unreadCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "$unreadCount new",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = FlameRedPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "The Gideonites - The Youth Alive Fellowship Updates",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        if (unreadCount > 0) {
                            TextButton(
                                onClick = { notificationService.markAllAsRead() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                            ) {
                                Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Read All", fontSize = 12.sp)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FlameRedPrimary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )

                // Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All (${notifications.size})") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FlameRedPrimary, selectedLabelColor = Color.White)
                    )
                    NotificationType.values().forEach { type ->
                        val count = notifications.count { it.type == type }
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = { selectedFilter = if (selectedFilter == type) null else type },
                            label = { Text("${type.displayName} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FlameRedPrimary, selectedLabelColor = Color.White)
                        )
                    }
                }

                // Android 13+ Device Status-Bar Permission Banner
                if (!hasOsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FlameGoldAccent.copy(alpha = 0.16f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("banner_enable_notifications")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Device alerts are currently disabled",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = GlobeBlack
                                )
                                Text(
                                    text = "Enable notifications to get announcements, programmes & game alerts even when the app is closed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlobeBlack.copy(alpha = 0.78f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Allow", fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        runCatching { context.startActivity(intent) }.onFailure {
                                            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            runCatching { context.startActivity(fallback) }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text("Settings", fontSize = 11.sp, color = FlameRedPrimary)
                                }
                            }
                        }
                    }
                }

                // Notification Feed
                if (filteredList.isEmpty()) {
                    ModernEmptyState(
                        icon = Icons.Default.NotificationsActive,
                        title = "You're All Caught Up!",
                        subtitle = "No notifications yet. You will be alerted on your device whenever a forum post, announcement, or programme is scheduled on the platform.",
                        accentColor = FlameRedPrimary
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            NotificationCardItem(
                                notification = item,
                                onMarkAsRead = { notificationService.markAsRead(item.id) },
                                onDelete = { notificationService.deleteNotification(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    notification: NotificationEntity,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit
) {
    val (typeColor, typeIcon) = when (notification.type) {
        NotificationType.ANNOUNCEMENT -> Color(0xFF1368CE) to Icons.Default.Campaign
        NotificationType.EVENT -> FlameGoldAccent to Icons.Default.Event
        NotificationType.COMMUNITY_POST -> Color(0xFF8B5CF6) to Icons.Default.Person
        NotificationType.GAME_TIME -> FlameRedPrimary to Icons.Default.SportsEsports
        NotificationType.REPORT_REVIEW -> Color(0xFF10B981) to Icons.Default.Verified
        NotificationType.GENERAL -> ShieldSilver to Icons.Default.Notifications
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!notification.isRead) 2.dp else 0.5.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMarkAsRead() }
            .testTag("notification_card_${notification.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Type Badge, Urgent Badge, Unread Dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = typeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = notification.type.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                color = typeColor
                            )
                        }
                    }

                    if (notification.isUrgent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = FlameRedPrimary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "URGENT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(FlameRedPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = ShieldSilver, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = GlobeBlack
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Message Content
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Author & Time Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ShieldSilver, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${notification.authorRole} • ${notification.authorName}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = GlobeBlack.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = ShieldSilver
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}
