package com.example.ui.community

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.CommunityCommentEntity
import com.example.data.local.entity.CommunityPostEntity
import com.example.data.local.entity.ExecutiveInquiryEntity
import com.example.data.local.entity.SessionEntity
import com.example.ui.components.ModernEmptyState
import com.example.ui.components.UserAvatar
import com.example.ui.components.persistProfilePhotoUri
import com.example.ui.theme.FlameGoldAccent
import com.example.ui.theme.FlameRedPrimary
import com.example.ui.theme.GlobeBlack
import com.example.ui.theme.ShieldSilver
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.WorkOutline
import com.example.ui.components.persistPostAttachmentPhotoUri
import com.example.ui.components.resolveImageModelData
import java.net.URLEncoder

private fun formatWhatsAppUrl(rawContact: String, postTitle: String): String {
    val trimmed = rawContact.trim()
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }
    if (trimmed.startsWith("wa.me/", ignoreCase = true) || trimmed.startsWith("chat.whatsapp.com/", ignoreCase = true)) {
        return "https://$trimmed"
    }
    val digitsOnly = trimmed.replace(Regex("[^0-9]"), "")
    val normalizedNumber = when {
        digitsOnly.startsWith("0") && digitsOnly.length == 11 -> "234${digitsOnly.drop(1)}"
        digitsOnly.isNotEmpty() -> digitsOnly
        else -> ""
    }
    val encodedMsg = try {
        URLEncoder.encode("Hello! I saw your Work/Skills post (\"$postTitle\") on The Gideonites Youth Fellowship platform.", "UTF-8")
    } catch (_: Exception) {
        ""
    }
    return if (normalizedNumber.isNotEmpty()) {
        "https://wa.me/$normalizedNumber?text=$encodedMsg"
    } else {
        "https://wa.me/?text=$encodedMsg"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel,
    session: SessionEntity
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLeadership = session.userRole.isLeadership()

    var showCreatePostDialog by remember { mutableStateOf(false) }
    var postToEdit by remember { mutableStateOf<CommunityPostEntity?>(null) }
    var postToDeleteConfirm by remember { mutableStateOf<CommunityPostEntity?>(null) }
    var selectedInquiryToReply by remember { mutableStateOf<ExecutiveInquiryEntity?>(null) }

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
            if (uiState.activeTab == 0) {
                FloatingActionButton(
                    onClick = { showCreatePostDialog = true },
                    containerColor = FlameRedPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_create_post")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create Post")
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
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Youth Forum", fontWeight = FontWeight.Bold)
                            if (isLeadership && uiState.flaggedPostsCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = FlameRedPrimary, contentColor = Color.White) {
                                    Text("${uiState.flaggedPostsCount}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_forum")
                )
                Tab(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.setTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Executive Contact", fontWeight = FontWeight.Bold)
                            if (isLeadership && uiState.pendingInquiriesCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = FlameGoldAccent, contentColor = GlobeBlack) {
                                    Text("${uiState.pendingInquiriesCount}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_pastor_contact")
                )
            }

            when (uiState.activeTab) {
                0 -> {
                    ForumTab(
                        posts = uiState.filteredPosts,
                        currentUserId = session.userId,
                        selectedCategory = uiState.selectedCategory,
                        searchQuery = uiState.searchQuery,
                        flaggedCount = uiState.flaggedPostsCount,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onSelectCategory = { viewModel.selectCategory(it) },
                        onToggleLike = { postId -> viewModel.toggleLikePost(postId, session.userId) },
                        onOpenComments = { viewModel.openComments(it) },
                        onEditPost = { postToEdit = it },
                        onDeletePost = { postToDeleteConfirm = it },
                        onFlag = { viewModel.flagPost(it) },
                        onUnflag = { viewModel.unflagPost(it) },
                        onTogglePin = { post -> viewModel.togglePinPost(post.id, post.isPinned, session.userId) },
                        onHide = { viewModel.hidePost(it, session.userId) },
                        onOpenCreatePost = { showCreatePostDialog = true },
                        isLeadership = isLeadership
                    )
                }
                1 -> {
                    val myInquiries = remember(uiState.allInquiries, session.userId) {
                        uiState.allInquiries.filter { it.senderId == session.userId }
                    }
                    ExecutiveContactTab(
                        session = session,
                        isLeadership = isLeadership,
                        isSent = uiState.isContactSent,
                        myInquiries = myInquiries,
                        executiveInquiries = uiState.filteredExecutiveInquiries,
                        pendingInquiriesCount = uiState.pendingInquiriesCount,
                        inquiryStatusFilter = uiState.inquiryStatusFilter,
                        onSelectStatusFilter = { viewModel.selectInquiryStatusFilter(it) },
                        onSend = { topic, msg, isAnonymous ->
                            viewModel.sendExecutiveContact(
                                senderId = session.userId,
                                senderName = session.userName,
                                senderRole = session.userRole.displayName,
                                isAnonymous = isAnonymous,
                                topic = topic,
                                message = msg
                            )
                        },
                        onReset = { viewModel.resetContactSent() },
                        onOpenReplyDialog = { inquiry -> selectedInquiryToReply = inquiry }
                    )
                }
            }
        }
    }

    if (showCreatePostDialog) {
        CreateOrEditPostDialog(
            initialPost = null,
            onDismiss = { showCreatePostDialog = false },
            onSubmit = { cat, title, content, attachmentUri, whatsappContact ->
                viewModel.createPost(
                    authorId = session.userId,
                    authorName = session.userName,
                    authorRole = session.userRole.displayName,
                    category = cat,
                    title = title,
                    content = content,
                    attachmentPhotoUri = attachmentUri,
                    whatsappContact = whatsappContact
                )
                showCreatePostDialog = false
            }
        )
    }

    postToEdit?.let { editingPost ->
        CreateOrEditPostDialog(
            initialPost = editingPost,
            onDismiss = { postToEdit = null },
            onSubmit = { cat, title, content, attachmentUri, whatsappContact ->
                viewModel.editPost(
                    postId = editingPost.id,
                    actorId = session.userId,
                    actorRole = session.userRole,
                    category = cat,
                    title = title,
                    content = content,
                    attachmentPhotoUri = attachmentUri,
                    whatsappContact = whatsappContact
                )
                postToEdit = null
            }
        )
    }

    postToDeleteConfirm?.let { deletingPost ->
        AlertDialog(
            onDismissRequest = { postToDeleteConfirm = null },
            title = { Text("Delete Forum Post?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete \"${deletingPost.title}\"? This will permanently remove the post along with all its replies and likes across all devices."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePost(
                            postId = deletingPost.id,
                            actorId = session.userId,
                            actorRole = session.userRole
                        )
                        postToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_post")
                ) {
                    Text("Delete Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    selectedInquiryToReply?.let { inquiry ->
        RespondToInquiryDialog(
            inquiry = inquiry,
            onDismiss = { selectedInquiryToReply = null },
            onSubmit = { status, replyText ->
                viewModel.respondToExecutiveInquiry(
                    inquiryId = inquiry.id,
                    status = status,
                    responseMessage = replyText,
                    responderId = session.userId,
                    responderName = session.userName,
                    responderRole = session.userRole.displayName
                )
                selectedInquiryToReply = null
            }
        )
    }

    uiState.selectedPostForComments?.let { post ->
        CommentsSheetDialog(
            post = post,
            comments = uiState.commentsForSelectedPost,
            onDismiss = { viewModel.closeComments() },
            onAddComment = { content ->
                viewModel.addComment(
                    postId = post.id,
                    authorId = session.userId,
                    authorName = session.userId.let { session.userName },
                    authorRole = session.userRole.displayName,
                    content = content
                )
            }
        )
    }
}

@Composable
private fun ForumTab(
    posts: List<CommunityPostEntity>,
    currentUserId: String,
    selectedCategory: String,
    searchQuery: String,
    flaggedCount: Int,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onOpenComments: (CommunityPostEntity) -> Unit,
    onEditPost: (CommunityPostEntity) -> Unit,
    onDeletePost: (CommunityPostEntity) -> Unit,
    onFlag: (String) -> Unit,
    onUnflag: (String) -> Unit,
    onTogglePin: (CommunityPostEntity) -> Unit,
    onHide: (String) -> Unit,
    onOpenCreatePost: () -> Unit,
    isLeadership: Boolean
) {
    val isTuesdayToday = remember { CommunityPostEntity.isTuesdayToday() }
    val baseCategories = listOf(
        "All",
        "Pinned",
        CommunityPostEntity.CATEGORY_DISCUSSION,
        CommunityPostEntity.CATEGORY_WORK_SKILLS,
        "Testimonies",
        "Prayer Requests",
        "Q&A"
    )
    val categories = if (isLeadership) {
        listOf("All", "Pinned", "Flagged") + baseCategories.drop(2)
    } else {
        baseCategories
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search discussions, skills, topics, or authors...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Forum",
                    tint = FlameRedPrimary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("input_forum_search")
        )

        // Category + Moderation Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isWorkSkills = cat == CommunityPostEntity.CATEGORY_WORK_SKILLS
                val chipLabel = when {
                    cat == "Flagged" -> "Flagged ($flaggedCount)"
                    isWorkSkills && !isTuesdayToday -> "Work/Skills (Tuesdays)"
                    else -> cat
                }
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onSelectCategory(cat) },
                    label = { Text(chipLabel, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = if (isWorkSkills) {
                        {
                            Icon(
                                imageVector = Icons.Default.WorkOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (cat == "Flagged") Color(0xFFB91C1C) else FlameRedPrimary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = if (isWorkSkills && !isTuesdayToday) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        } else {
                            Color.Unspecified
                        },
                        labelColor = if (isWorkSkills && !isTuesdayToday) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            Color.Unspecified
                        }
                    ),
                    modifier = Modifier.testTag("chip_filter_${cat.lowercase().replace(" ", "_").replace("/", "_")}")
                )
            }
        }

        // Tuesday Work/Skills Banner when Work/Skills filter is selected
        if (selectedCategory == CommunityPostEntity.CATEGORY_WORK_SKILLS) {
            Surface(
                color = if (isTuesdayToday) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkOutline,
                        contentDescription = null,
                        tint = if (isTuesdayToday) Color(0xFF15803D) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTuesdayToday) {
                            "Tuesday Work/Skills Showcase is open! Share your business, job, or skill with your WhatsApp contact."
                        } else {
                            "Work/Skills posting opens every Tuesday."
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isTuesdayToday) Color(0xFF14532D) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        if (posts.isEmpty()) {
            ModernEmptyState(
                icon = Icons.AutoMirrored.Filled.Comment,
                title = when (selectedCategory) {
                    "Flagged" -> "No Flagged Posts"
                    CommunityPostEntity.CATEGORY_WORK_SKILLS -> "No Active Work/Skills Posts"
                    else -> "No Discussions Found"
                },
                subtitle = when (selectedCategory) {
                    "Flagged" -> "All reported forum discussions have been reviewed."
                    CommunityPostEntity.CATEGORY_WORK_SKILLS -> if (isTuesdayToday) {
                        "It's Tuesday! Share the work you do, your skills, and your WhatsApp contact so members can connect with you."
                    } else {
                        "Members share their businesses, jobs, and skills every Tuesday."
                    }
                    else -> "Start discussions, share testimonies, and connect with other Gideonites."
                },
                actionText = "+ Start Conversation",
                onAction = onOpenCreatePost,
                accentColor = FlameRedPrimary
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    CommunityPostCard(
                        post = post,
                        currentUserId = currentUserId,
                        isLikedByCurrentUser = post.isLikedBy(currentUserId),
                        onLike = { onToggleLike(post.id) },
                        onComments = { onOpenComments(post) },
                        onEdit = { onEditPost(post) },
                        onDelete = { onDeletePost(post) },
                        onFlag = { onFlag(post.id) },
                        onUnflag = { onUnflag(post.id) },
                        onTogglePin = { onTogglePin(post) },
                        onHide = { onHide(post.id) },
                        isLeadership = isLeadership
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPostEntity,
    currentUserId: String,
    isLikedByCurrentUser: Boolean,
    onLike: () -> Unit,
    onComments: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFlag: () -> Unit,
    onUnflag: () -> Unit,
    onTogglePin: () -> Unit,
    onHide: () -> Unit,
    isLeadership: Boolean
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isPostAuthor = post.authorId == currentUserId
    val displayCategory = when {
        post.departmentOrCategory.equals("Faith & Career", ignoreCase = true) -> CommunityPostEntity.CATEGORY_DISCUSSION
        post.departmentOrCategory.equals("Spiritual Growth", ignoreCase = true) -> CommunityPostEntity.CATEGORY_WORK_SKILLS
        else -> post.departmentOrCategory
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (post.isPinned) {
                    Modifier.border(1.5.dp, FlameGoldAccent, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
            .testTag("post_card_${post.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Pinned or Flagged Status Banner
            if (post.isPinned || post.isFlagged) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.isPinned) {
                        Surface(
                            color = FlameGoldAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = FlameGoldAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PINNED BY LEADERSHIP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FlameGoldAccent
                                    )
                                )
                            }
                        }
                    }
                    if (post.isFlagged) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = "Flagged",
                                    tint = Color(0xFFB91C1C),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "FLAGGED FOR REVIEW",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB91C1C)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    photoUri = post.authorAvatarUri,
                    fullName = post.authorName,
                    sizeDp = 40
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${post.authorRole} • ${timeFormat.format(Date(post.createdAt))}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (CommunityPostEntity.isWorkSkillsCategory(displayCategory)) {
                                Color(0xFFDCFCE7)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = displayCategory,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (CommunityPostEntity.isWorkSkillsCategory(displayCategory)) {
                                Color(0xFF15803D)
                            } else {
                                FlameRedPrimary
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            // Optional Attached Photo (supports cross-device base64 data URI, file://, and content://)
            if (!post.attachmentPhotoUri.isNullOrBlank()) {
                val modelData = remember(post.attachmentPhotoUri) {
                    resolveImageModelData(post.attachmentPhotoUri)
                }
                if (modelData != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(modelData)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Post attachment image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }

            // WhatsApp Direct Contact & Platform Comment Action Bar for Work/Skills (or any post with WhatsApp contact)
            if (!post.whatsappContact.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WORK / SKILLS CONTACT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF166534)
                                    )
                                )
                                Text(
                                    text = "WhatsApp: ${post.whatsappContact}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF14532D)
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val waUrl = formatWhatsAppUrl(post.whatsappContact, post.title)
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(waUrl)
                                        ).apply {
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_whatsapp_${post.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Message on WhatsApp",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Message on WhatsApp", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = onComments,
                                modifier = Modifier.testTag("btn_comment_skill_${post.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Comment,
                                    contentDescription = "Message in Comments",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Comment", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onLike,
                        modifier = Modifier.testTag("btn_like_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (isLikedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isLikedByCurrentUser) "Unlike" else "Like",
                            tint = if (isLikedByCurrentUser) FlameRedPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "${post.likesCount}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isLikedByCurrentUser) FlameRedPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = onComments,
                        modifier = Modifier.testTag("btn_comments_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "${post.commentsCount} replies",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Author can Edit or Delete their own post
                    if (isPostAuthor) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("btn_edit_post_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit post",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("btn_delete_post_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete post",
                                tint = Color(0xFFBA1A1A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (!post.isFlagged && !isPostAuthor) {
                        IconButton(
                            onClick = onFlag,
                            modifier = Modifier.testTag("btn_flag_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Report post",
                                tint = ShieldSilver,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (post.isFlagged && isLeadership) {
                        IconButton(
                            onClick = onUnflag,
                            modifier = Modifier.testTag("btn_unflag_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Approve & unflag post",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (isLeadership) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.testTag("btn_pin_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = if (post.isPinned) "Unpin post" else "Pin post",
                                tint = if (post.isPinned) FlameGoldAccent else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (!isPostAuthor) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.testTag("btn_hide_${post.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Moderate & delete post",
                                    tint = Color(0xFFBA1A1A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentsSheetDialog(
    post: CommunityPostEntity,
    comments: List<CommunityCommentEntity>,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Replies", fontWeight = FontWeight.Bold)
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.labelSmall.copy(color = ShieldSilver),
                    maxLines = 1
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (comments.isEmpty()) {
                    Text(
                        text = "Be the first to share your thoughts or message ${post.authorName} here!",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(comments, key = { it.id }) { comment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                UserAvatar(
                                    photoUri = comment.authorAvatarUri,
                                    fullName = comment.authorName,
                                    sizeDp = 28
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${comment.authorName} (${comment.authorRole})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = dateFormat.format(Date(comment.createdAt)),
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(text = comment.content, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Write a reply...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_comment_reply")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                onAddComment(newCommentText)
                                newCommentText = ""
                            }
                        },
                        modifier = Modifier.testTag("btn_send_comment")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply", tint = FlameRedPrimary)
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
private fun CreateOrEditPostDialog(
    initialPost: CommunityPostEntity?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String?, String?) -> Unit
) {
    val context = LocalContext.current
    val isTuesdayToday = remember { CommunityPostEntity.isTuesdayToday() }
    val categories = listOf(
        CommunityPostEntity.CATEGORY_DISCUSSION,
        CommunityPostEntity.CATEGORY_WORK_SKILLS,
        "Testimonies",
        "Prayer Requests",
        "Q&A"
    )
    val initialMappedCategory = remember(initialPost) {
        when {
            initialPost == null -> CommunityPostEntity.CATEGORY_DISCUSSION
            initialPost.departmentOrCategory.equals("Faith & Career", ignoreCase = true) -> CommunityPostEntity.CATEGORY_DISCUSSION
            initialPost.departmentOrCategory.equals("Spiritual Growth", ignoreCase = true) -> CommunityPostEntity.CATEGORY_WORK_SKILLS
            categories.any { it.equals(initialPost.departmentOrCategory, ignoreCase = true) } -> initialPost.departmentOrCategory
            else -> CommunityPostEntity.CATEGORY_DISCUSSION
        }
    }
    var selectedCategory by remember { mutableStateOf(initialMappedCategory) }
    var title by remember { mutableStateOf(initialPost?.title ?: "") }
    var content by remember { mutableStateOf(initialPost?.content ?: "") }
    var attachedPhotoUri by remember { mutableStateOf(initialPost?.attachmentPhotoUri) }
    var whatsappContact by remember { mutableStateOf(initialPost?.whatsappContact ?: "") }
    var tuesdayNotice by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedPhotoUri = persistPostAttachmentPhotoUri(context, uri)
        }
    }

    val isWorkSkillsSelected = CommunityPostEntity.isWorkSkillsCategory(selectedCategory)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialPost == null) "New Forum Post" else "Edit Forum Post",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Category:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isWorkSkillsChip = cat == CommunityPostEntity.CATEGORY_WORK_SKILLS
                        val isWorkSkillsAllowed = isTuesdayToday ||
                            (initialPost != null && CommunityPostEntity.isWorkSkillsCategory(initialMappedCategory))
                        val enabled = !isWorkSkillsChip || isWorkSkillsAllowed

                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                if (enabled) {
                                    selectedCategory = cat
                                    tuesdayNotice = null
                                } else {
                                    tuesdayNotice = "Work/Skills posts are only available on Tuesdays!"
                                }
                            },
                            enabled = enabled,
                            label = {
                                Text(
                                    if (isWorkSkillsChip && !isWorkSkillsAllowed) "Work/Skills (Tuesdays Only)" else cat
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameRedPrimary,
                                selectedLabelColor = Color.White,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                disabledLabelColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            )
                        )
                    }
                }

                if (!isTuesdayToday) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tuesdayNotice ?: "Note: 'Work/Skills' is available on Tuesdays.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (tuesdayNotice != null) FlameRedPrimary else MaterialTheme.colorScheme.outline
                        )
                    )
                } else if (isWorkSkillsSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tuesday Work/Skills Showcase: Share your job or skill and your WhatsApp contact.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text(
                            if (isWorkSkillsSelected) "Your Job / Skill / Business Title"
                            else "Discussion Topic / Question"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_post_title")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = {
                        Text(
                            if (isWorkSkillsSelected) "Describe the work you do, services offered, & skills..."
                            else "Share details, testimony, or scripture..."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_post_content"),
                    minLines = 3
                )

                if (isWorkSkillsSelected || whatsappContact.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = whatsappContact,
                        onValueChange = { whatsappContact = it },
                        label = { Text("WhatsApp Number or Link (e.g. +23480... or wa.me/...)") },
                        placeholder = { Text("+2348012345678 or https://wa.me/...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_post_whatsapp")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Picture Attachment + Preview
                if (!attachedPhotoUri.isNullOrBlank()) {
                    val previewModel = remember(attachedPhotoUri) {
                        resolveImageModelData(attachedPhotoUri)
                    }
                    if (previewModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(previewModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Selected attachment preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("btn_attach_post_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach Picture",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (attachedPhotoUri == null) "Attach Picture (Optional)" else "Change Picture")
                    }

                    if (attachedPhotoUri != null) {
                        TextButton(onClick = { attachedPhotoUri = null }) {
                            Text("Remove", color = FlameRedPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        selectedCategory,
                        title,
                        content,
                        attachedPhotoUri,
                        whatsappContact.trim().takeIf { it.isNotBlank() }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                modifier = Modifier.testTag("btn_submit_post")
            ) {
                Text(if (initialPost == null) "Share Post" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ExecutiveContactTab(
    session: SessionEntity,
    isLeadership: Boolean,
    isSent: Boolean,
    myInquiries: List<ExecutiveInquiryEntity>,
    executiveInquiries: List<ExecutiveInquiryEntity>,
    pendingInquiriesCount: Int,
    inquiryStatusFilter: String,
    onSelectStatusFilter: (String) -> Unit,
    onSend: (String, String, Boolean) -> Unit,
    onReset: () -> Unit,
    onOpenReplyDialog: (ExecutiveInquiryEntity) -> Unit
) {
    val topics = listOf(
        "Spiritual Counseling & Mentorship",
        "Confidential Prayer Request",
        "Career & Academic Guidance",
        "Feedback for Youth Leadership",
        "Disciplinary / Personal Inquiry"
    )
    var selectedTopic by remember { mutableStateOf(topics.first()) }
    var message by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    var leadershipInboxMode by remember(isLeadership) { mutableStateOf(isLeadership) }
    val timeFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = GlobeBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = FlameGoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DIRECT PASTORAL & EXECUTIVE ACCESS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = FlameGoldAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Need confidential prayer, counseling, or direct executive assistance? Messages sent here are delivered directly to the Youth Executive Board and Pastoral Leadership with two-way response tracking.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f))
                    )
                }
            }
        }

        // Leadership Mode Switcher (Executive Inbox vs Submit Form)
        if (isLeadership) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = leadershipInboxMode,
                        onClick = { leadershipInboxMode = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Executive Inbox ($pendingInquiriesCount Pending)",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlameRedPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("btn_mode_executive_inbox")
                    )

                    FilterChip(
                        selected = !leadershipInboxMode,
                        onClick = { leadershipInboxMode = false },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Send Inquiry", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlameRedPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("btn_mode_submit_inquiry")
                    )
                }
            }
        }

        if (isLeadership && leadershipInboxMode) {
            // Executive Inbox Filter Row
            item {
                val statusFilters = listOf("All", "PENDING", "IN_PROGRESS", "RESOLVED")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusFilters) { status ->
                        FilterChip(
                            selected = inquiryStatusFilter == status,
                            onClick = { onSelectStatusFilter(status) },
                            label = {
                                Text(
                                    text = status.replace("_", " "),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlobeBlack,
                                selectedLabelColor = FlameGoldAccent
                            )
                        )
                    }
                }
            }

            if (executiveInquiries.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = ShieldSilver,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Inquiries in Queue",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Confidential member prayer requests, counseling inquiries, and leadership feedback will appear here.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(executiveInquiries, key = { it.id }) { inquiry ->
                    ExecutiveInquiryCard(
                        inquiry = inquiry,
                        timeFormat = timeFormat,
                        isLeadershipView = true,
                        onReplyClick = { onOpenReplyDialog(inquiry) }
                    )
                }
            }
        } else {
            // Member / Submit Inquiry Form
            if (isSent) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "MESSAGE DELIVERED IN CONFIDENCE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "The Pastoral & Executive board has received your inquiry. Track leadership replies below in 'My Inquiries & Responses'.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF14532D)),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    message = ""
                                    onReset()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Text("Send Another Message")
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isAnonymous) {
                                            "From: Anonymous Youth Member"
                                        } else {
                                            "From: ${session.userName} (${session.userRole.displayName})"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Toggle anonymity for sensitive counseling/prayer requests",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Anonymous",
                                        tint = if (isAnonymous) FlameGoldAccent else ShieldSilver,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Switch(
                                        checked = isAnonymous,
                                        onCheckedChange = { isAnonymous = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = GlobeBlack,
                                            checkedTrackColor = FlameGoldAccent
                                        ),
                                        modifier = Modifier.testTag("switch_inquiry_anonymous")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Subject Category:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(topics) { topic ->
                                    FilterChip(
                                        selected = selectedTopic == topic,
                                        onClick = { selectedTopic = topic },
                                        label = { Text(topic) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = FlameRedPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                label = { Text("Your confidential message or prayer request...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_executive_message"),
                                minLines = 4
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onSend(selectedTopic, message, isAnonymous) },
                                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_send_executive_contact")
                            ) {
                                Icon(imageVector = Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Transmit Confidential Message")
                            }
                        }
                    }
                }
            }

            // Member's Sent Inquiries & Pastoral Responses History
            if (myInquiries.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MY INQUIRIES & PASTORAL RESPONSES (${myInquiries.size})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = FlameRedPrimary,
                            letterSpacing = 0.6.sp
                        )
                    )
                }
                items(myInquiries, key = { it.id }) { inquiry ->
                    ExecutiveInquiryCard(
                        inquiry = inquiry,
                        timeFormat = timeFormat,
                        isLeadershipView = false,
                        onReplyClick = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun ExecutiveInquiryCard(
    inquiry: ExecutiveInquiryEntity,
    timeFormat: SimpleDateFormat,
    isLeadershipView: Boolean,
    onReplyClick: () -> Unit
) {
    val statusColor = when (inquiry.status) {
        "RESOLVED" -> Color(0xFF16A34A)
        "IN_PROGRESS" -> FlameGoldAccent
        else -> FlameRedPrimary
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inquiry_card_${inquiry.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (inquiry.isAnonymous) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GlobeBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Anonymous",
                                tint = FlameGoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        UserAvatar(
                            photoUri = inquiry.senderAvatarUri,
                            fullName = inquiry.senderName,
                            sizeDp = 36
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = inquiry.topic,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${inquiry.senderName} • ${timeFormat.format(Date(inquiry.createdAt))}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = inquiry.status.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = inquiry.message,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )

            if (!inquiry.responseMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = GlobeBlack,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "PASTORAL / EXECUTIVE RESPONSE • ${inquiry.respondedByName ?: "Youth Leadership"} (${inquiry.respondedByRole ?: "Executive"})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FlameGoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = inquiry.responseMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                        )
                    }
                }
            }

            if (isLeadershipView) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReplyClick,
                        modifier = Modifier.testTag("btn_reply_inquiry_${inquiry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Respond to inquiry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (inquiry.responseMessage.isNullOrBlank()) "Send Pastoral Reply" else "Update Reply / Status")
                    }
                }
            }
        }
    }
}

@Composable
private fun RespondToInquiryDialog(
    inquiry: ExecutiveInquiryEntity,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val statuses = listOf("IN_PROGRESS", "RESOLVED")
    var selectedStatus by remember { mutableStateOf(if (inquiry.status == "RESOLVED") "RESOLVED" else "RESOLVED") }
    var responseText by remember { mutableStateOf(inquiry.responseMessage ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pastoral & Executive Response", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Topic: ${inquiry.topic}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = FlameRedPrimary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${inquiry.message}\"",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Resolution Status:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    statuses.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st.replace("_", " ")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameRedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = responseText,
                    onValueChange = { responseText = it },
                    label = { Text("Write counsel, prayer, or leadership response...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_inquiry_response"),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedStatus, responseText) },
                colors = ButtonDefaults.buttonColors(containerColor = FlameRedPrimary),
                modifier = Modifier.testTag("btn_submit_inquiry_response")
            ) {
                Text("Send Response")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
