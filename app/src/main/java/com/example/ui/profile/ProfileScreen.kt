package com.example.ui.profile

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.DepartmentType
import com.example.data.model.UserRole
import com.example.ui.components.RoleBadge
import com.example.ui.components.UserAvatar
import com.example.ui.components.persistProfilePhotoUri
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    session: SessionEntity,
    modifier: Modifier = Modifier,
    updateManager: com.example.core.update.GitHubUpdateManager? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val pendingUsers by viewModel.pendingUsers.collectAsState()
    val allMembers by viewModel.allMembers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Profile Header Card
            item {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        val isAdminRole = session.userRole == UserRole.ADMINISTRATOR
                        val resolvedDisplayName = when {
                            isAdminRole -> "Admin"
                            !currentUser?.fullName.isNullOrBlank() -> currentUser!!.fullName
                            else -> session.userName
                        }

                        UserAvatar(
                            photoUri = currentUser?.profilePhotoUri,
                            fullName = resolvedDisplayName,
                            sizeDp = 80
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = resolvedDisplayName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = GlobeBlack
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        RoleBadge(role = session.userRole)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { viewModel.setEditProfileOpen(true) }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isAdminRole) "Edit Admin Info" else "Edit Profile")
                            }

                            Button(
                                onClick = { viewModel.logout() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out")
                            }
                        }
                    }
                }
            }

            // Dedicated Membership Action for GUEST User Type
            if (session.userRole == UserRole.GUEST) {
                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_guest_upgrade")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = FlameRedPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Become a Kingdom Giant!",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Step into active youth fellowship & ministry",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Join our fellowship family as a verified Member to join specialized units (Choir, Media, Ushering, Drama), connect with a Cell fellowship, and record your Bible quiz scores on the leaderboards!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.switchActiveRole(UserRole.YOUTH_MEMBER) },
                                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_upgrade_to_member")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upgrade to Full Member", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }



            // System Administration Details (for ADMINISTRATOR) vs Membership Details (for Members/Executives)
            item {
                val isAdminRole = session.userRole == UserRole.ADMINISTRATOR
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAdminRole) Icons.Default.Shield else Icons.Default.Groups,
                                contentDescription = null,
                                tint = FlameRedPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAdminRole) "System Administration Account" else "Fellowship & Ministry Unit Details",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isAdminRole) {
                            ProfileDetailRow("Account Identity", "Official Fellowship System Account")
                            ProfileDetailRow("Access Scope", "Full Platform & Cloud Oversight")
                            ProfileDetailRow("Official Contact Line", currentUser?.phone.orEmpty().ifBlank { "+2348011223344" })
                            ProfileDetailRow(
                                "Secretariat / Office",
                                currentUser?.address
                                    ?.replace("ECWA", "Living Faith Church", ignoreCase = true)
                                    .orEmpty()
                                    .ifBlank { "Living Faith Church Youth Alive Fellowship Secretariat" }
                            )
                        } else {
                            ProfileDetailRow("Phone Number", currentUser?.phone.orEmpty().ifBlank { "Not specified" })
                            ProfileDetailRow("Department / Unit", currentUser?.department?.displayName ?: "General Youth Fellowship")
                            ProfileDetailRow("Residential Address", currentUser?.address.orEmpty().ifBlank { "Not specified" })
                        }
                    }
                }
            }

            // Administrative Executive Approval Console (Strictly for ADMINISTRATOR)
            if (session.userRole.canManageUsers()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = FlameRedPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Executive Approval Console (${pendingUsers.size} Pending)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (pendingUsers.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "All Executive registrations have been reviewed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(pendingUsers, key = { it.id }) { pending ->
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                UserAvatar(
                                    photoUri = pending.profilePhotoUri,
                                    fullName = pending.fullName,
                                    sizeDp = 46
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pending.fullName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${pending.role.displayName} • ${pending.department.displayName}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = FlameRedPrimary
                                    )
                                    Text(
                                        text = "${pending.email} • ${pending.phone}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.declineUser(pending.id) },
                                        modifier = Modifier.testTag("btn_decline_${pending.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Decline",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.approveUser(pending.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier.testTag("btn_approve_${pending.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Display & Theme Settings
            item {
                val isDark = com.example.ui.theme.ThemeManager.isDarkMode.value
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("card_theme_settings")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Appearance Theme",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GlobeBlack
                                )
                                Text(
                                    text = if (isDark) "Dark Mode" else "Light Mode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = isDark,
                                onCheckedChange = { com.example.ui.theme.ThemeManager.setDark(it) },
                                modifier = Modifier.testTag("switch_dark_mode")
                            )
                        }
                    }
                }
            }

            // App Version & GitHub Releases Update Card
            if (updateManager != null) {
                item {
                    val context = LocalContext.current
                    val updateScope = androidx.compose.runtime.rememberCoroutineScope()
                    val updateState by updateManager.state.collectAsState()

                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("card_app_updates")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "App Version & Updates",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GlobeBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Installed: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Source: github.com/${com.example.core.update.AppUpdateChecker.GITHUB_OWNER}/${com.example.core.update.AppUpdateChecker.GITHUB_REPO}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val statusText = when (val st = updateState) {
                                is com.example.core.update.UpdateCheckState.Checking -> "Checking GitHub Releases..."
                                is com.example.core.update.UpdateCheckState.UpToDate ->
                                    st.message ?: "You are on the latest version (v${st.currentVersion})."
                                is com.example.core.update.UpdateCheckState.UpdateAvailable ->
                                    "Update available: v${st.release.latestVersion} (${st.release.releaseTitle})"
                                is com.example.core.update.UpdateCheckState.Error -> st.message
                                else -> null
                            }
                            if (!statusText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (updateState) {
                                            is com.example.core.update.UpdateCheckState.UpdateAvailable -> FlameRedPrimary
                                            is com.example.core.update.UpdateCheckState.Error -> MaterialTheme.colorScheme.error
                                            else -> Color(0xFF16A34A)
                                        }
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        updateScope.launch {
                                            updateManager.checkForUpdates(manual = true)
                                        }
                                    },
                                    enabled = updateState !is com.example.core.update.UpdateCheckState.Checking,
                                    colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                    modifier = Modifier.weight(1f).testTag("btn_check_github_updates")
                                ) {
                                    Text(
                                        text = if (updateState is com.example.core.update.UpdateCheckState.Checking) {
                                            "Checking..."
                                        } else {
                                            "Check for Updates"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        updateManager.openDownloadInBrowser(
                                            context = context,
                                            url = com.example.core.update.AppUpdateChecker.GITHUB_RELEASES_PAGE
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_open_github_releases")
                                ) {
                                    Text("GitHub Releases", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val userToEdit = currentUser
    if (uiState.isEditProfileOpen && userToEdit != null) {
        EditProfileDialog(
            user = userToEdit,
            onDismiss = { viewModel.setEditProfileOpen(false) },
            onSave = { name, phone, address, dept, photoUri ->
                viewModel.updateProfile(name, phone, address, dept, photoUri)
            }
        )
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSave: (
        fullName: String,
        phone: String,
        address: String,
        department: DepartmentType,
        profilePhotoUri: String?
    ) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(user.fullName) }
    var phone by remember { mutableStateOf(user.phone) }
    var address by remember { mutableStateOf(user.address) }
    var department by remember { mutableStateOf(user.department) }
    var profilePhotoUri by remember { mutableStateOf(user.profilePhotoUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            profilePhotoUri = persistProfilePhotoUri(context, uri)
        }
    }

    val isAdminRole = user.role == UserRole.ADMINISTRATOR
    var deptDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isAdminRole) "Edit Administrator Account" else "Edit Member Profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(
                    photoUri = profilePhotoUri,
                    fullName = fullName.ifBlank { user.fullName },
                    sizeDp = 72
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (profilePhotoUri == null) (if (isAdminRole) "Attach Emblem" else "Attach Picture") else (if (isAdminRole) "Change Emblem" else "Change Picture"))
                    }
                    if (profilePhotoUri != null) {
                        TextButton(onClick = { profilePhotoUri = null }) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(if (isAdminRole) "Administrator Title / Display Name" else "Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (isAdminRole) "Official Contact Line" else "Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (!isAdminRole) {
                    ExposedDropdownMenuBox(
                        expanded = deptDropdownExpanded,
                        onExpandedChange = { deptDropdownExpanded = !deptDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = department.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Department / Ministry Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = deptDropdownExpanded,
                            onDismissRequest = { deptDropdownExpanded = false }
                        ) {
                            DepartmentType.values().forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d.displayName) },
                                    onClick = {
                                        department = d
                                        deptDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (isAdminRole) "Secretariat / Office Location" else "Residential Address") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(fullName, phone, address, department, profilePhotoUri)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
