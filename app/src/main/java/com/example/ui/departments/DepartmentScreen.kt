package com.example.ui.departments

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.DepartmentType
import com.example.data.model.UserRole
import com.example.ui.components.RoleBadge
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentScreen(
    viewModel: DepartmentViewModel,
    session: SessionEntity,
    engagementViewModel: com.example.ui.engagement.EngagementViewModel? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val deptMembers by viewModel.deptMembers.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val appStrings = com.example.core.localization.LanguageManager.strings()

    var showRecordAttendanceDialog by remember { mutableStateOf(false) }
    var showAssignMemberDialog by remember { mutableStateOf(false) }
    var selectedDeptForDetail by remember { mutableStateOf<DepartmentEntity?>(null) }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.activeTab == 2 && session.userRole.isLeadership()) {
                FloatingActionButton(
                    onClick = { showRecordAttendanceDialog = true },
                    containerColor = FlameRedPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_record_attendance")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Record Attendance")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(
                selectedTabIndex = uiState.activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = FlameRedPrimary
            ) {
                Tab(
                    selected = uiState.activeTab == 0,
                    onClick = { viewModel.setTab(0) },
                    text = { Text("Units", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1) },
                    modifier = Modifier.testTag("tab_departments")
                )
                Tab(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.setTab(1) },
                    text = { Text(appStrings.tabUnitReports, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1) },
                    modifier = Modifier.testTag("subtab_unit_reports")
                )
                Tab(
                    selected = uiState.activeTab == 2,
                    onClick = { viewModel.setTab(2) },
                    text = { Text("Attendance", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1) },
                    modifier = Modifier.testTag("tab_attendance")
                )
                Tab(
                    selected = uiState.activeTab == 3,
                    onClick = { viewModel.setTab(3) },
                    text = { Text("QR Check-In", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1) },
                    modifier = Modifier.testTag("tab_checkin")
                )
            }

            when (uiState.activeTab) {
                0 -> {
                    DepartmentsWorkspaceTab(
                        departments = uiState.departments,
                        session = session,
                        onSelectDepartment = { dept ->
                            selectedDeptForDetail = dept
                            viewModel.selectDepartment(dept)
                        }
                    )
                }
                1 -> {
                    if (engagementViewModel != null) {
                        com.example.ui.engagement.WeeklyReportsScreen(
                            viewModel = engagementViewModel,
                            session = session
                        )
                    }
                }
                2 -> {
                    AttendanceTrackerTab(
                        records = uiState.attendanceRecords,
                        totalAcrossServices = uiState.totalAttendeesAcrossServices,
                        session = session,
                        onRecordClick = { showRecordAttendanceDialog = true },
                        isLeadership = session.userRole.isLeadership()
                    )
                }
                3 -> {
                    QrCheckInTab(
                        generatedCode = uiState.generatedCheckInCode,
                        isLeadership = session.userRole.isLeadership(),
                        onRefreshCode = { viewModel.refreshCheckInCode() },
                        onCheckIn = { code ->
                            viewModel.performSelfCheckIn(
                                code = code,
                                userId = session.userId,
                                userName = session.userName,
                                serviceTitle = "Sunday Youth Service"
                            )
                        },
                        checkInRecord = uiState.checkInSuccessRecord
                    )
                }
            }
        }
    }

    // Detail Dialog for Department Workspace
    selectedDeptForDetail?.let { dept ->
        DepartmentWorkspaceDialog(
            department = dept,
            members = deptMembers,
            isLeadership = session.userRole.isLeadership(),
            onDismiss = { selectedDeptForDetail = null },
            onAssignMemberClick = { showAssignMemberDialog = true },
            onOpenUnitReports = {
                selectedDeptForDetail = null
                viewModel.setTab(1)
            }
        )
    }

    // Record Attendance Dialog
    if (showRecordAttendanceDialog) {
        RecordAttendanceDialog(
            departments = uiState.departments,
            onDismiss = { showRecordAttendanceDialog = false },
            onSubmit = { title, type, dept, m, f, ft, notes ->
                viewModel.recordManualAttendance(
                    title = title,
                    type = type,
                    department = dept,
                    males = m,
                    females = f,
                    firstTimers = ft,
                    notes = notes,
                    recorderId = session.userId,
                    recorderName = session.userName
                )
                showRecordAttendanceDialog = false
            }
        )
    }

    // Assign Member Dialog
    if (showAssignMemberDialog && selectedDeptForDetail != null) {
        AssignMemberDialog(
            department = selectedDeptForDetail!!,
            allUsers = allUsers,
            onDismiss = { showAssignMemberDialog = false },
            onAssign = { userId ->
                viewModel.assignMember(userId, selectedDeptForDetail!!.type, session.userId)
                showAssignMemberDialog = false
            }
        )
    }
}

@Composable
private fun DepartmentsWorkspaceTab(
    departments: List<DepartmentEntity>,
    session: SessionEntity,
    onSelectDepartment: (DepartmentEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = GlobeBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "THE GIDEONITES - THE YOUTH ALIVE FELLOWSHIP UNITS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = FlameGoldAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Specialized workspaces for youth departments, ministries, and cell fellowships. Tap any unit to view rosters and meeting times.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f))
                    )
                }
            }
        }

        // Tailored orientation banner for GUEST
        if (session.userRole == UserRole.GUEST) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_guest_units")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = FlameRedPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Discover Your Ministry Calling",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "At The Gideonites - The Youth Alive Fellowship, every youth has a place to shine. Browse our specialized units below (Choir, Media, Ushering, Drama, Prayer, Sports) to discover where you can serve as a Kingdom Giant!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        items(departments, key = { it.id }) { dept ->
            DepartmentCard(department = dept, onClick = { onSelectDepartment(dept) })
        }
    }
}

