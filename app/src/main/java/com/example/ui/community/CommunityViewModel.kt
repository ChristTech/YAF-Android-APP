package com.example.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CommunityCommentEntity
import com.example.data.local.entity.CommunityPostEntity
import com.example.data.local.entity.ExecutiveInquiryEntity
import com.example.data.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CommunityUiState(
    val posts: List<CommunityPostEntity> = emptyList(),
    val filteredPosts: List<CommunityPostEntity> = emptyList(),
    val flaggedPostsCount: Int = 0,
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val activeTab: Int = 0,
    val selectedPostForComments: CommunityPostEntity? = null,
    val commentsForSelectedPost: List<CommunityCommentEntity> = emptyList(),
    val allInquiries: List<ExecutiveInquiryEntity> = emptyList(),
    val filteredExecutiveInquiries: List<ExecutiveInquiryEntity> = emptyList(),
    val pendingInquiriesCount: Int = 0,
    val inquiryStatusFilter: String = "All",
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val isContactSent: Boolean = false
)

class CommunityViewModel(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _activeTab = MutableStateFlow(0)
    private val _selectedPost = MutableStateFlow<CommunityPostEntity?>(null)
    private val _comments = MutableStateFlow<List<CommunityCommentEntity>>(emptyList())
    private var commentsJob: kotlinx.coroutines.Job? = null
    private val _inquiryStatusFilter = MutableStateFlow("All")
    private val _successMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _contactSent = MutableStateFlow(false)

    val uiState: StateFlow<CommunityUiState> = combine(
        communityRepository.getActivePosts(),
        _selectedCategory,
        _searchQuery,
        _activeTab,
        _selectedPost
    ) { posts, category, query, tab, selPost ->
        val categoryMatched = when {
            category.equals("All", ignoreCase = true) -> posts
            category.equals("Pinned", ignoreCase = true) -> posts.filter { it.isPinned }
            category.equals("Flagged", ignoreCase = true) -> posts.filter { it.isFlagged }
            category.equals("Discussion", ignoreCase = true) -> posts.filter {
                it.departmentOrCategory.equals("Discussion", ignoreCase = true) ||
                    it.departmentOrCategory.equals("Faith & Career", ignoreCase = true)
            }
            CommunityPostEntity.isWorkSkillsCategory(category) -> posts.filter {
                CommunityPostEntity.isWorkSkillsCategory(it.departmentOrCategory) ||
                    it.departmentOrCategory.equals("Spiritual Growth", ignoreCase = true)
            }
            else -> posts.filter { it.departmentOrCategory.equals(category, ignoreCase = true) }
        }
        val queryTrimmed = query.trim()
        val filtered = if (queryTrimmed.isEmpty()) {
            categoryMatched
        } else {
            categoryMatched.filter { post ->
                post.title.contains(queryTrimmed, ignoreCase = true) ||
                    post.content.contains(queryTrimmed, ignoreCase = true) ||
                    post.authorName.contains(queryTrimmed, ignoreCase = true) ||
                    post.departmentOrCategory.contains(queryTrimmed, ignoreCase = true) ||
                    (post.whatsappContact?.contains(queryTrimmed, ignoreCase = true) == true)
            }
        }
        CommunityUiState(
            posts = posts,
            filteredPosts = filtered,
            flaggedPostsCount = posts.count { it.isFlagged },
            selectedCategory = category,
            searchQuery = query,
            activeTab = tab,
            selectedPostForComments = selPost,
            commentsForSelectedPost = _comments.value,
            successMessage = _successMessage.value,
            errorMessage = _errorMessage.value,
            isContactSent = _contactSent.value
        )
    }.combine(communityRepository.getAllInquiries()) { state, inquiries ->
        state.copy(
            allInquiries = inquiries,
            pendingInquiriesCount = inquiries.count { it.status == "PENDING" }
        )
    }.combine(_inquiryStatusFilter) { state, statusFilter ->
        val filteredInq = if (statusFilter == "All") {
            state.allInquiries
        } else {
            state.allInquiries.filter { it.status.equals(statusFilter, ignoreCase = true) }
        }
        state.copy(
            inquiryStatusFilter = statusFilter,
            filteredExecutiveInquiries = filteredInq
        )
    }.combine(_comments) { state, comments ->
        state.copy(commentsForSelectedPost = comments)
    }.combine(_successMessage) { state, succ ->
        state.copy(successMessage = succ)
    }.combine(_errorMessage) { state, err ->
        state.copy(errorMessage = err)
    }.combine(_contactSent) { state, sent ->
        state.copy(isContactSent = sent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CommunityUiState())

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectInquiryStatusFilter(filter: String) {
        _inquiryStatusFilter.value = filter
    }

    fun setTab(tab: Int) {
        _activeTab.value = tab
        clearMessages()
    }

    fun createPost(
        authorId: String,
        authorName: String,
        authorRole: String,
        category: String,
        title: String,
        content: String,
        attachmentPhotoUri: String? = null,
        whatsappContact: String? = null
    ) {
        viewModelScope.launch {
            if (title.isBlank() || content.isBlank()) {
                _errorMessage.value = "Title and content cannot be empty"
                return@launch
            }
            if (CommunityPostEntity.isWorkSkillsCategory(category) && !CommunityPostEntity.isTuesdayToday()) {
                _errorMessage.value = "Work/Skills posts are only available on Tuesdays!"
                return@launch
            }
            val result = communityRepository.createPost(
                authorId = authorId,
                authorName = authorName,
                authorRole = authorRole,
                category = category,
                title = title.trim(),
                content = content.trim(),
                attachmentPhotoUri = attachmentPhotoUri,
                whatsappContact = whatsappContact?.trim()?.takeIf { it.isNotBlank() }
            )
            if (result.isSuccess) {
                _successMessage.value = if (CommunityPostEntity.isWorkSkillsCategory(category)) {
                    "Work/Skills post shared! Automatically clears after 3 days."
                } else {
                    "Post shared with The Gideonites community!"
                }
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to post"
            }
        }
    }

    fun editPost(
        postId: String,
        actorId: String,
        actorRole: com.example.data.model.UserRole,
        category: String,
        title: String,
        content: String,
        attachmentPhotoUri: String? = null,
        whatsappContact: String? = null
    ) {
        viewModelScope.launch {
            if (title.isBlank() || content.isBlank()) {
                _errorMessage.value = "Title and content cannot be empty"
                return@launch
            }
            val result = communityRepository.editPost(
                postId = postId,
                actorId = actorId,
                actorRole = actorRole,
                category = category,
                title = title.trim(),
                content = content.trim(),
                attachmentPhotoUri = attachmentPhotoUri,
                whatsappContact = whatsappContact?.trim()?.takeIf { it.isNotBlank() }
            )
            if (result.isSuccess) {
                _successMessage.value = "Post updated across all devices!"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to update post"
            }
        }
    }

    fun deletePost(
        postId: String,
        actorId: String,
        actorRole: com.example.data.model.UserRole
    ) {
        viewModelScope.launch {
            val result = communityRepository.deletePost(
                postId = postId,
                actorId = actorId,
                actorRole = actorRole
            )
            if (result.isSuccess) {
                _successMessage.value = "Post deleted"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to delete post"
            }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            communityRepository.likePost(postId)
        }
    }

    fun toggleLikePost(postId: String, userId: String) {
        viewModelScope.launch {
            communityRepository.toggleLikePost(postId, userId)
        }
    }

    fun togglePinPost(postId: String, currentlyPinned: Boolean, adminUserId: String) {
        viewModelScope.launch {
            val newPinned = !currentlyPinned
            val result = communityRepository.pinPost(postId, newPinned, adminUserId)
            if (result.isSuccess) {
                _successMessage.value = if (newPinned) "Discussion pinned to top of forum" else "Discussion unpinned"
            }
        }
    }

    fun openComments(post: CommunityPostEntity) {
        commentsJob?.cancel()
        _selectedPost.value = post
        commentsJob = viewModelScope.launch {
            communityRepository.getCommentsForPost(post.id).collect {
                _comments.value = it
            }
        }
    }

    fun closeComments() {
        commentsJob?.cancel()
        commentsJob = null
        _selectedPost.value = null
        _comments.value = emptyList()
    }

    fun addComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorRole: String,
        content: String
    ) {
        viewModelScope.launch {
            if (content.isBlank()) return@launch
            val result = communityRepository.addComment(postId, authorId, authorName, authorRole, content.trim())
            if (result.isSuccess) {
                _successMessage.value = "Reply posted"
            }
        }
    }

    fun flagPost(postId: String) {
        viewModelScope.launch {
            communityRepository.flagPost(postId, true)
            _successMessage.value = "Post reported to leadership moderators for review"
        }
    }

    fun unflagPost(postId: String) {
        viewModelScope.launch {
            communityRepository.flagPost(postId, false)
            _successMessage.value = "Flag dismissed — post approved"
        }
    }

    fun hidePost(postId: String, adminUserId: String) {
        viewModelScope.launch {
            communityRepository.hidePost(postId, true, adminUserId)
            _successMessage.value = "Post removed by moderation"
        }
    }

    fun sendExecutiveContact(
        senderId: String,
        senderName: String,
        senderRole: String,
        isAnonymous: Boolean = false,
        topic: String,
        message: String
    ) {
        viewModelScope.launch {
            if (message.isBlank()) {
                _errorMessage.value = "Please enter your message for leadership"
                return@launch
            }
            val result = communityRepository.submitExecutiveInquiry(
                senderId = senderId,
                senderName = senderName,
                senderRole = senderRole,
                isAnonymous = isAnonymous,
                topic = topic,
                message = message.trim()
            )
            if (result.isSuccess) {
                _contactSent.value = true
                _successMessage.value = "Confidential message delivered to the Youth Pastoral & Executive board!"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to send inquiry"
            }
        }
    }

    fun respondToExecutiveInquiry(
        inquiryId: String,
        status: String,
        responseMessage: String,
        responderId: String,
        responderName: String,
        responderRole: String
    ) {
        viewModelScope.launch {
            if (responseMessage.isBlank()) {
                _errorMessage.value = "Please enter a pastoral response message"
                return@launch
            }
            val result = communityRepository.respondToExecutiveInquiry(
                inquiryId = inquiryId,
                status = status,
                responseMessage = responseMessage.trim(),
                responderId = responderId,
                responderName = responderName,
                responderRole = responderRole
            )
            if (result.isSuccess) {
                _successMessage.value = "Pastoral response sent ($status)"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to send response"
            }
        }
    }

    fun resetContactSent() {
        _contactSent.value = false
    }

    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }

    companion object {
        fun provideFactory(
            communityRepository: CommunityRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CommunityViewModel(communityRepository) as T
            }
        }
    }
}
