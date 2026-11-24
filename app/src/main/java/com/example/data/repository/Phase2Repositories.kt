package com.example.data.repository

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import java.nio.ByteBuffer
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.CommunityDao
import com.example.data.local.dao.DepartmentDao
import com.example.data.local.dao.SpiritualResourceDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CommunityCommentEntity
import com.example.data.local.entity.CommunityPostEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.ExecutiveInquiryEntity
import com.example.data.local.entity.SpiritualResourceEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.DepartmentType
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

// ==========================================
// 1. Department Repository (FR-013, FR-014, FR-015)
// ==========================================

interface DepartmentRepository {
    fun getAllDepartments(): Flow<List<DepartmentEntity>>
    fun getDepartmentByType(type: DepartmentType): Flow<DepartmentEntity?>
    fun getDepartmentMembers(type: DepartmentType): Flow<List<UserEntity>>
    suspend fun assignMemberToDepartment(userId: String, department: DepartmentType, adminUserId: String): Result<Unit>
    suspend fun updateDepartmentDetails(department: DepartmentEntity, adminUserId: String): Result<Unit>
}

class DepartmentRepositoryImpl(
    private val departmentDao: DepartmentDao,
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao
) : DepartmentRepository {

    override fun getAllDepartments(): Flow<List<DepartmentEntity>> =
        departmentDao.getAllDepartments()

    override fun getDepartmentByType(type: DepartmentType): Flow<DepartmentEntity?> =
        departmentDao.getDepartmentByType(type)

    override fun getDepartmentMembers(type: DepartmentType): Flow<List<UserEntity>> =
        userDao.getUsersByDepartment(type)

    override suspend fun assignMemberToDepartment(
        userId: String,
        department: DepartmentType,
        adminUserId: String
    ): Result<Unit> {
        return try {
            userDao.assignUserDepartment(userId, department)
            departmentDao.incrementMemberCount(department)
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = adminUserId,
                    userRole = UserRole.ADMINISTRATOR,
                    actionType = "DEPARTMENT_MEMBER_ASSIGNED",
                    targetEntity = "Department: ${department.displayName}",
                    details = "Assigned user $userId to department ${department.displayName}"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDepartmentDetails(
        department: DepartmentEntity,
        adminUserId: String
    ): Result<Unit> {
        return try {
            departmentDao.updateDepartment(department)
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = adminUserId,
                    userRole = UserRole.ADMINISTRATOR,
                    actionType = "DEPARTMENT_UPDATED",
                    targetEntity = "Department: ${department.name}",
                    details = "Updated schedule/venue for ${department.name}"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ==========================================
// 2. Attendance Repository (FR-016, FR-017)
// ==========================================

interface AttendanceRepository {
    fun getAllAttendance(): Flow<List<AttendanceRecordEntity>>
    fun getAttendanceByDepartment(dept: String): Flow<List<AttendanceRecordEntity>>
    fun getTotalAttendeesAcrossServices(): Flow<Int?>
    suspend fun recordAttendance(record: AttendanceRecordEntity): Result<Unit>
    fun generateCheckInCode(prefix: String = "CYP"): String
    suspend fun verifyAndCheckIn(
        verificationCode: String,
        userId: String,
        userName: String,
        serviceTitle: String
    ): Result<AttendanceRecordEntity>
}

class AttendanceRepositoryImpl(
    private val attendanceDao: AttendanceDao,
    private val auditLogDao: AuditLogDao
) : AttendanceRepository {

    override fun getAllAttendance(): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getAllAttendance()

    override fun getAttendanceByDepartment(dept: String): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getAttendanceByDepartment(dept)

    override fun getTotalAttendeesAcrossServices(): Flow<Int?> =
        attendanceDao.getTotalAttendeesAcrossServices()

    override suspend fun recordAttendance(record: AttendanceRecordEntity): Result<Unit> {
        return try {
            attendanceDao.insertAttendance(record)
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = record.recordedById,
                    userRole = UserRole.YOUTH_EXECUTIVE,
                    actionType = "ATTENDANCE_RECORDED",
                    targetEntity = "Attendance: ${record.meetingTitle}",
                    details = "Total: ${record.totalAttendees} (M: ${record.maleCount}, F: ${record.femaleCount}, FirstTimers: ${record.firstTimersCount})"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun generateCheckInCode(prefix: String): String {
        val randomDigits = (1000..9999).random()
        return "$prefix-$randomDigits"
    }

    override suspend fun verifyAndCheckIn(
        verificationCode: String,
        userId: String,
        userName: String,
        serviceTitle: String
    ): Result<AttendanceRecordEntity> {
        return try {
            val record = AttendanceRecordEntity(
                id = UUID.randomUUID().toString(),
                meetingTitle = serviceTitle,
                meetingType = "SELF_SERVICE",
                departmentOrGroup = "General",
                dateMillis = System.currentTimeMillis(),
                totalAttendees = 1,
                maleCount = 1,
                femaleCount = 0,
                firstTimersCount = 0,
                checkInMethod = "QR_SELF_CODE",
                verificationCode = verificationCode,
                recordedById = userId,
                recordedByName = userName,
                notes = "Self check-in verified with code $verificationCode"
            )
            attendanceDao.insertAttendance(record)
            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ==========================================
// 3. Community Forum & Executive Contact Repository (FR-018, FR-019)
// ==========================================

interface CommunityRepository {
    fun getActivePosts(): Flow<List<CommunityPostEntity>>
    fun getFlaggedPosts(): Flow<List<CommunityPostEntity>>
    fun getPostById(postId: String): Flow<CommunityPostEntity?>
    suspend fun purgeExpiredPosts(nowMillis: Long = System.currentTimeMillis()): Int
    suspend fun createPost(
        authorId: String,
        authorName: String,
        authorRole: String,
        category: String,
        title: String,
        content: String,
        attachmentPhotoUri: String? = null,
        whatsappContact: String? = null,
        authorAvatarUri: String? = null
    ): Result<Unit>
    suspend fun editPost(
        postId: String,
        actorId: String,
        actorRole: UserRole,
        category: String,
        title: String,
        content: String,
        attachmentPhotoUri: String? = null,
        whatsappContact: String? = null
    ): Result<Unit>
    suspend fun deletePost(
        postId: String,
        actorId: String,
        actorRole: UserRole
    ): Result<Unit>
    suspend fun likePost(postId: String): Result<Unit>
    suspend fun toggleLikePost(postId: String, userId: String): Result<Boolean>
    suspend fun pinPost(postId: String, pinned: Boolean, adminUserId: String): Result<Unit>
    fun getCommentsForPost(postId: String): Flow<List<CommunityCommentEntity>>
    suspend fun addComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorRole: String,
        content: String,
        authorAvatarUri: String? = null
    ): Result<Unit>
    suspend fun flagPost(postId: String, flagged: Boolean): Result<Unit>
    suspend fun hidePost(postId: String, hidden: Boolean, adminUserId: String): Result<Unit>

    // Executive Contact Inquiries
    fun getAllInquiries(): Flow<List<ExecutiveInquiryEntity>>
    fun getInquiriesForUser(userId: String): Flow<List<ExecutiveInquiryEntity>>
    suspend fun submitExecutiveInquiry(
        senderId: String,
        senderName: String,
        senderRole: String,
        isAnonymous: Boolean = false,
        topic: String,
        message: String,
        senderAvatarUri: String? = null
    ): Result<ExecutiveInquiryEntity>
    suspend fun respondToExecutiveInquiry(
        inquiryId: String,
        status: String,
        responseMessage: String,
        responderId: String,
        responderName: String,
        responderRole: String
    ): Result<Unit>
    suspend fun syncCloudPostsToLocalDb(
        cloudPosts: List<com.example.data.remote.CloudCommunityPost>,
        announcementDao: com.example.data.local.dao.AnnouncementDao? = null,
        nowMillis: Long = System.currentTimeMillis()
    )
    suspend fun syncCloudInquiriesToLocalDb(
        cloudInquiries: List<com.example.data.remote.CloudExecutiveInquiry>
    )
    suspend fun pushLocalPostsToCloud()
}

class CommunityRepositoryImpl(
    private val communityDao: CommunityDao,
    private val auditLogDao: AuditLogDao,
    private val userDao: UserDao? = null,
    private val cloudRepository: GideonitesCloudRepository? = null,
    private val notificationService: com.example.core.notification.FellowshipNotificationService? = null
) : CommunityRepository {

    companion object {
        const val SEVEN_DAYS_MILLIS = CommunityPostEntity.SEVEN_DAYS_MILLIS
        const val THREE_DAYS_MILLIS = CommunityPostEntity.THREE_DAYS_MILLIS
    }

    private fun serializeCommentsJson(comments: List<CommunityCommentEntity>): String? {
        if (comments.isEmpty()) return null
        fun buildJson(includeAvatars: Boolean): String {
            val arr = org.json.JSONArray()
            for (c in comments) {
                val obj = org.json.JSONObject().apply {
                    put("id", c.id)
                    put("postId", c.postId)
                    put("authorId", c.authorId)
                    put("authorName", c.authorName)
                    put("authorRole", c.authorRole)
                    if (includeAvatars && !c.authorAvatarUri.isNullOrBlank()) {
                        put("authorAvatarUri", c.authorAvatarUri)
                    }
                    put("content", c.content)
                    put("createdAt", c.createdAt)
                }
                arr.put(obj)
            }
            return arr.toString()
        }
        val withAvatars = buildJson(includeAvatars = true)
        return if (withAvatars.length <= 300_000) withAvatars else buildJson(includeAvatars = false)
    }

    private fun deserializeCommentsJson(postId: String, json: String?): List<CommunityCommentEntity> {
        if (json.isNullOrBlank()) return emptyList()
        val result = mutableListOf<CommunityCommentEntity>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id").trim()
                val content = obj.optString("content").trim()
                if (id.isBlank() || content.isBlank()) continue
                result.add(
                    CommunityCommentEntity(
                        id = id,
                        postId = postId,
                        authorId = obj.optString("authorId").ifBlank { "member" },
                        authorName = obj.optString("authorName").ifBlank { "Youth Member" },
                        authorRole = obj.optString("authorRole").ifBlank { "Youth Member" },
                        authorAvatarUri = obj.optString("authorAvatarUri").takeIf { it.isNotBlank() },
                        content = content,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }

    override fun getActivePosts(): Flow<List<CommunityPostEntity>> =
        communityDao.getActivePosts()

    override fun getFlaggedPosts(): Flow<List<CommunityPostEntity>> =
        communityDao.getFlaggedPosts()

    override fun getPostById(postId: String): Flow<CommunityPostEntity?> =
        communityDao.getPostById(postId)

    override suspend fun pushLocalPostsToCloud() {
        val cloudRepo = cloudRepository ?: return
        try {
            purgeExpiredPosts()
            val activePosts = communityDao.getActivePosts().firstOrNull().orEmpty()
            for (post in activePosts) {
                try {
                    val resolvedAvatar = com.example.ui.components.ensureSyncableImageUri(
                        uriStr = post.authorAvatarUri?.takeIf { it.isNotBlank() }
                            ?: userDao?.getUserByIdSync(post.authorId)?.profilePhotoUri?.takeIf { it.isNotBlank() },
                        maxDimensionPx = 280,
                        maxBase64Length = 90_000
                    )
                    val resolvedAttachment = com.example.ui.components.ensureSyncableImageUri(
                        uriStr = post.attachmentPhotoUri,
                        maxDimensionPx = 760,
                        maxBase64Length = 260_000
                    )
                    val localComments = communityDao.getCommentsForPostSync(post.id)
                    val commentsJson = serializeCommentsJson(localComments)
                    cloudRepo.publishCommunityPost(
                        postId = post.id,
                        authorId = post.authorId,
                        authorName = post.authorName,
                        authorRole = post.authorRole,
                        authorAvatarUri = resolvedAvatar,
                        category = post.departmentOrCategory,
                        title = post.title,
                        content = post.content,
                        attachmentPhotoUri = resolvedAttachment,
                        whatsappContact = post.whatsappContact,
                        commentsJson = commentsJson,
                        isPinned = post.isPinned
                    )
                } catch (e: Exception) {
                    // Continue pushing remaining posts
                }
            }
        } catch (e: Exception) {
            // Ignore when offline
        }
    }

    override suspend fun purgeExpiredPosts(nowMillis: Long): Int {
        val sevenDayCutoff = nowMillis - SEVEN_DAYS_MILLIS
        val threeDayCutoff = nowMillis - THREE_DAYS_MILLIS
        val expiredPosts = try {
            communityDao.getExpiredPostsSync(sevenDayCutoff, threeDayCutoff)
        } catch (e: Exception) {
            emptyList()
        }
        val deletedCount = communityDao.purgeExpiredPostsWithCommentsAndLikes(sevenDayCutoff, threeDayCutoff)
        if (expiredPosts.isNotEmpty() && cloudRepository != null) {
            try {
                expiredPosts.forEach { expired ->
                    cloudRepository.deleteCommunityPost(expired.id)
                }
            } catch (e: Exception) {
                // Local database cleanup already completed
            }
        }
        return deletedCount
    }

    override suspend fun createPost(
        authorId: String,
        authorName: String,
        authorRole: String,
        category: String,
        title: String,
        content: String,
        attachmentPhotoUri: String?,
        whatsappContact: String?,
        authorAvatarUri: String?
    ): Result<Unit> {
        return try {
            purgeExpiredPosts()
            if (CommunityPostEntity.isWorkSkillsCategory(category) && !CommunityPostEntity.isTuesdayToday()) {
                return Result.failure(
                    IllegalStateException("Work/Skills posts are only available on Tuesdays (Market & Skills Showcase Day).")
                )
            }
            val rawAvatar = authorAvatarUri?.takeIf { it.isNotBlank() }
                ?: userDao?.getUserByIdSync(authorId)?.profilePhotoUri?.takeIf { it.isNotBlank() }
            val resolvedAvatar = com.example.ui.components.ensureSyncableImageUri(
                uriStr = rawAvatar,
                maxDimensionPx = 280,
                maxBase64Length = 90_000
            )
            val resolvedAttachment = com.example.ui.components.ensureSyncableImageUri(
                uriStr = attachmentPhotoUri,
                maxDimensionPx = 760,
                maxBase64Length = 260_000
            )
            val normalizedWhatsapp = whatsappContact?.trim()?.takeIf { it.isNotBlank() }
            val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val post = CommunityPostEntity(
                id = postId,
                authorId = authorId,
                authorName = authorName,
                authorRole = authorRole,
                authorAvatarUri = resolvedAvatar,
                departmentOrCategory = category,
                title = title,
                content = content,
                attachmentPhotoUri = resolvedAttachment,
                whatsappContact = normalizedWhatsapp,
                likesCount = 0,
                likedByUserIds = "",
                commentsCount = 0,
                isPinned = false,
                isFlagged = false,
                isHidden = false,
                createdAt = System.currentTimeMillis()
            )
            communityDao.insertPost(post)

            // Trigger real Android & in-app notification to all users' devices
            notificationService?.postNotification(
                id = "notif_post_$postId",
                title = "💬 New Forum Post ($category): $title",
                message = "$authorName ($authorRole): $content",
                type = com.example.data.local.entity.NotificationType.COMMUNITY_POST,
                authorName = authorName,
                authorRole = authorRole
            )

            // Sync to Firestore across all devices
            try {
                cloudRepository?.publishCommunityPost(
                    postId = postId,
                    authorId = authorId,
                    authorName = authorName,
                    authorRole = authorRole,
                    authorAvatarUri = resolvedAvatar,
                    category = category,
                    title = title,
                    content = content,
                    attachmentPhotoUri = resolvedAttachment,
                    whatsappContact = normalizedWhatsapp
                )
            } catch (e: Exception) {
                // Local Room insert already succeeded
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun editPost(
        postId: String,
        actorId: String,
        actorRole: UserRole,
        category: String,
        title: String,
        content: String,
        attachmentPhotoUri: String?,
        whatsappContact: String?
    ): Result<Unit> {
        return try {
            val existing = communityDao.getPostByIdSnapshot(postId)
                ?: return Result.failure(IllegalArgumentException("Post not found"))
            if (existing.authorId != actorId && !actorRole.isLeadership()) {
                return Result.failure(IllegalAccessException("You can only edit your own forum posts"))
            }
            if (CommunityPostEntity.isWorkSkillsCategory(category) &&
                !CommunityPostEntity.isWorkSkillsCategory(existing.departmentOrCategory) &&
                !CommunityPostEntity.isTuesdayToday()
            ) {
                return Result.failure(
                    IllegalStateException("Work/Skills posts can only be created or switched to on Tuesdays.")
                )
            }
            val resolvedAttachment = com.example.ui.components.ensureSyncableImageUri(
                uriStr = attachmentPhotoUri,
                maxDimensionPx = 760,
                maxBase64Length = 260_000
            )
            val resolvedAvatar = com.example.ui.components.ensureSyncableImageUri(
                uriStr = existing.authorAvatarUri?.takeIf { it.isNotBlank() }
                    ?: userDao?.getUserByIdSync(existing.authorId)?.profilePhotoUri?.takeIf { it.isNotBlank() },
                maxDimensionPx = 280,
                maxBase64Length = 90_000
            )
            val normalizedWhatsapp = whatsappContact?.trim()?.takeIf { it.isNotBlank() }

            communityDao.updatePostContent(
                postId = postId,
                category = category,
                title = title.trim(),
                content = content.trim(),
                attachmentPhotoUri = resolvedAttachment,
                whatsappContact = normalizedWhatsapp
            )

            try {
                cloudRepository?.updateCommunityPostContent(
                    postId = postId,
                    category = category,
                    title = title.trim(),
                    content = content.trim(),
                    attachmentPhotoUri = resolvedAttachment,
                    whatsappContact = normalizedWhatsapp,
                    authorAvatarUri = resolvedAvatar
                )
            } catch (e: Exception) {
                // Local update already succeeded
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(
        postId: String,
        actorId: String,
        actorRole: UserRole
    ): Result<Unit> {
        return try {
            val existing = communityDao.getPostByIdSnapshot(postId)
                ?: return Result.failure(IllegalArgumentException("Post not found"))
            if (existing.authorId != actorId && !actorRole.isLeadership()) {
                return Result.failure(IllegalAccessException("You can only delete your own forum posts"))
            }
            communityDao.deletePostWithCommentsAndLikes(postId)
            try {
                cloudRepository?.deleteCommunityPost(postId)
            } catch (e: Exception) {
                // Local delete already succeeded
            }
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = actorId,
                    userRole = actorRole,
                    actionType = "FORUM_POST_DELETED",
                    targetEntity = "CommunityPost: $postId",
                    details = "Deleted forum post '${existing.title}'"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likePost(postId: String): Result<Unit> {
        return try {
            val post = communityDao.getPostByIdSnapshot(postId)
                ?: return Result.failure(IllegalArgumentException("Post not found"))
            val syntheticUserId = "anon_${UUID.randomUUID().toString().take(8)}"
            communityDao.insertPostLike(
                com.example.data.local.entity.CommunityPostLikeEntity(
                    id = "${postId}_$syntheticUserId",
                    postId = postId,
                    userId = syntheticUserId,
                    createdAt = System.currentTimeMillis()
                )
            )
            communityDao.likePost(postId)
            try {
                cloudRepository?.updateCommunityPostEngagement(
                    postId = postId,
                    likesCount = (post.likesCount + 1).toLong()
                )
            } catch (e: Exception) {
                // Local update already succeeded
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleLikePost(postId: String, userId: String): Result<Boolean> {
        return try {
            val post = communityDao.getPostByIdSnapshot(postId)
                ?: return Result.failure(IllegalArgumentException("Post not found"))
            val existingUsers = post.likedByUserIds
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toMutableSet()

            val nowLiked: Boolean
            val newLikesCount: Int
            if (existingUsers.contains(userId)) {
                existingUsers.remove(userId)
                nowLiked = false
                newLikesCount = (post.likesCount - 1).coerceAtLeast(0)
                communityDao.deletePostLike(postId, userId)
            } else {
                existingUsers.add(userId)
                nowLiked = true
                newLikesCount = post.likesCount + 1
                communityDao.insertPostLike(
                    com.example.data.local.entity.CommunityPostLikeEntity(
                        id = "${postId}_$userId",
                        postId = postId,
                        userId = userId,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            communityDao.updatePostLikes(
                postId = postId,
                likesCount = newLikesCount,
                likedByUserIds = existingUsers.joinToString(",")
            )
            try {
                cloudRepository?.updateCommunityPostEngagement(
                    postId = postId,
                    likesCount = newLikesCount.toLong()
                )
            } catch (e: Exception) {
                // Local update already succeeded
            }
            Result.success(nowLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pinPost(postId: String, pinned: Boolean, adminUserId: String): Result<Unit> {
        return try {
            communityDao.pinPost(postId, pinned)
            try {
                cloudRepository?.updateCommunityPostEngagement(
                    postId = postId,
                    isPinned = pinned
                )
            } catch (e: Exception) {
                // Local update already succeeded
            }
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = adminUserId,
                    userRole = UserRole.YOUTH_EXECUTIVE,
                    actionType = if (pinned) "POST_PINNED" else "POST_UNPINNED",
                    targetEntity = "CommunityPost: $postId",
                    details = "Leadership toggled pinned=$pinned"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCommentsForPost(postId: String): Flow<List<CommunityCommentEntity>> =
        communityDao.getCommentsForPost(postId)

    override suspend fun addComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorRole: String,
        content: String,
        authorAvatarUri: String?
    ): Result<Unit> {
        return try {
            val rawAvatar = authorAvatarUri?.takeIf { it.isNotBlank() }
                ?: userDao?.getUserByIdSync(authorId)?.profilePhotoUri?.takeIf { it.isNotBlank() }
            val resolvedAvatar = com.example.ui.components.ensureSyncableImageUri(
                uriStr = rawAvatar,
                maxDimensionPx = 120,
                maxBase64Length = 25_000
            )
            val comment = CommunityCommentEntity(
                id = UUID.randomUUID().toString(),
                postId = postId,
                authorId = authorId,
                authorName = authorName,
                authorRole = authorRole,
                authorAvatarUri = resolvedAvatar,
                content = content,
                createdAt = System.currentTimeMillis()
            )
            communityDao.insertComment(comment)
            communityDao.incrementCommentsCount(postId)
            val postSnapshot = communityDao.getPostByIdSnapshot(postId)
            val allComments = communityDao.getCommentsForPostSync(postId)
            val commentsJson = serializeCommentsJson(allComments)
            try {
                val updateResult = cloudRepository?.updateCommunityPostEngagement(
                    postId = postId,
                    commentsCount = allComments.size.toLong(),
                    commentsJson = commentsJson
                )
                if (updateResult?.isFailure == true && postSnapshot != null) {
                    cloudRepository?.publishCommunityPost(
                        postId = postSnapshot.id,
                        authorId = postSnapshot.authorId,
                        authorName = postSnapshot.authorName,
                        authorRole = postSnapshot.authorRole,
                        authorAvatarUri = postSnapshot.authorAvatarUri,
                        category = postSnapshot.departmentOrCategory,
                        title = postSnapshot.title,
                        content = postSnapshot.content,
                        attachmentPhotoUri = postSnapshot.attachmentPhotoUri,
                        whatsappContact = postSnapshot.whatsappContact,
                        commentsJson = commentsJson,
                        isPinned = postSnapshot.isPinned
                    )
                }
            } catch (e: Exception) {
                // Local insert already succeeded
            }
            notificationService?.postNotification(
                id = "notif_comment_${comment.id}",
                title = "💬 New Reply on '${postSnapshot?.title ?: "Forum Post"}'",
                message = "$authorName ($authorRole): $content",
                type = com.example.data.local.entity.NotificationType.COMMUNITY_POST,
                authorName = authorName,
                authorRole = authorRole
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun flagPost(postId: String, flagged: Boolean): Result<Unit> {
        return try {
            communityDao.flagPost(postId, flagged)
            try {
                cloudRepository?.updateCommunityPostEngagement(
                    postId = postId,
                    isFlagged = flagged
                )
            } catch (e: Exception) {
                // Local update already succeeded
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hidePost(postId: String, hidden: Boolean, adminUserId: String): Result<Unit> {
        return try {
            communityDao.hidePost(postId, hidden)
            try {
                cloudRepository?.updateCommunityPostEngagement(
                    postId = postId,
                    isHidden = hidden
                )
            } catch (e: Exception) {
                // Local update already succeeded
            }
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = adminUserId,
                    userRole = UserRole.ADMINISTRATOR,
                    actionType = if (hidden) "POST_HIDDEN_MODERATION" else "POST_UNHIDDEN",
                    targetEntity = "CommunityPost: $postId",
                    details = "Moderation action toggled hidden=$hidden"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllInquiries(): Flow<List<ExecutiveInquiryEntity>> =
        communityDao.getAllInquiries()

    override fun getInquiriesForUser(userId: String): Flow<List<ExecutiveInquiryEntity>> =
        communityDao.getInquiriesBySender(userId)

    override suspend fun submitExecutiveInquiry(
        senderId: String,
        senderName: String,
        senderRole: String,
        isAnonymous: Boolean,
        topic: String,
        message: String,
        senderAvatarUri: String?
    ): Result<ExecutiveInquiryEntity> {
        return try {
            val resolvedAvatar: String? = if (isAnonymous) {
                null
            } else {
                val raw = senderAvatarUri?.takeIf { it.isNotBlank() }
                    ?: userDao?.getUserByIdSync(senderId)?.profilePhotoUri?.takeIf { it.isNotBlank() }
                com.example.ui.components.ensureSyncableImageUri(
                    uriStr = raw,
                    maxDimensionPx = 280,
                    maxBase64Length = 90_000
                )
            }
            val inquiryId = "inq_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val displayName = if (isAnonymous) "Anonymous Youth Member" else senderName
            val inquiry = ExecutiveInquiryEntity(
                id = inquiryId,
                senderId = senderId,
                senderName = displayName,
                senderRole = senderRole,
                senderAvatarUri = resolvedAvatar,
                isAnonymous = isAnonymous,
                topic = topic,
                message = message.trim(),
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
            communityDao.insertInquiry(inquiry)

            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = senderId,
                    userRole = UserRole.YOUTH_MEMBER,
                    actionType = "EXECUTIVE_INQUIRY_SUBMITTED",
                    targetEntity = "ExecutiveInquiry: $inquiryId",
                    details = "Topic: $topic (Anonymous: $isAnonymous)"
                )
            )

            try {
                cloudRepository?.submitExecutiveInquiry(
                    inquiryId = inquiryId,
                    senderId = senderId,
                    senderName = displayName,
                    senderRole = senderRole,
                    senderAvatarUri = resolvedAvatar,
                    isAnonymous = isAnonymous,
                    topic = topic,
                    message = message.trim()
                )
            } catch (e: Exception) {
                // Local Room insert already succeeded
            }
            notificationService?.postNotification(
                id = "notif_inq_$inquiryId",
                title = "📩 Executive Desk Inquiry: $topic",
                message = "$displayName: ${message.trim()}",
                type = com.example.data.local.entity.NotificationType.COMMUNITY_POST,
                authorName = displayName,
                authorRole = senderRole
            )
            Result.success(inquiry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun respondToExecutiveInquiry(
        inquiryId: String,
        status: String,
        responseMessage: String,
        responderId: String,
        responderName: String,
        responderRole: String
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            communityDao.respondToInquiry(
                inquiryId = inquiryId,
                status = status,
                responseMessage = responseMessage.trim(),
                respondedByName = responderName,
                respondedByRole = responderRole,
                respondedAt = now
            )
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = responderId,
                    userRole = UserRole.YOUTH_EXECUTIVE,
                    actionType = "EXECUTIVE_INQUIRY_RESPONDED",
                    targetEntity = "ExecutiveInquiry: $inquiryId",
                    details = "Status updated to $status by $responderName"
                )
            )
            val existingInquiry = communityDao.getInquiryByIdSnapshot(inquiryId)
            try {
                cloudRepository?.respondToExecutiveInquiry(
                    inquiryId = inquiryId,
                    status = status,
                    responseMessage = responseMessage.trim(),
                    respondedByName = responderName,
                    respondedByRole = responderRole
                )
            } catch (e: Exception) {
                // Local Room update already succeeded
            }
            notificationService?.postNotification(
                id = "notif_inq_resp_${inquiryId}_$now",
                title = "✅ Executive Response (${existingInquiry?.topic ?: "Inquiry"})",
                message = "$responderName ($responderRole): ${responseMessage.trim()}",
                type = com.example.data.local.entity.NotificationType.ANNOUNCEMENT,
                authorName = responderName,
                authorRole = responderRole,
                recipientUserId = existingInquiry?.senderId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncCloudPostsToLocalDb(
        cloudPosts: List<com.example.data.remote.CloudCommunityPost>,
        announcementDao: com.example.data.local.dao.AnnouncementDao?,
        nowMillis: Long
    ) {
        purgeExpiredPosts(nowMillis)
        val cloudForumPostIds = mutableSetOf<String>()
        for (cloudPost in cloudPosts) {
            if (cloudPost.id.isBlank()) continue
            val createdAtMillis = cloudPost.createdAt?.toDate()?.time ?: nowMillis
            val isAnnouncement = cloudPost.category.startsWith("ANNOUNCEMENT", ignoreCase = true)

            if (isAnnouncement) {
                if (announcementDao != null) {
                    val rawId = cloudPost.id.removePrefix("ann_")
                    val isUrgent = cloudPost.category.equals("ANNOUNCEMENT_URGENT", ignoreCase = true)
                    val announcement = com.example.data.local.entity.AnnouncementEntity(
                        id = rawId,
                        title = cloudPost.title,
                        content = cloudPost.content,
                        priority = if (isUrgent) {
                            com.example.data.model.AnnouncementPriority.URGENT
                        } else {
                            com.example.data.model.AnnouncementPriority.IMPORTANT
                        },
                        targetAudience = com.example.data.model.AnnouncementAudience.ALL,
                        authorId = cloudPost.authorId,
                        authorName = cloudPost.authorName,
                        authorRole = cloudPost.authorRole,
                        publishedAt = createdAtMillis,
                        isPinned = cloudPost.isPinned
                    )
                    announcementDao.insertAnnouncement(announcement)
                }
            } else {
                val retentionMillis = CommunityPostEntity.retentionDurationForCategory(cloudPost.category)
                // Enforce 3-day auto-deletion for Work/Skills posts and 7-day auto-deletion for other Forum Posts alongside all replies and likes
                if (nowMillis - createdAtMillis >= retentionMillis) {
                    communityDao.deletePostWithCommentsAndLikes(cloudPost.id)
                    try {
                        cloudRepository?.deleteCommunityPost(cloudPost.id)
                    } catch (e: Exception) {
                        // Ignore cloud delete permission errors if not post owner/admin
                    }
                } else {
                    cloudForumPostIds.add(cloudPost.id)
                    val existing = communityDao.getPostByIdSnapshot(cloudPost.id)
                    val authorUser = userDao?.getUserByIdSync(cloudPost.authorId)
                    val resolvedAvatar = cloudPost.authorAvatarUri?.takeIf { it.isNotBlank() }
                        ?: existing?.authorAvatarUri?.takeIf { it.isNotBlank() }
                        ?: authorUser?.profilePhotoUri?.takeIf { it.isNotBlank() }
                    val normalizedCategory = when {
                        cloudPost.category.equals("Faith & Career", ignoreCase = true) -> CommunityPostEntity.CATEGORY_DISCUSSION
                        cloudPost.category.equals("Spiritual Growth", ignoreCase = true) -> CommunityPostEntity.CATEGORY_WORK_SKILLS
                        else -> cloudPost.category
                    }

                    val existingComments = communityDao.getCommentsForPostSync(cloudPost.id)
                    val existingCommentsById = existingComments.associateBy { it.id }
                    val remoteComments = deserializeCommentsJson(cloudPost.id, cloudPost.commentsJson)
                    val resolvedInitialCommentsCount = maxOf(
                        existing?.commentsCount ?: 0,
                        cloudPost.commentsCount.toInt(),
                        existingComments.size,
                        remoteComments.size
                    )

                    val syncedPost = CommunityPostEntity(
                        id = cloudPost.id,
                        authorId = cloudPost.authorId,
                        authorName = cloudPost.authorName,
                        authorRole = cloudPost.authorRole,
                        authorAvatarUri = resolvedAvatar,
                        departmentOrCategory = normalizedCategory,
                        title = cloudPost.title,
                        content = cloudPost.content,
                        attachmentPhotoUri = cloudPost.attachmentPhotoUri ?: existing?.attachmentPhotoUri,
                        whatsappContact = cloudPost.whatsappContact ?: existing?.whatsappContact,
                        likesCount = maxOf(existing?.likesCount ?: 0, cloudPost.likesCount.toInt()),
                        likedByUserIds = existing?.likedByUserIds ?: "",
                        commentsCount = resolvedInitialCommentsCount,
                        isPinned = cloudPost.isPinned,
                        isFlagged = cloudPost.isFlagged,
                        isHidden = cloudPost.isHidden,
                        createdAt = existing?.createdAt ?: createdAtMillis
                    )
                    // Upsert parent post FIRST so foreign key constraint on community_comments is always satisfied
                    communityDao.insertPost(syncedPost)

                    for (rc in remoteComments) {
                        val existingComment = existingCommentsById[rc.id]
                        val commentAvatar = rc.authorAvatarUri?.takeIf { it.isNotBlank() }
                            ?: existingComment?.authorAvatarUri?.takeIf { it.isNotBlank() }
                            ?: userDao?.getUserByIdSync(rc.authorId)?.profilePhotoUri?.takeIf { it.isNotBlank() }
                        communityDao.insertComment(rc.copy(authorAvatarUri = commentAvatar))
                        if (existingComment == null) {
                            notificationService?.syncRemoteNotificationIfNew(
                                id = "notif_comment_${rc.id}",
                                title = "💬 New Reply on '${cloudPost.title}'",
                                message = "${rc.authorName} (${rc.authorRole}): ${rc.content}",
                                type = com.example.data.local.entity.NotificationType.COMMUNITY_POST,
                                authorName = rc.authorName,
                                authorRole = rc.authorRole,
                                timestamp = rc.createdAt,
                                activeUserId = cloudRepository?.activeSessionUserId
                            )
                        }
                    }

                    val totalLocalComments = communityDao.getCommentsForPostSync(cloudPost.id).size
                    if (totalLocalComments > resolvedInitialCommentsCount) {
                        communityDao.updatePostCommentsCount(cloudPost.id, totalLocalComments)
                    }
                }
            }
        }
    }

    override suspend fun syncCloudInquiriesToLocalDb(
        cloudInquiries: List<com.example.data.remote.CloudExecutiveInquiry>
    ) {
        for (inq in cloudInquiries) {
            if (inq.id.isBlank()) continue
            val existing = communityDao.getInquiryByIdSnapshot(inq.id)
            val createdAtMillis = existing?.createdAt ?: (inq.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            val synced = ExecutiveInquiryEntity(
                id = inq.id,
                senderId = inq.senderId,
                senderName = inq.senderName,
                senderRole = inq.senderRole,
                senderAvatarUri = inq.senderAvatarUri ?: existing?.senderAvatarUri,
                isAnonymous = inq.isAnonymous,
                topic = inq.topic,
                message = inq.message,
                status = inq.status,
                responseMessage = inq.responseMessage ?: existing?.responseMessage,
                respondedByName = inq.respondedByName ?: existing?.respondedByName,
                respondedByRole = inq.respondedByRole ?: existing?.respondedByRole,
                respondedAt = inq.respondedAt ?: existing?.respondedAt,
                createdAt = createdAtMillis
            )
            communityDao.insertInquiry(synced)
            if (existing == null) {
                notificationService?.syncRemoteNotificationIfNew(
                    id = "notif_inq_${inq.id}",
                    title = "📩 Executive Desk Inquiry: ${inq.topic}",
                    message = "${inq.senderName}: ${inq.message}",
                    type = com.example.data.local.entity.NotificationType.COMMUNITY_POST,
                    authorName = inq.senderName,
                    authorRole = inq.senderRole,
                    timestamp = createdAtMillis,
                    activeUserId = cloudRepository?.activeSessionUserId
                )
            } else if (existing.responseMessage.isNullOrBlank() && !inq.responseMessage.isNullOrBlank()) {
                notificationService?.syncRemoteNotificationIfNew(
                    id = "notif_inq_resp_${inq.id}",
                    title = "✅ Executive Response (${inq.topic})",
                    message = "${inq.respondedByName ?: "Youth Executive"}: ${inq.responseMessage}",
                    type = com.example.data.local.entity.NotificationType.ANNOUNCEMENT,
                    authorName = inq.respondedByName ?: "Youth Executive",
                    authorRole = inq.respondedByRole ?: "Youth Executive",
                    timestamp = inq.respondedAt ?: System.currentTimeMillis(),
                    recipientUserId = inq.senderId,
                    activeUserId = cloudRepository?.activeSessionUserId
                )
            }
        }
    }
}

// ==========================================
// 4. Spiritual Resource Repository (FR-020)
// ==========================================

data class SermonAudioDraft(
    val audioUri: String,
    val audioCloudId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val durationMinutes: Int
)

interface SpiritualResourceRepository {
    fun getAllResources(): Flow<List<SpiritualResourceEntity>>
    fun getResourcesByCategory(category: String): Flow<List<SpiritualResourceEntity>>
    fun getBookmarkedResources(): Flow<List<SpiritualResourceEntity>>
    fun getResourceById(id: String): Flow<SpiritualResourceEntity?>
    suspend fun toggleBookmark(id: String, currentStatus: Boolean): Result<Unit>
    suspend fun addResource(resource: SpiritualResourceEntity): Result<Unit>
    suspend fun updateResource(resource: SpiritualResourceEntity): Result<Unit>
    suspend fun deleteResource(id: String): Result<Unit>
    suspend fun pushLocalResourcesToCloud()
    suspend fun syncCloudScripturesToLocalDb(cloudScriptures: List<com.example.data.remote.CloudDailyScripture>)
    suspend fun importAndCacheAudioFile(contentUriString: String): Result<SermonAudioDraft> =
        Result.failure(UnsupportedOperationException("Audio import requires Android context"))
    suspend fun ensureAudioFileReady(resource: SpiritualResourceEntity): Result<File> =
        Result.failure(UnsupportedOperationException("Audio playback requires Android context"))
    suspend fun cleanupStaleAudioDraft(audioUri: String?) {}
}

class SpiritualResourceRepositoryImpl(
    private val spiritualResourceDao: SpiritualResourceDao,
    private val cloudRepository: GideonitesCloudRepository? = null,
    private val notificationService: com.example.core.notification.FellowshipNotificationService? = null,
    private val appContext: Context? = null
) : SpiritualResourceRepository {

    companion object {
        const val MAX_SERMON_AUDIO_BYTES = 24L * 1024L * 1024L // 24 MB practical chunked Firestore limit
        private const val SERMON_AUDIO_DIR = "sermon_audio"

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0L) return ""
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format(java.util.Locale.US, "%.1f MB", mb)
            } else {
                val kb = (bytes / 1024.0).coerceAtLeast(1.0)
                String.format(java.util.Locale.US, "%.0f KB", kb)
            }
        }

        private val EXPLICIT_AUDIO_EXTENSIONS = setOf(
            "mp3", "m4a", "aac", "wav", "ogg", "oga", "opus", "flac", "3gp", "amr", "wma", "m4b", "mpga", "mpeg"
        )
        private val VIDEO_CONTAINER_EXTENSIONS = setOf(
            "mp4", "m4v", "mov", "mkv", "webm", "3gp", "avi", "ts"
        )
        private val SUPPORTED_AUDIO_EXTENSIONS = EXPLICIT_AUDIO_EXTENSIONS + VIDEO_CONTAINER_EXTENSIONS
        private val REJECTED_NON_AUDIO_EXTENSIONS = setOf(
            "pdf", "epub", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "json", "xml",
            "html", "htm", "jpg", "jpeg", "png", "gif", "webp", "zip", "rar", "7z", "apk"
        )

        fun isExplicitAudioFile(mimeType: String?, fileName: String?): Boolean {
            val normalizedMime = mimeType?.trim()?.lowercase().orEmpty()
            val ext = fileName?.substringAfterLast('.', "")?.trim()?.lowercase().orEmpty()
            if (ext in EXPLICIT_AUDIO_EXTENSIONS) return true
            if (normalizedMime.startsWith("audio/")) return true
            if (normalizedMime == "application/ogg" || normalizedMime == "application/x-ogg") return true
            return false
        }

        fun isExplicitAudioFile(context: Context, uri: Uri): Boolean {
            val (name, mime) = resolveDisplayNameAndMime(context, uri)
            return isExplicitAudioFile(mime, name)
        }

        fun isVideoContainerCandidate(mimeType: String?, fileName: String?): Boolean {
            val normalizedMime = mimeType?.trim()?.lowercase().orEmpty()
            val ext = fileName?.substringAfterLast('.', "")?.trim()?.lowercase().orEmpty()
            if (ext in EXPLICIT_AUDIO_EXTENSIONS && !normalizedMime.startsWith("video/")) return false
            return normalizedMime.startsWith("video/") || ext in VIDEO_CONTAINER_EXTENSIONS
        }

        fun isSupportedAudioCandidate(mimeType: String?, fileName: String?): Boolean {
            val normalizedMime = mimeType?.trim()?.lowercase().orEmpty()
            val ext = fileName?.substringAfterLast('.', "")?.trim()?.lowercase().orEmpty()
            if (ext in REJECTED_NON_AUDIO_EXTENSIONS) return false
            if (normalizedMime.startsWith("text/") || normalizedMime.startsWith("image/")) return false
            if (normalizedMime.contains("pdf") || normalizedMime.contains("epub") || normalizedMime.contains("wordprocessing")) {
                return false
            }
            if (isExplicitAudioFile(normalizedMime, fileName)) return true
            if (isVideoContainerCandidate(normalizedMime, fileName)) return true
            if (ext in SUPPORTED_AUDIO_EXTENSIONS) return true
            if (normalizedMime in setOf("application/octet-stream", "")) {
                return true
            }
            return false
        }

        fun isSupportedAudioCandidate(context: Context, uri: Uri): Boolean {
            val (name, mime) = resolveDisplayNameAndMime(context, uri)
            return isSupportedAudioCandidate(mime, name)
        }

        private fun extensionForMimeOrName(mimeType: String?, fileName: String?): String {
            val fromName = fileName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it in SUPPORTED_AUDIO_EXTENSIONS }
            if (fromName != null) return fromName
            return when (mimeType?.lowercase()) {
                "audio/mpeg", "audio/mp3", "audio/mpeg3", "audio/x-mpeg-3" -> "mp3"
                "audio/mp4", "audio/aac", "audio/x-m4a", "audio/m4a", "audio/mp4a-latm", "video/mp4" -> "m4a"
                "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
                "audio/ogg", "application/ogg", "application/x-ogg" -> "ogg"
                "audio/opus" -> "opus"
                "audio/flac", "audio/x-flac" -> "flac"
                "audio/3gpp", "video/3gpp" -> "3gp"
                "audio/amr", "audio/amr-wb" -> "amr"
                "audio/x-ms-wma" -> "wma"
                "audio/webm", "video/webm" -> "webm"
                "video/quicktime" -> "mov"
                "video/x-matroska" -> "mkv"
                else -> "mp3"
            }
        }

        private fun resolveDisplayNameAndMime(ctx: Context, uri: Uri): Pair<String, String> {
            var name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "sermon_audio.mp3"
            try {
                ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { name = it }
                        }
                    }
                }
            } catch (_: Exception) {
            }
            val rawMime = runCatching { ctx.contentResolver.getType(uri) }.getOrNull().orEmpty()
            val ext = name.substringAfterLast('.', "").lowercase()
            val mime = if (rawMime.isNotBlank() && rawMime != "application/octet-stream") {
                rawMime
            } else {
                when (ext) {
                    "mp3", "mpga", "mpeg" -> "audio/mpeg"
                    "m4a", "aac", "m4b" -> "audio/mp4"
                    "wav" -> "audio/wav"
                    "ogg", "oga" -> "audio/ogg"
                    "opus" -> "audio/opus"
                    "flac" -> "audio/flac"
                    "3gp" -> "audio/3gpp"
                    "amr" -> "audio/amr"
                    "wma" -> "audio/x-ms-wma"
                    "mp4", "m4v" -> rawMime.ifBlank { "video/mp4" }
                    "mov" -> rawMime.ifBlank { "video/quicktime" }
                    "mkv" -> rawMime.ifBlank { "video/x-matroska" }
                    "webm" -> rawMime.ifBlank { "video/webm" }
                    "avi" -> rawMime.ifBlank { "video/x-msvideo" }
                    else -> rawMime.ifBlank { "application/octet-stream" }
                }
            }
            return name to mime
        }

        private data class ExtractedVideoAudioResult(
            val file: File,
            val ext: String,
            val mimeType: String,
            val durationMs: Long
        )

        /**
         * Strips the video stream from a video sermon (.mp4, .mov, .mkv, .webm, .3gp)
         * and writes ONLY the encoded audio track into an audio-only .m4a (or .webm) file.
         * This lets large sermon videos fit cleanly inside the 24 MB cloud limit and guarantees
         * MediaPlayer plays the actual audio track cleanly across all Android devices.
         */
        private fun tryExtractAudioTrackFromVideoUri(
            context: Context,
            uri: Uri,
            audioDir: File,
            audioCloudId: String
        ): ExtractedVideoAudioResult? {
            val extractor = MediaExtractor()
            var muxer: MediaMuxer? = null
            var outFile: File? = null
            try {
                extractor.setDataSource(context, uri, null)
                var audioTrackIndex = -1
                var audioFormat: MediaFormat? = null
                var trackMime = ""
                for (i in 0 until extractor.trackCount) {
                    val fmt = extractor.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME)?.lowercase().orEmpty()
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        audioFormat = fmt
                        trackMime = mime
                        break
                    }
                }
                if (audioTrackIndex < 0 || audioFormat == null) return null

                val useWebm = trackMime.contains("opus") || trackMime.contains("vorbis")
                val outExt = if (useWebm) "webm" else "m4a"
                val muxerFormat = if (useWebm) {
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
                } else {
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                }
                val candidateFile = File(audioDir, "$audioCloudId.$outExt")
                outFile = candidateFile

                extractor.selectTrack(audioTrackIndex)
                val activeMuxer = MediaMuxer(candidateFile.absolutePath, muxerFormat)
                muxer = activeMuxer
                val dstTrack = activeMuxer.addTrack(audioFormat)
                activeMuxer.start()

                val maxInputSize = runCatching {
                    audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                }.getOrDefault(256 * 1024).coerceIn(64 * 1024, 1024 * 1024)
                val buffer = ByteBuffer.allocate(maxInputSize)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                    bufferInfo.flags = extractor.sampleFlags
                    activeMuxer.writeSampleData(dstTrack, buffer, bufferInfo)
                    if (candidateFile.length() > MAX_SERMON_AUDIO_BYTES) {
                        runCatching { activeMuxer.stop() }
                        runCatching { activeMuxer.release() }
                        muxer = null
                        runCatching { candidateFile.delete() }
                        return null
                    }
                    extractor.advance()
                }
                activeMuxer.stop()
                activeMuxer.release()
                muxer = null

                if (!candidateFile.exists() || candidateFile.length() <= 0L) {
                    runCatching { candidateFile.delete() }
                    return null
                }

                val durationUs = runCatching {
                    if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                        audioFormat.getLong(MediaFormat.KEY_DURATION)
                    } else {
                        0L
                    }
                }.getOrDefault(0L)

                return ExtractedVideoAudioResult(
                    file = candidateFile,
                    ext = outExt,
                    mimeType = if (useWebm) "audio/webm" else "audio/mp4",
                    durationMs = (durationUs / 1000L).coerceAtLeast(0L)
                )
            } catch (_: Exception) {
                runCatching { outFile?.delete() }
                return null
            } finally {
                runCatching { muxer?.release() }
                runCatching { extractor.release() }
            }
        }

        suspend fun importAudioFromContentUri(
            context: Context,
            contentUriString: String
        ): Result<SermonAudioDraft> = withContext(Dispatchers.IO) {
            val audioDir = File(context.applicationContext.filesDir, SERMON_AUDIO_DIR).apply {
                if (!exists()) mkdirs()
            }
            try {
                val uri = Uri.parse(contentUriString)
                val (rawFileName, initialMimeType) = resolveDisplayNameAndMime(context, uri)
                if (!isSupportedAudioCandidate(initialMimeType, rawFileName)) {
                    return@withContext Result.failure(
                        IllegalArgumentException("'$rawFileName' is not a supported audio/video file. Please pick an MP3, M4A, AAC, WAV, OGG, Voice Note, or MP4 video.")
                    )
                }

                val audioCloudId = "sa_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val isVideoSource = isVideoContainerCandidate(initialMimeType, rawFileName)

                // If the user picked a video file (.mp4, .mov, .mkv, .webm, etc.), extract ONLY its audio track first
                if (isVideoSource) {
                    val extracted = tryExtractAudioTrackFromVideoUri(context, uri, audioDir, audioCloudId)
                    if (extracted != null) {
                        var durationMs = extracted.durationMs
                        if (durationMs <= 0L) {
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(extracted.file.absolutePath)
                                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                            } catch (_: Exception) {
                            } finally {
                                runCatching { retriever.release() }
                            }
                        }
                        val baseName = rawFileName.substringBeforeLast('.').ifBlank { "sermon_audio" }
                        val outFileName = "$baseName.${extracted.ext}"
                        val durationMins = if (durationMs > 0L) {
                            kotlin.math.ceil(durationMs / 60_000.0).toInt().coerceAtLeast(1)
                        } else {
                            1
                        }
                        return@withContext Result.success(
                            SermonAudioDraft(
                                audioUri = extracted.file.absolutePath,
                                audioCloudId = audioCloudId,
                                fileName = outFileName,
                                mimeType = extracted.mimeType,
                                sizeBytes = extracted.file.length(),
                                durationMs = durationMs,
                                durationMinutes = durationMins
                            )
                        )
                    }
                }

                val ext = extensionForMimeOrName(initialMimeType, rawFileName)
                val fileName = if (rawFileName.contains('.')) rawFileName else "$rawFileName.$ext"
                val destFile = File(audioDir, "$audioCloudId.$ext")

                var totalBytes = 0L
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            totalBytes += read
                            if (totalBytes > MAX_SERMON_AUDIO_BYTES) {
                                break
                            }
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                } ?: return@withContext Result.failure(IllegalStateException("Unable to read selected audio file"))

                if (totalBytes <= 0L) {
                    runCatching { destFile.delete() }
                    return@withContext Result.failure(IllegalArgumentException("Selected audio file is empty"))
                }
                if (totalBytes > MAX_SERMON_AUDIO_BYTES) {
                    runCatching { destFile.delete() }
                    val approxMb = String.format(java.util.Locale.US, "%.1f", totalBytes / (1024.0 * 1024.0))
                    return@withContext Result.failure(
                        IllegalArgumentException(
                            if (isVideoSource) {
                                "Video audio exceeds 24 MB (~$approxMb+ MB). Please pick an MP3/M4A audio file or shorter video under 24 MB."
                            } else {
                                "Audio file is larger than 24 MB (~$approxMb+ MB). Please choose a compressed MP3/M4A/AAC file under 24 MB."
                            }
                        )
                    )
                }

                var durationMs = 0L
                var hasAudioTrack = false
                var detectedMime: String? = null
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(destFile.absolutePath)
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    hasAudioTrack = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                        ?.equals("yes", ignoreCase = true) == true
                    detectedMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                        ?.takeIf { it.isNotBlank() }
                } catch (_: Exception) {
                } finally {
                    runCatching { retriever.release() }
                }

                val confirmedAudio = isExplicitAudioFile(initialMimeType, fileName) ||
                    hasAudioTrack ||
                    durationMs > 0L ||
                    detectedMime?.lowercase()?.startsWith("audio/") == true

                if (!confirmedAudio) {
                    runCatching { destFile.delete() }
                    return@withContext Result.failure(
                        IllegalArgumentException("'$fileName' does not contain a playable audio track. Please select an MP3, M4A, AAC, WAV, OGG, Voice Note, or video with sound.")
                    )
                }

                val resolvedMime = when {
                    initialMimeType.startsWith("audio/") -> initialMimeType
                    detectedMime?.startsWith("audio/") == true -> detectedMime!!
                    isVideoSource -> "video/$ext"
                    else -> "audio/$ext"
                }

                val durationMins = if (durationMs > 0L) {
                    kotlin.math.ceil(durationMs / 60_000.0).toInt().coerceAtLeast(1)
                } else {
                    1
                }

                Result.success(
                    SermonAudioDraft(
                        audioUri = destFile.absolutePath,
                        audioCloudId = audioCloudId,
                        fileName = fileName,
                        mimeType = resolvedMime,
                        sizeBytes = totalBytes,
                        durationMs = durationMs,
                        durationMinutes = durationMins
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getAudioDir(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, SERMON_AUDIO_DIR).apply { if (!exists()) mkdirs() }
    }

    override suspend fun importAndCacheAudioFile(contentUriString: String): Result<SermonAudioDraft> {
        val ctx = appContext ?: return Result.failure(IllegalStateException("App context unavailable"))
        return importAudioFromContentUri(ctx, contentUriString)
    }

    override suspend fun ensureAudioFileReady(resource: SpiritualResourceEntity): Result<File> =
        withContext(Dispatchers.IO) {
            val existingPath = resource.audioUri?.takeIf { it.isNotBlank() }
            if (existingPath != null) {
                val existingFile = File(existingPath)
                if (existingFile.exists() && existingFile.length() > 0L) {
                    return@withContext Result.success(existingFile)
                }
            }

            val cloudId = resource.audioCloudId?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(IllegalStateException("No audio recording attached to this sermon"))

            val audioDir = getAudioDir()
                ?: return@withContext Result.failure(IllegalStateException("Audio cache directory unavailable"))
            val ext = extensionForMimeOrName(resource.audioMimeType, resource.audioFileName)
            val cachedFile = File(audioDir, "$cloudId.$ext")
            if (cachedFile.exists() && cachedFile.length() > 0L) {
                spiritualResourceDao.insertResources(listOf(resource.copy(audioUri = cachedFile.absolutePath)))
                return@withContext Result.success(cachedFile)
            }

            val cloud = cloudRepository
                ?: return@withContext Result.failure(IllegalStateException("Cloud audio repository unavailable"))
            val downloaded = cloud.downloadSermonAudioFile(
                audioCloudId = cloudId,
                expectedChunkCount = resource.audioChunkCount.coerceAtLeast(1),
                destFile = cachedFile
            )
            downloaded.onSuccess { file ->
                spiritualResourceDao.insertResources(listOf(resource.copy(audioUri = file.absolutePath)))
            }
            downloaded
        }

    override suspend fun cleanupStaleAudioDraft(audioUri: String?) {
        if (audioUri.isNullOrBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val target = File(audioUri)
                val stillReferenced = spiritualResourceDao.getAllResourcesSync().any { it.audioUri == audioUri }
                if (!stillReferenced && target.exists()) {
                    target.delete()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun deleteLocalFilesExcept(keepAudioUri: String?, keepAudioCloudId: String?) {
        try {
            val dir = getAudioDir() ?: return
            val keepNamePrefix = keepAudioCloudId?.takeIf { it.isNotBlank() }
            val keepFile = keepAudioUri?.takeIf { it.isNotBlank() }?.let { File(it).canonicalPath }
            dir.listFiles()?.forEach { f ->
                val matchesPath = keepFile != null && runCatching { f.canonicalPath == keepFile }.getOrDefault(false)
                val matchesCloud = keepNamePrefix != null && f.nameWithoutExtension == keepNamePrefix
                if (!matchesPath && !matchesCloud) {
                    runCatching { f.delete() }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun deleteLocalFileForResource(resource: SpiritualResourceEntity) {
        try {
            resource.audioUri?.takeIf { it.isNotBlank() }?.let { path ->
                runCatching { File(path).delete() }
            }
            resource.audioCloudId?.takeIf { it.isNotBlank() }?.let { cloudId ->
                val dir = getAudioDir()
                dir?.listFiles()?.filter { it.nameWithoutExtension == cloudId }?.forEach {
                    runCatching { it.delete() }
                }
            }
        } catch (_: Exception) {
        }
    }

    internal fun buildResourceMetadataJson(resource: SpiritualResourceEntity): String {
        return try {
            fun makeJson(titleCap: Int, summaryCap: Int): String {
                val obj = org.json.JSONObject().apply {
                    put("category", resource.category.ifBlank { "SERMON" }.take(40))
                    put("audioDurationMinutes", resource.audioDurationMinutes)
                    put("publishedAt", resource.publishedAt)
                    if (!resource.audioCloudId.isNullOrBlank()) {
                        put("audioCloudId", resource.audioCloudId.take(100))
                        put("audioFileName", (resource.audioFileName ?: "sermon_audio.mp3").take(80))
                        put("audioMimeType", (resource.audioMimeType ?: "audio/mpeg").take(60))
                        put("audioSizeBytes", resource.audioSizeBytes)
                        put("audioChunkCount", resource.audioChunkCount)
                    }
                    put("title", resource.title.take(titleCap))
                    if (summaryCap > 0 && resource.summary.isNotBlank()) {
                        put("summary", resource.summary.take(summaryCap))
                    }
                }
                return obj.toString()
            }

            // Guarantee complete, valid JSON <= 980 chars so audioCloudId and closing '}' are NEVER truncated
            val candidates = listOf(
                180 to 260,
                160 to 140,
                140 to 0,
                80 to 0
            )
            for ((tCap, sCap) in candidates) {
                val json = makeJson(tCap, sCap)
                if (json.length <= 980) return json
            }
            makeJson(40, 0).let { if (it.length <= 980) it else makeJson(0, 0) }
        } catch (_: Exception) {
            "Lord, strengthen The Gideonites in word and deed."
        }
    }

    private suspend fun syncResourceToCloud(resource: SpiritualResourceEntity) {
        val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(resource.publishedAt)) ?: "2026-10-25"
        val fullBody = resource.contentBody.ifBlank { resource.summary.ifBlank { "Daily charge for The Gideonites." } }
        val part1 = fullBody.take(3000).ifBlank { "Daily charge for The Gideonites." }
        val part2 = fullBody.drop(3000).take(3000).takeIf { it.isNotBlank() }
        val part3 = fullBody.drop(6000).take(3000).takeIf { it.isNotBlank() }
        val part4 = fullBody.drop(9000).take(3000).takeIf { it.isNotBlank() }
        cloudRepository?.publishDailyScripture(
            scriptureId = resource.id,
            authorName = resource.speakerOrAuthor.ifBlank { "Youth Leadership" },
            dateKey = formattedDate,
            referenceEn = resource.scriptureReference.ifBlank { "Judges 6:12" },
            verseTextEn = resource.summary.ifBlank { resource.title.ifBlank { "Spiritual Resource" } },
            reflectionEn = part1,
            reflectionYo = part2,
            reflectionHa = part3,
            reflectionIg = part4,
            prayerPromptEn = buildResourceMetadataJson(resource)
        )
    }

    /**
     * Enforces single active uploaded audio sermon: whenever a new audio sermon is uploaded
     * (or an existing sermon gets a replacement audio file), previous uploaded audio sermons,
     * their local cached files, and their cloud chunks/documents are deleted and replaced.
     */
    private suspend fun replacePreviousAudioSermons(activeResource: SpiritualResourceEntity) {
        if (!activeResource.hasAudio) return
        val existingAll = try {
            spiritualResourceDao.getAllResourcesSync()
        } catch (_: Exception) {
            emptyList()
        }
        val previousAudioSermons = existingAll.filter { other ->
            other.id != activeResource.id && other.hasAudio
        }
        for (old in previousAudioSermons) {
            runCatching { spiritualResourceDao.deleteResourceById(old.id) }
            runCatching { cloudRepository?.deleteDailyScripture(old.id) }
        }
        deleteLocalFilesExcept(activeResource.audioUri, activeResource.audioCloudId)
        runCatching { cloudRepository?.deleteOtherSermonAudioChunks(activeResource.audioCloudId) }
    }

    override fun getAllResources(): Flow<List<SpiritualResourceEntity>> =
        spiritualResourceDao.getAllResources()

    override fun getResourcesByCategory(category: String): Flow<List<SpiritualResourceEntity>> =
        spiritualResourceDao.getResourcesByCategory(category)

    override fun getBookmarkedResources(): Flow<List<SpiritualResourceEntity>> =
        spiritualResourceDao.getBookmarkedResources()

    override fun getResourceById(id: String): Flow<SpiritualResourceEntity?> =
        spiritualResourceDao.getResourceById(id)

    override suspend fun toggleBookmark(id: String, currentStatus: Boolean): Result<Unit> {
        return try {
            spiritualResourceDao.toggleBookmark(id, !currentStatus)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addResource(resource: SpiritualResourceEntity): Result<Unit> {
        return try {
            var resolvedResource = resource
            if (resolvedResource.hasAudio && !resolvedResource.audioUri.isNullOrBlank()) {
                val localFile = File(resolvedResource.audioUri)
                if (!localFile.exists() || localFile.length() <= 0L) {
                    return Result.failure(IllegalStateException("Selected audio file could not be read from cache. Please pick the file again."))
                }
                val cloudId = resolvedResource.audioCloudId
                    ?.takeIf { it.isNotBlank() }
                    ?: "sa_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                if (cloudRepository != null && resolvedResource.audioChunkCount <= 0) {
                    val uploadResult = withContext(Dispatchers.IO) {
                        cloudRepository.uploadSermonAudioFile(cloudId, localFile)
                    }
                    val uploadedChunks = uploadResult.getOrElse { err ->
                        return Result.failure(
                            IllegalStateException("Audio upload failed: ${err.message ?: "Check your connection and retry."}")
                        )
                    }
                    resolvedResource = resolvedResource.copy(
                        audioCloudId = cloudId,
                        audioChunkCount = uploadedChunks
                    )
                }
            }

            if (resolvedResource.hasAudio) {
                replacePreviousAudioSermons(resolvedResource)
            }

            spiritualResourceDao.insertResources(listOf(resolvedResource))
            try {
                syncResourceToCloud(resolvedResource)
            } catch (_: Exception) {
                // Local Room insert already succeeded
            }
            if (!resolvedResource.category.equals("SCRIPTURE_OF_THE_DAY", ignoreCase = true)) {
                val categoryLabel = resolvedResource.category.replace("_", " ")
                notificationService?.postNotification(
                    id = "notif_res_${resolvedResource.id}",
                    title = if (resolvedResource.hasAudio) {
                        "🎧 New Audio Sermon: ${resolvedResource.title}"
                    } else {
                        "📚 New $categoryLabel: ${resolvedResource.title}"
                    },
                    message = "${resolvedResource.speakerOrAuthor.ifBlank { "Youth Leadership" }} • ${resolvedResource.summary.ifBlank { resolvedResource.contentBody.take(120) }}",
                    type = com.example.data.local.entity.NotificationType.ANNOUNCEMENT,
                    authorName = resolvedResource.speakerOrAuthor.ifBlank { "Youth Leadership" },
                    authorRole = "Spiritual Library"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateResource(resource: SpiritualResourceEntity): Result<Unit> {
        return try {
            val existing = spiritualResourceDao.getResourceByIdSync(resource.id)
            var resolvedResource = resource.copy(isBookmarked = existing?.isBookmarked ?: resource.isBookmarked)

            val audioChanged = existing?.audioCloudId != resolvedResource.audioCloudId ||
                existing?.audioUri != resolvedResource.audioUri

            if (resolvedResource.hasAudio && audioChanged && !resolvedResource.audioUri.isNullOrBlank()) {
                val localFile = File(resolvedResource.audioUri)
                if (!localFile.exists() || localFile.length() <= 0L) {
                    return Result.failure(IllegalStateException("Selected audio file could not be read from cache. Please pick the file again."))
                }
                val cloudId = resolvedResource.audioCloudId
                    ?.takeIf { it.isNotBlank() }
                    ?: "sa_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                if (cloudRepository != null && resolvedResource.audioChunkCount <= 0) {
                    val uploadResult = withContext(Dispatchers.IO) {
                        cloudRepository.uploadSermonAudioFile(cloudId, localFile)
                    }
                    val uploadedChunks = uploadResult.getOrElse { err ->
                        return Result.failure(
                            IllegalStateException("Audio upload failed: ${err.message ?: "Check your connection and retry."}")
                        )
                    }
                    resolvedResource = resolvedResource.copy(
                        audioCloudId = cloudId,
                        audioChunkCount = uploadedChunks
                    )
                }
            }

            if (resolvedResource.hasAudio) {
                replacePreviousAudioSermons(resolvedResource)
            } else if (existing?.hasAudio == true && !resolvedResource.hasAudio) {
                // Audio removed during edit
                deleteLocalFileForResource(existing)
                existing.audioCloudId?.let { oldCloudId ->
                    runCatching { cloudRepository?.deleteSermonAudioChunksForId(oldCloudId) }
                }
            }

            spiritualResourceDao.insertResources(listOf(resolvedResource))
            try {
                syncResourceToCloud(resolvedResource)
            } catch (_: Exception) {
                // Local update already succeeded
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteResource(id: String): Result<Unit> {
        return try {
            val existing = spiritualResourceDao.getResourceByIdSync(id)
            if (existing != null && existing.hasAudio) {
                deleteLocalFileForResource(existing)
                existing.audioCloudId?.takeIf { it.isNotBlank() }?.let { cloudId ->
                    runCatching { cloudRepository?.deleteSermonAudioChunksForId(cloudId) }
                }
            }
            spiritualResourceDao.deleteResourceById(id)
            try {
                cloudRepository?.deleteDailyScripture(id)
            } catch (_: Exception) {
                // Local delete already succeeded
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushLocalResourcesToCloud() {
        if (cloudRepository == null) return
        try {
            val all = spiritualResourceDao.getAllResourcesSync()
            for (res in all) {
                try {
                    var target = res
                    if (target.hasAudio && target.audioChunkCount <= 0 && !target.audioUri.isNullOrBlank()) {
                        val localFile = File(target.audioUri)
                        if (localFile.exists() && localFile.length() > 0L) {
                            val cloudId = target.audioCloudId?.takeIf { it.isNotBlank() }
                                ?: "sa_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                            val uploaded = withContext(Dispatchers.IO) {
                                cloudRepository.uploadSermonAudioFile(cloudId, localFile)
                            }.getOrNull() ?: 0
                            if (uploaded > 0) {
                                target = target.copy(audioCloudId = cloudId, audioChunkCount = uploaded)
                                spiritualResourceDao.insertResources(listOf(target))
                            }
                        }
                    }
                    syncResourceToCloud(target)
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    override suspend fun syncCloudScripturesToLocalDb(
        cloudScriptures: List<com.example.data.remote.CloudDailyScripture>
    ) {
        val entities = mutableListOf<SpiritualResourceEntity>()
        val activeCloudIds = mutableSetOf<String>()
        var latestCloudAudioId: String? = null
        var latestCloudAudioPublishedAt = -1L

        for (s in cloudScriptures) {
            if (s.id.isBlank()) continue
            if (s.id.startsWith("scripture_bibledoc_") || s.id.startsWith("scripture_biblepack_")) continue
            val rawId = s.id.removePrefix("scripture_")
            val existing = spiritualResourceDao.getResourceByIdSync(rawId) ?: spiritualResourceDao.getResourceByIdSync(s.id)
            val targetId = existing?.id ?: rawId.ifBlank { s.id }

            var parsedCategory: String? = null
            var parsedTitle: String? = null
            var parsedSummary: String? = null
            var parsedDuration: Int? = null
            var parsedPublishedAt: Long? = null
            var parsedAudioCloudId: String? = null
            var parsedAudioFileName: String? = null
            var parsedAudioMimeType: String? = null
            var parsedAudioSizeBytes = 0L
            var parsedAudioChunkCount = 0

            val prompt = s.prayerPromptEn.trim()
            if (prompt.startsWith("{") && prompt.endsWith("}")) {
                try {
                    val obj = org.json.JSONObject(prompt)
                    parsedCategory = obj.optString("category").takeIf { it.isNotBlank() }
                    parsedTitle = obj.optString("title").takeIf { it.isNotBlank() }
                    parsedSummary = obj.optString("summary").takeIf { it.isNotBlank() }
                    if (obj.has("audioDurationMinutes")) {
                        parsedDuration = obj.optInt("audioDurationMinutes", 0)
                    }
                    if (obj.has("publishedAt")) {
                        parsedPublishedAt = obj.optLong("publishedAt", 0L).takeIf { it > 0L }
                    }
                    parsedAudioCloudId = obj.optString("audioCloudId").takeIf { it.isNotBlank() }
                    parsedAudioFileName = obj.optString("audioFileName").takeIf { it.isNotBlank() }
                    parsedAudioMimeType = obj.optString("audioMimeType").takeIf { it.isNotBlank() }
                    parsedAudioSizeBytes = obj.optLong("audioSizeBytes", 0L)
                    parsedAudioChunkCount = obj.optInt("audioChunkCount", 0)
                } catch (_: Exception) {
                }
            }

            if (parsedCategory.equals("BIBLE_DOCUMENT", ignoreCase = true) ||
                parsedCategory.equals("BIBLE_VERSE_PACK", ignoreCase = true)
            ) {
                continue
            }

            activeCloudIds.add(targetId)
            activeCloudIds.add(s.id)

            val resolvedCategory = parsedCategory
                ?: existing?.category
                ?: "SCRIPTURE_OF_THE_DAY"
            val resolvedTitle = parsedTitle
                ?: existing?.title
                ?: if (resolvedCategory.equals("SCRIPTURE_OF_THE_DAY", ignoreCase = true)) {
                    "Scripture for the Day • ${s.referenceEn}"
                } else {
                    s.verseTextEn.ifBlank { "Spiritual Resource (${s.referenceEn})" }
                }
            val resolvedSummary = parsedSummary
                ?: existing?.summary?.takeIf { it.isNotBlank() }
                ?: s.verseTextEn.ifBlank { s.reflectionEn.take(140) }
            val resolvedDuration = parsedDuration ?: existing?.audioDurationMinutes ?: 0
            val resolvedPublishedAt = parsedPublishedAt
                ?: existing?.publishedAt
                ?: s.createdAt?.toDate()?.time
                ?: System.currentTimeMillis()

            if (!parsedAudioCloudId.isNullOrBlank() && resolvedPublishedAt >= latestCloudAudioPublishedAt) {
                latestCloudAudioPublishedAt = resolvedPublishedAt
                latestCloudAudioId = parsedAudioCloudId
            }

            val preservedLocalAudioUri = if (parsedAudioCloudId != null && existing?.audioCloudId == parsedAudioCloudId) {
                existing.audioUri?.takeIf { path -> File(path).exists() }
            } else if (parsedAudioCloudId == null) {
                null
            } else {
                // Check if pre-cached in sermon_audio folder
                val dir = getAudioDir()
                val ext = extensionForMimeOrName(parsedAudioMimeType, parsedAudioFileName)
                val expectedFile = dir?.let { File(it, "$parsedAudioCloudId.$ext") }
                expectedFile?.takeIf { it.exists() && it.length() > 0L }?.absolutePath
            }

            val reassembledBody = if (prompt.startsWith("{")) {
                buildString {
                    append(s.reflectionEn)
                    if (!s.reflectionYo.isNullOrBlank()) append(s.reflectionYo)
                    if (!s.reflectionHa.isNullOrBlank()) append(s.reflectionHa)
                    if (!s.reflectionIg.isNullOrBlank()) append(s.reflectionIg)
                }
            } else {
                s.reflectionEn
            }

            entities.add(
                SpiritualResourceEntity(
                    id = targetId,
                    title = resolvedTitle,
                    category = resolvedCategory,
                    speakerOrAuthor = s.authorName.ifBlank { existing?.speakerOrAuthor ?: "Youth Leadership" },
                    scriptureReference = s.referenceEn,
                    summary = resolvedSummary,
                    contentBody = reassembledBody,
                    audioDurationMinutes = resolvedDuration,
                    isBookmarked = existing?.isBookmarked ?: false,
                    publishedAt = resolvedPublishedAt,
                    audioUri = preservedLocalAudioUri,
                    audioCloudId = parsedAudioCloudId,
                    audioFileName = parsedAudioFileName,
                    audioMimeType = parsedAudioMimeType,
                    audioSizeBytes = parsedAudioSizeBytes,
                    audioChunkCount = parsedAudioChunkCount
                )
            )
        }
        if (entities.isNotEmpty()) {
            spiritualResourceDao.insertResources(entities)
        }

        // Remove any local audio sermons that were deleted or replaced on another device
        try {
            val nowMs = System.currentTimeMillis()
            val localAll = spiritualResourceDao.getAllResourcesSync()
            for (localRes in localAll) {
                val isAudioRes = !localRes.audioCloudId.isNullOrBlank()
                if (!isAudioRes) continue
                val recentlyPublishedLocally = (nowMs - localRes.publishedAt) in 0L..15_000L
                val removedFromCloud = !recentlyPublishedLocally &&
                    localRes.audioChunkCount > 0 &&
                    localRes.id !in activeCloudIds &&
                    "scripture_${localRes.id}" !in activeCloudIds
                val supersededByNewAudio = latestCloudAudioId != null &&
                    localRes.audioCloudId != latestCloudAudioId &&
                    latestCloudAudioPublishedAt > localRes.publishedAt
                if (removedFromCloud || supersededByNewAudio) {
                    deleteLocalFileForResource(localRes)
                    spiritualResourceDao.deleteResourceById(localRes.id)
                }
            }
        } catch (_: Exception) {
        }
    }
}
