package com.example.ui.engagement

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Stars
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.WeeklyReportEntity
import com.example.data.model.DepartmentType
import com.example.data.model.ReportStatus
import com.example.data.model.UserRole
import com.example.ui.components.ModernEmptyState
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeeklyReportsTabContent(
    reports: List<WeeklyReportEntity>,
    session: SessionEntity,
    selectedDeptFilter: DepartmentType?,
    selectedStatusFilter: ReportStatus?,
    onSetDeptFilter: (DepartmentType?) -> Unit,
    onSetStatusFilter: (ReportStatus?) -> Unit,
    onOpenReview: (WeeklyReportEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredReports = reports.filter { report ->
        val deptMatches = selectedDeptFilter == null || report.departmentOrUnit == selectedDeptFilter
        val statusMatches = selectedStatusFilter == null || report.status == selectedStatusFilter
        deptMatches && statusMatches
    }

    val totalAttendance = reports.sumOf { it.meetingAttendance }
    val totalFirstTimers = reports.sumOf { it.firstTimersCount }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Role-specific Header Banner
        when (session.userRole) {
            UserRole.ADMINISTRATOR -> {
                item {
                    // Admin Oversight KPI Cards
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GlobeBlack),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_reports_kpi_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Executive Oversight & Reports Audit",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Surface(
                                    color = FlameRedPrimary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Admin Mode",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${reports.size}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = FlameGoldAccent
                                        )
                                    )
                                    Text(
                                        text = "Reports Total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$totalAttendance",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF10B981)
                                        )
                                    )
                                    Text(
                                        text = "Total Attended",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$totalFirstTimers",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = FlameRedPrimary
                                        )
                                    )
                                    Text(
                                        text = "1st Timers",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            UserRole.YOUTH_EXECUTIVE -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = FlameGoldAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Unit & Cell Accountability Ledger",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GlobeBlack
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Submit your weekly department/cell records and monitor executive reviews. Tap '+' to create a new submission.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            UserRole.YOUTH_MEMBER -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("member_praise_reports_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Weekly Fellowship Praise Reports & Highlights",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GlobeBlack
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Rejoice in what God is doing through our choir, media, ushering, and cell groups! Weekly administrative reporting is maintained by unit executives.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlobeBlack.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
            UserRole.GUEST -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("guest_reports_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = FlameRedPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Discover Our Ministry Units In Action",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "See how young people serve with excellence across Media, Choir, Ushering, Drama, and Prayer units. Join as a Member to be part of an active team!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Department & Status Filter Chips (for Admin & Executive)
        if (session.userRole.isLeadership()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedDeptFilter == null,
                        onClick = { onSetDeptFilter(null) },
                        label = { Text("All Units") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlameRedPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    DepartmentType.values().forEach { dept ->
                        FilterChip(
                            selected = selectedDeptFilter == dept,
                            onClick = { onSetDeptFilter(if (selectedDeptFilter == dept) null else dept) },
                            label = { Text(dept.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameRedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (session.userRole == UserRole.YOUTH_MEMBER) "Unit Activities & Answered Prayers"
                           else if (session.userRole == UserRole.GUEST) "Ministry Unit Weekly Highlights"
                           else "Submitted Unit Reports",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlobeBlack
                    )
                )
                Text(
                    text = "${filteredReports.size} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (filteredReports.isEmpty()) {
            item {
                ModernEmptyState(
                    icon = Icons.Default.Assessment,
                    title = if (selectedDeptFilter != null) "No Reports for ${selectedDeptFilter.displayName}" else "No Weekly Reports Yet",
                    subtitle = if (session.userRole == UserRole.YOUTH_EXECUTIVE)
                        "Maintain accountability for your unit or cell. Tap the floating '+' button below to submit your first report with attendance, activities, and goals."
                    else if (session.userRole == UserRole.ADMINISTRATOR)
                        "Unit executives submit weekly attendance ledgers and ministry summaries here. As submissions arrive, you can review and approve them."
                    else
                        "Weekly unit milestones, answered prayers, and ministry updates will be displayed here once submitted by unit coordinators.",
                    accentColor = FlameGoldAccent
                )
            }
        } else {
            items(filteredReports, key = { it.id }) { report ->
                WeeklyReportCardItem(
                    report = report,
                    session = session,
                    onOpenReview = { onOpenReview(report) }
                )
            }
        }
    }
}

@Composable
fun WeeklyReportCardItem(
    report: WeeklyReportEntity,
    session: SessionEntity,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = report.departmentOrUnit.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = when (report.status) {
                        ReportStatus.REVIEWED -> Color(0xFF10B981)
                        ReportStatus.SUBMITTED -> FlameGoldAccent
                        else -> ShieldSilver
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = report.status.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = report.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GlobeBlack
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Submitted by ${report.submittedByName} • Attended: ${report.meetingAttendance} (M:${report.maleCount}, F:${report.femaleCount}, 1st:${report.firstTimersCount})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = report.activitiesConducted,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (report.keyAchievements.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Highlights: ${report.keyAchievements}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF10B981)
                    )
                }
            }

            if (report.reviewerNotes != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Executive Leadership Feedback: ${report.reviewerNotes}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = FlameRedPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Admin Review Action
            if (session.userRole == UserRole.ADMINISTRATOR) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenReview,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FlameRedPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.RateReview,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Review & Leave Official Notes")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewReportDialog(
    report: WeeklyReportEntity,
    onDismiss: () -> Unit,
    onConfirmReview: (reportId: String, status: ReportStatus, notes: String) -> Unit
) {
    var status by remember { mutableStateOf(ReportStatus.REVIEWED) }
    var notes by remember { mutableStateOf(report.reviewerNotes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Review Weekly Report",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(
                    text = "Report: ${report.title}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = GlobeBlack
                )
                Text(
                    text = "Department: ${report.departmentOrUnit.displayName} | Submitted by: ${report.submittedByName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ShieldSilver
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Review Decision Status:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == ReportStatus.REVIEWED,
                        onClick = { status = ReportStatus.REVIEWED },
                        label = { Text("Reviewed / Approved") }
                    )
                    FilterChip(
                        selected = status == ReportStatus.ARCHIVED,
                        onClick = { status = ReportStatus.ARCHIVED },
                        label = { Text("Archived") }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Official Executive Feedback / Notes *") },
                    placeholder = { Text("e.g. Approved. Commendable outreach and attendance!") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReview(report.id, status, notes.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text("Save Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitWeeklyReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        dept: DepartmentType,
        fellowship: String,
        attendance: Int,
        males: Int,
        females: Int,
        firstTimers: Int,
        activities: String,
        achievements: String,
        challenges: String,
        finSummary: String,
        nextGoals: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var department by remember { mutableStateOf(DepartmentType.MEDIA) }
    var fellowship by remember { mutableStateOf("") }
    var attendance by remember { mutableStateOf("15") }
    var males by remember { mutableStateOf("8") }
    var females by remember { mutableStateOf("7") }
    var firstTimers by remember { mutableStateOf("2") }
    var activities by remember { mutableStateOf("") }
    var achievements by remember { mutableStateOf("") }
    var challenges by remember { mutableStateOf("") }
    var finSummary by remember { mutableStateOf("") }
    var nextGoals by remember { mutableStateOf("") }

    var deptDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Submit Weekly Unit Report",
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
                    label = { Text("Report Title *") },
                    placeholder = { Text("e.g. Media Unit Sunday Broadcast Report") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = deptDropdownExpanded,
                    onExpandedChange = { deptDropdownExpanded = !deptDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = department.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Department / Unit") },
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = attendance,
                        onValueChange = { attendance = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Total Att.") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = firstTimers,
                        onValueChange = { firstTimers = it.filter { ch -> ch.isDigit() } },
                        label = { Text("1st Timers") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = activities,
                    onValueChange = { activities = it },
                    label = { Text("Key Activities Conducted *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = achievements,
                    onValueChange = { achievements = it },
                    label = { Text("Achievements & Highlights") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = challenges,
                    onValueChange = { challenges = it },
                    label = { Text("Challenges Encountered") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nextGoals,
                    onValueChange = { nextGoals = it },
                    label = { Text("Goals for Next Week") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val att = attendance.toIntOrNull() ?: 0
                    val m = males.toIntOrNull() ?: 0
                    val f = females.toIntOrNull() ?: 0
                    val ft = firstTimers.toIntOrNull() ?: 0
                    onSubmit(title, department, fellowship, att, m, f, ft, activities, achievements, challenges, finSummary, nextGoals)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary)
            ) {
                Text("Submit Report")
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
fun WeeklyReportsScreen(
    viewModel: EngagementViewModel,
    session: SessionEntity,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val reports by viewModel.weeklyReports.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (session.userRole == UserRole.YOUTH_EXECUTIVE || session.userRole == UserRole.ADMINISTRATOR) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = { viewModel.setSubmitReportOpen(true) },
                    containerColor = FlameRedPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("submit_weekly_report_fab")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Assessment,
                        contentDescription = "Submit Weekly Unit Report"
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        WeeklyReportsTabContent(
            reports = reports,
            session = session,
            selectedDeptFilter = uiState.reportDepartmentFilter,
            selectedStatusFilter = uiState.reportStatusFilter,
            onSetDeptFilter = { viewModel.setReportDepartmentFilter(it) },
            onSetStatusFilter = { viewModel.setReportStatusFilter(it) },
            onOpenReview = { viewModel.openReviewReport(it) },
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (uiState.isSubmitReportOpen) {
        SubmitWeeklyReportDialog(
            onDismiss = { viewModel.setSubmitReportOpen(false) },
            onSubmit = { title, dept, fellowship, att, m, f, ft, acts, achs, chals, fin, goals ->
                viewModel.submitWeeklyReport(title, dept, fellowship, att, m, f, ft, acts, achs, chals, fin, goals)
            }
        )
    }

    val selectedReport = uiState.selectedReportForReview
    if (uiState.isReviewReportOpen && selectedReport != null) {
        ReviewReportDialog(
            report = selectedReport,
            onDismiss = { viewModel.closeReviewReport() },
            onConfirmReview = { id, status, notes ->
                viewModel.reviewReport(id, status, notes)
            }
        )
    }
}