@Composable
private fun DepartmentCard(
    department: DepartmentEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("dept_card_${department.type.name}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(FlameRedPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = FlameRedPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = department.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Leader: ${department.leaderName}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "${department.meetingSchedule} • ${department.meetingVenue}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${department.memberCount} members",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = FlameRedPrimary)
                )
            }
        }
    }
}

@Composable
private fun DepartmentWorkspaceDialog(
    department: DepartmentEntity,
    members: List<UserEntity>,
    isLeadership: Boolean,
    onDismiss: () -> Unit,
    onAssignMemberClick: () -> Unit,
    onOpenUnitReports: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = department.name, fontWeight = FontWeight.Bold)
                Text(
                    text = department.type.displayName,
                    style = MaterialTheme.typography.labelMedium.copy(color = FlameRedPrimary)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = department.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ShieldSilver, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Head: ${department.leaderName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = ShieldSilver, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = department.meetingSchedule, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = ShieldSilver, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = department.meetingVenue, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "UNIT ROSTER (${members.size})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = GlobeBlack)
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (members.isEmpty()) {
                    Text(
                        text = "No members currently tagged in this unit.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                } else {
                    members.take(5).forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "• ${member.fullName}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(text = member.role.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                if (onOpenUnitReports != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onOpenUnitReports,
                        modifier = Modifier.fillMaxWidth().testTag("btn_open_unit_reports")
                    ) {
                        Text("Open Unit Reports")
                    }
                }
                if (isLeadership) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAssignMemberClick,
                        colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("btn_assign_member")
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assign Member to Unit")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun AssignMemberDialog(
    department: DepartmentEntity,
    allUsers: List<UserEntity>,
    onDismiss: () -> Unit,
    onAssign: (String) -> Unit
) {
    var selectedUserId by remember { mutableStateOf(allUsers.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign to ${department.name}") },
        text = {
            Column {
                Text(
                    text = "Select a registered youth member to assign to this department roster:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(allUsers) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUserId = user.id }
                                .padding(8.dp)
                                .background(
                                    if (selectedUserId == user.id) FlameRedPrimary.copy(alpha = 0.1f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = user.fullName,
                                fontWeight = if (selectedUserId == user.id) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            Text(text = user.department.displayName, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedUserId.isNotBlank()) onAssign(selectedUserId) },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text("Confirm Assignment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AttendanceTrackerTab(
    records: List<AttendanceRecordEntity>,
    totalAcrossServices: Int,
    session: SessionEntity,
    onRecordClick: () -> Unit,
    isLeadership: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Welcoming card for GUEST
        if (session.userRole == UserRole.GUEST) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Guest Welcome & Service Check-in",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "First time worshipping with us? Our Ushers & Hospitality team are thrilled to receive you! You can check in at the entrance or generate your QR Check-In Pass on the next tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = GlobeBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL HEADCOUNT",
                                style = MaterialTheme.typography.labelMedium.copy(color = ShieldSilver)
                            )
                            Text(
                                text = "$totalAcrossServices Attendees",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        if (isLeadership) {
                            Button(
                                onClick = onRecordClick,
                                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
                            ) {
                                Text("Record")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "RECENT SERVICES & MEETINGS",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        items(records, key = { it.id }) { record ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = record.meetingTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FlameRedPrimary)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${record.totalAttendees} Total",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${dateFormat.format(Date(record.dateMillis))} • ${record.departmentOrGroup}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("Males: ${record.maleCount}")
                        StatPill("Females: ${record.femaleCount}")
                        StatPill("1st Timers: ${record.firstTimersCount}")
                    }
                    if (record.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Note: ${record.notes}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun QrCheckInTab(
    generatedCode: String,
    isLeadership: Boolean,
    onRefreshCode: () -> Unit,
    onCheckIn: (String) -> Unit,
    checkInRecord: AttendanceRecordEntity?
) {
    var inputCode by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = GlobeBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MEETING CHECK-IN (QR & PASS)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = FlameGoldAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Simulated QR code visual representation
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Code Checkin",
                                tint = GlobeBlack,
                                modifier = Modifier.size(90.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = generatedCode,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FlameRedPrimary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Active Meeting Pass: $generatedCode",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    if (isLeadership) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onRefreshCode,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate New Pass Code")
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
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Member Self Check-In",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Attending today's meeting or service? Enter the active pass code announced or displayed on the sanctuary screen:",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        label = { Text("Enter Pass Code (e.g. $generatedCode)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_checkin_code")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onCheckIn(inputCode) },
                        colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("btn_submit_checkin")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Check-In")
                    }
                }
            }
        }

        checkInRecord?.let { record ->
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CHECKED IN SUCCESSFULLY!",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            )
                            Text(
                                text = "${record.meetingTitle} • Recorded at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(record.dateMillis))}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF14532D))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordAttendanceDialog(
    departments: List<DepartmentEntity>,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Int, Int, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("Sunday Youth Service") }
    var type by remember { mutableStateOf("SUNDAY_SERVICE") }
    var department by remember { mutableStateOf("General") }
    var males by remember { mutableIntStateOf(25) }
    var females by remember { mutableIntStateOf(30) }
    var firstTimers by remember { mutableIntStateOf(3) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Service Attendance", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Service / Meeting Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = males.toString(),
                        onValueChange = { males = it.toIntOrNull() ?: 0 },
                        label = { Text("Males") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = females.toString(),
                        onValueChange = { females = it.toIntOrNull() ?: 0 },
                        label = { Text("Females") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = firstTimers.toString(),
                    onValueChange = { firstTimers = it.toIntOrNull() ?: 0 },
                    label = { Text("First Timers") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observations / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Total Headcount: ${males + females + firstTimers}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = FlameRedPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, type, department, males, females, firstTimers, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
